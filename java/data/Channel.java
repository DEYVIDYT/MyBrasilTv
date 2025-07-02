package com.example.iptvplayer.data;

public class Channel {
    private String name;
    private String url;
    private String logoUrl;
    private String groupTitle;

    public Channel(String name, String url, String logoUrl, String groupTitle) {
        this.name = name;
        this.url = url;
        this.logoUrl = logoUrl;
        this.groupTitle = groupTitle;
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

    public String getGroupTitle() {
        return groupTitle;
    }

    public String getStreamUrl() {
        return url;
    }
}

