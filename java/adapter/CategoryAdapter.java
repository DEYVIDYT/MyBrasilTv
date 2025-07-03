package com.example.iptvplayer.adapter;

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

    private Context context;
    private List<Map.Entry<String, List<Movie>>> categoryEntries;

    public CategoryAdapter(Context context, Map<String, List<Movie>> moviesByCategory) {
        this.context = context;
        this.categoryEntries = new ArrayList<>(moviesByCategory.entrySet());
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_movie_category_row, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Map.Entry<String, List<Movie>> entry = categoryEntries.get(position);
        holder.categoryTitle.setText(entry.getKey());

        MovieAdapter movieAdapter = new MovieAdapter(entry.getValue());
        holder.moviesRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        holder.moviesRecyclerView.setAdapter(movieAdapter);
    }

    @Override
    public int getItemCount() {
        return categoryEntries.size();
    }

    public void updateData(Map<String, List<Movie>> newMoviesByCategory) {
        this.categoryEntries = new ArrayList<>(newMoviesByCategory.entrySet());
        notifyDataSetChanged();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryTitle;
        RecyclerView moviesRecyclerView;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTitle = itemView.findViewById(R.id.text_view_category_title);
            moviesRecyclerView = itemView.findViewById(R.id.recycler_view_movies_horizontal);
        }
    }
}


