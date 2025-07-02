
package com.example.iptvplayer.data;

public class Movie {
    private String title;
    private String posterUrl;
    private String videoUrl;
    private String category;

    public Movie(String title, String posterUrl, String videoUrl, String category) {
        this.title = title;
        this.posterUrl = posterUrl;
        this.videoUrl = videoUrl;
        this.category = category;
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
}

