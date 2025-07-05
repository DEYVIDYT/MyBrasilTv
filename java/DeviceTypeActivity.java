package com.example.iptvplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
// import android.view.View; // Removido pois não é mais necessário
// import android.widget.Button; // Removido pois não é mais necessário
import androidx.appcompat.app.AppCompatActivity;

public class DeviceTypeActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "device_type_prefs";
    private static final String DEVICE_TYPE_KEY = "device_type";
    public static final String DEVICE_TYPE_MOBILE = "mobile";
    public static final String DEVICE_TYPE_TV = "tv"; // Mantido para compatibilidade com getDeviceType e isTvMode

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Salvar automaticamente o tipo de dispositivo como mobile
        saveDeviceType(DEVICE_TYPE_MOBILE);
        
        // Navegar diretamente para a DownloadProgressActivity
        navigateToDownloadProgressActivity();
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
    
    // Método mantido caso outras partes do código dependam dele,
    // mas agora sempre retornará DEVICE_TYPE_MOBILE ou o valor salvo anteriormente se houver.
    public static String getDeviceType(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        // Garante que se nada estiver salvo, retorne mobile por padrão.
        return prefs.getString(DEVICE_TYPE_KEY, DEVICE_TYPE_MOBILE);
    }
    
    // Método mantido caso outras partes do código dependam dele,
    // mas agora sempre retornará false, a menos que o valor tenha sido TV de uma versão anterior.
    public static boolean isTvMode(android.content.Context context) {
        return DEVICE_TYPE_TV.equals(getDeviceType(context));
    }
}

