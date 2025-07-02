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

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_tv, container, false);
        recyclerViewChannels = root.findViewById(R.id.recycler_view_channels);
        recyclerViewCategories = root.findViewById(R.id.recycler_view_categories);
        searchEditText = root.findViewById(R.id.search_edit_text);
        playerProgressBar = root.findViewById(R.id.player_progress_bar); // Conectar ProgressBar
        playerLoadingTextView = root.findViewById(R.id.player_loading_text); // Conectar TextView de Loading

        recyclerViewChannels.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewCategories.setLayoutManager(new LinearLayoutManager(getContext()));

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
            Log.e("TvFragment", "player_container FrameLayout not found in fragment_tv.xml");
            Toast.makeText(getContext(), "Erro: Container do player não encontrado.", Toast.LENGTH_LONG).show();
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
                Log.d("VideoPlayerState", "Current Play State: " + playStateToString(playState) + ", SwitchingChannels: " + mIsSwitchingChannels);

                // Se um novo vídeo começou a tocar ou está pronto (ou erro), a troca terminou.
                if (playState == VideoView.STATE_PLAYING || playState == VideoView.STATE_PREPARED || playState == VideoView.STATE_ERROR) {
                    if(mIsSwitchingChannels) {
                        Log.d("VideoPlayerState", "Transition to PLAYING/PREPARED/ERROR, resetting mIsSwitchingChannels for new stream.");
                    }
                    mIsSwitchingChannels = false;
                }

                // Não atualize as ações PiP se for um evento de pausa do vídeo antigo durante uma troca de canal.
                if (mIsSwitchingChannels && playState == VideoView.STATE_PAUSED) {
                    Log.d("VideoPlayerState", "PAUSED state during channel switch, likely old stream. PiP actions update SKIPPED.");
                    return;
                }
                 if (mIsSwitchingChannels && (playState == VideoView.STATE_IDLE || playState == VideoView.STATE_PREPARING)) {
                    Log.d("VideoPlayerState", "IDLE/PREPARING state during channel switch. PiP actions update SKIPPED.");
                    // Não faz sentido atualizar PiP se o player está idle ou preparando DURANTE uma troca.
                    // Isso pode acontecer se release() for chamado, e o player passar por IDLE.
                    return;
                }


                switch (playState) {
                    case VideoView.STATE_PAUSED:
                        updatePictureInPictureActions(
                                R.drawable.dkplayer_ic_action_play_arrow, "Play", CONTROL_TYPE_PLAY, REQUEST_PLAY);
                        break;
                    case VideoView.STATE_PLAYING:
                        // Quando estiver tocando, e não trocando de canal, reseta o flag (segurança extra).
                        // mIsSwitchingChannels = false; // Já tratado acima, mas pode ser uma garantia.
                        updatePictureInPictureActions(
                                R.drawable.dkplayer_ic_action_pause, "Pause", CONTROL_TYPE_PAUSE, REQUEST_PAUSE);
                        break;
                    case VideoView.STATE_PLAYBACK_COMPLETED:
                        updatePictureInPictureActions(
                                R.drawable.dkplayer_ic_action_replay, "Replay", CONTROL_TYPE_REPLAY, REQUEST_REPLAY);
                        break;
                    // Outros estados como STATE_PREPARING, STATE_BUFFERING podem ser usados para mostrar/esconder o ProgressBar
                    case VideoView.STATE_PREPARING:
                    case VideoView.STATE_BUFFERING:
                        showLoading(true);
                        break;
                    case VideoView.STATE_PREPARED: // Vídeo preparado, mas ainda não necessariamente tocando
                    case VideoView.STATE_BUFFERED: // Buffering completo
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
        Log.d("TvFragment", "onChannelClick: " + channel.getName() + " URL: " + channel.getStreamUrl());
        if (mVideoView == null) {
            Log.e("TvFragment", "Player not initialized");
            Toast.makeText(getContext(), "Player não inicializado", Toast.LENGTH_SHORT).show();
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
                Log.e("TvFragment", "mTitleViewComponent is null in onChannelClick. Title not updated.");
            }

            Toast.makeText(getContext(), "Iniciando: " + channel.getName(), Toast.LENGTH_SHORT).show();
            Log.d("TvFragment", "Playback initiated for: " + channel.getName() + " URL: " + channel.getStreamUrl());
        } else {
            Log.e("TvFragment", "Channel stream URL is null or empty");
            showLoading(false); // Esconder loading se a URL for inválida
            Toast.makeText(getContext(), "URL do canal inválida", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadInitialData() {
        showLoading(true);
        fetchXtreamCredentials(new CredentialsCallback() {
            @Override
            public void onCredentialsReceived(String baseUrl, String username, String password) {
                fetchLiveCategoriesFromApi(baseUrl, username, password, new CategoryCallback() {
                    @Override
                    public void onCategoriesReceived(Map<String, String> categoryMap) {
                        // Carrega todos os canais inicialmente (ou a primeira categoria, se preferir)
                        // Para carregar "Todos", passamos null ou um ID específico como "0"
                        fetchLiveChannelsFromApi(baseUrl, username, password, "0"); //  Ou null se a API tratar null como "todos"
                    }

                    @Override
                    public void onCategoryFailure(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Falha ao carregar categorias: " + error, Toast.LENGTH_LONG).show();
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
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Falha ao obter credenciais: " + error, Toast.LENGTH_LONG).show();
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
        Log.d("TvFragmentLoading", "showLoading called with: " + isLoading);
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
                        Toast.makeText(getContext(), "M3U carregado com sucesso!", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (IOException e) {
                Log.e("TvFragment", "Erro ao ler arquivo M3U", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Erro ao carregar M3U: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        showLoading(false);
                    });
                }
            }
        });
    }

    private void fetchXtreamCredentials(CredentialsCallback callback) {
        executor.execute(() -> {
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

                    Log.i("TvFragment", "Credentials received: Server=" + server + ", User=" + user);
                    callback.onCredentialsReceived(server, user, pass);

                } else {
                    callback.onCredentialsFailure("HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e("TvFragment", "Error fetching Xtream credentials", e);
                callback.onCredentialsFailure(e.getMessage());
            }
        });
    }

    private void fetchLiveCategoriesFromApi(String baseUrl, String username, String password, CategoryCallback callback) {
        executor.execute(() -> {
            XtreamApiService apiService = new XtreamApiService(baseUrl, username, password);
            apiService.fetchLiveStreamCategories(new XtreamApiService.XtreamApiCallback<XtreamApiService.CategoryInfo>() {
                @Override
                public void onSuccess(List<XtreamApiService.CategoryInfo> data) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
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
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> callback.onCategoryFailure(error));
                    }
                }
            });
        });
    }

    private void fetchLiveChannelsFromApi(String baseUrl, String username, String password, @Nullable String categoryId) {
        showLoading(true);
        executor.execute(() -> {
            XtreamApiService apiService = new XtreamApiService(baseUrl, username, password);
            apiService.fetchLiveStreams(new XtreamApiService.XtreamApiCallback<Channel>() {
                @Override
                public void onSuccess(List<Channel> data) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
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

                            if (channelAdapter == null) {
                                channelAdapter = new ChannelAdapter(getContext(), filteredChannels, TvFragment.this);
                                recyclerViewChannels.setAdapter(channelAdapter);
                            } else {
                                channelAdapter.updateData(filteredChannels);
                            }
                            channelAdapter.filterList(searchEditText.getText().toString()); // Reaplicar filtro de busca
                            showLoading(false);
                        });
                    }
                }

                @Override
                public void onFailure(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Falha ao carregar canais: " + error, Toast.LENGTH_LONG).show();
                            if (channelAdapter != null) {
                                channelAdapter.updateData(new ArrayList<>()); // Limpa canais em caso de falha
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
        if (mVideoView != null) {
            mVideoView.release();
        }
        if (downloadReceiver != null) {
            requireActivity().unregisterReceiver(downloadReceiver);
        }
    }

    void updatePictureInPictureActions(
            @DrawableRes int iconId, String title, int controlType, int requestCode) {
        if (getActivity() == null || !getActivity().isInPictureInPictureMode()) {
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
        if (mVideoView != null) {
            mVideoView.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mVideoView != null) {
            mVideoView.resume();
        }
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
        if (mVideoView != null) {
            return mVideoView.onBackPressed();
        }
        return false;
    }
}