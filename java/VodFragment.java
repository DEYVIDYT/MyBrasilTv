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
    private static final String VOD_TAG = "VOD_DEBUG"; // Tag para logs
    private DownloadReceiver downloadReceiver;

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
        return root;
    }

    private void loadMovies() {
        Log.d(VOD_TAG, "loadMovies() called");
        showLoading(true);
        List<Movie> cachedMovies = MovieCacheManager.loadMoviesFromCache(requireContext());
        Map<String, String> cachedCategoryMap = MovieCacheManager.loadCategoryMapFromCache(requireContext());

        Log.d(VOD_TAG, "loadMovies - Cached movies size: " + cachedMovies.size());
        Log.d(VOD_TAG, "loadMovies - Cached category map size: " + cachedCategoryMap.size());

        if (!cachedMovies.isEmpty() && !cachedCategoryMap.isEmpty()) {
            Log.i(VOD_TAG, "loadMovies - Cache HIT: Loaded " + cachedMovies.size() + " movies and category map from cache.");
            allMovies = cachedMovies;
            categoryIdToNameMap = cachedCategoryMap;
            setupAndDisplayMovies();
            if (!MovieCacheManager.isCacheValid(requireContext())) {
                 Log.i(VOD_TAG, "loadMovies - Cache is old or incomplete, scheduling a background refresh.");
                 fetchMoviesAndCategoriesFromApi(false);
            }
        } else {
            Log.i(VOD_TAG, "loadMovies - Cache MISS or incomplete: Fetching from API.");
            fetchMoviesAndCategoriesFromApi(true);
        }
    }

    private void fetchMoviesAndCategoriesFromApi(boolean showInitialLoading) {
        Log.d(VOD_TAG, "fetchMoviesAndCategoriesFromApi called - showInitialLoading: " + showInitialLoading);
        if (showInitialLoading) {
            showLoading(true);
        }

        fetchXtreamCredentials(new CredentialsCallback() {
            @Override
            public void onCredentialsReceived(String baseUrl, String username, String password) {
                Log.d(VOD_TAG, "fetchMoviesAndCategoriesFromApi - CredentialsReceived: " + baseUrl);
                fetchCategoryNames(baseUrl, username, password, new CategoryFetchCallback() {
                    @Override
                    public void onCategoriesReceived(Map<String, String> categoryMap) {
                        Log.d(VOD_TAG, "fetchMoviesAndCategoriesFromApi - CategoriesReceived, count: " + categoryMap.size());
                        categoryIdToNameMap = categoryMap;
                        MovieCacheManager.saveCategoryMapToCache(requireContext(), categoryIdToNameMap);

                        XtreamApiService apiService = new XtreamApiService(baseUrl, username, password);
                        apiService.fetchVodStreams(new XtreamApiService.XtreamApiCallback<Movie>() {
                            @Override
                            public void onSuccess(List<Movie> movies) {
                                MovieCacheManager.saveMoviesToCache(requireContext(), movies);
                                allMovies = movies;
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> { // Log já existe na API Service, mas um aqui também é bom
                                        Log.i(VOD_TAG, "fetchMoviesAndCategoriesFromApi - VOD Streams onSuccess, count: " + movies.size());
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
                                        Toast.makeText(getContext(), getString(R.string.error_loading_vod_streams, error), Toast.LENGTH_LONG).show();
                                        Log.e(VOD_TAG, "fetchMoviesAndCategoriesFromApi - VOD API Error: " + error);
                                    });
                                }
                            }
                        });
                    }
                    @Override
                    public void onCategoryFetchFailure(String error) {
                        Log.w(VOD_TAG, "fetchMoviesAndCategoriesFromApi - Failed to fetch category names ("+error+"), proceeding to fetch movies.");
                        // Mesmo com falha nas categorias, tenta buscar filmes
                        XtreamApiService apiService = new XtreamApiService(baseUrl, username, password);
                        apiService.fetchVodStreams(new XtreamApiService.XtreamApiCallback<Movie>() {
                            @Override
                            public void onSuccess(List<Movie> movies) {
                                MovieCacheManager.saveMoviesToCache(requireContext(), movies);
                                allMovies = movies;
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        Log.i(VOD_TAG, "fetchMoviesAndCategoriesFromApi (after cat failure) - VOD Streams onSuccess, count: " + movies.size());
                                        setupAndDisplayMovies();
                                        if (showInitialLoading) showLoading(false);
                                        Toast.makeText(getContext(), getString(R.string.vod_categories_missing_toast), Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
                             @Override
                            public void onFailure(String movieError) {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        if (showInitialLoading) showLoading(false);
                                        Toast.makeText(getContext(), getString(R.string.error_loading_vod_streams, movieError), Toast.LENGTH_LONG).show();
                                        Log.e(VOD_TAG, "fetchMoviesAndCategoriesFromApi (after cat failure) - VOD API Error: " + movieError);
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
                        Toast.makeText(getContext(), getString(R.string.error_fetching_credentials, error), Toast.LENGTH_LONG).show();
                        Log.e(VOD_TAG, "fetchMoviesAndCategoriesFromApi - Credentials Error: " + error);
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
            Log.d(VOD_TAG, "fetchXtreamCredentials called");
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

        if (allMovies == null || allMovies.isEmpty()) {
            Log.w(VOD_TAG, "setupAndDisplayMovies - No movies to display (allMovies is null or empty).");
            Map<String, List<Movie>> emptyMap = new LinkedHashMap<>();
            if (categoryAdapter == null) {
                // Se o adapter não existe, criamos um novo com dados vazios.
                // Isso pode acontecer se setupAndDisplayMovies for chamado antes de qualquer carregamento bem-sucedido.
                if (getContext() != null) { // Evitar crash se o contexto for nulo
                    categoryAdapter = new CategoryAdapter(getContext(), emptyMap);
                    Log.d(VOD_TAG, "setupAndDisplayMovies - Created new empty CategoryAdapter.");
                } else {
                    Log.e(VOD_TAG, "setupAndDisplayMovies - Context is null, cannot create CategoryAdapter.");
                    showLoading(false);
                    return; // Não podemos prosseguir sem contexto
                }
            } else {
                // Se o adapter já existe, apenas atualizamos com dados vazios.
                Log.d(VOD_TAG, "setupAndDisplayMovies - Clearing existing CategoryAdapter.");
                categoryAdapter.updateData(emptyMap);
            }
            // Sempre defina o adapter na RecyclerView para garantir que a UI reflita o estado vazio.
            if (mainRecyclerView != null) {
                mainRecyclerView.setAdapter(categoryAdapter);
                Log.d(VOD_TAG, "setupAndDisplayMovies - Set empty/cleared adapter to mainRecyclerView.");
            } else {
                Log.e(VOD_TAG, "setupAndDisplayMovies - mainRecyclerView is null, cannot set empty adapter.");
            }
            showLoading(false);
            return;
        }

        // Agrupar filmes por categoria
        Map<String, List<Movie>> moviesByCategory = allMovies.stream()
                .collect(Collectors.groupingBy(movie -> {
                    String categoryId = movie.getCategory();
                    // Usar getString para "Outros"
                    return categoryIdToNameMap.getOrDefault(categoryId, getContext() != null ? getContext().getString(R.string.label_other_category) : "Outros");
                }, LinkedHashMap::new, Collectors.toList())); // Manter a ordem de inserção
        Log.d(VOD_TAG, "setupAndDisplayMovies - Movies grouped by category. Number of categories: " + moviesByCategory.size());

        // Se 'All' for uma categoria, mover para o início ou tratar separadamente
        // No novo layout, 'All' não faz sentido como uma categoria separada, mas sim como um filtro.
        // Aqui, estamos exibindo todas as categorias disponíveis.

        if (categoryAdapter == null) {
            Log.d(VOD_TAG, "setupAndDisplayMovies - Creating new CategoryAdapter with movie data.");
            if (getContext() != null) {
                categoryAdapter = new CategoryAdapter(getContext(), moviesByCategory);
            } else {
                Log.e(VOD_TAG, "setupAndDisplayMovies - Context is null, cannot create CategoryAdapter with movie data.");
                showLoading(false);
                return; // Não podemos prosseguir sem contexto
            }
        } else {
            Log.d(VOD_TAG, "setupAndDisplayMovies - Updating existing CategoryAdapter with movie data.");
            categoryAdapter.updateData(moviesByCategory);
        }

        if (mainRecyclerView != null) {
            // Sempre (re)defina o adapter na RecyclerView.
            // Isso é crucial se a view do fragmento (e, portanto, mainRecyclerView) foi recriada.
            mainRecyclerView.setAdapter(categoryAdapter);
            Log.d(VOD_TAG, "setupAndDisplayMovies - Adapter with data set/reset on mainRecyclerView.");

            // Garantir que o LayoutManager também esteja presente.
            if (mainRecyclerView.getLayoutManager() == null) {
                Log.w(VOD_TAG, "setupAndDisplayMovies - LayoutManager was null, re-setting.");
                if (getContext() != null) {
                    mainRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                } else {
                    Log.e(VOD_TAG, "setupAndDisplayMovies - Context is null, cannot set LayoutManager.");
                }
            }
        } else {
            Log.e(VOD_TAG, "setupAndDisplayMovies - mainRecyclerView is null! Cannot set adapter with data.");
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
        Log.d(VOD_TAG, "fetchCategoryNames called");
        XtreamApiService apiService = new XtreamApiService(baseUrl, username, password);
        apiService.fetchVodCategories(new XtreamApiService.XtreamApiCallback<XtreamApiService.CategoryInfo>() {
            @Override
            public void onSuccess(List<XtreamApiService.CategoryInfo> categoryInfos) {
                Map<String, String> tempMap = new LinkedHashMap<>(); // Usar LinkedHashMap para manter a ordem
                for (XtreamApiService.CategoryInfo catInfo : categoryInfos) {
                    tempMap.put(catInfo.id, catInfo.name);
                    // Log.d(VOD_TAG, "Fetched category: ID=" + catInfo.id + ", Name=" + catInfo.name); // Pode ser muito verboso
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.i(VOD_TAG, "fetchCategoryNames - onSuccess, category count: " + tempMap.size());
                        callback.onCategoriesReceived(tempMap);
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e(VOD_TAG, "fetchCategoryNames - onFailure: " + error);
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
}


