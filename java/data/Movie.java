
package com.example.iptvplayer.data;

import java.io.Serializable;

public class Movie implements Serializable {
    private String title;
    private String posterUrl;
    private String videoUrl;
    private String category;
    private String name;
    private String streamIcon;
    private String releaseDate;
    private String categoryName;
    private String rating;
    private String duration;
    private String plot;

    private long lastRefreshedTimestamp; // Added for caching logic

    public Movie(String title, String posterUrl, String videoUrl, String category) {
        this.title = title;
        this.name = title; // Para compatibilidade
        this.posterUrl = posterUrl;
        this.streamIcon = posterUrl; // Para compatibilidade
        this.videoUrl = videoUrl;
        this.category = category;
        this.lastRefreshedTimestamp = 0; // Initialize, will be set when fetched/cached
    }

    // Constructor including timestamp for when restoring from cache
    public Movie(String title, String posterUrl, String videoUrl, String category, long lastRefreshedTimestamp) {
        this.title = title;
        this.name = title; // Para compatibilidade
        this.posterUrl = posterUrl;
        this.streamIcon = posterUrl; // Para compatibilidade
        this.videoUrl = videoUrl;
        this.category = category;
        this.lastRefreshedTimestamp = lastRefreshedTimestamp;
    }

    public String getTitle() {
        return title;
    }

    public String getName() {
        return name != null ? name : title;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public String getStreamIcon() {
        return streamIcon != null ? streamIcon : posterUrl;
    }

    public void setStreamIcon(String streamIcon) {
        this.streamIcon = streamIcon;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getCategory() {
        return category;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getPlot() {
        return plot;
    }

    public void setPlot(String plot) {
        this.plot = plot;
    }

    public long getLastRefreshedTimestamp() {
        return lastRefreshedTimestamp;
    }

    public void setLastRefreshedTimestamp(long lastRefreshedTimestamp) {
        this.lastRefreshedTimestamp = lastRefreshedTimestamp;
    }
}

