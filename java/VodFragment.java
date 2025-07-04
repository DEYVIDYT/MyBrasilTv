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
    // private List<Movie> allMovies = new ArrayList<>(); // Data will come from DataManager
    // private Map<String, String> categoryIdToNameMap = new LinkedHashMap<>(); // Data will come from DataManager
    private static final String VOD_TAG = "VOD_DEBUG"; // Tag para logs
    // private DownloadReceiver downloadReceiver; // Will be removed
    private BroadcastReceiver refreshDataReceiver; // For data refresh
    private DataManager dataManager;

    // private final ExecutorService executor = Executors.newSingleThreadExecutor(); // Not needed if DataManager handles async

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

        dataManager = MyApplication.getDataManager();

        // downloadReceiver related code removed.

        setupAndDisplayMovies(); // Display data from DataManager

        // Broadcast receiver for data refresh
        refreshDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ProfileFragment.ACTION_REFRESH_DATA.equals(intent.getAction())) {
                    Log.d(VOD_TAG, "ACTION_REFRESH_DATA received. Re-displaying movies from DataManager.");
                     if (isAdded() && getContext() != null) { // Ensure fragment is attached
                        setupAndDisplayMovies(); // Re-fetch from DataManager and update UI
                    }
                }
            }
        };

        return root;
    }

    // All old data fetching methods (loadMovies, fetchMoviesAndCategoriesFromApi,
    // fetchXtreamCredentials, fetchCategoryNames) and their associated interfaces
    // (CredentialsCallback, CategoryFetchCallback) are removed.
    // DataManager now handles all data fetching.

    private void setupAndDisplayMovies() {
        Log.d(VOD_TAG, "setupAndDisplayMovies called");
        if (!isAdded() || getContext() == null || getView() == null || dataManager == null) {
            Log.w(VOD_TAG, "setupAndDisplayMovies - Fragment not in a usable state or DataManager is null. Aborting.");
            showLoading(false);
            return;
        }

        List<Movie> allMovies = dataManager.getVodStreams();
        List<XtreamApiService.CategoryInfo> vodCategories = dataManager.getVodCategories();
        Map<String, String> categoryIdToNameMap = new LinkedHashMap<>();
        if (vodCategories != null) {
            for (XtreamApiService.CategoryInfo catInfo : vodCategories) {
                categoryIdToNameMap.put(catInfo.id, catInfo.name);
            }
        }

        if (allMovies == null || allMovies.isEmpty()) {
            Log.w(VOD_TAG, "setupAndDisplayMovies - No movies to display from DataManager.");
            // Ensure UI is cleared or shows an empty state
            if (categoryAdapter != null) {
                categoryAdapter.updateData(new LinkedHashMap<>()); // Clear adapter
            } else {
                 categoryAdapter = new CategoryAdapter(getContext(), new LinkedHashMap<>(), this::openMovieDetails);
                 if (mainRecyclerView != null) mainRecyclerView.setAdapter(categoryAdapter);
            }
            showLoading(false);
            if (getContext() != null) { // Check context before showing Toast
                Toast.makeText(getContext(), "Nenhum filme/série encontrado.", Toast.LENGTH_SHORT).show();
            }
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
            if (mainRecyclerView != null) {
                 mainRecyclerView.setAdapter(categoryAdapter);
            }
        } else {
            categoryAdapter.updateData(moviesByCategory);
            categoryAdapter.setOnMovieClickListener(this::openMovieDetails);
        }

        if (mainRecyclerView != null && mainRecyclerView.getLayoutManager() == null) {
            mainRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        showLoading(false);
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
        // downloadReceiver and its unregistration removed.
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
                    Log.d(VOD_TAG, "ACTION_REFRESH_DATA received. Re-displaying movies from DataManager.");
                     if (isAdded() && getContext() != null) { // Ensure fragment is attached
                        setupAndDisplayMovies(); // Re-fetch from DataManager and update UI
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

    // DownloadReceiver inner class removed.

    @Override
    public void onResume() {
        super.onResume();
        Log.d(VOD_TAG, "onResume called");
        // Força o recarregamento dos filmes sempre que o fragmento se torna visível novamente
        // Isso garante que os dados sejam atualizados e exibidos corretamente.
        if (dataManager != null) { // Check if dataManager is initialized
            setupAndDisplayMovies();
        } else {
            Log.w(VOD_TAG, "onResume - DataManager is null. Cannot refresh movies. This might indicate an issue with MainActivity flow.");
            // Optionally, redirect to DownloadProgressActivity or show an error
        }
    }

    private void openMovieDetails(Movie movie) {
        Intent intent = new Intent(getActivity(), MovieDetailsActivity.class);
        intent.putExtra("movie", movie);
        startActivity(intent);
    }
}


