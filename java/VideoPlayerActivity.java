package com.example.iptvplayer;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem; // Corrected import for ExoPlayer v2
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;


import com.example.iptvplayer.data.Movie;

public class VideoPlayerActivity extends AppCompatActivity {

    private PlayerView playerView;
    private SimpleExoPlayer exoPlayer; // Use SimpleExoPlayer for v2
    private static final String TAG = "VideoPlayerActivity";

    private TextView customTitleTextView;
    private ImageButton backButton;
    private LinearLayout errorView;
    private TextView errorMessageTextView;
    private Button retryButton;
    private ImageButton replayButton;
    // ProgressBar is part of PlayerView's controller (exo_buffering)

    // Bottom bar controls - references will be obtained from playerView.findViewById()
    private ImageButton playPauseBottomButton;
    private View exoPositionTextView; // Reference to exo_position in controller
    private View exoDurationTextView; // Reference to exo_duration in controller
    private View exoProgressTimeBar;  // Reference to exo_progress in controller
    private ImageButton refreshButton;
    private ImageButton channelGridToggleButton; // Not typically in a generic VideoPlayerActivity, but was in custom layout
    private ImageButton proportionButton;
    private ImageButton speedButton;

    private boolean isLiveStream;
    private Movie movie;
    private String videoUrl;
    private String movieTitle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        playerView = findViewById(R.id.exoplayer_view_v2); // ID from activity_video_player.xml
        if (playerView == null) {
            Log.e(TAG, "PlayerView (exoplayer_view_v2) not found in layout");
            finish();
            return;
        }
        // Set the custom controller layout for ExoPlayer v2
        playerView.setControllerLayoutId(R.layout.custom_exoplayer2_controls);

        // Initialize UI elements from the custom controller
        customTitleTextView = playerView.findViewById(R.id.exo_custom_title_v2);
        backButton = playerView.findViewById(R.id.exo_custom_back_button_v2);
        errorView = playerView.findViewById(R.id.exo_custom_error_view_v2);
        errorMessageTextView = playerView.findViewById(R.id.exo_custom_error_message_v2);
        retryButton = playerView.findViewById(R.id.exo_custom_retry_button_v2);
        replayButton = playerView.findViewById(R.id.exo_custom_replay_button_v2);

        // Bottom bar controls from custom_exoplayer2_controls.xml
        playPauseBottomButton = playerView.findViewById(R.id.exo_play_pause_bottom_v2);
        exoPositionTextView = playerView.findViewById(R.id.exo_position);
        exoDurationTextView = playerView.findViewById(R.id.exo_duration);
        exoProgressTimeBar = playerView.findViewById(R.id.exo_progress);
        refreshButton = playerView.findViewById(R.id.exo_custom_refresh_button_v2);
        channelGridToggleButton = playerView.findViewById(R.id.exo_custom_channel_grid_toggle_button_v2);
        proportionButton = playerView.findViewById(R.id.exo_custom_proportion_button_v2);
        speedButton = playerView.findViewById(R.id.exo_custom_speed_button_v2);
        // Fullscreen button (@id/exo_fullscreen) is part of the controller, handled by PlayerView

        movie = (Movie) getIntent().getSerializableExtra("movie");
        isLiveStream = getIntent().getBooleanExtra("isLiveStream", false);

        if (movie != null) {
            videoUrl = movie.getVideoUrl();
            movieTitle = movie.getName();
            if (customTitleTextView != null) {
                customTitleTextView.setText(movieTitle);
            }
            Log.d(TAG, "Playing: " + movieTitle + " from URL: " + videoUrl + (isLiveStream ? " (LIVE)" : " (VOD)"));
            // Player initialization will be in onStart / onResume for robustness
        } else {
            Log.e(TAG, "No media data (movie/channel) received");
            showError("No media data received.");
            if (retryButton != null) retryButton.setEnabled(false);
        }

