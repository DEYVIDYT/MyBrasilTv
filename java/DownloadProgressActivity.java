
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
        mainHandler = new Handler(Looper.getMainLooper());

        // Simula o progresso de download
        simulateDownloadProgress();
    }

    private void simulateDownloadProgress() {
        new Thread(() -> {
            try {
                // Fase 1: Conectando
                updateProgress(10, "Conectando ao servidor...");
                Thread.sleep(1000);

                // Fase 2: Baixando canais
                updateProgress(30, "Baixando lista de canais...");
                Thread.sleep(2000);

                // Fase 3: Baixando VOD
                updateProgress(60, "Baixando lista de filmes/séries...");
                Thread.sleep(2000);

                // Fase 4: Baixando EPG
                updateProgress(90, "Baixando guia de programação (EPG)...");
                Thread.sleep(1500);

                // Fase 5: Finalizando
                updateProgress(100, "Finalizando...");
                Thread.sleep(500);

                // Volta para MainActivity
                mainHandler.post(() -> {
                    Intent intent = new Intent(DownloadProgressActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateProgress(int progress, String status) {
        mainHandler.post(() -> {
            currentProgress = progress;
            progressBar.setProgress(progress);
            progressText.setText("Carregando... " + progress + "%");
            statusText.setText(status);
        });
    }

    public void setProgress(int progress, String status) {
        updateProgress(progress, status);
    }

    public void setStatus(String status) {
        mainHandler.post(() -> statusText.setText(status));
    }

    @Override
    public void onBackPressed() {
        // Impede que o usuário volte durante o download
        // Pode ser removido se quiser permitir cancelamento
    }
}


