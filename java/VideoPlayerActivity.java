package com.example.iptvplayer;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import xyz.doikki.videoplayer.player.VideoView;
import com.example.iptvplayer.StandardVideoController;
import com.example.iptvplayer.component.CompleteView;
import com.example.iptvplayer.component.ErrorView;
import com.example.iptvplayer.component.PrepareView;
import com.example.iptvplayer.component.VodControlView;
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
        controller.addControlComponent(new VodControlView(this));

        TitleView titleView = new TitleView(this);
        controller.addControlComponent(titleView);

        mVideoView.setVideoController(controller);

        Movie movie = (Movie) getIntent().getSerializableExtra("movie");
        if (movie != null) {
            String videoUrl = movie.getVideoUrl();
            String movieTitle = movie.getName();
            Log.d(TAG, "Playing movie: " + movieTitle + " from URL: " + videoUrl);
            titleView.setTitle(movieTitle);
            mVideoView.setUrl(videoUrl);
            mVideoView.start();
        } else {
            Log.e(TAG, "No movie data received");
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


