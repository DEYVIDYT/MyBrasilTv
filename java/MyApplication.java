package com.example.iptvplayer;

import android.app.Application;
import android.util.Log;

// import com.squareup.picasso.OkHttp3Downloader; // Removido - Picasso
// import com.squareup.picasso.Picasso; // Removido - Picasso

// Importações do Glide (se necessárias para configuração global, caso contrário podem ser omitidas aqui)
// import com.bumptech.glide.Glide;
// import com.bumptech.glide.GlideBuilder;

public class MyApplication extends Application {

    private static DataManager dataManagerInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MyApplication", "onCreate called.");
        // Initialize DataManager singleton
        dataManagerInstance = new DataManager(getApplicationContext());

        // O Glide geralmente se auto-configura ou é configurado via AppGlideModule.
        // Não há necessidade de chamar setSingletonInstance como no Picasso.

        // Se você precisar de configurações globais para o Glide (raro para uso básico):
        // GlideBuilder builder = new GlideBuilder();
        // builder.setLogLevel(Log.VERBOSE); // Exemplo de configuração
        // Glide.init(this, builder);
    }

    public static synchronized DataManager getDataManager() {
        if (dataManagerInstance == null) {
            // This should ideally not happen if Application's onCreate is correctly called first
            // and dataManagerInstance is initialized there.
            // However, as a fallback, though it might lack proper context if called too early.
            // Consider throwing an IllegalStateException if context is critical and it's null.
            Log.w("MyApplication", "DataManager instance was null, re-initializing (fallback). This might indicate an issue if context is needed before Application onCreate.");
            // dataManagerInstance = new DataManager(getApplicationContext()); // This line is problematic as getApplicationContext() might not be available in a static context if called before onCreate.
            // For a true singleton initialized in onCreate, this path should not be hit.
            // If it must be robust against being called before onCreate, context needs to be passed or handled differently.
            // For now, assuming it's always called after Application.onCreate completes.
            throw new IllegalStateException("DataManager not initialized. Ensure MyApplication.onCreate has completed.");
        }
        return dataManagerInstance;
    }
}
