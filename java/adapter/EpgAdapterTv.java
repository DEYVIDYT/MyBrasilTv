package com.example.iptvplayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.iptvplayer.R;
import com.example.iptvplayer.data.EpgProgram;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EpgAdapterTv extends RecyclerView.Adapter<EpgAdapterTv.EpgViewHolder> {

    private Context context;
    private List<EpgProgram> programs;
    private OnProgramClickListener listener;

    public interface OnProgramClickListener {
        void onProgramClick(EpgProgram program);
    }

    public EpgAdapterTv(Context context, List<EpgProgram> programs, OnProgramClickListener listener) {
        this.context = context;
        this.programs = programs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EpgViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Corrigido para inflar o layout item_epg_program_tv.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_epg_program_tv, parent, false);
        return new EpgViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EpgViewHolder holder, int position) {
        EpgProgram program = programs.get(position);
        
        holder.titleTextView.setText(program.getTitle());
        holder.descriptionTextView.setText(program.getDescription());
        
        // Formatar horários
        String timeRange = formatTimeRange(program.getStartTime(), program.getEndTime());
        holder.timeTextView.setText(timeRange);
        
        // Destacar programa atual
        if (program.isCurrentlyActive()) {
            holder.itemView.setBackgroundResource(R.drawable.tab_selected_background);
            holder.titleTextView.setTextColor(context.getResources().getColor(android.R.color.white));
        } else {
            holder.itemView.setBackgroundResource(R.drawable.tab_unselected_background);
            holder.titleTextView.setTextColor(context.getResources().getColor(android.R.color.primary_text_light));
        }
        
        // Mostrar categoria se disponível
        if (program.getCategory() != null && !program.getCategory().isEmpty()) {
            // holder.categoryTextView.setText(program.getCategory()); // Removed as it's not in TV layout
            // holder.categoryTextView.setVisibility(View.VISIBLE); // Removed as it's not in TV layout
        } else {
            // holder.categoryTextView.setVisibility(View.GONE); // Removed as it's not in TV layout
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProgramClick(program);
            }
        });
    }

    @Override
    public int getItemCount() {
        return programs != null ? programs.size() : 0;
    }

    public void updateData(List<EpgProgram> newPrograms) {
        this.programs = newPrograms;
        notifyDataSetChanged();
    }

    private String formatTimeRange(String startTime, String endTime) {
        try {
            long start = Long.parseLong(startTime) * 1000; // Converter para milliseconds
            long end = Long.parseLong(endTime) * 1000;
            
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String startFormatted = timeFormat.format(new Date(start));
            String endFormatted = timeFormat.format(new Date(end));
            
            return startFormatted + " - " + endFormatted;
        } catch (NumberFormatException e) {
            return startTime + " - " + endTime; // Fallback para formato original
        }
    }

    static class EpgViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView descriptionTextView;
        TextView timeTextView;

        public EpgViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.epg_item_tv_title);
            descriptionTextView = itemView.findViewById(R.id.epg_item_tv_description);
            timeTextView = itemView.findViewById(R.id.epg_item_tv_time);
        }
    }
}
