package com.example.iptvplayer;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent; // Adicionado para iniciar VideoPlayerActivity
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
import android.view.KeyEvent;
import com.example.iptvplayer.component.ChannelGridView;

import com.example.iptvplayer.adapter.ChannelCategoryAdapterTv;
import com.example.iptvplayer.adapter.ChannelAdapterTv;
import com.example.iptvplayer.adapter.EpgAdapterTv;
import com.example.iptvplayer.component.TitleView;

import xyz.doikki.videoplayer.player.VideoView; // Assuming DoikkiPlayer
// import xyz.doikki.videoplayer.controller.StandardVideoController; // Or a custom TV controller
import com.example.iptvplayer.StandardVideoController; // Corrigido para usar o controller do projeto

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TvFragmentTv extends Fragment implements DataManager.DataManagerListener, TvKeyHandler.TvKeyListener {

    private static final String TV_TV_TAG = "TV_TV_DEBUG";

    private DataManager dataManager;

    // Views do layout original fragment_tv_tv.xml
    private RecyclerView recyclerViewCategoriesTv;
    private RecyclerView recyclerViewChannelsTv;
    private RecyclerView recyclerViewEpgTv;
    private FrameLayout playerContainerTv;
    private ProgressBar playerProgressBarTv; // ProgressBar para o player
    // private ProgressBar channelsProgressBar; // Removido, usando playerProgressBarTv para o player

    // Componentes do Player Embutido
    private VideoView videoViewTv;
    private StandardVideoController videoControllerTv;
    private TitleView mTitleViewComponent;
    private ChannelGridView mChannelGridView;

    // Adapters
    private ChannelCategoryAdapterTv categoryAdapterTv;
    private ChannelAdapterTv channelAdapterTv;
    private EpgAdapterTv epgAdapterTv; // Reintroduzido

    // Listas de dados (se necessário manter no fragmento)
    // private List<Channel> currentChannels = new ArrayList<>();
    // private List<EpgProgram> currentEpgPrograms = new ArrayList<>();


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d(TV_TV_TAG, "onAttach called");
        dataManager = MyApplication.getDataManager(context);
        if (dataManager == null) {
            Log.e(TV_TV_TAG, "DataManager is null in onAttach!");
            // Handle this error appropriately, e.g., show a toast or finish activity
            return;
        }
        dataManager.setListener(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TV_TV_TAG, "onCreateView called - Restoring Original TV Layout");
        // 1a. Inflar o layout original da TV
        View root = inflater.inflate(R.layout.fragment_tv_tv, container, false);

        // 1b & 1c. Encontrar views do layout original
        playerContainerTv = root.findViewById(R.id.tv_player_container_tv);
        playerProgressBarTv = root.findViewById(R.id.tv_player_progress_bar_tv); // ProgressBar do Player

        recyclerViewCategoriesTv = root.findViewById(R.id.recycler_view_tv_categories_tv);
        recyclerViewChannelsTv = root.findViewById(R.id.recycler_view_tv_channels_tv);
        recyclerViewEpgTv = root.findViewById(R.id.recycler_view_tv_epg_tv); // EPG RecyclerView

        Log.d(TV_TV_TAG, "Calling setupRecyclerViews() for original layout");
        setupRecyclerViews(); // Ajustar para os IDs corretos e reintroduzir EPG

        // 1f. Inicializar a base do player (VideoView e seu listener de estado)
        initializePlayerView();

        Log.d(TV_TV_TAG, "Views initialized for original TV layout.");
        return root;
    }

    private void setupRecyclerViews() {
        Log.d(TV_TV_TAG, "setupRecyclerViews called for original layout");
        if (getContext() == null) {
            Log.e(TV_TV_TAG, "Context is null in setupRecyclerViews!");
            return;
        }
        // Configurar RecyclerView de Categorias
        recyclerViewCategoriesTv.setLayoutManager(new LinearLayoutManager(getContext()));
        categoryAdapterTv = new ChannelCategoryAdapterTv(getContext(), new ArrayList<>(), this::onCategorySelected);
        recyclerViewCategoriesTv.setAdapter(categoryAdapterTv);
        recyclerViewCategoriesTv.setFocusable(true);

        // Configurar RecyclerView de Canais
        recyclerViewChannelsTv.setLayoutManager(new LinearLayoutManager(getContext()));
        channelAdapterTv = new ChannelAdapterTv(getContext(), new ArrayList<>(), this::onChannelSelected);
        recyclerViewChannelsTv.setAdapter(channelAdapterTv);
        recyclerViewChannelsTv.setFocusable(true);
        
        // Configurar RecyclerView de EPG (1c)
        recyclerViewEpgTv.setLayoutManager(new LinearLayoutManager(getContext()));
        epgAdapterTv = new EpgAdapterTv(getContext(), new ArrayList<>(), this::onEpgProgramSelected);
        recyclerViewEpgTv.setAdapter(epgAdapterTv);
        recyclerViewEpgTv.setFocusable(true);

        Log.d(TV_TV_TAG, "RecyclerViews setup complete for original layout.");
    }

    private void initializePlayerView() {
        Log.d(TV_TV_TAG, "initializePlayerView called");
        if (getContext() == null || playerContainerTv == null) {
            Log.e(TV_TV_TAG, "Context or playerContainerTv is null in initializePlayerView.");
            return;
        }

        videoViewTv = new VideoView(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        videoViewTv.setLayoutParams(params);
        playerContainerTv.addView(videoViewTv);

        videoViewTv.addOnStateChangeListener(new VideoView.SimpleOnStateChangeListener() {
            @Override
            public void onPlayStateChanged(int playState) {
                Log.d(TV_TV_TAG, "Player state changed: " + playState);
                if (!isAdded() || playerProgressBarTv == null) {
                    Log.w(TV_TV_TAG, "onPlayStateChanged: Fragment not added or playerProgressBarTv is null. State: " + playState);
                    return;
                }
                if (playState == VideoView.STATE_PREPARING || playState == VideoView.STATE_BUFFERING) {
                    playerProgressBarTv.setVisibility(View.VISIBLE);
                } else {
                    playerProgressBarTv.setVisibility(View.GONE);
                }
            }
        });
        Log.d(TV_TV_TAG, "Base VideoView initialized and added to container.");
    }

    private void setupControllerAndComponents(Channel channelToPlay) {
        Log.d(TV_TV_TAG, "setupControllerAndComponents for channel: " + channelToPlay.getName());
        if (getContext() == null || videoViewTv == null) {
            Log.e(TV_TV_TAG, "Context or videoViewTv is null in setupControllerAndComponents.");
            return;
        }

        // 2.b.i. Anular referência ao controller antigo. O VideoView lida com a limpeza do controller anterior ao setar um novo.
        videoControllerTv = null;

        // 2.b.ii. Criar uma nova instância de StandardVideoController.
        videoControllerTv = new StandardVideoController(requireContext());

        // 2.b.iii. Criar novas instâncias de TitleView e ChannelGridView.
        mTitleViewComponent = new TitleView(requireContext());
        mChannelGridView = new ChannelGridView(requireContext());

        // 2.b.iv. Adicionar o novo TitleView e o novo ChannelGridView ao novo videoControllerTv.
        videoControllerTv.addControlComponent(mTitleViewComponent);
        videoControllerTv.addControlComponent(mChannelGridView);

        // Adicionar outros componentes de controle padrão se necessário para TV (ex: ErrorView, PrepareView)
        // controller.addControlComponent(new ErrorView(requireContext()));
        // controller.addControlComponent(new PrepareView(requireContext()));


        // 2.b.v. Popular o mChannelGridView com a lista de canais relevante.
        if (dataManager != null && dataManager.getLiveStreams() != null && dataManager.getLiveCategoriesMap() != null) {
            Log.d(TV_TV_TAG, "Populating ChannelGridView with " + dataManager.getLiveStreams().size() + " channels.");
            mChannelGridView.setChannelsData(dataManager.getLiveStreams(), dataManager.getLiveCategoriesMap());
        } else {
            Log.e(TV_TV_TAG, "DataManager or its data is null when trying to set ChannelGridView data!");
            // Opcional: popular com uma lista vazia ou mostrar um estado de erro na grade
            mChannelGridView.setChannelsData(new ArrayList<>(), new java.util.HashMap<>());
        }

        // 2.b.vi. Configurar o listener do mChannelGridView.
        mChannelGridView.setChannelSelectedListener(selectedChannelFromGrid -> {
            Log.d(TV_TV_TAG, "Channel selected from ChannelGridView: " + selectedChannelFromGrid.getName());
            onChannelSelected(selectedChannelFromGrid); // Chama o mesmo método para trocar o canal
        });

        // 2.b.vii. Definir o novo videoControllerTv no videoViewTv.
        videoViewTv.setVideoController(videoControllerTv);

        // 2.b.viii. Atualizar o título no mTitleViewComponent.
        if (mTitleViewComponent != null) {
            mTitleViewComponent.setTitle(channelToPlay.getName());
        }
        Log.d(TV_TV_TAG, "Controller and components re-initialized and set for: " + channelToPlay.getName());
    }


    // 1e. Ajustar ciclo de vida para o player embutido
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TV_TV_TAG, "onResume called - Original TV Layout");
        if (videoViewTv != null) {
            videoViewTv.resume();
        }
        Log.d(TV_TV_TAG, "Calling updateUi() from onResume");
        updateUi();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TV_TV_TAG, "onPause called - Original TV Layout");
        if (videoViewTv != null) {
            videoViewTv.pause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TV_TV_TAG, "onDestroyView called - Original TV Layout");
        if (videoViewTv != null) {
            videoViewTv.release();
            videoViewTv = null; // Limpar referência
        }
        // Outras limpezas se necessário (ex: videoControllerTv = null)
    }

    @Override
    public void onDataLoaded() {
        Log.d(TV_TV_TAG, "onDataLoaded callback received.");
        if (isAdded() && getContext() != null) {
            Log.d(TV_TV_TAG, "Calling updateUi() from onDataLoaded");
            updateUi();
        } else {
            Log.w(TV_TV_TAG, "onDataLoaded: Fragment not added or context is null. Cannot update UI.");
        }
    }

    @Override
    public void onProgressUpdate(DataManager.LoadState state, int percentage, String message) {
        Log.d(TV_TV_TAG, "DataManager Progress: " + state + " - " + percentage + "% - " + message);
        if (state == DataManager.LoadState.COMPLETE || state == DataManager.LoadState.FAILED) { // Corrigido para FAILED
            showLoading(false); // Assumindo que showLoading manipula o ProgressBar principal do fragmento
        } else {
            showLoading(true);
        }
    }

    @Override
    public void onError(String errorMessage) {
        Log.e(TV_TV_TAG, "DataManager Error: " + errorMessage);
        if (isAdded() && getContext() != null) { // getContext() check is good here
            showLoading(false);
            Toast.makeText(getContext(), "Erro ao carregar dados para TV Ao Vivo: " + errorMessage, Toast.LENGTH_LONG).show();
        } else {
            Log.w(TV_TV_TAG, "onError: Fragment not added or context is null. Error: " + errorMessage);
        }
    }

    private void updateUi() {
        Log.d(TV_TV_TAG, "updateUi called");
        if (dataManager == null) {
            Log.e(TV_TV_TAG, "DataManager is null in updateUi!");
            return;
        }
        if (dataManager.isDataFullyLoaded()) {
            Log.d(TV_TV_TAG, "Data is fully loaded for Live TV.");
            showLoading(false);
            Log.d(TV_TV_TAG, "Calling loadLiveCategories()");
            loadLiveCategories();
            // Carregar canais da primeira categoria ou todos os canais por padrão
            if (categoryAdapterTv != null && dataManager.getLiveCategories() != null && !dataManager.getLiveCategories().isEmpty()) {
                Log.d(TV_TV_TAG, "Selecting first category: " + dataManager.getLiveCategories().get(0).name);
                onCategorySelected(dataManager.getLiveCategories().get(0));
            } else {
                Log.w(TV_TV_TAG, "categoryAdapterTv is null or live categories are empty. Cannot select first category.");
            }
        } else {
            Log.d(TV_TV_TAG, "Data not loaded for Live TV. Displaying loading indicator.");
            showLoading(true);
            if (!dataManager.isLoading()) { // Agora usa o método isLoading()
                Log.d(TV_TV_TAG, "DataManager not loading, starting data load.");
                dataManager.startDataLoading();
            } else {
                Log.d(TV_TV_TAG, "DataManager is already loading.");
            }
        }
    }

    private void showLoading(boolean isLoading) {
        // Este método controlava o channelsProgressBar, que foi removido.
        // O playerProgressBarTv é controlado pelo listener de estado do player.
        // Se for necessário um ProgressBar geral para carregamento de categorias/canais,
        // ele precisaria ser adicionado ao layout e referenciado aqui.
        // Por enquanto, este método não fará nada ou controlará um ProgressBar geral se adicionado.
        Log.d(TV_TV_TAG, "showLoading (geral): " + isLoading);
        // Exemplo se tivéssemos um progressBarGeral:
        // if (progressBarGeral != null) {
        //     progressBarGeral.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        // }
    }

    private void loadLiveCategories() {
        Log.d(TV_TV_TAG, "loadLiveCategories called");
        List<XtreamApiService.CategoryInfo> categories = dataManager.getLiveCategories();
        if (categories != null) {
            Log.d(TV_TV_TAG, "Loading " + categories.size() + " live categories.");
            if (categoryAdapterTv != null) {
                categoryAdapterTv.updateData(categories);
            } else {
                Log.e(TV_TV_TAG, "categoryAdapterTv is null in loadLiveCategories!");
            }
            // Se o adapter não existir, instanciar aqui.
            // Fazer a primeira categoria ser selecionada por padrão e carregar seus canais.
            if (!categories.isEmpty()) {
                 Log.d(TV_TV_TAG, "Selecting first category in loadLiveCategories: " + categories.get(0).name);
                 onCategorySelected(categories.get(0));
            }
        } else {
            Log.w(TV_TV_TAG, "No live categories found.");
        }
    }

    private void onCategorySelected(XtreamApiService.CategoryInfo category) {
        Log.d(TV_TV_TAG, "onCategorySelected: " + category.name);
        // A ProgressBar geral (showLoading) pode ser usada aqui se a busca for longa
        // showLoading(true);

        List<Channel> channels = dataManager.getLiveStreamsByCategory(category.id);
        // showLoading(false);

        if (channels != null) {
            Log.d(TV_TV_TAG, "Updating channelAdapterTv with " + channels.size() + " channels for category: " + category.name);
            if (channelAdapterTv != null) {
                channelAdapterTv.updateData(channels);
                if (channels.isEmpty()) {
                    if (getContext() != null) Toast.makeText(getContext(), "Nenhum canal nesta categoria.", Toast.LENGTH_SHORT).show();
                } else {
                    if (recyclerViewChannelsTv != null) recyclerViewChannelsTv.requestFocus();
                }
            } else {
                Log.e(TV_TV_TAG, "channelAdapterTv is null in onCategorySelected!");
            }
        } else {
            Log.w(TV_TV_TAG, "No channels found for category: " + category.name);
            if (channelAdapterTv != null) {
                channelAdapterTv.updateData(new ArrayList<>()); // Limpar lista
            }
            if (getContext() != null) Toast.makeText(getContext(), "Nenhum canal encontrado para: " + category.name, Toast.LENGTH_SHORT).show();
        }
    }

    // 1d. Modificar onChannelSelected para player embutido (lógica completa no Passo 3)
    private void onChannelSelected(Channel channel) {
        Log.d(TV_TV_TAG, "onChannelSelected for embedded player: " + channel.getName() + ", URL: " + channel.getStreamUrl());

        if (!isAdded() || getContext() == null) {
            Log.e(TV_TV_TAG, "onChannelSelected: Fragment not added or context is null.");
            return;
        }

        String streamUrl = channel.getStreamUrl();
        if (streamUrl == null || streamUrl.isEmpty()) {
            Log.e(TV_TV_TAG, "Stream URL is null or empty for channel: " + channel.getName());
            if (getContext() != null) Toast.makeText(getContext(), "URL de stream inválida para " + channel.getName(), Toast.LENGTH_LONG).show();
            // Limpar o player se estiver tocando algo
            if (videoViewTv != null) {
                videoViewTv.release();
            }
            return;
        }

        // Lógica de re-inicialização do player e controller virá no Passo 2 e 3 do plano.
        // Por enquanto, apenas log e uma chamada básica se videoViewTv existir.
        if (videoViewTv == null) {
            Log.e(TV_TV_TAG, "videoViewTv is null in onChannelSelected. Player not fully initialized yet by this new flow.");
            // Isso pode acontecer se onChannelSelected for chamado antes do player estar pronto
            // (ex: na primeira carga de dados). O Passo 2 e 3 cuidarão da inicialização completa.
            initializePlayerView(); // Garante que o videoViewTv base exista
        }

        // 3a. Chamar videoViewTv.release() para parar e limpar o stream atual.
        // Isso limpa o player interno. A instância VideoView (videoViewTv) permanece.
        if (videoViewTv.getCurrentPlayState() != VideoView.STATE_IDLE) { // Só liberar se não estiver idle (evita liberar se já liberado)
            videoViewTv.release(); // Limpa o player interno, para a reprodução, etc.
        }

        // 3b. Chamar o novo método setupControllerAndComponents(channel).
        // Isso irá (re)criar e configurar videoControllerTv, mTitleViewComponent, mChannelGridView
        // e definir o novo controller no videoViewTv.
        setupControllerAndComponents(channel);

        // 3c. Definir a URL no videoViewTv.
        videoViewTv.setUrl(streamUrl);

        // 3d. Iniciar a reprodução.
        videoViewTv.start();

        Log.d(TV_TV_TAG, "Playback started for: " + channel.getName());

        // 3f. (Opcional por enquanto) Chamar loadEpgForChannel.
        // Vamos reativar isso pois a UI do EPG foi restaurada.
        loadEpgForChannel(channel.getStreamId());
    }

    // 1c. Reintroduzir lógica de EPG
    private void loadEpgForChannel(String streamId) {
        Log.d(TV_TV_TAG, "loadEpgForChannel called for streamId: " + streamId);
        if (dataManager == null || dataManager.getXmltvEpgService() == null) {
            Log.e(TV_TV_TAG, "DataManager or XmltvEpgService is null in loadEpgForChannel!");
            return;
        }
        // Mostrar um indicador de carregamento para o EPG se houver um específico
        dataManager.getXmltvEpgService().fetchChannelEpg(streamId, new EpgService.EpgCallback() {
            @Override
            public void onSuccess(List<EpgProgram> programs) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded() || getContext() == null) {
                            Log.w(TV_TV_TAG, "loadEpgForChannel.onSuccess: Fragment not added or context null in UI thread.");
                            return;
                        }
                        Log.d(TV_TV_TAG, "XMLTV EPG loaded successfully: " + programs.size() + " programs for streamId " + streamId);
                        if (epgAdapterTv != null) {
                            epgAdapterTv.updateData(programs);
                        } else {
                            Log.e(TV_TV_TAG, "epgAdapterTv is null in loadEpgForChannel.onSuccess!");
                        }
                    });
                } else {
                    Log.w(TV_TV_TAG, "loadEpgForChannel.onSuccess: Fragment not attached or activity is null.");
                }
            }

            @Override
            public void onFailure(String error) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded() || getContext() == null) {
                            Log.w(TV_TV_TAG, "loadEpgForChannel.onFailure: Fragment not added or context null in UI thread.");
                            return;
                        }
                        Log.e(TV_TV_TAG, "Failed to load XMLTV EPG: " + error);
                        if (getContext() != null) Toast.makeText(getContext(), "Erro ao carregar EPG XMLTV: " + error, Toast.LENGTH_LONG).show();
                        if (epgAdapterTv != null) {
                            epgAdapterTv.updateData(new ArrayList<>());
                        } else {
                            Log.e(TV_TV_TAG, "epgAdapterTv is null in loadEpgForChannel.onFailure!");
                        }
                    });
                } else {
                    Log.w(TV_TV_TAG, "loadEpgForChannel.onFailure: Fragment not attached or activity is null.");
                }
            }
        });
    }

    public void onEpgProgramSelected(EpgProgram program) {
        Log.d(TV_TV_TAG, "onEpgProgramSelected: " + program.getTitle());
        // Implementar o que acontece quando um programa EPG é clicado.
        // Ex: Mostrar detalhes do programa em um Toast ou Dialog.
        if (getContext() != null) {
            String details = "Programa: " + program.getTitle() + "\nDescrição: " + program.getDescription();
            Toast.makeText(getContext(), details, Toast.LENGTH_LONG).show();
        }
    }

    // onTvKeyDown e onTvKeyUp serão restaurados no Passo 4.
    // Se a navegação por D-Pad precisar de tratamento especial para os RecyclerViews,
    // isso seria feito de forma diferente, geralmente pelo foco do sistema.
    @Override
    public boolean onTvKeyDown(int keyCode, KeyEvent event) {
        Log.d(TV_TV_TAG, "onTvKeyDown: keyCode=" + keyCode);
        // 4a. Restaurar lógica para mostrar/esconder mChannelGridView
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_BUTTON_SELECT) {
            if (mChannelGridView != null && videoViewTv != null && videoViewTv.isPlaying()) { // Só mostrar se o player estiver ativo
                if (mChannelGridView.isShown()) {
                    Log.d(TV_TV_TAG, "Hiding ChannelGridView via DPAD_CENTER");
                    mChannelGridView.hideChannelGrid();
                } else {
                    Log.d(TV_TV_TAG, "Showing ChannelGridView via DPAD_CENTER");
                    // Popular dados antes de mostrar, caso DataManager tenha sido atualizado
                    if (dataManager != null && dataManager.getLiveStreams() != null && dataManager.getLiveCategoriesMap() != null) {
                         mChannelGridView.setChannelsData(dataManager.getLiveStreams(), dataManager.getLiveCategoriesMap());
                    }
                    mChannelGridView.showChannelGrid();
                }
                return true; // Evento consumido
            }
        }
        // Considerar adicionar navegação para cima/baixo na lista de canais se o foco estiver no player
        // ou outras interações de D-Pad específicas do player.
        return false; // Deixar o sistema tratar outros eventos de D-Pad
    }

    @Override
    public boolean onTvKeyUp(int keyCode, KeyEvent event) {
        // Log.d(TV_TV_TAG, "onTvKeyUp: keyCode=" + keyCode); // Geralmente não necessário
        return false;
    }

    public boolean onBackPressed() {
        Log.d(TV_TV_TAG, "onBackPressed called - Restored Logic");
        // 4b. Restaurar lógica de tela cheia e ChannelGridView
        if (videoViewTv != null && videoViewTv.isFullScreen()) {
            Log.d(TV_TV_TAG, "Handling back press: Exiting fullscreen video.");
            return videoViewTv.onBackPressed(); // DoikkiPlayer lida com saída de tela cheia
        }
        if (mChannelGridView != null && mChannelGridView.isShown()) {
            Log.d(TV_TV_TAG, "Handling back press: Hiding ChannelGridView.");
            mChannelGridView.hideChannelGrid();
            return true; // Evento consumido
        }
        Log.d(TV_TV_TAG, "Back press not handled by TvFragmentTv, allowing activity to handle.");
        return false; // Permitir que a Activity (MainTvActivity) lide com o back press
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TV_TV_TAG, "onDetach called");
        if (dataManager != null) {
            dataManager.setListener(null); // Corrigido para setListener(null)
        }
    }
}
