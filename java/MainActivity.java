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

    // SharedPreferences AppPrefs and related keys are removed as layout choice is now handled by initial activity flow.

    private final VodFragment vodFragment = new VodFragment();
    private final TvFragment tvFragment = new TvFragment();
    private final ProfileFragment profileFragment = new ProfileFragment();

    private static final String TAG_BACK_MAIN = "MainActivity_Back";
    // TV specific Sidenav components are removed as MainActivity is now mobile-only.
    // private RecyclerView sideNavRecyclerView;
    // private TvSideNavAdapter tvSideNavAdapter;
    // private List<TvSideNavItem> tvNavItemsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // MainActivity is now only for mobile, so direct initialization.
        initializeApp();
    }

    // The showLayoutChooserDialog() and saveLayoutPreference() methods are removed.

    private void initializeApp() {
        // Check if data was loaded by DownloadProgressActivity
        boolean dataLoadedSuccessfully = getIntent().getBooleanExtra("DATA_LOADED_SUCCESSFULLY", false);
        DataManager dataManager = MyApplication.getDataManager(getApplicationContext());

        if (!dataLoadedSuccessfully && (dataManager.getLiveStreams() == null || dataManager.getVodStreams() == null)) {
            Log.d("MainActivity", "Data not loaded, redirecting to DownloadProgressActivity.");
            Intent intent = new Intent(this, DownloadProgressActivity.class);
            // No need to pass device type here, DownloadProgressActivity will handle it based on saved prefs
            startActivity(intent);
            finish();
            return;
        }

        Log.d("MainActivity", "Data loaded or already available. Setting up Mobile Layout.");

        // Setup for Mobile Layout directly
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED); // Or portrait as before
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "Mobile Layout selected. Inflated activity_main.xml.");

        BottomNavigationView navView = findViewById(R.id.nav_view);
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

    // setupTvSidenav() and loadTvFragment() are removed as they are TV-specific.

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG_BACK_MAIN, "onBackPressed called in MainActivity (Mobile).");
        // The container for mobile fragments is R.id.fragment_container
        FragmentManager fm = getSupportFragmentManager();
        Fragment currentFragment = fm.findFragmentById(R.id.fragment_container);

        Log.d(TAG_BACK_MAIN, "Current fragment: " + (currentFragment != null ? currentFragment.getClass().getName() : "null"));

        // Example: If TvFragment (mobile version) has specific back press logic
        if (currentFragment instanceof TvFragment) {
            Log.d(TAG_BACK_MAIN, "CurrentFragment is TvFragment (mobile). Calling its onBackPressed...");
            if (((TvFragment) currentFragment).onBackPressed()) {
                Log.d(TAG_BACK_MAIN, "TvFragment.onBackPressed() returned true. Event consumed.");
                return;
            } else {
                Log.d(TAG_BACK_MAIN, "TvFragment.onBackPressed() returned false.");
            }
        }
        // Add similar checks for VodFragment or ProfileFragment if they need custom back handling

        Log.d(TAG_BACK_MAIN, "Calling super.onBackPressed().");
        super.onBackPressed();
    }
}
