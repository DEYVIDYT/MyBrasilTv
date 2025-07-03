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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.RequestOptions;
import com.example.iptvplayer.R;
import com.example.iptvplayer.data.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    private static final String MOVIE_ADAPTER_TAG = "MovieAdapter_DEBUG";
    private List<Movie> movieListDisplayed;
    private final List<Movie> movieListAll;
    private Context context;

    public MovieAdapter(List<Movie> movieList) {
        this.movieListAll = new ArrayList<>(movieList);
        this.movieListDisplayed = new ArrayList<>(movieList);
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_movie, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movieListDisplayed.get(position);
        holder.movieTitle.setText(movie.getTitle());


        String posterUrl = movie.getPosterUrl();
        Log.d(MOVIE_ADAPTER_TAG, "Movie: " + movie.getTitle() + ", Attempting to load poster URL: " + posterUrl);

        if (posterUrl != null && !posterUrl.isEmpty()) {
            // Adicionando opções de requisição para Glide
            RequestOptions requestOptions = new RequestOptions()
                    .placeholder(R.drawable.rounded_corner_image_placeholder) // Placeholder personalizado
                    .error(R.drawable.dkplayer_ic_action_error) // Imagem de erro personalizada
                    .diskCacheStrategy(DiskCacheStrategy.ALL); // Estratégia de cache para todas as imagens
            Glide.with(context)
                    .load(posterUrl)
                    .apply(requestOptions)
                    .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@androidx.annotation.Nullable GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            Log.e(MOVIE_ADAPTER_TAG, "Glide onLoadFailed for VOD: " + movie.getTitle() + ", URL: " + model, e);
                            return false; // Importante retornar false para que o error() drawable seja exibido.
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            Log.d(MOVIE_ADAPTER_TAG, "Glide onResourceReady for VOD: " + movie.getTitle() + ", URL: " + model);
                            return false;
                        }
                    })
                    .into(holder.moviePoster);
        } else {
            Log.w(MOVIE_ADAPTER_TAG, "Poster URL is null or empty for VOD movie: " + movie.getTitle() + ". Setting placeholder.");
            holder.moviePoster.setImageResource(R.drawable.rounded_corner_image_placeholder); // Placeholder padrão
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
        filterByCategory("All");
    }
}


