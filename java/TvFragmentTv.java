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

    // Views do layout fragment_tv_tv.xml (focado no player)
    private FrameLayout playerContainerTv;
    private ProgressBar playerProgressBarTv;

    // Componentes do Player Embutido
    private VideoView videoViewTv;
    private StandardVideoController videoControllerTv;
    private TitleView mTitleViewComponent;
    private ChannelGridView mChannelGridView;
    private SideNavToggleListener sideNavToggleListener; // Listener para interagir com a Sidenav da Activity - Tipo atualizado

    // As seguintes variáveis foram removidas pois as RecyclerViews laterais e seus adaptadores não são mais usados:
    // private RecyclerView recyclerViewCategoriesTv;
    // private RecyclerView recyclerViewChannelsTv;
    // private RecyclerView recyclerViewEpgTv;
    // private ChannelCategoryAdapterTv categoryAdapterTv;
    // private ChannelAdapterTv channelAdapterTv;
    // private EpgAdapterTv epgAdapterTv;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d(TV_TV_TAG, "onAttach called");
        dataManager = MyApplication.getDataManager(context);
        if (dataManager == null) {
            Log.e(TV_TV_TAG, "DataManager is null in onAttach!");
            return;
        }
        dataManager.setListener(this);
    }

    // A primeira ocorrência de onCreateView foi removida.
    // A versão correta, com listeners de foco, está mais abaixo.

    // setupRecyclerViews() foi removido.
    // loadLiveCategories() foi removido.
    // onCategorySelected(XtreamApiService.CategoryInfo category) foi removido.
    // loadEpgForChannel(String streamId) foi removido.
    // onEpgProgramSelected(EpgProgram program) foi removido.

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
        if (sideNavToggleListener != null) { // Passar o listener da Activity para a ChannelGridView
            mChannelGridView.setSideNavToggleListener(sideNavToggleListener);
            Log.d(TV_TV_TAG, "SideNavToggleListener passed to ChannelGridView.");
        } else {
            Log.w(TV_TV_TAG, "SideNavToggleListener is null in TvFragmentTv, cannot pass to ChannelGridView.");
        }

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
    // A primeira ocorrência de onResume foi removida.
    // A versão correta, com lógica de foco, está mais abaixo.

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
            showLoading(false); // Esconder loading se o DataManager for nulo por algum motivo
            return;
        }

        if (dataManager.isDataFullyLoaded()) {
            Log.d(TV_TV_TAG, "Data is fully loaded for Live TV.");
            showLoading(false); // Esconder o ProgressBar geral do fragmento

            List<Channel> liveStreams = dataManager.getLiveStreams();
            if (liveStreams != null && !liveStreams.isEmpty()) {
                // Se o player ainda não estiver tocando ou se nenhum canal foi selecionado ainda
                if (videoViewTv != null && videoViewTv.getCurrentPlayState() == VideoView.STATE_IDLE) {
                     Log.d(TV_TV_TAG, "Data loaded. Auto-selecting first channel: " + liveStreams.get(0).getName());
                    onChannelSelected(liveStreams.get(0));
                    // Após iniciar o primeiro canal, garantir que o fragmento (ou seu player) tenha foco
                    // se a Sidenav não estiver visível.
                    if (getView() != null) {
                        if (sideNavToggleListener == null || !sideNavToggleListener.isSideNavVisible()) {
                            Log.i(TV_TV_TAG, "Requesting focus for fragment's root view in updateUi after starting first channel (Sidenav hidden or null).");
                            getView().requestFocus();
                        } else {
                            Log.d(TV_TV_TAG, "Sidenav is visible, not forcing focus in updateUi after starting channel.");
                        }
                    }
                } else {
                    Log.d(TV_TV_TAG, "Player already active or no VideoView, skipping auto-selection of first channel.");
                     // Mesmo se não for auto-selecionar, garantir foco se a sidenav estiver escondida.
                    if (getView() != null && (sideNavToggleListener == null || !sideNavToggleListener.isSideNavVisible())) {
                        Log.d(TV_TV_TAG, "Requesting focus for fragment's root view in updateUi (player active, Sidenav hidden or null).");
                        getView().requestFocus();
                    }
                }
            } else {
                Log.w(TV_TV_TAG, "Live streams are null or empty after data load.");
                // Mostrar alguma mensagem para o usuário, se apropriado
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Nenhum canal ao vivo disponível.", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            Log.d(TV_TV_TAG, "Data not loaded for Live TV. Displaying loading indicator.");
            showLoading(true); // Mostrar o ProgressBar geral do fragmento
            if (!dataManager.isLoading()) {
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
        // Ajuste: Fazer este método controlar o playerProgressBarTv para o carregamento inicial de dados
        if (playerProgressBarTv != null) {
            playerProgressBarTv.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    // O método loadLiveCategories() foi removido.
    // O método onCategorySelected(XtreamApiService.CategoryInfo category) foi removido.

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

        // A chamada para loadEpgForChannel(channel.getStreamId()) foi removida
        // pois a lógica de EPG lateral foi removida.
        // Se o EPG for exibido, será como parte da ChannelGridView ou um componente similar.
    }

    // O método loadEpgForChannel(String streamId) foi removido.
    // O método onEpgProgramSelected(EpgProgram program) foi removido.

    public void setSideNavToggleListener(SideNavToggleListener listener) {
        this.sideNavToggleListener = listener;
        Log.d(TV_TV_TAG, "setSideNavToggleListener called. Listener is " + (listener == null ? "null" : "not null"));
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TV_TV_TAG, "onResume called");
        if (videoViewTv != null) {
            videoViewTv.resume();
        }
        // Garantir que a view raiz do fragmento seja focável e tente obter foco
        if (getView() != null) {
            getView().setFocusable(true);
            getView().setFocusableInTouchMode(true);
            // Não chamar requestFocus() aqui diretamente sempre, pois pode roubar foco da Sidenav
            // se ela estiver visível e o usuário estiver interagindo com ela.
            // O foco será gerenciado mais ativamente em updateUi ou quando a Sidenav for escondida.
            Log.d(TV_TV_TAG, "View focusability set in onResume.");
        }

        // Se os dados já estão carregados e o player está ocioso, updateUi pode iniciar um canal
        // e então tentar pegar o foco.
        if (dataManager != null && dataManager.isDataFullyLoaded() &&
            (videoViewTv == null || videoViewTv.getCurrentPlayState() == VideoView.STATE_IDLE)) {
             Log.d(TV_TV_TAG, "Calling updateUi() from onResume as data is loaded and player is idle.");
             updateUi();
        } else if (dataManager != null && !dataManager.isDataFullyLoaded() && !dataManager.isLoading()) {
            // Se os dados não estão carregados e o DataManager não está carregando, inicie.
            Log.d(TV_TV_TAG, "Data not loaded and not loading, calling updateUi to trigger load in onResume.");
            updateUi();
        }
        // Se o fragmento se torna visível e a Sidenav NÃO está, ele deve tentar pegar o foco.
        // Esta lógica pode ser mais bem colocada quando a Sidenav é escondida pela Activity.
         if (sideNavToggleListener != null && !sideNavToggleListener.isSideNavVisible() && getView() != null) {
            Log.d(TV_TV_TAG, "Sidenav is hidden, fragment requesting focus in onResume.");
            getView().requestFocus();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TV_TV_TAG, "onCreateView called - Inflating fullscreen player layout");
        View root = inflater.inflate(R.layout.fragment_tv_tv, container, false);

        playerContainerTv = root.findViewById(R.id.tv_player_container_tv);
        playerProgressBarTv = root.findViewById(R.id.tv_player_progress_bar_tv);

        initializePlayerView();

        // Listener de Foco para o container principal do fragmento
        root.setOnFocusChangeListener((v, hasFocus) -> {
            Log.d(TV_TV_TAG, "Root view focus changed: " + hasFocus);
        });
        if (playerContainerTv != null) {
            playerContainerTv.setOnFocusChangeListener((v, hasFocus) -> {
                Log.d(TV_TV_TAG, "playerContainerTv focus changed: " + hasFocus);
            });
        }


        Log.d(TV_TV_TAG, "Views initialized for fullscreen player layout.");
        return root;
    }

    // onTvKeyDown e onTvKeyUp são mantidos para interação com ChannelGridView
    // Se a navegação por D-Pad precisar de tratamento especial para os RecyclerViews,
    // isso seria feito de forma diferente, geralmente pelo foco do sistema.
    @Override
    public boolean onTvKeyDown(int keyCode, KeyEvent event) {
        Log.i(TV_TV_TAG, "onTvKeyDown: keyCode=" + KeyEvent.keyCodeToString(keyCode) + ", event: " + event);
        View focusedView = getActivity() != null ? getActivity().getCurrentFocus() : null;
        Log.d(TV_TV_TAG, "Current focused view: " + (focusedView != null ? focusedView.getClass().getSimpleName() : "null"));


        // Interação com a Sidenav da Activity ao pressionar D-Pad Direita (se a grade de canais não estiver visível)
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            Log.d(TV_TV_TAG, "DPAD_RIGHT pressed.");
            Log.d(TV_TV_TAG, "mChannelGridView is " + (mChannelGridView == null ? "null" : (mChannelGridView.isChannelGridVisible() ? "visible" : "hidden")));
            if (mChannelGridView == null || !mChannelGridView.isChannelGridVisible()) {
                Log.d(TV_TV_TAG, "SideNavToggleListener is " + (sideNavToggleListener == null ? "null" : "not null"));
                if (sideNavToggleListener != null) {
                    Log.d(TV_TV_TAG, "Sidenav is currently " + (sideNavToggleListener.isSideNavVisible() ? "visible" : "hidden"));
                    if (sideNavToggleListener.isSideNavVisible()) {
                        Log.i(TV_TV_TAG, "DPAD_RIGHT: Requesting hide Sidenav from Activity.");
                        sideNavToggleListener.requestHideSideNav();
                        return true; // Evento consumido
                    }
                }
            }
        }
        // A lógica para KEYCODE_DPAD_LEFT (para mostrar a Sidenav a partir da ChannelGridView)
        // será implementada no Passo 5, dentro da ChannelGridView.java

        // Interação com a ChannelGridView (grade de canais do player) ao pressionar D-Pad Center/OK
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_BUTTON_SELECT) {
            Log.d(TV_TV_TAG, "DPAD_CENTER or BUTTON_SELECT pressed.");
            Log.d(TV_TV_TAG, "mChannelGridView is " + (mChannelGridView == null ? "null" : "not null") + ", videoViewTv is " + (videoViewTv == null ? "null" : "not null"));
            if (mChannelGridView != null && videoViewTv != null) {
                if (mChannelGridView.isChannelGridVisible()) {
                    Log.i(TV_TV_TAG, "Hiding ChannelGridView via DPAD_CENTER");
                    mChannelGridView.hideChannelGrid();
                } else {
                    Log.i(TV_TV_TAG, "Showing ChannelGridView via DPAD_CENTER");
                    if (dataManager != null && dataManager.getLiveStreams() != null && dataManager.getLiveCategoriesMap() != null) {
                        Log.d(TV_TV_TAG, "Populating ChannelGridView with data.");
                        mChannelGridView.setChannelsData(dataManager.getLiveStreams(), dataManager.getLiveCategoriesMap());
                    } else {
                        Log.w(TV_TV_TAG, "Data for ChannelGridView is not ready. Grid might be empty or show old data.");
                        mChannelGridView.setChannelsData(new ArrayList<>(), new java.util.HashMap<>());
                    }
                    mChannelGridView.showChannelGrid();
                }
                return true;
            } else {
                Log.w(TV_TV_TAG, "ChannelGridView or VideoView is null. Cannot toggle grid.");
            }
        }
        Log.d(TV_TV_TAG, "onTvKeyDown: Event not consumed by TvFragmentTv for keyCode " + KeyEvent.keyCodeToString(keyCode));
        return false;
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
