package com.ferramentafacil.app.repository;

import android.content.Context;
import android.net.Uri;

import com.ferramentafacil.app.model.RentalStatus;
import com.ferramentafacil.app.model.Tool;
import com.ferramentafacil.app.model.User;
import com.ferramentafacil.app.utils.Constants;
import com.ferramentafacil.app.utils.FirebaseErrorUtils;
import com.ferramentafacil.app.utils.TextoUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Anúncios de ferramentas: publicar, editar, listar, excluir. */
public class ToolRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final StorageRepository storage = new StorageRepository();
    private final UserRepository userRepo = new UserRepository();

    private CollectionReference tools() {
        return db.collection(Constants.COL_TOOLS);
    }

    private String uid() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u != null ? u.getUid() : null;
    }

    /**
     * Publica (nova = true) ou atualiza um anúncio.
     * 1) Comprime as fotos novas (Base64) — ver StorageRepository;
     * 2) Grava o anúncio público em tools/{id};
     * 3) Grava o endereço completo em tools/{id}/privado/endereco (somente o dono lê).
     *
     * @param tool            dados do anúncio; tool.imagens deve conter as URLs já existentes que serão mantidas
     * @param novasImagens    fotos novas (galeria/câmera) a enviar
     * @param enderecoPrivado endereço completo e coordenadas exatas
     */
    public void salvar(Context ctx, Tool tool, List<Uri> novasImagens, Map<String, Object> enderecoPrivado,
                       boolean nova, Callback<String> cb) {
        String uid = uid();
        if (uid == null) {
            cb.onError("Sessão expirada. Entre novamente.");
            return;
        }
        DocumentReference ref = nova ? tools().document() : tools().document(tool.toolId);
        String toolId = ref.getId();
        String pasta = "tools/" + uid + "/" + toolId + "/";

        storage.enviarImagens(ctx, novasImagens, pasta)
                .addOnSuccessListener(urls -> {
                    List<String> imagens = new ArrayList<>(tool.imagens != null ? tool.imagens : new ArrayList<>());
                    imagens.addAll(urls);
                    if (imagens.size() > Constants.MAX_FOTOS) {
                        imagens = new ArrayList<>(imagens.subList(0, Constants.MAX_FOTOS));
                    }
                    // As fotos ficam dentro do documento, que tem limite de 1 MB no Firestore
                    long tamanho = 0;
                    for (String img : imagens) tamanho += img.length();
                    if (tamanho > StorageRepository.TAMANHO_MAXIMO_FOTOS) {
                        cb.onError("As fotos ficaram grandes demais para um anúncio. Remova uma ou duas fotos e tente novamente.");
                        return;
                    }
                    long agora = System.currentTimeMillis();
                    tool.imagens = imagens;
                    tool.ownerId = uid;
                    tool.nomeBusca = TextoUtils.normalizar(tool.nome);
                    tool.dataAtualizacao = agora;

                    WriteBatch batch = db.batch();
                    if (nova) {
                        tool.dataCadastro = agora;
                        tool.somaNotas = 0;
                        tool.totalAvaliacoes = 0;
                        tool.avaliacaoMedia = 0;
                        tool.periodosOcupados = new ArrayList<>();
                        batch.set(ref, tool);
                    } else {
                        // Na edição atualizamos só os campos do formulário (avaliações e reservas não mudam)
                        batch.update(ref, camposEditaveis(tool));
                    }
                    batch.set(ref.collection(Constants.SUB_PRIVADO).document(Constants.DOC_ENDERECO), enderecoPrivado);
                    batch.commit()
                            .addOnSuccessListener(v -> cb.onSuccess(toolId))
                            .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
                })
                .addOnFailureListener(e -> cb.onError("Falha ao enviar as fotos. " + FirebaseErrorUtils.mensagem(e)));
    }

    private Map<String, Object> camposEditaveis(Tool t) {
        Map<String, Object> m = new HashMap<>();
        m.put("nome", t.nome);
        m.put("nomeBusca", t.nomeBusca);
        m.put("categoria", t.categoria);
        m.put("marca", t.marca);
        m.put("modelo", t.modelo);
        m.put("descricao", t.descricao);
        m.put("estadoConservacao", t.estadoConservacao);
        m.put("imagens", t.imagens);
        m.put("valorDiaria", t.valorDiaria);
        m.put("latitude", t.latitude);
        m.put("longitude", t.longitude);
        m.put("cidade", t.cidade);
        m.put("bairro", t.bairro);
        m.put("disponivel", t.disponivel);
        m.put("dataAtualizacao", t.dataAtualizacao);
        return m;
    }

    /** Lista anúncios (opcionalmente apenas os disponíveis) já com dados do proprietário. */
    public void listar(boolean somenteDisponiveis, Callback<List<Tool>> cb) {
        Query q = tools();
        if (somenteDisponiveis) q = q.whereEqualTo("disponivel", true);
        q.limit(500).get()
                .addOnSuccessListener(snap -> enriquecer(snap.toObjects(Tool.class), cb))
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    public void listarMinhas(Callback<List<Tool>> cb) {
        tools().whereEqualTo("ownerId", uid()).get()
                .addOnSuccessListener(snap -> {
                    List<Tool> lista = snap.toObjects(Tool.class);
                    Collections.sort(lista, (a, b) -> Long.compare(b.dataCadastro, a.dataCadastro));
                    cb.onSuccess(lista);
                })
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    public void contarDoUsuario(String uid, Callback<Integer> cb) {
        tools().whereEqualTo("ownerId", uid).get()
                .addOnSuccessListener(snap -> cb.onSuccess(snap.size()))
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    public void buscarPorId(String toolId, Callback<Tool> cb) {
        tools().document(toolId).get()
                .addOnSuccessListener(doc -> {
                    Tool t = doc.toObject(Tool.class);
                    if (t == null) {
                        cb.onError("Esta ferramenta não está mais anunciada.");
                        return;
                    }
                    List<Tool> lista = new ArrayList<>();
                    lista.add(t);
                    enriquecer(lista, new Callback<List<Tool>>() {
                        @Override public void onSuccess(List<Tool> r) { cb.onSuccess(r.get(0)); }
                        @Override public void onError(String m) { cb.onSuccess(t); }
                    });
                })
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    /** Busca os documentos de várias ferramentas por ID (usado nos favoritos). */
    public void buscarVarias(List<String> ids, Callback<List<Tool>> cb) {
        if (ids.isEmpty()) {
            cb.onSuccess(new ArrayList<>());
            return;
        }
        List<com.google.android.gms.tasks.Task<DocumentSnapshot>> tarefas = new ArrayList<>();
        for (String id : ids) tarefas.add(tools().document(id).get());
        com.google.android.gms.tasks.Tasks.whenAllComplete(tarefas).addOnCompleteListener(t -> {
            List<Tool> lista = new ArrayList<>();
            for (com.google.android.gms.tasks.Task<DocumentSnapshot> tk : tarefas) {
                if (tk.isSuccessful() && tk.getResult() != null) {
                    Tool tool = tk.getResult().toObject(Tool.class);
                    if (tool != null) lista.add(tool);
                }
            }
            enriquecer(lista, cb);
        });
    }

    /** Preenche nome, foto e avaliação do proprietário em cada ferramenta. */
    public void enriquecer(List<Tool> lista, Callback<List<Tool>> cb) {
        Set<String> ids = new HashSet<>();
        for (Tool t : lista) if (t.ownerId != null) ids.add(t.ownerId);
        userRepo.buscarVarios(ids).addOnCompleteListener(task -> {
            Map<String, User> mapa = task.isSuccessful() && task.getResult() != null
                    ? task.getResult() : new HashMap<>();
            for (Tool t : lista) {
                User u = mapa.get(t.ownerId);
                if (u != null) {
                    t.ownerNome = u.nome;
                    t.ownerFoto = u.fotoPerfil;
                    t.ownerNota = u.media();
                    t.ownerTotalAvaliacoes = u.totalAvaliacoes;
                    t.ownerVerificado = u.verificado;
                }
            }
            cb.onSuccess(lista);
        });
    }

    public void buscarEndereco(String toolId, Callback<Map<String, Object>> cb) {
        tools().document(toolId).collection(Constants.SUB_PRIVADO).document(Constants.DOC_ENDERECO).get()
                .addOnSuccessListener(doc -> cb.onSuccess(doc.getData() != null ? doc.getData() : new HashMap<>()))
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    public void alterarDisponibilidade(String toolId, boolean disponivel, Callback<Void> cb) {
        tools().document(toolId).update("disponivel", disponivel, "dataAtualizacao", System.currentTimeMillis())
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    /**
     * Altera o preço da diária. Aluguéis já solicitados/aprovados mantêm o valor
     * que foi "congelado" no momento da solicitação.
     */
    public void alterarPreco(String toolId, double valor, Callback<Void> cb) {
        tools().document(toolId).update("valorDiaria", valor, "dataAtualizacao", System.currentTimeMillis())
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }

    /** Exclui o anúncio — bloqueado se houver solicitações pendentes ou aluguéis ativos. */
    public void excluir(Tool tool, Callback<Void> cb) {
        db.collection(Constants.COL_RENTALS)
                .whereEqualTo("ownerId", uid())
                .whereEqualTo("toolId", tool.toolId)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        if (RentalStatus.ativo(d.getString("status"))) {
                            cb.onError("Esta ferramenta tem solicitações pendentes ou aluguéis em andamento. "
                                    + "Responda/finalize-os antes de excluir o anúncio.");
                            return;
                        }
                    }
                    DocumentReference ref = tools().document(tool.toolId);
                    WriteBatch b = db.batch();
                    b.delete(ref.collection(Constants.SUB_PRIVADO).document(Constants.DOC_ENDERECO));
                    b.delete(ref);
                    b.commit()
                            .addOnSuccessListener(v -> {
                                storage.excluirPorUrl(tool.imagens);
                                cb.onSuccess(null);
                            })
                            .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
                })
                .addOnFailureListener(e -> cb.onError(FirebaseErrorUtils.mensagem(e)));
    }
}
