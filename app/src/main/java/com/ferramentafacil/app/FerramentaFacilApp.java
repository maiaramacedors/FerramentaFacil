package com.ferramentafacil.app;

import android.app.Application;

import com.ferramentafacil.app.service.NotificationHelper;

/**
 * Classe Application: executada uma única vez quando o app é iniciado.
 * Aqui criamos o canal de notificações (obrigatório a partir do Android 8).
 * O Firestore já mantém cache offline por padrão.
 */
public class FerramentaFacilApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.criarCanais(this);
    }
}
