package com.example.iptvplayer;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import com.example.iptvplayer.data.Movie;
import com.example.iptvplayer.data.Channel;

public class MainActivity extends AppCompatActivity {

    private final VodFragment vodFragment = new VodFragment();
    private final TvFragment tvFragment = new TvFragment();
    private final ProfileFragment profileFragment = new ProfileFragment();
    private XtreamLoginService xtreamLoginService;
    private XtreamApiService xtreamApiService;
    private NotificationHelper notificationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notificationHelper = new NotificationHelper(this);
        xtreamLoginService = new XtreamLoginService();
        fetchXtreamLoginData();

        BottomNavigationView navView = findViewById(R.id.nav_view);

        loadFragment(vodFragment);

        navView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.navigation_vod) {
                    selectedFragment = vodFragment;
                } else if (itemId == R.id.navigation_tv) {
                    selectedFragment = tvFragment;
                } else if (itemId == R.id.navigation_profile) {
                    selectedFragment = profileFragment;
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                    return true;
                }
                return false;
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    private void fetchXtreamLoginData() {
        notificationHelper.showProgressNotification("Conectando", "Conectando ao Xtream Codes...", 0, 0, true);
        xtreamLoginService.getLoginData(new XtreamLoginService.LoginCallback() {
            @Override
            public void onSuccess(XtreamLoginService.XtreamAccount account) {
                Log.d("MainActivity", "Login data received: " + account.server + ", " + account.username + ", " + account.password);
                xtreamApiService = new XtreamApiService(account.server, account.username, account.password);
                notificationHelper.showCompletionNotification("Conectado", "Conexão com Xtream Codes estabelecida.");
                // Removed direct call to fetchVodAndChannelData() here
                // as VodFragment now handles its own data loading on creation/resume.
                // This prevents redundant API calls and ensures data is loaded when the fragment is ready.
            }

            @Override
            public void onFailure(String error) {
                Log.e("MainActivity", "Failed to get login data: " + error);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erro ao obter dados de login: " + error, Toast.LENGTH_LONG).show());
                notificationHelper.showCompletionNotification("Erro de Conexão", "Falha ao conectar ao Xtream Codes.");
            }
        });
    }

    // Removed fetchVodAndChannelData() method entirely as data fetching is now handled within VodFragment and TvFragment.
    // This simplifies MainActivity and delegates data loading responsibilities to the respective fragments.
}


