package com.example.iptvplayer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.iptvplayer.data.Channel;
import com.example.iptvplayer.data.EpgProgram;
import com.example.iptvplayer.data.Movie;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;

public class DataManager {

    private static final String TAG = "DataManager";

    public enum LoadState {
        IDLE,
        FETCHING_CREDENTIALS,
        FETCHING_LIVE_CATEGORIES,
        FETCHING_LIVE_STREAMS,
        FETCHING_VOD_CATEGORIES,
        FETCHING_VOD_STREAMS,
        FETCHING_EPG,
        PARSING_EPG,
        COMPLETE,
        FAILED
    }

    public interface DataManagerListener {
        void onProgressUpdate(LoadState state, int percentage, String message);
        void onDataLoaded(); // Called when all data is successfully loaded
        void onError(String errorMessage);
    }

    private Context context;
    private ExecutorService executorService;
    private Handler mainHandler;
    private DataManagerListener listener;

    private XtreamApiService xtreamApiService;
    private EpgService epgService;
    private XmltvEpgService xmltvEpgService;
    private CacheManager cacheManager;

    // Data holders (consider a dedicated data model class if it gets complex)
    private String baseUrl;
    private String username;
    private String password;
    private List<XtreamApiService.CategoryInfo> liveCategories;
    private List<Channel> liveStreams;
    private List<XtreamApiService.CategoryInfo> vodCategories;
    private List<Movie> vodStreams;
    private Map<String, String> epgPrograms; // This will hold XMLTV parsed data eventually

    // Flag to indicate if data has been successfully loaded at least once during the app\'s lifecycle
    private boolean isDataFullyLoaded = false;

