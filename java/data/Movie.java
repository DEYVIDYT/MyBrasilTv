
package com.example.iptvplayer.data;

public class Movie {
    private String title;
    private String posterUrl;
    private String videoUrl;
    private String category;

    private long lastRefreshedTimestamp; // Added for caching logic

    public Movie(String title, String posterUrl, String videoUrl, String category) {
        this.title = title;
        this.posterUrl = posterUrl;
        this.videoUrl = videoUrl;
        this.category = category;
        this.lastRefreshedTimestamp = 0; // Initialize, will be set when fetched/cached
    }

    // Constructor including timestamp for when restoring from cache
    public Movie(String title, String posterUrl, String videoUrl, String category, long lastRefreshedTimestamp) {
        this.title = title;
        this.posterUrl = posterUrl;
        this.videoUrl = videoUrl;
        this.category = category;
        this.lastRefreshedTimestamp = lastRefreshedTimestamp;
    }

    public String getTitle() {
        return title;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getCategory() {
        return category;
    }

    public long getLastRefreshedTimestamp() {
        return lastRefreshedTimestamp;
    }

    public void setLastRefreshedTimestamp(long lastRefreshedTimestamp) {
        this.lastRefreshedTimestamp = lastRefreshedTimestamp;
    }
}

