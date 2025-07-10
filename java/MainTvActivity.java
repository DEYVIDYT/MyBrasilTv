package com.example.iptvplayer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull; // Adicionada para onAttachFragment
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

// A interface SideNavToggleListener foi movida para seu próprio arquivo: SideNavToggleListener.java

public class MainTvActivity extends AppCompatActivity implements TvKeyHandler.TvKeyListener, SideNavToggleListener {
    // A implementação da interface agora usa o nome simples "SideNavToggleListener"

    private static final String TAG = "MainTvActivity";
    
    private RecyclerView sideNavRecyclerView;
    private TvSideNavAdapter sideNavAdapter;
    private View currentFocusedView;
    private int currentSideNavPosition = 0;
    private boolean isSideNavCurrentlyVisible = true; // Default: Sidenav está visível
    private static final long SIDENAV_ANIMATION_DURATION = 250; // Duração da animação em ms
    
    private final VodFragment vodFragment = new VodFragment();
    // private final TvFragment tvFragment = new TvFragment(); // Mobile version, not needed here
    private final ProfileFragment profileFragment = new ProfileFragment();
    private final TvFragmentTv tvFragmentTv = new TvFragmentTv(); // TV version for live channels
    
    private FrameLayout tvFragmentContainer; // Adicionado para referência ao container do fragmento
    private TextView bannerTitle;
    private TextView bannerInfo;
    private TextView bannerDescription;
    
    // O primeiro método onCreate foi removido por ser uma duplicata.
    // A versão correta de onCreate está mais abaixo e inclui a chamada para setupFocusListeners().

    private void initializeViews() {
        sideNavRecyclerView = findViewById(R.id.side_nav_recycler);
        tvFragmentContainer = findViewById(R.id.tv_fragment_container); // Adicionado para garantir que está aqui
        bannerTitle = findViewById(R.id.banner_title);
        bannerInfo = findViewById(R.id.banner_info);
        bannerDescription = findViewById(R.id.banner_description);
    }
    
