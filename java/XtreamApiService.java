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
                        String streamIcon = jsonObject.optString("stream_icon");
                        String categoryId = jsonObject.optString("category_id");
                        String containerExtension = jsonObject.optString("container_extension");
                        String directSource = String.format("%s/%s/%s/%s.%s", baseUrl, streamId, username, password, containerExtension);
                        movies.add(new Movie(name, directSource, streamIcon, categoryId));
                    }
                    callback.onSuccess(movies);

                } else {
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
}


