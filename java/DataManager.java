package com.example.iptvplayer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.iptvplayer.data.Channel;
import com.example.iptvplayer.data.EpgProgram;
import com.example.iptvplayer.data.Movie;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private CacheManager cacheManager;

    // Data holders (consider a dedicated data model class if it gets complex)
    private String baseUrl;
    private String username;
    private String password;
    private List<XtreamApiService.CategoryInfo> liveCategories;
    private List<Channel> liveStreams;
    private List<XtreamApiService.CategoryInfo> vodCategories;
    private List<Movie> vodStreams;
    private List<EpgProgram> epgPrograms; // This will hold XMLTV parsed data eventually

    public DataManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.cacheManager = new CacheManager(this.context);
    }

    public void setListener(DataManagerListener listener) {
        this.listener = listener;
    }

    public void startDataLoading() {
        // Implementation will start here in the next steps
        // For now, just a placeholder
        Log.d(TAG, "startDataLoading called");
        if (listener != null) {
            mainHandler.post(() -> listener.onProgressUpdate(LoadState.IDLE, 0, "Starting data load..."));
        }
        fetchXtreamCredentials();
    }

    private void fetchXtreamCredentials() {
        notifyProgress(LoadState.FETCHING_CREDENTIALS, 5, "Fetching server credentials...");
        executorService.execute(() -> {
            try {
                // Assuming GetLoguin.php gives server, username, password
                // This logic is similar to what was in TvFragment/VodFragment
                URL url = new URL("http://mybrasiltv.x10.mx/GetLoguin.php"); // Consider making this configurable
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000); // 15 seconds
                conn.setReadTimeout(15000);    // 15 seconds

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

                    Log.i(TAG, "Credentials received: Server=" + this.baseUrl + ", User=" + this.username);
                    notifyProgress(LoadState.FETCHING_CREDENTIALS, 10, "Credentials obtained.");

                    // Initialize services here as we have credentials
                    this.xtreamApiService = new XtreamApiService(this.baseUrl, this.username, this.password);
                    this.xtreamApiService.setCacheManager(this.cacheManager); // Set existing cache manager

                    this.epgService = new EpgService(this.baseUrl, this.username, this.password);
                    this.epgService.setCacheManager(this.cacheManager); // Set existing cache manager

                    // Proceed to the next step
                    fetchLiveCategories();
                } else {
                    Log.e(TAG, "fetchXtreamCredentials - HTTP error code: " + responseCode);
                    notifyError("Failed to fetch credentials. Server error: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "fetchXtreamCredentials - Error: ", e);
                notifyError("Failed to fetch credentials: " + e.getMessage());
            }
        });
    }

    private void fetchEpg() {
        if (epgService == null) {
            notifyError("EpgService not initialized. Cannot fetch EPG.");
            // Optionally, we could consider EPG non-critical and call notifyComplete here.
            // For now, let's treat it as an error if the service isn't there.
            return;
        }
        notifyProgress(LoadState.FETCHING_EPG, 75, "Fetching EPG (XMLTV)...");

        // Note: EpgService.fetchXmlEpg currently does not parse the XML.
        // This will need to be implemented for EPG data to be useful.
        epgService.fetchXmlEpg(new EpgService.EpgCallback() {
            @Override
            public void onSuccess(List<EpgProgram> programs) {
                // Even if parsing is not implemented in EpgService, programs might be an empty list.
                DataManager.this.epgPrograms = programs;
                Log.i(TAG, "EPG data fetched. Program count (pre-parsing): " + (programs != null ? programs.size() : 0));

                if (programs != null && !programs.isEmpty()) {
                    notifyProgress(LoadState.PARSING_EPG, 85, "EPG data received (parsing may be incomplete).");
                } else if (programs != null && programs.isEmpty()) {
                     notifyProgress(LoadState.PARSING_EPG, 85, "EPG data received (empty or parsing not implemented in EpgService).");
                }

                // For now, we assume fetching (even if not parsing) is a step towards completion.
                // A more robust solution would involve actual parsing progress if possible.
                notifyProgress(LoadState.COMPLETE, 100, "All data fetched.");
                notifyComplete();
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to fetch EPG: " + error);
                // Decide if EPG failure is critical. For now, let's notify error and stop.
                // Alternatively, could log warning and call notifyComplete().
                notifyError("Failed to fetch EPG (XMLTV): " + error);
            }
        });
    }

    private void fetchVodCategories() {
        if (xtreamApiService == null) {
            notifyError("XtreamApiService not initialized. Cannot fetch VOD categories.");
            return;
        }
        notifyProgress(LoadState.FETCHING_VOD_CATEGORIES, 45, "Fetching VOD categories...");
        xtreamApiService.fetchVodCategories(new XtreamApiService.XtreamApiCallback<XtreamApiService.CategoryInfo>() {
            @Override
            public void onSuccess(List<XtreamApiService.CategoryInfo> data) {
                DataManager.this.vodCategories = data;
                Log.i(TAG, "VOD categories fetched: " + (data != null ? data.size() : 0));
                notifyProgress(LoadState.FETCHING_VOD_CATEGORIES, 55, "VOD categories loaded.");
                fetchVodStreams(); // Proceed to fetch VOD streams
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to fetch VOD categories: " + error);
                notifyError("Failed to fetch VOD categories: " + error);
            }
        });
    }

    private void fetchVodStreams() {
        if (xtreamApiService == null) {
            notifyError("XtreamApiService not initialized. Cannot fetch VOD streams.");
            return;
        }
        notifyProgress(LoadState.FETCHING_VOD_STREAMS, 60, "Fetching VOD streams...");
        xtreamApiService.fetchVodStreams(new XtreamApiService.XtreamApiCallback<Movie>() {
            @Override
            public void onSuccess(List<Movie> data) {
                DataManager.this.vodStreams = data;
                Log.i(TAG, "VOD streams fetched: " + (data != null ? data.size() : 0));
                notifyProgress(LoadState.FETCHING_VOD_STREAMS, 70, "VOD streams loaded.");
                fetchEpg(); // Next major step
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to fetch VOD streams: " + error);
                notifyError("Failed to fetch VOD streams: " + error);
            }
        });
    }

    private void notifyProgress(LoadState state, int percentage, String message) {
        if (listener != null) {
            mainHandler.post(() -> listener.onProgressUpdate(state, percentage, message));
        }
        Log.d(TAG, "Progress: " + state + " - " + percentage + "% - " + message);
    }

    private void notifyComplete() {
        if (listener != null) {
            mainHandler.post(() -> listener.onDataLoaded());
        }
        Log.d(TAG, "Data loading complete.");
    }

    private void notifyError(String message) {
        if (listener != null) {
            mainHandler.post(() -> listener.onError(message));
        }
        Log.e(TAG, "Data loading error: " + message);
    }

    private void fetchLiveCategories() {
        if (xtreamApiService == null) {
            notifyError("XtreamApiService not initialized. Cannot fetch live categories.");
            return;
        }
        notifyProgress(LoadState.FETCHING_LIVE_CATEGORIES, 15, "Fetching live TV categories...");
        xtreamApiService.fetchLiveStreamCategories(new XtreamApiService.XtreamApiCallback<XtreamApiService.CategoryInfo>() {
            @Override
            public void onSuccess(List<XtreamApiService.CategoryInfo> data) {
                DataManager.this.liveCategories = data;
                Log.i(TAG, "Live categories fetched: " + (data != null ? data.size() : 0));
                notifyProgress(LoadState.FETCHING_LIVE_CATEGORIES, 25, "Live TV categories loaded.");
                fetchLiveStreams(); // Proceed to fetch streams
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to fetch live categories: " + error);
                notifyError("Failed to fetch live TV categories: " + error);
            }
        });
    }

    private void fetchLiveStreams() {
        if (xtreamApiService == null) {
            notifyError("XtreamApiService not initialized. Cannot fetch live streams.");
            return;
        }
        notifyProgress(LoadState.FETCHING_LIVE_STREAMS, 30, "Fetching live TV streams...");
        xtreamApiService.fetchLiveStreams(new XtreamApiService.XtreamApiCallback<Channel>() {
            @Override
            public void onSuccess(List<Channel> data) {
                DataManager.this.liveStreams = data;
                Log.i(TAG, "Live streams fetched: " + (data != null ? data.size() : 0));
                notifyProgress(LoadState.FETCHING_LIVE_STREAMS, 40, "Live TV streams loaded.");
                fetchVodCategories(); // Next major step
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to fetch live streams: " + error);
                notifyError("Failed to fetch live TV streams: " + error);
            }
        });
    }


    // Getters for the loaded data (to be used by fragments/activities)
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

    public List<EpgProgram> getEpgPrograms() {
        return epgPrograms;
    }

    public String getXmlTvUrl() {
        if (epgService != null) {
            return epgService.getXmltvUrl();
        }
        return null;
    }
}
