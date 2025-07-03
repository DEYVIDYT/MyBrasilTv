package com.example.iptvplayer.adapter;

import android.util.Log;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptvplayer.R;
import com.example.iptvplayer.XtreamApiService;

import java.util.List;

public class LiveCategoryAdapter extends RecyclerView.Adapter<LiveCategoryAdapter.CategoryViewHolder> {

    private static final String LIVE_CAT_TAG = "LiveCategoryAdapter_DEBUG";
    private List<XtreamApiService.CategoryInfo> categoryList;
    private final OnCategoryClickListener listener;
    private int selectedPosition = 0;

    public interface OnCategoryClickListener {
        void onCategoryClick(String categoryId);
    }

    public LiveCategoryAdapter(Context context, List<XtreamApiService.CategoryInfo> categoryList, OnCategoryClickListener listener) {
        Log.d(LIVE_CAT_TAG, "Constructor called with " + categoryList.size() + " categories.");
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Log.d(LIVE_CAT_TAG, "onCreateViewHolder called"); // Pode ser verboso
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_channel_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        XtreamApiService.CategoryInfo category = categoryList.get(position);
        // Log.d(LIVE_CAT_TAG, "onBindViewHolder for category: " + category.name); // Pode ser verboso
        holder.categoryName.setText(category.name);

        holder.itemView.setSelected(selectedPosition == position);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                Log.d(LIVE_CAT_TAG, "Category clicked: ID=" + category.id + ", Name=" + category.name);
                listener.onCategoryClick(category.id);
            }
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
        });
    }

    @Override
    public int getItemCount() {
        // Log.d(LIVE_CAT_TAG, "getItemCount: " + categoryList.size()); // Pode ser verboso
        return categoryList.size();
    }

    public void updateData(List<XtreamApiService.CategoryInfo> newCategoryList) {
        Log.d(LIVE_CAT_TAG, "updateData called with " + newCategoryList.size() + " categories.");
        this.categoryList.clear();
        this.categoryList.addAll(newCategoryList);
        selectedPosition = 0;
        notifyDataSetChanged();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.category_name_text_view);
        }
    }
}