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


    // Helper class for Sidenav items (pode ser movida para TvSideNavAdapter ou ficar aqui)
    public static class NavItem { // Tornar public static para ser acessível pelo Adapter se ele for classe separada
        final String id;
        @DrawableRes
        final int iconResId;
        final Fragment fragment;
        // boolean isSelected; // O adapter pode gerenciar isso

        NavItem(String id, @DrawableRes int iconResId, Fragment fragment) {
            this.id = id;
            this.iconResId = iconResId;
            this.fragment = fragment;
            // this.isSelected = false;
        }
    }

    private List<NavItem> tvNavItemsList; // Renomeado para clareza

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView will be called in initializeApp or showLayoutChooserDialog

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean layoutChosen = prefs.getBoolean(KEY_LAYOUT_CHOSEN, false);

        if (!layoutChosen) {
            showLayoutChooserDialog();
        } else {
            // Layout já escolhido, prosseguir com a inicialização normal
            initializeApp();
        }
    }

    private void showLayoutChooserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_layout_chooser, null);
        builder.setView(dialogView);
        builder.setCancelable(false); // Impede o usuário de fechar o diálogo sem escolher

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
                new NavItem("VOD", R.drawable.ic_home_black_24dp, vodFragment), // Placeholder for VodFragmentTv
                new NavItem("TV", R.drawable.ic_dashboard_black_24dp, tvFragment), // Placeholder for TvFragmentTv
                new NavItem("Profile", R.drawable.ic_notifications_black_24dp, profileFragment) // Placeholder for ProfileFragmentTv
        );

        // Interface para o adapter notificar cliques
        TvSideNavAdapter.OnNavItemClickListener listener = navItem -> {
            // A view selecionada será gerenciada pelo adapter ou podemos passar o item clicado
            // Para simplificar, o adapter pode gerenciar a seleção visual e apenas nos dar o fragmento.
            // Ou, o adapter nos dá a view e o fragmento.
            // Por agora, apenas carregamos o fragmento. A seleção visual será tratada no loadTvFragment.
             View itemView = findViewByNavItemId(navItem.id); // Precisamos de uma forma de obter a view
             loadTvFragment(navItem.fragment, itemView); // Passar a view para gerenciar seleção
        };

        // Assumindo que TvSideNavAdapter existe e aceita List<NavItem> e um listener
        // Se TvSideNavAdapter não existir, esta linha causará erro de compilação.
        // O erro original era sobre IDs DENTRO do adapter, então ele deve existir.
        tvSideNavAdapter = new TvSideNavAdapter(tvNavItemsList, listener);
        sideNavRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        sideNavRecyclerView.setAdapter(tvSideNavAdapter);

        // Load initial TV fragment
        if (!tvNavItemsList.isEmpty()) {
            // Precisamos de uma forma de obter a View do primeiro item para passar para loadTvFragment
            // Isso é complicado sem ter o adapter gerenciando a seleção ou um callback melhor.
            // Solução temporária: carregar o fragmento, a seleção visual pode não funcionar perfeitamente no início.
            loadTvFragment(tvNavItemsList.get(0).fragment, null); // Passar null para selectedView inicialmente
            // Idealmente, o adapter chamaria o listener para o primeiro item ou teríamos um método selectItem(position)
        }
    }

    private View findViewByNavItemId(String navItemId) {
        if (sideNavRecyclerView == null || tvSideNavAdapter == null) return null;
        for (int i = 0; i < tvNavItemsList.size(); i++) {
            if (tvNavItemsList.get(i).id.equals(navItemId)) {
                RecyclerView.ViewHolder holder = sideNavRecyclerView.findViewHolderForAdapterPosition(i);
                return holder != null ? holder.itemView : null;
            }
        }
        return null;
    }


    private void loadTvFragment(Fragment fragment, @Nullable View selectedItemView) {
        Log.d("MainActivity", "Loading TV Fragment: " + fragment.getClass().getSimpleName());
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.tv_fragment_container, fragment);
        fragmentTransaction.commit();

        // Update Sidenav item selection state (visual)
        // Esta lógica é melhor dentro do Adapter ou via um método no adapter para definir o item selecionado
        if (lastSelectedSidenavItemView != null) {
            lastSelectedSidenavItemView.setSelected(false);
        }
        if (selectedItemView != null) {
            selectedItemView.setSelected(true);
            lastSelectedSidenavItemView = selectedItemView;
        } else {
            // Se selectedItemView for null (ex: carregamento inicial sem view específica),
            // podemos tentar encontrar a view correspondente ao fragmento ou deixar sem seleção visual inicial.
            // Por enquanto, apenas resetamos.
            lastSelectedSidenavItemView = null;
        }

        // O adapter deve atualizar seus próprios itens se ele mantiver o estado de seleção.
        // tvSideNavAdapter.setSelectedItem(fragment); // Exemplo se o adapter tivesse tal método

        // Basic focus request. More specific focus might be needed within each TV fragment.
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
