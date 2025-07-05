package com.example.iptvplayer;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import xyz.doikki.videoplayer.player.VideoView;
import com.example.iptvplayer.StandardVideoController;
import com.example.iptvplayer.component.CompleteView;
import com.example.iptvplayer.component.ErrorView;
import com.example.iptvplayer.component.PrepareView;
import com.example.iptvplayer.component.VodControlView;
import com.example.iptvplayer.component.LiveControlView; // Adicionado para streams ao vivo
import com.example.iptvplayer.component.TitleView;
import com.example.iptvplayer.data.Movie;
import android.widget.FrameLayout;
import android.view.ViewGroup;
import android.util.Log;

public class VideoPlayerActivity extends AppCompatActivity {

    private VideoView mVideoView;
    private static final String TAG = "VideoPlayerActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player); // You'll need to create this layout file

        mVideoView = findViewById(R.id.video_player);
        if (mVideoView == null) {
            Log.e(TAG, "VideoView not found in layout");
            finish();
            return;
        }

        StandardVideoController controller = new StandardVideoController(this);
        controller.addControlComponent(new CompleteView(this));
        controller.addControlComponent(new ErrorView(this));
        controller.addControlComponent(new PrepareView(this));
        // controller.addControlComponent(new VodControlView(this)); // Removido para adicionar condicionalmente

        TitleView titleView = new TitleView(this); // TitleView é sempre bom ter
        controller.addControlComponent(titleView);

        mVideoView.setVideoController(controller); // Definir controller antes de adicionar mais componentes pode ser melhor

        Movie movie = (Movie) getIntent().getSerializableExtra("movie");
        boolean isLiveStream = getIntent().getBooleanExtra("isLiveStream", false);

        if (isLiveStream) {
            Log.d(TAG, "Setting up for LIVE stream");
            LiveControlView liveControlView = new LiveControlView(this);
            // Se LiveControlView precisar de referência ao ChannelGridView (como no TvFragment),
            // isso não será possível aqui diretamente, pois VideoPlayerActivity é genérica.
            // Para TV ao vivo, o LiveControlView pode precisar ser adaptado ou usado como está.
            // Por agora, vamos apenas adicioná-lo.
            controller.addControlComponent(liveControlView);
        } else {
            Log.d(TAG, "Setting up for VOD stream");
            controller.addControlComponent(new VodControlView(this));
        }
        // É importante que o controller seja setado no VideoView ANTES ou DEPOIS de adicionar todos os componentes.
        // A ordem pode importar para alguns players. DoikkiPlayer geralmente permite adicionar depois.
        // Se mVideoView.setVideoController(controller) foi chamado antes, não precisa chamar de novo.

        if (movie != null) {
            String videoUrl = movie.getVideoUrl();
            String movieTitle = movie.getName();
            Log.d(TAG, "Playing: " + movieTitle + " from URL: " + videoUrl + (isLiveStream ? " (LIVE)" : " (VOD)"));
            titleView.setTitle(movieTitle); // titleView já foi adicionado ao controller
            mVideoView.setUrl(videoUrl);
            mVideoView.start();
        } else {
            Log.e(TAG, "No media data (movie/channel) received");
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoView.release();
    }

    @Override
    public void onBackPressed() {
        if (!mVideoView.onBackPressed()) {
            super.onBackPressed();
        }
    }
}


