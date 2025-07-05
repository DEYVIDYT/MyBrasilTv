package com.example.iptvplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptvplayer.adapter.CategoryAdapter;
import com.example.iptvplayer.data.Movie;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VodFragment extends Fragment implements DataManager.DataManagerListener {

    private static final String VOD_TAG = "VOD_DEBUG";

    private RecyclerView mainRecyclerView;
    private CategoryAdapter categoryAdapter;
    private ProgressBar progressBar;
    private DataManager dataManager;
    private BroadcastReceiver refreshDataReceiver;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d(VOD_TAG, "onAttach called");
        // Get DataManager and set listener
        dataManager = MyApplication.getDataManager(context);
        dataManager.setListener(this);

        // Register broadcast receiver for data refresh requests
        refreshDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ProfileFragment.ACTION_REFRESH_DATA.equals(intent.getAction())) {
                    Log.d(VOD_TAG, "ACTION_REFRESH_DATA received. Re-starting data loading.");
                    updateUi(); // Update UI to show loading indicator
                    dataManager.startDataLoading();
                }
            }
        };
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(refreshDataReceiver, new IntentFilter(ProfileFragment.ACTION_REFRESH_DATA));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(VOD_TAG, "onCreateView called");
        View root = inflater.inflate(R.layout.fragment_vod, container, false);

        // Initialize Views
        progressBar = root.findViewById(R.id.progress_bar_vod);
        mainRecyclerView = root.findViewById(R.id.recycler_view_categories);

        // Setup RecyclerView with an empty adapter initially
        mainRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        categoryAdapter = new CategoryAdapter(getContext(), new LinkedHashMap<>(), this::openMovieDetails);
        mainRecyclerView.setAdapter(categoryAdapter);

        // Setup search icon click listener
        View searchIcon = root.findViewById(R.id.search_icon);
        if (searchIcon != null) {
            searchIcon.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            });
        }

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(VOD_TAG, "onResume called");
        // Update the UI every time the fragment becomes visible
        updateUi();
    }

    @Override
    public void onDataLoaded() {
        Log.d(VOD_TAG, "onDataLoaded callback received.");
        // When data is loaded, update the UI
        if (isAdded()) { // Ensure fragment is still attached
            updateUi();
        }
    }

    private void updateUi() {
        Log.d(VOD_TAG, "updateUi called");
        if (dataManager != null && dataManager.isDataFullyLoaded()) {
            Log.d(VOD_TAG, "Data is fully loaded. Displaying movies.");
            setupAndDisplayMovies();
        } else {
            Log.d(VOD_TAG, "Data not loaded. Displaying loading indicator.");
            showLoading(true);
        }
    }

    private void setupAndDisplayMovies() {
        if (!isAdded() || getContext() == null) {
            Log.w(VOD_TAG, "setupAndDisplayMovies aborted: fragment not in a usable state.");
            return;
        }

        List<Movie> allMovies = dataManager.getVodStreams();
        if (allMovies == null || allMovies.isEmpty()) {
            Log.w(VOD_TAG, "No movies to display from DataManager.");
            categoryAdapter.updateData(new LinkedHashMap<>()); // Clear adapter
            showLoading(false);
            Toast.makeText(getContext(), "Nenhum filme/série encontrado.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<XtreamApiService.CategoryInfo> vodCategories = dataManager.getVodCategories();
        Map<String, String> categoryIdToNameMap = new LinkedHashMap<>();
        if (vodCategories != null) {
            for (XtreamApiService.CategoryInfo catInfo : vodCategories) {
                categoryIdToNameMap.put(catInfo.id, catInfo.name);
            }
        }

        Map<String, List<Movie>> moviesByCategory = allMovies.stream()
                .collect(Collectors.groupingBy(movie -> {
                    String categoryId = movie.getCategory();
                    return categoryIdToNameMap.getOrDefault(categoryId, getContext().getString(R.string.label_other_category));
                }, LinkedHashMap::new, Collectors.toList()));

        Log.d(VOD_TAG, "Updating adapter with " + moviesByCategory.size() + " categories.");
        categoryAdapter.updateData(moviesByCategory);
        showLoading(false);
    }

    @Override
    public void onProgressUpdate(DataManager.LoadState state, int percentage, String message) {
        // This can be used to show more detailed progress if needed
    }

    @Override
    public void onError(String errorMessage) {
        Log.e(VOD_TAG, "DataManager Error: " + errorMessage);
        if (isAdded() && getContext() != null) {
            showLoading(false);
            Toast.makeText(getContext(), "Erro ao carregar dados: " + errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (mainRecyclerView != null) {
            mainRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
    }

    private void openMovieDetails(Movie movie) {
        Intent intent = new Intent(getActivity(), MovieDetailsActivity.class);
        intent.putExtra("movie", movie);
        startActivity(intent);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(VOD_TAG, "onDetach called");
        // Unset listener and unregister receiver to prevent memory leaks
        if (dataManager != null) {
            dataManager.setListener(null);
        }
        if (refreshDataReceiver != null) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(refreshDataReceiver);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(VOD_TAG, "onDestroyView called");
        // Release resources tied to the view
        if (mainRecyclerView != null) {
            mainRecyclerView.setAdapter(null);
        }
    }
}