    public DataManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.cacheManager = new CacheManager(this.context);
    }

    public void setListener(DataManagerListener listener) {
        this.listener = listener;
        // If data is already loaded, notify the new listener immediately
        if (isDataFullyLoaded) {
            mainHandler.post(() -> {
                if (this.listener != null) {
                    this.listener.onDataLoaded();
                }
            });
        }
    }

    public boolean isDataFullyLoaded() {
        return isDataFullyLoaded;
    }

    private LoadState currentLoadState = LoadState.IDLE; // Adicionar para rastrear estado

    public boolean isLoading() {
        return currentLoadState != LoadState.IDLE &&
               currentLoadState != LoadState.COMPLETE &&
               currentLoadState != LoadState.FAILED;
    }

    public void startDataLoading() {
        Log.d(TAG, "startDataLoading called");
        if (listener != null) {
            mainHandler.post(() -> listener.onProgressUpdate(LoadState.IDLE, 0, "Starting data load..."));
        }

        // If data is already fully loaded, just notify and return
        if (isDataFullyLoaded) {
            Log.d(TAG, "Data already fully loaded. Notifying listener.");
            notifyProgress(LoadState.COMPLETE, 100, "Data already loaded.");
            notifyComplete();
            return;
        }

        // Tentar carregar do cache primeiro
        if (cacheManager.hasChannelCache() && cacheManager.hasMovieCache() && cacheManager.hasLiveCategoriesCache() && cacheManager.hasVodCategoriesCache()) {
            Log.d(TAG, "Found cached data. Loading from cache.");
            this.liveStreams = cacheManager.getCachedChannels();
            this.vodStreams = cacheManager.getCachedMovies();
            this.liveCategories = cacheManager.getCachedLiveCategories();
            this.vodCategories = cacheManager.getCachedVodCategories();
            // this.epgPrograms = cacheManager.getCachedEpg("global_epg_key"); // Assuming a global EPG key

            // If we have cached data, we can skip API calls and notify complete
            if (liveStreams != null && vodStreams != null && liveCategories != null && vodCategories != null) {
                isDataFullyLoaded = true; // Mark as fully loaded
                notifyProgress(LoadState.COMPLETE, 100, "Data loaded from cache.");
                notifyComplete();
                return;
            }
        }

        // Se não houver cache válido ou dados completos, buscar credenciais e iniciar o processo de API
        fetchXtreamCredentials();
    }

    private void fetchXtreamCredentials() {
        notifyProgress(LoadState.FETCHING_CREDENTIALS, 5, "Fetching server credentials...");
        executorService.execute(() -> {
            try {
                URL url = new URL("http://mybrasiltv.x10.mx/GetLoguin.php"); // Considerar tornar isso configurável
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000); // 15 segundos
                conn.setReadTimeout(15000);    // 15 segundos

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
                    this.baseUrl = jsonObject.getString("server");
                    this.username = jsonObject.getString("username");
                    this.password = jsonObject.getString("password");

                    if (!this.baseUrl.toLowerCase().startsWith("http://") && !this.baseUrl.toLowerCase().startsWith("https://")) {
                        this.baseUrl = "http://" + this.baseUrl;
                    }

                    Log.i(TAG, "Credenciais recebidas: Servidor=" + this.baseUrl + ", Usuário=" + this.username);
                    notifyProgress(LoadState.FETCHING_CREDENTIALS, 10, "Credenciais obtidas.");

                    // Inicializar serviços aqui, pois temos as credenciais
                    this.xtreamApiService = new XtreamApiService(this.baseUrl, this.username, this.password);
                    this.xtreamApiService.setCacheManager(this.cacheManager); // Definir gerenciador de cache existente

                    this.epgService = new EpgService(this.baseUrl, this.username, this.password);
                    this.epgService.setCacheManager(this.cacheManager); // Definir gerenciador de cache existente

                    // Inicializar XmltvEpgService aqui
                    this.xmltvEpgService = new XmltvEpgService(this.baseUrl, this.username, this.password);
                    this.xmltvEpgService.setCacheManager(this.cacheManager);

                    // Prosseguir para a próxima etapa
                    fetchLiveCategories();
                } else {
                    Log.e(TAG, "fetchXtreamCredentials - Erro HTTP: " + responseCode);
                    notifyError("Falha ao buscar credenciais. Erro do servidor: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "fetchXtreamCredentials - Erro: ", e);
                notifyError("Falha ao buscar credenciais: " + e.getMessage());
            }
        });
    }

    private void fetchEpg() {
        if (xmltvEpgService == null) {
            notifyError("XmltvEpgService não inicializado. Não é possível buscar EPG.");
            isDataFullyLoaded = true;
            notifyProgress(LoadState.COMPLETE, 100, "Dados principais carregados, EPG não disponível.");
            notifyComplete();
            return;
        }
        notifyProgress(LoadState.FETCHING_EPG, 75, "Buscando EPG (XMLTV)...");

        xmltvEpgService.fetchCurrentPrograms(liveStreams, new XmltvEpgService.XmltvEpgCallback() {
            @Override
            public void onSuccess(Map<String, String> currentPrograms) {
                DataManager.this.epgPrograms = currentPrograms;
                Log.i(TAG, "Dados EPG buscados. Contagem de programas: " + (currentPrograms != null ? currentPrograms.size() : 0));

                isDataFullyLoaded = true;
                notifyProgress(LoadState.COMPLETE, 100, "Todos os dados buscados.");
                notifyComplete();
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Falha ao buscar EPG: " + error);
                notifyError("Falha ao buscar EPG (XMLTV): " + error);
                isDataFullyLoaded = true;
                notifyProgress(LoadState.COMPLETE, 100, "Dados principais carregados, EPG falhou.");
                notifyComplete();
            }
        });
    }

    private void fetchVodCategories() {
        if (xtreamApiService == null) {
            notifyError("XtreamApiService não inicializado. Não é possível buscar categorias VOD.");
            return;
        }
        notifyProgress(LoadState.FETCHING_VOD_CATEGORIES, 45, "Buscando categorias VOD...");
        xtreamApiService.fetchVodCategories(new XtreamApiService.XtreamApiCallback<XtreamApiService.CategoryInfo>() {
            @Override
            public void onSuccess(List<XtreamApiService.CategoryInfo> data) {
                DataManager.this.vodCategories = data;
                Log.i(TAG, "Categorias VOD buscadas: " + (data != null ? data.size() : 0));
                notifyProgress(LoadState.FETCHING_VOD_CATEGORIES, 55, "Categorias VOD carregadas.");
                fetchVodStreams(); // Prosseguir para buscar streams VOD
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Falha ao buscar categorias VOD: " + error);
                notifyError("Falha ao buscar categorias VOD: " + error);
                // Se as categorias VOD falharem, ainda podemos tentar buscar streams VOD ou considerar carregado
                fetchVodStreams(); // Tentar buscar streams VOD mesmo sem categorias
            }
        });
    }

    private void fetchVodStreams() {
        if (xtreamApiService == null) {
            notifyError("XtreamApiService não inicializado. Não é possível buscar streams VOD.");
            return;
        }
        notifyProgress(LoadState.FETCHING_VOD_STREAMS, 60, "Buscando streams VOD...");
        xtreamApiService.fetchVodStreams(new XtreamApiService.XtreamApiCallback<Movie>() {
            @Override
            public void onSuccess(List<Movie> data) {
                DataManager.this.vodStreams = data;
                Log.i(TAG, "Streams VOD buscados: " + (data != null ? data.size() : 0));
                notifyProgress(LoadState.FETCHING_VOD_STREAMS, 70, "Streams VOD carregados.");
                // Após carregar VOD streams, podemos considerar os dados principais carregados
                isDataFullyLoaded = true; 
                fetchEpg(); // Próxima etapa principal (EPG é secundário para VOD)
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Falha ao buscar streams VOD: " + error);
                notifyError("Falha ao buscar streams VOD: " + error);
                // Se os streams VOD falharem, ainda podemos considerar os dados principais carregados
                isDataFullyLoaded = true; 
                fetchEpg(); // Tentar buscar EPG mesmo com falha nos streams VOD
            }
        });
    }

    private void notifyProgress(LoadState state, int percentage, String message) {
        this.currentLoadState = state; // Atualizar estado atual
        if (listener != null) {
            mainHandler.post(() -> listener.onProgressUpdate(state, percentage, message));
        }
        Log.d(TAG, "Progresso: " + state + " - " + percentage + "% - " + message);
    }

    private void notifyComplete() {
        this.currentLoadState = LoadState.COMPLETE; // Atualizar estado atual
        if (listener != null) {
            mainHandler.post(() -> listener.onDataLoaded());
        }
        Log.d(TAG, "Carregamento de dados completo.");
    }

    private void notifyError(String message) {
        this.currentLoadState = LoadState.FAILED; // Atualizar estado atual
        if (listener != null) {
            mainHandler.post(() -> listener.onError(message));
        }
        Log.e(TAG, "Erro no carregamento de dados: " + message);
    }

    private void fetchLiveCategories() {
        if (xtreamApiService == null) {
            notifyError("XtreamApiService não inicializado. Não é possível buscar categorias de TV ao vivo.");
            return;
        }
        notifyProgress(LoadState.FETCHING_LIVE_CATEGORIES, 15, "Buscando categorias de TV ao vivo...");
        xtreamApiService.fetchLiveStreamCategories(new XtreamApiService.XtreamApiCallback<XtreamApiService.CategoryInfo>() {
            @Override
            public void onSuccess(List<XtreamApiService.CategoryInfo> data) {
                DataManager.this.liveCategories = data;
                Log.i(TAG, "Categorias ao vivo buscadas: " + (data != null ? data.size() : 0));
                notifyProgress(LoadState.FETCHING_LIVE_CATEGORIES, 25, "Categorias de TV ao vivo carregadas.");
                fetchLiveStreams(); // Prosseguir para buscar streams
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Falha ao buscar categorias ao vivo: " + error);
                notifyError("Falha ao buscar categorias de TV ao vivo: " + error);
                fetchLiveStreams(); // Tentar buscar streams ao vivo mesmo sem categorias
            }
        });
    }

    private void fetchLiveStreams() {
        if (xtreamApiService == null) {
            notifyError("XtreamApiService não inicializado. Não é possível buscar streams de TV ao vivo.");
            return;
        }
        notifyProgress(LoadState.FETCHING_LIVE_STREAMS, 30, "Buscando streams de TV ao vivo...");
        xtreamApiService.fetchLiveStreams(new XtreamApiService.XtreamApiCallback<Channel>() {
            @Override
            public void onSuccess(List<Channel> data) {
                DataManager.this.liveStreams = data;
                Log.i(TAG, "Streams ao vivo buscados: " + (data != null ? data.size() : 0));
                notifyProgress(LoadState.FETCHING_LIVE_STREAMS, 40, "Streams de TV ao vivo carregados.");

                // Criar mapeamento de streamId para channelId para o XmltvEpgService
                if (xmltvEpgService != null && liveStreams != null) {
                    Map<String, String> channelIdMapping = new HashMap<>();
                    for (Channel channel : liveStreams) {
                        if (channel.getStreamId() != null) {
                            channelIdMapping.put(channel.getStreamId(), channel.getStreamId()); // Assumindo streamId == XMLTV channelId
                        }
                    }
                    
                }

                fetchVodCategories(); // Próxima etapa principal
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Falha ao buscar streams ao vivo: " + error);
                notifyError("Falha ao buscar streams de TV ao vivo: " + error);
                fetchVodCategories(); // Tentar buscar categorias VOD mesmo com falha nos streams ao vivo
            }
        });
    }


    // Getters para os dados carregados (a serem usados por fragmentos/atividades)
    public List<XtreamApiService.CategoryInfo> getLiveCategories() {
        return liveCategories;
    }

    public List<Channel> getLiveStreams() {
        return liveStreams;
    }

    public List<XtreamApiService.CategoryInfo> getVodCategories() {
        return vodCategories;
    }

    public List<Movie> getVodStreams() {
        return vodStreams;
    }

    public Map<String, String> getEpgPrograms() {
        return epgPrograms;
    }

    public String getXmlTvUrl() {
        if (epgService != null) {
            return epgService.getXmltvUrl();
        }
        return null;
    }

    // Adicionar getters para baseUrl, username, password se necessário por outras classes
    public String getBaseUrl() {
        return baseUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public XmltvEpgService getXmltvEpgService() {
        return xmltvEpgService;
    }

    // Método para limpar todos os dados e redefinir o sinalizador de carregado (por exemplo, ao fazer logout)
    public void clearAllData() {
        liveCategories = null;
        liveStreams = null;
        vodCategories = null;
        vodStreams = null;
        epgPrograms = null;
        isDataFullyLoaded = false;
        cacheManager.clearCache();
        Log.d(TAG, "Todos os dados limpos e cache redefinido.");
    }
}


