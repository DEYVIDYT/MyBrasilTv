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
import android.util.Log;
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
        notificationHelper.showProgressNotification(
                getString(R.string.connecting_xtream_title),
                getString(R.string.connecting_xtream_message), 0, 0, true);
        xtreamLoginService.getLoginData(new XtreamLoginService.LoginCallback() {
            @Override
            public void onSuccess(XtreamLoginService.XtreamAccount account) {
                Log.d("MainActivity", "Login data received: " + account.server + ", " + account.username + ", " + account.password);
                xtreamApiService = new XtreamApiService(account.server, account.username, account.password);
                notificationHelper.showCompletionNotification(
                        getString(R.string.connected_xtream_title),
                        getString(R.string.connected_xtream_message));
            }

            @Override
            public void onFailure(String error) {
                Log.e("MainActivity", "Failed to get login data: " + error);
                String errorMessage = getString(R.string.error_login_data_message, error);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show());
                notificationHelper.showCompletionNotification(
                        getString(R.string.error_xtream_connection_title),
                        getString(R.string.error_xtream_connection_message));
            }
        });
    }

    

    

import android.util.Log; // Ensure Log is imported

// ... other imports

public class MainActivity extends AppCompatActivity {

    // ... fields ...
    private static final String TAG_BACK_MAIN = "MainActivity_Back";

    // ... onCreate and other methods ...

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


