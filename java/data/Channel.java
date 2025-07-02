package com.example.iptvplayer.data;

public class Channel {
    private String name;
    private String url;
    private String logoUrl;
    private String categoryId; // Renomeado de groupTitle

    public Channel(String name, String url, String logoUrl, String categoryId) {
        this.name = name;
        this.url = url;
        this.logoUrl = logoUrl;
        this.categoryId = categoryId; // Atribuído ao novo campo
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getCategoryId() { // Renomeado de getGroupTitle
        return categoryId;
    }

    public String getStreamUrl() {
        return url;
    }
}

