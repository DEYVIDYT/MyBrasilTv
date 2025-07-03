package com.example.iptvplayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide; // Import Glide
import com.bumptech.glide.request.RequestListener; // Para logs e tratamento de erro
import com.bumptech.glide.load.engine.GlideException; // Para logs e tratamento de erro
import com.bumptech.glide.request.target.Target; // Para logs e tratamento de erro
import android.graphics.drawable.Drawable; // Para RequestListener
import com.example.iptvplayer.R;
import com.example.iptvplayer.data.Channel;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder> {

    private List<Channel> channelList;
    private List<Channel> channelListFull;
    private OnChannelClickListener listener;

    public interface OnChannelClickListener {
        void onChannelClick(Channel channel);
    }

    public ChannelAdapter(Context context, List<Channel> channelList, OnChannelClickListener listener) {
        this.channelList = channelList;
        this.channelListFull = new ArrayList<>(channelList);
        this.listener = listener;
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
        holder.channelNumber.setText(String.format("%03d", position + 1));

        holder.channelProgram.setText("Recebendo a programação...");

        String logoUrl = channel.getLogoUrl();
        Log.d("ChannelAdapter", "Channel: " + channel.getName() + ", Logo URL: " + logoUrl);

        if (logoUrl != null && !logoUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(logoUrl)
                    .placeholder(R.drawable.rounded_corner_image_placeholder)
                    .error(R.drawable.rounded_corner_image_placeholder) // Define um drawable de erro
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@androidx.annotation.Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e("ChannelAdapter", "Glide onLoadFailed for " + channel.getName() + ": " + logoUrl, e);
                            return false; // Importante retornar false para que o error() drawable seja exibido.
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            Log.d("ChannelAdapter", "Glide onResourceReady for " + channel.getName() + ": " + logoUrl);
                            return false;
                        }
                    })
                    .into(holder.channelLogo);
        } else {
            Log.d("ChannelAdapter", "Logo URL is null or empty for " + channel.getName());
            // Define uma imagem padrão se a URL do logo for nula ou vazia
            Glide.with(holder.itemView.getContext())
                 .load(R.drawable.rounded_corner_image_placeholder)
                 .into(holder.channelLogo);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChannelClick(channel);
            }
        });
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
        this.channelListFull.clear();
        this.channelListFull.addAll(newChannelList);
        notifyDataSetChanged();
    }

    public void filterList(String text) {
        channelList.clear();
        if (text.isEmpty()) {
            channelList.addAll(channelListFull);
        } else {
            text = text.toLowerCase();
            for (Channel item : channelListFull) {
                if (item.getName().toLowerCase().contains(text)) {
                    channelList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }
}