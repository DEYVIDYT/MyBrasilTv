package com.example.iptvplayer;

import android.util.Base64; // Added for Base64 decoding
import android.util.Log;
import org.json.JSONArray; // Added for JSON array parsing
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList; // Added for converting JSONArray
import java.util.List; // Added for type hinting
import java.util.Random; // Added for random selection
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class XtreamLoginService {

    private static final String LOGIN_URL = "https://raw.githubusercontent.com/DEYVIDYT/CineStream-Pro/refs/heads/main/credentials_base64.txt"; // Updated URL
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface LoginCallback {
        void onSuccess(XtreamAccount account);
        void onFailure(String error);
    }

    public void getLoginData(LoginCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(LOGIN_URL);
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

                    // Decode Base64 response
                    String base64Response = response.toString();
                    byte[] decodedBytes = Base64.decode(base64Response, Base64.DEFAULT);
                    String decodedJsonString = new String(decodedBytes);

                    // Parse the JSON array
                    JSONArray jsonArray = new JSONArray(decodedJsonString);

                    if (jsonArray.length() == 0) {
                        callback.onFailure("No login credentials found in the response.");
                        return;
                    }

                    // Select a random JSONObject from the array
                    Random random = new Random();
                    int randomIndex = random.nextInt(jsonArray.length());
                    JSONObject selectedCredentials = jsonArray.getJSONObject(randomIndex);

                    String server = selectedCredentials.getString("server");
                    String username = selectedCredentials.getString("username");
                    String password = selectedCredentials.getString("password");

                    XtreamAccount account = new XtreamAccount(server, username, password);
                    callback.onSuccess(account);

                } else {
                    callback.onFailure("Failed to get login data. HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e("XtreamLoginService", "Error getting login data", e);
                callback.onFailure("Error: " + e.getMessage());
            }
        });
    }

    public static class XtreamAccount {
        public final String server;
        public final String username;
        public final String password;

        public XtreamAccount(String server, String username, String password) {
            this.server = server;
            this.username = username;
            this.password = password;
        }
    }
}


