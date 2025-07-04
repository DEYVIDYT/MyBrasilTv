package com.example.iptvplayer.data;

public class Channel {
    private String name;
    private String url;
    private String logoUrl;
    private String categoryId;
    private String streamId; // Adicionado para armazenar o stream_id
    private String currentProgramTitle; // Adicionado para armazenar o título do programa atual

    public Channel(String name, String url, String logoUrl, String categoryId, String streamId) {
        this.name = name;
        this.url = url;
        this.logoUrl = logoUrl;
        this.categoryId = categoryId;
        this.streamId = streamId;
        this.currentProgramTitle = "Programação não disponível"; // Valor inicial atualizado
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

    public String getCategoryId() {
        return categoryId;
    }

    public String getStreamId() {
        return streamId;
    }

    public String getCurrentProgramTitle() {
        return currentProgramTitle;
    }

    public void setCurrentProgramTitle(String currentProgramTitle) {
        this.currentProgramTitle = currentProgramTitle;
    }

    public String getStreamUrl() {
        return url;
    }
}


