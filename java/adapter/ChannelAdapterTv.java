package com.example.iptvplayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.iptvplayer.R;
import com.example.iptvplayer.data.Channel;

import java.util.ArrayList;
import java.util.List;

public class ChannelAdapterTv extends RecyclerView.Adapter<ChannelAdapterTv.ChannelViewHolder> {

    private List<Channel> channelList;
    private List<Channel> channelListFull;
    private OnChannelClickListener listener;

    public interface OnChannelClickListener {
        void onChannelClick(Channel channel);
    }

    public ChannelAdapterTv(Context context, List<Channel> channelList, OnChannelClickListener listener) {
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

        if (channel.getCurrentProgramTitle() != null && !channel.getCurrentProgramTitle().isEmpty()) {
            String programTitle = channel.getCurrentProgramTitle();
            if (!programTitle.equals("Carregando programação...") && 
                !programTitle.equals("Programação não disponível") &&
                !programTitle.equals("Recebendo a programação...")) {
                holder.channelProgram.setText("Agora: " + programTitle);
            } else {
                holder.channelProgram.setText(programTitle);
            }
        } else {
            holder.channelProgram.setText("Carregando programação...");
        }

        String logoUrl = channel.getLogoUrl();
        if (logoUrl != null && !logoUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(logoUrl)
                    .placeholder(R.drawable.rounded_corner_image_placeholder)
                    .error(R.drawable.rounded_corner_image_placeholder)
                    .into(holder.channelLogo);
        } else {
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
            channelLogo = itemView.findViewById(R.id.channel_tv_logo);
            channelNumber = itemView.findViewById(R.id.channel_number); // Assuming this ID is correct or not used
            channelName = itemView.findViewById(R.id.channel_tv_name);
            channelProgram = itemView.findViewById(R.id.channel_tv_program);
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

    public void updateChannelProgram(String channelId, String programTitle) {
        for (int i = 0; i < channelList.size(); i++) {
            Channel channel = channelList.get(i);
            if (channel.getStreamId() != null && channel.getStreamId().equals(channelId)) {
                channel.setCurrentProgramTitle(programTitle);
                notifyItemChanged(i);
                break;
            }
        }
    }
}
