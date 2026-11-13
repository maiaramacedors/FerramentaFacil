package com.ferramentafacil.app.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.ferramentafacil.app.R;
import com.google.android.material.appbar.MaterialToolbar;

/** Termos de uso e política de privacidade (texto em res/values/strings.xml). */
public class TermosActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_termos);
        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
    }
}
