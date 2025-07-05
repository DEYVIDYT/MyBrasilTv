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

import java.util.List;

public class ChannelGridAdapter extends RecyclerView.Adapter<ChannelGridAdapter.ChannelViewHolder> {

    private Context mContext;
    private List<Channel> mChannels;
    private OnChannelClickListener mListener;

    public interface OnChannelClickListener {
        void onChannelClick(Channel channel);
    }

    public ChannelGridAdapter(Context context, List<Channel> channels, OnChannelClickListener listener) {
        mContext = context;
        mChannels = channels;
        mListener = listener;
    }

    @NonNull
    @Override
    public ChannelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_channel_grid, parent, false);
        return new ChannelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelViewHolder holder, int position) {
        Channel channel = mChannels.get(position);
        holder.bind(channel, position);
    }

    @Override
    public int getItemCount() {
        return mChannels != null ? mChannels.size() : 0;
    }

    public void updateChannels(List<Channel> channels) {
        mChannels = channels;
        notifyDataSetChanged();
    }

    class ChannelViewHolder extends RecyclerView.ViewHolder {
        private TextView mChannelNumber;
        private ImageView mChannelLogo;
        private TextView mChannelName;
        private TextView mChannelProgram;
        private ImageView mQualityIndicator;

        public ChannelViewHolder(@NonNull View itemView) {
            super(itemView);
            mChannelNumber = itemView.findViewById(R.id.tv_channel_number);
            mChannelLogo = itemView.findViewById(R.id.iv_channel_logo);
            mChannelName = itemView.findViewById(R.id.tv_channel_name);
            mChannelProgram = itemView.findViewById(R.id.tv_channel_program);
            mQualityIndicator = itemView.findViewById(R.id.iv_quality_indicator);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && mListener != null) {
                    Channel channel = mChannels.get(position);
                    mListener.onChannelClick(channel);
                }
            });
        }

        public void bind(Channel channel, int position) {
            // Channel number (using position + 1 as a simple numbering)
            String channelNumber = String.format("%03d", position + 1);
            mChannelNumber.setText(channelNumber);
            
            // Channel name
            mChannelName.setText(channel.getName());
            
            // Channel program (EPG info if available)
            String programInfo = channel.getCurrentProgramTitle();
            if (programInfo != null && !programInfo.isEmpty()) {
                mChannelProgram.setText(programInfo);
                mChannelProgram.setVisibility(View.VISIBLE);
            } else {
                mChannelProgram.setText("Sem programação");
                mChannelProgram.setVisibility(View.VISIBLE);
            }
            
            // Channel logo (placeholder for now)
            mChannelLogo.setImageResource(R.drawable.ic_dashboard_black_24dp);
            
            // Quality indicator (show for 4K channels)
            if (channel.getName().toLowerCase().contains("4k")) {
                mQualityIndicator.setVisibility(View.VISIBLE);
            } else {
                mQualityIndicator.setVisibility(View.GONE);
            }
        }
    }
}

