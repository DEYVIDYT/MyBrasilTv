package com.example.iptvplayer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.iptvplayer.data.Movie;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MovieDetailsActivity extends AppCompatActivity {

    private ImageView moviePoster;
    private TextView movieTitle;
    private TextView movieYearGenre;
    private TextView movieRating;
    private TextView movieDuration;
    private TextView movieDescription;
    private Button watchButton;
    private ImageButton favoriteButton;
    private ImageButton backButton;
    private TabLayout contentTabs;
    private ViewPager2 contentViewPager;
    private android.widget.ProgressBar detailsProgressBar;

    // Novos TextViews para detalhes adicionais
    private TextView movieGenreDetail;
    private TextView movieDirectorDetail;
    private TextView movieCastDetail;
    private View additionalDetailsLayout; // LinearLayout para controlar visibilidade do grupo
    
    private Movie movie;
    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);

        // Receber o filme do Intent
        movie = (Movie) getIntent().getSerializableExtra("movie");
        if (movie == null) {
            finish();
            return;
        }

        dataManager = MyApplication.getDataManager(this); // Inicializa DataManager

        initViews();

        // Mostrar ProgressBar e buscar detalhes
        if (movie.getStreamId() != null && !movie.getStreamId().isEmpty()) {
            detailsProgressBar.setVisibility(View.VISIBLE);
            fetchMovieDetails(movie.getStreamId());
        } else {
            // Se não há streamId, apenas configurar com os dados existentes
            // e esconder o ProgressBar (caso estivesse visível por algum motivo)
            detailsProgressBar.setVisibility(View.GONE);
            setupViews(); // Configura a UI com os dados básicos disponíveis
        }

        setupListeners();
    }

    private void initViews() {
        moviePoster = findViewById(R.id.movie_poster);
        movieTitle = findViewById(R.id.movie_title);
        movieYearGenre = findViewById(R.id.movie_year_genre);
        movieRating = findViewById(R.id.movie_rating);
        movieDuration = findViewById(R.id.movie_duration);
        movieDescription = findViewById(R.id.movie_description);
        watchButton = findViewById(R.id.watch_button);
        favoriteButton = findViewById(R.id.favorite_button);
        backButton = findViewById(R.id.back_button);
        contentTabs = findViewById(R.id.content_tabs);
        contentViewPager = findViewById(R.id.content_viewpager);
        detailsProgressBar = findViewById(R.id.details_progress_bar);

        // Referenciar novos TextViews e Layout
        movieGenreDetail = findViewById(R.id.movie_genre_detail);
        movieDirectorDetail = findViewById(R.id.movie_director_detail);
        movieCastDetail = findViewById(R.id.movie_cast_detail);
        additionalDetailsLayout = findViewById(R.id.additional_details_layout);
    }

    private void fetchMovieDetails(String streamId) {
        dataManager.fetchMovieDetails(streamId, new DataManager.MovieDetailsCallback() {
            @Override
            public void onDetailsFetched(Movie movieWithDetails) {
                detailsProgressBar.setVisibility(View.GONE);
                MovieDetailsActivity.this.movie = movieWithDetails; // Atualiza o filme da atividade
                setupViews(); // Reconfigura as views com os dados completos
            }

            @Override
            public void onError(String errorMessage) {
                detailsProgressBar.setVisibility(View.GONE);
                android.widget.Toast.makeText(MovieDetailsActivity.this, "Erro ao buscar detalhes: " + errorMessage, android.widget.Toast.LENGTH_LONG).show();
                setupViews(); // Configura com os dados básicos que já tinha
            }
        });
    }

    private void setupViews() {
        // Configurar informações do filme
        movieTitle.setText(movie.getName());
        
        // Carregar poster usando Glide
        if (movie.getStreamIcon() != null && !movie.getStreamIcon().isEmpty()) {
            Glide.with(this)
                    .load(movie.getStreamIcon())
                    .placeholder(R.drawable.rounded_corner_image_placeholder)
                    .error(R.drawable.rounded_corner_image_placeholder)
                    .into(moviePoster);
        }

        // Configurar ano e gênero (se disponível)
        StringBuilder yearGenreText = new StringBuilder();
        if (movie.getReleaseDate() != null && !movie.getReleaseDate().isEmpty()) {
            yearGenreText.append(movie.getReleaseDate());
        }
        if (movie.getCategoryName() != null && !movie.getCategoryName().isEmpty()) {
            if (yearGenreText.length() > 0) {
                yearGenreText.append(" • ");
            }
            yearGenreText.append(movie.getCategoryName());
        }
        movieYearGenre.setText(yearGenreText.toString());

        // Configurar avaliação (se disponível)
        if (movie.getRating() != null && !movie.getRating().isEmpty()) {
            movieRating.setText(movie.getRating() + "%");
        } else {
            movieRating.setText("N/A");
        }

        // Configurar duração (se disponível)
        if (movie.getDuration() != null && !movie.getDuration().isEmpty()) {
            movieDuration.setText(movie.getDuration());
        } else {
            movieDuration.setVisibility(View.GONE);
        }

        // Configurar descrição
        if (movie.getPlot() != null && !movie.getPlot().isEmpty()) {
            movieDescription.setText(movie.getPlot());
        } else {
            movieDescription.setText("Descrição não disponível.");
        }

        // Configurar detalhes adicionais (Gênero, Diretor, Elenco)
        boolean hasAdditionalDetails = false;
        if (movie.getGenre() != null && !movie.getGenre().isEmpty()) {
            movieGenreDetail.setText(movie.getGenre());
            movieGenreDetail.getParent().requestLayout(); // Força o pai a se ajustar
            ((View) movieGenreDetail.getParent()).setVisibility(View.VISIBLE);
            hasAdditionalDetails = true;
        } else {
            ((View) movieGenreDetail.getParent()).setVisibility(View.GONE);
        }

        if (movie.getDirector() != null && !movie.getDirector().isEmpty()) {
            movieDirectorDetail.setText(movie.getDirector());
            ((View) movieDirectorDetail.getParent()).setVisibility(View.VISIBLE);
            hasAdditionalDetails = true;
        } else {
            ((View) movieDirectorDetail.getParent()).setVisibility(View.GONE);
        }

        if (movie.getCast() != null && !movie.getCast().isEmpty()) {
            movieCastDetail.setText(movie.getCast());
            ((View) movieCastDetail.getParent()).setVisibility(View.VISIBLE);
            hasAdditionalDetails = true;
        } else {
            ((View) movieCastDetail.getParent()).setVisibility(View.GONE);
        }

        additionalDetailsLayout.setVisibility(hasAdditionalDetails ? View.VISIBLE : View.GONE);


        // Configurar o adaptador para o ViewPager2 ANTES de configurar as tabs
        ContentViewPagerAdapter adapter = new ContentViewPagerAdapter(this);
        contentViewPager.setAdapter(adapter);

        // Configurar tabs
        setupTabs();
    }

    private void setupTabs() {
        new TabLayoutMediator(contentTabs, contentViewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("AMOSTRA DE VÍDEO");
                            break;
                        case 1:
                            tab.setText("ELENCO");
                            break;
                        case 2:
                            tab.setText("MÉDIA");
                            break;
                    }
                }
        ).attach();
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        watchButton.setOnClickListener(v -> {
            // Implementar reprodução do filme
            // Por enquanto, apenas mostrar um toast ou abrir o player
            playMovie();
        });

        favoriteButton.setOnClickListener(v -> {
            // Implementar funcionalidade de favoritos
            toggleFavorite();
        });
    }

    private void playMovie() {
        // Implementar a lógica para reproduzir o filme
        // Isso pode envolver abrir uma nova Activity com o player de vídeo
        // ou usar o player existente no projeto
        
        // Por enquanto, apenas um exemplo básico
        Intent intent = new Intent(this, com.example.iptvplayer.VideoPlayerActivity.class);
        intent.putExtra("movie", movie);
        startActivity(intent);
    }

    private void toggleFavorite() {
        // Implementar lógica de favoritos
        // Por enquanto, apenas alterar o ícone
        // Aqui você pode salvar/remover dos favoritos e atualizar o ícone
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    // Adaptador para o ViewPager2
    private static class ContentViewPagerAdapter extends FragmentStateAdapter {

        public ContentViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // Retornar um Fragment placeholder para cada tab
            // Em um aplicativo real, você retornaria Fragments com conteúdo relevante
            return new PlaceholderFragment(); 
        }

        @Override
        public int getItemCount() {
            return 3; // Temos 3 tabs: Amostra de Vídeo, Elenco, Mídia
        }
    }
}






