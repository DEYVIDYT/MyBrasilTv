package com.example.iptvplayer;

import android.app.Application;
import android.util.Log;

// import com.squareup.picasso.OkHttp3Downloader; // Removido - Picasso
// import com.squareup.picasso.Picasso; // Removido - Picasso

// Importações do Glide (se necessárias para configuração global, caso contrário podem ser omitidas aqui)
// import com.bumptech.glide.Glide;
// import com.bumptech.glide.GlideBuilder;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MyApplication", "onCreate called. Glide does not require explicit global initialization here unless custom global configs are needed.");
        // O Glide geralmente se auto-configura ou é configurado via AppGlideModule.
        // Não há necessidade de chamar setSingletonInstance como no Picasso.

        // Se você precisar de configurações globais para o Glide (raro para uso básico):
        // GlideBuilder builder = new GlideBuilder();
        // builder.setLogLevel(Log.VERBOSE); // Exemplo de configuração
        // Glide.init(this, builder);
    }
}
