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
    private static final String CACHE_FILE_NAME = "movie_cache.json";
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
                movieJson.put("lastRefreshedTimestamp", currentTimestamp); // Save with current time
                jsonArray.put(movieJson);
            } catch (JSONException e) {
                Log.e(TAG, "Error creating JSON for movie: " + movie.getTitle(), e);
            }
        }

        try (FileOutputStream fos = context.openFileOutput(CACHE_FILE_NAME, Context.MODE_PRIVATE)) {
            fos.write(jsonArray.toString().getBytes(StandardCharsets.UTF_8));
            Log.i(TAG, "Successfully saved " + movies.size() + " movies to cache.");
        } catch (IOException e) {
            Log.e(TAG, "Error saving movies to cache", e);
        }
    }

    public static List<Movie> loadMoviesFromCache(Context context) {
        List<Movie> cachedMovies = new ArrayList<>();
        try (FileInputStream fis = context.openFileInput(CACHE_FILE_NAME);
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
                long lastRefreshedTimestamp = movieJson.getLong("lastRefreshedTimestamp");

                if (System.currentTimeMillis() - lastRefreshedTimestamp < CACHE_EXPIRY_DURATION_MS) {
                    cachedMovies.add(new Movie(title, posterUrl, videoUrl, category, lastRefreshedTimestamp));
                } else {
                    Log.i(TAG, "Cached data for movie '" + title + "' is expired.");
                    // Do not add expired data, it will be refreshed from API
                }
            }
            Log.i(TAG, "Successfully loaded " + cachedMovies.size() + " non-expired movies from cache.");

        } catch (IOException e) {
            Log.w(TAG, "Cache file not found or error reading cache: " + e.getMessage());
            // This is normal if cache doesn't exist yet
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON from cache", e);
            // Cache might be corrupted, consider deleting it
            context.deleteFile(CACHE_FILE_NAME);
        }
        return cachedMovies;
    }

    public static boolean isCacheValid(Context context) {
        List<Movie> movies = loadMoviesFromCache(context); // This already checks expiry for individual items
        if (movies.isEmpty()) {
            // If cache is empty, it might be because all items expired, or it never existed.
            // To be more precise, we can check the file modification time or a global timestamp.
            // For simplicity, if loadMoviesFromCache (which filters expired) returns empty,
            // assume cache is not valid or needs refresh.
            return false;
        }
        // If it returns any movie, it means at least one movie is not expired.
        // A more robust check might involve checking if *all* desired categories are present and not expired.
        // For this implementation, if we have any valid movies, we can consider the cache "partially valid".
        // The VodFragment will decide if it needs a full refresh.
        // A simpler check: is the file itself very old?
        java.io.File cacheFile = new java.io.File(context.getFilesDir(), CACHE_FILE_NAME);
        if (!cacheFile.exists()) return false;
        // Check if the cache file itself (as a whole) is older than expiry.
        // This is a simpler check than individual item expiry for a quick "is anything likely valid".
        return (System.currentTimeMillis() - cacheFile.lastModified() < CACHE_EXPIRY_DURATION_MS);
    }
     public static void clearCache(Context context) {
        if (context.deleteFile(CACHE_FILE_NAME)) {
            Log.i(TAG, "Movie cache cleared successfully.");
        } else {
            Log.w(TAG, "Failed to clear movie cache or cache file did not exist.");
        }
    }
}
