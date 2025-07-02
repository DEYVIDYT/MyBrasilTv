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
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.TrackSelectionParameters; // Import necessário
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector; // Import necessário
import androidx.media3.ui.PlayerView;
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

    private PlayerView playerView; // Media3 PlayerView
    private ExoPlayer player; // Media3 ExoPlayer
    private ProgressBar playerProgressBar;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_tv, container, false);
        recyclerViewChannels = root.findViewById(R.id.recycler_view_channels);
        recyclerViewCategories = root.findViewById(R.id.recycler_view_categories);
        searchEditText = root.findViewById(R.id.search_edit_text);

        recyclerViewChannels.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewCategories.setLayoutManager(new LinearLayoutManager(getContext()));

        // A inicialização do PlayerView e do ExoPlayer agora segue o padrão Media3
        playerView = root.findViewById(R.id.player_view); // Supondo que você tenha um PlayerView com este ID no seu XML
        playerProgressBar = root.findViewById(R.id.player_progress_bar); // Supondo um ID para a ProgressBar

        // Initialize ExoPlayer
        if (getContext() != null) {
            // Configurar DefaultTrackSelector para desabilitar áudio espacializado
            DefaultTrackSelector.ParametersBuilder parametersBuilder =
                    new DefaultTrackSelector.ParametersBuilder(getContext());
            parametersBuilder.setSpatializationBehavior(TrackSelectionParameters.SPATIALIZATION_BEHAVIOR_NEVER);
            DefaultTrackSelector trackSelector = new DefaultTrackSelector(getContext(), parametersBuilder.build());

            player = new ExoPlayer.Builder(getContext())
                    .setTrackSelector(trackSelector)
                    .build();
            playerView.setPlayer(player);
        }

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
        if (player == null) {
            Log.e("TvFragment", "Player not initialized");
            Toast.makeText(getContext(), "Player não inicializado", Toast.LENGTH_SHORT).show();
            return;
        }
        if (channel.getStreamUrl() != null && !channel.getStreamUrl().isEmpty()) {
            // Cria o MediaItem usando o novo Builder do Media3
            Uri videoUri = Uri.parse(channel.getStreamUrl());
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(videoUri)
                    .build();

            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();

            Toast.makeText(getContext(), "Iniciando: " + channel.getName(), Toast.LENGTH_SHORT).show();
            Log.d("TvFragment", "Playback started for: " + channel.getName());
        } else {
            Log.e("TvFragment", "Channel stream URL is null or empty");
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
        if (player != null) {
            player.release();
            player = null;
        }
        if (downloadReceiver != null) {
            requireActivity().unregisterReceiver(downloadReceiver);
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
}