package com.example.iptvplayer;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;
import android.widget.FrameLayout;
// import android.widget.ProgressBar; // Original ProgressBar for player is removed from XML
import android.widget.Toast;
import android.widget.TextView; // For title in controller
import android.widget.Button; // For buttons in controller
import android.widget.ImageButton; // For buttons in controller

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.iptvplayer.data.Channel;
import com.example.iptvplayer.data.EpgProgram;
import android.view.KeyEvent;
import com.example.iptvplayer.component.ChannelGridView; // Keep if ChannelGridView itself is a generic view

import com.example.iptvplayer.adapter.ChannelCategoryAdapterTv;
import com.example.iptvplayer.adapter.ChannelAdapterTv;
import com.example.iptvplayer.adapter.EpgAdapterTv;
// import com.example.iptvplayer.component.TitleView; // Removed, title handled by custom controller

// --- ExoPlayer v2 Imports ---
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.PlaybackParameters; // For speed
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.lxj.xpopup.XPopup; // Keep for speed/proportion popups
import android.graphics.Color; // For popups
// --- End ExoPlayer v2 Imports ---


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TvFragmentTv extends Fragment implements DataManager.DataManagerListener, TvKeyHandler.TvKeyListener {

    private static final String TV_TV_TAG = "TV_TV_DEBUG";

    private DataManager dataManager;

    // Views do layout original fragment_tv_tv.xml
    private RecyclerView recyclerViewCategoriesTv;
    private RecyclerView recyclerViewChannelsTv;
    private RecyclerView recyclerViewEpgTv;
    // private FrameLayout playerContainerTv; // PlayerView replaces this in XML
    // private ProgressBar playerProgressBarTv; // Removed from XML, PlayerView has its own

    // --- ExoPlayer v2 Components ---
    private PlayerView playerViewTv;
    private SimpleExoPlayer exoPlayerTv;
    // --- End ExoPlayer v2 Components ---

    // private VideoView videoViewTv; // REMOVED
    // private StandardVideoController videoControllerTv; // REMOVED
    // private TitleView mTitleViewComponent; // REMOVED
    private ChannelGridView mChannelGridView; // Keep if used as a standalone view, not dkplayer component

    // Adapters
    private ChannelCategoryAdapterTv categoryAdapterTv;
    private ChannelAdapterTv channelAdapterTv;
    private EpgAdapterTv epgAdapterTv; // Reintroduzido

    // Listas de dados (se necessário manter no fragmento)
    // private List<Channel> currentChannels = new ArrayList<>();
    // private List<EpgProgram> currentEpgPrograms = new ArrayList<>();


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
        Log.d(TV_TV_TAG, "onCreateView called - Restoring Original TV Layout");
        // 1a. Inflar o layout original da TV
        View root = inflater.inflate(R.layout.fragment_tv_tv, container, false);

        // 1b & 1c. Encontrar views do layout original
        // playerContainerTv = root.findViewById(R.id.tv_player_container_tv); // REMOVED
        // playerProgressBarTv = root.findViewById(R.id.tv_player_progress_bar_tv); // REMOVED

        playerViewTv = root.findViewById(R.id.player_view_tv_fragment_tv_v2); // New PlayerView ID

        recyclerViewCategoriesTv = root.findViewById(R.id.recycler_view_tv_categories_tv);
        recyclerViewChannelsTv = root.findViewById(R.id.recycler_view_tv_channels_tv);
        recyclerViewEpgTv = root.findViewById(R.id.recycler_view_tv_epg_tv); // EPG RecyclerView

        Log.d(TV_TV_TAG, "Calling setupRecyclerViews() for original layout");
        setupRecyclerViews(); // Ajustar para os IDs corretos e reintroduzir EPG

        // 1f. Inicializar a base do player (VideoView e seu listener de estado)
        initializePlayerView();

        Log.d(TV_TV_TAG, "Views initialized for original TV layout.");
        return root;
    }

    private void setupRecyclerViews() {
        Log.d(TV_TV_TAG, "setupRecyclerViews called for original layout");
        if (getContext() == null) {
            Log.e(TV_TV_TAG, "Context is null in setupRecyclerViews!");
            return;
        }
        // Configurar RecyclerView de Categorias
        recyclerViewCategoriesTv.setLayoutManager(new LinearLayoutManager(getContext()));
        categoryAdapterTv = new ChannelCategoryAdapterTv(getContext(), new ArrayList<>(), this::onCategorySelected);
        recyclerViewCategoriesTv.setAdapter(categoryAdapterTv);
        recyclerViewCategoriesTv.setFocusable(true);

        // Configurar RecyclerView de Canais
        recyclerViewChannelsTv.setLayoutManager(new LinearLayoutManager(getContext()));
        channelAdapterTv = new ChannelAdapterTv(getContext(), new ArrayList<>(), this::onChannelSelected);
        recyclerViewChannelsTv.setAdapter(channelAdapterTv);
        recyclerViewChannelsTv.setFocusable(true);
        
        // Configurar RecyclerView de EPG (1c)
        recyclerViewEpgTv.setLayoutManager(new LinearLayoutManager(getContext()));
        epgAdapterTv = new EpgAdapterTv(getContext(), new ArrayList<>(), this::onEpgProgramSelected);
        recyclerViewEpgTv.setAdapter(epgAdapterTv);
        recyclerViewEpgTv.setFocusable(true);

        Log.d(TV_TV_TAG, "RecyclerViews setup complete for original layout.");
    }

    private void initializePlayerView() { // Renamed to initializePlayerV2
        Log.d(TV_TV_TAG, "initializePlayerV2 called");
        if (getContext() == null || playerViewTv == null) {
            Log.e(TV_TV_TAG, "Context or playerViewTv is null in initializePlayerV2.");
            return;
        }

        if (exoPlayerTv == null) { // Create player only if it doesn't exist
            exoPlayerTv = new SimpleExoPlayer.Builder(requireContext()).build();
            playerViewTv.setPlayer(exoPlayerTv);
            playerViewTv.setControllerLayoutId(R.layout.custom_exoplayer2_controls);
            playerViewTv.setControllerShowTimeoutMs(3000); // Example

            exoPlayerTv.addListener(new Player.EventListener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    Log.d(TV_TV_TAG, "ExoPlayer v2 state changed: " + playbackState);
                     View bufferingView = playerViewTv.findViewById(R.id.exo_buffering); // Standard ID
                    if (bufferingView != null) {
                        bufferingView.setVisibility(playbackState == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
                    }
                    // Further UI updates based on state (error, ended etc.) will be similar to VideoPlayerActivity
                    // For example, showing replay button or error messages within the custom controller.
                    updateControllerUiOnErrorStateTv(null); // Clear error on new state
                    updateControllerUiOnEndedStateTv(playbackState == Player.STATE_ENDED);

                }

                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    // Update play/pause button in custom controller if needed
                    updatePlayPauseButtonsTv(isPlaying);
                }

                @Override
                public void onPlayerError(@NonNull ExoPlaybackException error) {
                    Log.e(TV_TV_TAG, "ExoPlayer v2 Error: ", error);
                    updateControllerUiOnErrorStateTv(error);
                }
            });
        }
        Log.d(TV_TV_TAG, "ExoPlayer v2 initialized and attached to PlayerView.");
        setupControllerClickListenersTv(); // Setup listeners for custom controls
    }

    // Removed setupControllerAndComponents as dkplayer controller is gone.
    // Title is set directly. ChannelGridView interaction needs re-evaluation.

    // Lifecycle methods for ExoPlayer v2
    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayerView(); // Changed from initializePlayerV2 to match method name
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TV_TV_TAG, "onResume called - ExoPlayer v2 TV Layout");
        if (Util.SDK_INT <= 23 || exoPlayerTv == null) {
            initializePlayerView(); // Changed from initializePlayerV2
        }
        if (exoPlayerTv != null) {
             if (exoPlayerTv.getPlaybackState() != Player.STATE_IDLE && exoPlayerTv.getPlaybackState() != Player.STATE_ENDED) {
                exoPlayerTv.setPlayWhenReady(true);
            }
        }
        Log.d(TV_TV_TAG, "Calling updateUi() from onResume");
        updateUi();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TV_TV_TAG, "onPause called - ExoPlayer v2 TV Layout");
        if (Util.SDK_INT <= 23) {
            releasePlayerTv();
        } else if (exoPlayerTv != null) {
            exoPlayerTv.setPlayWhenReady(false);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayerTv();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TV_TV_TAG, "onDestroyView called - ExoPlayer v2 TV Layout");
        releasePlayerTv();
    }

    private void releasePlayerTv() {
        if (exoPlayerTv != null) {
            exoPlayerTv.release();
            exoPlayerTv = null;
        }
        if (playerViewTv != null) {
            playerViewTv.setPlayer(null);
        }
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
        // showLoading is primarily for PlayerView's own indicator now.
        // If a fragment-wide loading overlay for data (categories/channels) is needed,
        // it would be a separate ProgressBar in fragment_tv_tv.xml.
        Log.d(TV_TV_TAG, "showLoading (geral for data): " + isLoading);
         // Example: if you add a general ProgressBar R.id.data_loading_progress_bar_tv
        // View generalProgressBar = getView().findViewById(R.id.data_loading_progress_bar_tv);
        // if (generalProgressBar != null) generalProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
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
        // A ProgressBar geral (showLoading) pode ser usada aqui se a busca for longa
        // showLoading(true);

        List<Channel> channels = dataManager.getLiveStreamsByCategory(category.id);
        // showLoading(false);

        if (channels != null) {
            Log.d(TV_TV_TAG, "Updating channelAdapterTv with " + channels.size() + " channels for category: " + category.name);
            if (channelAdapterTv != null) {
                channelAdapterTv.updateData(channels);
                if (channels.isEmpty()) {
                    if (getContext() != null) Toast.makeText(getContext(), "Nenhum canal nesta categoria.", Toast.LENGTH_SHORT).show();
                } else {
                    if (recyclerViewChannelsTv != null) recyclerViewChannelsTv.requestFocus();
                }
            } else {
                Log.e(TV_TV_TAG, "channelAdapterTv is null in onCategorySelected!");
            }
        } else {
            Log.w(TV_TV_TAG, "No channels found for category: " + category.name);
            if (channelAdapterTv != null) {
                channelAdapterTv.updateData(new ArrayList<>()); // Limpar lista
            }
            if (getContext() != null) Toast.makeText(getContext(), "Nenhum canal encontrado para: " + category.name, Toast.LENGTH_SHORT).show();
        }
    }

    // 1d. Modificar onChannelSelected para player embutido (lógica completa no Passo 3)
    private void onChannelSelected(Channel channel) {
        Log.d(TV_TV_TAG, "onChannelSelected for embedded player: " + channel.getName() + ", URL: " + channel.getStreamUrl());

        if (!isAdded() || getContext() == null) {
            Log.e(TV_TV_TAG, "onChannelSelected (ExoPlayer v2): Fragment not added or context is null.");
            return;
        }

        String streamUrl = channel.getStreamUrl();
        if (streamUrl == null || streamUrl.isEmpty()) {
            Log.e(TV_TV_TAG, "Stream URL is null or empty for channel (ExoPlayer v2): " + channel.getName());
            if (getContext() != null) Toast.makeText(getContext(), "URL de stream inválida para " + channel.getName(), Toast.LENGTH_LONG).show();
            if (exoPlayerTv != null) {
                exoPlayerTv.stop(); // Stop if playing invalid stream
            }
            return;
        }

        if (exoPlayerTv == null) {
            Log.e(TV_TV_TAG, "exoPlayerTv is null in onChannelSelected. Initializing.");
            initializePlayerView(); // Ensure player is initialized
             if (exoPlayerTv == null) { // Still null after attempt
                Log.e(TV_TV_TAG, "Failed to initialize exoPlayerTv in onChannelSelected.");
                return;
            }
        }

        Log.d(TV_TV_TAG, "Playing channel with ExoPlayer v2: " + channel.getName());
        exoPlayerTv.stop(); // Stop previous playback

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(requireContext(), Util.getUserAgent(requireContext(), requireContext().getPackageName()));
        Uri videoUri = Uri.parse(streamUrl);
        MediaSource mediaSource;
        if (streamUrl.toLowerCase().endsWith(".m3u8")) {
            mediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(videoUri));
        } else {
            mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(videoUri));
        }
        exoPlayerTv.setMediaSource(mediaSource);
        exoPlayerTv.prepare();
        exoPlayerTv.setPlayWhenReady(true);

        // Update title in custom controller
        if (playerViewTv != null) {
            TextView titleTextView = playerViewTv.findViewById(R.id.exo_custom_title_v2);
            if (titleTextView != null) {
                titleTextView.setText(channel.getName());
            }
        }

        // ChannelGridView setup is removed from here as it's not a dkplayer controller component anymore.
        // Its visibility and data population would be handled by onTvKeyDown or other UI logic.
        // setupControllerAndComponents(channel); // This method is removed.

        Log.d(TV_TV_TAG, "ExoPlayer v2 playback initiated for: " + channel.getName());
        loadEpgForChannel(channel.getStreamId());
    }

    private void loadEpgForChannel(String streamId) {
        Log.d(TV_TV_TAG, "loadEpgForChannel called for streamId: " + streamId);
        if (dataManager == null || dataManager.getXmltvEpgService() == null) {
            Log.e(TV_TV_TAG, "DataManager or XmltvEpgService is null in loadEpgForChannel!");
            return;
        }
        // Mostrar um indicador de carregamento para o EPG se houver um específico
        dataManager.getXmltvEpgService().fetchChannelEpg(streamId, new EpgService.EpgCallback() {
            @Override
            public void onSuccess(List<EpgProgram> programs) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded() || getContext() == null) {
                            Log.w(TV_TV_TAG, "loadEpgForChannel.onSuccess: Fragment not added or context null in UI thread.");
                            return;
                        }
                        Log.d(TV_TV_TAG, "XMLTV EPG loaded successfully: " + programs.size() + " programs for streamId " + streamId);
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
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded() || getContext() == null) {
                            Log.w(TV_TV_TAG, "loadEpgForChannel.onFailure: Fragment not added or context null in UI thread.");
                            return;
                        }
                        Log.e(TV_TV_TAG, "Failed to load XMLTV EPG: " + error);
                        if (getContext() != null) Toast.makeText(getContext(), "Erro ao carregar EPG XMLTV: " + error, Toast.LENGTH_LONG).show();
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
        // Implementar o que acontece quando um programa EPG é clicado.
        // Ex: Mostrar detalhes do programa em um Toast ou Dialog.
        if (getContext() != null) {
            String details = "Programa: " + program.getTitle() + "\nDescrição: " + program.getDescription();
            Toast.makeText(getContext(), details, Toast.LENGTH_LONG).show();
        }
    }

    // onTvKeyDown e onTvKeyUp serão restaurados no Passo 4.
    // Se a navegação por D-Pad precisar de tratamento especial para os RecyclerViews,
    // isso seria feito de forma diferente, geralmente pelo foco do sistema.
    @Override
    public boolean onTvKeyDown(int keyCode, KeyEvent event) {
        Log.d(TV_TV_TAG, "onTvKeyDown: keyCode=" + keyCode);
        // 4a. Adaptar lógica para mostrar/esconder mChannelGridView
        // ChannelGridView is no longer a player component. Its visibility needs to be managed differently.
        // If it's a view in the fragment's layout (it's not currently), you'd toggle its visibility.
        // For now, this D-Pad logic for ChannelGridView needs re-evaluation based on how/if it's used.
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_BUTTON_SELECT) {
            if (mChannelGridView != null && exoPlayerTv != null && exoPlayerTv.isPlaying()) {
                // Assuming mChannelGridView is a standalone view that needs to be shown/hidden.
                // This part requires mChannelGridView to be initialized and part of the fragment's view hierarchy.
                // As of now, mChannelGridView initialization was part of setupControllerAndComponents, which was removed.
                // This logic is currently non-functional without mChannelGridView being properly managed.
                // Placeholder:
                // if (mChannelGridView.getVisibility() == View.VISIBLE) {
                //     mChannelGridView.setVisibility(View.GONE);
                // } else {
                //    // Populate mChannelGridView data if needed
                //    mChannelGridView.setVisibility(View.VISIBLE);
                // }
                // return true;
                Toast.makeText(getContext(), "Channel Grid (D-Pad) TBD", Toast.LENGTH_SHORT).show();
            }
        }
        // Considerar adicionar navegação para cima/baixo na lista de canais se o foco estiver no player
        // ou outras interações de D-Pad específicas do player.
        return false; // Deixar o sistema tratar outros eventos de D-Pad
    }

    @Override
    public boolean onTvKeyUp(int keyCode, KeyEvent event) {
        // Log.d(TV_TV_TAG, "onTvKeyUp: keyCode=" + keyCode); // Geralmente não necessário
        return false;
    }

    public boolean onBackPressed() {
        Log.d(TV_TV_TAG, "onBackPressed called - ExoPlayer v2 Logic");
        // Fullscreen handling for PlayerView on TV needs a defined strategy.
        // If PlayerView's controller manages fullscreen (e.g. by activity requesting fullscreen),
        // then this might not need explicit handling here, or it might.
        // For ChannelGridView, if it's an overlay, its hiding logic would go here.
        // if (mChannelGridView != null && mChannelGridView.getVisibility() == View.VISIBLE) {
        //      mChannelGridView.setVisibility(View.GONE);
        //      return true;
        // }
        Log.d(TV_TV_TAG, "Back press not explicitly handled by TvFragmentTv (ExoPlayer v2). Returning false.");
        return false;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TV_TV_TAG, "onDetach called");
        if (dataManager != null) {
            dataManager.setListener(null);
        }
    }

    // +++ Helper methods for ExoPlayer v2 UI in TV Fragment +++
    private void setupControllerClickListenersTv() {
        if (playerViewTv == null) return;

        // Example: Back button in controller
        ImageButton ctrlBackButton = playerViewTv.findViewById(R.id.exo_custom_back_button_v2);
        if (ctrlBackButton != null) {
            ctrlBackButton.setOnClickListener(v -> {
                if (getActivity() != null) getActivity().onBackPressed();
            });
        }

        // Retry button
        Button ctrlRetryButton = playerViewTv.findViewById(R.id.exo_custom_retry_button_v2);
        if (ctrlRetryButton != null) {
            ctrlRetryButton.setOnClickListener(v -> {
                updateControllerUiOnErrorStateTv(null); // Hide error
                updateControllerUiOnBufferingStateTv(true); // Show buffering
                if (exoPlayerTv != null && mCurrentPlayingUrl != null) {
                    exoPlayerTv.stop();
                    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(requireContext(), Util.getUserAgent(requireContext(), requireContext().getPackageName()));
                    MediaSource mediaSource = mCurrentPlayingUrl.toLowerCase().endsWith(".m3u8") ?
                            new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(mCurrentPlayingUrl)) :
                            new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(mCurrentPlayingUrl));
                    exoPlayerTv.setMediaSource(mediaSource);
                    exoPlayerTv.prepare();
                    exoPlayerTv.setPlayWhenReady(true);
                } else {
                    updateControllerUiOnBufferingStateTv(false);
                }
            });
        }

        // Replay button
        ImageButton ctrlReplayButton = playerViewTv.findViewById(R.id.exo_custom_replay_button_v2);
        if (ctrlReplayButton != null) {
            ctrlReplayButton.setOnClickListener(v -> {
                if (exoPlayerTv != null) {
                    exoPlayerTv.seekTo(0);
                    exoPlayerTv.setPlayWhenReady(true);
                    ctrlReplayButton.setVisibility(View.GONE);
                }
            });
        }

        // Bottom Play/Pause
        ImageButton ctrlPlayPauseBottom = playerViewTv.findViewById(R.id.exo_play_pause_bottom_v2);
        if (ctrlPlayPauseBottom != null) {
            ctrlPlayPauseBottom.setOnClickListener(v -> {
                if (exoPlayerTv != null) {
                    exoPlayerTv.setPlayWhenReady(!exoPlayerTv.getPlayWhenReady());
                }
            });
        }

        // Speed Button (using XPopup, needs CustomDrawerPopupView adapted for ExoPlayer v2)
        ImageButton ctrlSpeedButton = playerViewTv.findViewById(R.id.exo_custom_speed_button_v2);
        if (ctrlSpeedButton != null) {
            ctrlSpeedButton.setOnClickListener(v -> {
                if (getContext() != null) {
                    new XPopup.Builder(getContext())
                            .popupPosition(com.lxj.xpopup.enums.PopupPosition.Right)
                            .asCustom(new CustomDrawerPopupViewTv(getContext())) // Use TvFragmentTv's own version
                            .show();
                }
            });
        }

        // Proportion Button (using XPopup, needs CustomDrawerPopupView1 adapted for ExoPlayer v2)
        ImageButton ctrlProportionButton = playerViewTv.findViewById(R.id.exo_custom_proportion_button_v2);
        if (ctrlProportionButton != null) {
            ctrlProportionButton.setOnClickListener(v -> {
                if (getContext() != null) {
                     new XPopup.Builder(getContext())
                            .popupPosition(com.lxj.xpopup.enums.PopupPosition.Right)
                            .asCustom(new CustomDrawerPopupView1Tv(getContext()))  // Use TvFragmentTv's own version
                            .show();
                }
            });
        }
         // Refresh Button
        ImageButton ctrlRefreshButton = playerViewTv.findViewById(R.id.exo_custom_refresh_button_v2);
        if (ctrlRefreshButton != null) {
            ctrlRefreshButton.setOnClickListener(v -> {
                 if (exoPlayerTv != null && mCurrentPlayingUrl != null) {
                     exoPlayerTv.stop();
                    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(requireContext(), Util.getUserAgent(requireContext(), requireContext().getPackageName()));
                    MediaSource mediaSource = mCurrentPlayingUrl.toLowerCase().endsWith(".m3u8") ?
                            new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(mCurrentPlayingUrl)) :
                            new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(mCurrentPlayingUrl));
                    exoPlayerTv.setMediaSource(mediaSource);
                    exoPlayerTv.prepare();
                    exoPlayerTv.setPlayWhenReady(true);
                 }
            });
        }
    }

    private void updatePlayPauseButtonsTv(boolean isPlaying) {
        if (playerViewTv == null) return;
        ImageButton playButton = playerViewTv.findViewById(R.id.exo_play);
        ImageButton pauseButton = playerViewTv.findViewById(R.id.exo_pause);
        ImageButton playPauseBottom = playerViewTv.findViewById(R.id.exo_play_pause_bottom_v2);

        if (playButton != null) playButton.setVisibility(isPlaying ? View.GONE : View.VISIBLE);
        if (pauseButton != null) pauseButton.setVisibility(isPlaying ? View.VISIBLE : View.GONE);
        if (playPauseBottom != null) {
            playPauseBottom.setImageResource(isPlaying ? R.drawable.dkplayer_ic_action_pause : R.drawable.dkplayer_ic_action_play_arrow);
        }
    }

    private void updateControllerUiOnErrorStateTv(ExoPlaybackException error) {
        if (playerViewTv == null) return;
        View loadingIndicator = playerViewTv.findViewById(R.id.exo_buffering);
        View errorViewFromController = playerViewTv.findViewById(R.id.exo_custom_error_view_v2);
        TextView errorMessageTextView = playerViewTv.findViewById(R.id.exo_custom_error_message_v2);
        Button ctrlRetryButton = playerViewTv.findViewById(R.id.exo_custom_retry_button_v2);

        if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);

        if (error != null) {
            if (errorViewFromController != null) errorViewFromController.setVisibility(View.VISIBLE);
            if (errorMessageTextView != null) errorMessageTextView.setText(error.getLocalizedMessage() != null ? error.getLocalizedMessage() : "An unknown error occurred.");
            if (ctrlRetryButton != null) ctrlRetryButton.setEnabled(true);
            playerViewTv.hideController();
        } else {
            if (errorViewFromController != null) errorViewFromController.setVisibility(View.GONE);
        }
    }

    private void updateControllerUiOnEndedStateTv(boolean hasEnded) {
        if (playerViewTv == null) return;
        View ctrlReplayButton = playerViewTv.findViewById(R.id.exo_custom_replay_button_v2);
        if (ctrlReplayButton != null) {
            ctrlReplayButton.setVisibility(hasEnded ? View.VISIBLE : View.GONE);
        }
        if (hasEnded) {
            playerViewTv.showController();
        }
    }

    private void updateControllerUiOnBufferingStateTv(boolean isBuffering) {
        if (playerViewTv == null) return;
        View loadingIndicator = playerViewTv.findViewById(R.id.exo_buffering);
        View errorViewFromController = playerViewTv.findViewById(R.id.exo_custom_error_view_v2);
        View ctrlReplayButton = playerViewTv.findViewById(R.id.exo_custom_replay_button_v2);

        if (loadingIndicator != null) loadingIndicator.setVisibility(isBuffering ? View.VISIBLE : View.GONE);
        if (isBuffering) {
            if (errorViewFromController != null) errorViewFromController.setVisibility(View.GONE);
            if (ctrlReplayButton != null) ctrlReplayButton.setVisibility(View.GONE);
        }
    }

    private void updatePiPIfNeededV2(int playbackState, boolean isPlaying) {
        // PiP logic is not present in TvFragmentTv.java originally, so this is a placeholder
        // if it were to be added. For now, this can be minimal or empty.
    }
    // End Helper methods

    //region Popups for Speed and Proportion (Duplicated and Adapted for TvFragmentTv)
    public class CustomDrawerPopupViewTv extends com.lxj.xpopup.core.DrawerPopupView {
        public CustomDrawerPopupViewTv(@androidx.annotation.NonNull Context context) {
            super(context);
        }
        @Override
        protected int getImplLayoutId() {
            return R.layout.speed;
        }
        @Override
        protected void onCreate() {
            super.onCreate();

            final TextView txt1 = findViewById(R.id.textview1);
            final TextView txt2 = findViewById(R.id.textview2);
            final TextView txt3 = findViewById(R.id.textview3);
            final TextView txt4 = findViewById(R.id.textview4);
            final TextView txt5 = findViewById(R.id.textview5);

            if (exoPlayerTv != null) {
                float currentSpeed = exoPlayerTv.getPlaybackParameters().speed;
                txt1.setTextColor(Color.parseColor(currentSpeed == 0.75f ? "#FF39C5BC" : "#ffffff"));
                txt2.setTextColor(Color.parseColor(currentSpeed == 1.0f ? "#FF39C5BC" : "#ffffff"));
                txt3.setTextColor(Color.parseColor(currentSpeed == 1.25f ? "#FF39C5BC" : "#ffffff"));
                txt4.setTextColor(Color.parseColor(currentSpeed == 1.5f ? "#FF39C5BC" : "#ffffff"));
                txt5.setTextColor(Color.parseColor(currentSpeed == 2.0f ? "#FF39C5BC" : "#ffffff"));
            }

            findViewById(R.id.cardview1).setOnClickListener(v1 -> {
                if (exoPlayerTv != null) exoPlayerTv.setPlaybackParameters(new PlaybackParameters(0.75f));
                updateSpeedPopupSelectionV2(txt1, txt2, txt3, txt4, txt5); // Use existing helper from TvFragment, or duplicate
                dismiss();
            });
            findViewById(R.id.cardview2).setOnClickListener(v1 -> {
                if (exoPlayerTv != null) exoPlayerTv.setPlaybackParameters(new PlaybackParameters(1.0f));
                updateSpeedPopupSelectionV2(txt2, txt1, txt3, txt4, txt5);
                dismiss();
            });
            findViewById(R.id.cardview3).setOnClickListener(v1 -> {
                if (exoPlayerTv != null) exoPlayerTv.setPlaybackParameters(new PlaybackParameters(1.25f));
                updateSpeedPopupSelectionV2(txt3, txt1, txt2, txt4, txt5);
                dismiss();
            });
            findViewById(R.id.cardview4).setOnClickListener(v1 -> {
                if (exoPlayerTv != null) exoPlayerTv.setPlaybackParameters(new PlaybackParameters(1.5f));
                updateSpeedPopupSelectionV2(txt4, txt1, txt2, txt3, txt5);
                dismiss();
            });
            findViewById(R.id.cardview5).setOnClickListener(v1 -> {
                if (exoPlayerTv != null) exoPlayerTv.setPlaybackParameters(new PlaybackParameters(2.0f));
                updateSpeedPopupSelectionV2(txt5, txt1, txt2, txt3, txt4);
                dismiss();
            });
        }

        private void updateSpeedPopupSelectionV2(TextView selected, TextView... others) {
            selected.setTextColor(Color.parseColor("#FF39C5BC"));
            for (TextView other : others) {
                other.setTextColor(Color.parseColor("#ffffff"));
            }
        }
    }

    public class CustomDrawerPopupView1Tv extends com.lxj.xpopup.core.DrawerPopupView {
        public CustomDrawerPopupView1Tv(@androidx.annotation.NonNull Context context) {
            super(context);
        }
        @Override
        protected int getImplLayoutId() {
            return R.layout.proportion;
        }
        @Override
        protected void onCreate() {
            super.onCreate();
            final TextView txt1 = findViewById(R.id.textview1);
            final TextView txt2 = findViewById(R.id.textview2);
            final TextView txt3 = findViewById(R.id.textview3);
            final TextView txt4 = findViewById(R.id.textview4);
            final TextView txt5 = findViewById(R.id.textview5);

            if (playerViewTv != null) {
                int currentMode = playerViewTv.getResizeMode();
                updateProportionPopupSelectionVisualsV2(currentMode, txt1, txt2, txt3, txt4, txt5);
            }

            findViewById(R.id.cardview1).setOnClickListener(v1 -> { // Default
                if (playerViewTv != null) playerViewTv.setResizeMode(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                updateProportionPopupSelectionVisualsV2(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT, txt1, txt2, txt3, txt4, txt5);
                dismiss();
            });
            findViewById(R.id.cardview2).setOnClickListener(v1 -> { // 16:9 -> Fixed Width
                if (playerViewTv != null) playerViewTv.setResizeMode(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);
                updateProportionPopupSelectionVisualsV2(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH, txt1, txt2, txt3, txt4, txt5);
                dismiss();
            });
            findViewById(R.id.cardview3).setOnClickListener(v1 -> { // Original -> FIT
                if (playerViewTv != null) playerViewTv.setResizeMode(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                updateProportionPopupSelectionVisualsV2(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT, txt1, txt2, txt3, txt4, txt5);
                dismiss();
            });
            findViewById(R.id.cardview4).setOnClickListener(v1 -> { // Fill
                if (playerViewTv != null) playerViewTv.setResizeMode(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL);
                updateProportionPopupSelectionVisualsV2(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL, txt1, txt2, txt3, txt4, txt5);
                dismiss();
            });
            findViewById(R.id.cardview5).setOnClickListener(v1 -> { // Center Crop -> Zoom
                if (playerViewTv != null) playerViewTv.setResizeMode(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                updateProportionPopupSelectionVisualsV2(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM, txt1, txt2, txt3, txt4, txt5);
                dismiss();
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
    //endregion
}
