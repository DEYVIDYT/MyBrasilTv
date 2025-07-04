package com.example.iptvplayer;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.iptvplayer.data.Channel;
import com.example.iptvplayer.data.EpgProgram;
import android.view.KeyEvent;
import com.example.iptvplayer.component.ChannelGridView;

import com.example.iptvplayer.adapter.ChannelCategoryAdapterTv;
import com.example.iptvplayer.adapter.ChannelAdapterTv;
import com.example.iptvplayer.adapter.EpgAdapterTv;
import com.example.iptvplayer.component.TitleView;

import xyz.doikki.videoplayer.player.VideoView; // Assuming DoikkiPlayer
// import xyz.doikki.videoplayer.controller.StandardVideoController; // Or a custom TV controller
import com.example.iptvplayer.StandardVideoController; // Corrigido para usar o controller do projeto

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TvFragmentTv extends Fragment implements DataManager.DataManagerListener, TvKeyHandler.TvKeyListener {

    private static final String TV_TV_TAG = "TV_TV_DEBUG";

    private DataManager dataManager;

    private RecyclerView recyclerViewCategoriesTv, recyclerViewChannelsTv, recyclerViewEpgTv;
    private FrameLayout playerContainerTv;
    private VideoView videoViewTv;
    private StandardVideoController videoControllerTv; // Ou um controlador customizado para TV
    private ProgressBar playerProgressBarTv;
    private TitleView mTitleViewComponent; // Adicionar esta linha
    private ChannelGridView mChannelGridView; // Adicionar esta linha

    private ChannelCategoryAdapterTv categoryAdapterTv;
    private ChannelAdapterTv channelAdapterTv;
    private EpgAdapterTv epgAdapterTv;

    private List<Channel> currentChannels = new ArrayList<>();
    private List<EpgProgram> currentEpgPrograms = new ArrayList<>();
    private Map<String, String> currentCategoryMap; // Para nomes de categoria

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d(TV_TV_TAG, "onAttach called");
        dataManager = MyApplication.getDataManager(context);
        if (dataManager == null) {
            Log.e(TV_TV_TAG, "DataManager is null in onAttach!");
            // Handle this error appropriately, e.g., show a toast or finish activity
            return;
        }
        dataManager.setListener(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TV_TV_TAG, "onCreateView called");
        View root = inflater.inflate(R.layout.fragment_tv_tv, container, false);

        playerContainerTv = root.findViewById(R.id.tv_player_container_tv);
        playerProgressBarTv = root.findViewById(R.id.tv_player_progress_bar_tv);

        recyclerViewCategoriesTv = root.findViewById(R.id.recycler_view_tv_categories_tv);
        recyclerViewChannelsTv = root.findViewById(R.id.recycler_view_tv_channels_tv);
        recyclerViewEpgTv = root.findViewById(R.id.recycler_view_tv_epg_tv);

        Log.d(TV_TV_TAG, "Calling setupRecyclerViews()");
        setupRecyclerViews();
        // initializePlayer() is removed from here. Player will be initialized on first channel selection.
        // Log.d(TV_TV_TAG, "Calling initializePlayer()");
        // initializePlayer();

        Log.d(TV_TV_TAG, "Views initialized and methods called.");
        return root;
    }

    private void setupRecyclerViews() {
        Log.d(TV_TV_TAG, "setupRecyclerViews called");
        if (getContext() == null) {
            Log.e(TV_TV_TAG, "Context is null in setupRecyclerViews!");
            return;
        }
        recyclerViewCategoriesTv.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewChannelsTv.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewEpgTv.setLayoutManager(new LinearLayoutManager(getContext()));

        Log.d(TV_TV_TAG, "Initializing categoryAdapterTv");
        categoryAdapterTv = new ChannelCategoryAdapterTv(getContext(), new ArrayList<>(), this::onCategorySelected);
        recyclerViewCategoriesTv.setAdapter(categoryAdapterTv);
        
        Log.d(TV_TV_TAG, "Initializing channelAdapterTv");
        channelAdapterTv = new ChannelAdapterTv(getContext(), new ArrayList<>(), this::onChannelSelected);
        recyclerViewChannelsTv.setAdapter(channelAdapterTv);
        
        Log.d(TV_TV_TAG, "Initializing epgAdapterTv");
        epgAdapterTv = new EpgAdapterTv(getContext(), new ArrayList<>(), this::onEpgProgramSelected);
        recyclerViewEpgTv.setAdapter(epgAdapterTv);

        // Adicionar foco para D-Pad
        recyclerViewCategoriesTv.setFocusable(true);
        recyclerViewChannelsTv.setFocusable(true);
        recyclerViewEpgTv.setFocusable(true);
        Log.d(TV_TV_TAG, "RecyclerViews setup complete.");
    }

    // This method is being replaced by setupVideoPlayerComponents
    // private void initializePlayer() { ... }


    private void setupVideoPlayerComponents(Channel channel) {
        Log.d(TV_TV_TAG, "setupVideoPlayerComponents for channel: " + channel.getName());
        if (getContext() == null || playerContainerTv == null) {
            Log.e(TV_TV_TAG, "Context or playerContainerTv is null in setupVideoPlayerComponents.");
            if (getContext() != null) {
                 Toast.makeText(getContext(), "Error initializing player components.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Create new VideoView instance
        videoViewTv = new VideoView(requireContext()); // Use requireContext for non-null context
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        videoViewTv.setLayoutParams(params);
        playerContainerTv.addView(videoViewTv);

        // Create new StandardVideoController and add components
        videoControllerTv = new StandardVideoController(requireContext());

        // TitleView
        mTitleViewComponent = new TitleView(requireContext());
        videoControllerTv.addControlComponent(mTitleViewComponent);

        // ChannelGridView (for TV interaction)
        mChannelGridView = new ChannelGridView(requireContext());
        mChannelGridView.setChannelSelectedListener(this::onChannelSelected); // Assuming this is correct
        videoControllerTv.addControlComponent(mChannelGridView);

        // Standard player components (like in VideoPlayerActivity, if needed for TV)
        // Example: controller.addControlComponent(new CompleteView(requireContext()));
        // Example: controller.addControlComponent(new ErrorView(requireContext()));
        // Example: controller.addControlComponent(new PrepareView(requireContext()));
        // For now, keeping it minimal to TitleView and ChannelGridView as per TvFragmentTv's explicit members

        videoViewTv.setVideoController(videoControllerTv);

        videoViewTv.addOnStateChangeListener(new VideoView.SimpleOnStateChangeListener() {
            @Override
            public void onPlayStateChanged(int playState) {
                Log.d(TV_TV_TAG, "Player state changed: " + playState);
                if (!isAdded() || playerProgressBarTv == null) {
                    Log.w(TV_TV_TAG, "onPlayStateChanged: Fragment not added or playerProgressBarTv is null. State: " + playState);
                    return;
                }

                if (playState == VideoView.STATE_PREPARING || playState == VideoView.STATE_BUFFERING) {
                    playerProgressBarTv.setVisibility(View.VISIBLE);
                } else {
                    playerProgressBarTv.setVisibility(View.GONE);
                }
            }
        });
        Log.d(TV_TV_TAG, "Video player components setup complete.");
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TV_TV_TAG, "onResume called");
        if (videoViewTv != null) videoViewTv.resume();
        Log.d(TV_TV_TAG, "Calling updateUi() from onResume");
        updateUi();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TV_TV_TAG, "onPause called");
        if (videoViewTv != null) videoViewTv.pause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TV_TV_TAG, "onDestroyView called");
        if (videoViewTv != null) videoViewTv.release();
    }

    @Override
    public void onDataLoaded() {
        Log.d(TV_TV_TAG, "onDataLoaded callback received.");
        if (isAdded() && getContext() != null) {
            Log.d(TV_TV_TAG, "Calling updateUi() from onDataLoaded");
            updateUi();
        } else {
            Log.w(TV_TV_TAG, "onDataLoaded: Fragment not added or context is null. Cannot update UI.");
        }
    }

    @Override
    public void onProgressUpdate(DataManager.LoadState state, int percentage, String message) {
        Log.d(TV_TV_TAG, "DataManager Progress: " + state + " - " + percentage + "% - " + message);
        if (state == DataManager.LoadState.COMPLETE || state == DataManager.LoadState.FAILED) { // Corrigido para FAILED
            showLoading(false); // Assumindo que showLoading manipula o ProgressBar principal do fragmento
        } else {
            showLoading(true);
        }
    }

    @Override
    public void onError(String errorMessage) {
        Log.e(TV_TV_TAG, "DataManager Error: " + errorMessage);
        if (isAdded() && getContext() != null) { // getContext() check is good here
            showLoading(false);
            Toast.makeText(getContext(), "Erro ao carregar dados para TV Ao Vivo: " + errorMessage, Toast.LENGTH_LONG).show();
        } else {
            Log.w(TV_TV_TAG, "onError: Fragment not added or context is null. Error: " + errorMessage);
        }
    }

    private void updateUi() {
        Log.d(TV_TV_TAG, "updateUi called");
        if (dataManager == null) {
            Log.e(TV_TV_TAG, "DataManager is null in updateUi!");
            return;
        }
        if (dataManager.isDataFullyLoaded()) {
            Log.d(TV_TV_TAG, "Data is fully loaded for Live TV.");
            showLoading(false);
            Log.d(TV_TV_TAG, "Calling loadLiveCategories()");
            loadLiveCategories();
            // Carregar canais da primeira categoria ou todos os canais por padrão
            if (categoryAdapterTv != null && dataManager.getLiveCategories() != null && !dataManager.getLiveCategories().isEmpty()) {
                Log.d(TV_TV_TAG, "Selecting first category: " + dataManager.getLiveCategories().get(0).name);
                onCategorySelected(dataManager.getLiveCategories().get(0));
            } else {
                Log.w(TV_TV_TAG, "categoryAdapterTv is null or live categories are empty. Cannot select first category.");
            }
        } else {
            Log.d(TV_TV_TAG, "Data not loaded for Live TV. Displaying loading indicator.");
            showLoading(true);
            if (!dataManager.isLoading()) { // Agora usa o método isLoading()
                Log.d(TV_TV_TAG, "DataManager not loading, starting data load.");
                dataManager.startDataLoading();
            } else {
                Log.d(TV_TV_TAG, "DataManager is already loading.");
            }
        }
    }

    private void showLoading(boolean isLoading) {
        Log.d(TV_TV_TAG, "showLoading called with: " + isLoading + " (Fragment-level, not player)");
        // TODO: Implementar lógica para mostrar/ocultar um indicador de loading geral para o fragmento se necessário.
        // playerProgressBarTv é para o player.
    }

    private void loadLiveCategories() {
        Log.d(TV_TV_TAG, "loadLiveCategories called");
        List<XtreamApiService.CategoryInfo> categories = dataManager.getLiveCategories();
        if (categories != null) {
            Log.d(TV_TV_TAG, "Loading " + categories.size() + " live categories.");
            if (categoryAdapterTv != null) {
                categoryAdapterTv.updateData(categories);
            } else {
                Log.e(TV_TV_TAG, "categoryAdapterTv is null in loadLiveCategories!");
            }
            // Se o adapter não existir, instanciar aqui.
            // Fazer a primeira categoria ser selecionada por padrão e carregar seus canais.
            if (!categories.isEmpty()) {
                 Log.d(TV_TV_TAG, "Selecting first category in loadLiveCategories: " + categories.get(0).name);
                 onCategorySelected(categories.get(0));
            }
        } else {
            Log.w(TV_TV_TAG, "No live categories found.");
        }
    }

    private void onCategorySelected(XtreamApiService.CategoryInfo category) {
        Log.d(TV_TV_TAG, "onCategorySelected: " + category.name);
        List<Channel> channels = dataManager.getLiveStreamsByCategory(category.id);
        if (channels != null) {
            Log.d(TV_TV_TAG, "Updating channelAdapterTv with " + channels.size() + " channels.");
            if (channelAdapterTv != null) {
                channelAdapterTv.updateData(channels);
            } else {
                Log.e(TV_TV_TAG, "channelAdapterTv is null in onCategorySelected!");
            }
            // Se houver canais, selecionar o primeiro e carregar EPG/Player.
            if(!channels.isEmpty()) {
                Log.d(TV_TV_TAG, "Selecting first channel: " + channels.get(0).getName());
                onChannelSelected(channels.get(0));
            }
        } else {
            Log.w(TV_TV_TAG, "No channels found for category: " + category.name);
        }
    }

    private void onChannelSelected(Channel channel) {
        Log.d(TV_TV_TAG, "onChannelSelected: " + channel.getName() + " URL: " + channel.getStreamUrl());
        Log.d(TV_TV_TAG, "onChannelSelected: " + channel.getName());
        if (!isAdded() || getContext() == null || playerContainerTv == null) {
            Log.e(TV_TV_TAG, "onChannelSelected: Fragment not added, context is null, or playerContainerTv is null.");
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error selecting channel.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        String streamUrl = channel.getStreamUrl();
        if (streamUrl == null || streamUrl.isEmpty()) {
            Log.e(TV_TV_TAG, "Stream URL is null or empty for channel: " + channel.getName());
            Toast.makeText(getContext(), "Invalid stream URL for " + channel.getName(), Toast.LENGTH_LONG).show();
            // Optionally, clear the player view or show an error message in the player area
            if (videoViewTv != null) {
                 playerContainerTv.removeView(videoViewTv);
                 videoViewTv.release();
                 videoViewTv = null;
                 videoControllerTv = null; // Also nullify controller
            }
            return;
        }
        Log.d(TV_TV_TAG, "Stream URL: " + streamUrl);

        // Release existing player if any
        if (videoViewTv != null) {
            Log.d(TV_TV_TAG, "Releasing existing videoViewTv.");
            playerContainerTv.removeView(videoViewTv);
            videoViewTv.release();
            videoViewTv = null;
            videoControllerTv = null; // Ensure controller is also ready to be recreated
            mTitleViewComponent = null; // Will be recreated in setupVideoPlayerComponents
            mChannelGridView = null; // Will be recreated in setupVideoPlayerComponents
        }

        // Setup new player components for the selected channel
        setupVideoPlayerComponents(channel); // This will create new videoViewTv, controller, etc.

        if (videoViewTv == null) {
            Log.e(TV_TV_TAG, "videoViewTv is null after setupVideoPlayerComponents!");
            Toast.makeText(getContext(), "Failed to initialize player.", Toast.LENGTH_LONG).show();
            return;
        }

        // Set URL and start playback
        videoViewTv.setUrl(streamUrl);
        if (mTitleViewComponent != null) { // mTitleViewComponent is re-instantiated in setupVideoPlayerComponents
            mTitleViewComponent.setTitle(channel.getName());
        } else {
            Log.e(TV_TV_TAG, "mTitleViewComponent is null after setup, cannot set title for channel: " + channel.getName());
        }
        videoViewTv.start();

        loadEpgForChannel(channel.getStreamId());
    }

    private void loadEpgForChannel(String streamId) {
        Log.d(TV_TV_TAG, "loadEpgForChannel called for streamId: " + streamId);
        if (dataManager == null || dataManager.getXmltvEpgService() == null) {
            Log.e(TV_TV_TAG, "DataManager or XmltvEpgService is null in loadEpgForChannel!");
            return;
        }
        dataManager.getXmltvEpgService().fetchChannelEpg(streamId, new EpgService.EpgCallback() {
            @Override
            public void onSuccess(List<EpgProgram> programs) {
                if (getActivity() != null && isAdded()) { // Added isAdded() check
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded() || getContext() == null) { // Double check inside runOnUiThread
                            Log.w(TV_TV_TAG, "loadEpgForChannel.onSuccess: Fragment not added or context null in UI thread.");
                            return;
                        }
                        Log.d(TV_TV_TAG, "XMLTV EPG loaded successfully for EPG tab: " + programs.size() + " programs");
                        if (epgAdapterTv != null) {
                            epgAdapterTv.updateData(programs);
                        } else {
                            Log.e(TV_TV_TAG, "epgAdapterTv is null in loadEpgForChannel.onSuccess!");
                        }
                    });
                } else {
                    Log.w(TV_TV_TAG, "loadEpgForChannel.onSuccess: Fragment not attached or activity is null.");
                }
            }

            @Override
            public void onFailure(String error) {
                if (getActivity() != null && isAdded()) { // Added isAdded() check
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded() || getContext() == null) { // Double check inside runOnUiThread
                            Log.w(TV_TV_TAG, "loadEpgForChannel.onFailure: Fragment not added or context null in UI thread.");
                            return;
                        }
                        Log.e(TV_TV_TAG, "Failed to load XMLTV EPG for EPG tab: " + error);
                        Toast.makeText(getContext(), "Erro ao carregar EPG XMLTV: " + error, Toast.LENGTH_LONG).show();
                        if (epgAdapterTv != null) {
                            epgAdapterTv.updateData(new ArrayList<>());
                        } else {
                            Log.e(TV_TV_TAG, "epgAdapterTv is null in loadEpgForChannel.onFailure!");
                        }
                    });
                } else {
                    Log.w(TV_TV_TAG, "loadEpgForChannel.onFailure: Fragment not attached or activity is null.");
                }
            }
        });
    }

    public void onEpgProgramSelected(EpgProgram program) {
        Log.d(TV_TV_TAG, "onEpgProgramSelected: " + program.getTitle());
        // Implement what happens when an EPG program is clicked
    }

    @Override
    public boolean onTvKeyDown(int keyCode, KeyEvent event) {
        Log.d(TV_TV_TAG, "onTvKeyDown: keyCode=" + keyCode);
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_BUTTON_SELECT) {
            if (mChannelGridView != null) {
                if (mChannelGridView.isShown()) {
                    Log.d(TV_TV_TAG, "Hiding ChannelGridView");
                    mChannelGridView.hideChannelGrid();
                } else {
                    Log.d(TV_TV_TAG, "Showing ChannelGridView");
                    mChannelGridView.showChannelGrid();
                    // Ensure ChannelGridView has the latest data
                    if (dataManager != null && dataManager.getLiveStreams() != null && dataManager.getLiveCategoriesMap() != null) {
                        Log.d(TV_TV_TAG, "Setting ChannelGridView data.");
                        mChannelGridView.setChannelsData(dataManager.getLiveStreams(), dataManager.getLiveCategoriesMap());
                    } else {
                        Log.e(TV_TV_TAG, "DataManager or its data is null when trying to set ChannelGridView data!");
                    }
                }
                return true; // Event handled
            }
        }
        return false; // Event not handled
    }

    @Override
    public boolean onTvKeyUp(int keyCode, KeyEvent event) {
        Log.d(TV_TV_TAG, "onTvKeyUp: keyCode=" + keyCode);
        return false; // Not handling key up events for now
    }

    public boolean onBackPressed() {
        Log.d(TV_TV_TAG, "onBackPressed called.");
        if (videoViewTv != null && videoViewTv.isFullScreen()) {
            Log.d(TV_TV_TAG, "Exiting fullscreen video.");
            return videoViewTv.onBackPressed(); // Sair da tela cheia
        }
        if (mChannelGridView != null && mChannelGridView.isShown()) {
            Log.d(TV_TV_TAG, "Hiding ChannelGridView on back press.");
            mChannelGridView.hideChannelGrid();
            return true; // Consumed back press to hide channel grid
        }
        Log.d(TV_TV_TAG, "Back press not handled by fragment.");
        return false; // Permitir que a Activity lide com o back press
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TV_TV_TAG, "onDetach called");
        if (dataManager != null) {
            dataManager.setListener(null); // Corrigido para setListener(null)
        }
    }
}
