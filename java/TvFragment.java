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
import com.lxj.xpopup.XPopup;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.app.PendingIntent;
import android.content.Intent;
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
import com.example.iptvplayer.data.Channel;
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

public class TvFragment extends Fragment implements ChannelAdapter.OnChannelClickListener {

    private RecyclerView recyclerViewChannels;
    private RecyclerView recyclerViewCategories;
    private ChannelAdapter channelAdapter;
    private TextInputEditText searchEditText;
    private List<Channel> allChannels = new ArrayList<>();
    private DownloadReceiver downloadReceiver;

    private ProgressBar playerProgressBar; // Conectado ao XML
    private TextView playerLoadingTextView; // Para a mensagem "Carregando"

    private VideoView mVideoView;
    private StandardVideoController mController;
    private TitleView mTitleViewComponent; // Referência ao componente TitleView
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
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_tv, container, false);
        Log.d(TV_TAG, "onCreateView called");
        recyclerViewChannels = root.findViewById(R.id.recycler_view_channels);
        recyclerViewCategories = root.findViewById(R.id.recycler_view_categories);
        searchEditText = root.findViewById(R.id.search_edit_text);
        searchEditText.setText(""); // Limpar o texto de busca na criação da view
        playerProgressBar = root.findViewById(R.id.player_progress_bar); // Conectar ProgressBar
        playerLoadingTextView = root.findViewById(R.id.player_loading_text); // Conectar TextView de Loading

        if (getContext() != null) {
            if (recyclerViewChannels != null) {
                if (recyclerViewChannels.getLayoutManager() == null) {
                    Log.d(TV_TAG, "onCreateView - Setting LayoutManager for recyclerViewChannels.");
                }
                // Definir ou redefinir para garantir que está presente
                recyclerViewChannels.setLayoutManager(new LinearLayoutManager(getContext()));
            }
            if (recyclerViewCategories != null) {
                if (recyclerViewCategories.getLayoutManager() == null) {
                    Log.d(TV_TAG, "onCreateView - Setting LayoutManager for recyclerViewCategories.");
                }
                // Definir ou redefinir para garantir que está presente
                recyclerViewCategories.setLayoutManager(new LinearLayoutManager(getContext()));
            }
        } else {
            Log.e(TV_TAG, "onCreateView - getContext() is null, cannot set LayoutManagers.");
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
        mController.addControlComponent(new ErrorView(getContext()));
        mController.addControlComponent(new PrepareView(getContext()));
        mController.addControlComponent(new GestureView(getContext()));
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
        mController.addControlComponent(vodControlView);

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
                Log.d(TV_TAG, "PlayerStateChanged: " + playStateToString(playState) + ", SwitchingChannels: " + mIsSwitchingChannels + ", PiP: " + (getActivity() != null && getActivity().isInPictureInPictureMode()));

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
                    case VideoView.STATE_BUFFERING:
                        Log.d(TV_TAG, "Player is PREPARING or BUFFERING.");
                        showLoading(true);
                        break;
                    case VideoView.STATE_PREPARED: // Vídeo preparado, mas ainda não necessariamente tocando
                    case VideoView.STATE_BUFFERED: // Buffering completo
                        Log.d(TV_TAG, "Player is PREPARED or BUFFERED (finished buffering).");
                        // showLoading(false); // Não esconder aqui necessariamente, esperar pelo PLAYING ou PAUSED
                        // showLoading(false); // Ocultar loading apenas quando PLAYING ou se o player não for iniciar automaticamente
                        break;
                    case VideoView.STATE_ERROR:
                        showLoading(false); // Esconder loading em caso de erro
                        // ErrorView já deve estar sendo exibido pelo controller
                        break;
                }
                // Assegurar que o loading seja escondido se o vídeo estiver tocando ou pausado (após preparo/buffering)
                if (playState == VideoView.STATE_PLAYING || playState == VideoView.STATE_PAUSED || playState == VideoView.STATE_PLAYBACK_COMPLETED) {
                    showLoading(false);
                }
            }
        });
        // Adicionar um listener de erro mais explícito, se disponível e útil,
        // embora o STATE_ERROR já seja tratado acima e pelo ErrorView.
        // Exemplo hipotético (verificar documentação do DKPlayer para o método correto):
        /*
        mVideoView.setOnErrorListener(new xyz.doikki.videoplayer.player.OnErrorListener() {
            @Override
            public void onError() {
                Log.e(TV_TAG, "Explicit OnErrorListener triggered in VideoView.");
                // Aqui você poderia obter mais detalhes do erro se a API do player permitir
            }
        });
        */

        // Register receiver
        downloadReceiver = new DownloadReceiver(this);
        IntentFilter filter = new IntentFilter(DownloadService.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireActivity().registerReceiver(downloadReceiver, filter);
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

        loadInitialData();
        return root;
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

            // Parar e liberar o player anterior completamente para evitar problemas de estado.
            // O release() limpa o player interno. Listeners no VideoView (Java object) devem permanecer.
            mVideoView.release();

            // Definir nova URL e iniciar.
            // O controller já está definido no mVideoView desde o onCreateView.
            // Se release() limpasse o controller do objeto VideoView, precisaríamos de mVideoView.setVideoController(mController);
            mVideoView.setUrl(channel.getStreamUrl());
            mVideoView.start();

            // Atualizar título no controller usando a referência mTitleViewComponent
            if (mTitleViewComponent != null) {
                mTitleViewComponent.setTitle(channel.getName());
            } else {
                // Fallback muito improvável: se mTitleViewComponent for nulo, logar erro.
                // Isso não deveria acontecer se onCreateView foi chamado corretamente.
                Log.e(TV_TAG, "mTitleViewComponent is null in onChannelClick. Title not updated.");
            }

            Toast.makeText(getContext(), getString(R.string.starting_channel_toast, channel.getName()), Toast.LENGTH_SHORT).show();
            Log.d(TV_TAG, "Playback initiated for: " + channel.getName());
        } else {
            Log.e(TV_TAG, "Channel stream URL is null or empty for channel: " + channel.getName());
            showLoading(false); // Esconder loading se a URL for inválida
            Toast.makeText(getContext(), getString(R.string.invalid_channel_url_error), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadInitialData() {
        Log.d(TV_TAG, "loadInitialData called");
        showLoading(true);
        fetchXtreamCredentials(new CredentialsCallback() {
            @Override
            public void onCredentialsReceived(String baseUrl, String username, String password) {
                fetchLiveCategoriesFromApi(baseUrl, username, password, new CategoryCallback() {
                    @Override
                    public void onCategoriesReceived(Map<String, String> categoryMap) {
                        Log.d(TV_TAG, "loadInitialData - onCategoriesReceived, categoryMap size: " + categoryMap.size());
                        // Carrega todos os canais inicialmente (ou a primeira categoria, se preferir)
                        // Para carregar "Todos", passamos null ou um ID específico como "0"
                        fetchLiveChannelsFromApi(baseUrl, username, password, "0"); //  Ou null se a API tratar null como "todos"
                    }

                    @Override
                    public void onCategoryFailure(String error) {
                        Log.e(TV_TAG, "loadInitialData - onCategoryFailure: " + error);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), getString(R.string.error_loading_categories, error), Toast.LENGTH_LONG).show();
                                showLoading(false);
                            });
                        }
                        // Tenta carregar canais mesmo se as categorias falharem, talvez com categoryId nulo (todos)
                        fetchLiveChannelsFromApi(baseUrl, username, password, "0");
                    }
                });
            }

            @Override
            public void onCredentialsFailure(String error) {
                Log.e(TV_TAG, "loadInitialData - onCredentialsFailure: " + error);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), getString(R.string.error_fetching_credentials, error), Toast.LENGTH_LONG).show();
                        showLoading(false);
                    });
                }
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (playerProgressBar != null) {
            playerProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (playerLoadingTextView != null) {
            // Opcional: mostrar/esconder texto de loading junto com a barra
            // playerLoadingTextView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        // Log.d(TV_TAG, "showLoading called with: " + isLoading); // Pode ser muito verboso
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
            } catch (IOException e) {
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

    private void fetchXtreamCredentials(CredentialsCallback callback) {
        executor.execute(() -> {
            Log.d(TV_TAG, "fetchXtreamCredentials called");
            try {
                URL url = new URL("http://mybrasiltv.x10.mx/GetLoguin.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject jsonObject = new JSONObject(response.toString());
                    String server = jsonObject.getString("server");
                    String user = jsonObject.getString("username");
                    String pass = jsonObject.getString("password");

                    if (!server.toLowerCase().startsWith("http://") && !server.toLowerCase().startsWith("https://")) {
                        server = "http://" + server;
                    }

                    Log.i(TV_TAG, "fetchXtreamCredentials - Credentials received: Server=" + server + ", User=" + user + ", Pass=***");
                    callback.onCredentialsReceived(server, user, pass);

                } else {
                    Log.e(TV_TAG, "fetchXtreamCredentials - HTTP error code: " + responseCode);
                    callback.onCredentialsFailure("HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TV_TAG, "fetchXtreamCredentials - Error fetching Xtream credentials", e);
                callback.onCredentialsFailure(e.getMessage());
            }
        });
    }

    private void fetchLiveCategoriesFromApi(String baseUrl, String username, String password, CategoryCallback callback) {
        Log.d(TV_TAG, "fetchLiveCategoriesFromApi called");

        executor.execute(() -> {
            XtreamApiService apiService = new XtreamApiService(baseUrl, username, password);
            apiService.fetchLiveStreamCategories(new XtreamApiService.XtreamApiCallback<XtreamApiService.CategoryInfo>() {
                @Override
                public void onSuccess(List<XtreamApiService.CategoryInfo> data) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Log.d(TV_TAG, "fetchLiveCategoriesFromApi - onSuccess, category count: " + data.size());
                            Map<String, String> categoryMap = new java.util.HashMap<>();
                            for (XtreamApiService.CategoryInfo categoryInfo : data) {
                                categoryMap.put(categoryInfo.id, categoryInfo.name);
                            }
                            callback.onCategoriesReceived(categoryMap);
                            LiveCategoryAdapter categoryAdapter = new LiveCategoryAdapter(getContext(), data, categoryId -> fetchLiveChannelsFromApi(baseUrl, username, password, categoryId));
                            recyclerViewCategories.setAdapter(categoryAdapter);
                        });
                    }
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TV_TAG, "fetchLiveCategoriesFromApi - onFailure: " + error);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> callback.onCategoryFailure(error));
                    }
                }
            });
        });
    }

    private void fetchLiveChannelsFromApi(String baseUrl, String username, String password, @Nullable String categoryId) {
        Log.d(TV_TAG, "fetchLiveChannelsFromApi called for categoryId: " + categoryId);
        showLoading(true);
        executor.execute(() -> {
            XtreamApiService apiService = new XtreamApiService(baseUrl, username, password);
            apiService.fetchLiveStreams(new XtreamApiService.XtreamApiCallback<Channel>() {
                @Override
                public void onSuccess(List<Channel> data) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Log.d(TV_TAG, "fetchLiveChannelsFromApi - onSuccess, channels from API: " + data.size() + ", for categoryId: " + categoryId);
                            allChannels.clear();
                            allChannels.addAll(data); // Armazena todos os canais recebidos

                            List<Channel> filteredChannels = new ArrayList<>();
                            if (categoryId == null || categoryId.isEmpty() || categoryId.equals("0")) { // "0" ou nulo pode ser "Todos os canais"
                                filteredChannels.addAll(allChannels);
                            } else {
                                for (Channel channel : allChannels) {
                                    if (channel.getCategoryId() != null && channel.getCategoryId().equals(categoryId)) {
                                        filteredChannels.add(channel);
                                    }
                                }
                            }
                            Log.d(TV_TAG, "fetchLiveChannelsFromApi - filteredChannels size: " + filteredChannels.size());

                            if (getContext() == null) { // Proteção adicional
                                Log.e(TV_TAG, "fetchLiveChannelsFromApi onSuccess - Context is null, cannot update adapter.");
                                showLoading(false);
                                return;
                            }

                            if (channelAdapter == null) {
                                Log.d(TV_TAG, "fetchLiveChannelsFromApi onSuccess - Creating new ChannelAdapter.");
                                channelAdapter = new ChannelAdapter(getContext(), filteredChannels, TvFragment.this);
                            } else {
                                Log.d(TV_TAG, "fetchLiveChannelsFromApi onSuccess - Updating existing ChannelAdapter data.");
                                channelAdapter.updateData(filteredChannels); // Isso já chama notifyDataSetChanged e atualiza channelListFull
                            }

                            if (recyclerViewChannels != null) {
                                Log.d(TV_TAG, "fetchLiveChannelsFromApi onSuccess - Setting/Resetting adapter on recyclerViewChannels.");
                                recyclerViewChannels.setAdapter(channelAdapter); // Sempre definir/redefinir na RecyclerView

                                if (recyclerViewChannels.getLayoutManager() == null) {
                                    Log.w(TV_TAG, "fetchLiveChannelsFromApi onSuccess - LayoutManager was null, re-setting.");
                                    recyclerViewChannels.setLayoutManager(new LinearLayoutManager(getContext()));
                                }
                            } else {
                                Log.e(TV_TAG, "fetchLiveChannelsFromApi onSuccess - recyclerViewChannels is null!");
                            }

                            // A chamada filterList é importante, mas updateData no ChannelAdapter já limpa e recria
                            // channelListFull. Se searchEditText.getText() estiver vazio (o que deveria estar
                            // devido à correção anterior), filterList("") irá popular channelList com channelListFull.
                            if (channelAdapter != null && searchEditText != null) { // Garantir que ambos não sejam nulos
                                Log.d(TV_TAG, "fetchLiveChannelsFromApi onSuccess - Applying search filter: '" + searchEditText.getText().toString() + "'");
                                channelAdapter.filterList(searchEditText.getText().toString());
                            }
                            showLoading(false);
                        });
                    }
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TV_TAG, "fetchLiveChannelsFromApi - onFailure for categoryId: " + categoryId + ", Error: " + error);
                    if (getActivity() != null && getContext() != null) { // Adicionado getContext() != null
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), getString(R.string.error_loading_channels, error), Toast.LENGTH_LONG).show();
                            if (channelAdapter != null) {
                                Log.d(TV_TAG, "fetchLiveChannelsFromApi onFailure - Clearing channelAdapter.");
                                channelAdapter.updateData(new ArrayList<>()); // Limpa canais em caso de falha
                                if (recyclerViewChannels != null) { // Garante que o adapter vazio seja setado
                                    recyclerViewChannels.setAdapter(channelAdapter);
                                }
                            } else if (recyclerViewChannels != null) {
                                // Se channelAdapter é nulo mas a view existe, limpar a RecyclerView explicitamente
                                recyclerViewChannels.setAdapter(null);
                            }
                            showLoading(false);
                        });
                    }
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TV_TAG, "onDestroyView called");
        if (mVideoView != null) {
            mVideoView.release();
        }
        if (downloadReceiver != null) {
            if (getActivity() != null) {
                getActivity().unregisterReceiver(downloadReceiver);
                Log.d(TV_TAG, "DownloadReceiver unregistered.");
            }
        } catch (IllegalArgumentException e) {
            Log.w(TV_TAG, "DownloadReceiver not registered or already unregistered.", e);
        }
    }
    if (recyclerViewChannels != null) {
        recyclerViewChannels.setAdapter(null);
        Log.d(TV_TAG, "onDestroyView - Set null adapter to recyclerViewChannels.");
    }
    if (recyclerViewCategories != null) {
        recyclerViewCategories.setAdapter(null);
        Log.d(TV_TAG, "onDestroyView - Set null adapter to recyclerViewCategories.");
    }
    // channelAdapter e o adapter de categorias (se fosse uma variável de instância)
    // não são nulificados aqui para permitir que persistam se a instância do fragmento persistir.
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
        if (mVideoView != null) {
            mVideoView.pause();
        }
        // Não é ideal recarregar dados aqui, pois onPause pode ser chamado por vários motivos.
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TV_TAG, "onResume called");
        if (mVideoView != null) {
            mVideoView.resume();
        }
        // Se precisarmos recarregar categorias ou canais ao voltar, a lógica seria aqui.
        // Por exemplo, verificar se os adapters estão populados.
        // No entanto, o problema descrito sugere que os dados SÃO recarregados (pelo clique na categoria), mas não exibidos.
    }

    public interface CredentialsCallback {
        void onCredentialsReceived(String baseUrl, String username, String password);
        void onCredentialsFailure(String error);
    }

    public interface CategoryCallback {
        void onCategoriesReceived(Map<String, String> categoryMap);
        void onCategoryFailure(String error);
    }

    public interface ChannelCallback {
        void onChannelsReceived(List<Channel> channels);
        void onChannelsFailure(String error);
    }

    public boolean onBackPressed() {
        Log.d(TV_TAG, "onBackPressed called in TvFragment");
        if (mVideoView != null) {
            return mVideoView.onBackPressed();
        }
        return false;
    }
}