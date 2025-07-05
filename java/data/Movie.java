
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
    private String streamId; // Adicionado
    private String cast; // Adicionado
    private String director; // Adicionado
    private String genre; // Adicionado
    private java.util.List<String> backdropPaths; // Adicionado

    private long lastRefreshedTimestamp; // Added for caching logic

    // Construtor principal atualizado para incluir streamId
    public Movie(String title, String posterUrl, String videoUrl, String category, String streamId) {
        this.title = title;
        this.name = title; // Para compatibilidade
        this.posterUrl = posterUrl;
        this.streamIcon = posterUrl; // Para compatibilidade
        this.videoUrl = videoUrl;
        this.category = category;
        this.streamId = streamId; // Inicializa streamId
        this.lastRefreshedTimestamp = 0; // Initialize, will be set when fetched/cached
    }

    // Construtor para restauração do cache atualizado
    public Movie(String title, String posterUrl, String videoUrl, String category, String streamId, long lastRefreshedTimestamp) {
        this.title = title;
        this.name = title; // Para compatibilidade
        this.posterUrl = posterUrl;
        this.streamIcon = posterUrl; // Para compatibilidade
        this.videoUrl = videoUrl;
        this.category = category;
        this.streamId = streamId; // Inicializa streamId
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

    // Getters e Setters para os novos campos
    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public String getCast() {
        return cast;
    }

    public void setCast(String cast) {
        this.cast = cast;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public java.util.List<String> getBackdropPaths() {
        return backdropPaths;
    }

    public void setBackdropPaths(java.util.List<String> backdropPaths) {
        this.backdropPaths = backdropPaths;
    }
}

