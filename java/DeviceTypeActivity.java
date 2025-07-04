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
        
        // Verificar se o tipo de dispositivo já foi selecionado
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String deviceType = prefs.getString(DEVICE_TYPE_KEY, null);
        
        if (deviceType != null) {
            // Tipo já foi selecionado, ir para a MainActivity
            navigateToMainActivity();
            return;
        }
        
        setContentView(R.layout.activity_device_type);
        
        Button mobileButton = findViewById(R.id.btn_mobile);
        Button tvButton = findViewById(R.id.btn_tv);
        
        mobileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDeviceType(DEVICE_TYPE_MOBILE);
                navigateToMainActivity();
            }
        });
        
        tvButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDeviceType(DEVICE_TYPE_TV);
                navigateToMainActivity();
            }
        });
    }
    
    private void saveDeviceType(String deviceType) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(DEVICE_TYPE_KEY, deviceType);
        editor.apply();
    }
    
    private void navigateToMainActivity() {
        Intent intent = new Intent(this, DownloadProgressActivity.class);
        startActivity(intent);
        finish();
    }
    
    public static String getDeviceType(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        return prefs.getString(DEVICE_TYPE_KEY, DEVICE_TYPE_MOBILE);
    }
    
    public static boolean isTvMode(android.content.Context context) {
        return DEVICE_TYPE_TV.equals(getDeviceType(context));
    }
}

