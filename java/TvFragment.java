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
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.DrawableRes;
import androidx.fragment.app.Fragment;
import xyz.doikki.videoplayer.player.VideoView;
import com.example.iptvplayer.StandardVideoController;
import android.widget.LinearLayout;
import android.content.BroadcastReceiver;
import com.example.iptvplayer.component.CompleteView;
import com.example.iptvplayer.component.ErrorView;
import com.example.iptvplayer.component.GestureView;
import com.example.iptvplayer.component.PrepareView;
import com.example.iptvplayer.component.TitleView;
import com.example.iptvplayer.component.VodControlView;
import com.example.iptvplayer.component.ChannelGridView;
import com.example.iptvplayer.component.LiveControlView; // ADDED IMPORT
import com.lxj.xpopup.XPopup;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
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

    private ProgressBar playerProgressBar; // Conectado ao XML
    private TextView playerLoadingTextView; // Para a mensagem "Carregando"

    private VideoView mVideoView;
    private StandardVideoController mController;
    private TitleView mTitleViewComponent; // Referência ao componente TitleView
    private ChannelGridView mChannelGridView; // Componente da grade de canais
    private GestureView mGestureView; // Componente de gestos
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

    private String speed = "1.0";
    private String proportion = "默认";
    private String title = "测试标题";

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

    // Variáveis para monitoramento de estagnação
    private Handler mStagnationCheckHandler;
    private Runnable mStagnationCheckRunnable;
    private long mLastPlaybackPosition = 0;
    private long mLastPositionCheckTime = 0;
    private static final int STAGNATION_THRESHOLD_MS = 20000; // 20 segundos
    private static final int POSITION_CHECK_INTERVAL_MS = 5000; // Verificar a cada 5 segundos


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
        playerProgressBar = root.findViewById(R.id.player_progress_bar); // Conectar ProgressBar
        playerLoadingTextView = root.findViewById(R.id.player_loading_text); // Conectar TextView de Loading

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

        // New player initialization
        mPictureInPictureParamsBuilder = new PictureInPictureParams.Builder();

        mVideoView = new xyz.doikki.videoplayer.player.VideoView(getContext());
        FrameLayout playerContainer = root.findViewById(R.id.player_container);
        if (playerContainer != null) {
            // Fazer o VideoView preencher o player_container
            FrameLayout.LayoutParams videoViewParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            mVideoView.setLayoutParams(videoViewParams);
            playerContainer.addView(mVideoView); // Adicionar ao FrameLayout
        } else {
            Log.e(TV_TAG, "player_container FrameLayout not found in fragment_tv.xml");
            Toast.makeText(getContext(), getString(R.string.player_container_not_found_error), Toast.LENGTH_LONG).show();
        }
        mWidthPixels = getResources().getDisplayMetrics().widthPixels;
        

        mController = new StandardVideoController(getContext());
        mController.addControlComponent(new CompleteView(getContext()));
        // mController.addControlComponent(new ErrorView(getContext())); // Removido para que nossa lógica de retentativa seja a única a lidar com erros.
        mController.addControlComponent(new PrepareView(getContext()));
        
        // Configurar GestureView com listener para clique no lado esquerdo
        mGestureView = new GestureView(getContext());

        // Configurar ChannelGridView
        mChannelGridView = new ChannelGridView(getContext());
        mChannelGridView.setChannelSelectedListener(channel -> {
            // Quando um canal é selecionado na grade, reproduzi-lo
            onChannelClick(channel);
        });
        mController.addControlComponent(mChannelGridView);
        VodControlView vodControlView = new VodControlView(getContext());
        vodControlView.findViewById(R.id.speed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new XPopup.Builder(getContext())
                        .popupPosition(com.lxj.xpopup.enums.PopupPosition.Right)
                        .asCustom(new CustomDrawerPopupView(getContext()))
                        .show();
            }
        });
        vodControlView.findViewById(R.id.proportion).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new XPopup.Builder(getContext())
                        .popupPosition(com.lxj.xpopup.enums.PopupPosition.Right)
                        .asCustom(new CustomDrawerPopupView1(getContext()))
                        .show();
            }
        });
        // mController.addControlComponent(vodControlView); // REPLACED with LiveControlView

        LiveControlView liveControlView = new LiveControlView(getContext()); // ADDED
        if (mChannelGridView != null) { // Ensure mChannelGridView is initialized
            liveControlView.setChannelGridViewRef(mChannelGridView); // Set the reference
        }
        liveControlView.setOnProportionButtonClickListener(this); // Definir o listener
        mController.addControlComponent(liveControlView); // ADDED


        // Criar e configurar o TitleView, depois armazenar a referência
        mTitleViewComponent = new TitleView(getContext());
        mTitleViewComponent.findViewById(R.id.pip).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Rational aspectRatio = new Rational(16, 9);
                mPictureInPictureParamsBuilder.setAspectRatio(aspectRatio); // Corrigido: .build() removido daqui
                if (getActivity() != null) {
                    getActivity().enterPictureInPictureMode(mPictureInPictureParamsBuilder.build());
                }
            }
        });
        mTitleViewComponent.setTitle(title); // Define um título inicial
        mController.addControlComponent(mTitleViewComponent);

        mVideoView.setVideoController(mController);
        mVideoView.addOnStateChangeListener(new VideoView.SimpleOnStateChangeListener() {
            @Override

            public void onPlayStateChanged(int playState) {
                String currentPlayStateString = playStateToString(playState);
                boolean isFullScreen = (mVideoView != null && mVideoView.isFullScreen());
                Log.d(TAG_PLAYER_STATE_TV, "Player state event: " + currentPlayStateString + ". IsFullScreen: " + isFullScreen + ". SwitchingChannels: " + mIsSwitchingChannels + ". PiP: " + (getActivity() != null && getActivity().isInPictureInPictureMode()));

                // Se um novo vídeo começou a tocar ou está pronto (ou erro), a troca terminou.
                if (playState == VideoView.STATE_PLAYING || playState == VideoView.STATE_PREPARED || playState == VideoView.STATE_ERROR) {
                    if(mIsSwitchingChannels) {
                        Log.d(TV_TAG, "Transition to PLAYING/PREPARED/ERROR, resetting mIsSwitchingChannels for new stream.");
                    }
                    mIsSwitchingChannels = false;
                }

                // Não atualize as ações PiP se for um evento de pausa do vídeo antigo durante uma troca de canal.
                if (mIsSwitchingChannels && playState == VideoView.STATE_PAUSED) {
                    Log.d(TV_TAG, "PAUSED state during channel switch, likely old stream. PiP actions update SKIPPED.");
                    return;
                }
                 if (mIsSwitchingChannels && (playState == VideoView.STATE_IDLE || playState == VideoView.STATE_PREPARING)) {
                    Log.d(TV_TAG, "IDLE/PREPARING state during channel switch. PiP actions update SKIPPED.");
                    // Não faz sentido atualizar PiP se o player está idle ou preparando DURANTE uma troca.
                    // Isso pode acontecer se release() for chamado, e o player passar por IDLE.
                    return;
                }


                switch (playState) {
                    case VideoView.STATE_PAUSED:
                        updatePictureInPictureActions(
                                R.drawable.dkplayer_ic_action_play_arrow, getString(R.string.pip_action_play), CONTROL_TYPE_PLAY, REQUEST_PLAY);
                        break;
                    case VideoView.STATE_PLAYING:
                        // Quando estiver tocando, e não trocando de canal, reseta o flag (segurança extra).
                        // mIsSwitchingChannels = false; // Já tratado acima, mas pode ser uma garantia.
                        updatePictureInPictureActions(
                                R.drawable.dkplayer_ic_action_pause, getString(R.string.pip_action_pause), CONTROL_TYPE_PAUSE, REQUEST_PAUSE);
                        break;
                    case VideoView.STATE_PLAYBACK_COMPLETED:
                        updatePictureInPictureActions(
                                R.drawable.dkplayer_ic_action_replay, getString(R.string.pip_action_replay), CONTROL_TYPE_REPLAY, REQUEST_REPLAY);
                        break;
                    // Outros estados como STATE_PREPARING, STATE_BUFFERING podem ser usados para mostrar/esconder o ProgressBar
                    case VideoView.STATE_PREPARING:
                        Log.d(TV_TAG, "Player is PREPARING.");
                        showLoading(true);
                        mBufferingStartTime = 0; // Resetar início de buffering ao preparar novo stream
                        mBufferOptimizeHandler.removeCallbacks(mCheckBufferingRunnable);
                        mBufferOptimizeHandler.removeCallbacks(mResumeAfterBufferRebuildRunnable);
                        mIsPausedForBuffering = false;
                        break;
                    case VideoView.STATE_BUFFERING:
                        Log.d(TV_TAG, "Player is BUFFERING.");
                        if (!mIsPausedForBuffering && !mIsRetrying) { // Só iniciar lógica de buffering excessivo se não estiver pausado por nós ou em retentativa de erro
                            if (mBufferingStartTime == 0) { // Marcar início do buffering
                                mBufferingStartTime = System.currentTimeMillis();
                                Log.d(TV_TAG, "Buffering started at: " + mBufferingStartTime);
                                // Postar verificação inicial, mCheckBufferingRunnable se auto-reagenda depois
                                mBufferOptimizeHandler.postDelayed(mCheckBufferingRunnable, 1000); // Verificar após 1s
                            }
                            // Mostrar "Carregando..." ou similar, mas não a mensagem de "Otimizando buffer" ainda
                           if (!playerLoadingTextView.getText().equals(getString(R.string.optimizing_buffer_message))) {
                                showLoading(true); // Usa a mensagem padrão de loading
                            }
                        }
                        break;
                    case VideoView.STATE_PREPARED: // Vídeo preparado
                        Log.d(TV_TAG, "Player is PREPARED.");
                        mBufferingStartTime = 0; // Resetar, pois o buffer foi preparado
                        mBufferOptimizeHandler.removeCallbacks(mCheckBufferingRunnable);
                        // Não esconder loading aqui, esperar PLAYING ou PAUSED
                        break;
                    case VideoView.STATE_BUFFERED: // Buffering completo
                        Log.d(TV_TAG, "Player is BUFFERED (finished buffering).");
                        mBufferingStartTime = 0; // Resetar, pois o buffering terminou
                        mBufferOptimizeHandler.removeCallbacks(mCheckBufferingRunnable);
                        // Não esconder loading aqui, esperar PLAYING ou PAUSED
                        break;
                    case VideoView.STATE_ERROR:
                        showLoading(false); // Esconder loading em caso de erro
                        mBufferingStartTime = 0;
                        mBufferOptimizeHandler.removeCallbacks(mCheckBufferingRunnable);
                        mBufferOptimizeHandler.removeCallbacks(mResumeAfterBufferRebuildRunnable);
                        mIsPausedForBuffering = false;
                        // ErrorView já deve estar sendo exibido pelo controller
                        if (mVideoView != null && mCurrentPlayingUrl != null && !mCurrentPlayingUrl.isEmpty()) {
                            Log.e(TV_TAG, "Error playing channel: " + mCurrentPlayingChannelName + ". Initiating retry mechanism.");
                            mIsRetrying = true;
                            showLoadingWithMessage(getString(R.string.retrying_channel_message, mCurrentPlayingChannelName));
                            mRetryHandler.postDelayed(mRetryRunnable, RETRY_DELAY_MS);
                        }
                        break;
                }

                // Lógica de esconder loading
                if (playState == VideoView.STATE_PLAYING || playState == VideoView.STATE_PAUSED || playState == VideoView.STATE_PLAYBACK_COMPLETED) {
                    if (mIsPausedForBuffering && playState == VideoView.STATE_PAUSED) {
                        // Se foi pausado por nós para otimizar buffer, manter a mensagem de otimização.
                        // showLoadingWithMessage(getString(R.string.optimizing_buffer_message)); // Já deve estar setada
                    } else if (!mIsRetrying) {
                        // Esconder loading se não estivermos retentando um erro fatal e não estivermos explicitamente pausados para buffer.
                        showLoading(false);
                    }
                     // Se estava pausado para buffer e agora está tocando, resetar flags.
                    if (mIsPausedForBuffering && playState == VideoView.STATE_PLAYING) {
                        mIsPausedForBuffering = false;
                        mBufferingStartTime = 0; // Reset
                        mBufferOptimizeHandler.removeCallbacks(mResumeAfterBufferRebuildRunnable); // Cancelar se já estiver agendado
                    }
                }


                // Se começou a tocar e estávamos em retentativa de ERRO, cancelar o estado de retentativa.
                if (playState == VideoView.STATE_PLAYING && mIsRetrying) {
                    Log.d(TV_TAG, "Channel " + mCurrentPlayingChannelName + " started playing successfully after ERROR retry.");
                    mIsRetrying = false;
                    mRetryHandler.removeCallbacks(mRetryRunnable);
                    showLoading(false);
                }

                // Gerenciamento do StagnationCheckRunnable
                if (playState == VideoView.STATE_PLAYING) {
                    Log.d(TV_TAG, "Player is PLAYING. Starting/resetting stagnation check.");
                    mLastPlaybackPosition = mVideoView.getCurrentPosition(); // Iniciar com a posição atual
                    mLastPositionCheckTime = System.currentTimeMillis();   // Marcar o tempo atual
                    mStagnationCheckHandler.removeCallbacks(mStagnationCheckRunnable); // Remover callbacks anteriores
                    mStagnationCheckHandler.postDelayed(mStagnationCheckRunnable, POSITION_CHECK_INTERVAL_MS);
                } else if (playState == VideoView.STATE_PAUSED ||
                           playState == VideoView.STATE_PLAYBACK_COMPLETED ||
                           playState == VideoView.STATE_ERROR ||
                           playState == VideoView.STATE_IDLE) {
                    Log.d(TV_TAG, "Player not PLAYING (state: " + playStateToString(playState) + "). Stopping stagnation check.");
                    mStagnationCheckHandler.removeCallbacks(mStagnationCheckRunnable);
                    mLastPlaybackPosition = 0; // Resetar para não causar falsa detecção na próxima vez
                    mLastPositionCheckTime = 0;
                }


                // Se o player for para IDLE (ex: após release), limpar tudo
                if (playState == VideoView.STATE_IDLE) {
                    Log.d(TV_TAG, "Player is IDLE. Clearing buffering and retry states.");
                    mBufferingStartTime = 0;
                    mBufferOptimizeHandler.removeCallbacks(mCheckBufferingRunnable);
                    mBufferOptimizeHandler.removeCallbacks(mResumeAfterBufferRebuildRunnable);
                    mIsPausedForBuffering = false;
                    // Não mexer em mIsRetrying aqui, pois o release pode ser parte da retentativa.
                    // mIsRetrying é resetado em onChannelClick ou quando a retentativa é bem sucedida.
                    // Stagnation check já é parado pela lógica acima.
                }
            }
        });

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
                if (mIsRetrying && mVideoView != null && mCurrentPlayingUrl != null && !mCurrentPlayingUrl.isEmpty()) {
                    Log.d(TV_TAG, "Retrying channel: " + mCurrentPlayingChannelName + " URL: " + mCurrentPlayingUrl);
                    showLoadingWithMessage(getString(R.string.retrying_channel_message, mCurrentPlayingChannelName));
                    mVideoView.release(); // Libera recursos do player anterior
                    mVideoView.setUrl(mCurrentPlayingUrl);
                    mVideoView.start();
                    // O listener onPlayStateChanged lidará com o próximo estado (PREPARING, PLAYING ou ERROR novamente)
                } else {
                    Log.d(TV_TAG, "Retry condition not met or retry cancelled.");
                    mIsRetrying = false; // Garante que o estado de retentativa seja resetado se não for mais necessário
                    showLoading(false); // Esconde o loading se a retentativa foi cancelada ou não pôde prosseguir
                }
            }
        };

        // Inicializar Handler e Runnables para otimização de buffer
        mBufferOptimizeHandler = new Handler(Looper.getMainLooper());
        mCheckBufferingRunnable = new Runnable() {
            @Override
            public void run() {
                if (mVideoView != null && mVideoView.getCurrentPlayState() == VideoView.STATE_BUFFERING &&
                    mBufferingStartTime > 0 && !mIsPausedForBuffering) {
                    long elapsedBufferingTime = System.currentTimeMillis() - mBufferingStartTime;
                    if (elapsedBufferingTime >= MAX_BUFFERING_TIME_MS) {
                        Log.d(TV_TAG, "Max buffering time reached. Pausing to rebuild buffer.");
                        mIsPausedForBuffering = true;
                        mVideoView.pause();
                        showLoadingWithMessage(getString(R.string.optimizing_buffer_message)); // NECESSÁRIO ADICIONAR STRING
                        mBufferOptimizeHandler.postDelayed(mResumeAfterBufferRebuildRunnable, PAUSE_FOR_BUFFER_REBUILD_MS);
                    } else {
                        // Re-agendar a verificação se ainda estiver em buffering e o tempo não esgotou
                        mBufferOptimizeHandler.postDelayed(this, 1000); // Verificar a cada segundo
                    }
                } else {
                     // Se não estiver mais em buffering ou condições não atendidas, para de verificar.
                    mBufferingStartTime = 0; // Reseta para evitar re-checagem desnecessária
                }
            }
        };

        mResumeAfterBufferRebuildRunnable = new Runnable() {
            @Override
            public void run() {
                if (mIsPausedForBuffering && mVideoView != null) {
                    Log.d(TV_TAG, "Resuming playback after buffer rebuild pause.");
                    // Não precisa mais da mensagem "Otimizando...", o player vai para buffering/playing
                    // showLoadingWithMessage(getString(R.string.resuming_playback_message)); // NECESSÁRIO ADICIONAR STRING
                    showLoading(true); // Mostrar loading padrão ao tentar resumir
                    mVideoView.resume(); // Tentar resumir. O player passará por seus estados normais.
                    mIsPausedForBuffering = false;
                    // mBufferingStartTime será resetado quando o estado mudar para PLAYING ou PAUSED (não por nós)
                }
            }
        };

        // Inicializar Handler e Runnable para monitoramento de estagnação
        mStagnationCheckHandler = new Handler(Looper.getMainLooper());
        mStagnationCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (mVideoView != null && (mVideoView.getCurrentPlayState() == VideoView.STATE_PLAYING || mVideoView.getCurrentPlayState() == VideoView.STATE_BUFFERING)) {
                    long currentPosition = mVideoView.getCurrentPosition();
                    long currentTime = System.currentTimeMillis();

                    // Log para depuração
                    // Log.d(TV_TAG, "StagnationCheck: currentPos=" + currentPosition + ", lastPos=" + mLastPlaybackPosition + ", currentTime=" + currentTime + ", lastCheckTime=" + mLastPositionCheckTime);

                    if (mLastPositionCheckTime > 0 && currentPosition == mLastPlaybackPosition) { // Verifica se a posição não mudou desde a última checagem válida
                        if (currentTime - mLastPositionCheckTime >= STAGNATION_THRESHOLD_MS) {
                            Log.w(TV_TAG, "Stagnation detected! Current position " + currentPosition + " hasn't changed in " + STAGNATION_THRESHOLD_MS + "ms. Re-initializing stream.");
                            showLoadingWithMessage(getString(R.string.reestablishing_connection_message)); // NECESSÁRIO ADICIONAR STRING

                            if (mCurrentPlayingUrl != null && !mCurrentPlayingUrl.isEmpty()) {
                                mVideoView.release();
                                mVideoView.setUrl(mCurrentPlayingUrl);
                                mVideoView.start();
                            }
                            // Resetar contadores após tentativa de reinicialização para dar chance ao stream
                            mLastPlaybackPosition = 0; // Resetar para a próxima vez que PLAYING for atingido
                            mLastPositionCheckTime = 0; // Resetar para a próxima vez que PLAYING for atingido
                            // Não reagendar aqui; onPlayStateChanged(STATE_PLAYING) o fará se a reinicialização for bem-sucedida.
                            return; // Sair do runnable após detectar estagnação e agir
                        }
                    } else {
                        // A posição mudou ou é a primeira verificação útil após o início/retomada
                        mLastPlaybackPosition = currentPosition;
                        mLastPositionCheckTime = currentTime;
                    }
                    // Reagendar a próxima verificação
                    mStagnationCheckHandler.postDelayed(this, POSITION_CHECK_INTERVAL_MS);
                } else {
                    // O player não está em PLAYING ou BUFFERING, então paramos de verificar.
                    Log.d(TV_TAG, "StagnationCheck: Player not in PLAYING/BUFFERING state. Stopping checks.");
                    mLastPlaybackPosition = 0;
                    mLastPositionCheckTime = 0;
                }
            }
        };
        return root;
    }

    @Override
    public void onProgressUpdate(DataManager.LoadState state, int percentage, String message) {
        Log.d(TV_TAG, "DataManager Progress: " + state + " - " + percentage + "% - " + message);
        if (state == DataManager.LoadState.COMPLETE) {
            showLoading(false);
        } else {
            showLoading(true);
        }
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
            showLoading(false);
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
            showLoading(true);
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
                            
                            showLoading(false);
                            
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
                            showLoading(false);
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
            showLoading(false);
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
        Log.d(TV_TAG, "onChannelClick: " + channel.getName() + " URL: " + channel.getStreamUrl());
        if (mVideoView == null) {
            Log.e(TV_TAG, "Player not initialized in onChannelClick");
            Toast.makeText(getContext(), getString(R.string.player_not_initialized_error), Toast.LENGTH_SHORT).show();
            return;
        }
        if (channel.getStreamUrl() != null && !channel.getStreamUrl().isEmpty()) {
            mIsSwitchingChannels = true; // Sinaliza o início da troca de canal
            showLoading(true); // Mostrar loading imediatamente ao clicar

            // Cancelar retentativas anteriores
            mRetryHandler.removeCallbacks(mRetryRunnable);
            mIsRetrying = false;

            // Resetar e cancelar lógica de otimização de buffer
            mBufferingStartTime = 0;
            mIsPausedForBuffering = false;
            if (mBufferOptimizeHandler != null) { // Adicionar verificação de nulidade para segurança
                mBufferOptimizeHandler.removeCallbacks(mCheckBufferingRunnable);
                mBufferOptimizeHandler.removeCallbacks(mResumeAfterBufferRebuildRunnable);
            }
            if (mStagnationCheckHandler != null) { // Adicionar verificação de nulidade
                mStagnationCheckHandler.removeCallbacks(mStagnationCheckRunnable);
            }
            mLastPlaybackPosition = 0;
            mLastPositionCheckTime = 0;

            // Atualizar informações do canal atual para retentativas
            mCurrentPlayingUrl = channel.getStreamUrl();
            mCurrentPlayingChannelName = channel.getName();

            // Extract stream ID from channel for EPG
            currentChannelStreamId = channel.getStreamId(); // Usar o streamId já disponível no objeto Channel
            Log.d(TV_TAG, "Current channel stream ID set to: " + currentChannelStreamId);

            // Parar e liberar o player anterior completamente para evitar problemas de estado.
            mVideoView.release();

            // Definir nova URL e iniciar.
            mVideoView.setUrl(mCurrentPlayingUrl);
            mVideoView.start();

            // Atualizar título no controller usando a referência mTitleViewComponent
            if (mTitleViewComponent != null) {
                mTitleViewComponent.setTitle(mCurrentPlayingChannelName);
            } else {
                Log.e(TV_TAG, "mTitleViewComponent is null in onChannelClick. Title not updated.");
            }

            Toast.makeText(getContext(), getString(R.string.starting_channel_toast, channel.getName()), Toast.LENGTH_SHORT).show();
            Log.d(TV_TAG, "Playback initiated for: " + mCurrentPlayingChannelName);
        } else {
            Log.e(TV_TAG, "Channel stream URL is null or empty for channel: " + channel.getName());
            mCurrentPlayingUrl = null; // Limpar URL se inválida
            mCurrentPlayingChannelName = null;
            showLoading(false); // Esconder loading se a URL for inválida
            Toast.makeText(getContext(), getString(R.string.invalid_channel_url_error), Toast.LENGTH_SHORT).show();
        }
    }


    private void loadInitialData() {
        Log.d(TV_TAG, "loadInitialData called - using DataManager");
        if (!isAdded() || getContext() == null || dataManager == null) {
            Log.w(TV_TAG, "loadInitialData - Fragment not usable or DataManager is null. Aborting.");
            showLoading(false);
            if (getContext() != null && dataManager == null) {
                 Toast.makeText(getContext(), "Error: DataManager not available.", Toast.LENGTH_LONG).show();
            }
            return;
        }

        // Ensure DataManager has finished loading data before attempting to retrieve it
        // This is a simplified check; a more robust solution would involve DataManagerListener
        // or observing LiveData if using Architecture Components.
        if (dataManager.getLiveStreams() == null || dataManager.getLiveCategories() == null) {
            Log.d(TV_TAG, "DataManager has not finished loading data. Waiting...");
            // If data is not yet loaded, ensure DataManager starts loading it.
            // The onDataLoaded callback will then update the UI.
            dataManager.startDataLoading();
            showLoading(true);
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

        if (mChannelGridView != null) {
            mChannelGridView.setChannelsData(allChannels, mFetchedCategoryMap);
        }

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

        showLoading(false);
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


    private void showLoading(boolean isLoading) {
        if (!isAdded() || getView() == null) return; // Check if view is available

        if (playerProgressBar != null) {
            playerProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (playerLoadingTextView != null) {
            playerLoadingTextView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (!isLoading) { // Limpar texto se não estiver carregando
                playerLoadingTextView.setText("");
            }
        }
    }

    private void showLoadingWithMessage(String message) {
        if (!isAdded() || getView() == null) return;

        if (playerProgressBar != null) {
            playerProgressBar.setVisibility(View.VISIBLE);
        }
        if (playerLoadingTextView != null) {
            playerLoadingTextView.setVisibility(View.VISIBLE);
            playerLoadingTextView.setText(message);
            Log.d(TV_TAG, "showLoadingWithMessage: " + message);
        }
    }

    // Helper para converter estado do player em string para logs
    private String playStateToString(int playState) {
        switch (playState) {
            case VideoView.STATE_ERROR: return "STATE_ERROR";
            case VideoView.STATE_IDLE: return "STATE_IDLE";
            case VideoView.STATE_PREPARING: return "STATE_PREPARING";
            case VideoView.STATE_PREPARED: return "STATE_PREPARED";
            case VideoView.STATE_PLAYING: return "STATE_PLAYING";
            case VideoView.STATE_PAUSED: return "STATE_PAUSED";
            case VideoView.STATE_PLAYBACK_COMPLETED: return "STATE_PLAYBACK_COMPLETED";
            case VideoView.STATE_BUFFERING: return "STATE_BUFFERING";
            case VideoView.STATE_BUFFERED: return "STATE_BUFFERED";
            case VideoView.STATE_START_ABORT: return "STATE_START_ABORT";
            default: return "STATE_UNKNOWN (" + playState + ")";
        }
    }

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
                        showLoading(false);
                        Toast.makeText(getContext(), getString(R.string.m3u_loaded_success_toast), Toast.LENGTH_SHORT).show();
                    });
                }
            }
            catch (IOException e) {
                Log.e(TV_TAG, "Erro ao ler arquivo M3U", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), getString(R.string.m3u_load_error_toast, e.getMessage()), Toast.LENGTH_LONG).show();
                        showLoading(false);
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

        if (mBufferOptimizeHandler != null) {
            mBufferOptimizeHandler.removeCallbacks(mCheckBufferingRunnable);
            mBufferOptimizeHandler.removeCallbacks(mResumeAfterBufferRebuildRunnable);
        }
        mBufferingStartTime = 0;
        mIsPausedForBuffering = false;

        if (mStagnationCheckHandler != null) {
            mStagnationCheckHandler.removeCallbacks(mStagnationCheckRunnable);
        }
        mLastPlaybackPosition = 0;
        mLastPositionCheckTime = 0;

        if (mVideoView != null) {
            mVideoView.release();
        }
        if (dataManager != null) {
            dataManager.setListener(null); // Remove listener to prevent leaks
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
                public void onClick(View v) {
                    mVideoView.setSpeed(0.75f);
                    speed = "0.75";
                    txt1.setTextColor(Color.parseColor("#FF39C5BC"));
                    txt2.setTextColor(Color.parseColor("#ffffff"));
                    txt3.setTextColor(Color.parseColor("#ffffff"));
                    txt4.setTextColor(Color.parseColor("#ffffff"));
                    txt5.setTextColor(Color.parseColor("#ffffff"));
                }
            });

            findViewById(R.id.cardview2).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mVideoView.setSpeed(1.0f);
                    speed = "1.0";
                    txt2.setTextColor(Color.parseColor("#FF39C5BC"));
                    txt1.setTextColor(Color.parseColor("#ffffff"));
                    txt3.setTextColor(Color.parseColor("#ffffff"));
                    txt4.setTextColor(Color.parseColor("#ffffff"));
                    txt5.setTextColor(Color.parseColor("#ffffff"));
                }
            });

            findViewById(R.id.cardview3).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mVideoView.setSpeed(1.25f);
                    speed = "1.25";
                    txt3.setTextColor(Color.parseColor("#FF39C5BC"));
                    txt2.setTextColor(Color.parseColor("#ffffff"));
                    txt1.setTextColor(Color.parseColor("#ffffff"));
                    txt4.setTextColor(Color.parseColor("#ffffff"));
                    txt5.setTextColor(Color.parseColor("#ffffff"));
                }
            });

            findViewById(R.id.cardview4).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mVideoView.setSpeed(1.5f);
                    speed = "1.5";
                    txt4.setTextColor(Color.parseColor("#FF39C5BC"));
                    txt2.setTextColor(Color.parseColor("#ffffff"));
                    txt3.setTextColor(Color.parseColor("#ffffff"));
                    txt1.setTextColor(Color.parseColor("#ffffff"));
                    txt5.setTextColor(Color.parseColor("#ffffff"));
                }
            });

            findViewById(R.id.cardview5).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mVideoView.setSpeed(2.0f);
                    speed = "2.0";
                    txt5.setTextColor(Color.parseColor("#FF39C5BC"));
                    txt2.setTextColor(Color.parseColor("#ffffff"));
                    txt3.setTextColor(Color.parseColor("#ffffff"));
                    txt4.setTextColor(Color.parseColor("#ffffff"));
                    txt1.setTextColor(Color.parseColor("#ffffff"));
                }
            });
        }
    }

    public class CustomDrawerPopupView1 extends com.lxj.xpopup.core.DrawerPopupView {
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

            if (proportion.equals("默认")) {
                txt1.setTextColor(Color.parseColor("#FF39C5BC"));
            }
            if (proportion.equals("16:9")) {
                txt2.setTextColor(Color.parseColor("#FF39C5BC"));
            }
            if (proportion.equals("原始大小")) {
                txt3.setTextColor(Color.parseColor("#FF39C5BC"));
            }
            if (proportion.equals("填充")) {
                txt4.setTextColor(Color.parseColor("#FF39C5BC"));
            }
            if (proportion.equals("居中裁剪")) {
                txt5.setTextColor(Color.parseColor("#FF39C5BC"));
            }

            findViewById(R.id.cardview1).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mVideoView.setScreenScaleType(VideoView.SCREEN_SCALE_DEFAULT);
                    proportion = "默认";
                    txt1.setTextColor(Color.parseColor("#FF39C5BC"));
                    txt2.setTextColor(Color.parseColor("#ffffff"));
                    txt3.setTextColor(Color.parseColor("#ffffff"));
                    txt4.setTextColor(Color.parseColor("#ffffff"));
                    txt5.setTextColor(Color.parseColor("#ffffff"));
                }
            });

            findViewById(R.id.cardview2).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mVideoView.setScreenScaleType(VideoView.SCREEN_SCALE_16_9);
                    proportion = "16:9";
                    txt2.setTextColor(Color.parseColor("#FF39C5BC"));
                    txt1.setTextColor(Color.parseColor("#ffffff"));
                    txt3.setTextColor(Color.parseColor("#ffffff"));
                    txt4.setTextColor(Color.parseColor("#ffffff"));
                    txt5.setTextColor(Color.parseColor("#ffffff"));
                }
            });

            findViewById(R.id.cardview3).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mVideoView.setScreenScaleType(VideoView.SCREEN_SCALE_ORIGINAL);
                    proportion = "原始大小";
                    txt3.setTextColor(Color.parseColor("#FF39C5BC"));
                    txt2.setTextColor(Color.parseColor("#ffffff"));
                    txt1.setTextColor(Color.parseColor("#ffffff"));
                    txt4.setTextColor(Color.parseColor("#ffffff"));
                    txt5.setTextColor(Color.parseColor("#ffffff"));
                }
            });

            findViewById(R.id.cardview4).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mVideoView.setScreenScaleType(VideoView.SCREEN_SCALE_MATCH_PARENT);
                    proportion = "填充";
                    txt4.setTextColor(Color.parseColor("#FF39C5BC"));
                    txt2.setTextColor(Color.parseColor("#ffffff"));
                    txt3.setTextColor(Color.parseColor("#ffffff"));
                    txt1.setTextColor(Color.parseColor("#ffffff"));
                    txt5.setTextColor(Color.parseColor("#ffffff"));
                }
            });

            findViewById(R.id.cardview5).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mVideoView.setScreenScaleType(VideoView.SCREEN_SCALE_CENTER_CROP);
                    proportion = "居中裁剪";
                    txt5.setTextColor(Color.parseColor("#FF39C5BC"));
                    txt2.setTextColor(Color.parseColor("#ffffff"));
                    txt3.setTextColor(Color.parseColor("#ffffff"));
                    txt4.setTextColor(Color.parseColor("#ffffff"));
                    txt1.setTextColor(Color.parseColor("#ffffff"));
                }
            });
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mVideoView != null) {
            
        }
        if (getActivity() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (newConfig.smallestScreenWidthDp >= 600) { // Example: Check for tablet layout
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
    public void onPause() {
        super.onPause();
        Log.d(TV_TAG, "onPause called");
        // Não cancelar retentativas aqui, pois o PiP pode estar ativo ou o usuário pode voltar logo.
        // Apenas pausar o vídeo.
        if (mVideoView != null) {
            mVideoView.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TV_TAG, "onResume called");
        if (mVideoView != null) {
            mVideoView.resume();
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
        Log.d(TAG_BACK_TV, "onBackPressed called in TvFragment.");
        if (mVideoView != null && mVideoView.isFullScreen()) {
            Log.d(TAG_BACK_TV, "Player is fullscreen. Calling stopFullScreen().");
            mVideoView.stopFullScreen(); // Tenta sair da tela cheia explicitamente
            if (getActivity() != null) {
                Log.d(TAG_BACK_TV, "Restoring portrait orientation after exiting fullscreen.");
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            return true; // Evento consumido, pois saímos da tela cheia
        }
        // Se não estava em tela cheia, ou se stopFullScreen() não foi chamado,
        // deixa o VideoView tentar lidar com outros casos (como player flutuante).
        if (mVideoView != null && mVideoView.onBackPressed()) {
            Log.d(TAG_BACK_TV, "mVideoView.onBackPressed() handled the event (e.g., closing channel grid).");
            return true; // Evento consumido pelo VideoView (ex: fechou a grade de canais)
        }

        Log.d(TAG_BACK_TV, "Back press not handled by TvFragment (player not fullscreen, or VideoView did not handle). Returning false.");
        return false; // Evento não consumido, MainActivity prosseguirá
    }

    // Implementação do listener do LiveControlView para o botão de proporção
    @Override
    public void onProportionButtonClick() {
        if (getContext() != null && mVideoView != null) { // Verificar contexto e VideoView
            new XPopup.Builder(getContext())
                    .popupPosition(com.lxj.xpopup.enums.PopupPosition.Right) // Ou outra posição desejada
                    .asCustom(new CustomDrawerPopupView1(getContext())) // Reutilizar o popup de proporção existente
                    .show();
        }
    }
}


