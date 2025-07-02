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

        // Load the default fragment
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
                fetchVodAndChannelData();
            }

            @Override
            public void onFailure(String error) {
                Log.e("MainActivity", "Failed to get login data: " + error);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erro ao obter dados de login: " + error, Toast.LENGTH_LONG).show());
                notificationHelper.showCompletionNotification("Erro de Conexão", "Falha ao conectar ao Xtream Codes.");
            }
        });
    }

    private void fetchVodAndChannelData() {
        // Fetch VOD streams
        notificationHelper.showProgressNotification("Baixando Filmes", "Baixando dados de filmes...", 0, 0, true);
        xtreamApiService.fetchVodStreams(new XtreamApiService.XtreamApiCallback<Movie>() {
            @Override
            public void onSuccess(List<Movie> movies) {
                Log.d("MainActivity", "VOD streams fetched: " + movies.size());
                notificationHelper.showCompletionNotification("Filmes Concluídos", "Dados de filmes baixados com sucesso!");
                vodFragment.updateMovies(movies);
            }

            @Override
            public void onFailure(String error) {
                Log.e("MainActivity", "Failed to fetch VOD streams: " + error);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erro ao obter filmes: " + error, Toast.LENGTH_LONG).show());
                notificationHelper.showCompletionNotification("Erro Filmes", "Falha ao baixar dados de filmes.");
            }
        });

        // Fetch Live streams
        notificationHelper.showProgressNotification("Baixando Canais", "Baixando dados de canais...", 0, 0, true);
        xtreamApiService.fetchLiveStreams(new XtreamApiService.XtreamApiCallback<Channel>() {
            @Override
            public void onSuccess(List<Channel> channels) {
                Log.d("MainActivity", "Live streams fetched: " + channels.size());
                notificationHelper.showCompletionNotification("Canais Concluídos", "Dados de canais baixados com sucesso!");
                tvFragment.updateChannels(channels);
            }

            @Override
            public void onFailure(String error) {
                Log.e("MainActivity", "Failed to fetch Live streams: " + error);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erro ao obter canais: " + error, Toast.LENGTH_LONG).show());
                notificationHelper.showCompletionNotification("Erro Canais", "Falha ao baixar dados de canais.");
            }
        });
    }
}


