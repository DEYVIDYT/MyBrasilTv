package com.example.iptvplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class DeviceTypeActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "device_type_prefs";
    private static final String DEVICE_TYPE_KEY = "device_type";
    public static final String DEVICE_TYPE_MOBILE = "mobile";
    public static final String DEVICE_TYPE_TV = "tv";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verificar se o tipo de dispositivo já foi escolhido
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.contains(DEVICE_TYPE_KEY)) {
            // Se já foi escolhido, navega direto para a próxima atividade
            navigateToDownloadProgressActivity();
            return; // Impede que o restante do onCreate seja executado
        }

        // Se não foi escolhido, infla o layout para permitir a seleção
        setContentView(R.layout.activity_device_type);

        Button mobileButton = findViewById(R.id.btn_mobile);
        Button tvButton = findViewById(R.id.btn_tv);

        mobileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDeviceType(DEVICE_TYPE_MOBILE);
                navigateToDownloadProgressActivity();
            }
        });

        tvButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDeviceType(DEVICE_TYPE_TV);
                navigateToDownloadProgressActivity();
            }
        });
    }

    private void saveDeviceType(String deviceType) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(DEVICE_TYPE_KEY, deviceType);
        editor.apply();
    }

    private void navigateToDownloadProgressActivity() {
        Intent intent = new Intent(this, DownloadProgressActivity.class);
        startActivity(intent);
        finish(); // Finaliza esta atividade para que o usuário não possa voltar para ela
    }

    public static String getDeviceType(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        // Retorna null se não houver preferência salva, para que a tela de seleção apareça.
        // Ou podemos definir um padrão aqui, mas a lógica no onCreate já lida com a ausência da chave.
        return prefs.getString(DEVICE_TYPE_KEY, null); // Alterado para retornar null se não definido
    }

    public static boolean isTvMode(android.content.Context context) {
        // Esta função agora reflete corretamente se o modo TV foi explicitamente selecionado.
        return DEVICE_TYPE_TV.equals(getDeviceType(context));
    }
}

