package com.example.iptvplayer;

import android.content.Context;

import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.SimpleExoPlayer; // androidx.media3.exoplayer.SimpleExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory; // Para Media3
// import com.google.android.exoplayer2.DefaultLoadControl; // Antigo ExoPlayer
// import com.google.android.exoplayer2.SimpleExoPlayer; // Antigo ExoPlayer

import xyz.doikki.videoplayer.exo.ExoMediaPlayer; // Wrapper do DKPlayer para ExoPlayer
import xyz.doikki.videoplayer.exo.ExoPlayerFactory; // Fábrica base do DKPlayer
import xyz.doikki.videoplayer.player.AbstractPlayer;

public class CustomExoPlayerFactory extends ExoPlayerFactory {

    // Valores de buffer agressivos (em milissegundos)
    private static final int MIN_BUFFER_MS = 60 * 1000; // 60 segundos
    private static final int MAX_BUFFER_MS = 120 * 1000; // 120 segundos
    private static final int BUFFER_FOR_PLAYBACK_MS = 15 * 1000; // 15 segundos
    private static final int BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 20 * 1000; // 20 segundos

    @Override
    public AbstractPlayer create(Context context) {
        // Configurar o LoadControl com buffers aumentados
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        MIN_BUFFER_MS,
                        MAX_BUFFER_MS,
                        BUFFER_FOR_PLAYBACK_MS,
                        BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                )
                // .setAllocator(new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE)) // Opcional, geralmente o padrão é bom
                // .setTargetBufferBytes(DefaultLoadControl.DEFAULT_TARGET_BUFFER_BYTES) // Opcional
                // .setPrioritizeTimeOverSizeThresholds(DefaultLoadControl.DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS) // Opcional
                .build();

        // Criar a instância do SimpleExoPlayer com o LoadControl customizado
        // Usando Media3
        SimpleExoPlayer exoPlayer = new SimpleExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                // Para Media3, MediaSourceFactory é configurada aqui se necessário, mas para HLS/Dash
                // o DefaultMediaSourceFactory geralmente já lida bem com URLs diretas.
                // Se precisar de configurações específicas de DataSource (como RetryPolicy),
                // seria aqui através de uma custom MediaSourceFactory.
                .build();

        // Retornar o wrapper do DKVideoPlayer para o ExoPlayer
        return new ExoMediaPlayer(exoPlayer);
    }
}
