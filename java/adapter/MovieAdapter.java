package com.example.iptvplayer.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.iptvplayer.R;
import com.example.iptvplayer.data.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    private List<Movie> movieListDisplayed;
    private final List<Movie> movieListAll;
    private Context context; // Adicionar contexto para Glide

    public MovieAdapter(List<Movie> movieList) {
        this.movieListAll = new ArrayList<>(movieList);
        this.movieListDisplayed = new ArrayList<>(movieList);
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext(); // Obter contexto aqui
        View view = LayoutInflater.from(context).inflate(R.layout.item_movie, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movieListDisplayed.get(position);
        holder.movieTitle.setText(movie.getTitle());

        if (movie.getPosterUrl() != null && !movie.getPosterUrl().isEmpty()) {
            Glide.with(context)
                    .load(movie.getPosterUrl())
                    .placeholder(android.R.color.darker_gray) // Placeholder enquanto carrega
                    .error(android.R.drawable.ic_menu_report_image) // Imagem de erro se falhar
                    .into(holder.moviePoster);
        } else {
            Log.w("MovieAdapter", "Poster URL is null or empty for movie: " + movie.getTitle());
            holder.moviePoster.setImageResource(android.R.color.darker_gray); // Placeholder padrão
        }
    }

    @Override
    public int getItemCount() {
        return movieListDisplayed.size();
    }

    public void filterByCategory(String category) {
        if (category == null || category.equalsIgnoreCase("All")) {
            movieListDisplayed = new ArrayList<>(movieListAll);
        } else {
            movieListDisplayed = movieListAll.stream()
                    .filter(movie -> category.equalsIgnoreCase(movie.getCategory()))
                    .collect(Collectors.toList());
        }
        notifyDataSetChanged();
    }

    public static class MovieViewHolder extends RecyclerView.ViewHolder {
        ImageView moviePoster;
        TextView movieTitle;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            moviePoster = itemView.findViewById(R.id.movie_poster);
            movieTitle = itemView.findViewById(R.id.movie_title);
        }
    }

    public void updateData(List<Movie> newMovieList) {
        this.movieListAll.clear();
        this.movieListAll.addAll(newMovieList);
        filterByCategory("All"); // Reset filter to show all movies after update
    }
}

