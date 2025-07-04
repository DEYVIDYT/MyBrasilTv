package com.example.iptvplayer;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.iptvplayer.data.Channel;
import com.example.iptvplayer.data.EpgProgram;

import xyz.doikki.videoplayer.player.VideoView; // Assuming DoikkiPlayer
// import xyz.doikki.videoplayer.controller.StandardVideoController; // Or a custom TV controller
import com.example.iptvplayer.StandardVideoController; // Corrigido para usar o controller do projeto

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TvFragmentTv extends Fragment implements DataManager.DataManagerListener {

    private static final String TV_TV_TAG = "TV_TV_DEBUG";

    private DataManager dataManager;

    private RecyclerView recyclerViewCategoriesTv, recyclerViewChannelsTv, recyclerViewEpgTv;
    private FrameLayout playerContainerTv;
    private VideoView videoViewTv;
    private StandardVideoController videoControllerTv; // Ou um controlador customizado para TV
    private ProgressBar playerProgressBarTv;

    // TODO: Criar adaptadores específicos para TV:
    // private ChannelCategoryAdapterTv categoryAdapterTv;
    // private ChannelAdapterTv channelAdapterTv;
    // private EpgAdapterTv epgAdapterTv;

    private List<Channel> currentChannels = new ArrayList<>();
    private List<EpgProgram> currentEpgPrograms = new ArrayList<>();
    private Map<String, String> currentCategoryMap; // Para nomes de categoria

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d(TV_TV_TAG, "onAttach called");
        dataManager = MyApplication.getDataManager(context);
        dataManager.setListener(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TV_TV_TAG, "onCreateView called");
        View root = inflater.inflate(R.layout.fragment_tv_tv, container, false);

        playerContainerTv = root.findViewById(R.id.tv_player_container_tv);
        playerProgressBarTv = root.findViewById(R.id.tv_player_progress_bar_tv);

        recyclerViewCategoriesTv = root.findViewById(R.id.recycler_view_tv_categories_tv);
        recyclerViewChannelsTv = root.findViewById(R.id.recycler_view_tv_channels_tv);
        recyclerViewEpgTv = root.findViewById(R.id.recycler_view_tv_epg_tv);

        setupRecyclerViews();
        initializePlayer();

        Log.d(TV_TV_TAG, "Views initialized.");
        return root;
    }

    private void setupRecyclerViews() {
        recyclerViewCategoriesTv.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewChannelsTv.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewEpgTv.setLayoutManager(new LinearLayoutManager(getContext()));

        // TODO: Inicializar e setar os adaptadores (CategoryAdapterTv, ChannelAdapterTv, EpgAdapterTv)
        // Exemplo:
        // categoryAdapterTv = new ChannelCategoryAdapterTv(getContext(), ..., this::onCategorySelected);
        // recyclerViewCategoriesTv.setAdapter(categoryAdapterTv);
        //
        // channelAdapterTv = new ChannelAdapterTv(getContext(), ..., this::onChannelSelected);
        // recyclerViewChannelsTv.setAdapter(channelAdapterTv);
        //
        // epgAdapterTv = new EpgAdapterTv(getContext(), ..., this::onEpgProgramSelected);
        // recyclerViewEpgTv.setAdapter(epgAdapterTv);

        // Adicionar foco para D-Pad
        recyclerViewCategoriesTv.setFocusable(true);
        recyclerViewChannelsTv.setFocusable(true);
        recyclerViewEpgTv.setFocusable(true);
    }

    private void initializePlayer() {
        videoViewTv = new VideoView(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        videoViewTv.setLayoutParams(params);
        playerContainerTv.addView(videoViewTv);

        videoControllerTv = new StandardVideoController(requireContext());
        // TODO: Adicionar componentes de controle customizados para TV se necessário
        // videoControllerTv.addControlComponent(new TitleView(requireContext())); // Exemplo
        videoViewTv.setVideoController(videoControllerTv);

        videoViewTv.addOnStateChangeListener(new VideoView.SimpleOnStateChangeListener() {
            @Override
            public void onPlayStateChanged(int playState) {
                if (playState == VideoView.STATE_PREPARING || playState == VideoView.STATE_BUFFERING) {
                    playerProgressBarTv.setVisibility(View.VISIBLE);
                } else {
                    playerProgressBarTv.setVisibility(View.GONE);
                }
                // Outros tratamentos de estado (erro, completado, etc.)
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TV_TV_TAG, "onResume called");
        if (videoViewTv != null) videoViewTv.resume();
        updateUi();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TV_TV_TAG, "onPause called");
        if (videoViewTv != null) videoViewTv.pause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TV_TV_TAG, "onDestroyView called");
        if (videoViewTv != null) videoViewTv.release();
    }

    @Override
    public void onDataLoaded() {
        Log.d(TV_TV_TAG, "onDataLoaded callback received.");
        if (isAdded() && getContext() != null) {
            updateUi();
        }
    }

    @Override
    public void onProgressUpdate(DataManager.LoadState state, int percentage, String message) {
        if (state == DataManager.LoadState.COMPLETE || state == DataManager.LoadState.FAILED) { // Corrigido para FAILED
            showLoading(false); // Assumindo que showLoading manipula o ProgressBar principal do fragmento
        } else {
            showLoading(true);
        }
    }

    @Override
    public void onError(String errorMessage) {
        Log.e(TV_TV_TAG, "DataManager Error: " + errorMessage);
        if (isAdded() && getContext() != null) {
            showLoading(false);
            Toast.makeText(getContext(), "Erro ao carregar dados para TV Ao Vivo: " + errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void updateUi() {
        Log.d(TV_TV_TAG, "updateUi called");
        if (dataManager != null && dataManager.isDataFullyLoaded()) {
            Log.d(TV_TV_TAG, "Data is fully loaded for Live TV.");
            showLoading(false);
            loadLiveCategories();
            // Carregar canais da primeira categoria ou todos os canais por padrão
            // loadChannelsForCategory(null); // ou um ID de categoria padrão
        } else {
            Log.d(TV_TV_TAG, "Data not loaded for Live TV. Displaying loading indicator.");
            showLoading(true);
            if (dataManager != null && !dataManager.isLoading()) { // Agora usa o método isLoading()
                dataManager.startDataLoading();
            }
        }
    }

    private void showLoading(boolean isLoading) {
        // TODO: Implementar lógica para mostrar/ocultar um indicador de loading geral para o fragmento se necessário.
        // playerProgressBarTv é para o player.
        Log.d(TV_TV_TAG, "showLoading called with: " + isLoading + " (Fragment-level, not player)");
    }

    private void loadLiveCategories() {
        List<XtreamApiService.CategoryInfo> categories = dataManager.getLiveCategories();
        if (categories != null) {
            Log.d(TV_TV_TAG, "Loading " + categories.size() + " live categories.");
            // TODO: categoryAdapterTv.updateData(categories);
            // Se o adapter não existir, instanciar aqui.
            // Fazer a primeira categoria ser selecionada por padrão e carregar seus canais.
            if (!categories.isEmpty() && recyclerViewCategoriesTv.getChildCount() > 0) {
                 // Simular seleção da primeira categoria para carregar canais
                 // onCategorySelected(categories.get(0));
            }
        } else {
            Log.w(TV_TV_TAG, "No live categories found.");
        }
    }

    // private void onCategorySelected(XtreamApiService.CategoryInfo category) {
        // Log.d(TV_TV_TAG, "Category selected: " + category.name);
        // List<Channel> channels = dataManager.getLiveStreamsByCategory(category.id);
        // if (channels != null) {
            // channelAdapterTv.updateData(channels);
            // Se houver canais, selecionar o primeiro e carregar EPG/Player.
            // if(!channels.isEmpty()) onChannelSelected(channels.get(0));
        // }
    // }

    // private void onChannelSelected(Channel channel) {
        // Log.d(TV_TV_TAG, "Channel selected: " + channel.getName());
        // videoViewTv.release();
        // videoViewTv.setUrl(channel.getStreamUrl());
        // videoViewTv.start();
        // if (videoControllerTv != null && videoControllerTv.getTitleView() != null) {
        //    videoControllerTv.getTitleView().setTitle(channel.getName());
        // }
        // loadEpgForChannel(channel.getStreamId());
    // }

    // private void loadEpgForChannel(String streamId) {
        // dataManager.getXmltvEpgService().fetchChannelEpg(streamId, new EpgService.EpgCallback() { ... });
        // no callback: epgAdapterTv.updateData(programs);
    // }

    // public boolean onBackPressed() {
    //    if (videoViewTv != null && videoViewTv.isFullScreen()) {
    //        return videoViewTv.onBackPressed(); // Sair da tela cheia
    //    }
    //    return false; // Permitir que a Activity lide com o back press
    // }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TV_TV_TAG, "onDetach called");
        if (dataManager != null) {
            dataManager.setListener(null); // Corrigido para setListener(null)
        }
    }
}
