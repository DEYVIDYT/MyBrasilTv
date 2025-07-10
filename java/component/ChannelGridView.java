package com.example.iptvplayer.component;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log; // Mover import para o topo
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptvplayer.R;
import com.example.iptvplayer.adapter.ChannelGridAdapter;
import com.example.iptvplayer.adapter.ChannelGridCategoryAdapter;
import com.example.iptvplayer.data.Channel;
import com.example.iptvplayer.SideNavToggleListener; // Importar a interface refatorada
import xyz.doikki.videoplayer.controller.IControlComponent;
import xyz.doikki.videoplayer.controller.ControlWrapper;
import xyz.doikki.videoplayer.player.VideoView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Channel grid overlay component for fullscreen mode
 */
public class ChannelGridView extends FrameLayout implements IControlComponent, View.OnClickListener {

    private ControlWrapper mControlWrapper;
    private FrameLayout mChannelGridOverlay;
    private RecyclerView mRecyclerCategories;
    private RecyclerView mRecyclerChannels;
    private TextView mCategoryTitle;
    
    private ChannelGridCategoryAdapter mCategoryAdapter;
    private ChannelGridAdapter mChannelAdapter;
    
    private List<Channel> mAllChannels = new ArrayList<>();
    private Map<String, String> mCategoryMap;
    private String mCurrentCategoryId = "0";
    
    private OnChannelSelectedListener mChannelSelectedListener;
    private SideNavToggleListener sideNavToggleListener;
    private static final String TAG = "ChannelGridView_Debug";

    public interface OnChannelSelectedListener {
        void onChannelSelected(Channel channel);
    }

    public ChannelGridView(@NonNull Context context) {
        super(context);
        Log.d(TAG, "Constructor ChannelGridView(Context) called");
        init();
    }

