package com.example.iptvplayer;

import android.Manifest;
import android.util.Log;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONObject;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptvplayer.adapter.MovieAdapter;
import com.example.iptvplayer.adapter.CategoryAdapter; // Importar o novo adapter
import com.example.iptvplayer.data.Movie;
import com.example.iptvplayer.parser.M3uParser;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap; // Usar LinkedHashMap para manter a ordem das categorias
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import androidx.localbroadcastmanager.content.LocalBroadcastManager; // ADDED IMPORT
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class VodFragment extends Fragment {

    private RecyclerView mainRecyclerView; // Renomeado de recyclerView para mainRecyclerView
    private CategoryAdapter categoryAdapter; // Novo adapter para as categorias
    private ProgressBar progressBar;
    // private ChipGroup chipGroupCategories; // Não será mais usado com o novo layout
    private List<Movie> allMovies = new ArrayList<>();
    private Map<String, String> categoryIdToNameMap = new LinkedHashMap<>(); // Usar LinkedHashMap
    private static final String VOD_TAG = "VOD_DEBUG"; // Tag para logs
    private DownloadReceiver downloadReceiver;
    private BroadcastReceiver refreshDataReceiver; // For data refresh

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Log.d(VOD_TAG, "Notification permission granted.");
            } else {
                Toast.makeText(getContext(), getString(R.string.notification_permission_required_toast), Toast.LENGTH_LONG).show();
                Log.w(VOD_TAG, "Notification permission denied.");
            }
        });


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(VOD_TAG, "onCreateView called");
        View root = inflater.inflate(R.layout.fragment_vod, container, false);
        mainRecyclerView = root.findViewById(R.id.recycler_view_categories); // ID do novo RecyclerView principal
        progressBar = root.findViewById(R.id.progress_bar_vod);
        // chipGroupCategories = root.findViewById(R.id.chip_group_categories); // Não será mais usado

        // Configurar o listener para o ícone de pesquisa
        View searchIcon = root.findViewById(R.id.search_icon);
        if (searchIcon != null) {
            searchIcon.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            });
        }

        // Garantir que o LayoutManager esteja sempre definido na nova instância da view.
        if (mainRecyclerView != null && mainRecyclerView.getLayoutManager() == null) {
            Log.d(VOD_TAG, "onCreateView - Setting LayoutManager for mainRecyclerView.");
            mainRecyclerView.setLayoutManager(new LinearLayoutManager(getContext())); // Layout vertical para as categorias
        } else if (mainRecyclerView != null) {
            // Se já tem um layout manager (ex: do XML), não precisa redefinir, a menos que queira explicitamente.
            // A linha original mainRecyclerView.setLayoutManager(new LinearLayoutManager(getContext())); já fazia isso incondicionalmente.
            // Para manter o comportamento original caso o XML não defina um:
            mainRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }


        downloadReceiver = new DownloadReceiver();
        IntentFilter filter = new IntentFilter(DownloadService.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireActivity().registerReceiver(downloadReceiver, filter);
        }

        loadMovies();

        // Broadcast receiver for data refresh
        refreshDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ProfileFragment.ACTION_REFRESH_DATA.equals(intent.getAction())) {
                    Log.d(VOD_TAG, "ACTION_REFRESH_DATA received. Reloading movies.");
                     if (isAdded() && getContext() != null) { // Ensure fragment is attached
                        loadMovies();
                    }
                }
            }
        };

        return root;
    }

    private void loadMovies() {
        Log.d(VOD_TAG, "loadMovies() called");
        if (!isAdded() || getContext() == null) {
            Log.w(VOD_TAG, "loadMovies - Fragment not added or context is null. Aborting.");
            return;
        }
        showLoading(true);
        // Use getContext() instead of requireContext() after checking for null
        List<Movie> cachedMovies = MovieCacheManager.loadMoviesFromCache(getContext());
        Map<String, String> cachedCategoryMap = MovieCacheManager.loadCategoryMapFromCache(getContext());

        Log.d(VOD_TAG, "loadMovies - Cached movies size: " + (cachedMovies != null ? cachedMovies.size() : "null"));
        Log.d(VOD_TAG, "loadMovies - Cached category map size: " + (cachedCategoryMap != null ? cachedCategoryMap.size() : "null"));

        if (cachedMovies != null && !cachedMovies.isEmpty() && cachedCategoryMap != null && !cachedCategoryMap.isEmpty()) {
            Log.i(VOD_TAG, "loadMovies - Cache HIT: Loaded " + cachedMovies.size() + " movies and category map from cache.");
            allMovies = cachedMovies;
            categoryIdToNameMap = cachedCategoryMap;
            setupAndDisplayMovies();
            if (!MovieCacheManager.isCacheValid(getContext())) {
                 Log.i(VOD_TAG, "loadMovies - Cache is old or incomplete, scheduling a background refresh.");
                 fetchMoviesAndCategoriesFromApi(false);
            } else {
                showLoading(false); // Hide loading if cache is valid and used
            }
        } else {
            Log.i(VOD_TAG, "loadMovies - Cache MISS or incomplete: Fetching from API.");
            fetchMoviesAndCategoriesFromApi(true);
        }
    }

    private void fetchMoviesAndCategoriesFromApi(boolean showInitialLoading) {
        Log.d(VOD_TAG, "fetchMoviesAndCategoriesFromApi called - showInitialLoading: " + showInitialLoading);
        if (!isAdded() || getContext() == null) {
             Log.w(VOD_TAG, "fetchMoviesAndCategoriesFromApi - Fragment not usable. Aborting.");
            if (showInitialLoading) showLoading(false); // Ensure loading is hidden if we abort early
            return;
        }
        if (showInitialLoading) {
            showLoading(true);
        }

        fetchXtreamCredentials(new CredentialsCallback() {
            @Override
            public void onCredentialsReceived(String baseUrl, String username, String password) {
                if (!isAdded() || getContext() == null) return;
                Log.d(VOD_TAG, "fetchMoviesAndCategoriesFromApi - CredentialsReceived: " + baseUrl);
                fetchCategoryNames(baseUrl, username, password, new CategoryFetchCallback() {
                    @Override
                    public void onCategoriesReceived(Map<String, String> categoryMap) {
                        if (!isAdded() || getContext() == null) return;
                        Log.d(VOD_TAG, "fetchMoviesAndCategoriesFromApi - CategoriesReceived, count: " + categoryMap.size());
                        categoryIdToNameMap = categoryMap;
                        MovieCacheManager.saveCategoryMapToCache(getContext(), categoryIdToNameMap);

                        XtreamApiService apiService = new XtreamApiService(baseUrl, username, password);
                        apiService.fetchVodStreams(new XtreamApiService.XtreamApiCallback<Movie>() {
                            @Override
                            public void onSuccess(List<Movie> movies) {
                                if (!isAdded() || getContext() == null) return;
                                MovieCacheManager.saveMoviesToCache(getContext(), movies);
                                allMovies = movies;
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        if (!isAdded()) return;
                                        Log.i(VOD_TAG, "fetchMoviesAndCategoriesFromApi - VOD Streams onSuccess, count: " + movies.size());
                                        setupAndDisplayMovies();
                                        if (showInitialLoading) showLoading(false);
                                    });
                                }
                            }

                            @Override
                            public void onFailure(String error) {
                                if (!isAdded() || getActivity() == null) return;
                                getActivity().runOnUiThread(() -> {
                                    if (!isAdded() || getContext() == null) return;
                                    if (showInitialLoading) showLoading(false);
                                    Toast.makeText(getContext(), getString(R.string.error_loading_vod_streams, error), Toast.LENGTH_LONG).show();
                                    Log.e(VOD_TAG, "fetchMoviesAndCategoriesFromApi - VOD API Error: " + error);
                                });
                            }
                        });
                    }
                    @Override
                    public void onCategoryFetchFailure(String error) {
                        if (!isAdded() || getContext() == null) return;
                        Log.w(VOD_TAG, "fetchMoviesAndCategoriesFromApi - Failed to fetch category names ("+error+"), proceeding to fetch movies.");
                        XtreamApiService apiService = new XtreamApiService(baseUrl, username, password);
                        apiService.fetchVodStreams(new XtreamApiService.XtreamApiCallback<Movie>() {
                            @Override
                            public void onSuccess(List<Movie> movies) {
                                if (!isAdded() || getContext() == null) return;
                                MovieCacheManager.saveMoviesToCache(getContext(), movies);
                                allMovies = movies;
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        if (!isAdded() || getContext() == null) return;
                                        Log.i(VOD_TAG, "fetchMoviesAndCategoriesFromApi (after cat failure) - VOD Streams onSuccess, count: " + movies.size());
                                        setupAndDisplayMovies();
                                        if (showInitialLoading) showLoading(false);
                                        Toast.makeText(getContext(), getString(R.string.vod_categories_missing_toast), Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
                             @Override
                            public void onFailure(String movieError) {
                                 if (!isAdded() || getActivity() == null) return;
                                getActivity().runOnUiThread(() -> {
                                    if (!isAdded() || getContext() == null) return;
                                    if (showInitialLoading) showLoading(false);
                                    Toast.makeText(getContext(), getString(R.string.error_loading_vod_streams, movieError), Toast.LENGTH_LONG).show();
                                    Log.e(VOD_TAG, "fetchMoviesAndCategoriesFromApi (after cat failure) - VOD API Error: " + movieError);
                                });
                            }
                        });
                    }
                });
            }
            @Override
            public void onCredentialsFailure(String error) {
                if (!isAdded() || getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (!isAdded() || getContext() == null) return;
                    if (showInitialLoading) showLoading(false);
                    Toast.makeText(getContext(), getString(R.string.error_fetching_credentials, error), Toast.LENGTH_LONG).show();
                    Log.e(VOD_TAG, "fetchMoviesAndCategoriesFromApi - Credentials Error: " + error);
                });
            }
        });
    }

    private interface CredentialsCallback {
        void onCredentialsReceived(String baseUrl, String username, String password);
        void onCredentialsFailure(String error);
    }

    private void fetchXtreamCredentials(CredentialsCallback callback) {
        executor.execute(() -> {
            Log.d(VOD_TAG, "fetchXtreamCredentials called");
            if (!isAdded() || getContext() == null) { // Early exit
                Log.w(VOD_TAG, "fetchXtreamCredentials - Fragment not usable, aborting.");
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> callback.onCredentialsFailure("Fragment not available"));
                }
                return;
            }
            try {
                URL url = new URL("http://mybrasiltv.x10.mx/GetLoguin.php");
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

                    if (!server.toLowerCase().startsWith("http://") && !server.toLowerCase().startsWith("https://")) {
                        server = "http://" + server;
                    }

                    Log.i(VOD_TAG, "fetchXtreamCredentials - Credentials received: Server=" + server + ", User=" + user + ", Pass=***");
                    callback.onCredentialsReceived(server, user, pass);

                } else {
                    Log.e(VOD_TAG, "fetchXtreamCredentials - HTTP error code: " + responseCode);
                    callback.onCredentialsFailure("HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(VOD_TAG, "fetchXtreamCredentials - Error fetching Xtream credentials", e);
                callback.onCredentialsFailure(e.getMessage());
            }
        });
    }

    private void setupAndDisplayMovies() {
        Log.d(VOD_TAG, "setupAndDisplayMovies called");
        if (!isAdded() || getContext() == null || getView() == null) {
            Log.w(VOD_TAG, "setupAndDisplayMovies - Fragment not in a usable state. Aborting.");
            showLoading(false); // Attempt to hide loading if called in bad state
            return;
        }

        if (allMovies == null || allMovies.isEmpty()) {
            Log.w(VOD_TAG, "setupAndDisplayMovies - No movies to display.");
            Map<String, List<Movie>> emptyMap = new LinkedHashMap<>();
            if (categoryAdapter == null) {
                categoryAdapter = new CategoryAdapter(getContext(), emptyMap);
                Log.d(VOD_TAG, "setupAndDisplayMovies - Created new empty CategoryAdapter.");
            } else {
                categoryAdapter.updateData(emptyMap);
                Log.d(VOD_TAG, "setupAndDisplayMovies - Cleared existing CategoryAdapter.");
            }
            if (mainRecyclerView != null) {
                mainRecyclerView.setAdapter(categoryAdapter);
            } else {
                 Log.e(VOD_TAG, "setupAndDisplayMovies - mainRecyclerView is null, cannot set empty adapter.");
            }
            showLoading(false);
            return;
        }

        Map<String, List<Movie>> moviesByCategory = allMovies.stream()
                .collect(Collectors.groupingBy(movie -> {
                    String categoryId = movie.getCategory();
                    return categoryIdToNameMap.getOrDefault(categoryId, getContext().getString(R.string.label_other_category));
                }, LinkedHashMap::new, Collectors.toList()));
        Log.d(VOD_TAG, "setupAndDisplayMovies - Movies grouped, category count: " + moviesByCategory.size());

        if (categoryAdapter == null) {
            categoryAdapter = new CategoryAdapter(getContext(), moviesByCategory, this::openMovieDetails);
        } else {
            categoryAdapter.updateData(moviesByCategory);
            categoryAdapter.setOnMovieClickListener(this::openMovieDetails); // Ensure listener is set
        }

        if (mainRecyclerView != null) {
            mainRecyclerView.setAdapter(categoryAdapter);
            if (mainRecyclerView.getLayoutManager() == null) {
                mainRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            }
        } else {
            Log.e(VOD_TAG, "setupAndDisplayMovies - mainRecyclerView is null! Cannot set adapter.");
        }
        showLoading(false);
    }


    private interface CategoryFetchCallback {
        void onCategoriesReceived(Map<String, String> categoryMap);
        void onCategoryFetchFailure(String error);
    }

    private void fetchCategoryNames(String baseUrl, String username, String password, CategoryFetchCallback callback) {
        Log.d(VOD_TAG, "fetchCategoryNames called");
        if (!isAdded()){ // Early exit
            Log.w(VOD_TAG, "fetchCategoryNames - Fragment not added. Aborting.");
            callback.onCategoryFetchFailure("Fragment not available"); // Notify failure
            return;
        }
        XtreamApiService apiService = new XtreamApiService(baseUrl, username, password);
        apiService.fetchVodCategories(new XtreamApiService.XtreamApiCallback<XtreamApiService.CategoryInfo>() {
            @Override
            public void onSuccess(List<XtreamApiService.CategoryInfo> categoryInfos) {
                if (!isAdded() || getActivity() == null) return;
                Map<String, String> tempMap = new LinkedHashMap<>();
                for (XtreamApiService.CategoryInfo catInfo : categoryInfos) {
                    tempMap.put(catInfo.id, catInfo.name);
                }
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    Log.i(VOD_TAG, "fetchCategoryNames - onSuccess, category count: " + tempMap.size());
                    callback.onCategoriesReceived(tempMap);
                });
            }

            @Override
            public void onFailure(String error) {
                if (!isAdded() || getActivity() == null) return;
                Log.e(VOD_TAG, "fetchCategoryNames - onFailure: " + error);
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    callback.onCategoryFetchFailure(error);
                }); // Corrected: Added );
            }
        });
    }


    private void showLoading(boolean isLoading) {
        if (!isAdded() || getView() == null) return; // Check if view is available

        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (mainRecyclerView != null) {
            mainRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(VOD_TAG, "onDestroyView called");
        if (downloadReceiver != null) {
            try {
                if (getActivity() != null) { // Adicionada verificação de getActivity()
                    getActivity().unregisterReceiver(downloadReceiver);
                    Log.d(VOD_TAG, "DownloadReceiver unregistered.");
                } else {
                    Log.w(VOD_TAG, "onDestroyView - getActivity is null, cannot unregister DownloadReceiver.");
                }
            } catch (IllegalArgumentException e) {
                // Isso pode acontecer se o receiver não foi registrado ou já foi desregistrado.
                Log.w(VOD_TAG, "DownloadReceiver not registered or already unregistered.", e);
            }
        }
        // Limpar o adapter da RecyclerView para ajudar o GC e evitar problemas
        // se a instância do fragmento sobreviver mas a view for recriada.
        if (mainRecyclerView != null) {
            mainRecyclerView.setAdapter(null);
            Log.d(VOD_TAG, "onDestroyView - Set null adapter to mainRecyclerView.");
        }
        // Não é necessário nulificar mainRecyclerView ou progressBar aqui, pois são obtidos novamente em onCreateView.
        // categoryAdapter é uma variável de instância e sua persistência (ou não) depende do ciclo de vida do Fragmento em si.
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Broadcast receiver for data refresh
        refreshDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ProfileFragment.ACTION_REFRESH_DATA.equals(intent.getAction())) {
                    Log.d(VOD_TAG, "ACTION_REFRESH_DATA received. Reloading movies.");
                     if (isAdded() && getContext() != null) { // Ensure fragment is attached
                        loadMovies();
                    }
                }
            }
        };
        // Register refreshDataReceiver
        IntentFilter filter = new IntentFilter(ProfileFragment.ACTION_REFRESH_DATA);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(refreshDataReceiver, filter);
        Log.d(VOD_TAG, "refreshDataReceiver registered.");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Unregister refreshDataReceiver
        if (refreshDataReceiver != null) {
            try {
                LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(refreshDataReceiver);
                Log.d(VOD_TAG, "refreshDataReceiver unregistered.");
            } catch (IllegalArgumentException e) {
                Log.w(VOD_TAG, "refreshDataReceiver not registered or already unregistered.", e);
            }
        }
    }

    private class DownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String filePath = intent.getStringExtra(DownloadService.EXTRA_FILE_PATH);
            if (filePath != null) {
                Log.d(VOD_TAG, "DownloadReceiver received file: " + filePath + " but processM3uFile is no longer in use.");
                Toast.makeText(getContext(), getString(R.string.download_complete_manual_processing_toast), Toast.LENGTH_LONG).show();
            } else {
                showLoading(false);
                Toast.makeText(getContext(), getString(R.string.download_failed_toast), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(VOD_TAG, "onResume called");
        // Força o recarregamento dos filmes sempre que o fragmento se torna visível novamente
        // Isso garante que os dados sejam atualizados e exibidos corretamente.
        loadMovies();
    }

    private void openMovieDetails(Movie movie) {
        Intent intent = new Intent(getActivity(), MovieDetailsActivity.class);
        intent.putExtra("movie", movie);
        startActivity(intent);
    }
}


