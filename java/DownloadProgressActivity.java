
package com.example.iptvplayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DownloadProgressActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView progressText;
    private TextView statusText;
    private Handler mainHandler;
    private int currentProgress = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_progress);

        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);
        statusText = findViewById(R.id.statusText);
        // mainHandler is no longer needed directly here as DataManager handles threading for callbacks

        // Get the singleton DataManager instance
        DataManager dataManager = MyApplication.getDataManager();
        dataManager.setListener(new DataManager.DataManagerListener() {
            @Override
            public void onProgressUpdate(DataManager.LoadState state, int percentage, String message) {
                runOnUiThread(() -> {
                    progressBar.setProgress(percentage);
                    progressText.setText("Carregando... " + percentage + "%");
                    statusText.setText(message); // Displaying the detailed message from DataManager
                });
            }

            @Override
            public void onDataLoaded() {
                runOnUiThread(() -> {
                    statusText.setText("Dados carregados com sucesso!");
                    progressBar.setProgress(100);
                    progressText.setText("Carregando... 100%");
                    // Proceed to MainActivity
                    Intent intent = new Intent(DownloadProgressActivity.this, MainActivity.class);
                    // Pass a flag or some data to MainActivity to indicate data is ready
                    intent.putExtra("DATA_LOADED_SUCCESSFULLY", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    statusText.setText("Erro: " + errorMessage);
                    // Handle error display, maybe allow retry or exit
                    // For now, just shows the error.
                    // You might want to add a button to go back or retry.
                });
            }
        });

        dataManager.startDataLoading();
    }

    // The old simulateDownloadProgress, updateProgress, setProgress, setStatus methods are removed.

    @Override
    public void onBackPressed() {
        // Impede que o usuário volte durante o download
        // Consider allowing cancellation, which would require a way to signal DataManager to stop.
        // For now, keeping it as non-cancellable by back press.
    }
}


