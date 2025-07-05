package com.example.iptvplayer;

import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import xyz.doikki.videoplayer.player.VideoView;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Icon;
import java.util.ArrayList;
import android.view.View;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import com.example.iptvplayer.component.CompleteView;
import com.example.iptvplayer.component.ErrorView;
import com.example.iptvplayer.component.GestureView;
import com.example.iptvplayer.component.PrepareView;
import com.example.iptvplayer.component.TitleView;
import com.example.iptvplayer.component.VodControlView;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import android.util.Log; // Movido para o topo
import android.widget.Toast;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import android.graphics.Color;
import android.util.Rational;
import android.widget.TextView;

import com.lxj.xpopup.XPopup;

import java.util.List;

import com.example.iptvplayer.data.Movie;
import com.example.iptvplayer.data.Channel;
import android.content.SharedPreferences;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ImageView;
import java.util.Arrays;
import java.util.List;
import android.content.pm.ActivityInfo;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;


public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_LAYOUT_CHOSEN = "LayoutChosen";
    private static final String KEY_SELECTED_LAYOUT = "SelectedLayout";
    private static final String LAYOUT_MOBILE = "Mobile";
    private static final String LAYOUT_TV = "TV";

    private final VodFragment vodFragment = new VodFragment();
    private final TvFragment tvFragment = new TvFragment();
    private final ProfileFragment profileFragment = new ProfileFragment();
    // private XtreamLoginService xtreamLoginService; // Removed
    // private XtreamApiService xtreamApiService; // Removed
    // private NotificationHelper notificationHelper; // Removed if only for login progress

    private static final String TAG_BACK_MAIN = "MainActivity_Back";
    // private LinearLayout sidenavContainer; // Agora é RecyclerView
    private RecyclerView sideNavRecyclerView;
    private View lastSelectedSidenavItemView = null; // Para o adapter gerenciar seleção visualmente se necessário
    private TvSideNavAdapter tvSideNavAdapter;


    

    private List<TvSideNavItem> tvNavItemsList; // Renomeado para clareza

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView will be called in initializeApp

        // DeviceTypeActivity é a entrada e já define o tipo de dispositivo para "mobile"
        // em "device_type_prefs".
        // MainActivity precisa respeitar isso e configurar suas próprias preferências
        // ("AppPrefs") se for a primeira vez.

        SharedPreferences appPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean mainActivityLayoutChosen = appPrefs.getBoolean(KEY_LAYOUT_CHOSEN, false);

        if (!mainActivityLayoutChosen) {
            // É a primeira vez que MainActivity está configurando o layout.
            // Leia o tipo de dispositivo definido por DeviceTypeActivity (que é sempre "mobile" agora)
            String deviceTypeFromInitialActivity = DeviceTypeActivity.getDeviceType(this); // Retorna "mobile"

            // Converta para o formato que MainActivity espera ("Mobile" ou "TV")
            String layoutToSet = DeviceTypeActivity.DEVICE_TYPE_MOBILE.equalsIgnoreCase(deviceTypeFromInitialActivity) ? LAYOUT_MOBILE : LAYOUT_TV;

            saveLayoutPreference(layoutToSet); // Salva em "AppPrefs" e define KEY_LAYOUT_CHOSEN = true
            Log.d("MainActivity", "Layout não escolhido anteriormente em AppPrefs. Definindo para: " + layoutToSet + " baseado na DeviceTypeActivity.");
        }

        // Agora KEY_LAYOUT_CHOSEN em AppPrefs será true, e KEY_SELECTED_LAYOUT terá o valor correto.
        initializeApp();
    }

    // O método showLayoutChooserDialog() não é mais chamado, mas pode ser mantido se houver planos futuros de reintroduzi-lo.
    // Por enquanto, para garantir que não seja usado, podemos comentá-lo ou remover.
    // Vamos remover para limpeza, já que a tarefa é ocultá-lo permanentemente.
    /*
    private void showLayoutChooserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_layout_chooser, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();

        Button mobileButton = dialogView.findViewById(R.id.button_mobile_layout);
        Button tvButton = dialogView.findViewById(R.id.button_tv_layout);

        mobileButton.setOnClickListener(v -> {
            saveLayoutPreference(LAYOUT_MOBILE);
            dialog.dismiss();
            initializeApp();
        });

        tvButton.setOnClickListener(v -> {
            saveLayoutPreference(LAYOUT_TV);
            dialog.dismiss();
            initializeApp();
        });

        dialog.show();
    }
    */

    private void saveLayoutPreference(String layoutType) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_LAYOUT_CHOSEN, true);
        editor.putString(KEY_SELECTED_LAYOUT, layoutType);
        editor.apply();
        Log.d("MainActivity", "Layout preference saved: " + layoutType);
    }

    private void initializeApp() {
        // Check if data was loaded by DownloadProgressActivity
        boolean dataLoadedSuccessfully = getIntent().getBooleanExtra("DATA_LOADED_SUCCESSFULLY", false);
        DataManager dataManager = MyApplication.getDataManager(getApplicationContext()); // Get singleton instance

        // If data is not loaded (e.g. app started directly) or DataManager is not yet complete,
        // redirect to DownloadProgressActivity.
        if (!dataLoadedSuccessfully && (dataManager.getLiveStreams() == null || dataManager.getVodStreams() == null)) {
            Log.d("MainActivity", "Data not loaded, redirecting to DownloadProgressActivity.");
            Intent intent = new Intent(this, DownloadProgressActivity.class);
            startActivity(intent);
            finish(); // Finish MainActivity to prevent user from seeing it without data
            return;   // Stop further execution of onCreate
        }

        Log.d("MainActivity", "Data loaded or already available. Proceeding with MainActivity setup.");

        // Aqui você pode adicionar lógica para carregar diferentes UIs ou comportamentos
        // com base em getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_SELECTED_LAYOUT, LAYOUT_MOBILE)
        Log.d("MainActivity", "Selected layout: " + getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_SELECTED_LAYOUT, LAYOUT_MOBILE));
        String selectedLayout = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_SELECTED_LAYOUT, LAYOUT_MOBILE);

        if (LAYOUT_TV.equals(selectedLayout)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setContentView(R.layout.activity_main_tv);
            Log.d("MainActivity", "TV Layout selected. Inflated activity_main_tv.xml. Orientation set to Landscape.");
            setupTvSidenav();
        } else { // Mobile layout
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            setContentView(R.layout.activity_main);
            Log.d("MainActivity", "Mobile Layout selected. Inflated activity_main.xml. Orientation set to Unspecified.");
            BottomNavigationView navView = findViewById(R.id.nav_view);
            // navView.setVisibility(View.VISIBLE); // Default in XML

            loadFragment(vodFragment); // Load initial mobile fragment

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
    }

    // Placeholder for setupTvSidenav, loadTvFragment, loadFragment, onBackPressed
    // These will be added in subsequent steps. For now, ensure initializeApp structure is correct.

    private void setupTvSidenav() {
        sideNavRecyclerView = findViewById(R.id.side_nav_recycler);
        if (sideNavRecyclerView == null) {
            Log.e("MainActivity", "Sidenav RecyclerView (R.id.side_nav_recycler) not found in activity_main_tv.xml");
            return;
        }

        tvNavItemsList = Arrays.asList(
                new TvSideNavItem("VOD", R.drawable.ic_home_black_24dp), // Placeholder for VodFragmentTv
                new TvSideNavItem("TV", R.drawable.ic_dashboard_black_24dp), // Placeholder for TvFragmentTv
                new TvSideNavItem("Profile", R.drawable.ic_notifications_black_24dp) // Placeholder for ProfileFragmentTv
        );

        // Interface para o adapter notificar cliques
        TvSideNavAdapter.OnNavItemClickListener listener = position -> {
            // A view selecionada será gerenciada pelo adapter ou podemos passar o item clicado
            // Para simplificar, o adapter pode gerenciar a seleção visual e apenas nos dar o fragmento.
            // Ou, o adapter nos dá a view e o fragmento.
            // Por agora, apenas carregamos o fragmento. A seleção visual será tratada no loadTvFragment.
             // View itemView = findViewByNavItemId(navItem.getTitle()); // Precisamos de uma forma de obter a view
             // loadTvFragment(navItem.fragment, itemView); // Passar a view para gerenciar seleção
             // The fragment is not part of TvSideNavItem, so we need to map it based on position
             Fragment selectedFragment = null;
             switch (position) {
                 case 0: selectedFragment = vodFragment; break;
                 case 1: selectedFragment = tvFragment; break;
                 case 2: selectedFragment = profileFragment; break;
             }
             if (selectedFragment != null) {
                 loadTvFragment(selectedFragment, null); // Pass null for itemView for now
             }
        };

        // Assumindo que TvSideNavAdapter existe e aceita List<NavItem> e um listener
        // Se TvSideNavAdapter não existir, esta linha causará erro de compilação.
        // O erro original era sobre IDs DENTRO do adapter, então ele deve existir.
        tvSideNavAdapter = new TvSideNavAdapter(tvNavItemsList, listener);
        sideNavRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        sideNavRecyclerView.setAdapter(tvSideNavAdapter);

        // Load initial TV fragment
        if (tvSideNavAdapter != null && !tvNavItemsList.isEmpty()) {
            // Simula o clique no primeiro item para carregar o fragmento inicial
            // e garantir que o estado de seleção do adapter seja atualizado.
            // O listener do adapter chamará loadTvFragment.
            listener.onNavItemClicked(0);

            // Opcional: Forçar foco no primeiro item visível do RecyclerView após a configuração
            // sideNavRecyclerView.post(() -> {
            //    RecyclerView.ViewHolder firstViewHolder = sideNavRecyclerView.findViewHolderForAdapterPosition(0);
            //    if (firstViewHolder != null && firstViewHolder.itemView != null) {
            //        firstViewHolder.itemView.requestFocus();
            //    }
            // });
        }
    }

    private View findViewByNavItemId(String navItemId) {
        // This method is no longer needed as we are passing position directly
        return null;
    }


    private void loadTvFragment(Fragment fragment, @Nullable View selectedItemView) { // selectedItemView pode ser usado para focar
        Log.d("MainActivity", "Loading TV Fragment: " + fragment.getClass().getSimpleName());
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.tv_fragment_container, fragment);
        fragmentTransaction.commit();

        // A seleção visual agora é primariamente gerenciada pelo TvSideNavAdapter.
        // A Activity apenas precisa garantir que o fragmento correto seja carregado.
        // Se precisarmos focar na view do item da Sidenav após o clique:
        if (selectedItemView != null) {
            // selectedItemView.requestFocus(); // O adapter já faz isso no onClick
        }

        // Basic focus request para o novo fragmento.
        // Delay focus request slightly to ensure fragment view is fully ready.
        if (fragment.getView() != null) {
             fragment.getView().post(() -> {
                if (fragment.getView() != null) { // Check again as view might be destroyed
                    fragment.getView().requestFocus();
                }
            });
        } else {
            // If view is null, try to request focus once it's available
            fragmentManager.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull View v, @Nullable Bundle savedInstanceState) {
                    super.onFragmentViewCreated(fm, f, v, savedInstanceState);
                    if (f == fragment) { // Only for the target fragment
                        v.requestFocus();
                        fm.unregisterFragmentLifecycleCallbacks(this); // Unregister self
                    }
                }
            }, false);
        }
    }

    private void loadFragment(Fragment fragment) {
        // This is for mobile layout's R.id.fragment_container
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment); // R.id.fragment_container is from activity_main.xml
        fragmentTransaction.commit();
    }

    // Removed fetchXtreamLoginData() method as DataManager now handles this.

    @Override
    public void onBackPressed() {
        Log.d(TAG_BACK_MAIN, "onBackPressed called.");
        String selectedLayout = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_SELECTED_LAYOUT, LAYOUT_MOBILE);
        int containerId = LAYOUT_TV.equals(selectedLayout) ? R.id.tv_fragment_container : R.id.fragment_container;

        FragmentManager fm = getSupportFragmentManager();
        Fragment currentFragment = fm.findFragmentById(containerId);
        Log.d(TAG_BACK_MAIN, "Current fragment: " + (currentFragment != null ? currentFragment.getClass().getName() : "null") + " in container " + containerId);

        if (currentFragment instanceof TvFragment) { // This check might need to be more generic for TV fragments
            Log.d(TAG_BACK_MAIN, "CurrentFragment is TvFragment. Calling its onBackPressed...");
            if (((TvFragment) currentFragment).onBackPressed()) {
                Log.d(TAG_BACK_MAIN, "TvFragment.onBackPressed() returned true. Event consumed by fragment.");
                return;
            } else {
                Log.d(TAG_BACK_MAIN, "TvFragment.onBackPressed() returned false. Event NOT consumed by fragment.");
            }
        } else {
            Log.d(TAG_BACK_MAIN, "CurrentFragment is not TvFragment or is null.");
        }

        Log.d(TAG_BACK_MAIN, "Calling super.onBackPressed().");
        super.onBackPressed();
    }
}
