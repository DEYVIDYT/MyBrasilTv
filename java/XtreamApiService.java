package com.example.iptvplayer;

import android.util.Log;
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

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String baseUrl;
    private String username;
    private String password;

    public XtreamApiService(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
    }

    public interface XtreamApiCallback<T> {
        void onSuccess(List<T> data);
        void onFailure(String error);
    }

    public void fetchVodStreams(XtreamApiCallback<Movie> callback) {
        executor.execute(() -> {
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

                    List<Movie> movies = new ArrayList<>();
                    JSONArray jsonArray = new JSONArray(response.toString());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String name = jsonObject.optString("name");
                        String streamId = jsonObject.optString("stream_id");
                        String streamIcon = jsonObject.optString("stream_icon", null); // Default to null if not present
                        String categoryId = jsonObject.optString("category_id");
                        String containerExtension = jsonObject.optString("container_extension");
                        // Construct the direct source URL carefully
                        // String directSource = String.format("%s/movie/%s/%s/%s.%s", baseUrl, username, password, streamId, containerExtension);
                        // The above directSource might be incorrect based on typical Xtream Codes structure.
                        // Usually, stream_id is part of the query parameters for playback, not the path for the VOD stream itself.
                        // The 'name', 'stream_icon', and 'category_id' are primary for listing.
                        // The actual playback URL is often constructed differently or obtained via another action.
                        // For now, we focus on the metadata for listing.
                        // Let's assume directSource is not immediately needed for cover loading, focusing on streamIcon.

                        Log.d("XtreamApiService", "Movie: " + name + ", Icon URL: " + streamIcon);
                        movies.add(new Movie(name, "placeholder_stream_url", streamIcon, categoryId)); // Using placeholder for stream URL for now
                    }
                    callback.onSuccess(movies);

                } else {
                    Log.e("XtreamApiService", "Failed to fetch VOD streams. HTTP error code: " + responseCode);
                    callback.onFailure("Failed to fetch VOD streams. HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e("XtreamApiService", "Error fetching VOD streams", e);
                callback.onFailure("Error: " + e.getMessage());
            }
        });
    }

    public void fetchLiveStreams(XtreamApiCallback<Channel> callback) {
        executor.execute(() -> {
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

                    List<Channel> channels = new ArrayList<>();
                    JSONArray jsonArray = new JSONArray(response.toString());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String name = jsonObject.optString("name");
                        String streamId = jsonObject.optString("stream_id");
                        String streamIcon = jsonObject.optString("stream_icon");
                        String categoryId = jsonObject.optString("category_id");
                        String directSource = String.format("%s/%s/%s/%s", baseUrl, streamId, username, password);
                        channels.add(new Channel(name, directSource, streamIcon, categoryId));
                    }
                    callback.onSuccess(channels);

                } else {
                    callback.onFailure("Failed to fetch live streams. HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e("XtreamApiService", "Error fetching live streams", e);
                callback.onFailure("Error: " + e.getMessage());
            }
        });
    }

    public void fetchVodCategories(XtreamApiCallback<CategoryInfo> callback) {
        executor.execute(() -> {
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

                    List<CategoryInfo> categories = new ArrayList<>();
                    JSONArray jsonArray = new JSONArray(response.toString());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String categoryId = jsonObject.getString("category_id");
                        String categoryName = jsonObject.getString("category_name");
                        categories.add(new CategoryInfo(categoryId, categoryName));
                    }
                    callback.onSuccess(categories);
                } else {
                    callback.onFailure("Failed to fetch VOD categories. HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e("XtreamApiService", "Error fetching VOD categories", e);
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
            this(id, name, "0"); // Default parentId if not provided
        }

        public CategoryInfo(String id, String name, String parentId) {
            this.id = id;
            this.name = name;
            this.parentId = parentId;
        }
    }

    public void fetchLiveStreamCategories(XtreamApiCallback<CategoryInfo> callback) {
        executor.execute(() -> {
            try {
                String apiUrl = String.format("%s/player_api.php?username=%s&password=%s&action=get_live_categories", baseUrl, username, password);
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    List<CategoryInfo> categories = new ArrayList<>();
                    JSONArray jsonArray = new JSONArray(response.toString());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String categoryId = jsonObject.getString("category_id");
                        String categoryName = jsonObject.getString("category_name");
                        String parentId = jsonObject.optString("parent_id", "0");
                        categories.add(new CategoryInfo(categoryId, categoryName, parentId));
                    }
                    callback.onSuccess(categories);
                } else {
                    Log.e("XtreamApiService", "Failed to fetch live categories. HTTP error: " + responseCode);
                    callback.onFailure("Failed to fetch live categories. HTTP error: " + responseCode);
                }
            } catch (Exception e) {
                Log.e("XtreamApiService", "Error fetching live categories", e);
                callback.onFailure("Error: " + e.getMessage());
            }
        });
    }
}


