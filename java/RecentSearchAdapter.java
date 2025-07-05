package com.example.iptvplayer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptvplayer.R;

import java.util.List;

public class RecentSearchAdapter extends RecyclerView.Adapter<RecentSearchAdapter.RecentSearchViewHolder> {

    private List<String> recentSearches;
    private OnSearchClickListener listener;

    public interface OnSearchClickListener {
        void onSearchClick(String query);
    }

    public RecentSearchAdapter(List<String> recentSearches, OnSearchClickListener listener) {
        this.recentSearches = recentSearches;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecentSearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_search, parent, false);
        return new RecentSearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentSearchViewHolder holder, int position) {
        String searchTerm = recentSearches.get(position);
        holder.searchText.setText(searchTerm);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSearchClick(searchTerm);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recentSearches.size();
    }

    static class RecentSearchViewHolder extends RecyclerView.ViewHolder {
        TextView searchText;

        RecentSearchViewHolder(@NonNull View itemView) {
            super(itemView);
            searchText = itemView.findViewById(R.id.recent_search_text);
        }
    }
}

