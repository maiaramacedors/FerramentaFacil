package com.ferramentafacil.app.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.ferramentafacil.app.R;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Tela de abertura: anima o logotipo e redireciona para o Login
 * ou direto para a Home, se o usuário já estiver autenticado.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long DURACAO_MS = 1600;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View logo = findViewById(R.id.logoContainer);
        logo.setScaleX(0.5f);
        logo.setScaleY(0.5f);
        logo.setAlpha(0f);
        logo.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(700)
                .setInterpolator(new OvershootInterpolator()).start();

        View ilustracao = findViewById(R.id.ilustracao);
        ilustracao.setAlpha(0f);
        ilustracao.setTranslationY(40f);
        ilustracao.animate().alpha(1f).translationY(0f).setStartDelay(400).setDuration(600).start();

        handler.postDelayed(this::seguir, DURACAO_MS);
    }

    private void seguir() {
        if (isFinishing()) return;
        boolean logado = FirebaseAuth.getInstance().getCurrentUser() != null;
        startActivity(new Intent(this, logado ? MainActivity.class : LoginActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
