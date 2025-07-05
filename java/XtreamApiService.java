package com.example.iptvplayer;

import android.util.Log;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.iptvplayer.data.Movie;
import com.example.iptvplayer.data.Channel;

public class XtreamApiService {
    private static final String API_TAG = "XtreamApi_DEBUG";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String baseUrl;
    private String username;
    private String password;
    private CacheManager cacheManager;

    public XtreamApiService(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public interface XtreamApiCallback<T> {
        void onSuccess(List<T> data);
        void onFailure(String error);
    }

    // Novo callback para detalhes de um único VOD
    public interface VodInfoCallback {
        void onSuccess(Movie movieDetails); // Retorna um único objeto Movie enriquecido ou atualizado
        void onFailure(String error);
    }

    public void fetchVodStreams(XtreamApiCallback<Movie> callback) {
        executor.execute(() -> {
            if (cacheManager != null) {
                List<Movie> cachedMovies = cacheManager.getCachedMovies();
                if (cachedMovies != null) {
                    Log.d(API_TAG, "fetchVodStreams - Using cached data: " + cachedMovies.size() + " movies");
                    callback.onSuccess(cachedMovies);
                    return;
                }
            } else {
                Log.w(API_TAG, "fetchVodStreams - CacheManager is null. Proceeding without cache.");
            }
            
            Log.d(API_TAG, "fetchVodStreams called. URL: " + String.format("%s/player_api.php?username=%s&password=%s&action=get_vod_streams", baseUrl, username, "******"));
            try {
                String apiUrl = String.format("%s/player_api.php?username=%s&password=%s&action=get_vod_streams", baseUrl, username, password);
                URL url = new URL(apiUrl);
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

                    Log.d(API_TAG, "fetchVodStreams - Raw API Response: " + response.toString().substring(0, Math.min(response.toString().length(), 500)) + "..."); // Log first 500 chars
                    List<Movie> movies = new ArrayList<>();
                    JSONArray jsonArray = new JSONArray(response.toString());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String name = jsonObject.optString("name");
                        String streamId = jsonObject.optString("stream_id");
                        String streamIcon = jsonObject.optString("stream_icon", null);
                        String categoryId = jsonObject.optString("category_id");
                        String containerExtension = jsonObject.optString("container_extension");

                        String processedPosterUrl = null;
                        if (streamIcon != null && !streamIcon.isEmpty() && !streamIcon.equalsIgnoreCase("null")) {
                            String lowerStreamIcon = streamIcon.toLowerCase();
                            if (lowerStreamIcon.startsWith("http://") || lowerStreamIcon.startsWith("https://")) {
                                processedPosterUrl = streamIcon;
                            } else if (streamIcon.startsWith("//")) {
                                try {
                                    URL base = new URL(this.baseUrl);
                                    processedPosterUrl = base.getProtocol() + ":" + streamIcon;
                                } catch (java.net.MalformedURLException e) {
                                    Log.e(API_TAG, "Malformed baseUrl for VOD icon construction (protocol-relative): " + this.baseUrl, e);
                                }
                            } else if (streamIcon.startsWith("/")) {
                                try {
                                    URL base = new URL(this.baseUrl);
                                    processedPosterUrl = base.getProtocol() + "://" + base.getHost() + (base.getPort() != -1 ? ":" + base.getPort() : "") + streamIcon;
                                    Log.d(API_TAG, "VOD Icon: Relative path \'" + streamIcon + "\' resolved to: " + processedPosterUrl);
                                } catch (java.net.MalformedURLException e) {
                                    Log.e(API_TAG, "Malformed baseUrl for VOD icon (relative path /): " + this.baseUrl, e);
                                }
                            } else {
                                if (!streamIcon.contains("://") && !streamIcon.trim().isEmpty()) {
                                    try {
                                        URL base = new URL(this.baseUrl);
                                        String domainBase = base.getProtocol() + "://" + base.getHost() + (base.getPort() != -1 ? ":" + base.getPort() : "");
                                        processedPosterUrl = domainBase + (streamIcon.startsWith("/") ? "" : "/") + streamIcon;
                                        Log.d(API_TAG, "VOD Icon: Other relative path \'" + streamIcon + "\' resolved to: " + processedPosterUrl);
                                    } catch (java.net.MalformedURLException e) {
                                        Log.e(API_TAG, "Malformed baseUrl for VOD icon (other relative): " + this.baseUrl, e);
                                    }
                                } else if (!streamIcon.trim().isEmpty()){
                                     Log.w(API_TAG, "Unhandled or already absolute stream_icon format for VOD: " + streamIcon);
                                }
                            }
                        }

                        String videoUrl = null;
                        if (streamId != null && !streamId.isEmpty() && containerExtension != null && !containerExtension.isEmpty()) {
                            videoUrl = String.format("%s/movie/%s/%s/%s.%s", baseUrl, username, password, streamId, containerExtension);
                        } else {
                            Log.w(API_TAG, "Could not construct video URL for movie: " + name + ". Missing stream_id or container_extension.");
                        }

                        movies.add(new Movie(name, processedPosterUrl, videoUrl, categoryId, streamId)); // Passando streamId
                    }
                    
                    if (cacheManager != null) {
                        cacheManager.saveMovies(movies);
                    } else {
                        Log.w(API_TAG, "fetchVodStreams - CacheManager is null. Skipping cache save.");
                    }
                    
                    Log.i(API_TAG, "fetchVodStreams - Successfully parsed " + movies.size() + " movies.");
                    callback.onSuccess(movies);

                } else {
                    Log.e(API_TAG, "fetchVodStreams - Failed. HTTP error code: " + responseCode);
                    callback.onFailure("Failed to fetch VOD streams. HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(API_TAG, "fetchVodStreams - Error: ", e);
                callback.onFailure("Error: " + e.getMessage());
            }
        });
    }

    public void fetchLiveStreams(XtreamApiCallback<Channel> callback) {
        executor.execute(() -> {
            if (cacheManager != null) {
                List<Channel> cachedChannels = cacheManager.getCachedChannels();
                if (cachedChannels != null) {
                    Log.d(API_TAG, "fetchLiveStreams - Using cached data: " + cachedChannels.size() + " channels");
                    callback.onSuccess(cachedChannels);
                    return;
                }
            } else {
                Log.w(API_TAG, "fetchLiveStreams - CacheManager is null. Proceeding without cache.");
            }
            
            Log.d(API_TAG, "fetchLiveStreams called. URL: " + String.format("%s/player_api.php?username=%s&password=%s&action=get_live_streams", baseUrl, username, "******"));
            try {
                String apiUrl = String.format("%s/player_api.php?username=%s&password=%s&action=get_live_streams", baseUrl, username, password);
                URL url = new URL(apiUrl);
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

                    Log.d(API_TAG, "fetchLiveStreams - Raw API Response: " + response.toString().substring(0, Math.min(response.toString().length(), 500)) + "...");
                    List<Channel> channels = new ArrayList<>();
                    JSONArray jsonArray = new JSONArray(response.toString());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String name = jsonObject.optString("name");
                        String streamId = jsonObject.optString("stream_id");
                        String streamIconPath = jsonObject.optString("stream_icon");
                        String categoryId = jsonObject.optString("category_id");
                        String fullLogoUrl = null;

                        if (streamIconPath != null && !streamIconPath.isEmpty() && !streamIconPath.equalsIgnoreCase("null")) {
                            String lowerStreamIconPath = streamIconPath.toLowerCase();
                            if (lowerStreamIconPath.startsWith("http://") || lowerStreamIconPath.startsWith("https://")) {
                                fullLogoUrl = streamIconPath;
                            } else if (streamIconPath.startsWith("//")) {
                                try {
                                    URL base = new URL(this.baseUrl); // Assume this.baseUrl é http(s)://domain.com
                                    fullLogoUrl = base.getProtocol() + ":" + streamIconPath;
                                } catch (java.net.MalformedURLException e) {
                                    Log.e(API_TAG, "fetchLiveStreams - Malformed baseUrl (", e);
                                }
                            }
                        }

                        String directSource = String.format("%s/live/%s/%s/%s.ts", this.baseUrl, username, password, streamId);
                        channels.add(new Channel(name, directSource, fullLogoUrl, categoryId, streamId));
                    }
                    
                    if (cacheManager != null) {
                        cacheManager.saveChannels(channels);
                    } else {
                        Log.w(API_TAG, "fetchLiveStreams - CacheManager is null. Skipping cache save.");
                    }
                    
                    Log.i(API_TAG, "fetchLiveStreams - Successfully parsed " + channels.size() + " channels.");
                    callback.onSuccess(channels);

                } else {
                    Log.e(API_TAG, "fetchLiveStreams - Failed. HTTP error code: " + responseCode);
                    callback.onFailure("Failed to fetch live streams. HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(API_TAG, "fetchLiveStreams - Error: ", e);
                callback.onFailure("Error: " + e.getMessage());
            }
        });
    }

    public void fetchVodCategories(XtreamApiCallback<CategoryInfo> callback) {
        executor.execute(() -> {
            // Adicionar verificação de cache para categorias VOD
            if (cacheManager != null) {
                List<CategoryInfo> cachedCategories = cacheManager.getCachedVodCategories();
                if (cachedCategories != null) {
                    Log.d(API_TAG, "fetchVodCategories - Using cached data: " + cachedCategories.size() + " categories");
                    callback.onSuccess(cachedCategories);
                    return;
                }
            } else {
                Log.w(API_TAG, "fetchVodCategories - CacheManager is null. Proceeding without cache.");
            }

            Log.d(API_TAG, "fetchVodCategories called. URL: " + String.format("%s/player_api.php?username=%s&password=%s&action=get_vod_categories", baseUrl, username, "******"));
            try {
                String apiUrl = String.format("%s/player_api.php?username=%s&password=%s&action=get_vod_categories", baseUrl, username, password);
                URL url = new URL(apiUrl);
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

                    Log.d(API_TAG, "fetchVodCategories - Raw API Response: " + response.toString().substring(0, Math.min(response.toString().length(), 500)) + "...");
                    List<CategoryInfo> categories = new ArrayList<>();
                    JSONArray jsonArray = new JSONArray(response.toString());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String categoryId = jsonObject.getString("category_id");
                        String categoryName = jsonObject.getString("category_name");
                        categories.add(new CategoryInfo(categoryId, categoryName));
                    }
                    // Salvar categorias no cache
                    if (cacheManager != null) {
                        cacheManager.saveVodCategories(categories);
                    } else {
                        Log.w(API_TAG, "fetchVodCategories - CacheManager is null. Skipping cache save.");
                    }
                    Log.i(API_TAG, "fetchVodCategories - Successfully parsed " + categories.size() + " VOD categories.");
                    callback.onSuccess(categories);
                } else {
                    Log.e(API_TAG, "fetchVodCategories - Failed. HTTP error code: " + responseCode);
                    callback.onFailure("Failed to fetch VOD categories. HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(API_TAG, "fetchVodCategories - Error: ", e);
                callback.onFailure("Error: " + e.getMessage());
            }
        });
    }

    public void fetchLiveStreamCategories(XtreamApiCallback<CategoryInfo> callback) {
        executor.execute(() -> {
            // Adicionar verificação de cache para categorias Live
            if (cacheManager != null) {
                List<CategoryInfo> cachedCategories = cacheManager.getCachedLiveCategories();
                if (cachedCategories != null) {
                    Log.d(API_TAG, "fetchLiveStreamCategories - Using cached data: " + cachedCategories.size() + " categories");
                    callback.onSuccess(cachedCategories);
                    return;
                }
            } else {
                Log.w(API_TAG, "fetchLiveStreamCategories - CacheManager is null. Proceeding without cache.");
            }

            Log.d(API_TAG, "fetchLiveStreamCategories called. URL: " + String.format("%s/player_api.php?username=%s&password=%s&action=get_live_categories", baseUrl, username, "******"));
            try {
                String apiUrl = String.format("%s/player_api.php?username=%s&password=%s&action=get_live_categories", baseUrl, username, password);
                URL url = new URL(apiUrl);
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

                    Log.d(API_TAG, "fetchLiveStreamCategories - Raw API Response: " + response.toString().substring(0, Math.min(response.toString().length(), 500)) + "...");
                    List<CategoryInfo> categories = new ArrayList<>();
                    JSONArray jsonArray = new JSONArray(response.toString());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String categoryId = jsonObject.getString("category_id");
                        String categoryName = jsonObject.getString("category_name");
                        categories.add(new CategoryInfo(categoryId, categoryName));
                    }
                    // Salvar categorias no cache
                    if (cacheManager != null) {
                        cacheManager.saveLiveCategories(categories);
                    } else {
                        Log.w(API_TAG, "fetchLiveStreamCategories - CacheManager is null. Skipping cache save.");
                    }
                    Log.i(API_TAG, "fetchLiveStreamCategories - Successfully parsed " + categories.size() + " live categories.");
                    callback.onSuccess(categories);
                } else {
                    Log.e(API_TAG, "fetchLiveStreamCategories - Failed. HTTP error code: " + responseCode);
                    callback.onFailure("Failed to fetch Live Stream categories. HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(API_TAG, "fetchLiveStreamCategories - Error: ", e);
                callback.onFailure("Error: " + e.getMessage());
            }
        });
    }

    public static class CategoryInfo {
        public final String id;
        public final String name;
        public final String parentId; 
        public CategoryInfo(String id, String name) {
            this.id = id;
            this.name = name;
            this.parentId = null;
        }
    }

    public void fetchVodInfo(String vodId, VodInfoCallback callback) {
        executor.execute(() -> {
            // Idealmente, verificaríamos o cache aqui primeiro se tivéssemos um objeto Movie existente.
            // Mas como DataManager chamará isso, ele pode lidar com a lógica de cache antes de chamar.
            // Ou, este método poderia aceitar um objeto Movie para preencher.
            // Por enquanto, vamos buscar e parsear, retornando um novo Movie (ou o Movie atualizado).

            Log.d(API_TAG, "fetchVodInfo called for VOD ID: " + vodId);
            String apiUrl = String.format("%s/player_api.php?username=%s&password=%s&action=get_vod_info&vod_id=%s",
                    baseUrl, username, password, vodId);

            try {
                URL url = new URL(apiUrl);
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

                    Log.d(API_TAG, "fetchVodInfo - Raw API Response: " + response.toString().substring(0, Math.min(response.toString().length(), 1000)) + "...");

                    JSONObject jsonResponse = new JSONObject(response.toString());

                    // A resposta da API Xtream para get_vod_info geralmente tem uma estrutura com "info" e "movie_data"
                    JSONObject info = jsonResponse.optJSONObject("info");
                    JSONObject movieData = jsonResponse.optJSONObject("movie_data"); // Para dados básicos que já teríamos

                    if (info == null && movieData == null) {
                         // Se a estrutura for plana (sem 'info' ou 'movie_data' aninhados, e os campos estiverem na raiz)
                        info = jsonResponse; // Tenta ler da raiz
                        Log.d(API_TAG, "fetchVodInfo - 'info' and 'movie_data' not found, attempting to parse from root JSON object.");
                    } else if (info == null) {
                        info = new JSONObject(); // Cria um objeto vazio para evitar NullPointerExceptions se 'info' estiver ausente mas 'movie_data' existir
                        Log.d(API_TAG, "fetchVodInfo - 'info' object not found in response. Details might be missing.");
                    }
                     if (movieData == null) {
                        movieData = new JSONObject();
                        Log.d(API_TAG, "fetchVodInfo - 'movie_data' object not found in response. Basic info might be missing if creating new Movie object.");
                    }


                    // Criar ou atualizar um objeto Movie.
                    // Por agora, vamos assumir que criamos um novo baseado nos detalhes.
                    // Idealmente, o DataManager passaria o Movie existente e nós o atualizaríamos.
                    // Para simplificar, vamos criar um novo e o DataManager pode mesclar.

                    // Pegar dados básicos de movie_data se disponíveis, senão usar os do info ou deixar nulo/padrão
                    String name = movieData.optString("name", info.optString("name"));
                    String streamIcon = movieData.optString("stream_icon", info.optString("stream_icon"));
                     // streamId já é fornecido como parâmetro vodId
                    String categoryId = movieData.optString("category_id", info.optString("category_id"));
                    String containerExtension = movieData.optString("container_extension", info.optString("container_extension"));

                    String videoUrl = "";
                     if (vodId != null && !vodId.isEmpty() && containerExtension != null && !containerExtension.isEmpty()) {
                        videoUrl = String.format("%s/movie/%s/%s/%s.%s", baseUrl, username, password, vodId, containerExtension);
                    }


                    Movie movieDetails = new Movie(name, streamIcon, videoUrl, categoryId, vodId);

                    // Preencher com detalhes do objeto 'info'
                    movieDetails.setPlot(info.optString("plot", null));
                    movieDetails.setCast(info.optString("cast", null));
                    movieDetails.setDirector(info.optString("director", null));
                    movieDetails.setGenre(info.optString("genre", null));
                    movieDetails.setReleaseDate(info.optString("releaseDate", info.optString("releasedate", null))); // Tenta "releaseDate" e "releasedate"
                    movieDetails.setRating(info.optString("rating", null));
                    movieDetails.setDuration(info.optString("duration", null));

                    // Tratar backdrop_path (pode ser uma string ou um array de strings)
                    List<String> backdropPaths = new ArrayList<>();
                    Object backdropObj = info.opt("backdrop_path");
                    if (backdropObj instanceof JSONArray) {
                        JSONArray backdropArray = (JSONArray) backdropObj;
                        for (int i = 0; i < backdropArray.length(); i++) {
                            backdropPaths.add(backdropArray.getString(i));
                        }
                    } else if (backdropObj instanceof String) {
                        String backdropStr = (String) backdropObj;
                        if (backdropStr != null && !backdropStr.isEmpty() && !backdropStr.equals("null")) {
                             backdropPaths.add(backdropStr);
                        }
                    }
                    if (!backdropPaths.isEmpty()) {
                        movieDetails.setBackdropPaths(backdropPaths);
                    }

                    // Campos que já podem estar no Movie.java mas podem ser atualizados se vierem no 'info'
                    if (info.has("name") && movieDetails.getName() == null) movieDetails.setName(info.optString("name"));
                    if (info.has("stream_icon") && movieDetails.getPosterUrl() == null) movieDetails.setStreamIcon(info.optString("stream_icon"));
                    // Adicionar outros campos conforme necessário


                    Log.i(API_TAG, "fetchVodInfo - Successfully parsed VOD info for ID: " + vodId);
                    callback.onSuccess(movieDetails);

                } else {
                    Log.e(API_TAG, "fetchVodInfo - Failed for VOD ID: " + vodId + ". HTTP error code: " + responseCode);
                    callback.onFailure("Failed to fetch VOD info. HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(API_TAG, "fetchVodInfo - Error for VOD ID: " + vodId, e);
                callback.onFailure("Error fetching VOD info: " + e.getMessage());
            }
        });
    }
}


