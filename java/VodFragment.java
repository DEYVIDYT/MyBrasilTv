package com.example.iptvplayer;

import android.Manifest;
import android.util.Log;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONObject;
// JSONArray might be needed if GetLoguin.php returns an array, but based on example, it's an object.
// import org.json.JSONArray;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptvplayer.adapter.MovieAdapter;
import com.example.iptvplayer.data.Movie;
import com.example.iptvplayer.parser.M3uParser;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VodFragment extends Fragment {

    private RecyclerView recyclerView;
    private MovieAdapter movieAdapter;
    private ProgressBar progressBar;
    private ChipGroup chipGroupCategories;
    private List<Movie> allMovies = new ArrayList<>();
    private java.util.Map<String, String> categoryIdToNameMap = new java.util.HashMap<>();
    private DownloadReceiver downloadReceiver;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    // private static final String VOD_URL = ""; // No longer needed for M3U

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // startDownload(); // Commented out as startDownload is removed
                Log.d("VodFragment", "Notification permission granted.");
                // If there's a relevant action to take after permission, add it here.
                // For now, just logging.
            } else {
                Toast.makeText(getContext(), "Permission for notifications is required to see download progress.", Toast.LENGTH_LONG).show();
                // startDownload();  // Commented out
                Log.w("VodFragment", "Notification permission denied.");
            }
        });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_vod, container, false);
        recyclerView = root.findViewById(R.id.recycler_view_vod);
        progressBar = root.findViewById(R.id.progress_bar_vod);
        chipGroupCategories = root.findViewById(R.id.chip_group_categories);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        downloadReceiver = new DownloadReceiver();
        IntentFilter filter = new IntentFilter(DownloadService.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireActivity().registerReceiver(downloadReceiver, filter);
        }

        // checkNotificationPermissionAndStartDownload();
        loadMovies(); // Unified loading logic
        return root;
    }

    private void loadMovies() {
        showLoading(true);
        List<Movie> cachedMovies = MovieCacheManager.loadMoviesFromCache(requireContext());
        java.util.Map<String, String> cachedCategoryMap = MovieCacheManager.loadCategoryMapFromCache(requireContext());

        if (!cachedMovies.isEmpty() && !cachedCategoryMap.isEmpty()) {
            Log.i("VodFragment", "Loaded " + cachedMovies.size() + " movies and category map from cache.");
            allMovies = cachedMovies;
            categoryIdToNameMap = cachedCategoryMap;
            setupAndDisplayMovies();
            // Check if the cache (either movies or categories) is considered "globally" expired to force API refresh
            if (!MovieCacheManager.isCacheValid(requireContext())) {
                 Log.i("VodFragment", "Cache is old or incomplete, scheduling a background refresh.");
                 fetchMoviesAndCategoriesFromApi(false);
            }
        } else {
            Log.i("VodFragment", "Cache is empty or expired/incomplete, fetching from API.");
            fetchMoviesAndCategoriesFromApi(true);
        }
    }

    private void fetchMoviesAndCategoriesFromApi(boolean showInitialLoading) {
        if (showInitialLoading) {
            showLoading(true);
        }

        // Step 1: Fetch credentials from GetLoguin.php
        fetchXtreamCredentials(new CredentialsCallback() {
            @Override
            public void onCredentialsReceived(String baseUrl, String username, String password) {
                // Step 2: Fetch Category Names
                fetchCategoryNames(baseUrl, username, password, new CategoryFetchCallback() {
                    @Override
                    public void onCategoriesReceived(java.util.Map<String, String> categoryMap) {
                        categoryIdToNameMap = categoryMap;
                        MovieCacheManager.saveCategoryMapToCache(requireContext(), categoryIdToNameMap);

                        // Step 3: Use credentials to fetch VOD streams
                        XtreamApiService apiService = new XtreamApiService(baseUrl, username, password);
                        apiService.fetchVodStreams(new XtreamApiService.XtreamApiCallback<Movie>() {
                            @Override
                            public void onSuccess(List<Movie> movies) {
                                MovieCacheManager.saveMoviesToCache(requireContext(), movies);
                                allMovies = movies;
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        Log.i("VodFragment", "Successfully fetched " + movies.size() + " movies from API.");
                                        setupAndDisplayMovies();
                                        if (showInitialLoading) showLoading(false);
                                    });
                                }
                            }

                            @Override
                            public void onFailure(String error) {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        if (showInitialLoading) showLoading(false);
                                        Toast.makeText(getContext(), "Failed to load VOD streams: " + error, Toast.LENGTH_LONG).show();
                                        Log.e("VodFragment", "VOD API Error: " + error);
                                    });
                                }
                            }
                        });
                    }

                    @Override
                    public void onCategoryFetchFailure(String error) {
                        // Still try to load movies, but categories will be IDs
                        Log.w("VodFragment", "Failed to fetch category names ("+error+"), proceeding to fetch movies.");
                        XtreamApiService apiService = new XtreamApiService(baseUrl, username, password);
                        apiService.fetchVodStreams(new XtreamApiService.XtreamApiCallback<Movie>() {
                            @Override
                            public void onSuccess(List<Movie> movies) {
                                MovieCacheManager.saveMoviesToCache(requireContext(), movies); // Save movies even if categories failed
                                allMovies = movies;
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        setupAndDisplayMovies(); // Will display category IDs
                                        if (showInitialLoading) showLoading(false);
                                        Toast.makeText(getContext(), "Movies loaded, but category names might be missing.", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
                             @Override
                            public void onFailure(String movieError) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (showInitialLoading) showLoading(false);
                                Toast.makeText(getContext(), "Failed to load VOD streams: " + error, Toast.LENGTH_LONG).show();
                                Log.e("VodFragment", "VOD API Error: " + error);
                            });
                        }
                    }
                });
            }

            @Override
            public void onCredentialsFailure(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (showInitialLoading) showLoading(false);
                        Toast.makeText(getContext(), "Failed to get Xtream credentials: " + error, Toast.LENGTH_LONG).show();
                        Log.e("VodFragment", "Credentials Error: " + error);
                    });
                }
            }
        });
    }

    private interface CredentialsCallback {
        void onCredentialsReceived(String baseUrl, String username, String password);
        void onCredentialsFailure(String error);
    }

    private void fetchXtreamCredentials(CredentialsCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL("http://mybrasiltv.x10.mx/GetLoguin.php"); // TODO: Make this configurable if needed
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

                    // Ensure http/https is present, default to http if not
                    if (!server.toLowerCase().startsWith("http://") && !server.toLowerCase().startsWith("https://")) {
                        server = "http://" + server;
                    }

                    Log.i("VodFragment", "Credentials received: Server=" + server + ", User=" + user);
                    callback.onCredentialsReceived(server, user, pass);

                } else {
                    callback.onCredentialsFailure("HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e("VodFragment", "Error fetching Xtream credentials", e);
                callback.onCredentialsFailure(e.getMessage());
            }
        });
    }

    private void setupAndDisplayMovies() {
        if (allMovies == null || allMovies.isEmpty()) {
            Log.w("VodFragment", "No movies to display.");
            if (movieAdapter != null) movieAdapter.updateData(new ArrayList<>()); // Clear adapter
            showLoading(false); // Ensure loading is hidden if nothing to show
            // Optionally show a "No movies found" message
            return;
        }
        setupCategoryChips(allMovies);
        if (movieAdapter == null) {
            movieAdapter = new MovieAdapter(allMovies);
            recyclerView.setAdapter(movieAdapter);
        } else {
            movieAdapter.updateData(allMovies);
        }
        showLoading(false);
    }


    // private void checkNotificationPermissionAndStartDownload() {
    //     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    //         if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
    //             // startDownload(); // Original call
    //         } else {
    //             requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    //         }
    //     } else {
    //         // startDownload(); // Original call
    //     }
    // }

    // Removed startDownload() and processM3uFile() as they load local/demo data.
    // The DownloadReceiver might still be relevant if you intend to download actual VOD files for offline viewing later.
    // For now, its primary function of loading the M3U list is replaced by API loading.

    private void setupCategoryChips(List<Movie> movies) {
        Set<String> categoryNames = new LinkedHashSet<>();
        categoryNames.add("All"); // "All" category first

        for (Movie movie : movies) {
            String categoryId = movie.getCategory();
            if (categoryId != null && !categoryId.isEmpty()) {
                String categoryName = categoryIdToNameMap.getOrDefault(categoryId, categoryId); // Use ID if name not found
                categoryNames.add(categoryName);
            }
        }

        chipGroupCategories.removeAllViews();
        boolean firstChip = true;
        for (String name : categoryNames) {
            Chip chip = new Chip(getContext());
            chip.setText(name);
            chip.setCheckable(true);
            // Store the original category ID or "All" in the tag if needed for filtering
            if (name.equals("All")) {
                chip.setTag("All");
            } else {
                // Find the ID for this name (this is a bit inefficient, could optimize)
                for (java.util.Map.Entry<String, String> entry : categoryIdToNameMap.entrySet()) {
                    if (entry.getValue().equals(name)) {
                        chip.setTag(entry.getKey());
                        break;
                    }
                }
                if (chip.getTag() == null) chip.setTag(name); // Fallback if ID not found (e.g. was an ID already)
            }
            chipGroupCategories.addView(chip);
            if (firstChip) {
                chip.setChecked(true);
                firstChip = false;
            }
        }

        chipGroupCategories.setOnCheckedChangeListener((group, checkedId) -> {
            Chip selectedChip = group.findViewById(checkedId);
            if (movieAdapter != null && selectedChip != null) {
                // We need to filter by the original category ID, not the displayed name,
                // unless it's "All". The Movie objects store category IDs.
                String filterKey = (String) selectedChip.getTag();
                 if ("All".equals(filterKey)) {
                    movieAdapter.filterByCategory("All");
                } else {
                    // If the tag is a name (fallback), try to get ID again or use name itself
                    // This part assumes movie.getCategory() returns ID
                    movieAdapter.filterByCategory(filterKey);
                }
            }
        });
    }

    private interface CategoryFetchCallback {
        void onCategoriesReceived(java.util.Map<String, String> categoryMap);
        void onCategoryFetchFailure(String error);
    }

    private void fetchCategoryNames(String baseUrl, String username, String password, CategoryFetchCallback callback) {
        XtreamApiService apiService = new XtreamApiService(baseUrl, username, password);
        apiService.fetchVodCategories(new XtreamApiService.XtreamApiCallback<XtreamApiService.CategoryInfo>() {
            @Override
            public void onSuccess(List<XtreamApiService.CategoryInfo> categoryInfos) {
                java.util.Map<String, String> tempMap = new java.util.HashMap<>();
                for (XtreamApiService.CategoryInfo catInfo : categoryInfos) {
                    tempMap.put(catInfo.id, catInfo.name);
                    Log.d("VodFragment", "Fetched category: ID=" + catInfo.id + ", Name=" + catInfo.name);
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> callback.onCategoriesReceived(tempMap));
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e("VodFragment", "Failed to fetch category names: " + error);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> callback.onCategoryFetchFailure(error));
                }
            }
        });
    }


    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        chipGroupCategories.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (downloadReceiver != null) {
            requireActivity().unregisterReceiver(downloadReceiver);
        }
    }

    private class DownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String filePath = intent.getStringExtra(DownloadService.EXTRA_FILE_PATH);
            if (filePath != null) {
                // processM3uFile(filePath); // Commented out as processM3uFile is removed
                Log.d("VodFragment", "DownloadReceiver received file: " + filePath + " but processM3uFile is no longer in use.");
                // If DownloadService is still used for other VOD downloads (not M3U lists),
                // different logic would be needed here.
                Toast.makeText(getContext(), "File download complete (manual processing needed).", Toast.LENGTH_LONG).show();
            } else {
                showLoading(false);
                Toast.makeText(getContext(), "Download failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void updateMovies(List<Movie> movies) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                allMovies.clear();
                allMovies.addAll(movies);
                setupCategoryChips(allMovies);
                if (movieAdapter == null) {
                    movieAdapter = new MovieAdapter(allMovies);
                    recyclerView.setAdapter(movieAdapter);
                } else {
                    movieAdapter.updateData(allMovies);
                }
                showLoading(false);
            });
        }
    }
}