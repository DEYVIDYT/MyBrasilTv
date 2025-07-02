package com.example.iptvplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iptvplayer.adapter.ChannelAdapter;
import com.example.iptvplayer.data.Channel;
import com.example.iptvplayer.parser.M3uParser;
import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TvFragment extends Fragment {

    private RecyclerView recyclerViewChannels;
    private RecyclerView recyclerViewCategories;
    private ChannelAdapter channelAdapter;
    private TextInputEditText searchEditText;
    private List<Channel> allChannels = new ArrayList<>();
    private DownloadReceiver downloadReceiver;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_tv, container, false);
        recyclerViewChannels = root.findViewById(R.id.recycler_view_channels);
        recyclerViewCategories = root.findViewById(R.id.recycler_view_categories);
        recyclerViewChannels.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        searchEditText = root.findViewById(R.id.search_edit_text);

        // Register receiver
        downloadReceiver = new DownloadReceiver();
        IntentFilter filter = new IntentFilter(DownloadService.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireActivity().registerReceiver(downloadReceiver, filter);
        }

        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterChannels(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {

            }
        });

        // Para fins de demonstração, vamos criar dados de exemplo para evitar problemas de download
        createExampleChannels();

        return root;
    }

    private void createExampleChannels() {
        executor.execute(() -> {
            try {
                // Criar dados de exemplo para canais IPTV
                StringBuilder channelContentBuilder = new StringBuilder();
                channelContentBuilder.append("#EXTM3U\n");
                
                // Canais Infantis
                channelContentBuilder.append("#EXTINF:-1 tvg-id=\"CartoonNetwork\" tvg-name=\"CARTOON NETWORK HD\" ");
                channelContentBuilder.append("tvg-logo=\"https://example.com/cartoon.png\" ");
                channelContentBuilder.append("group-title=\"Infantis\",CARTOON NETWORK HD\n");
                channelContentBuilder.append("http://example.com/cartoon.m3u8\n");
                
                channelContentBuilder.append("#EXTINF:-1 tvg-id=\"DiscoveryKids\" tvg-name=\"DISCOVERY KIDS\" ");
                channelContentBuilder.append("tvg-logo=\"https://example.com/discovery.png\" ");
                channelContentBuilder.append("group-title=\"Infantis\",DISCOVERY KIDS\n");
                channelContentBuilder.append("http://example.com/discovery.m3u8\n");
                
                channelContentBuilder.append("#EXTINF:-1 tvg-id=\"DragonBallSuper\" tvg-name=\"DRAGON BALL SUPER\" ");
                channelContentBuilder.append("tvg-logo=\"https://example.com/dragonball.png\" ");
                channelContentBuilder.append("group-title=\"Infantis\",DRAGON BALL SUPER\n");
                channelContentBuilder.append("http://example.com/dragonball.m3u8\n");
                
                channelContentBuilder.append("#EXTINF:-1 tvg-id=\"DragonBallZ\" tvg-name=\"DRAGON BALL Z\" ");
                channelContentBuilder.append("tvg-logo=\"https://example.com/dragonballz.png\" ");
                channelContentBuilder.append("group-title=\"Infantis\",DRAGON BALL Z\n");
                channelContentBuilder.append("http://example.com/dragonballz.m3u8\n");
                
                channelContentBuilder.append("#EXTINF:-1 tvg-id=\"GloobHD\" tvg-name=\"GLOOB HD\" ");
                channelContentBuilder.append("tvg-logo=\"https://example.com/gloob.png\" ");
                channelContentBuilder.append("group-title=\"Infantis\",GLOOB HD\n");
                channelContentBuilder.append("http://example.com/gloob.m3u8\n");
                
                channelContentBuilder.append("#EXTINF:-1 tvg-id=\"Gloobinho\" tvg-name=\"GLOOBINHO\" ");
                channelContentBuilder.append("tvg-logo=\"https://example.com/gloobinho.png\" ");
                channelContentBuilder.append("group-title=\"Infantis\",GLOOBINHO\n");
                channelContentBuilder.append("http://example.com/gloobinho.m3u8\n");
                
                channelContentBuilder.append("#EXTINF:-1 tvg-id=\"NickJr\" tvg-name=\"NICK JR HD\" ");
                channelContentBuilder.append("tvg-logo=\"https://example.com/nickjr.png\" ");
                channelContentBuilder.append("group-title=\"Infantis\",NICK JR HD\n");
                channelContentBuilder.append("http://example.com/nickjr.m3u8\n");
                
                // Canais Abertos
                channelContentBuilder.append("#EXTINF:-1 tvg-id=\"Globo\" tvg-name=\"GLOBO HD\" ");
                channelContentBuilder.append("tvg-logo=\"https://example.com/globo.png\" ");
                channelContentBuilder.append("group-title=\"Abertos\",GLOBO HD\n");
                channelContentBuilder.append("http://example.com/globo.m3u8\n");
                
                channelContentBuilder.append("#EXTINF:-1 tvg-id=\"SBT\" tvg-name=\"SBT HD\" ");
                channelContentBuilder.append("tvg-logo=\"https://example.com/sbt.png\" ");
                channelContentBuilder.append("group-title=\"Abertos\",SBT HD\n");
                channelContentBuilder.append("http://example.com/sbt.m3u8\n");
                
                // Canais de Notícias
                channelContentBuilder.append("#EXTINF:-1 tvg-id=\"GloboNews\" tvg-name=\"GLOBO NEWS\" ");
                channelContentBuilder.append("tvg-logo=\"https://example.com/globonews.png\" ");
                channelContentBuilder.append("group-title=\"Notícias\",GLOBO NEWS\n");
                channelContentBuilder.append("http://example.com/globonews.m3u8\n");
                
                String channelContent = channelContentBuilder.toString();
                
                try (BufferedReader br = new BufferedReader(new StringReader(channelContent))) {
                    allChannels = M3uParser.parse(br);
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        channelAdapter = new ChannelAdapter(allChannels);
                        recyclerViewChannels.setAdapter(channelAdapter);
                        Toast.makeText(getContext(), "Canais carregados com sucesso!", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Erro ao carregar canais de exemplo", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void startDownload() {
        // Método mantido para compatibilidade, mas agora usa dados de exemplo
        createExampleChannels();
    }

    private void processM3uFile(String filePath) {
        executor.execute(() -> {
            try {
                File file = new File(filePath);
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    allChannels = M3uParser.parse(br);
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        channelAdapter = new ChannelAdapter(allChannels);
                        recyclerViewChannels.setAdapter(channelAdapter);
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Failed to process list", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void filterChannels(String query) {
        List<Channel> filteredList = new ArrayList<>();
        for (Channel channel : allChannels) {
            if (channel.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(channel);
            }
        }
        if (channelAdapter != null) {
            channelAdapter = new ChannelAdapter(filteredList);
            recyclerViewChannels.setAdapter(channelAdapter);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unregister receiver
        if (downloadReceiver != null) {
            requireActivity().unregisterReceiver(downloadReceiver);
        }
    }

    private class DownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String filePath = intent.getStringExtra(DownloadService.EXTRA_FILE_PATH);
            if (filePath != null) {
                processM3uFile(filePath);
            } else {
                Toast.makeText(getContext(), "Download failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void updateChannels(List<Channel> channels) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                allChannels.clear();
                allChannels.addAll(channels);
                if (channelAdapter == null) {
                    channelAdapter = new ChannelAdapter(allChannels);
                    recyclerViewChannels.setAdapter(channelAdapter);
                } else {
                    channelAdapter.updateData(allChannels);
                }
            });
        }
    }
}