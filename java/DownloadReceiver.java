
package com.example.iptvplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DownloadReceiver extends BroadcastReceiver {
    private final TvFragment tvFragment;

    public DownloadReceiver(TvFragment tvFragment) {
        this.tvFragment = tvFragment;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DownloadService.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
            String filePath = intent.getStringExtra(DownloadService.EXTRA_FILE_PATH);
            if (filePath != null) {
                tvFragment.parseM3uFile(filePath);
            }
        }
    }
}
