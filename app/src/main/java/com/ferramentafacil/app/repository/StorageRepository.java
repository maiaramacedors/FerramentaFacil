package com.ferramentafacil.app.repository;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import com.ferramentafacil.app.utils.ImageUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Armazenamento das imagens SEM o Firebase Storage.
 *
 * Motivo: o Firebase Storage exige o plano pago (Blaze, com cartão de crédito) em projetos novos.
 * Para manter o projeto 100% gratuito (plano Spark), as fotos são:
 *   1) redimensionadas e comprimidas em JPEG;
 *   2) convertidas para texto Base64 no formato "data:image/jpeg;base64,...";
 *   3) gravadas no próprio documento do Firestore (campo "imagens" da ferramenta
 *      ou "fotoPerfil" do usuário).
 * O Glide sabe exibir esse formato diretamente, então as telas não mudam.
 *
 * Limite: cada documento do Firestore aceita até 1 MB, por isso as fotos são reduzidas
 * (800 px, qualidade 60) e o tamanho total é conferido antes de salvar.
 *
 * Para voltar a usar o Firebase Storage no futuro, basta trocar a implementação desta classe.
 */
public class StorageRepository {

    public static final int LADO_FERRAMENTA = 800;
    public static final int QUALIDADE_FERRAMENTA = 60;
    public static final int LADO_PERFIL = 320;
    public static final int QUALIDADE_PERFIL = 70;
    /** Tamanho máximo somado das fotos de um anúncio (margem abaixo do limite de 1 MB). */
    public static final int TAMANHO_MAXIMO_FOTOS = 850_000;

    private static final String PREFIXO = "data:image/jpeg;base64,";

    /** Thread em segundo plano para comprimir as imagens sem travar a tela. */
    private static final Executor EXECUTOR = Executors.newFixedThreadPool(2);

    /** Converte uma foto em texto Base64 (data URI) já comprimida. */
    public Task<String> converter(Context context, Uri uri, int ladoMaximo, int qualidade) {
        Context app = context.getApplicationContext();
        TaskCompletionSource<String> tcs = new TaskCompletionSource<>();
        EXECUTOR.execute(() -> {
            try {
                byte[] bytes = ImageUtils.comprimir(app, uri, ladoMaximo, qualidade);
                tcs.setResult(PREFIXO + Base64.encodeToString(bytes, Base64.NO_WRAP));
            } catch (Exception e) {
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    /** Foto de ferramenta. O parâmetro "caminho" é mantido por compatibilidade e não é usado. */
    public Task<String> enviarImagem(Context context, Uri uri, String caminho) {
        return converter(context, uri, LADO_FERRAMENTA, QUALIDADE_FERRAMENTA);
    }

    /** Converte várias fotos em paralelo, mantendo a ordem. */
    public Task<List<String>> enviarImagens(Context context, List<Uri> uris, String pasta) {
        if (uris == null || uris.isEmpty()) return Tasks.forResult(new ArrayList<>());
        List<Task<String>> tarefas = new ArrayList<>();
        for (Uri uri : uris) tarefas.add(enviarImagem(context, uri, pasta));
        return Tasks.whenAllSuccess(tarefas);
    }

    /**
     * Nada a fazer: as fotos ficam dentro do documento do Firestore
     * e somem junto com ele quando o anúncio é editado ou excluído.
     */
    public void excluirPorUrl(List<String> urls) {
        // sem Storage, não há arquivos separados para apagar
    }
}
