package com.example.iptvplayer;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class XtreamLoginService {

    private static final String LOGIN_URL = "http://mybrasiltv.x10.mx/GetLoguin.php";
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

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String server = jsonResponse.getString("server");
                    String username = jsonResponse.getString("username");
                    String password = jsonResponse.getString("password");

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


