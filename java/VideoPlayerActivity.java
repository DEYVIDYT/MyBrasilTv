package com.example.iptvplayer;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.example.iptvplayer.data.Movie;

public class VideoPlayerActivity extends AppCompatActivity {

    private PlayerView playerView;
    private ExoPlayer exoPlayer;
    private static final String TAG = "VideoPlayerActivity";

    private TextView customTitleTextView;
    private ImageButton backButton;
    private LinearLayout errorView;
    private TextView errorMessageTextView;
    private Button retryButton;
    private ImageButton replayButton;
    private ProgressBar loadingIndicator;

    // Bottom bar controls
    private LinearLayout bottomBarContainer;
    private ImageButton playPauseBottomButton;
    private View exoPosition; // TextView
    private View exoDuration; // TextView
    private View exoProgress; // DefaultTimeBar
    private ImageButton refreshButton;
    private ImageButton channelGridToggleButton;
    private ImageButton proportionButton;
    private ImageButton speedButton;
    // exo_fullscreen is handled by PlayerView if ID is correct

    private boolean isLiveStream;
    private Movie movie;
    private String videoUrl;
    private String movieTitle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        playerView = findViewById(R.id.exoplayer_view);
        if (playerView == null) {
            Log.e(TAG, "PlayerView not found in layout");
            finish();
            return;
        }
        // Set the custom controller layout
        playerView.setControllerLayoutId(R.layout.custom_exoplayer_controls);

        // Initialize UI elements from the custom controller
        // It's important that these IDs match those in custom_exoplayer_controls.xml
        // Top Bar
        customTitleTextView = playerView.findViewById(R.id.exo_custom_title);
        backButton = playerView.findViewById(R.id.exo_back_button);

        // Center view elements
        errorView = playerView.findViewById(R.id.exo_custom_error_view);
        errorMessageTextView = playerView.findViewById(R.id.exo_custom_error_message);
        retryButton = playerView.findViewById(R.id.exo_custom_retry_button);
        replayButton = playerView.findViewById(R.id.exo_custom_replay_button);
        loadingIndicator = playerView.findViewById(R.id.exo_buffering); // Standard ExoPlayer ID

        // Bottom Bar
        bottomBarContainer = playerView.findViewById(R.id.bottom_bar_container);
        playPauseBottomButton = playerView.findViewById(R.id.exo_play_pause_bottom);
        exoPosition = playerView.findViewById(R.id.exo_position);
        exoDuration = playerView.findViewById(R.id.exo_duration);
        exoProgress = playerView.findViewById(R.id.exo_progress); // DefaultTimeBar
        refreshButton = playerView.findViewById(R.id.exo_custom_refresh_button);
        channelGridToggleButton = playerView.findViewById(R.id.exo_custom_channel_grid_toggle_button);
        proportionButton = playerView.findViewById(R.id.exo_custom_proportion_button);
        speedButton = playerView.findViewById(R.id.exo_custom_speed_button);


        movie = (Movie) getIntent().getSerializableExtra("movie");
        isLiveStream = getIntent().getBooleanExtra("isLiveStream", false);

        if (movie != null) {
            videoUrl = movie.getVideoUrl();
            movieTitle = movie.getName();
            if (customTitleTextView != null) {
                customTitleTextView.setText(movieTitle);
            }
            Log.d(TAG, "Playing: " + movieTitle + " from URL: " + videoUrl + (isLiveStream ? " (LIVE)" : " (VOD)"));
            initializePlayer();
        } else {
            Log.e(TAG, "No media data (movie/channel) received");
            showError("No media data received.");
            if (retryButton != null) retryButton.setEnabled(false); // No point in retrying if no data
        }

