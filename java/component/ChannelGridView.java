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
    private SideNavToggleListener sideNavToggleListener; // Listener para interagir com a Sidenav da Activity - Tipo atualizado
    private static final String TAG = "ChannelGridView_Debug"; // Tag for logging

    public interface OnChannelSelectedListener {
        void onChannelSelected(Channel channel);
    }

    public ChannelGridView(@NonNull Context context) {
        super(context);
        init();
    }

    public ChannelGridView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChannelGridView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
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
        mRecyclerCategories.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
                    Log.d(TAG, "DPAD_RIGHT on Categories. Requesting focus for Channels.");
                    if (mRecyclerChannels.getChildCount() > 0) {
                        mRecyclerChannels.requestFocus();
                        return true;
                    }
                } else if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) {
                    // Se pressionar Esquerda na lista de categorias, tentar mostrar a Sidenav da Activity
                    if (sideNavToggleListener != null && !sideNavToggleListener.isSideNavVisible()) {
                        // Adicionar verificação se o foco está no primeiro item ou se não pode rolar mais para esquerda (opcional, mas bom)
                        // Por simplicidade, vamos assumir que qualquer DPAD_LEFT aqui pode tentar mostrar a Sidenav
                        Log.d(TAG, "DPAD_LEFT on Categories: Requesting show Sidenav from Activity.");
                        sideNavToggleListener.requestShowSideNav();
                        return true; // Evento consumido
                    }
                }
            }
            return false;
        });

        mRecyclerChannels.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) {
                    // Verifica se o item focado é o primeiro da lista (ou se a rolagem horizontal não é possível)
                    // Esta é uma verificação simples. Uma mais robusta verificaria se o LayoutManager
                    // pode rolar para a esquerda.
                    LinearLayoutManager lm = (LinearLayoutManager) mRecyclerChannels.getLayoutManager();
                    if (lm != null) {
                        int firstVisibleItemPosition = lm.findFirstCompletelyVisibleItemPosition();
                        View focusedChild = lm.getFocusedChild();
                        int focusedItemPosition = (focusedChild != null) ? lm.getPosition(focusedChild) : -1;

                        // Se o item focado é o primeiro, ou não há item focado (foco no próprio RecyclerView)
                        // ou se o item focado está na primeira "coluna" visual (para Grid Layouts, mas aqui é LinearLayout)
                        // Basicamente, se não há mais para onde ir para a esquerda DENTRO desta lista.
                        // Para LinearLayoutManager vertical, qualquer DPAD_LEFT deve mudar de coluna.
                        Log.d(TAG, "DPAD_LEFT on Channels. Requesting focus for Categories.");
                        mRecyclerCategories.requestFocus();
                        return true; // Evento consumido
                    }
                }
            }
            return false; // Evento não consumido
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
    }

    public void setSideNavToggleListener(SideNavToggleListener listener) {
        this.sideNavToggleListener = listener;
    }

    public void setChannelsData(List<Channel> channels, Map<String, String> categoryMap) {
        mAllChannels.clear();
        mAllChannels.addAll(channels);
        mCategoryMap = categoryMap;
        
        setupAdapters();
    }

    private void setupAdapters() {
        // Setup category adapter
        List<CategoryItem> categoryItems = new ArrayList<>();
        categoryItems.add(new CategoryItem("0", "TODOS"));
        
        if (mCategoryMap != null) {
            for (Map.Entry<String, String> entry : mCategoryMap.entrySet()) {
                categoryItems.add(new CategoryItem(entry.getKey(), entry.getValue().toUpperCase()));
            }
        }
        
        mCategoryAdapter = new ChannelGridCategoryAdapter(getContext(), categoryItems, this::onCategorySelected);
        mRecyclerCategories.setAdapter(mCategoryAdapter);
        
        // Setup channel adapter
        mChannelAdapter = new ChannelGridAdapter(getContext(), getChannelsForCategory(mCurrentCategoryId), this::onChannelSelected);
        mRecyclerChannels.setAdapter(mChannelAdapter);
        
        updateCategoryTitle();
    }

    private void onCategorySelected(String categoryId) {
        mCurrentCategoryId = categoryId;
        List<Channel> filteredChannels = getChannelsForCategory(categoryId);

        if (mChannelAdapter != null) {
            mChannelAdapter.updateChannels(filteredChannels);
        }

        updateCategoryTitle();

        // Update category selection
        if (mCategoryAdapter != null) {
            mCategoryAdapter.setSelectedCategory(categoryId);
        }
        // hideChannelGrid(); // REMOVIDO: Não fechar a grade ao selecionar uma categoria.
    }

    private void onChannelSelected(Channel channel) {
        hideChannelGrid();
        if (mChannelSelectedListener != null) {
            mChannelSelectedListener.onChannelSelected(channel);
        }
    }

    private List<Channel> getChannelsForCategory(String categoryId) {
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
        if (mControlWrapper != null && mControlWrapper.isFullScreen()) {
            setVisibility(VISIBLE);
            mChannelGridOverlay.setVisibility(VISIBLE);
            
            // Hide other controls
            if (mControlWrapper != null) {
                mControlWrapper.hide(); // Esconde outros componentes do player (barra de progresso, etc.)
            }

            // Solicitar foco para a lista de categorias ao mostrar a grade
            if (mRecyclerCategories != null) {
                Log.d(TAG, "Requesting focus for categories RecyclerView.");
                mRecyclerCategories.requestFocus();
            }
        }
    }

    public void hideChannelGrid() {
        setVisibility(GONE);
        mChannelGridOverlay.setVisibility(GONE);
    }

    public boolean isChannelGridVisible() {
        return getVisibility() == VISIBLE && mChannelGridOverlay.getVisibility() == VISIBLE;
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
