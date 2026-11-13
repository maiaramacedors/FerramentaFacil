package com.ferramentafacil.app.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.ferramentafacil.app.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Funções auxiliares de interface (diálogos, toasts, imagens). */
public final class UiUtils {

    private UiUtils() { }

    public static String texto(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    public static void toast(Context c, String mensagem) {
        if (c != null) Toast.makeText(c, mensagem, Toast.LENGTH_LONG).show();
    }

    /** Diálogo de confirmação (usado antes de excluir anúncios, cancelar solicitações etc.). */
    public static void confirmar(Context c, String titulo, String mensagem, String textoConfirmar, Runnable aoConfirmar) {
        new MaterialAlertDialogBuilder(c)
                .setTitle(titulo)
                .setMessage(mensagem)
                .setNegativeButton("Voltar", null)
                .setPositiveButton(textoConfirmar, (d, w) -> aoConfirmar.run())
                .show();
    }

    public static void alerta(Context c, String titulo, String mensagem) {
        new MaterialAlertDialogBuilder(c)
                .setTitle(titulo)
                .setMessage(mensagem)
                .setPositiveButton("OK", null)
                .show();
    }

    /** Diálogo de "carregando..." que impede toques enquanto uma operação acontece. */
    public static AlertDialog carregando(Context c, String mensagem) {
        View v = LayoutInflater.from(c).inflate(R.layout.dialog_loading, null);
        ((TextView) v.findViewById(R.id.tvMensagem)).setText(mensagem);
        return new MaterialAlertDialogBuilder(c).setView(v).setCancelable(false).create();
    }

    /** Carrega uma imagem (URL ou Uri) com placeholder. */
    public static void carregarImagem(ImageView iv, Object fonte, @DrawableRes int placeholder) {
        if (fonte == null || (fonte instanceof String && ((String) fonte).isEmpty())) {
            Glide.with(iv).clear(iv);
            iv.setImageResource(placeholder);
            return;
        }
        Glide.with(iv).load(fonte).placeholder(placeholder).error(placeholder).centerCrop().into(iv);
    }

    public static int dp(Context c, int dp) {
        return Math.round(dp * c.getResources().getDisplayMetrics().density);
    }

    public static String formatarNota(double media, long total) {
        if (total <= 0) return "Novo";
        return String.format(new java.util.Locale("pt", "BR"), "%.1f", media);
    }
}
