package com.example.iptvplayer.adapter;

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

public class ChannelCategoryAdapterTv extends RecyclerView.Adapter<ChannelCategoryAdapterTv.CategoryViewHolder> {

    private List<XtreamApiService.CategoryInfo> categoryList;
    private final OnCategoryClickListener listener;
    private int selectedPosition = 0;

    public interface OnCategoryClickListener {
        void onCategoryClick(XtreamApiService.CategoryInfo category);
    }

    public ChannelCategoryAdapterTv(Context context, List<XtreamApiService.CategoryInfo> categoryList, OnCategoryClickListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Corrigido para inflar o layout item_channel_category_tv.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_channel_category_tv, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        XtreamApiService.CategoryInfo category = categoryList.get(position);
        holder.categoryName.setText(category.name);

        holder.itemView.setSelected(selectedPosition == position);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(category);
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

    public void updateData(List<XtreamApiService.CategoryInfo> newCategoryList) {
        this.categoryList.clear();
        this.categoryList.addAll(newCategoryList);
        selectedPosition = 0;
        notifyDataSetChanged();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.category_name_text_view_tv);
        }
    }
}
