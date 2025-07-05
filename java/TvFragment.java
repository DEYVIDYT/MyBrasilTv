package com.example.iptvplayer;

import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View; // Added
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.DrawableRes;
import androidx.fragment.app.Fragment;
import android.widget.LinearLayout;
import android.content.BroadcastReceiver;
import com.example.iptvplayer.component.LiveControlView; // Kept for interface
import com.lxj.xpopup.XPopup;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;

// --- ExoPlayer v2.9.6 Imports ---
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
// --- End ExoPlayer v2 Imports ---
import android.app.PendingIntent;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager; // ADDED IMPORT
import android.graphics.drawable.Icon;
import java.util.ArrayList;
import android.util.Rational;
import android.widget.TextView;
import android.graphics.Color;
import android.content.res.Configuration;
import android.view.ViewGroup;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptvplayer.adapter.ChannelAdapter;
import com.example.iptvplayer.adapter.LiveCategoryAdapter;
import com.example.iptvplayer.adapter.EpgAdapter;
import com.example.iptvplayer.data.Channel;
import com.example.iptvplayer.data.EpgProgram;
import com.example.iptvplayer.parser.M3uParser;
import com.google.android.material.textfield.TextInputEditText;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Base64; // Importar Base64
import android.os.Handler; // Adicionado para retentativas
import android.os.Looper; // Adicionado para retentativas
import android.content.pm.ActivityInfo; // Adicionado para controle de orientação
import com.example.iptvplayer.component.LiveControlView; // Importar LiveControlView para o listener

public class TvFragment extends Fragment implements ChannelAdapter.OnChannelClickListener, EpgAdapter.OnProgramClickListener, DataManager.DataManagerListener, LiveControlView.OnProportionButtonClickListener {

    private RecyclerView recyclerViewChannels;
    private RecyclerView recyclerViewCategories;
    private RecyclerView recyclerViewEpg;
    private ChannelAdapter channelAdapter;
    private EpgAdapter epgAdapter;
    private TextInputEditText searchEditText;
    private List<Channel> allChannels = new ArrayList<>(); // Will be populated from DataManager
    private List<EpgProgram> currentEpgPrograms = new ArrayList<>(); // Will be populated from DataManager
    private Map<String, String> mFetchedCategoryMap; // Will be populated from DataManager
    
    private String currentChannelStreamId = null;
    private DataManager dataManager;

    // Tab views
    private TextView tabChannels;
    private TextView tabEpg;
    private TextView tabFavorites;

    // private ProgressBar playerProgressBar; // Already removed
    // private TextView playerLoadingTextView; // Already removed

    // --- ExoPlayer v2.9.6 specific ---
    private PlayerView playerView;
    private SimpleExoPlayer exoPlayer;
    // --- End ExoPlayer v2.9.6 specific ---

    // References to dkplayer's VideoView, StandardVideoController, TitleView, ChannelGridView (as controller component), GestureView are removed.
    // mChannelGridView might be reintroduced if it's a standalone View for UI, not a dkplayer controller component.

    private int mWidthPixels;
    private PictureInPictureParams.Builder mPictureInPictureParamsBuilder;
    private BroadcastReceiver mReceiver;

    // Sinalizador para controle do PiP durante a troca de canais
    private boolean mIsSwitchingChannels = false;

    private static final String ACTION_MEDIA_CONTROL = "media_control";
    private static final String EXTRA_CONTROL_TYPE = "control_type";
    private static final int CONTROL_TYPE_PLAY = 1;
    private static final int CONTROL_TYPE_PAUSE = 2;
    private static final int CONTROL_TYPE_REPLAY = 3;
    private static final int REQUEST_PLAY = 1;
    private static final int REQUEST_PAUSE = 2;
    private static final int REQUEST_REPLAY = 3;

    // private String speed = "1.0"; // Will be managed by ExoPlayer's PlaybackParameters
    // private String proportion = "默认"; // Will be managed by PlayerView's resizeMode
    // private String title = "测试标题"; // Title will be set on the TextView in custom controls

    private static final String TV_TAG = "TV_DEBUG"; // Tag para logs
    private static final String TAG_BACK_TV = "TvFragment_Back"; // Tag para logs do onBackPressed
    private static final String TAG_PLAYER_STATE_TV = "TvFragment_PlayerState"; // Tag para logs de estado do player
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private BroadcastReceiver refreshDataReceiver;

    // Variáveis para lógica de retentativa
    private String mCurrentPlayingUrl;
    private String mCurrentPlayingChannelName;
    private Handler mRetryHandler;
    private Runnable mRetryRunnable;
    private boolean mIsRetrying = false;
    private static final int RETRY_DELAY_MS = 5000; // 5 segundos de delay

