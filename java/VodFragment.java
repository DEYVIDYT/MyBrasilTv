package com.example.iptvplayer;

import android.Manifest;
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
    private DownloadReceiver downloadReceiver;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final String VOD_URL = ""; // URL para conteúdo VOD, se aplicável. Deixe vazio se o conteúdo VOD for local ou gerenciado de outra forma.

    private final ActivityResultLauncher<String> requestPermissionLauncher = 
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                startDownload();
            } else {
                Toast.makeText(getContext(), "Permission for notifications is required to see download progress.", Toast.LENGTH_LONG).show();
                // You could also start the download here, but the user won't see the notification
                 startDownload(); 
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

        checkNotificationPermissionAndStartDownload();
        return root;
    }

    private void checkNotificationPermissionAndStartDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                startDownload();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            startDownload();
        }
    }

    private void startDownload() {
        showLoading(true);
        // Para fins de demonstração, vamos simular o carregamento de um arquivo local
        // Em um aplicativo real, você carregaria o conteúdo VOD de uma API ou de um arquivo local específico para VOD.
        // Por enquanto, vamos criar um arquivo M3U de exemplo para VOD.
        executor.execute(() -> {
            try {
                // Simular um arquivo M3U de VOD usando StringBuilder para evitar problemas de sintaxe
                StringBuilder vodContentBuilder = new StringBuilder();
                vodContentBuilder.append("#EXTM3U\n");
                vodContentBuilder.append("#EXTINF:-1 tvg-id=\"Movie1\" tvg-name=\"O Castelo do Lobisomem\" ");
                vodContentBuilder.append("tvg-logo=\"https://image.tmdb.org/t/p/w500/example1.jpg\" ");
                vodContentBuilder.append("group-title=\"FILMES DIVERSOS\",O Castelo do Lobisomem (2022)\n");
                vodContentBuilder.append("http://example.com/movie1.mp4\n");
                
                vodContentBuilder.append("#EXTINF:-1 tvg-id=\"Movie2\" tvg-name=\"Divida de Honra\" ");
                vodContentBuilder.append("tvg-logo=\"https://image.tmdb.org/t/p/w500/example2.jpg\" ");
                vodContentBuilder.append("group-title=\"FILMES DIVERSOS\",Divida de Honra (2023)\n");
                vodContentBuilder.append("http://example.com/movie2.mp4\n");
                
                vodContentBuilder.append("#EXTINF:-1 tvg-id=\"Movie3\" tvg-name=\"O Comando\" ");
                vodContentBuilder.append("tvg-logo=\"https://image.tmdb.org/t/p/w500/example3.jpg\" ");
                vodContentBuilder.append("group-title=\"FILMES DIVERSOS\",O Comando (2022)\n");
                vodContentBuilder.append("http://example.com/movie3.mp4\n");
                
                vodContentBuilder.append("#EXTINF:-1 tvg-id=\"Movie4\" tvg-name=\"Como Treinar o Seu Dragao\" ");
                vodContentBuilder.append("tvg-logo=\"https://image.tmdb.org/t/p/w500/example4.jpg\" ");
                vodContentBuilder.append("group-title=\"FILMES: [Qualidade Cinema]\",Como Treinar o Seu Dragao (2020)\n");
                vodContentBuilder.append("http://example.com/movie4.mp4\n");
                
                vodContentBuilder.append("#EXTINF:-1 tvg-id=\"Movie5\" tvg-name=\"Confinado\" ");
                vodContentBuilder.append("tvg-logo=\"https://image.tmdb.org/t/p/w500/example5.jpg\" ");
                vodContentBuilder.append("group-title=\"FILMES: [Qualidade Cinema]\",Confinado (2020)\n");
                vodContentBuilder.append("http://example.com/movie5.mp4\n");
                
                vodContentBuilder.append("#EXTINF:-1 tvg-id=\"Movie6\" tvg-name=\"Bailarina\" ");
                vodContentBuilder.append("tvg-logo=\"https://image.tmdb.org/t/p/w500/example6.jpg\" ");
                vodContentBuilder.append("group-title=\"FILMES: [Qualidade Cinema]\",Bailarina (2022)\n");
                vodContentBuilder.append("http://example.com/movie6.mp4\n");
                
                String vodContent = vodContentBuilder.toString();

                File tempFile = new File(requireContext().getCacheDir(), "vod_example.m3u");
                try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
                    writer.write(vodContent);
                }
                processM3uFile(tempFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(getContext(), "Failed to create example VOD file.", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void processM3uFile(String filePath) {
        executor.execute(() -> {
            try {
                File file = new File(filePath);
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    allMovies = M3uParser.parseMovies(br);
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        setupCategoryChips(allMovies);
                        movieAdapter = new MovieAdapter(allMovies);
                        recyclerView.setAdapter(movieAdapter);
                        showLoading(false);
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(getContext(), "Failed to process list", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void setupCategoryChips(List<Movie> movies) {
        Set<String> categories = new LinkedHashSet<>();
        categories.add("All");
        for (Movie movie : movies) {
            if (movie.getCategory() != null && !movie.getCategory().isEmpty()) {
                categories.add(movie.getCategory());
            }
        }

        chipGroupCategories.removeAllViews();
        for (String category : categories) {
            Chip chip = new Chip(getContext());
            chip.setText(category);
            chip.setCheckable(true);
            chipGroupCategories.addView(chip);
        }

        if (chipGroupCategories.getChildCount() > 0) {
            ((Chip) chipGroupCategories.getChildAt(0)).setChecked(true);
        }

        chipGroupCategories.setOnCheckedChangeListener((group, checkedId) -> {
            Chip selectedChip = group.findViewById(checkedId);
            if (movieAdapter != null && selectedChip != null) {
                movieAdapter.filterByCategory(selectedChip.getText().toString());
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
                processM3uFile(filePath);
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