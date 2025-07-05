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

    // Views do novo layout new_fragment_tv_tv.xml
    private RecyclerView recyclerViewCategories; // Renomeado de recyclerViewCategoriesTv
    private RecyclerView recyclerViewChannels;   // Renomeado de recyclerViewChannelsTv
    private ProgressBar channelsProgressBar;    // Novo ProgressBar para canais

    // Adapters (nomes mantidos, mas EPG removido)
    private ChannelCategoryAdapterTv categoryAdapterTv;
    private ChannelAdapterTv channelAdapterTv;
    // private EpgAdapterTv epgAdapterTv; // Removido pois EPG não está mais neste layout

    // Variáveis de estado (EPG removido)
    // private List<Channel> currentChannels = new ArrayList<>(); // Não é mais necessário aqui se o adapter gerencia sua própria lista
    // private List<EpgProgram> currentEpgPrograms = new ArrayList<>(); // Removido
    // private Map<String, String> currentCategoryMap; // Pode não ser necessário se não houver ChannelGridView

    // Componentes do player embutido removidos:
    // private FrameLayout playerContainerTv;
    // private VideoView videoViewTv;
    // private StandardVideoController videoControllerTv;
    // private ProgressBar playerProgressBarTv; // Antigo ProgressBar do player
    // private TitleView mTitleViewComponent;
    // private ChannelGridView mChannelGridView;


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
        Log.d(TV_TV_TAG, "onCreateView called - New Layout");
        // Inflar o novo layout
        View root = inflater.inflate(R.layout.new_fragment_tv_tv, container, false);

        // Inicializar views do novo layout
        recyclerViewCategories = root.findViewById(R.id.recycler_view_tv_categories);
        recyclerViewChannels = root.findViewById(R.id.recycler_view_tv_channels);
        channelsProgressBar = root.findViewById(R.id.channels_progress_bar_tv);

        Log.d(TV_TV_TAG, "Calling setupRecyclerViews() for new layout");
        setupRecyclerViews(); // Método será ajustado para os novos IDs e sem EPG

        Log.d(TV_TV_TAG, "Views initialized for new layout.");
        return root;
    }

    private void setupRecyclerViews() {
        Log.d(TV_TV_TAG, "setupRecyclerViews called for new layout");
        if (getContext() == null) {
            Log.e(TV_TV_TAG, "Context is null in setupRecyclerViews!");
            return;
        }
        // Configurar RecyclerView de Categorias
        recyclerViewCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        categoryAdapterTv = new ChannelCategoryAdapterTv(getContext(), new ArrayList<>(), this::onCategorySelected);
        recyclerViewCategories.setAdapter(categoryAdapterTv);
        recyclerViewCategories.setFocusable(true); // Manter focabilidade para D-Pad

        // Configurar RecyclerView de Canais
        recyclerViewChannels.setLayoutManager(new LinearLayoutManager(getContext()));
        channelAdapterTv = new ChannelAdapterTv(getContext(), new ArrayList<>(), this::onChannelSelected);
        recyclerViewChannels.setAdapter(channelAdapterTv);
        recyclerViewChannels.setFocusable(true); // Manter focabilidade para D-Pad
        
        // EPG RecyclerView e adapter foram removidos
        Log.d(TV_TV_TAG, "RecyclerViews setup complete for new layout.");
    }

    // Métodos relacionados ao player embutido (setupVideoPlayerComponents, onResume, onPause, onDestroyView referentes ao videoViewTv)
    // serão removidos ou ajustados drasticamente pois o player será em outra Activity.

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TV_TV_TAG, "onResume called - New Layout");
        // Não há player embutido para resumir aqui
        Log.d(TV_TV_TAG, "Calling updateUi() from onResume");
        updateUi();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TV_TV_TAG, "onPause called - New Layout");
        // Não há player embutido para pausar aqui
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TV_TV_TAG, "onDestroyView called - New Layout");
        // Não há player embutido para liberar aqui
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
        // Este método agora controlará channelsProgressBar
        Log.d(TV_TV_TAG, "showLoading (channels list): " + isLoading);
        if (channelsProgressBar != null) {
            channelsProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        // O playerProgressBarTv antigo foi removido.
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
        if (channelsProgressBar != null) {
            channelsProgressBar.setVisibility(View.VISIBLE); // Mostrar ProgressBar
        }
        recyclerViewChannels.setVisibility(View.GONE); // Esconder lista de canais enquanto carrega

        // Simular um pequeno atraso para o carregamento ou usar um Handler/Executor se a busca for demorada.
        // Por enquanto, vamos buscar diretamente.
        List<Channel> channels = dataManager.getLiveStreamsByCategory(category.id);

        if (channelsProgressBar != null) {
            channelsProgressBar.setVisibility(View.GONE); // Esconder ProgressBar
        }
        recyclerViewChannels.setVisibility(View.VISIBLE); // Mostrar lista de canais

        if (channels != null) {
            Log.d(TV_TV_TAG, "Updating channelAdapterTv with " + channels.size() + " channels for category: " + category.name);
            if (channelAdapterTv != null) {
                channelAdapterTv.updateData(channels);
                if (channels.isEmpty()) {
                    Toast.makeText(getContext(), "Nenhum canal nesta categoria.", Toast.LENGTH_SHORT).show();
                } else {
                    // Opcional: focar no primeiro canal da lista para melhor navegação na TV
                    recyclerViewChannels.requestFocus();
                }
            } else {
                Log.e(TV_TV_TAG, "channelAdapterTv is null in onCategorySelected!");
            }
            // Não selecionar automaticamente o primeiro canal para reprodução aqui.
            // O usuário deve explicitamente selecionar um canal da lista.
            // if(!channels.isEmpty()) {
            //     Log.d(TV_TV_TAG, "Selecting first channel: " + channels.get(0).getName());
            //     onChannelSelected(channels.get(0));
            // }
        } else {
            Log.w(TV_TV_TAG, "No channels found for category: " + category.name);
            if (channelAdapterTv != null) {
                channelAdapterTv.updateData(new ArrayList<>()); // Limpar lista
            }
            Toast.makeText(getContext(), "Nenhum canal encontrado para: " + category.name, Toast.LENGTH_SHORT).show();
        }
    }

    private void onChannelSelected(Channel channel) {
        Log.d(TV_TV_TAG, "onChannelSelected: " + channel.getName() + ", URL: " + channel.getStreamUrl());

        if (!isAdded() || getContext() == null) {
            Log.e(TV_TV_TAG, "onChannelSelected: Fragment not added or context is null.");
            return;
        }

        String streamUrl = channel.getStreamUrl();
        if (streamUrl == null || streamUrl.isEmpty()) {
            Log.e(TV_TV_TAG, "Stream URL is null or empty for channel: " + channel.getName());
            Toast.makeText(getContext(), "URL de stream inválida para " + channel.getName(), Toast.LENGTH_LONG).show();
            return;
        }

        // Criar um objeto Movie simples para passar para VideoPlayerActivity
        // VideoPlayerActivity espera um 'Movie' serializable com 'name' e 'videoUrl'.
        // Usaremos o nome do canal e a URL do stream. Outros campos de Movie podem ser nulos ou padrão.
        com.example.iptvplayer.data.Movie movieToPlay = new com.example.iptvplayer.data.Movie();
        movieToPlay.setName(channel.getName());
        movieToPlay.setVideoUrl(streamUrl);
        // Se Channel tivesse um ID numérico ou outros campos que Movie tem, poderíamos mapeá-los aqui.
        // Por agora, apenas nome e URL são essenciais para VideoPlayerActivity.

        Intent intent = new Intent(getActivity(), VideoPlayerActivity.class);
        intent.putExtra("movie", movieToPlay); // VideoPlayerActivity espera "movie"
        intent.putExtra("isLiveStream", true); // Indicar que é um stream ao vivo
        startActivity(intent);

        // Não há mais EPG embutido nesta tela para carregar
        // loadEpgForChannel(channel.getStreamId());
    }

    // Método loadEpgForChannel e onEpgProgramSelected podem ser removidos
    // pois o RecyclerView de EPG e seu adapter foram removidos do layout e da lógica.
    // private void loadEpgForChannel(String streamId) { ... }
    // public void onEpgProgramSelected(EpgProgram program) { ... }


    // onTvKeyDown e onTvKeyUp não são mais necessários aqui se mChannelGridView foi removido.
    // Se a navegação por D-Pad precisar de tratamento especial para os RecyclerViews,
    // isso seria feito de forma diferente, geralmente pelo foco do sistema.
    @Override
    public boolean onTvKeyDown(int keyCode, KeyEvent event) {
        // Log.d(TV_TV_TAG, "onTvKeyDown: keyCode=" + keyCode);
        // Se mChannelGridView foi removido, esta lógica não se aplica mais.
        // Retornar false para permitir que o sistema manipule.
        return false;
    }

    @Override
    public boolean onTvKeyUp(int keyCode, KeyEvent event) {
        // Log.d(TV_TV_TAG, "onTvKeyUp: keyCode=" + keyCode);
        return false;
    }

    public boolean onBackPressed() {
        Log.d(TV_TV_TAG, "onBackPressed called - New Layout");
        // Não há player embutido ou ChannelGridView para tratar aqui.
        // Deixar a Activity pai (MainTvActivity) lidar com o back press.
        return false;
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
