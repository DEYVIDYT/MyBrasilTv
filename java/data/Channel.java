package com.example.iptvplayer.data;

public class Channel {
    private String name;
    private String url;
    private String logoUrl;
    private String categoryId; // Renomeado de groupTitle
    private String streamId; // Important for EPG fetching

    // EPG fields
    private String currentEpgTitle;
    private String currentEpgStartTime;
    private String currentEpgEndTime;
    private String currentEpgDescription;
    private boolean epgFetched; // To track if EPG has been fetched for this channel in the current session

    public Channel(String name, String url, String logoUrl, String categoryId, String streamId) {
        this.name = name;
        this.url = url;
        this.logoUrl = logoUrl;
        this.categoryId = categoryId; // Atribuído ao novo campo
        this.streamId = streamId;

        // Initialize EPG fields
        this.currentEpgTitle = "";
        this.currentEpgStartTime = "";
        this.currentEpgEndTime = "";
        this.currentEpgDescription = "";
        this.epgFetched = false;
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

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public String getCurrentEpgTitle() {
        return currentEpgTitle;
    }

    public void setCurrentEpgTitle(String currentEpgTitle) {
        this.currentEpgTitle = currentEpgTitle;
    }

    public String getCurrentEpgStartTime() {
        return currentEpgStartTime;
    }

    public void setCurrentEpgStartTime(String currentEpgStartTime) {
        this.currentEpgStartTime = currentEpgStartTime;
    }

    public String getCurrentEpgEndTime() {
        return currentEpgEndTime;
    }

    public void setCurrentEpgEndTime(String currentEpgEndTime) {
        this.currentEpgEndTime = currentEpgEndTime;
    }

    public String getCurrentEpgDescription() {
        return currentEpgDescription;
    }

    public void setCurrentEpgDescription(String currentEpgDescription) {
        this.currentEpgDescription = currentEpgDescription;
    }

    public boolean isEpgFetched() {
        return epgFetched;
    }

    public void setEpgFetched(boolean epgFetched) {
        this.epgFetched = epgFetched;
    }
}

