package com.example.iptvplayer;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.iptvplayer.data.Movie;
import com.squareup.picasso.Picasso; // Para carregar imagens na Hero section

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VodFragmentTv extends Fragment implements DataManager.DataManagerListener {

    private static final String VOD_TV_TAG = "VOD_TV_DEBUG";

    private RecyclerView recyclerViewCategoriesTv;
    private ProgressBar progressBarVodTv;
    private DataManager dataManager;

    // Hero Section Views
    private ImageView heroBackgroundImage;
    private TextView heroTitle, heroDuration, heroYear, heroRating, heroGenres, heroDescription, heroCast;
    // private Button heroButtonWatch, heroButtonFavorite; // Ações da Hero section

    // TODO: Criar um novo CategoryAdapterTv que use item_movie_category_row_tv e MovieAdapterTv que use item_movie_tv
    // Por enquanto, usaremos um placeholder ou tentaremos adaptar o existente se for simples.
    // private CategoryAdapterTv categoryAdapterTv;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d(VOD_TV_TAG, "onAttach called");
        dataManager = MyApplication.getDataManager(context);
        dataManager.setListener(this); // Definir listener para atualizações de dados
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(VOD_TV_TAG, "onCreateView called");
        View root = inflater.inflate(R.layout.fragment_vod_tv, container, false);

        // Initialize Hero Section Views
        heroBackgroundImage = root.findViewById(R.id.hero_background_image);
        heroTitle = root.findViewById(R.id.hero_title);
        heroDuration = root.findViewById(R.id.hero_duration);
        heroYear = root.findViewById(R.id.hero_year);
        heroRating = root.findViewById(R.id.hero_rating);
        heroGenres = root.findViewById(R.id.hero_genres);
        heroDescription = root.findViewById(R.id.hero_description);
        heroCast = root.findViewById(R.id.hero_cast);
        // heroButtonWatch = root.findViewById(R.id.hero_button_watch);
        // heroButtonFavorite = root.findViewById(R.id.hero_button_favorite);

        // Initialize other views
        progressBarVodTv = root.findViewById(R.id.progress_bar_vod_tv);
        recyclerViewCategoriesTv = root.findViewById(R.id.recycler_view_vod_categories_tv);
        recyclerViewCategoriesTv.setLayoutManager(new LinearLayoutManager(getContext()));
        // recyclerViewCategoriesTv.setFocusable(true); // O NestedScrollView gerencia o foco inicial

        // TODO: Inicializar e setar o CategoryAdapterTv
        // categoryAdapterTv = new CategoryAdapterTv(getContext(), new LinkedHashMap<>(), this::openMovieDetailsTv);
        // recyclerViewCategoriesTv.setAdapter(categoryAdapterTv);

        Log.d(VOD_TV_TAG, "Views initialized.");
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(VOD_TV_TAG, "onResume called");
        updateUi(); // Tenta atualizar a UI caso os dados já estejam carregados
    }

    @Override
    public void onDataLoaded() {
        Log.d(VOD_TV_TAG, "onDataLoaded callback received.");
        if (isAdded() && getContext() != null) {
            updateUi();
        }
    }

    @Override
    public void onProgressUpdate(DataManager.LoadState state, int percentage, String message) {
        if (state == DataManager.LoadState.COMPLETE || state == DataManager.LoadState.ERROR) {
            showLoading(false);
        } else {
            showLoading(true);
        }
    }

    @Override
    public void onError(String errorMessage) {
        Log.e(VOD_TV_TAG, "DataManager Error: " + errorMessage);
        if (isAdded() && getContext() != null) {
            showLoading(false);
            Toast.makeText(getContext(), "Erro ao carregar dados VOD TV: " + errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void updateUi() {
        Log.d(VOD_TV_TAG, "updateUi called");
        if (dataManager != null && dataManager.isDataFullyLoaded()) {
            Log.d(VOD_TV_TAG, "Data is fully loaded. Displaying movies for TV.");
            showLoading(false);
            populateHeroSection();
            populateCategories();
        } else {
            Log.d(VOD_TV_TAG, "Data not loaded for TV. Displaying loading indicator.");
            showLoading(true);
            if (dataManager != null && !dataManager.isLoading()) { // Se não estiver carregando, iniciar
                dataManager.startDataLoading();
            }
        }
    }

    private void showLoading(boolean isLoading) {
        if (progressBarVodTv != null) {
            progressBarVodTv.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (recyclerViewCategoriesTv != null) {
            recyclerViewCategoriesTv.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
        // A hero section pode ficar visível, mas com placeholders ou vazia até os dados carregarem
    }

    private void populateHeroSection() {
        // Pegar o primeiro filme da lista como destaque, por exemplo
        List<Movie> allMovies = dataManager.getVodStreams();
        if (allMovies != null && !allMovies.isEmpty()) {
            Movie featuredMovie = allMovies.get(0); // Exemplo simples
            Log.d(VOD_TV_TAG, "Populating Hero Section with: " + featuredMovie.getName());

            heroTitle.setText(featuredMovie.getName());
            // heroDuration.setText(featuredMovie.getDuration() != null ? featuredMovie.getDuration() : "N/A");
            // heroYear.setText(featuredMovie.getYear() != null ? featuredMovie.getYear() : "N/A");
            // heroRating.setText(featuredMovie.getRating() != null ? featuredMovie.getRating() : "N/A");
            // heroGenres.setText(featuredMovie.getGenre() != null ? featuredMovie.getGenre() : "N/A");
            // heroDescription.setText(featuredMovie.getPlot() != null ? featuredMovie.getPlot() : "No description available.");
            // heroCast.setText(featuredMovie.getCast() != null ? featuredMovie.getCast() : "");


            if (featuredMovie.getStreamIcon() != null && !featuredMovie.getStreamIcon().isEmpty()) {
                Picasso.get()
                        .load(featuredMovie.getStreamIcon())
                        .placeholder(R.drawable.hero_section_background_gradient) // Um placeholder enquanto carrega
                        .error(R.drawable.hero_section_background_gradient) // Imagem de erro
                        .into(heroBackgroundImage);
            } else {
                 heroBackgroundImage.setImageResource(R.drawable.hero_section_background_gradient); // Default
            }
            // TODO: Formatar duração, ano, rating, gênero, descrição, elenco de forma mais robusta.
            // Exemplo: heroDuration.setText(String.format("%d min", featuredMovie.getEpisodeRunTime()));
            //          heroYear.setText(featuredMovie.getReleaseDate() != null ? featuredMovie.getReleaseDate().substring(0,4) : "----");
            //          heroRating.setText(String.format("%.1f", featuredMovie.getRating5based() * 2)); // Convert 5-based to 10-based
            //          heroDescription.setText(featuredMovie.getPlot());
        } else {
            Log.d(VOD_TV_TAG, "No movies available for Hero Section.");
            // Limpar campos da Hero Section ou mostrar placeholders
            heroTitle.setText("Nenhum conteúdo em destaque");
            // ... limpar outros campos
        }
    }

    private void populateCategories() {
        // Lógica similar ao VodFragment para agrupar filmes por categoria
        // e popular o recyclerViewCategoriesTv usando o novo CategoryAdapterTv
        List<Movie> allMovies = dataManager.getVodStreams();
        if (allMovies == null || allMovies.isEmpty()) {
            Log.w(VOD_TV_TAG, "No movies to display in categories.");
            if (getContext() != null) Toast.makeText(getContext(), "Nenhum filme/série encontrado.", Toast.LENGTH_SHORT).show();
            // categoryAdapterTv.updateData(new LinkedHashMap<>()); // Limpar adapter
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
                    if (getContext() != null) {
                       return categoryIdToNameMap.getOrDefault(categoryId, getContext().getString(R.string.label_other_category));
                    }
                    return categoryIdToNameMap.getOrDefault(categoryId, "Outros");
                }, LinkedHashMap::new, Collectors.toList()));

        Log.d(VOD_TV_TAG, "Updating TV adapter with " + moviesByCategory.size() + " categories.");
        // categoryAdapterTv.updateData(moviesByCategory);
        // TODO: Precisaremos de um CategoryAdapterTv e MovieAdapterTv
        // No momento, esta parte não vai funcionar sem os adaptadores corretos.
        if (getContext() != null) {
            Toast.makeText(getContext(), "TODO: Implementar CategoryAdapterTv e MovieAdapterTv", Toast.LENGTH_LONG).show();
        }
    }

    // private void openMovieDetailsTv(Movie movie) {
        // Intent intent = new Intent(getActivity(), MovieDetailsActivityTv.class); // Precisará de uma MovieDetailsActivityTv
        // intent.putExtra("movie", movie);
        // startActivity(intent);
    // }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(VOD_TV_TAG, "onDetach called");
        if (dataManager != null) {
            dataManager.removeListener(this); // Remover listener específico
        }
    }
}
