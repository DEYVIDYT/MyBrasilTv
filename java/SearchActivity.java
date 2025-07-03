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
        
        // Só filtra se allMovies não for nulo/vazio E a query não for nula/vazia
        if (allMovies != null && !allMovies.isEmpty() && query != null && !query.isEmpty()) {
            int moviesLogged = 0;
            for (Movie movie : allMovies) {
                if (movie != null && movie.getName() != null) {
                    if (moviesLogged < 5) {
                        Log.d(TAG_SEARCH, "Checking movie: " + movie.getName());
                        moviesLogged++;
                    }
                    if (movie.getName().toLowerCase().contains(query.toLowerCase())) {
                        searchResults.add(movie);
                        Log.d(TAG_SEARCH, "Added to searchResults: " + movie.getName());
                    }
                }
            }
        }
        // Se a query for vazia, searchResults permanecerá vazia (ou será esvaziada no início).
        Log.d(TAG_SEARCH, "searchResults size after filter: " + searchResults.size());
        
        if (searchResultsAdapter != null) {
            searchResultsAdapter.notifyDataSetChanged();
            Log.d(TAG_SEARCH, "searchResultsAdapter notified. Adapter item count: " + searchResultsAdapter.getItemCount());
        } else {
            Log.w(TAG_SEARCH, "searchResultsAdapter is null, cannot notify or get item count.");
        }

        // Decidir qual view mostrar com base na query
        if (query != null && !query.isEmpty()) {
            showSearchResults();
        } else {
            showRecentSearches();
        }
    }

    private void showRecentSearches() {
        Log.d(TAG_SEARCH, "showRecentSearches called.");
        // Não mostrar recentes se a mensagem "sem dados para pesquisar" estiver ativa
        if (noSearchDataText != null && noSearchDataText.getVisibility() == View.VISIBLE) {
            if (recentSearchesRecycler != null) recentSearchesRecycler.setVisibility(View.GONE);
            if (searchResultsRecycler != null) searchResultsRecycler.setVisibility(View.GONE);
            if (noHistoryText != null) noHistoryText.setVisibility(View.GONE);
            Log.d(TAG_SEARCH, "showRecentSearches: Hiding all due to noSearchDataText visible.");
            return;
        }

        Log.d(TAG_SEARCH, "showRecentSearches: Setting recentSearchesRecycler to VISIBLE, searchResultsRecycler to GONE.");
        if (recentSearchesRecycler != null) recentSearchesRecycler.setVisibility(View.VISIBLE);
        if (searchResultsRecycler != null) searchResultsRecycler.setVisibility(View.GONE);
        if (noHistoryText != null) noHistoryText.setVisibility(recentSearches.isEmpty() ? View.VISIBLE : View.GONE);
        // if (noSearchDataText != null) noSearchDataText.setVisibility(View.GONE); // Já tratado acima
    }

    private void showSearchResults() {
        Log.d(TAG_SEARCH, "showSearchResults called.");
        // Não mostrar resultados se a mensagem "sem dados para pesquisar" estiver ativa
         if (noSearchDataText != null && noSearchDataText.getVisibility() == View.VISIBLE) {
            if (recentSearchesRecycler != null) recentSearchesRecycler.setVisibility(View.GONE);
            if (searchResultsRecycler != null) searchResultsRecycler.setVisibility(View.GONE);
            if (noHistoryText != null) noHistoryText.setVisibility(View.GONE);
            Log.d(TAG_SEARCH, "showSearchResults: Hiding all due to noSearchDataText visible.");
            return;
        }

        Log.d(TAG_SEARCH, "showSearchResults: Setting searchResultsRecycler to VISIBLE, recentSearchesRecycler to GONE.");
        if (recentSearchesRecycler != null) recentSearchesRecycler.setVisibility(View.GONE);
        if (searchResultsRecycler != null) searchResultsRecycler.setVisibility(View.VISIBLE);
        if (noHistoryText != null) noHistoryText.setVisibility(View.GONE);
        // if (noSearchDataText != null) noSearchDataText.setVisibility(View.GONE); // Já tratado acima
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

