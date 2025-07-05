package com.example.iptvplayer;

import android.content.Context;
import android.util.Log;
import android.util.Log;
import com.example.iptvplayer.data.Movie;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MovieCacheManager {

    private static final String TAG = "MovieCacheManager";
    private static final String MOVIE_CACHE_FILE_NAME = "movie_cache.json";
    private static final String CATEGORY_MAP_CACHE_FILE_NAME = "category_map_cache.json";
    private static final long CACHE_EXPIRY_DURATION_MS = 24 * 60 * 60 * 1000; // 24 hours

    public static void saveMoviesToCache(Context context, List<Movie> movies) {
        JSONArray jsonArray = new JSONArray();
        long currentTimestamp = System.currentTimeMillis();
        for (Movie movie : movies) {
            try {
                JSONObject movieJson = new JSONObject();
                movieJson.put("title", movie.getTitle());
                movieJson.put("posterUrl", movie.getPosterUrl());
                movieJson.put("videoUrl", movie.getVideoUrl());
                movieJson.put("category", movie.getCategory());
                movieJson.put("streamId", movie.getStreamId()); // Adicionado para salvar streamId
                movieJson.put("lastRefreshedTimestamp", currentTimestamp); // Save with current time
                jsonArray.put(movieJson);
            } catch (JSONException e) {
                Log.e(TAG, "Error creating JSON for movie: " + movie.getTitle(), e);
            }
        }

        try (FileOutputStream fos = context.openFileOutput(MOVIE_CACHE_FILE_NAME, Context.MODE_PRIVATE)) {
            fos.write(jsonArray.toString().getBytes(StandardCharsets.UTF_8));
            Log.i(TAG, "Successfully saved " + movies.size() + " movies to cache.");
        } catch (IOException e) {
            Log.e(TAG, "Error saving movies to cache", e);
        }
    }

    public static void saveCategoryMapToCache(Context context, java.util.Map<String, String> categoryMap) {
        JSONObject jsonObject = new JSONObject(categoryMap);
        try (FileOutputStream fos = context.openFileOutput(CATEGORY_MAP_CACHE_FILE_NAME, Context.MODE_PRIVATE)) {
            fos.write(jsonObject.toString().getBytes(StandardCharsets.UTF_8));
            // Also save a timestamp for the category map itself
            fos.write(("\n" + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
            Log.i(TAG, "Successfully saved category map to cache.");
        } catch (IOException e) {
            Log.e(TAG, "Error saving category map to cache", e);
        }
    }


    public static List<Movie> loadMoviesFromCache(Context context) {
        List<Movie> cachedMovies = new ArrayList<>();
        try (FileInputStream fis = context.openFileInput(MOVIE_CACHE_FILE_NAME);
             InputStreamReader inputStreamReader = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }

            JSONArray jsonArray = new JSONArray(stringBuilder.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject movieJson = jsonArray.getJSONObject(i);
                String title = movieJson.getString("title");
                String posterUrl = movieJson.optString("posterUrl", null);
                String videoUrl = movieJson.getString("videoUrl");
                String category = movieJson.getString("category");
                String streamId = movieJson.optString("streamId", null); // Adicionado para ler streamId (optString para retrocompatibilidade)
                long lastRefreshedTimestamp = movieJson.getLong("lastRefreshedTimestamp");

                cachedMovies.add(new Movie(title, posterUrl, videoUrl, category, streamId, lastRefreshedTimestamp));           }
            Log.i(TAG, "Successfully loaded " + cachedMovies.size() + " non-expired movies from cache.");

        } catch (IOException e) {
            Log.w(TAG, "Movie cache file not found or error reading cache: " + e.getMessage());
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON from movie cache", e);
            context.deleteFile(MOVIE_CACHE_FILE_NAME);
        }
        return cachedMovies;
    }

    public static java.util.Map<String, String> loadCategoryMapFromCache(Context context) {
        java.util.Map<String, String> categoryMap = new java.util.HashMap<>();
        try (FileInputStream fis = context.openFileInput(CATEGORY_MAP_CACHE_FILE_NAME);
             InputStreamReader inputStreamReader = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            StringBuilder stringBuilder = new StringBuilder();
            String line;
            String firstLine = bufferedReader.readLine(); // JSON map
            String secondLine = bufferedReader.readLine(); // Timestamp

            if (firstLine == null || secondLine == null) {
                 Log.w(TAG, "Category map cache file is incomplete.");
                 context.deleteFile(CATEGORY_MAP_CACHE_FILE_NAME);
                 return categoryMap; // Empty map
            }

            long cacheTimestamp = Long.parseLong(secondLine);
            if (System.currentTimeMillis() - cacheTimestamp >= CACHE_EXPIRY_DURATION_MS) {
                Log.i(TAG, "Category map cache is expired.");
                context.deleteFile(CATEGORY_MAP_CACHE_FILE_NAME);
                return categoryMap; // Return empty map, will be refreshed
            }

            JSONObject jsonObject = new JSONObject(firstLine);
            java.util.Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                categoryMap.put(key, jsonObject.getString(key));
            }
            Log.i(TAG, "Successfully loaded " + categoryMap.size() + " categories from cache.");

        } catch (IOException e) {
            Log.w(TAG, "Category map cache file not found or error reading: " + e.getMessage());
        } catch (JSONException | NumberFormatException e) {
            Log.e(TAG, "Error parsing JSON or timestamp from category map cache", e);
            context.deleteFile(CATEGORY_MAP_CACHE_FILE_NAME);
        }
        return categoryMap;
    }


    public static boolean isCacheValid(Context context) {
        // Check movie cache file
        java.io.File movieCacheFile = new java.io.File(context.getFilesDir(), MOVIE_CACHE_FILE_NAME);
        if (!movieCacheFile.exists() || (System.currentTimeMillis() - movieCacheFile.lastModified() >= CACHE_EXPIRY_DURATION_MS)) {
            return false; // Movie cache is invalid or too old
        }

        // Check category map cache file (using the stored timestamp method)
        java.util.Map<String, String> categoryMap = loadCategoryMapFromCache(context); // This already checks its own expiry
        return !categoryMap.isEmpty(); // If map is loaded and not empty, it's considered valid for this check
    }

     public static void clearCache(Context context) {
        boolean moviesCleared = context.deleteFile(MOVIE_CACHE_FILE_NAME);
        boolean categoriesCleared = context.deleteFile(CATEGORY_MAP_CACHE_FILE_NAME);

        if (moviesCleared) Log.i(TAG, "Movie cache cleared successfully.");
        else Log.w(TAG, "Failed to clear movie cache or cache file did not exist.");

        if (categoriesCleared) Log.i(TAG, "Category map cache cleared successfully.");
        else Log.w(TAG, "Failed to clear category map cache or cache file did not exist.");
    }
}
