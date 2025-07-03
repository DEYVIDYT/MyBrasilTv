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
        mChannelGridOverlay.setOnClickListener(this);
        
        View contentAreaView = findViewById(R.id.channel_grid_content_area);
        if (contentAreaView != null) {
            contentAreaView.setOnClickListener(v -> { /* Consume click */ });
        }
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
        Log.d(TAG, "onClick triggered by view: " + v.getClass().getName() + " with ID: " + v.getId() + " (hex: " + Integer.toHexString(v.getId()) + ")");
        Log.d(TAG, "Overlay ID expected: " + R.id.channel_grid_overlay + " (hex: " + Integer.toHexString(R.id.channel_grid_overlay) + ")");
        if (v.getId() == R.id.channel_grid_overlay) {
            Log.d(TAG, "Condition met: Clicked view IS the overlay. Hiding channel grid.");
            hideChannelGrid();
        } else {
            Log.d(TAG, "Condition NOT met: Clicked view is NOT the overlay. Grid remains visible.");
        }
    }

    public void setChannelSelectedListener(OnChannelSelectedListener listener) {
        mChannelSelectedListener = listener;
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
                mControlWrapper.hide();
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
