package com.example.iptvplayer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.iptvplayer.data.Channel;
import java.util.List;
import java.util.Map;

public class EpgUpdateService extends Service {
    private static final String EPG_UPDATE_TAG = "EpgUpdateService";
    public static final String ACTION_EPG_UPDATED = "com.example.iptvplayer.EPG_UPDATED";
    public static final String EXTRA_STREAM_ID = "stream_id";
    public static final String EXTRA_PROGRAM_TITLE = "program_title";
    
    private XmltvEpgService xmltvEpgService;
    private Handler updateHandler;
    private Runnable updateRunnable;
    private List<Channel> channels;
    private static final long UPDATE_INTERVAL = 5 * 60 * 1000; // 5 minutos
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(EPG_UPDATE_TAG, "EpgUpdateService created");
        updateHandler = new Handler(Looper.getMainLooper());
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(EPG_UPDATE_TAG, "EpgUpdateService started");
        
        if (intent != null) {
            String baseUrl = intent.getStringExtra("baseUrl");
            String username = intent.getStringExtra("username");
            String password = intent.getStringExtra("password");
            
            if (baseUrl != null && username != null && password != null) {
                initializeEpgService(baseUrl, username, password);
                startPeriodicUpdates();
            }
        }
        
        return START_STICKY; // Reiniciar se o serviço for morto
    }
    
    private void initializeEpgService(String baseUrl, String username, String password) {
        xmltvEpgService = new XmltvEpgService(baseUrl, username, password);
        xmltvEpgService.setCacheManager(new CacheManager(this));
        Log.d(EPG_UPDATE_TAG, "XmltvEpgService initialized");
    }
    
    private void startPeriodicUpdates() {
        if (updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
        
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateEpgData();
                updateHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
        
        // Primeira execução imediata
        updateHandler.post(updateRunnable);
        Log.d(EPG_UPDATE_TAG, "Periodic EPG updates started");
    }
    
    private void updateEpgData() {
        if (xmltvEpgService == null) {
            Log.w(EPG_UPDATE_TAG, "XmltvEpgService not initialized");
            return;
        }
        
        Log.d(EPG_UPDATE_TAG, "Updating EPG data...");
        
        if (channels == null || channels.isEmpty()) {
            Log.w(EPG_UPDATE_TAG, "No channels available for EPG update.");
            return;
        }
        
        xmltvEpgService.fetchCurrentPrograms(channels, new XmltvEpgService.XmltvEpgCallback() {
            @Override
            public void onSuccess(Map<String, String> currentPrograms) {
                Log.d(EPG_UPDATE_TAG, "EPG update successful: " + currentPrograms.size() + " programs");
                
                // Enviar broadcast para notificar sobre atualizações
                for (Map.Entry<String, String> entry : currentPrograms.entrySet()) {
                    Intent broadcastIntent = new Intent(ACTION_EPG_UPDATED);
                    broadcastIntent.putExtra(EXTRA_STREAM_ID, entry.getKey());
                    broadcastIntent.putExtra(EXTRA_PROGRAM_TITLE, entry.getValue());
                    LocalBroadcastManager.getInstance(EpgUpdateService.this).sendBroadcast(broadcastIntent);
                }
            }
            
            @Override
            public void onFailure(String error) {
                Log.e(EPG_UPDATE_TAG, "EPG update failed: " + error);
            }
        });
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(EPG_UPDATE_TAG, "EpgUpdateService destroyed");
        
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Não é um serviço bound
    }
}

