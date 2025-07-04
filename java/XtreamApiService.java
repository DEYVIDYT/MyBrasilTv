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

    public XtreamApiService(String baseUrl, String username, String password, Context context) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.cacheManager = new CacheManager(context);
    }

    public interface XtreamApiCallback<T> {
        void onSuccess(List<T> data);
        void onFailure(String error);
    }

    public void fetchVodStreams(XtreamApiCallback<Movie> callback) {
        executor.execute(() -> {
            // Primeiro, verifica se existe cache válido
            List<Movie> cachedMovies = cacheManager.getCachedMovies();
            if (cachedMovies != null) {
                Log.d(API_TAG, "fetchVodStreams - Using cached data: " + cachedMovies.size() + " movies");
                callback.onSuccess(cachedMovies);
                return;
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

                        // Construir a URL do vídeo usando streamId e containerExtension
                        String videoUrl = null;
                        if (streamId != null && !streamId.isEmpty() && containerExtension != null && !containerExtension.isEmpty()) {
                            videoUrl = String.format("%s/movie/%s/%s/%s.%s", baseUrl, username, password, streamId, containerExtension);
                        } else {
                            Log.w(API_TAG, "Could not construct video URL for movie: " + name + ". Missing stream_id or container_extension.");
                        }

                        movies.add(new Movie(name, processedPosterUrl, videoUrl, categoryId));
                    }
                    
                    // Salva no cache
                    cacheManager.saveMovies(movies);
                    
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
            // Primeiro, verifica se existe cache válido
            List<Channel> cachedChannels = cacheManager.getCachedChannels();
            if (cachedChannels != null) {
                Log.d(API_TAG, "fetchLiveStreams - Using cached data: " + cachedChannels.size() + " channels");
                callback.onSuccess(cachedChannels);
                return;
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
                    
                    // Salva no cache
                    cacheManager.saveChannels(channels);
                    
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

    // Simple class to hold category ID and Name
    public static class CategoryInfo {
        public final String id;
        public final String name;
        public final String parentId; // Not always used, but some APIs provide it
        public CategoryInfo(String id, String name) {
            this.id = id;
            this.name = name;
            this.parentId = null;
        }
    }
}