        setupClickListeners();
        updateControlsVisibility();
    }

    private void initializePlayer() {
        if (exoPlayer == null) {
            exoPlayer = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(exoPlayer);
            playerView.setControllerShowTimeoutMs(3000); // Show controls for 3 seconds

            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    updateControlsVisibility();
                    switch (playbackState) {
                        case Player.STATE_BUFFERING:
                            if (loadingIndicator != null) loadingIndicator.setVisibility(View.VISIBLE);
                            if (errorView != null) errorView.setVisibility(View.GONE);
                            if (replayButton != null) replayButton.setVisibility(View.GONE);
                            break;
                        case Player.STATE_READY:
                            if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
                            if (errorView != null) errorView.setVisibility(View.GONE);
                            if (replayButton != null) replayButton.setVisibility(View.GONE);
                            exoPlayer.play(); // Start playback when ready
                            break;
                        case Player.STATE_ENDED:
                            if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
                            if (errorView != null) errorView.setVisibility(View.GONE);
                            if (replayButton != null) replayButton.setVisibility(View.VISIBLE);
                            // Optionally show all controls or specific "replay" message
                            playerView.showController();
                            break;
                        case Player.STATE_IDLE:
                            // Can also show loading or prepare for new media
                            if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
                            break;
                    }
                }

                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    updatePlayPauseButtonState(isPlaying);
                }

                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    Log.e(TAG, "Player Error: ", error);
                    if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
                    showError(error.getLocalizedMessage() != null ? error.getLocalizedMessage() : "An unknown error occurred.");
                }
            });
        }

        MediaItem mediaItem = MediaItem.fromUri(videoUrl);
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
    }

    private void updatePlayPauseButtonState(boolean isPlaying) {
        if (playPauseBottomButton != null) {
            playPauseBottomButton.setImageResource(
                isPlaying ? R.drawable.dkplayer_ic_action_pause : R.drawable.dkplayer_ic_action_play_arrow
            );
            playPauseBottomButton.setContentDescription(
                isPlaying ? getString(R.string.exo_controls_pause_description) : getString(R.string.exo_controls_play_description)
            );
        }
        // Also update large center buttons if they are used/visible
        ImageButton playButton = playerView.findViewById(R.id.exo_play);
        ImageButton pauseButton = playerView.findViewById(R.id.exo_pause);
        if (playButton != null) playButton.setVisibility(isPlaying ? View.GONE : View.VISIBLE);
        if (pauseButton != null) pauseButton.setVisibility(isPlaying ? View.VISIBLE : View.GONE);

    }


    private void setupClickListeners() {
        if (backButton != null) {
            backButton.setOnClickListener(v -> onBackPressed());
        }

        if (retryButton != null) {
            retryButton.setOnClickListener(v -> {
                if (errorView != null) errorView.setVisibility(View.GONE);
                if (loadingIndicator != null) loadingIndicator.setVisibility(View.VISIBLE);
                initializePlayer(); // Re-initialize and prepare
            });
        }

        if (replayButton != null) {
            replayButton.setOnClickListener(v -> {
                if (exoPlayer != null) {
                    exoPlayer.seekTo(0);
                    exoPlayer.play();
                    replayButton.setVisibility(View.GONE);
                }
            });
        }

        if (playPauseBottomButton != null) {
            playPauseBottomButton.setOnClickListener(v -> {
                if (exoPlayer != null) {
                    if (exoPlayer.isPlaying()) {
                        exoPlayer.pause();
                    } else {
                        exoPlayer.play();
                    }
                }
            });
        }

        // Fullscreen button is typically handled by PlayerView if ID is @id/exo_fullscreen
        // We can add explicit listener if needed or if using custom ID.

        if (speedButton != null) {
            speedButton.setOnClickListener(v -> {
                // Placeholder for speed selection logic
                Toast.makeText(this, "Speed button clicked", Toast.LENGTH_SHORT).show();
                // Example: Cycle through speeds or show a dialog
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
                // Placeholder for aspect ratio logic
                Toast.makeText(this, "Proportion button clicked", Toast.LENGTH_SHORT).show();
                // Example: Cycle through resize modes
                if (playerView != null) {
                    int currentMode = playerView.getResizeMode();
                    int newMode = (currentMode + 1) % 5; // Cycle through 0 to 4
                    playerView.setResizeMode(newMode);
                     String modeString;
                        switch (newMode) {
                            case PlayerView.RESIZE_MODE_FIT: modeString = "Fit"; break;
                            case PlayerView.RESIZE_MODE_FIXED_WIDTH: modeString = "Fixed Width"; break;
                            case PlayerView.RESIZE_MODE_FIXED_HEIGHT: modeString = "Fixed Height"; break;
                            case PlayerView.RESIZE_MODE_FILL: modeString = "Fill"; break;
                            case PlayerView.RESIZE_MODE_ZOOM: modeString = "Zoom"; break;
                            default: modeString = "Unknown";
                        }
                    Toast.makeText(this, "Aspect Ratio: " + modeString, Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> {
                 Toast.makeText(this, "Refresh clicked", Toast.LENGTH_SHORT).show();
                 if (exoPlayer != null && isLiveStream) {
                     // For live streams, re-preparing might be what's needed.
                     // Or, if it's a dynamic manifest, this might not be necessary.
                     exoPlayer.stop();
                     exoPlayer.prepare(); // Re-prepare the media source
                 }
            });
        }

        if (channelGridToggleButton != null) {
            channelGridToggleButton.setOnClickListener(v -> {
                 Toast.makeText(this, "Channel Grid Toggle Clicked", Toast.LENGTH_SHORT).show();
                 // Actual implementation would involve showing/hiding a channel grid overlay
            });
        }
    }

    private void updateControlsVisibility() {
        if (bottomBarContainer == null) return; // Not yet initialized

        boolean isVod = !isLiveStream;

        if (playPauseBottomButton != null) playPauseBottomButton.setVisibility(View.VISIBLE); // Always show play/pause

        if (exoPosition != null) exoPosition.setVisibility(isVod ? View.VISIBLE : View.GONE);
        if (exoProgress != null) exoProgress.setVisibility(isVod ? View.VISIBLE : View.GONE);
        if (exoDuration != null) exoDuration.setVisibility(isVod ? View.VISIBLE : View.GONE);

        if (refreshButton != null) refreshButton.setVisibility(isLiveStream ? View.VISIBLE : View.GONE);
        if (channelGridToggleButton != null) channelGridToggleButton.setVisibility(isLiveStream ? View.VISIBLE : View.GONE);

        // Speed and Proportion buttons are currently shown for both. Can be adjusted.
        if (speedButton != null) speedButton.setVisibility(isVod ? View.VISIBLE : View.GONE); // Typically VOD only
        if (proportionButton != null) proportionButton.setVisibility(View.VISIBLE); // Can be for both
    }


    private void showError(String message) {
        if (errorView != null) errorView.setVisibility(View.VISIBLE);
        if (errorMessageTextView != null) errorMessageTextView.setText(message);
        if (playerView != null) playerView.hideController(); // Hide controls when error shows
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Per Media3 guide, player init should be in onStart or onResume
        // if API level is 23 or lower. We are minSdk 21.
        // However, we initialize based on intent data in onCreate.
        // If player can be created earlier, this is a good place.
        // For now, our init in onCreate driven by intent data is okay.
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (exoPlayer != null) {
            exoPlayer.play(); // Resume playback
        }
        playerView.onResume(); // Required by PlayerView
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayer != null) {
            exoPlayer.pause(); // Pause playback
        }
        playerView.onPause(); // Required by PlayerView
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Per Media3 guide, release player here if API level > 23
        // For API level 23 and lower, release in onDestroy
        // We will release in onDestroy to cover all cases simply.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    @Override
    public void onBackPressed() {
        // ExoPlayer's PlayerView does not have a direct equivalent of dkplayer's mVideoView.onBackPressed()
        // for handling things like exiting custom fullscreen or floating windows.
        // Fullscreen is often handled by orientation changes or system UI visibility.
        // If playerView.isControllerFullyVisible() && playerView.hideController() works, it's one way.
        // Or, if in fullscreen, trigger exit fullscreen logic.
        // For now, a simple super.onBackPressed() or finish().

        boolean isFullScreen = playerView.isControllerFullyVisible(); // This is not a fullscreen check
        // A more robust check for fullscreen would involve checking window flags or orientation.
        // For now, assume if controller is visible, maybe we want to hide it first.
        // However, typical behavior is that back press closes the player unless in specific states.

        // Let's assume for now that exiting fullscreen is managed by the fullscreen button
        // and system back button behavior. If specific fullscreen logic is needed (e.g. orientation lock),
        // it would need to be implemented here.

        super.onBackPressed(); // Default behavior: close activity
    }

    // Gesture handling would be complex and involve a custom OnTouchListener on PlayerView.
    // This is a placeholder for where such logic would begin.
    // For now, gestures are not implemented in this step.
}