    public ChannelGridView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "Constructor ChannelGridView(Context, Attrs) called");
        init();
    }

    public ChannelGridView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.d(TAG, "Constructor ChannelGridView(Context, Attrs, DefStyle) called");
        init();
    }

    private void init() {
        Log.d(TAG, "init() called");
        setVisibility(GONE);
        LayoutInflater.from(getContext()).inflate(R.layout.dkplayer_layout_channel_grid, this, true);
        
        mChannelGridOverlay = findViewById(R.id.channel_grid_overlay);
        mRecyclerCategories = findViewById(R.id.recycler_categories_grid);
        mRecyclerChannels = findViewById(R.id.recycler_channels_grid);
        mCategoryTitle = findViewById(R.id.tv_category_title);
        
        // Setup RecyclerViews
        mRecyclerCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerChannels.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Setup click listener for overlay to close grid
        if (mChannelGridOverlay != null) {
            mChannelGridOverlay.setOnClickListener(this);
        }
        
        View contentAreaView = findViewById(R.id.channel_grid_content_area);
        if (contentAreaView != null) {
            contentAreaView.setOnClickListener(v -> {
                // Consome o clique na área de conteúdo para não fechar a grade
            });
        }

        // Adicionar listeners de tecla para navegação D-Pad entre as listas
        setupDpadNavigation();
    }

    private void setupDpadNavigation() {
        Log.d(TAG, "setupDpadNavigation() called");
        mRecyclerCategories.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "mRecyclerCategories onKey: keyCode=" + android.view.KeyEvent.keyCodeToString(keyCode));
                if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
                    Log.d(TAG, "DPAD_RIGHT on Categories. ChildCount Channels: " + mRecyclerChannels.getChildCount());
                    if (mRecyclerChannels.getChildCount() > 0) {
                        Log.i(TAG, "Requesting focus for Channels RecyclerView.");
                        mRecyclerChannels.requestFocus();
                        return true;
                    }
                } else if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) {
                    Log.d(TAG, "DPAD_LEFT on Categories. sideNavToggleListener is " + (sideNavToggleListener == null ? "null" : "not null"));
                    if (sideNavToggleListener != null) {
                        Log.d(TAG, "Sidenav is currently " + (sideNavToggleListener.isSideNavVisible() ? "visible" : "hidden"));
                        if (!sideNavToggleListener.isSideNavVisible()) {
                            Log.i(TAG, "DPAD_LEFT on Categories: Requesting show Sidenav from Activity.");
                            sideNavToggleListener.requestShowSideNav();
                            return true;
                        }
                    }
                }
            }
            return false;
        });

        mRecyclerChannels.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "mRecyclerChannels onKey: keyCode=" + android.view.KeyEvent.keyCodeToString(keyCode));
                if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) {
                    LinearLayoutManager lm = (LinearLayoutManager) mRecyclerChannels.getLayoutManager();
                    if (lm != null) {
                        // Simplificando: qualquer D-PAD Esquerda nos canais tenta mover para categorias
                        Log.i(TAG, "DPAD_LEFT on Channels. Requesting focus for Categories RecyclerView.");
                        mRecyclerCategories.requestFocus();
                        return true;
                    }
                }
            }
            return false;
        });
    }

    @Override
    public void attach(@NonNull ControlWrapper controlWrapper) {
        mControlWrapper = controlWrapper;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void onVisibilityChanged(boolean isVisible, Animation anim) {
        // This component manages its own visibility
    }

    @Override
    public void onPlayStateChanged(int playState) {
        // Hide grid if player stops or has error
        if (playState == VideoView.STATE_IDLE
                || playState == VideoView.STATE_ERROR
                || playState == VideoView.STATE_PLAYBACK_COMPLETED) {
            hideChannelGrid();
        }
    }

    @Override
    public void onPlayerStateChanged(int playerState) {
        // Only show in fullscreen mode
        if (playerState != VideoView.PLAYER_FULL_SCREEN) {
            hideChannelGrid();
        }
    }

    @Override
    public void setProgress(int duration, int position) {
        // Not needed for this component
    }

    @Override
    public void onLockStateChanged(boolean isLocked) {
        // Hide grid when locked
        if (isLocked) {
            hideChannelGrid();
        }
    }

    @Override
    public void onClick(View v) {
        // Este onClick é chamado pelo mChannelGridOverlay (o FrameLayout raiz).
        // Se o clique foi na área de conteúdo (channel_grid_content_area), ele já foi
        // consumido pelo OnClickListener de contentAreaView e não deveria chegar aqui.
        // Portanto, qualquer clique que chega aqui é considerado um clique "fora" da área de conteúdo.
        Log.d(TAG, "onClick on ChannelGridView (mChannelGridOverlay) triggered. View ID: " + v.getId() + " (hex: " + Integer.toHexString(v.getId()) + ")");
        if (v.getId() == R.id.channel_grid_overlay) { // Verifica se o clique foi no próprio overlay/fundo
            Log.d(TAG, "Clicked on the overlay background (ID: channel_grid_overlay). Hiding channel grid.");
            hideChannelGrid();
        } else {
            // Este caso não deveria acontecer se o único listener que chama este método
            // é o do mChannelGridOverlay. Mas é bom ter um log.
            Log.d(TAG, "Clicked on a different view within ChannelGridView that wasn\'t consumed? View ID: " + v.getId());
        }
    }
    public void setChannelSelectedListener(OnChannelSelectedListener listener) {
        mChannelSelectedListener = listener;
        Log.d(TAG, "setChannelSelectedListener " + (listener == null ? "cleared" : "set"));
    }

    public void setSideNavToggleListener(SideNavToggleListener listener) {
        this.sideNavToggleListener = listener;
        Log.d(TAG, "setSideNavToggleListener " + (listener == null ? "cleared" : "set"));
    }

    public void setChannelsData(List<Channel> channels, Map<String, String> categoryMap) {
        Log.d(TAG, "setChannelsData called. Channels: " + (channels != null ? channels.size() : "null") + ", Categories: " + (categoryMap != null ? categoryMap.size() : "null"));
        mAllChannels.clear();
        if (channels != null) {
            mAllChannels.addAll(channels);
        }
        mCategoryMap = categoryMap;
        
        setupAdapters();
    }

    private void setupAdapters() {
        Log.d(TAG, "setupAdapters() called");
        // Setup category adapter
        List<CategoryItem> categoryItems = new ArrayList<>();
        categoryItems.add(new CategoryItem("0", "TODOS")); // "Todos" category
        
        if (mCategoryMap != null) {
            for (Map.Entry<String, String> entry : mCategoryMap.entrySet()) {
                categoryItems.add(new CategoryItem(entry.getKey(), entry.getValue().toUpperCase()));
            }
        }
        Log.d(TAG, "Category items count: " + categoryItems.size());
        
        mCategoryAdapter = new ChannelGridCategoryAdapter(getContext(), categoryItems, this::onCategorySelected);
        mRecyclerCategories.setAdapter(mCategoryAdapter);
        
        // Setup channel adapter
        mChannelAdapter = new ChannelGridAdapter(getContext(), getChannelsForCategory(mCurrentCategoryId), this::onChannelSelected);
        mRecyclerChannels.setAdapter(mChannelAdapter);
        
        updateCategoryTitle();
        Log.d(TAG, "Adapters setup complete.");
    }

    private void onCategorySelected(String categoryId) {
        Log.i(TAG, "onCategorySelected: categoryId=" + categoryId);
        mCurrentCategoryId = categoryId;
        List<Channel> filteredChannels = getChannelsForCategory(categoryId);
        Log.d(TAG, "Filtered channels count: " + filteredChannels.size());

        if (mChannelAdapter != null) {
            mChannelAdapter.updateChannels(filteredChannels);
        } else {
            Log.e(TAG, "mChannelAdapter is null in onCategorySelected!");
        }

        updateCategoryTitle();

        if (mCategoryAdapter != null) {
            mCategoryAdapter.setSelectedCategory(categoryId);
        } else {
            Log.e(TAG, "mCategoryAdapter is null in onCategorySelected (for setSelectedCategory)!");
        }
    }

    private void onChannelSelected(Channel channel) {
        Log.i(TAG, "onChannelSelected: channel=" + channel.getName());
        hideChannelGrid(); // Esconde a grade ao selecionar um canal
        if (mChannelSelectedListener != null) {
            mChannelSelectedListener.onChannelSelected(channel);
        }
    }

    private List<Channel> getChannelsForCategory(String categoryId) {
        Log.d(TAG, "getChannelsForCategory: categoryId=" + categoryId);
        List<Channel> filteredChannels = new ArrayList<>();
        
        if ("0".equals(categoryId)) {
            // Show all channels
            filteredChannels.addAll(mAllChannels);
        } else {
            // Filter by category
            for (Channel channel : mAllChannels) {
                if (categoryId.equals(channel.getCategoryId())) {
                    filteredChannels.add(channel);
                }
            }
        }
        
        return filteredChannels;
    }

    private void updateCategoryTitle() {
        if (mCategoryTitle != null) {
            String title = "TODOS";
            if (mCategoryMap != null && mCategoryMap.containsKey(mCurrentCategoryId)) {
                title = mCategoryMap.get(mCurrentCategoryId).toUpperCase();
            }
            mCategoryTitle.setText(title);
        }
    }

    public void showChannelGrid() {
        Log.i(TAG, "showChannelGrid() called. Current visibility: " + getVisibility());
        if (mControlWrapper != null && mControlWrapper.isFullScreen()) {
            setVisibility(VISIBLE);
            mChannelGridOverlay.setVisibility(VISIBLE);
            Log.d(TAG, "ChannelGridView and Overlay set to VISIBLE.");
            
            if (mControlWrapper != null) {
                mControlWrapper.hide();
            }

            if (mRecyclerCategories != null) {
                Log.i(TAG, "Requesting focus for categories RecyclerView.");
                mRecyclerCategories.requestFocus();
            }
        } else {
            Log.w(TAG, "showChannelGrid: Conditions not met (mControlWrapper=" + mControlWrapper +
                       (mControlWrapper != null ? ", isFullScreen=" + mControlWrapper.isFullScreen() : "") + ")");
        }
    }

    public void hideChannelGrid() {
        Log.i(TAG, "hideChannelGrid() called. Current visibility: " + getVisibility());
        setVisibility(GONE);
        mChannelGridOverlay.setVisibility(GONE);
        Log.d(TAG, "ChannelGridView and Overlay set to GONE.");
    }

    public boolean isChannelGridVisible() {
        boolean isVisible = getVisibility() == VISIBLE && mChannelGridOverlay.getVisibility() == VISIBLE;
        Log.d(TAG, "isChannelGridVisible() called, returning: " + isVisible);
        return isVisible;
    }

    // Helper class for category items
    public static class CategoryItem {
        public final String id;
        public final String name;

        public CategoryItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