    private void setupSideNavigation() {
        List<TvSideNavItem> navItems = new ArrayList<>();
        navItems.add(new TvSideNavItem("Search", R.drawable.ic_search)); // Position 0
        navItems.add(new TvSideNavItem("Home (VOD)", R.drawable.ic_home_black_24dp)); // Position 1
        navItems.add(new TvSideNavItem("Live TV", R.drawable.ic_tv)); // Position 2 - New Item for Live TV
        navItems.add(new TvSideNavItem("Favorites", R.drawable.ic_favorite_border)); // Position 3
        navItems.add(new TvSideNavItem("Downloads", R.drawable.ic_dashboard_black_24dp)); // Position 4
        navItems.add(new TvSideNavItem("Settings", R.drawable.ic_help_outline)); // Position 5
        navItems.add(new TvSideNavItem("Profile", R.drawable.ic_notifications_black_24dp)); // Position 6
        
        sideNavAdapter = new TvSideNavAdapter(navItems, new TvSideNavAdapter.OnNavItemClickListener() {
            @Override
            public void onNavItemClicked(int position) {
                handleSideNavClick(position);
            }
        });
        
        sideNavRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        sideNavRecyclerView.setAdapter(sideNavAdapter);
        
        // Definir foco inicial
        sideNavRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                View firstItem = sideNavRecyclerView.getChildAt(0);
                if (firstItem != null) {
                    firstItem.requestFocus();
                }
            }
        });
    }
    
    private void handleSideNavClick(int position) {
        currentSideNavPosition = position;
        Fragment selectedFragment = null;
        
        switch (position) {
            case 0: // Search
                Intent searchIntent = new Intent(this, SearchActivity.class);
                startActivity(searchIntent);
                break;
            case 1: // Home (VOD)
                selectedFragment = vodFragment;
                updateBannerContent("Guardians of the Galaxy", "150min • 2014 • IMDb 8.3 • Action | Adventure | Comedy", 
                                   "Set to the backdrop of 'Awesome Mixtape #1,' Marvel's Guardians of the Galaxy introduces an unlikely cast of cosmic misfits who must team up to save the universe from a fanatical warrior who seeks to purge the galaxy of all emotion.");
                // Consider clearing banner or setting a generic one for TV if banner is not applicable
                break;
            case 2: // Live TV
                selectedFragment = tvFragmentTv;
                // Update banner for Live TV or hide it if not applicable
                updateBannerContent("Live TV Channels", "Select a channel to start watching.", "");
                break;
            case 3: // Favorites
                selectedFragment = vodFragment; // Por enquanto usar VOD for Favorites
                // Update banner accordingly
                break;
            case 4: // Downloads
                selectedFragment = vodFragment; // Por enquanto usar VOD for Downloads
                // Update banner accordingly
                break;
            case 5: // Settings
                selectedFragment = profileFragment;
                // Update banner accordingly or hide
                break;
            case 6: // Profile
                selectedFragment = profileFragment;
                break;
        }
        
        if (selectedFragment != null) {
            loadFragment(selectedFragment);
        }
    }
    
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.tv_fragment_container, fragment);
        fragmentTransaction.commit();
    }
    
    private void updateBannerContent(String title, String info, String description) {
        if (bannerTitle != null) {
            bannerTitle.setText(title);
        } else {
            Log.w(TAG, "bannerTitle TextView not found, cannot update banner title.");
        }
        if (bannerInfo != null) {
            bannerInfo.setText(info);
        } else {
            Log.w(TAG, "bannerInfo TextView not found, cannot update banner info.");
        }
        if (bannerDescription != null) {
            bannerDescription.setText(description);
        } else {
            Log.w(TAG, "bannerDescription TextView not found, cannot update banner description.");
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "Key pressed: " + keyCode);
        
        // Usar TvKeyHandler para processar eventos de tecla
        if (TvKeyHandler.handleKeyEvent(keyCode, event, this)) {
            return true;
        }
        
        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    public boolean onTvKeyDown(int keyCode, KeyEvent event) {
        // Mapear teclas do gamepad para controle remoto se necessário
        int mappedKeyCode = TvKeyHandler.mapGamepadToRemote(keyCode);
        
        switch (mappedKeyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                // Deixar o sistema Android lidar com a navegação por foco
                return false;
                
            case KeyEvent.KEYCODE_BACK:
                // Voltar para seleção de dispositivo ou sair do app
                // finish(); // Let's not finish the app on back, but rely on fragment's back behavior or super.onBackPressed()
                // return true;
                // The default behavior of onBackPressed should handle fragment backstack or activity finish.
                // If TvFragmentTv consumes back press, it will return true. Otherwise, super.onBackPressed() will be called.
                onBackPressed(); // Call our onBackPressed to allow fragment to handle it first
                return true; // We've handled it by calling onBackPressed
                
            case KeyEvent.KEYCODE_MENU:
                // Abrir menu ou configurações
                handleSideNavClick(5); // Settings is now at index 5
                return true;
                
            case KeyEvent.KEYCODE_SEARCH:
                // Abrir busca
                handleSideNavClick(0); // Search
                return true;
                
            // Controles de mídia
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                // Enviar para o fragmento atual se for um player
                FragmentManager fm = getSupportFragmentManager();
                Fragment currentFragment = fm.findFragmentById(R.id.tv_fragment_container);
                if (currentFragment instanceof TvFragment) {
                    // Implementar controle de play/pause no TvFragment
                    return true;
                }
                break;
                
            default:
                return false;
        }
        
        return false;
    }
    
    @Override
    public boolean onTvKeyUp(int keyCode, KeyEvent event) {
        // Processar eventos de key up se necessário
        return false;
    }
    
    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed called.");
        FragmentManager fm = getSupportFragmentManager();
        Fragment currentFragment = fm.findFragmentById(R.id.tv_fragment_container);
        
        // Check for TvFragmentTv first as it's more specific for TV UI
        if (currentFragment instanceof TvFragmentTv) {
            if (((TvFragmentTv) currentFragment).onBackPressed()) {
                Log.d(TAG, "onBackPressed handled by TvFragmentTv");
                return;
            }
        } else if (currentFragment instanceof TvFragment) { // Fallback for other TvFragment if any
            if (((TvFragment) currentFragment).onBackPressed()) {
                Log.d(TAG, "onBackPressed handled by TvFragment");
                return;
            }
        }
        // Add other fragment checks here if they need custom back press handling (e.g., VodFragment, ProfileFragmentTv)

        Log.d(TAG, "onBackPressed: no fragment handled it, calling super.onBackPressed()");
        super.onBackPressed();
    }

    // Implementação da interface SideNavToggleListener
    // A primeira ocorrência dos métodos requestHideSideNav, requestShowSideNav, isSideNavVisible foi removida.
    // A versão correta, com logs mais detalhados, está mais abaixo.

    @Override
    public void onAttachFragment(@NonNull Fragment fragment) {
        super.onAttachFragment(fragment);
        Log.d(TAG, "onAttachFragment: fragment=" + fragment.getClass().getSimpleName());
        if (fragment instanceof TvFragmentTv) {
            TvFragmentTv tvFragment = (TvFragmentTv) fragment;
            tvFragment.setSideNavToggleListener(this);
            Log.i(TAG, "SideNavToggleListener set for TvFragmentTv");
        }
    }

    // Implementação da interface SideNavToggleListener
    @Override
    public void requestHideSideNav() {
        Log.i(TAG, "requestHideSideNav called. isSideNavCurrentlyVisible=" + isSideNavCurrentlyVisible);
        if (isSideNavCurrentlyVisible && sideNavRecyclerView != null) {
            Log.d(TAG, "Animating Sidenav to hide. Current translationX: " + sideNavRecyclerView.getTranslationX());
            ObjectAnimator animator = ObjectAnimator.ofFloat(sideNavRecyclerView, "translationX", 0f, -sideNavRecyclerView.getWidth());
            animator.setDuration(SIDENAV_ANIMATION_DURATION);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    Log.d(TAG, "Hide Sidenav Animation START");
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    sideNavRecyclerView.setVisibility(View.GONE);
                    isSideNavCurrentlyVisible = false;
                    Log.d(TAG, "Hide Sidenav Animation END. Sidenav GONE, isSideNavCurrentlyVisible=false");
                    if (tvFragmentContainer != null) {
                        Log.i(TAG, "Requesting focus for tvFragmentContainer.");
                        tvFragmentContainer.requestFocus();
                    }
                }
            });
            animator.start();
        } else {
            Log.w(TAG, "requestHideSideNav: Sidenav already hidden, null, or animation conditions not met.");
        }
    }

    @Override
    public void requestShowSideNav() {
        Log.i(TAG, "requestShowSideNav called. isSideNavCurrentlyVisible=" + isSideNavCurrentlyVisible);
        if (!isSideNavCurrentlyVisible && sideNavRecyclerView != null) {
            Log.d(TAG, "Animating Sidenav to show. Current translationX: " + sideNavRecyclerView.getTranslationX());
            sideNavRecyclerView.setVisibility(View.VISIBLE);
            ObjectAnimator animator = ObjectAnimator.ofFloat(sideNavRecyclerView, "translationX", -sideNavRecyclerView.getWidth(), 0f);
            animator.setDuration(SIDENAV_ANIMATION_DURATION);
            animator.addListener(new AnimatorListenerAdapter() {
                 @Override
                public void onAnimationStart(Animator animation) {
                    Log.d(TAG, "Show Sidenav Animation START");
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    isSideNavCurrentlyVisible = true;
                    Log.d(TAG, "Show Sidenav Animation END. Sidenav VISIBLE, isSideNavCurrentlyVisible=true");
                    Log.i(TAG, "Requesting focus for sideNavRecyclerView.");
                    sideNavRecyclerView.requestFocus();
                }
            });
            animator.start();
        } else {
            Log.w(TAG, "requestShowSideNav: Sidenav already visible, null, or animation conditions not met.");
        }
    }

    @Override
    public boolean isSideNavVisible() {
        Log.d(TAG, "isSideNavVisible called, returning: " + isSideNavCurrentlyVisible);
        return isSideNavCurrentlyVisible;
    }

    private void setupFocusListeners() { // Novo método para logs de foco
        if (sideNavRecyclerView != null) {
            sideNavRecyclerView.setOnFocusChangeListener((v, hasFocus) -> {
                Log.d(TAG, "sideNavRecyclerView focus changed: " + hasFocus);
            });
        }
        if (tvFragmentContainer != null) {
            tvFragmentContainer.setOnFocusChangeListener((v, hasFocus) -> {
                Log.d(TAG, "tvFragmentContainer focus changed: " + hasFocus);
                if (hasFocus) {
                    // Tentar passar o foco para o fragmento atual se o container ganhar foco
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.tv_fragment_container);
                    if (currentFragment != null && currentFragment.getView() != null) {
                        Log.d(TAG, "tvFragmentContainer has focus, attempting to pass to current fragment's view: " + currentFragment.getClass().getSimpleName());
                        currentFragment.getView().requestFocus();
                    }
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        setContentView(R.layout.activity_main_tv);

        // ... (código existente do onCreate) ...

        initializeViews(); // tvFragmentContainer é inicializado aqui
        setupSideNavigation();
        setupFocusListeners(); // Chamar novo método

        // ... (código existente do onCreate) ...
    }
}

