package com.ferramentafacil.app.service;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.activity.NotificacoesActivity;
import com.ferramentafacil.app.model.Notificacao;

/** Cria o canal e exibe notificações do Android. */
public final class NotificationHelper {

    public static final String CANAL_ATIVIDADES = "atividades";

    private NotificationHelper() { }

    public static void criarCanais(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(CANAL_ATIVIDADES,
                    ctx.getString(R.string.canal_notificacoes), NotificationManager.IMPORTANCE_HIGH);
            canal.setDescription(ctx.getString(R.string.canal_notificacoes_desc));
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(canal);
        }
    }

    public static boolean podeNotificar(Context ctx) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void mostrar(Context ctx, Notificacao n) {
        if (!podeNotificar(ctx)) return;
        Intent intent = new Intent(ctx, NotificacoesActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CANAL_ATIVIDADES)
                .setSmallIcon(R.drawable.ic_build)
                .setColor(ContextCompat.getColor(ctx, R.color.laranja))
                .setContentTitle(n.titulo)
                .setContentText(n.mensagem)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(n.mensagem))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);
        try {
            int id = n.id != null ? n.id.hashCode() : (int) System.currentTimeMillis();
            NotificationManagerCompat.from(ctx).notify(id, b.build());
        } catch (SecurityException ignorado) {
            // Permissão revogada pelo usuário
        }
    }
}
