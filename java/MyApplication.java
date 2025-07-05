package com.example.iptvplayer;

import android.app.Application;
import android.content.Context;
import android.util.Log;

// Importações para DoikkiPlayer e ExoPlayer
import xyz.doikki.videoplayer.player.VideoViewManager;
import xyz.doikki.videoplayer.player.PlayerFactory;
import xyz.doikki.videoplayer.player.PlayerConfig;
import xyz.doikki.videoplayer.exo.ExoMediaPlayerFactory;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.C; // Para C.DEFAULT_BUFFER_SEGMENT_SIZE


public class MyApplication extends Application {

    private static DataManager dataManagerInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MyApplication", "onCreate called.");

        // Configuração global do DoikkiPlayer
        VideoViewManager.setConfig(
            PlayerConfig.newBuilder()
                .setPlayerFactory(createCustomExoPlayerFactory(getApplicationContext()))
                .setReconnectCount(Integer.MAX_VALUE) // Retentativas "ilimitadas"
                // Outras configurações globais do PlayerConfig podem ser adicionadas aqui
                // .setBufferTimeout(10000) // Exemplo
                .build());
        Log.d("MyApplication", "DoikkiPlayer global config set with custom ExoPlayer factory and reconnect count.");

        // Initialize DataManager singleton
        initializeDataManager(getApplicationContext());
        // Start data loading as soon as the application starts
        if (dataManagerInstance != null) {
            dataManagerInstance.startDataLoading();
        }
    }

    private PlayerFactory createCustomExoPlayerFactory(Context context) {
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
            .setAllocator(new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS, // minBufferMs
                60000,                                     // maxBufferMs - 1 minuto
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS, // bufferForPlaybackMs
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS // bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true) // Padrão é true, mas para garantir
            .build();

        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context);

        return new ExoMediaPlayerFactory(renderersFactory, loadControl);
    }

    private static synchronized void initializeDataManager(Context context) {
        if (dataManagerInstance == null) {
            dataManagerInstance = new DataManager(context);
            Log.d("MyApplication", "DataManager initialized.");
        }
    }

    public static synchronized DataManager getDataManager(Context context) {
        // Ensure DataManager is initialized before returning
        initializeDataManager(context);
        return dataManagerInstance;
    }
}


