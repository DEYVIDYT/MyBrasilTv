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
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import android.graphics.drawable.Drawable;
import android.util.Log; // Import Log
import com.example.iptvplayer.R;
import com.example.iptvplayer.data.Channel;

import java.util.ArrayList;
import java.util.List;

public class ChannelAdapterTv extends RecyclerView.Adapter<ChannelAdapterTv.ChannelViewHolder> {

    private List<Channel> channelList;
    private List<Channel> channelListFull;
    private OnChannelClickListener listener;
    private static final String TAG = "ChannelAdapterTv"; // Tag para logs

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
        // Corrigido para inflar o layout item_channel_tv.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_channel_tv, parent, false);
        return new ChannelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelViewHolder holder, int position) {
        Channel channel = channelList.get(position);

        // Verifica se os TextViews não são nulos antes de usar, como boa prática,
        // embora com o layout correto isso não deva ser um problema.
        if (holder.channelName != null) {
            holder.channelName.setText(channel.getName());
        }

        // channelNumber não existe em item_channel_tv.xml, então removemos a tentativa de usá-lo.
        // Se precisar de um número, ele teria que ser adicionado ao layout item_channel_tv.xml
        // e referenciado no ViewHolder.
        // holder.channelNumber.setText(String.format("%03d", position + 1));

        if (holder.channelProgram != null) {
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
        }

        String logoUrl = channel.getLogoUrl();
        if (holder.channelLogo != null) {
        if (logoUrl != null && !logoUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(logoUrl)
                    .placeholder(R.drawable.rounded_corner_image_placeholder)
                    .error(R.drawable.rounded_corner_image_placeholder)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@androidx.annotation.Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "Glide onLoadFailed for channel: " + channel.getName() + ", URL: " + model, e);
                            // Importante retornar false para que o error() drawable seja exibido.
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            // Log.d(TAG, "Glide onResourceReady for " + channel.getName()); // Opcional: log de sucesso
                            return false;
                        }
                    })
                    .into(holder.channelLogo);
        } else {
            // Se logoUrl for nulo ou vazio, carrega o placeholder diretamente
            Glide.with(holder.itemView.getContext())
                 .load(R.drawable.rounded_corner_image_placeholder)
                 .into(holder.channelLogo);
        }
    } // Fechamento do "if (holder.channelLogo != null)"

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
        // TextView channelNumber; // Removido pois não existe em item_channel_tv.xml
        TextView channelName;
        TextView channelProgram;

        public ChannelViewHolder(@NonNull View itemView) {
            super(itemView);
            // Os IDs devem corresponder aos definidos em item_channel_tv.xml
            channelLogo = itemView.findViewById(R.id.channel_tv_logo);
            channelName = itemView.findViewById(R.id.channel_tv_name);
            channelProgram = itemView.findViewById(R.id.channel_tv_program);
            // channelNumber = itemView.findViewById(R.id.channel_number); // Removido
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
