package com.example.iptvplayer;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class MyApplication extends Application {

    private static DataManager dataManagerInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MyApplication", "onCreate called.");
        // Initialize DataManager singleton
        initializeDataManager(getApplicationContext());
        // Start data loading as soon as the application starts
        if (dataManagerInstance != null) {
            dataManagerInstance.startDataLoading();
        }
    }

    private static synchronized void initializeDataManager(Context context) {
        if (dataManagerInstance == null) {
            dataManagerInstance = new DataManager(context);
            Log.d("MyApplication", "DataManager initialized.");
        }
    }

    public static synchronized DataManager getDataManager(Context context) {
        // Ensure DataManager is initialized before returning
        initializeDataManager(context);
        return dataManagerInstance;
    }
}


