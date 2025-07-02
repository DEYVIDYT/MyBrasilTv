package com.example.iptvplayer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class NotificationHelper {

    private static final String CHANNEL_ID = "iptv_progress_channel";
    private static final String CHANNEL_NAME = "IPTV Progress Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for IPTV data loading progress.";
    private static final int NOTIFICATION_ID = 1;

    private Context context;
    private NotificationManager notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(CHANNEL_DESCRIPTION);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void showProgressNotification(String title, String message, int progress, int maxProgress, boolean indeterminate) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        if (indeterminate) {
            builder.setProgress(0, 0, true);
        } else {
            builder.setProgress(maxProgress, progress, false);
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public void updateProgressNotification(String title, String message, int progress, int maxProgress) {
        showProgressNotification(title, message, progress, maxProgress, false);
    }

    public void showCompletionNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(false)
                .setAutoCancel(true);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }
}


