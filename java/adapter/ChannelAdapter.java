package com.example.iptvplayer.adapter;

import android.content.Context;
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
import android.util.Log; // Import Log

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
            Picasso.get()
                    .load(logoUrl)
                    .placeholder(R.drawable.rounded_corner_image_placeholder) // Usando o placeholder novo
                    .error(R.drawable.rounded_corner_image_placeholder) // Usando o placeholder novo como erro também
                    .into(holder.channelLogo, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d("ChannelAdapter", "Picasso onSuccess: " + logoUrl);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e("ChannelAdapter", "Picasso onError: " + logoUrl, e);
                            // Fallback em caso de erro do Picasso, já definido pelo .error()
                        }
                    });
        } else {
            Log.d("ChannelAdapter", "Logo URL is null or empty for " + channel.getName());
            holder.channelLogo.setImageResource(R.drawable.rounded_corner_image_placeholder); // Define um placeholder
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