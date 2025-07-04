package com.example.iptvplayer;

import android.content.Intent;
import android.util.Log; // Import adicionado
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
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
    private static final String TAG_SEARCH = "SearchActivity_DEBUG"; // Tag para logs

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
        searchResultsRecycler.setLayoutManager(new GridLayoutManager(this, 2)); // 2 colunas na grade
        searchResultsRecycler.setAdapter(searchResultsAdapter);

        loadAllMovies(); // Carregar filmes aqui para garantir que allMovies esteja populado antes de qualquer pesquisa
    }

    private void setupSearchFunctionality() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                Log.d(TAG_SEARCH, "onTextChanged: query='" + query + "', allMovies.isEmpty()=" + (allMovies == null || allMovies.isEmpty()));

                if (allMovies == null || allMovies.isEmpty()) {
                    searchResults.clear();
                    if (searchResultsAdapter != null) {
                        searchResultsAdapter.notifyDataSetChanged();
                    }
                    // A mensagem noSearchDataText já deve estar visível por loadAllMovies().
                    // Garantir que as listas estejam escondidas se não houver dados.
                    if (searchResultsRecycler != null) searchResultsRecycler.setVisibility(View.GONE);
                    if (recentSearchesRecycler != null) recentSearchesRecycler.setVisibility(View.GONE);
                    if (noHistoryText != null) noHistoryText.setVisibility(View.GONE);
                    // Se noSearchDataText está visível, ele tem prioridade sobre noHistoryText
                    if (noSearchDataText != null && noSearchDataText.getVisibility() == View.VISIBLE && noHistoryText != null) {
                        noHistoryText.setVisibility(View.GONE);
                    } else if (noHistoryText != null && recentSearchesRecycler != null && recentSearchesRecycler.getVisibility() == View.VISIBLE) {
                         noHistoryText.setVisibility(recentSearches.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    return;
                }

                // Se allMovies não está vazio, processar a query
                performSearch(query); // performSearch agora decide se mostra resultados ou recentes
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            String query = searchEditText.getText().toString().trim();
            if (allMovies != null && !allMovies.isEmpty() && !query.isEmpty()) { // Só adiciona e pesquisa se houver dados e query
                addToRecentSearches(query);
                // performSearch(query); // performSearch já é chamado pelo TextWatcher
            }
            // Sempre retornar true pode impedir o teclado de fechar, dependendo do actionId.
            // Considerar verificar actionId == EditorInfo.IME_ACTION_SEARCH
            return true;
        });
    }

    private void performSearch(String query) {
        Log.d(TAG_SEARCH, "performSearch called with query: '" + query + "'");
        Log.d(TAG_SEARCH, "allMovies size: " + (allMovies != null ? allMovies.size() : "null"));

        searchResults.clear();
        
        if (allMovies != null && !allMovies.isEmpty() && query != null && !query.isEmpty()) {
            for (Movie movie : allMovies) {
                if (movie != null && movie.getName() != null && movie.getName().toLowerCase().contains(query.toLowerCase())) {
                    searchResults.add(movie);
                }
            }
        }
        
        if (searchResultsAdapter != null) {
            searchResultsAdapter.updateData(searchResults);
        }

        if (query != null && !query.isEmpty()) {
            showSearchResults();
        } else {
            showRecentSearches();
        }
    }

    private void showRecentSearches() {
        Log.d(TAG_SEARCH, "showRecentSearches called.");
        findViewById(R.id.recent_searches_title).setVisibility(View.VISIBLE);
        recentSearchesRecycler.setVisibility(View.VISIBLE);
        searchResultsRecycler.setVisibility(View.GONE);
        noHistoryText.setVisibility(recentSearches.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showSearchResults() {
        Log.d(TAG_SEARCH, "showSearchResults called.");
        findViewById(R.id.recent_searches_title).setVisibility(View.GONE);
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
        DataManager dataManager = MyApplication.getDataManager(this);
        if (dataManager != null && dataManager.isDataFullyLoaded()) {
            allMovies = dataManager.getVodStreams();
        } else {
            allMovies = new ArrayList<>(); // Inicializa como lista vazia se os dados não estiverem prontos
        }

        if (allMovies == null || allMovies.isEmpty()) {
            allMovies = new ArrayList<>(); // Garante que não seja nulo
            if (noSearchDataText != null) {
                noSearchDataText.setVisibility(View.VISIBLE);
                noSearchDataText.setText(R.string.no_movie_data_loaded_message);
            }
            if (searchEditText != null) {
                searchEditText.setHint(R.string.search_no_data_to_search);
            }
            if (recentSearchesRecycler != null) recentSearchesRecycler.setVisibility(View.GONE);
            if (searchResultsRecycler != null) searchResultsRecycler.setVisibility(View.GONE);
            if (noHistoryText != null) noHistoryText.setVisibility(View.GONE);
        } else {
            if (noSearchDataText != null) {
                noSearchDataText.setVisibility(View.GONE);
            }
            if (searchEditText != null) {
                searchEditText.setHint(getString(R.string.search_hint_vod));
            }
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

