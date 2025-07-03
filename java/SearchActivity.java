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
    private TextView noSearchDataText; // Adicionado
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
        noSearchDataText = findViewById(R.id.no_search_data_text); // Adicionado
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
                if (allMovies.isEmpty()) {
                    searchResults.clear();
                    if (searchResultsAdapter != null) { // Adicionada verificação de nulidade
                        searchResultsAdapter.notifyDataSetChanged();
                    }
                    if (searchResultsRecycler != null) { // Adicionada verificação de nulidade
                        searchResultsRecycler.setVisibility(View.GONE);
                    }
                    // Mostrar recentes apenas se a query for vazia, mesmo que allMovies esteja vazio
                    if (query.isEmpty() && recentSearchesRecycler != null && noHistoryText != null) {
                         recentSearchesRecycler.setVisibility(View.VISIBLE);
                         noHistoryText.setVisibility(recentSearches.isEmpty() ? View.VISIBLE : View.GONE);
                    } else if (recentSearchesRecycler != null) {
                         recentSearchesRecycler.setVisibility(View.GONE); // Esconde recentes se houver query
                    }
                    return;
                }

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
        if (searchResultsAdapter != null) { // Adicionada verificação de nulidade
            searchResultsAdapter.notifyDataSetChanged();
        }
    }

    private void showRecentSearches() {
        if (allMovies != null && allMovies.isEmpty() && noSearchDataText != null && noSearchDataText.getVisibility() == View.VISIBLE) {
            // Se a mensagem "no search data" está visível, não mostre "recent searches" ou "no history"
            if (recentSearchesRecycler != null) recentSearchesRecycler.setVisibility(View.GONE);
            if (searchResultsRecycler != null) searchResultsRecycler.setVisibility(View.GONE);
            if (noHistoryText != null) noHistoryText.setVisibility(View.GONE);
            return;
        }

        if (recentSearchesRecycler != null) recentSearchesRecycler.setVisibility(View.VISIBLE);
        if (searchResultsRecycler != null) searchResultsRecycler.setVisibility(View.GONE);
        if (noHistoryText != null) noHistoryText.setVisibility(recentSearches.isEmpty() ? View.VISIBLE : View.GONE);
        if (noSearchDataText != null) noSearchDataText.setVisibility(View.GONE);
    }

    private void showSearchResults() {
         if (allMovies != null && allMovies.isEmpty() && noSearchDataText != null && noSearchDataText.getVisibility() == View.VISIBLE) {
            // Se a mensagem "no search data" está visível, não mostre os resultados da pesquisa (que estarão vazios)
            if (recentSearchesRecycler != null) recentSearchesRecycler.setVisibility(View.GONE);
            if (searchResultsRecycler != null) searchResultsRecycler.setVisibility(View.GONE);
            if (noHistoryText != null) noHistoryText.setVisibility(View.GONE);
            return;
        }

        if (recentSearchesRecycler != null) recentSearchesRecycler.setVisibility(View.GONE);
        if (searchResultsRecycler != null) searchResultsRecycler.setVisibility(View.VISIBLE);
        if (noHistoryText != null) noHistoryText.setVisibility(View.GONE);
        if (noSearchDataText != null) noSearchDataText.setVisibility(View.GONE);
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
        allMovies = MovieCacheManager.loadMoviesFromCache(this);
        if (allMovies == null || allMovies.isEmpty()) {
            allMovies = new ArrayList<>(); // Evita NullPointerException
            if (noSearchDataText != null) {
                noSearchDataText.setVisibility(View.VISIBLE);
            }
            if (searchEditText != null) {
                 searchEditText.setHint(R.string.search_no_data_to_search); // Altera o hint
            }
            // Esconder outras views que não fazem sentido sem dados
            if (recentSearchesRecycler != null) recentSearchesRecycler.setVisibility(View.GONE);
            if (searchResultsRecycler != null) searchResultsRecycler.setVisibility(View.GONE);
            if (noHistoryText != null) noHistoryText.setVisibility(View.GONE);
            // Opcional: findViewById(R.id.recent_searches_title).setVisibility(View.GONE);
        } else {
            if (noSearchDataText != null) {
                noSearchDataText.setVisibility(View.GONE);
            }
            if (searchEditText != null) {
                searchEditText.setHint(getString(R.string.search_hint_vod)); // Restaura hint original
            }
            // Mostrar recentes se a query estiver vazia e houver dados
            if (searchEditText != null && searchEditText.getText().toString().isEmpty()) {
                 showRecentSearches();
            }
        }
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

