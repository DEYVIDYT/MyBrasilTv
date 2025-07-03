package com.example.iptvplayer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptvplayer.adapter.MovieAdapter;
import com.example.iptvplayer.adapter.RecentSearchAdapter;
import com.example.iptvplayer.data.Movie;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private EditText searchEditText;
    private RecyclerView recentSearchesRecycler;
    private RecyclerView searchResultsRecycler;
    private TextView noHistoryText;
    private RecentSearchAdapter recentSearchAdapter;
    private MovieAdapter searchResultsAdapter;
    
    private List<String> recentSearches = new ArrayList<>();
    private List<Movie> searchResults = new ArrayList<>();
    private List<Movie> allMovies = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initViews();
        setupRecyclerViews();
        setupSearchFunctionality();
        loadRecentSearches();
        loadAllMovies();
    }

    private void initViews() {
        searchEditText = findViewById(R.id.search_edit_text);
        recentSearchesRecycler = findViewById(R.id.recent_searches_recycler);
        searchResultsRecycler = findViewById(R.id.search_results_recycler);
        noHistoryText = findViewById(R.id.no_history_text);
    }

    private void setupRecyclerViews() {
        // RecyclerView para pesquisas recentes
        recentSearchAdapter = new RecentSearchAdapter(recentSearches, this::performSearch);
        recentSearchesRecycler.setLayoutManager(new LinearLayoutManager(this));
        recentSearchesRecycler.setAdapter(recentSearchAdapter);

        // RecyclerView para resultados de pesquisa
        searchResultsAdapter = new MovieAdapter(searchResults, this::openMovieDetails);
        searchResultsRecycler.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecycler.setAdapter(searchResultsAdapter);
    }

    private void setupSearchFunctionality() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    showRecentSearches();
                } else {
                    performSearch(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            String query = searchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                addToRecentSearches(query);
                performSearch(query);
            }
            return true;
        });
    }

    private void performSearch(String query) {
        searchResults.clear();
        
        // Filtrar filmes baseado na query
        for (Movie movie : allMovies) {
            if (movie.getName().toLowerCase().contains(query.toLowerCase())) {
                searchResults.add(movie);
            }
        }
        
        showSearchResults();
        searchResultsAdapter.notifyDataSetChanged();
    }

    private void showRecentSearches() {
        recentSearchesRecycler.setVisibility(View.VISIBLE);
        searchResultsRecycler.setVisibility(View.GONE);
        noHistoryText.setVisibility(recentSearches.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showSearchResults() {
        recentSearchesRecycler.setVisibility(View.GONE);
        searchResultsRecycler.setVisibility(View.VISIBLE);
        noHistoryText.setVisibility(View.GONE);
    }

    private void addToRecentSearches(String query) {
        if (!recentSearches.contains(query)) {
            recentSearches.add(0, query);
            if (recentSearches.size() > 10) {
                recentSearches.remove(recentSearches.size() - 1);
            }
            recentSearchAdapter.notifyDataSetChanged();
            saveRecentSearches();
        }
    }

    private void loadRecentSearches() {
        // Implementar carregamento das pesquisas recentes do SharedPreferences
        // Por enquanto, deixar vazio
    }

    private void saveRecentSearches() {
        // Implementar salvamento das pesquisas recentes no SharedPreferences
        // Por enquanto, deixar vazio
    }

    private void loadAllMovies() {
        // Carregar todos os filmes do cache
        allMovies = MovieCacheManager.loadMoviesFromCache(this);
    }

    private void openMovieDetails(Movie movie) {
        Intent intent = new Intent(this, MovieDetailsActivity.class);
        intent.putExtra("movie", movie);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}

