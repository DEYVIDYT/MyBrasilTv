package com.example.iptvplayer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.iptvplayer.R;
import java.util.List;
import java.util.Map;

public class LiveCategoryAdapter extends RecyclerView.Adapter<LiveCategoryAdapter.CategoryViewHolder> {

    private List<Map.Entry<String, String>> categoryList; // List of Map Entries (ID, Name)
    private final OnCategoryClickListener listener;
    private int selectedPosition = 0; // Default to "All" or first category

    public interface OnCategoryClickListener {
        void onCategoryClick(String categoryId);
    }

    public LiveCategoryAdapter(List<Map.Entry<String, String>> categoryList, OnCategoryClickListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_channel_category, parent, false); // Assuming a simple layout
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Map.Entry<String, String> categoryEntry = categoryList.get(position);
        holder.categoryName.setText(categoryEntry.getValue()); // Display category name

        holder.itemView.setSelected(selectedPosition == position);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(categoryEntry.getKey()); // Pass category ID on click
            }
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public void updateData(List<Map.Entry<String, String>> newCategoryList) {
        this.categoryList.clear();
        this.categoryList.addAll(newCategoryList);
        // Reset selection or maintain it if possible, for now, reset to first
        selectedPosition = 0;
        notifyDataSetChanged();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ensure your item_channel_category.xml has a TextView with this ID
            categoryName = itemView.findViewById(R.id.category_name_text_view);

            if (categoryName == null) {
                // Fallback or error if the ID is not found, to prevent crashes.
                // This might happen if R.id.category_name_text_view doesn't exist in the layout.
                // For a generic TextView, you might need a more robust way to find it or define a specific ID.
                // As a simple fallback, if it's the only TextView, this *might* work, but it's risky.
                if (itemView instanceof ViewGroup) {
                    ViewGroup vg = (ViewGroup) itemView;
                    for (int i = 0; i < vg.getChildCount(); i++) {
                        View child = vg.getChildAt(i);
                        if (child instanceof TextView) {
                            categoryName = (TextView) child;
                            break;
                        }
                    }
                }
                 if (categoryName == null && itemView instanceof TextView) {
                    categoryName = (TextView) itemView; // If the root item itself is a TextView
                }
                if (categoryName == null) {
                    // Still null, create a dummy to avoid NPE, though this indicates a layout issue
                    // categoryName = new TextView(itemView.getContext());
                    // Log an error
                     android.util.Log.e("LiveCategoryAdapter", "TextView with ID 'category_name_text_view' not found in item_channel_category.xml");
                }
            }
        }
    }
}
