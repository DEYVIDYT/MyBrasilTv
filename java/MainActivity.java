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

public class MainActivity extends AppCompatActivity {

    private final VodFragment vodFragment = new VodFragment();
    private final TvFragment tvFragment = new TvFragment();
    private final VodFragment vodFragment = new VodFragment();
    private final TvFragment tvFragment = new TvFragment();
    private final ProfileFragment profileFragment = new ProfileFragment();
    // private XtreamLoginService xtreamLoginService; // Removed
    // private XtreamApiService xtreamApiService; // Removed
    // private NotificationHelper notificationHelper; // Removed if only for login progress

    private static final String TAG_BACK_MAIN = "MainActivity_Back";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // notificationHelper = new NotificationHelper(this); // Removed
        // xtreamLoginService = new XtreamLoginService(); // Removed

        // Check if data was loaded by DownloadProgressActivity
        boolean dataLoadedSuccessfully = getIntent().getBooleanExtra("DATA_LOADED_SUCCESSFULLY", false);
        DataManager dataManager = MyApplication.getDataManager(); // Get singleton instance

        // If data is not loaded (e.g. app started directly) or DataManager is not yet complete,
        // redirect to DownloadProgressActivity.
        // This check might need to be more robust, e.g. checking specific data in DataManager.
        if (!dataLoadedSuccessfully && (dataManager.getLiveStreams() == null || dataManager.getVodStreams() == null)) {
            Log.d("MainActivity", "Data not loaded, redirecting to DownloadProgressActivity.");
            Intent intent = new Intent(this, DownloadProgressActivity.class);
            startActivity(intent);
            finish(); // Finish MainActivity to prevent user from seeing it without data
            return;   // Stop further execution of onCreate
        }

        Log.d("MainActivity", "Data loaded or already available. Proceeding with MainActivity setup.");

        BottomNavigationView navView = findViewById(R.id.nav_view);
        loadFragment(vodFragment); // Load initial fragment

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

    // Removed fetchXtreamLoginData() method as DataManager now handles this.

    @Override
    public void onBackPressed() {
        Log.d(TAG_BACK_MAIN, "onBackPressed called.");
        FragmentManager fm = getSupportFragmentManager();
        Fragment currentFragment = fm.findFragmentById(R.id.fragment_container);
        Log.d(TAG_BACK_MAIN, "Current fragment: " + (currentFragment != null ? currentFragment.getClass().getName() : "null"));

        if (currentFragment instanceof TvFragment) {
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
