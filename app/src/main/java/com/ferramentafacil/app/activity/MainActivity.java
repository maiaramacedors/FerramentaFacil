package com.ferramentafacil.app.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.ferramentafacil.app.R;
import com.ferramentafacil.app.fragment.FavoritosFragment;
import com.ferramentafacil.app.fragment.HomeFragment;
import com.ferramentafacil.app.fragment.MeusAlugueisFragment;
import com.ferramentafacil.app.fragment.PerfilFragment;
import com.ferramentafacil.app.service.NotificacaoWatcher;
import com.ferramentafacil.app.service.NotificationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Tela principal com a navegação inferior:
 * Início • Favoritos • Anunciar • Meus aluguéis • Perfil.
 * Os fragments são criados uma vez e apenas mostrados/escondidos (mantém o estado de cada aba).
 * "Anunciar" abre a tela de cadastro de ferramenta.
 */
public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_ABA = "extra_aba";
    public static final String EXTRA_SUB_ABA = "extra_sub_aba";
    public static final int ABA_INICIO = 0;
    public static final int ABA_FAVORITOS = 1;
    public static final int ABA_ALUGUEIS = 2;
    public static final int ABA_PERFIL = 3;

    private static final String[] TAGS = {"inicio", "favoritos", "alugueis", "perfil"};
    private static final int[] MENU_IDS = {R.id.nav_inicio, R.id.nav_favoritos, R.id.nav_alugueis, R.id.nav_perfil};

    private BottomNavigationView bottomNav;

    private final ActivityResultLauncher<String> permissaoNotificacao =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedida -> { });

    /** Abre a tela principal numa aba específica (ex.: depois de solicitar um aluguel). */
    public static Intent intentAba(android.content.Context c, int aba, int subAba) {
        return new Intent(c, MainActivity.class)
                .putExtra(EXTRA_ABA, aba)
                .putExtra(EXTRA_SUB_ABA, subAba)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        bottomNav = findViewById(R.id.bottomNav);

        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState == null) {
            FragmentTransaction t = fm.beginTransaction();
            for (int i = 0; i < TAGS.length; i++) {
                Fragment f = criarFragment(i);
                t.add(R.id.fragmentContainer, f, TAGS[i]);
                if (i != ABA_INICIO) t.hide(f);
            }
            t.commitNow();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_anunciar) {
                startActivity(new Intent(this, CadastrarFerramentaActivity.class));
                return false; // não troca a aba selecionada
            }
            for (int i = 0; i < MENU_IDS.length; i++) {
                if (MENU_IDS[i] == item.getItemId()) {
                    mostrar(i);
                    return true;
                }
            }
            return false;
        });

        tratarIntent(getIntent());
        pedirPermissaoNotificacao();
        NotificacaoWatcher.get().iniciar(getApplicationContext(), user.getUid());
    }

    private Fragment criarFragment(int i) {
        switch (i) {
            case ABA_FAVORITOS: return new FavoritosFragment();
            case ABA_ALUGUEIS: return new MeusAlugueisFragment();
            case ABA_PERFIL: return new PerfilFragment();
            default: return new HomeFragment();
        }
    }

    private void mostrar(int indice) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction t = fm.beginTransaction();
        for (int i = 0; i < TAGS.length; i++) {
            Fragment f = fm.findFragmentByTag(TAGS[i]);
            if (f == null) continue;
            if (i == indice) t.show(f);
            else t.hide(f);
        }
        t.commit();
    }

    /** Seleciona uma aba da navegação inferior. */
    public void abrirAba(int aba) {
        bottomNav.setSelectedItemId(MENU_IDS[aba]);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        tratarIntent(intent);
    }

    private void tratarIntent(Intent intent) {
        if (intent == null) return;
        int aba = intent.getIntExtra(EXTRA_ABA, -1);
        if (aba < 0) return;
        abrirAba(aba);
        int sub = intent.getIntExtra(EXTRA_SUB_ABA, -1);
        if (aba == ABA_ALUGUEIS && sub >= 0) {
            Fragment f = getSupportFragmentManager().findFragmentByTag(TAGS[ABA_ALUGUEIS]);
            if (f instanceof MeusAlugueisFragment) ((MeusAlugueisFragment) f).selecionarAba(sub);
        }
        intent.removeExtra(EXTRA_ABA);
    }

    private void pedirPermissaoNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.podeNotificar(this)) {
            permissaoNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}