    // Variáveis para otimização de buffer em canais lentos
    private static final int MAX_BUFFERING_TIME_MS = 15000; // 15 segundos
    private static final int PAUSE_FOR_BUFFER_REBUILD_MS = 7000; // 7 segundos
    private long mBufferingStartTime = 0;
    private boolean mIsPausedForBuffering = false;
    private Handler mBufferOptimizeHandler;
    private Runnable mCheckBufferingRunnable;
    private Runnable mResumeAfterBufferRebuildRunnable;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Broadcast receiver for data refresh
        refreshDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ProfileFragment.ACTION_REFRESH_DATA.equals(intent.getAction())) {
                    Log.d(TV_TAG, "ACTION_REFRESH_DATA received. Reloading initial data.");
                    if (isAdded() && getContext() != null) { // Ensure fragment is attached and context is available
                        // No need to call loadInitialData directly here.
                        // The onDataLoaded callback from DataManager will handle it.
                        // Ensure DataManager is re-initialized or its data is re-checked.
                        if (dataManager != null) {
                            dataManager.startDataLoading(); // Re-trigger data loading if needed
                        }
                    }
                }
            }
        };
        // Register receiver
        IntentFilter filter = new IntentFilter(ProfileFragment.ACTION_REFRESH_DATA);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(refreshDataReceiver, filter);
        Log.d(TV_TAG, "refreshDataReceiver registered.");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_tv, container, false);
        Log.d(TV_TAG, "onCreateView called");
        recyclerViewChannels = root.findViewById(R.id.recycler_view_channels);
        recyclerViewCategories = root.findViewById(R.id.recycler_view_categories);
        recyclerViewEpg = root.findViewById(R.id.recycler_view_epg);
        searchEditText = root.findViewById(R.id.search_edit_text);
        searchEditText.setText(""); // Limpar o texto de busca na criação da view
        // playerProgressBar = root.findViewById(R.id.player_progress_bar); // REMOVED
        // playerLoadingTextView = root.findViewById(R.id.player_loading_text); // REMOVED

        // Initialize tab views
        tabChannels = root.findViewById(R.id.tab_channels);
        tabEpg = root.findViewById(R.id.tab_epg);
        tabFavorites = root.findViewById(R.id.tab_favorites);

        recyclerViewChannels.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewEpg.setLayoutManager(new LinearLayoutManager(getContext()));

        // Setup tab click listeners
        setupTabListeners();

    // NEW: If adapter already exists (from a previous fragment instance),
    // re-link it to the new recyclerViewChannels instance immediately.
    // Also, clear its list to avoid showing stale data briefly if load is slow.
    if (channelAdapter != null) {
        Log.d(TV_TAG, "onCreateView: ChannelAdapter exists. Clearing its data and re-linking to new RecyclerView.");
        channelAdapter.updateData(new ArrayList<>()); // Clear old data
        recyclerViewChannels.setAdapter(channelAdapter);
    } else {
        Log.d(TV_TAG, "onCreateView: ChannelAdapter is null. Will be created by fetchLiveChannelsFromApi.");
        // Ensure RecyclerView doesn\"t have a stale adapter from XML or a previous different fragment.
        if (recyclerViewChannels != null) { // Check if recyclerViewChannels is initialized
            recyclerViewChannels.setAdapter(null);
        }
    }

        // ExoPlayer v2.9.6 Initialization for TvFragment
        mPictureInPictureParamsBuilder = new PictureInPictureParams.Builder();
        mWidthPixels = getResources().getDisplayMetrics().widthPixels;

        playerView = root.findViewById(R.id.player_view_tv_fragment_v2);
        if (playerView == null) {
            Log.e(TV_TAG, "PlayerView (player_view_tv_fragment_v2) not found in fragment_tv.xml");
            Toast.makeText(getContext(), "PlayerView not found error", Toast.LENGTH_LONG).show();
        } else {
            // Player is initialized in initializePlayerV2, called from onStart/onResume
            // Ensure controller_layout_id is set in fragment_tv.xml for playerView
            playerView.setControllerShowTimeoutMs(3000);
            // Listener is now setup in initializePlayerV2 to ensure it's (re)added if player is (re)created.
            // Basic setup of click listeners for the controller should also be here or in initializePlayerV2.
            setupControllerClickListenersV2();
        }

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (channelAdapter != null) {
                    channelAdapter.filterList(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        dataManager = MyApplication.getDataManager(getContext());
        dataManager.setListener(this); // Set this fragment as the listener

        // No need to call loadInitialData() here, it will be called via onDataLoaded()
        // or if data is already loaded, onResume() will handle it.

        // Inicializar Handler para retentativas
        mRetryHandler = new Handler(Looper.getMainLooper());
        mRetryRunnable = new Runnable() {
            @Override
            public void run() {
                if (mIsRetrying && exoPlayer != null && mCurrentPlayingUrl != null && !mCurrentPlayingUrl.isEmpty()) {
                    Log.d(TV_TAG, "Retrying channel with ExoPlayer v2: " + mCurrentPlayingChannelName + " URL: " + mCurrentPlayingUrl);
                    // showLoadingWithMessage is removed, rely on controller's buffering indicator
                    updateControllerUiOnErrorStateV2(null); // Hide error
                    updateControllerUiOnBufferingStateV2(true); // Show buffering

                    exoPlayer.stop();
                    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(requireContext(), Util.getUserAgent(requireContext(), requireContext().getPackageName()));
                    MediaSource mediaSource = videoUrl.toLowerCase().endsWith(".m3u8") ?
                            new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(mCurrentPlayingUrl)) :
                            new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(mCurrentPlayingUrl));
                    exoPlayer.setMediaSource(mediaSource);
                    exoPlayer.prepare();
                    exoPlayer.setPlayWhenReady(true);
                } else {
                    Log.d(TV_TAG, "ExoPlayer v2 retry condition not met or retry cancelled.");
                    mIsRetrying = false;
                    updateControllerUiOnBufferingStateV2(false);
                }
            }
        };

        // Custom buffer optimization logic (mBufferOptimizeHandler, etc.) is removed for ExoPlayer v2.9.6
        return root;
    }

    // showLoading and showLoadingWithMessage are removed as PlayerView handles its own indicators mostly.
    // If fragment-level loading indication (outside player) is needed for data loading, it would be a separate UI element.


    @Override
    public void onProgressUpdate(DataManager.LoadState state, int percentage, String message) {
        Log.d(TV_TAG, "DataManager Progress: " + state + " - " + percentage + "% - " + message);
    }

    @Override
    public void onDataLoaded() {
        Log.d(TV_TAG, "DataManager: All data loaded. Updating TV UI.");
        if (isAdded() && getContext() != null) {
            loadInitialData(); // Now safe to load initial data from DataManager
        }
    }

    @Override
    public void onError(String errorMessage) {
        Log.e(TV_TAG, "DataManager Error: " + errorMessage);
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), "Erro ao carregar dados: " + errorMessage, Toast.LENGTH_LONG).show();
            // showLoading(false); // PlayerView handles its own loading indicator; this was for general data.
        }
    }

    private void setupTabListeners() {
        tabChannels.setOnClickListener(v -> switchToChannelsTab());
        tabEpg.setOnClickListener(v -> switchToEpgTab());
        tabFavorites.setOnClickListener(v -> switchToFavoritesTab());
    }

    private void switchToChannelsTab() {
        // Update tab appearance
        updateTabAppearance(tabChannels, tabEpg, tabFavorites);
        
        // Show/hide views
        recyclerViewChannels.setVisibility(View.VISIBLE);
        recyclerViewEpg.setVisibility(View.GONE);
        
        // Ensure channels are displayed when switching back to this tab
        filterChannelsByCategory("0"); // Show all channels by default

        Log.d(TV_TAG, "Switched to Channels tab");
    }

    private void switchToEpgTab() {
        // Update tab appearance
        updateTabAppearance(tabEpg, tabChannels, tabFavorites);
        
        // Show/hide views
        recyclerViewChannels.setVisibility(View.GONE);
        recyclerViewEpg.setVisibility(View.VISIBLE);
        
        // Load EPG for current channel if available
        if (currentChannelStreamId != null && dataManager.getXmltvEpgService() != null) {
            // showLoading(true); // PlayerView handles its own loading indicator; this was for general data.
            dataManager.getXmltvEpgService().fetchChannelEpg(currentChannelStreamId, new EpgService.EpgCallback() {
                @Override
                public void onSuccess(List<EpgProgram> programs) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Log.d(TV_TAG, "XMLTV EPG loaded successfully for EPG tab: " + programs.size() + " programs");
                            currentEpgPrograms.clear();
                            currentEpgPrograms.addAll(programs);
                            
                            if (epgAdapter == null) {
                                epgAdapter = new EpgAdapter(getContext(), currentEpgPrograms, TvFragment.this);
                                recyclerViewEpg.setAdapter(epgAdapter);
                            } else {
                                epgAdapter.updateData(currentEpgPrograms);
                            }
                            
                            // showLoading(false); // PlayerView handles its own loading indicator; this was for general data.
                            
                            if (programs.isEmpty()) {
                                Toast.makeText(getContext(), "Nenhum programa EPG encontrado para este canal", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Log.e(TV_TAG, "Failed to load XMLTV EPG for EPG tab: " + error);
                            // showLoading(false); // PlayerView handles its own loading indicator; this was for general data.
                            Toast.makeText(getContext(), "Erro ao carregar EPG XMLTV: " + error, Toast.LENGTH_LONG).show();
                            
                            // Clear EPG list on failure
                            if (epgAdapter != null) {
                                epgAdapter.updateData(new ArrayList<>());
                            }
                        });
                    }
                }
            });
        } else {
            Log.d(TV_TAG, "No current channel selected for EPG or XmltvEpgService not available.");
            Toast.makeText(getContext(), "Selecione um canal primeiro para ver o EPG", Toast.LENGTH_SHORT).show();
            // showLoading(false); // PlayerView handles its own loading indicator; this was for general data.
            if (epgAdapter != null) {
                epgAdapter.updateData(new ArrayList<>());
            }
        }
        
        Log.d(TV_TAG, "Switched to EPG tab");
    }

    private void switchToFavoritesTab() {
        // Update tab appearance
        updateTabAppearance(tabFavorites, tabChannels, tabEpg);
        
        // Show/hide views
        recyclerViewChannels.setVisibility(View.VISIBLE);
        recyclerViewEpg.setVisibility(View.GONE);
        
        // TODO: Implement favorites functionality
        Log.d(TV_TAG, "Switched to Favorites tab (not implemented yet)");
    }

    private void updateTabAppearance(TextView selectedTab, TextView... otherTabs) {
        // Selected tab
        selectedTab.setTextColor(getResources().getColor(R.color.md_sys_color_primary));
        selectedTab.setBackgroundResource(R.drawable.tab_selected_background);
        
        // Other tabs
        for (TextView tab : otherTabs) {
            tab.setTextColor(getResources().getColor(R.color.md_sys_color_on_surface_variant));
            tab.setBackgroundResource(R.drawable.tab_unselected_background);
        }
    }

    @Override
    public void onProgramClick(EpgProgram program) {
        Log.d(TV_TAG, "EPG program clicked: " + program.getTitle());
        
        // Show program details
        String message = String.format("Programa: %s\nHorário: %s\nDescrição: %s", 
            program.getTitle(), 
            formatTimeRange(program.getStartTime(), program.getEndTime()),
            program.getDescription());
        
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        
        // TODO: Implement additional actions like setting reminders
    }

    private String formatTimeRange(String startTime, String endTime) {
        try {
            long start = Long.parseLong(startTime) * 1000;
            long end = Long.parseLong(endTime) * 1000;
            
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            String startFormatted = timeFormat.format(new java.util.Date(start));
            String endFormatted = timeFormat.format(new java.util.Date(end));
            
            return startFormatted + " - " + endFormatted;
        } catch (NumberFormatException e) {
            return startTime + " - " + endTime;
        }
    }

    @Override
    public void onChannelClick(Channel channel) {
        Log.d(TV_TAG, "onChannelClick (ExoPlayer v2): " + channel.getName() + " URL: " + channel.getStreamUrl());
        if (exoPlayer == null || playerView == null) {
            Log.e(TV_TAG, "ExoPlayer v2 or PlayerView not initialized in onChannelClick");
            Toast.makeText(getContext(), "ExoPlayer v2 not initialized error", Toast.LENGTH_SHORT).show();
            return;
        }
        if (channel.getStreamUrl() != null && !channel.getStreamUrl().isEmpty()) {
            mIsSwitchingChannels = true;
            // showLoading(true); // PlayerView will show its own buffering indicator

            mRetryHandler.removeCallbacks(mRetryRunnable);
            mIsRetrying = false;

            // Custom buffer optimization logic is removed for ExoPlayer v2.

            mCurrentPlayingUrl = channel.getStreamUrl();
            mCurrentPlayingChannelName = channel.getName();
            currentChannelStreamId = channel.getStreamId();
            Log.d(TV_TAG, "Current channel stream ID set for ExoPlayer v2: " + currentChannelStreamId);

            exoPlayer.stop(); // Stop any current playback
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(requireContext(), Util.getUserAgent(requireContext(), requireContext().getPackageName()));
            Uri videoUri = Uri.parse(mCurrentPlayingUrl);
            MediaSource mediaSource;
             if (mCurrentPlayingUrl.toLowerCase().endsWith(".m3u8")) {
                mediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(videoUri));
            } else {
                mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(videoUri));
            }
            exoPlayer.setMediaSource(mediaSource);
            exoPlayer.prepare();
            exoPlayer.setPlayWhenReady(true);

            TextView titleTextView = playerView.findViewById(R.id.exo_custom_title_v2); // ID from custom_exoplayer2_controls
            if (titleTextView != null) {
                titleTextView.setText(mCurrentPlayingChannelName);
            } else {
                Log.e(TV_TAG, "exo_custom_title_v2 TextView not found in controller for ExoPlayer v2.");
            }

            Toast.makeText(getContext(), getString(R.string.starting_channel_toast, channel.getName()), Toast.LENGTH_SHORT).show();
            Log.d(TV_TAG, "ExoPlayer v2 playback initiated for: " + mCurrentPlayingChannelName);
        } else {
            Log.e(TV_TAG, "Channel stream URL is null or empty for channel (ExoPlayer v2): " + channel.getName());
            mCurrentPlayingUrl = null;
            mCurrentPlayingChannelName = null;
            // showLoading(false); // Hide custom loading if used
            Toast.makeText(getContext(), getString(R.string.invalid_channel_url_error), Toast.LENGTH_SHORT).show();
        }
    }


    private void loadInitialData() {
        Log.d(TV_TAG, "loadInitialData called - using DataManager");
        if (!isAdded() || getContext() == null || dataManager == null) {
            Log.w(TV_TAG, "loadInitialData - Fragment not usable or DataManager is null. Aborting.");
            // showLoading(false); // If custom loading is used for this phase
            if (getContext() != null && dataManager == null) {
                 Toast.makeText(getContext(), "Error: DataManager not available.", Toast.LENGTH_LONG).show();
            }
            return;
        }

        if (dataManager.getLiveStreams() == null || dataManager.getLiveCategories() == null) {
            Log.d(TV_TAG, "DataManager has not finished loading data. Waiting...");
            dataManager.startDataLoading();
            // showLoading(true); // If custom loading is used for this phase
            return;
        }

        List<Channel> channels = dataManager.getLiveStreams();
        List<XtreamApiService.CategoryInfo> categories = dataManager.getLiveCategories();
        Map<String, String> epgProgramsMap = dataManager.getEpgPrograms(); // Global EPG as a Map

        mFetchedCategoryMap = new java.util.HashMap<>();
        if (categories != null) {
            for (XtreamApiService.CategoryInfo catInfo : categories) {
                mFetchedCategoryMap.put(catInfo.id, catInfo.name);
            }
        }

        allChannels.clear();
        if (channels != null) {
            allChannels.addAll(channels);
            // Update current program titles for all channels based on the EPG map
            if (epgProgramsMap != null) {
                for (Channel channel : allChannels) {
                    String programTitle = epgProgramsMap.get(channel.getStreamId());
                    if (programTitle != null) {
                        channel.setCurrentProgramTitle(programTitle);
                    }
                }
            }
        }

        // Update Category RecyclerView
        if (recyclerViewCategories != null && categories != null) {
            LiveCategoryAdapter categoryAdapter = new LiveCategoryAdapter(getContext(), categories, categoryId -> {
                filterChannelsByCategory(categoryId);
            });
            recyclerViewCategories.setAdapter(categoryAdapter);
        } else if (recyclerViewCategories != null) {
            recyclerViewCategories.setAdapter(null); // Clear if no categories
        }

        // Update Channels RecyclerView with all channels initially (or first category)
        filterChannelsByCategory("0"); // "0" or null for all channels

        // EPG tab is now handled by loadEpgForChannel which uses DataManager's XmltvEpgService
        // No need to update epgAdapter here directly with global EPG

        // if (mChannelGridView != null) { // ChannelGridView was a dkplayer component.
        //     mChannelGridView.setChannelsData(allChannels, mFetchedCategoryMap);
        // }

        // Start continuous EPG updates from DataManager
        if (dataManager.getXmltvEpgService() != null && allChannels != null && !allChannels.isEmpty()) {
            dataManager.getXmltvEpgService().startContinuousUpdate(allChannels, new XmltvEpgService.ChannelUpdateCallback() {
                @Override
                public void onChannelUpdated(String streamId, String currentProgram) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Log.d(TV_TAG, "EPG updated for channel " + streamId + ": " + currentProgram);
                            if (channelAdapter != null) {
                                channelAdapter.updateChannelProgram(streamId, currentProgram);
                            }
                        });
                    }
                }
            });
        }

        // showLoading(false); // If custom loading for this phase
    }

    private void setupControllerClickListenersV2() { // Renamed for v2
        if (playerView == null) return;

        ImageButton controllerBackButton = playerView.findViewById(R.id.exo_custom_back_button_v2);
        if (controllerBackButton != null) {
            controllerBackButton.setOnClickListener(v -> {
                if (getActivity() != null) {
                     // TODO: Add actual fullscreen exit logic if custom fullscreen is implemented
                    getActivity().onBackPressed();
                }
            });
        }

        Button controllerRetryButton = playerView.findViewById(R.id.exo_custom_retry_button_v2);
        if (controllerRetryButton != null) {
            controllerRetryButton.setOnClickListener(v -> {
                updateControllerUiOnErrorStateV2(null); // Hide error
                updateControllerUiOnBufferingStateV2(true); // Show buffering

                if (exoPlayer != null && mCurrentPlayingUrl != null) {
                    exoPlayer.stop();
                    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(requireContext(), Util.getUserAgent(requireContext(), requireContext().getPackageName()));
                    MediaSource mediaSource = mCurrentPlayingUrl.toLowerCase().endsWith(".m3u8") ?
                            new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(mCurrentPlayingUrl)) :
                            new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(mCurrentPlayingUrl));
                    exoPlayer.setMediaSource(mediaSource);
                    exoPlayer.prepare();
                    exoPlayer.setPlayWhenReady(true);
                } else {
                     updateControllerUiOnBufferingStateV2(false);
                     Toast.makeText(getContext(), "No stream to retry.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        ImageButton controllerReplayButton = playerView.findViewById(R.id.exo_custom_replay_button_v2);
        if (controllerReplayButton != null) {
            controllerReplayButton.setOnClickListener(v -> {
                if (exoPlayer != null) {
                    exoPlayer.seekTo(0);
                    exoPlayer.setPlayWhenReady(true);
                    controllerReplayButton.setVisibility(View.GONE);
                }
            });
        }

        ImageButton controllerPlayPauseBottom = playerView.findViewById(R.id.exo_play_pause_bottom_v2);
        if (controllerPlayPauseBottom != null) {
            controllerPlayPauseBottom.setOnClickListener(v -> {
                if (exoPlayer != null) {
                    exoPlayer.setPlayWhenReady(!exoPlayer.getPlayWhenReady());
                }
            });
        }

        ImageButton controllerSpeedButton = playerView.findViewById(R.id.exo_custom_speed_button_v2);
        if (controllerSpeedButton != null) {
            controllerSpeedButton.setOnClickListener(v -> {
                 if (getContext() != null) {
                    new XPopup.Builder(getContext())
                            .popupPosition(com.lxj.xpopup.enums.PopupPosition.Right)
                            .asCustom(new CustomDrawerPopupView(getContext()))
                            .show();
                }
            });
        }

        ImageButton controllerProportionButton = playerView.findViewById(R.id.exo_custom_proportion_button_v2);
        if (controllerProportionButton != null) {
            controllerProportionButton.setOnClickListener(v -> {
                if (getContext() != null) {
                     new XPopup.Builder(getContext())
                            .popupPosition(com.lxj.xpopup.enums.PopupPosition.Right)
                            .asCustom(new CustomDrawerPopupView1(getContext()))
                            .show();
                }
            });
        }

        ImageButton controllerRefreshButton = playerView.findViewById(R.id.exo_custom_refresh_button_v2);
        if (controllerRefreshButton != null) {
            controllerRefreshButton.setOnClickListener(v -> {
                 Toast.makeText(getContext(), "Refresh clicked (ExoPlayer v2)", Toast.LENGTH_SHORT).show();
                 if (exoPlayer != null && mCurrentPlayingUrl != null) {
                     exoPlayer.stop();
                    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(requireContext(), Util.getUserAgent(requireContext(), requireContext().getPackageName()));
                    MediaSource mediaSource = mCurrentPlayingUrl.toLowerCase().endsWith(".m3u8") ?
                            new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(mCurrentPlayingUrl)) :
                            new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(mCurrentPlayingUrl));
                    exoPlayer.setMediaSource(mediaSource);
                    exoPlayer.prepare();
                    exoPlayer.setPlayWhenReady(true);
                 }
            });
        }

        ImageButton controllerChannelGridToggleButton = playerView.findViewById(R.id.exo_custom_channel_grid_toggle_button_v2);
        if (controllerChannelGridToggleButton != null) {
            controllerChannelGridToggleButton.setOnClickListener(v -> {
                 Toast.makeText(getContext(), "Channel Grid Toggle Clicked (ExoPlayer v2)", Toast.LENGTH_SHORT).show();
                 // ChannelGridView dkplayer component was removed. This button's new action needs definition.
            });
        }

        ImageButton fullscreenButton = playerView.findViewById(R.id.exo_fullscreen); // Standard ID
        if (fullscreenButton != null) {
            // ExoPlayer v2's StyledPlayerControlView usually handles fullscreen toggle itself
            // by calling an internal FullscreenCallback. If custom logic is needed (e.g. orientation),
            // it would be more involved. For now, rely on its default or simple Toast.
            fullscreenButton.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Fullscreen toggled (ExoPlayer v2 default behavior)", Toast.LENGTH_SHORT).show();
            });
        }
    }


    private void filterChannelsByCategory(String categoryId) {
        List<Channel> filteredChannels = new ArrayList<>();
        if (categoryId == null || categoryId.isEmpty() || categoryId.equals("0")) {
            if (allChannels != null) filteredChannels.addAll(allChannels);
        } else {
            if (allChannels != null) {
                for (Channel channel : allChannels) {
                    if (channel.getCategoryId() != null && channel.getCategoryId().equals(categoryId)) {
                        filteredChannels.add(channel);
                    }
                }
            }
        }

        if (channelAdapter == null) {
            if (getContext() != null) { // Check context before creating adapter
                channelAdapter = new ChannelAdapter(getContext(), filteredChannels, TvFragment.this);
                if (recyclerViewChannels != null) recyclerViewChannels.setAdapter(channelAdapter);
            }
        } else {
            channelAdapter.updateData(filteredChannels);
        }

        if (searchEditText != null && channelAdapter != null) {
            String currentSearchText = searchEditText.getText().toString();
            channelAdapter.filterList(currentSearchText);
        }
    }


    // private void showLoading(boolean isLoading) { // REMOVED - PlayerView handles its own indicator
    // }

    // private void showLoadingWithMessage(String message) { // REMOVED
    // }

    // Helper para converter estado do player em string para logs (ExoPlayer v2 version)
    private String exoPlayStateToStringV2(int playState) {
        switch (playState) {
            case Player.STATE_IDLE: return "STATE_IDLE";
            case Player.STATE_BUFFERING: return "STATE_BUFFERING";
            case Player.STATE_READY: return "STATE_READY";
            case Player.STATE_ENDED: return "STATE_ENDED";
            default: return "STATE_UNKNOWN (" + playState + ")";
        }
    }
    // Original playStateToString for dkplayer is removed.

    public void parseM3uFile(String filePath) {
        executor.execute(() -> {
            try {
                File file = new File(filePath);
                BufferedReader reader = new BufferedReader(new FileReader(file));
                List<Channel> parsedChannels = M3uParser.parse(reader);
                reader.close();

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        allChannels.clear();
                        allChannels.addAll(parsedChannels);
                        channelAdapter = new ChannelAdapter(getContext(), allChannels, this);
                        recyclerViewChannels.setAdapter(channelAdapter);
                        // showLoading(false);
                        Toast.makeText(getContext(), getString(R.string.m3u_loaded_success_toast), Toast.LENGTH_SHORT).show();
                    });
                }
            }
            catch (IOException e) {
                Log.e(TV_TAG, "Erro ao ler arquivo M3U", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), getString(R.string.m3u_load_error_toast, e.getMessage()), Toast.LENGTH_LONG).show();
                        // showLoading(false);
                    });
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TV_TAG, "onDestroyView called");
        if (mRetryHandler != null && mRetryRunnable != null) {
            mRetryHandler.removeCallbacks(mRetryRunnable);
        }
        mIsRetrying = false;

        // Custom buffer optimization logic for dkplayer is removed.
        // if (mBufferOptimizeHandler != null) { ... }
        // mBufferingStartTime = 0;
        // mIsPausedForBuffering = false;

        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        if (playerView != null) {
            playerView.setPlayer(null);
        }
        if (dataManager != null) {
            dataManager.setListener(null);
        }
   }

    @Override
    public void onDetach() {
        super.onDetach();
        // Unregister receiver
        if (refreshDataReceiver != null) {
            try {
                LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(refreshDataReceiver);
                Log.d(TV_TAG, "refreshDataReceiver unregistered.");
            } catch (IllegalArgumentException e) {
                Log.w(TV_TAG, "refreshDataReceiver not registered or already unregistered.", e);
            }
        }
    }


    void updatePictureInPictureActions(
            @DrawableRes int iconId, String title, int controlType, int requestCode) {
        if (getActivity() == null || !getActivity().isInPictureInPictureMode()) {
            // Log.d(TV_TAG, "updatePictureInPictureActions - Not in PiP mode or activity is null. Skipping update.");
            // Only update PiP actions if the activity is currently in PiP mode
            return;
        }

        final ArrayList<RemoteAction> actions = new ArrayList<>();

        final PendingIntent intent =
                PendingIntent.getBroadcast(
                        getContext(),
                        requestCode,
                        new Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, controlType),
                        PendingIntent.FLAG_IMMUTABLE);
        final Icon icon = Icon.createWithResource(getContext(), iconId);
        actions.add(new RemoteAction(icon, title, title, intent));

        mPictureInPictureParamsBuilder.setActions(actions);

        if (getActivity() != null) {
            Log.d(TV_TAG, "updatePictureInPictureActions - Setting PiP params with new actions.");
            getActivity().setPictureInPictureParams(mPictureInPictureParamsBuilder.build());
        }
    }

    public class CustomDrawerPopupView extends com.lxj.xpopup.core.DrawerPopupView {
        public CustomDrawerPopupView(@androidx.annotation.NonNull Context context) {
            super(context);
        }
        @Override
        protected int getImplLayoutId() {
            return R.layout.speed;
        }
        @Override
        protected void onCreate() {
            super.onCreate();

            final TextView txt1 = (TextView) findViewById(R.id.textview1);
            final TextView txt2 = (TextView) findViewById(R.id.textview2);
            final TextView txt3 = (TextView) findViewById(R.id.textview3);
            final TextView txt4 = (TextView) findViewById(R.id.textview4);
            final TextView txt5 = (TextView) findViewById(R.id.textview5);

            if (speed.equals("0.75")) {
                txt1.setTextColor(Color.parseColor("#FF39C5BA"));
            }
            if (speed.equals("1.0")) {
                txt2.setTextColor(Color.parseColor("#FF39C5BC"));
            }
            if (speed.equals("1.25")) {
                txt3.setTextColor(Color.parseColor("#FF39C5BC"));
            }
            if (speed.equals("1.5")) {
                txt4.setTextColor(Color.parseColor("#FF39C5BC"));
            }
            if (speed.equals("2.0")) {
                txt5.setTextColor(Color.parseColor("#FF39C5BC"));
            }

            findViewById(R.id.cardview1).setOnClickListener(new OnClickListener() {
                @Override
            public void onClick(View v1) {
                if (exoPlayer != null) exoPlayer.setPlaybackParameters(new PlaybackParameters(0.75f));
                updateSpeedPopupSelectionV2(txt1, txt2, txt3, txt4, txt5);
                dismiss();
                }
            });

            findViewById(R.id.cardview2).setOnClickListener(new OnClickListener() {
                @Override
            public void onClick(View v1) {
                if (exoPlayer != null) exoPlayer.setPlaybackParameters(new PlaybackParameters(1.0f));
                updateSpeedPopupSelectionV2(txt2, txt1, txt3, txt4, txt5);
                dismiss();
                }
            });

            findViewById(R.id.cardview3).setOnClickListener(new OnClickListener() {
                @Override
            public void onClick(View v1) {
                if (exoPlayer != null) exoPlayer.setPlaybackParameters(new PlaybackParameters(1.25f));
                updateSpeedPopupSelectionV2(txt3, txt1, txt2, txt4, txt5);
                dismiss();
                }
            });

            findViewById(R.id.cardview4).setOnClickListener(new OnClickListener() {
                @Override
            public void onClick(View v1) {
                if (exoPlayer != null) exoPlayer.setPlaybackParameters(new PlaybackParameters(1.5f));
                updateSpeedPopupSelectionV2(txt4, txt1, txt2, txt3, txt5);
                dismiss();
                }
            });

            findViewById(R.id.cardview5).setOnClickListener(new OnClickListener() {
                @Override
            public void onClick(View v1) {
                if (exoPlayer != null) exoPlayer.setPlaybackParameters(new PlaybackParameters(2.0f));
                updateSpeedPopupSelectionV2(txt5, txt1, txt2, txt3, txt4);
                dismiss();
                }
            });
        }

    private void updateSpeedPopupSelectionV2(TextView selected, TextView... others) { // This helper is fine
        selected.setTextColor(Color.parseColor("#FF39C5BC"));
        for (TextView other : others) {
            other.setTextColor(Color.parseColor("#ffffff"));
        }
    }
    }

    public class CustomDrawerPopupView1 extends com.lxj.xpopup.core.DrawerPopupView { // For Proportion
        public CustomDrawerPopupView1(@androidx.annotation.NonNull Context context) {
            super(context);
        }
        @Override
        protected int getImplLayoutId() {
            return R.layout.proportion;
        }
        @Override
        protected void onCreate() {
            super.onCreate();

            final TextView txt1 = (TextView) findViewById(R.id.textview1);
            final TextView txt2 = (TextView) findViewById(R.id.textview2);
            final TextView txt3 = (TextView) findViewById(R.id.textview3);
            final TextView txt4 = (TextView) findViewById(R.id.textview4);
            final TextView txt5 = (TextView) findViewById(R.id.textview5);

            if (playerView != null) {
                int currentMode = playerView.getResizeMode();
                updateProportionPopupSelectionVisualsV2(currentMode, txt1, txt2, txt3, txt4, txt5);
            }

            findViewById(R.id.cardview1).setOnClickListener(new OnClickListener() { // Default
                @Override
                public void onClick(View v1) {
                    if (playerView != null) playerView.setResizeMode(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    updateProportionPopupSelectionVisualsV2(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT, txt1, txt2, txt3, txt4, txt5);
                    dismiss();
                }
            });

            findViewById(R.id.cardview2).setOnClickListener(new OnClickListener() { // 16:9 -> Fixed Width
                @Override
                public void onClick(View v1) {
                    if (playerView != null) playerView.setResizeMode(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);
                    updateProportionPopupSelectionVisualsV2(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH, txt1, txt2, txt3, txt4, txt5);
                    dismiss();
                }
            });

            findViewById(R.id.cardview3).setOnClickListener(new OnClickListener() { // Original -> FIT
                @Override
                public void onClick(View v1) {
                    if (playerView != null) playerView.setResizeMode(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    updateProportionPopupSelectionVisualsV2(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT, txt1, txt2, txt3, txt4, txt5);
                    dismiss();
                }
            });

            findViewById(R.id.cardview4).setOnClickListener(new OnClickListener() { // Fill
                @Override
                public void onClick(View v1) {
                    if (playerView != null) playerView.setResizeMode(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL);
                    updateProportionPopupSelectionVisualsV2(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL, txt1, txt2, txt3, txt4, txt5);
                    dismiss();
                }
            });

            findViewById(R.id.cardview5).setOnClickListener(new OnClickListener() { // Center Crop -> Zoom
                @Override
                public void onClick(View v1) {
                    if (playerView != null) playerView.setResizeMode(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                    updateProportionPopupSelectionVisualsV2(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM, txt1, txt2, txt3, txt4, txt5);
                    dismiss();
                }
            });
        }

        private void updateProportionPopupSelectionVisualsV2(int currentMode, TextView txt1, TextView txt2, TextView txt3, TextView txt4, TextView txt5) {
            txt1.setTextColor(Color.parseColor(currentMode == com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT ? "#FF39C5BC" : "#ffffff"));
            txt2.setTextColor(Color.parseColor(currentMode == com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH ? "#FF39C5BC" : "#ffffff"));
            txt3.setTextColor(Color.parseColor(currentMode == com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT ? "#FF39C5BC" : "#ffffff")); // Assuming "Original" maps to FIT
            txt4.setTextColor(Color.parseColor(currentMode == com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL ? "#FF39C5BC" : "#ffffff"));
            txt5.setTextColor(Color.parseColor(currentMode == com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM ? "#FF39C5BC" : "#ffffff"));
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (playerView != null) {
            // ExoPlayer v2.9.6 PlayerView typically handles this.
        }
        if (getActivity() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (newConfig.smallestScreenWidthDp >= 600) {
                // Handle tablet specific layout changes if needed
            }
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // Handle landscape specific layout changes if needed
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                // Handle portrait specific layout changes if needed
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayerV2();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TV_TAG, "onResume called (ExoPlayer v2.9.6)");
        if (Util.SDK_INT <= 23 || exoPlayer == null) {
            initializePlayerV2();
        }
        if (exoPlayer != null) {
             // Check playback state before resuming
            int playbackState = exoPlayer.getPlaybackState();
            if (exoPlayer.getPlayWhenReady() || (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED)) {
                 exoPlayer.setPlayWhenReady(true);
            }
        }
        // Always attempt to load initial data when fragment resumes
        if (dataManager != null) {
            // Check if data is already loaded. If so, display it. Otherwise, DataManager will notify via listener.
            if (dataManager.getLiveStreams() != null && dataManager.getLiveCategories() != null) {
                loadInitialData();
            } else {
                // If data is not yet loaded, ensure DataManager starts loading it.
                // The onDataLoaded callback will then update the UI.
                dataManager.startDataLoading();
            }
        } else {
            Log.w(TV_TAG, "onResume - DataManager is null. Cannot refresh channels. This might indicate an issue with MainActivity flow.");
        }
    }

    public boolean onBackPressed() {
        Log.d(TAG_BACK_TV, "onBackPressed called in TvFragment (ExoPlayer v2).");
        // Fullscreen logic for ExoPlayer v2 needs careful implementation if custom behavior beyond
        // what PlayerControlView offers is needed.
        // For now, returning false to let activity handle it.
        Log.d(TAG_BACK_TV, "Back press not explicitly handled by TvFragment (ExoPlayer v2). Returning false.");
        return false;
    }

    // This interface implementation was for dkplayer's LiveControlView.
    // The proportion button is now part of custom_exoplayer2_controls.xml.
    // Its click listener is set up in setupControllerClickListenersV2.
    @Override
    public void onProportionButtonClick() {
        if (getContext() != null && playerView != null) {
            new XPopup.Builder(getContext())
                    .popupPosition(com.lxj.xpopup.enums.PopupPosition.Right)
                    .asCustom(new CustomDrawerPopupView1(getContext()))
                    .show();
        }
    }

    // +++ ExoPlayer v2 Helper Methods for UI and Lifecycle +++
    private void initializePlayerV2() {
        if (exoPlayer == null && getContext() != null && playerView != null) { // Added playerView null check
            DefaultTrackSelector trackSelector = new DefaultTrackSelector(new AdaptiveTrackSelection.Factory(new DefaultBandwidthMeter()));
            exoPlayer = ExoPlayerFactory.newSimpleInstance(requireContext(), trackSelector, new DefaultLoadControl());
            playerView.setPlayer(exoPlayer);

            // Add the full EventListener implementation
            exoPlayer.addListener(new Player.EventListener() {
                @Override
                public void onTimelineChanged(@NonNull Timeline timeline, @Nullable Object manifest, int reason) { }
                @Override
                public void onTracksChanged(@NonNull TrackGroupArray trackGroups, @NonNull TrackSelectionArray trackSelections) { }
                @Override
                public void onLoadingChanged(boolean isLoading) { }
                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                    boolean isActuallyPlaying = playWhenReady && playbackState == Player.STATE_READY;
                    updatePiPIfNeededV2(playbackState, isActuallyPlaying);
                    updateControllerUiOnErrorStateV2(null);
                    updateControllerUiOnEndedStateV2(playbackState == Player.STATE_ENDED);
                    updateControllerUiOnBufferingStateV2(playbackState == Player.STATE_BUFFERING);

                    if (playbackState == Player.STATE_READY && mIsRetrying && playWhenReady) {
                         Log.d(TV_TAG, "Channel " + mCurrentPlayingChannelName + " started playing with ExoPlayer v2.9.6 after ERROR retry.");
                         mIsRetrying = false;
                         mRetryHandler.removeCallbacks(mRetryRunnable);
                    }
                    if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                        if(mIsSwitchingChannels && playbackState == Player.STATE_READY) {
                            Log.d(TV_TAG, "ExoPlayer v2.9.6 Ready after channel switch, resetting mIsSwitchingChannels.");
                        }
                        if (playbackState != Player.STATE_IDLE) {
                           mIsSwitchingChannels = false;
                        }
                    }
                     updatePlayPauseButtonsV2(isActuallyPlaying);
                }
                @Override
                public void onRepeatModeChanged(int repeatMode) { }
                @Override
                public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) { }
                @Override
                public void onPlayerError(@NonNull ExoPlaybackException error) {
                    Log.e(TV_TAG, "ExoPlayer v2.9.6 Error: ", error);
                    updateControllerUiOnErrorStateV2(error);
                    if (exoPlayer != null && mCurrentPlayingUrl != null && !mCurrentPlayingUrl.isEmpty()) {
                        Log.e(TV_TAG, "Error playing channel with ExoPlayer v2.9.6: " + mCurrentPlayingChannelName + ". Initiating retry.");
                        mIsRetrying = true;
                        mRetryHandler.postDelayed(mRetryRunnable, RETRY_DELAY_MS);
                    }
                }
                @Override
                public void onPositionDiscontinuity(int reason) { }
                @Override
                public void onPlaybackParametersChanged(@NonNull PlaybackParameters playbackParameters) { }
                @Override
                public void onSeekProcessed() { }
            });

            // Auto-play if URL available (e.g. on resume after config change)
            if (mCurrentPlayingUrl != null && !mCurrentPlayingUrl.isEmpty() && exoPlayer.getPlaybackState() == Player.STATE_IDLE) {
                DataSource.Factory dataSourceFactory = new DefaultHttpDataSourceFactory(Util.getUserAgent(requireContext(), requireContext().getPackageName()));
                Uri videoUri = Uri.parse(mCurrentPlayingUrl);
                MediaSource mediaSource = mCurrentPlayingUrl.toLowerCase().endsWith(".m3u8") ?
                        new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(videoUri) :
                        new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(videoUri);
                exoPlayer.prepare(mediaSource);
            }
        }
    }

    private void releasePlayerV2() { // This is correct for releasing the player
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
         if (playerView != null) { // Also clear player from PlayerView
            playerView.setPlayer(null);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayerV2();
        }
    }

    @Override
    public void onDestroyView() { // Ensure player is released in onDestroyView
        super.onDestroyView();
        Log.d(TV_TAG, "onDestroyView called - ExoPlayer v2.9.6");
        releasePlayerV2();

        if (mRetryHandler != null && mRetryRunnable != null) { // Also clear retry callbacks
            mRetryHandler.removeCallbacks(mRetryRunnable);
        }
        // Custom buffer optimization logic already removed
         if (dataManager != null) { // Clear listener for DataManager
            dataManager.setListener(null);
        }
    }


    private void updatePlayPauseButtonsV2(boolean isPlaying) { // isPlaying should be derived from playWhenReady and state
        if (playerView == null) return;
        ImageButton playButton = playerView.findViewById(R.id.exo_play);
        ImageButton pauseButton = playerView.findViewById(R.id.exo_pause);
        ImageButton playPauseBottom = playerView.findViewById(R.id.exo_play_pause_bottom_v2);

        if (playButton != null) playButton.setVisibility(isPlaying ? View.GONE : View.VISIBLE);
        if (pauseButton != null) pauseButton.setVisibility(isPlaying ? View.VISIBLE : View.GONE);
        if (playPauseBottom != null) {
            playPauseBottom.setImageResource(isPlaying ? R.drawable.dkplayer_ic_action_pause : R.drawable.dkplayer_ic_action_play_arrow);
            // Content description for v2 is usually handled by StyledPlayerControlView
        }
    }

    private void updateControllerUiOnErrorStateV2(ExoPlaybackException error) {
        if (playerView == null) return;
        View loadingIndicator = playerView.findViewById(R.id.exo_buffering);
        View errorViewFromController = playerView.findViewById(R.id.exo_custom_error_view_v2);
        TextView errorMessageTextView = playerView.findViewById(R.id.exo_custom_error_message_v2);
        Button ctrlRetryButton = playerView.findViewById(R.id.exo_custom_retry_button_v2);

        if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);

        if (error != null) {
            if (errorViewFromController != null) errorViewFromController.setVisibility(View.VISIBLE);
            if (errorMessageTextView != null) errorMessageTextView.setText(error.getLocalizedMessage() != null ? error.getLocalizedMessage() : "An unknown error occurred.");
            if (ctrlRetryButton != null) ctrlRetryButton.setEnabled(true);
            playerView.hideController();
        } else {
            if (errorViewFromController != null) errorViewFromController.setVisibility(View.GONE);
        }
    }

    private void updateControllerUiOnEndedStateV2(boolean hasEnded) {
        if (playerView == null) return;
        View ctrlReplayButton = playerView.findViewById(R.id.exo_custom_replay_button_v2);
        if (ctrlReplayButton != null) {
            ctrlReplayButton.setVisibility(hasEnded ? View.VISIBLE : View.GONE);
        }
        if (hasEnded) {
            playerView.showController();
        }
    }

    private void updateControllerUiOnBufferingStateV2(boolean isBuffering) {
        if (playerView == null) return;
        View loadingIndicator = playerView.findViewById(R.id.exo_buffering);
        View errorViewFromController = playerView.findViewById(R.id.exo_custom_error_view_v2);
        View ctrlReplayButton = playerView.findViewById(R.id.exo_custom_replay_button_v2);

        if (loadingIndicator != null) loadingIndicator.setVisibility(isBuffering ? View.VISIBLE : View.GONE);
        if (isBuffering) {
            if (errorViewFromController != null) errorViewFromController.setVisibility(View.GONE);
            if (ctrlReplayButton != null) ctrlReplayButton.setVisibility(View.GONE);
        }
    }

    private void updatePiPIfNeededV2(int playbackState, boolean isPlaying) {
        if (getActivity() == null || !getActivity().isInPictureInPictureMode()) {
            return;
        }
        // PiP actions for ExoPlayer v2 (same logic as before, just ensure it's called correctly)
        if (playbackState == Player.STATE_ENDED) {
            updatePictureInPictureActions(
                    R.drawable.dkplayer_ic_action_replay, getString(R.string.pip_action_replay), CONTROL_TYPE_REPLAY, REQUEST_REPLAY);
        } else if (isPlaying) {
            updatePictureInPictureActions(
                    R.drawable.dkplayer_ic_action_pause, getString(R.string.pip_action_pause), CONTROL_TYPE_PAUSE, REQUEST_PAUSE);
        } else {
            updatePictureInPictureActions(
                    R.drawable.dkplayer_ic_action_play_arrow, getString(R.string.pip_action_play), CONTROL_TYPE_PLAY, REQUEST_PLAY);
        }
    }
    // --- End ExoPlayer v2 Helper Methods ---
}