        setupClickListeners();
        updateControlsVisibilityForV2(); // Initial visibility setup
    }

    private void initializePlayer() {
        if (exoPlayer == null) {
            exoPlayer = new SimpleExoPlayer.Builder(this).build();
            playerView.setPlayer(exoPlayer);
            playerView.setControllerShowTimeoutMs(3000);

            exoPlayer.addListener(new Player.EventListener() { // Use Player.EventListener for ExoPlayer v2
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    updateControlsVisibilityForV2();
                    View bufferingView = playerView.findViewById(R.id.exo_buffering); // Standard ID in controller

                    switch (playbackState) {
                        case Player.STATE_BUFFERING:
                            if (bufferingView != null) bufferingView.setVisibility(View.VISIBLE);
                            if (errorView != null) errorView.setVisibility(View.GONE);
                            if (replayButton != null) replayButton.setVisibility(View.GONE);
                            break;
                        case Player.STATE_READY:
                            if (bufferingView != null) bufferingView.setVisibility(View.GONE);
                            if (errorView != null) errorView.setVisibility(View.GONE);
                            if (replayButton != null) replayButton.setVisibility(View.GONE);
                            // exoPlayer.setPlayWhenReady(true); // Usually set before prepare or after setMediaSource
                            break;
                        case Player.STATE_ENDED:
                            if (bufferingView != null) bufferingView.setVisibility(View.GONE);
                            if (errorView != null) errorView.setVisibility(View.GONE);
                            if (replayButton != null) replayButton.setVisibility(View.VISIBLE);
                            playerView.showController();
                            break;
                        case Player.STATE_IDLE:
                            if (bufferingView != null) bufferingView.setVisibility(View.GONE);
                            break;
                    }
                }

                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    updatePlayPauseButtonStateV2(isPlaying);
                }

                @Override
                public void onPlayerError(@NonNull ExoPlaybackException error) { // ExoPlaybackException for v2
                    Log.e(TAG, "Player Error: ", error);
                    View bufferingView = playerView.findViewById(R.id.exo_buffering);
                    if (bufferingView != null) bufferingView.setVisibility(View.GONE);
                    showError(error.getLocalizedMessage() != null ? error.getLocalizedMessage() : "An unknown error occurred.");
                }
            });
        }

        if (videoUrl == null || videoUrl.isEmpty()) {
            Log.e(TAG, "Video URL is null or empty, cannot prepare player.");
            showError("Invalid video URL.");
            return;
        }

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, getPackageName()));
        MediaSource mediaSource;
        Uri videoUri = Uri.parse(videoUrl);

        // Basic inference for HLS, otherwise assume Progressive. More types can be added.
        if (videoUrl.toLowerCase().endsWith(".m3u8")) {
            mediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(videoUri));
        } else {
            mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(videoUri));
        }

        exoPlayer.setMediaSource(mediaSource); // Use setMediaSource for v2
        exoPlayer.prepare();
        exoPlayer.setPlayWhenReady(true); // Start playback when ready
    }

    private void updatePlayPauseButtonStateV2(boolean isPlaying) {
        // Update central play/pause buttons (if used directly, StyledPlayerControlView often handles this)
        ImageButton playButton = playerView.findViewById(R.id.exo_play); // Standard ID for play
        ImageButton pauseButton = playerView.findViewById(R.id.exo_pause); // Standard ID for pause

        if (playButton != null) playButton.setVisibility(isPlaying ? View.GONE : View.VISIBLE);
        if (pauseButton != null) pauseButton.setVisibility(isPlaying ? View.VISIBLE : View.GONE);

        // Update bottom bar play/pause button
        if (playPauseBottomButton != null) {
            playPauseBottomButton.setImageResource(
                isPlaying ? R.drawable.dkplayer_ic_action_pause : R.drawable.dkplayer_ic_action_play_arrow
            );
            // For ExoPlayer v2, content descriptions are often handled by the StyledPlayerControlView itself
            // if using standard IDs. If setting manually:
            // playPauseBottomButton.setContentDescription(isPlaying ? "Pause" : "Play");
        }
    }

    private void setupClickListeners() {
        if (backButton != null) {
            backButton.setOnClickListener(v -> onBackPressed());
        }

        if (retryButton != null) {
            retryButton.setOnClickListener(v -> {
                if (errorView != null) errorView.setVisibility(View.GONE);
                View bufferingView = playerView.findViewById(R.id.exo_buffering);
                if (bufferingView != null) bufferingView.setVisibility(View.VISIBLE);
                initializePlayer();
            });
        }

        if (replayButton != null) {
            replayButton.setOnClickListener(v -> {
                if (exoPlayer != null) {
                    exoPlayer.seekTo(0);
                    exoPlayer.setPlayWhenReady(true);
                    replayButton.setVisibility(View.GONE);
                }
            });
        }

        if (playPauseBottomButton != null) {
            playPauseBottomButton.setOnClickListener(v -> {
                if (exoPlayer != null) {
                    exoPlayer.setPlayWhenReady(!exoPlayer.getPlayWhenReady());
                }
            });
        }

        if (speedButton != null) {
            speedButton.setOnClickListener(v -> {
                Toast.makeText(this, "Speed button clicked", Toast.LENGTH_SHORT).show();
                if (exoPlayer != null) {
                    float currentSpeed = exoPlayer.getPlaybackParameters().speed;
                    float newSpeed = (currentSpeed == 1.0f) ? 1.5f : (currentSpeed == 1.5f) ? 2.0f : (currentSpeed == 2.0f) ? 0.5f : 1.0f;
                    exoPlayer.setPlaybackParameters(new PlaybackParameters(newSpeed));
                    Toast.makeText(this, "Speed: " + newSpeed + "x", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (proportionButton != null) {
            proportionButton.setOnClickListener(v -> {
                Toast.makeText(this, "Proportion button clicked", Toast.LENGTH_SHORT).show();
                if (playerView != null) {
                    int currentMode = playerView.getResizeMode();
                    // Cycle through AspectRatioFrameLayout resize modes
                    int newMode = (currentMode + 1) % 5; // ExoPlayer v2 has 5 modes (0-4 typically)
                    playerView.setResizeMode(newMode);
                     String modeString;
                        switch (newMode) {
                            case com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT: modeString = "Fit"; break;
                            case com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH: modeString = "Fixed Width"; break;
                            case com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT: modeString = "Fixed Height"; break;
                            case com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL: modeString = "Fill"; break;
                            case com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM: modeString = "Zoom"; break;
                            default: modeString = "Unknown";
                        }
                    Toast.makeText(this, "Aspect Ratio: " + modeString, Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> {
                 Toast.makeText(this, "Refresh clicked", Toast.LENGTH_SHORT).show();
                 if (exoPlayer != null && isLiveStream && videoUrl != null) {
                     exoPlayer.stop(); // Stop before re-preparing
                     initializePlayer(); // Re-initialize the player with the same URL
                 }
            });
        }

        if (channelGridToggleButton != null) { // This button might be out of place for a generic player
            channelGridToggleButton.setOnClickListener(v -> {
                 Toast.makeText(this, "Channel Grid Toggle Clicked", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void updateControlsVisibilityForV2() {
        if (playerView == null) return;

        boolean isVod = !isLiveStream;

        if (playPauseBottomButton != null) playPauseBottomButton.setVisibility(View.VISIBLE);

        if (exoPositionTextView != null) exoPositionTextView.setVisibility(isVod ? View.VISIBLE : View.GONE);
        if (exoProgressTimeBar != null) exoProgressTimeBar.setVisibility(isVod ? View.VISIBLE : View.GONE);
        if (exoDurationTextView != null) exoDurationTextView.setVisibility(isVod ? View.VISIBLE : View.GONE);

        if (refreshButton != null) refreshButton.setVisibility(isLiveStream ? View.VISIBLE : View.GONE);
        if (channelGridToggleButton != null) channelGridToggleButton.setVisibility(isLiveStream ? View.GONE : View.GONE); // Usually not for generic player

        if (speedButton != null) speedButton.setVisibility(isVod ? View.VISIBLE : View.GONE);
        if (proportionButton != null) proportionButton.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        if (errorView != null) errorView.setVisibility(View.VISIBLE);
        if (errorMessageTextView != null) errorMessageTextView.setText(message);
        if (playerView != null) playerView.hideController();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) { // Initialize player in onStart for API > 23
            if (videoUrl != null && !videoUrl.isEmpty()) {
                initializePlayer();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ((Util.SDK_INT <= 23 || exoPlayer == null)) { // Initialize player in onResume for API <= 23 or if null
             if (videoUrl != null && !videoUrl.isEmpty()) {
                initializePlayer();
            }
        }
        if (exoPlayer != null) {
             exoPlayer.setPlayWhenReady(true); // Resume playback if it was playing
        }
        // playerView.onResume(); // Not explicitly needed for ExoPlayer v2 PlayerView
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(false); // Pause playback
        }
        // playerView.onPause(); // Not explicitly needed for ExoPlayer v2 PlayerView
        if (Util.SDK_INT <= 23) { // Release player in onPause for API <= 23
            releasePlayer();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) { // Release player in onStop for API > 23
            releasePlayer();
        }
    }

    private void releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    @Override
    protected void onDestroy() { // Fallback release, though onStop/onPause should handle it
        super.onDestroy();
        releasePlayer();
    }


    @Override
    public void onBackPressed() {
        // ExoPlayer v2 PlayerView doesn't have a direct .onBackPressed() to handle fullscreen.
        // Fullscreen is typically managed by activity orientation or System UI flags.
        // If a custom fullscreen toggle is implemented (e.g., changing orientation),
        // that logic should be reversed here.
        // For now, default behavior.
        super.onBackPressed();
    }
}


