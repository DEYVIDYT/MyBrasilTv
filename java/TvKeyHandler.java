package com.example.iptvplayer;

import android.view.KeyEvent;
import android.view.View;
import android.util.Log;

public class TvKeyHandler {
    
    private static final String TAG = "TvKeyHandler";
    
    public interface TvKeyListener {
        boolean onTvKeyDown(int keyCode, KeyEvent event);
        boolean onTvKeyUp(int keyCode, KeyEvent event);
    }
    
    /**
     * Processa eventos de tecla para controle remoto e gamepad
     */
    public static boolean handleKeyEvent(int keyCode, KeyEvent event, TvKeyListener listener) {
        Log.d(TAG, "Key event: " + keyCode + ", action: " + event.getAction());
        
        switch (keyCode) {
            // Teclas direcionais do controle remoto
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (event.getAction() == KeyEvent.ACTION_DOWN && listener != null) {
                    return listener.onTvKeyDown(keyCode, event);
                } else if (event.getAction() == KeyEvent.ACTION_UP && listener != null) {
                    return listener.onTvKeyUp(keyCode, event);
                }
                break;
                
            // Botões de controle de mídia
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_STOP:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                if (event.getAction() == KeyEvent.ACTION_DOWN && listener != null) {
                    return listener.onTvKeyDown(keyCode, event);
                }
                break;
                
            // Botões do gamepad
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_BUTTON_B:
            case KeyEvent.KEYCODE_BUTTON_X:
            case KeyEvent.KEYCODE_BUTTON_Y:
            case KeyEvent.KEYCODE_BUTTON_L1:
            case KeyEvent.KEYCODE_BUTTON_R1:
            case KeyEvent.KEYCODE_BUTTON_L2:
            case KeyEvent.KEYCODE_BUTTON_R2:
            case KeyEvent.KEYCODE_BUTTON_START:
            case KeyEvent.KEYCODE_BUTTON_SELECT:
                if (event.getAction() == KeyEvent.ACTION_DOWN && listener != null) {
                    return listener.onTvKeyDown(keyCode, event);
                }
                break;
                
            // Controle de volume
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                // Deixar o sistema lidar com volume
                return false;
                
            // Teclas especiais do controle remoto
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_HOME:
                if (event.getAction() == KeyEvent.ACTION_DOWN && listener != null) {
                    return listener.onTvKeyDown(keyCode, event);
                }
                break;
                
            default:
                Log.d(TAG, "Unhandled key: " + keyCode);
                return false;
        }
        
        return false;
    }
    
    /**
     * Configura foco para navegação por controle remoto
     */
    public static void setupTvFocus(View view, View nextFocusUp, View nextFocusDown, 
                                   View nextFocusLeft, View nextFocusRight) {
        view.setFocusable(true);
        view.setFocusableInTouchMode(false);
        
        if (nextFocusUp != null) {
            view.setNextFocusUpId(nextFocusUp.getId());
        }
        if (nextFocusDown != null) {
            view.setNextFocusDownId(nextFocusDown.getId());
        }
        if (nextFocusLeft != null) {
            view.setNextFocusLeftId(nextFocusLeft.getId());
        }
        if (nextFocusRight != null) {
            view.setNextFocusRightId(nextFocusRight.getId());
        }
    }
    
    /**
     * Mapeia teclas do gamepad para ações equivalentes do controle remoto
     */
    public static int mapGamepadToRemote(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A:
                return KeyEvent.KEYCODE_DPAD_CENTER;
            case KeyEvent.KEYCODE_BUTTON_B:
                return KeyEvent.KEYCODE_BACK;
            case KeyEvent.KEYCODE_BUTTON_X:
                return KeyEvent.KEYCODE_MENU;
            case KeyEvent.KEYCODE_BUTTON_Y:
                return KeyEvent.KEYCODE_SEARCH;
            case KeyEvent.KEYCODE_BUTTON_START:
                return KeyEvent.KEYCODE_MENU;
            case KeyEvent.KEYCODE_BUTTON_SELECT:
                return KeyEvent.KEYCODE_BACK;
            default:
                return keyCode;
        }
    }
}

