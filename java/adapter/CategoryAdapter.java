package com.example.iptvplayer.adapter;

import android.util.Log;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptvplayer.R;
import com.example.iptvplayer.data.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private static final String CAT_ADAPTER_TAG = "CategoryAdapter_DEBUG";
    private Context context;
    private List<Map.Entry<String, List<Movie>>> categoryEntries;
    private MovieAdapter.OnMovieClickListener onMovieClickListener;

    public CategoryAdapter(Context context, Map<String, List<Movie>> moviesByCategory) {
        Log.d(CAT_ADAPTER_TAG, "Constructor called with " + moviesByCategory.size() + " categories.");
        this.context = context;
        this.categoryEntries = new ArrayList<>(moviesByCategory.entrySet());
    }

    public CategoryAdapter(Context context, Map<String, List<Movie>> moviesByCategory, MovieAdapter.OnMovieClickListener listener) {
        Log.d(CAT_ADAPTER_TAG, "Constructor called with " + moviesByCategory.size() + " categories.");
        this.context = context;
        this.categoryEntries = new ArrayList<>(moviesByCategory.entrySet());
        this.onMovieClickListener = listener;
    }

    public void setOnMovieClickListener(MovieAdapter.OnMovieClickListener listener) {
        this.onMovieClickListener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(CAT_ADAPTER_TAG, "onCreateViewHolder called");
        View view = LayoutInflater.from(context).inflate(R.layout.item_movie_category_row, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Map.Entry<String, List<Movie>> entry = categoryEntries.get(position);
        Log.d(CAT_ADAPTER_TAG, "onBindViewHolder for category: '" + entry.getKey() + "' with " + entry.getValue().size() + " movies.");
        holder.categoryTitle.setText(entry.getKey());

        MovieAdapter movieAdapter = new MovieAdapter(entry.getValue(), onMovieClickListener);
        holder.moviesRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        holder.moviesRecyclerView.setAdapter(movieAdapter);
    }

    @Override
    public int getItemCount() {
        // Log.d(CAT_ADAPTER_TAG, "getItemCount called, size: " + categoryEntries.size()); // Pode ser muito verboso
        return categoryEntries.size();
    }

    public void updateData(Map<String, List<Movie>> newMoviesByCategory) {
        Log.d(CAT_ADAPTER_TAG, "updateData called with " + newMoviesByCategory.size() + " new categories.");
        this.categoryEntries = new ArrayList<>(newMoviesByCategory.entrySet());
        notifyDataSetChanged();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        // Log.d(CAT_ADAPTER_TAG, "CategoryViewHolder created"); // Não é ideal logar na criação de cada ViewHolder aqui
        TextView categoryTitle;
        RecyclerView moviesRecyclerView;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTitle = itemView.findViewById(R.id.text_view_category_title);
            moviesRecyclerView = itemView.findViewById(R.id.recycler_view_movies_horizontal);
        }
    }
}


