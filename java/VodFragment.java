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
    private DownloadReceiver downloadReceiver;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Log.d("VodFragment", "Notification permission granted.");
            } else {
                Toast.makeText(getContext(), "Permission for notifications is required to see download progress.", Toast.LENGTH_LONG).show();
                Log.w("VodFragment", "Notification permission denied.");
            }
        });


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_vod, container, false);
        mainRecyclerView = root.findViewById(R.id.recycler_view_categories); // ID do novo RecyclerView principal
        progressBar = root.findViewById(R.id.progress_bar_vod);
        // chipGroupCategories = root.findViewById(R.id.chip_group_categories); // Não será mais usado

        mainRecyclerView.setLayoutManager(new LinearLayoutManager(getContext())); // Layout vertical para as categorias

        downloadReceiver = new DownloadReceiver();
        IntentFilter filter = new IntentFilter(DownloadService.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireActivity().registerReceiver(downloadReceiver, filter);
        }

        loadMovies();
        return root;
    }

    private void loadMovies() {
        showLoading(true);
        List<Movie> cachedMovies = MovieCacheManager.loadMoviesFromCache(requireContext());
        Map<String, String> cachedCategoryMap = MovieCacheManager.loadCategoryMapFromCache(requireContext());

        if (!cachedMovies.isEmpty() && !cachedCategoryMap.isEmpty()) {
            Log.i("VodFragment", "Loaded " + cachedMovies.size() + " movies and category map from cache.");
            allMovies = cachedMovies;
            categoryIdToNameMap = cachedCategoryMap;
            setupAndDisplayMovies();
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

        fetchXtreamCredentials(new CredentialsCallback() {
            @Override
            public void onCredentialsReceived(String baseUrl, String username, String password) {
                fetchCategoryNames(baseUrl, username, password, new CategoryFetchCallback() {
                    @Override
                    public void onCategoriesReceived(Map<String, String> categoryMap) {
                        categoryIdToNameMap = categoryMap;
                        MovieCacheManager.saveCategoryMapToCache(requireContext(), categoryIdToNameMap);

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
                        Log.w("VodFragment", "Failed to fetch category names ("+error+"), proceeding to fetch movies.");
                        XtreamApiService apiService = new XtreamApiService(baseUrl, username, password);
                        apiService.fetchVodStreams(new XtreamApiService.XtreamApiCallback<Movie>() {
                            @Override
                            public void onSuccess(List<Movie> movies) {
                                MovieCacheManager.saveMoviesToCache(requireContext(), movies);
                                allMovies = movies;
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        setupAndDisplayMovies();
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
                                        Toast.makeText(getContext(), "Failed to load VOD streams: " + movieError, Toast.LENGTH_LONG).show();
                                        Log.e("VodFragment", "VOD API Error: " + movieError);
                                    });
                                }
                            }
                        });
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
            if (categoryAdapter != null) categoryAdapter.updateData(new LinkedHashMap<>()); // Limpar o adapter
            showLoading(false);
            return;
        }

        // Agrupar filmes por categoria
        Map<String, List<Movie>> moviesByCategory = allMovies.stream()
                .collect(Collectors.groupingBy(movie -> {
                    String categoryId = movie.getCategory();
                    return categoryIdToNameMap.getOrDefault(categoryId, "Outros"); // Usar nome da categoria ou 'Outros'
                }, LinkedHashMap::new, Collectors.toList())); // Manter a ordem de inserção

        // Se 'All' for uma categoria, mover para o início ou tratar separadamente
        // No novo layout, 'All' não faz sentido como uma categoria separada, mas sim como um filtro.
        // Aqui, estamos exibindo todas as categorias disponíveis.

        if (categoryAdapter == null) {
            categoryAdapter = new CategoryAdapter(getContext(), moviesByCategory);
            mainRecyclerView.setAdapter(categoryAdapter);
        } else {
            categoryAdapter.updateData(moviesByCategory);
        }
        showLoading(false);
    }

    // O método setupCategoryChips não é mais necessário com o novo layout
    // private void setupCategoryChips(List<Movie> movies) { ... }

    private interface CategoryFetchCallback {
        void onCategoriesReceived(Map<String, String> categoryMap);
        void onCategoryFetchFailure(String error);
    }

    private void fetchCategoryNames(String baseUrl, String username, String password, CategoryFetchCallback callback) {
        XtreamApiService apiService = new XtreamApiService(baseUrl, username, password);
        apiService.fetchVodCategories(new XtreamApiService.XtreamApiCallback<XtreamApiService.CategoryInfo>() {
            @Override
            public void onSuccess(List<XtreamApiService.CategoryInfo> categoryInfos) {
                Map<String, String> tempMap = new LinkedHashMap<>(); // Usar LinkedHashMap para manter a ordem
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
        mainRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        // chipGroupCategories.setVisibility(isLoading ? View.GONE : View.VISIBLE); // Não será mais usado
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
                Log.d("VodFragment", "DownloadReceiver received file: " + filePath + " but processM3uFile is no longer in use.");
                Toast.makeText(getContext(), "File download complete (manual processing needed).", Toast.LENGTH_LONG).show();
            } else {
                showLoading(false);
                Toast.makeText(getContext(), "Download failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("VodFragmentLifecycle", "onResume called. Calling loadMovies().");
        // Força o recarregamento dos filmes sempre que o fragmento se torna visível novamente
        // Isso garante que os dados sejam atualizados e exibidos corretamente.
        loadMovies();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("VodFragmentLifecycle", "onViewCreated called.");
        // loadMovies() é chamado em onCreateView (que chama antes de onViewCreated)
        // e também em onResume. A chamada em onResume é a principal para garantir
        // que os dados estejam atualizados ao voltar para o fragmento.
    }

    private void setupAndDisplayMovies() {
        Log.d("VodFragmentLogic", "setupAndDisplayMovies called.");
        if (allMovies == null || allMovies.isEmpty()) {
            Log.w("VodFragmentLogic", "No movies to display. allMovies is null or empty.");
            if (categoryAdapter != null) {
                Log.d("VodFragmentLogic", "Clearing categoryAdapter as allMovies is empty.");
                categoryAdapter.updateData(new LinkedHashMap<>()); // Limpar o adapter
            }
            showLoading(false);
            return;
        }

        Log.d("VodFragmentLogic", "Total movies to process: " + allMovies.size());
        Log.d("VodFragmentLogic", "Category ID to Name Map size: " + categoryIdToNameMap.size());

        Map<String, List<Movie>> moviesByCategory = allMovies.stream()
                .collect(Collectors.groupingBy(movie -> {
                    String categoryId = movie.getCategory();
                    String categoryName = categoryIdToNameMap.getOrDefault(categoryId, "Outros");
                    return categoryName;
                }, LinkedHashMap::new, Collectors.toList()));

        Log.d("VodFragmentLogic", "Number of categories with movies: " + moviesByCategory.size());
        if (moviesByCategory.isEmpty() && !allMovies.isEmpty()){
            Log.w("VodFragmentLogic", "moviesByCategory is empty, but allMovies is not. Check category mapping.");
        }

        if (categoryAdapter == null) {
            Log.d("VodFragmentLogic", "categoryAdapter is null, creating new instance.");
            categoryAdapter = new CategoryAdapter(getContext(), moviesByCategory);
            mainRecyclerView.setAdapter(categoryAdapter);
        } else {
            Log.d("VodFragmentLogic", "categoryAdapter exists, calling updateData.");
            categoryAdapter.updateData(moviesByCategory);
        }
        showLoading(false);
        Log.d("VodFragmentLogic", "setupAndDisplayMovies completed.");
    }
}


