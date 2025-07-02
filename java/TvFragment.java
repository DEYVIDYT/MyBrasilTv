package com.example.iptvplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptvplayer.adapter.ChannelAdapter;
import com.example.iptvplayer.data.Channel;
import com.example.iptvplayer.parser.M3uParser;
import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TvFragment extends Fragment {

    private RecyclerView recyclerViewChannels;
    private RecyclerView recyclerViewCategories;
    private ChannelAdapter channelAdapter;
    private TextInputEditText searchEditText;
    private List<Channel> allChannels = new ArrayList<>();
    private DownloadReceiver downloadReceiver;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_tv, container, false);
        recyclerViewChannels = root.findViewById(R.id.recycler_view_channels);
        recyclerViewCategories = root.findViewById(R.id.recycler_view_categories);
        recyclerViewChannels.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        searchEditText = root.findViewById(R.id.search_edit_text);

        // Register receiver
        downloadReceiver = new DownloadReceiver();
        IntentFilter filter = new IntentFilter(DownloadService.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireActivity().registerReceiver(downloadReceiver, filter);
        }

        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterChannels(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {

            }
        });

        // Para fins de demonstração, vamos criar dados de exemplo para evitar problemas de download
        // createExampleChannels(); // REMOVED - Will fetch from API
        loadInitialData();
        return root;
    }

    private void loadInitialData() {
        // This method will orchestrate fetching credentials, then categories, then channels.
        // Similar to VodFragment.java
        showLoading(true); // You'll need to implement showLoading if not already present
        fetchXtreamCredentials(new CredentialsCallback() {
            @Override
            public void onCredentialsReceived(String baseUrl, String username, String password) {
                // Credentials received, now fetch categories
                fetchLiveCategoriesFromApi(baseUrl, username, password, new CategoryCallback() {
                    @Override
                    public void onCategoriesReceived(java.util.Map<String, String> categoryMap) {
                        // Categories received, now fetch all live channels
                        // Store categoryMap for later use (e.g., in liveCategoryIdToNameMap)
                        // liveCategoryIdToNameMap.clear();
                        // liveCategoryIdToNameMap.putAll(categoryMap);
                        // updateCategoryRecyclerView(new ArrayList<>(categoryMap.values())); // Or however you structure it

                        fetchLiveChannelsFromApi(baseUrl, username, password, null); // null for all channels initially
                    }

                    @Override
                    public void onCategoryFailure(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Failed to load TV categories: " + error, Toast.LENGTH_LONG).show();
                                showLoading(false);
                            });
                        }
                        // Optionally, still try to load channels without categories
                         fetchLiveChannelsFromApi(baseUrl, username, password, null);
                    }
                });
            }

            @Override
            public void onCredentialsFailure(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Failed to get Xtream credentials for TV: " + error, Toast.LENGTH_LONG).show();
                        showLoading(false);
                    });
                }
            }
        });
    }

    // TODO: Implement showLoading(boolean) method, similar to VodFragment
    private void showLoading(boolean isLoading){
        // Example:
        // progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        // recyclerViewChannels.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        // recyclerViewCategories.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        android.util.Log.d("TvFragment", "showLoading: " + isLoading); // Placeholder
    }


    // Credentials fetching logic (similar to VodFragment)
    private interface CredentialsCallback {
        void onCredentialsReceived(String baseUrl, String username, String password);
        void onCredentialsFailure(String error);
    }

    private void fetchXtreamCredentials(CredentialsCallback callback) {
        executor.execute(() -> {
            try {
                java.net.URL url = new java.net.URL("http://mybrasiltv.x10.mx/GetLoguin.php");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                int responseCode = conn.getResponseCode();
                if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    org.json.JSONObject jsonObject = new org.json.JSONObject(response.toString());
                    String server = jsonObject.getString("server");
                    String user = jsonObject.getString("username");
                    String pass = jsonObject.getString("password");
                    if (!server.toLowerCase().startsWith("http://") && !server.toLowerCase().startsWith("https://")) {
                        server = "http://" + server;
                    }
                    callback.onCredentialsReceived(server, user, pass);
                } else {
                    callback.onCredentialsFailure("HTTP error: " + responseCode);
                }
            } catch (Exception e) {
                callback.onCredentialsFailure(e.getMessage());
            }
        });
    }

    // Category fetching logic
    private interface CategoryCallback {
        void onCategoriesReceived(java.util.Map<String, String> categoryMap);
        void onCategoryFailure(String error);
    }

    private java.util.Map<String, String> liveCategoryIdToNameMap = new java.util.HashMap<>();
    // TODO: Add an adapter for recyclerViewCategories (e.g., LiveCategoryAdapter)
    // private LiveCategoryAdapter liveCategoryAdapter; // You will need to create this adapter
    private com.example.iptvplayer.adapter.LiveCategoryAdapter liveCategoryAdapter;


    private void fetchLiveCategoriesFromApi(String baseUrl, String username, String password, CategoryCallback callback) {
        XtreamApiService apiService = new XtreamApiService(baseUrl, username, password);
        apiService.fetchLiveStreamCategories(new XtreamApiService.XtreamApiCallback<XtreamApiService.CategoryInfo>() {
            @Override
            public void onSuccess(List<XtreamApiService.CategoryInfo> data) {
                java.util.Map<String, String> categoryMap = new java.util.LinkedHashMap<>(); // Preserve order
                categoryMap.put("All", "All"); // Special "All" category
                liveCategoryIdToNameMap.clear();
                liveCategoryIdToNameMap.put("All", "All");

                for (XtreamApiService.CategoryInfo catInfo : data) {
                    categoryMap.put(catInfo.id, catInfo.name);
                    liveCategoryIdToNameMap.put(catInfo.id, catInfo.name);
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (liveCategoryAdapter == null) {
                            // Pass the map itself or a list of CategoryInfo objects to adapter
                            liveCategoryAdapter = new com.example.iptvplayer.adapter.LiveCategoryAdapter(
                                    new ArrayList<>(liveCategoryIdToNameMap.entrySet()),
                                    (catId) -> { // onItemClick lambda
                                fetchLiveChannelsFromApi(baseUrl, username, password, catId.equals("All") ? null : catId);
                            });
                            recyclerViewCategories.setAdapter(liveCategoryAdapter);
                        } else {
                            liveCategoryAdapter.updateData(new ArrayList<>(liveCategoryIdToNameMap.entrySet()));
                        }
                        android.util.Log.d("TvFragment", "Live categories loaded and adapter updated: " + liveCategoryIdToNameMap.size());
                        callback.onCategoriesReceived(liveCategoryIdToNameMap); // Pass the populated map
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                android.util.Log.e("TvFragment", "Failed to fetch live categories: " + error);
                if (getActivity() != null) {
                     getActivity().runOnUiThread(() -> callback.onCategoryFailure(error));
                }
            }
        });
    }

    // Channel fetching logic
    private void fetchLiveChannelsFromApi(String baseUrl, String username, String password, @Nullable String categoryIdToFilter) {
        XtreamApiService apiService = new XtreamApiService(baseUrl, username, password);
        apiService.fetchLiveStreams(new XtreamApiService.XtreamApiCallback<Channel>() {
            @Override
            public void onSuccess(List<Channel> channels) {
                allChannels.clear();
                allChannels.addAll(channels); // Store all fetched channels
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        List<Channel> channelsToDisplay = new ArrayList<>();
                        if (categoryIdToFilter == null || categoryIdToFilter.equals("All")) {
                            channelsToDisplay.addAll(allChannels);
                        } else {
                            for (Channel channel : allChannels) {
                                if (categoryIdToFilter.equals(channel.getGroupTitle())) { // Assuming getGroupTitle() is category ID
                                    channelsToDisplay.add(channel);
                                }
                            }
                        }

                        if (channelAdapter == null) {
                            channelAdapter = new ChannelAdapter(channelsToDisplay);
                            recyclerViewChannels.setAdapter(channelAdapter);
                        } else {
                            channelAdapter.updateData(channelsToDisplay);
                        }
                        showLoading(false);
                        // Toast.makeText(getContext(), "TV Channels loaded!", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                 if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(getContext(), "Failed to load TV channels: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }


    // private void startDownload() {
    //     // This method is likely obsolete if createExampleChannels is removed and API is used.
    //     // createExampleChannels();
    // }

    private void processM3uFile(String filePath) {
        // This method is likely obsolete if M3U files are not the primary source.
        executor.execute(() -> {
            try {
                File file = new File(filePath);
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    allChannels = M3uParser.parse(br);
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        channelAdapter = new ChannelAdapter(allChannels);
                        recyclerViewChannels.setAdapter(channelAdapter);
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Failed to process list", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void filterChannels(String query) {
        List<Channel> filteredList = new ArrayList<>();
        for (Channel channel : allChannels) {
            if (channel.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(channel);
            }
        }
        if (channelAdapter != null) {
            channelAdapter = new ChannelAdapter(filteredList);
            recyclerViewChannels.setAdapter(channelAdapter);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unregister receiver
        if (downloadReceiver != null) {
            requireActivity().unregisterReceiver(downloadReceiver);
        }
    }

    private class DownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String filePath = intent.getStringExtra(DownloadService.EXTRA_FILE_PATH);
            if (filePath != null) {
                processM3uFile(filePath);
            } else {
                Toast.makeText(getContext(), "Download failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void updateChannels(List<Channel> channels) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                allChannels.clear();
                allChannels.addAll(channels);
                if (channelAdapter == null) {
                    channelAdapter = new ChannelAdapter(allChannels);
                    recyclerViewChannels.setAdapter(channelAdapter);
                } else {
                    channelAdapter.updateData(allChannels);
                }
            });
        }
    }
}