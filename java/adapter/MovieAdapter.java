package com.example.iptvplayer.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import java.io.IOException;
import java.net.URL;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptvplayer.R;
import com.example.iptvplayer.data.Movie;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    private List<Movie> movieListDisplayed;
    private final List<Movie> movieListAll;

    public MovieAdapter(List<Movie> movieList) {
        this.movieListAll = new ArrayList<>(movieList);
        this.movieListDisplayed = new ArrayList<>(movieList);
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movieListDisplayed.get(position);
        holder.movieTitle.setText(movie.getTitle());
        // Store the URL in the tag for logging purposes in onPostExecute
        holder.moviePoster.setTag(movie.getPosterUrl());
        if (movie.getPosterUrl() != null && !movie.getPosterUrl().isEmpty()) {
            new DownloadImageTask(holder.moviePoster).execute(movie.getPosterUrl());
        } else {
            Log.w("MovieAdapter", "Poster URL is null or empty for movie: " + movie.getTitle());
            holder.moviePoster.setImageResource(android.R.color.darker_gray); // Default placeholder
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

    private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                Log.d("DownloadImageTask", "Attempting to download image from: " + urldisplay);
                java.io.InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
                if (mIcon11 == null) {
                    Log.e("DownloadImageTask", "Failed to decode bitmap from stream: " + urldisplay);
                }
            } catch (IOException e) {
                Log.e("DownloadImageTask", "IOException while downloading image: " + urldisplay, e);
            } catch (Exception e) {
                Log.e("DownloadImageTask", "General exception while downloading image: " + urldisplay, e);
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                bmImage.setImageBitmap(result);
                Log.d("DownloadImageTask", "Image successfully loaded and set for: " + bmImage.getTag());
            } else {
                Log.e("DownloadImageTask", "Bitmap result is null after download attempt for: " + bmImage.getTag());
                // Optionally set a placeholder if download fails
                // bmImage.setImageResource(R.drawable.placeholder_image_error);
            }
        }
    }

    public void updateData(List<Movie> newMovieList) {
        this.movieListAll.clear();
        this.movieListAll.addAll(newMovieList);
        filterByCategory("All"); // Reset filter to show all movies after update
    }
}