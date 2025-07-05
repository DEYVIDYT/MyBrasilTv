package com.example.iptvplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DownloadService extends Service {

    private static final String TAG = "DownloadService";
    public static final String ACTION_DOWNLOAD_COMPLETE = "com.example.iptvplayer.DOWNLOAD_COMPLETE";
    public static final String EXTRA_FILE_PATH = "extra_file_path";
    public static final String EXTRA_DOWNLOAD_URL = "extra_download_url";

    private static final int NOTIFICATION_ID = 1;
    private static final String NOTIFICATION_CHANNEL_ID = "download_channel";

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    private final OkHttpClient client = new OkHttpClient();

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String url = intent.getStringExtra(EXTRA_DOWNLOAD_URL);
        if (url == null) {
            Log.e(TAG, "No URL provided to DownloadService.");
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, createInitialNotification());

        new Thread(() -> downloadFile(url)).start();

        return START_STICKY;
    }

    private void downloadFile(String url) {
        File outputFile = new File(getCacheDir(), "iptv_list.m3u");
        Request request = new Request.Builder().url(url).build();

        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            ResponseBody body = response.body();
            if (body == null) throw new IOException("Response body is null");

            long fileLength = body.contentLength();
            try (InputStream inputStream = body.byteStream();
                 OutputStream outputStream = new FileOutputStream(outputFile)) {

                byte[] data = new byte[4096];
                long total = 0;
                int count;
                int lastProgress = 0;

                while ((count = inputStream.read(data)) != -1) {
                    total += count;
                    outputStream.write(data, 0, count);

                    if (fileLength > 0) {
                        int progress = (int) ((total * 100) / fileLength);
                        if (progress > lastProgress) {
                            updateNotification(progress);
                            lastProgress = progress;
                        }
                    }
                }
            }

            onDownloadComplete(outputFile.getAbsolutePath());
            saveDownloadTimestamp();

        } catch (IOException e) {
            Log.e(TAG, "Download failed", e);
            onDownloadFailed();
        } finally {
            stopSelf();
        }
    }

    private void saveDownloadTimestamp() {
        long currentTime = System.currentTimeMillis();
        getSharedPreferences("IPTV_PREFS", MODE_PRIVATE).edit()
                .putLong("last_download_timestamp", currentTime)
                .apply();
        Log.d(TAG, "Download timestamp saved: " + currentTime);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Download Channel",
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createInitialNotification() {
        notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Downloading IPTV List")
                .setContentText("Download in progress")
                .setSmallIcon(R.drawable.ic_dashboard_black_24dp) // Using an existing icon
                .setOngoing(true)
                .setProgress(100, 0, false);
        return notificationBuilder.build();
    }

    private void updateNotification(int progress) {
        notificationBuilder.setProgress(100, progress, false)
                .setContentText(progress + "%");
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void onDownloadComplete(String filePath) {
        notificationBuilder.setContentText("Download complete")
                .setProgress(0, 0, false)
                .setOngoing(false);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());

        Intent intent = new Intent(ACTION_DOWNLOAD_COMPLETE);
        intent.putExtra(EXTRA_FILE_PATH, filePath);
        sendBroadcast(intent);
    }

    private void onDownloadFailed() {
        notificationBuilder.setContentText("Download failed")
                .setProgress(0, 0, false)
                .setOngoing(false);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
