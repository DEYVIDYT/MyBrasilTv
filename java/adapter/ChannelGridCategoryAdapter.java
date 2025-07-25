package com.example.iptvplayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptvplayer.R;
import com.example.iptvplayer.component.ChannelGridView;

import java.util.List;

public class ChannelGridCategoryAdapter extends RecyclerView.Adapter<ChannelGridCategoryAdapter.CategoryViewHolder> {

    private Context mContext;
    private List<ChannelGridView.CategoryItem> mCategories;
    private OnCategoryClickListener mListener;
    private String mSelectedCategoryId = "0";

    public interface OnCategoryClickListener {
        void onCategoryClick(String categoryId);
    }

    public ChannelGridCategoryAdapter(Context context, List<ChannelGridView.CategoryItem> categories, OnCategoryClickListener listener) {
        mContext = context;
        mCategories = categories;
        mListener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_channel_grid_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        ChannelGridView.CategoryItem category = mCategories.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return mCategories != null ? mCategories.size() : 0;
    }

    public void setSelectedCategory(String categoryId) {
        mSelectedCategoryId = categoryId;
        notifyDataSetChanged();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private TextView mCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            mCategoryName = itemView.findViewById(R.id.tv_category_name);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && mListener != null) {
                    ChannelGridView.CategoryItem category = mCategories.get(position);
                    mListener.onCategoryClick(category.id);
                }
            });
        }

        public void bind(ChannelGridView.CategoryItem category) {
            mCategoryName.setText(category.name);
            
            // Highlight selected category
            if (category.id.equals(mSelectedCategoryId)) {
                // itemView.setSelected(true) é mais apropriado para usar com o seletor
                itemView.setSelected(true);
                // As cores de texto e fundo devem ser tratadas pelo seletor (category_item_selector.xml)
                // e pelo tema para texto.
                // No entanto, se quisermos forçar aqui, podemos usar atributos do tema:
                // TypedValue typedValue = new TypedValue();
                // mContext.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
                // mCategoryName.setTextColor(typedValue.data);
                // mContext.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true);
                // itemView.setBackgroundColor(typedValue.data);
            } else {
                itemView.setSelected(false);
                // mCategoryName.setTextColor(mContext.getResources().getColor(android.R.color.white)); // Deveria vir do tema/layout
                // itemView.setBackgroundColor(mContext.getResources().getColor(android.R.color.transparent)); // Deveria vir do seletor
            }
            // A cor do texto já está sendo definida no XML como ?attr/colorOnSurface.
            // O fundo do itemView é @drawable/category_item_selector.xml que lida com o estado selected.
            // Então, o código de mudança de cor aqui pode ser redundante ou entrar em conflito.
            // A melhor maneira é deixar o seletor cuidar do fundo e o tema/XML cuidar da cor do texto.
            // Apenas precisamos garantir que o estado 'selected' seja corretamente passado para a view.
             itemView.setSelected(category.id.equals(mSelectedCategoryId));

        }
    }
}

