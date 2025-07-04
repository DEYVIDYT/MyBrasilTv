package com.example.iptvplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.example.iptvplayer.data.Channel;
import com.example.iptvplayer.data.Movie;
import com.example.iptvplayer.data.EpgProgram;

public class CacheManager {
    private static final String TAG = "CacheManager";
    private static final String PREFS_NAME = "iptv_cache";
    private static final String KEY_CHANNELS = "cached_channels";
    private static final String KEY_MOVIES = "cached_movies";
    private static final String KEY_EPG = "cached_epg";
    private static final String KEY_LAST_UPDATE = "last_update";
    private static final String KEY_CACHE_EXPIRY = "cache_expiry";
    
    // Cache válido por 24 horas (em milissegundos)
    private static final long CACHE_DURATION = 24 * 60 * 60 * 1000;
    
    private SharedPreferences prefs;
    private Gson gson;
    
    public CacheManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }
    
    // Métodos para Canais
    public void saveChannels(List<Channel> channels) {
        try {
            String json = gson.toJson(channels);
            prefs.edit()
                .putString(KEY_CHANNELS, json)
                .putLong(KEY_LAST_UPDATE + "_channels", System.currentTimeMillis())
                .apply();
            Log.d(TAG, "Channels saved to cache: " + channels.size() + " items");
        } catch (Exception e) {
            Log.e(TAG, "Error saving channels to cache", e);
        }
    }
    
    public List<Channel> getCachedChannels() {
        try {
            if (!isCacheValid("_channels")) {
                Log.d(TAG, "Channel cache expired");
                return null;
            }
            
            String json = prefs.getString(KEY_CHANNELS, null);
            if (json != null) {
                Type listType = new TypeToken<List<Channel>>(){}.getType();
                List<Channel> channels = gson.fromJson(json, listType);
                Log.d(TAG, "Channels loaded from cache: " + channels.size() + " items");
                return channels;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading channels from cache", e);
        }
        return null;
    }
    
    // Métodos para Filmes/VOD
    public void saveMovies(List<Movie> movies) {
        try {
            String json = gson.toJson(movies);
            prefs.edit()
                .putString(KEY_MOVIES, json)
                .putLong(KEY_LAST_UPDATE + "_movies", System.currentTimeMillis())
                .apply();
            Log.d(TAG, "Movies saved to cache: " + movies.size() + " items");
        } catch (Exception e) {
            Log.e(TAG, "Error saving movies to cache", e);
        }
    }
    
    public List<Movie> getCachedMovies() {
        try {
            if (!isCacheValid("_movies")) {
                Log.d(TAG, "Movie cache expired");
                return null;
            }
            
            String json = prefs.getString(KEY_MOVIES, null);
            if (json != null) {
                Type listType = new TypeToken<List<Movie>>(){}.getType();
                List<Movie> movies = gson.fromJson(json, listType);
                Log.d(TAG, "Movies loaded from cache: " + movies.size() + " items");
                return movies;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading movies from cache", e);
        }
        return null;
    }
    
    // Métodos para EPG
    public void saveEpg(String channelId, List<EpgProgram> programs) {
        try {
            String json = gson.toJson(programs);
            prefs.edit()
                .putString(KEY_EPG + "_" + channelId, json)
                .putLong(KEY_LAST_UPDATE + "_epg_" + channelId, System.currentTimeMillis())
                .apply();
            Log.d(TAG, "EPG saved to cache for channel " + channelId + ": " + programs.size() + " programs");
        } catch (Exception e) {
            Log.e(TAG, "Error saving EPG to cache for channel " + channelId, e);
        }
    }
    
    public List<EpgProgram> getCachedEpg(String channelId) {
        try {
            if (!isCacheValid("_epg_" + channelId)) {
                Log.d(TAG, "EPG cache expired for channel " + channelId);
                return null;
            }
            
            String json = prefs.getString(KEY_EPG + "_" + channelId, null);
            if (json != null) {
                Type listType = new TypeToken<List<EpgProgram>>(){}.getType();
                List<EpgProgram> programs = gson.fromJson(json, listType);
                Log.d(TAG, "EPG loaded from cache for channel " + channelId + ": " + programs.size() + " programs");
                return programs;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading EPG from cache for channel " + channelId, e);
        }
        return null;
    }
    
    // Método para verificar se o cache é válido
    private boolean isCacheValid(String suffix) {
        long lastUpdate = prefs.getLong(KEY_LAST_UPDATE + suffix, 0);
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastUpdate) < CACHE_DURATION;
    }
    
    // Método para limpar todo o cache
    public void clearCache() {
        prefs.edit().clear().apply();
        Log.d(TAG, "All cache cleared");
    }
    
    // Método para limpar cache específico
    public void clearChannelCache() {
        prefs.edit()
            .remove(KEY_CHANNELS)
            .remove(KEY_LAST_UPDATE + "_channels")
            .apply();
        Log.d(TAG, "Channel cache cleared");
    }
    
    public void clearMovieCache() {
        prefs.edit()
            .remove(KEY_MOVIES)
            .remove(KEY_LAST_UPDATE + "_movies")
            .apply();
        Log.d(TAG, "Movie cache cleared");
    }
    
    public void clearEpgCache(String channelId) {
        prefs.edit()
            .remove(KEY_EPG + "_" + channelId)
            .remove(KEY_LAST_UPDATE + "_epg_" + channelId)
            .apply();
        Log.d(TAG, "EPG cache cleared for channel " + channelId);
    }
    
    // Método para verificar se existe cache
    public boolean hasChannelCache() {
        return prefs.contains(KEY_CHANNELS) && isCacheValid("_channels");
    }
    
    public boolean hasMovieCache() {
        return prefs.contains(KEY_MOVIES) && isCacheValid("_movies");
    }
    
    public boolean hasEpgCache(String channelId) {
        return prefs.contains(KEY_EPG + "_" + channelId) && isCacheValid("_epg_" + channelId);
    }
}

