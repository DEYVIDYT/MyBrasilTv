
package com.example.iptvplayer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptvplayer.R;
import com.example.iptvplayer.data.Channel;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder> {

    private List<Channel> channelList;

    public ChannelAdapter(List<Channel> channelList) {
        this.channelList = channelList;
    }

    @NonNull
    @Override
    public ChannelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_channel, parent, false);
        return new ChannelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelViewHolder holder, int position) {
        Channel channel = channelList.get(position);
        holder.channelName.setText(channel.getName());
        holder.channelNumber.setText(String.format("%03d", position + 1)); // Formato com 3 dígitos
        
        // Adicionando o texto de programação
        holder.channelProgram.setText("Recebendo a programação...");
        
        // Carregamento seguro de imagem com Picasso
        try {
            if (channel.getLogoUrl() != null && !channel.getLogoUrl().isEmpty()) {
                Picasso.get()
                    .load(channel.getLogoUrl())
                    .placeholder(R.drawable.ic_home_black_24dp) // Placeholder enquanto carrega
                    .error(R.drawable.ic_home_black_24dp) // Imagem de erro se falhar
                    .into(holder.channelLogo);
            } else {
                // Se não há URL, usar imagem padrão
                holder.channelLogo.setImageResource(R.drawable.ic_home_black_24dp);
            }
        } catch (Exception e) {
            // Em caso de erro, usar imagem padrão
            holder.channelLogo.setImageResource(R.drawable.ic_home_black_24dp);
        }
    }

    @Override
    public int getItemCount() {
        return channelList.size();
    }

    public static class ChannelViewHolder extends RecyclerView.ViewHolder {
        ImageView channelLogo;
        TextView channelNumber;
        TextView channelName;
        TextView channelProgram;

        public ChannelViewHolder(@NonNull View itemView) {
            super(itemView);
            channelLogo = itemView.findViewById(R.id.channel_logo);
            channelNumber = itemView.findViewById(R.id.channel_number);
            channelName = itemView.findViewById(R.id.channel_name);
            channelProgram = itemView.findViewById(R.id.channel_program);
        }
    }

    public void updateData(List<Channel> newChannelList) {
        this.channelList.clear();
        this.channelList.addAll(newChannelList);
        notifyDataSetChanged();
    }
}