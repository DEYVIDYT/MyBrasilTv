package com.example.iptvplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TvSideNavAdapter extends RecyclerView.Adapter<TvSideNavAdapter.ViewHolder> {

    private List<MainActivity.NavItem> navItems;
    private OnNavItemClickListener listener;
    private int selectedPosition = 0; // Padrão para o primeiro item selecionado

    public interface OnNavItemClickListener {
        void onNavItemClicked(MainActivity.NavItem navItem);
    }

    public TvSideNavAdapter(List<MainActivity.NavItem> navItems, OnNavItemClickListener listener) {
        this.navItems = navItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tv_side_nav, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MainActivity.NavItem currentItem = navItems.get(position);
        holder.icon.setImageResource(currentItem.iconResId);
        // holder.title.setText(currentItem.id); // Se o título for usado e visível

        holder.itemView.setSelected(selectedPosition == position);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNavItemClicked(currentItem);
            }
            // Atualizar a posição selecionada e notificar mudança para feedback visual
            // A MainActivity também pode controlar isso chamando um método no adapter se necessário
            int previousSelectedPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousSelectedPosition);
            notifyItemChanged(selectedPosition);

            // Dar foco ao item clicado, importante para D-Pad
            v.requestFocus();
        });

        // Para navegação D-Pad, garantir que o foco inicial vá para o item selecionado
        if (selectedPosition == position && holder.itemView.isInTouchMode()) {
            // Não forçar foco se estiver em modo touch para evitar comportamento estranho
        } else if (selectedPosition == position) {
             holder.itemView.requestFocus();
        }
    }

    @Override
    public int getItemCount() {
        return navItems != null ? navItems.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title; // Mesmo que não esteja visível, o ID existe

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.nav_icon);
            title = itemView.findViewById(R.id.nav_title);
        }
    }

    // Método para definir o item selecionado programaticamente se necessário pela Activity
    public void setSelectedPosition(int position) {
        if (position >= 0 && position < getItemCount()) {
            int previousSelectedPosition = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(previousSelectedPosition);
            notifyItemChanged(selectedPosition);
        }
    }
     public int getSelectedPosition() {
        return selectedPosition;
    }
}
