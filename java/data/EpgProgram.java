package com.example.iptvplayer.data;

public class EpgProgram {
    private String id;
    private String title;
    private String description;
    private String startTime;
    private String endTime;
    private String channelId;
    private String category;
    private String language;
    private String rating;
    private String icon;

    public EpgProgram(String id, String title, String description, String startTime, String endTime, String channelId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.channelId = channelId;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getCategory() {
        return category;
    }

    public String getLanguage() {
        return language;
    }

    public String getRating() {
        return rating;
    }

    public String getIcon() {
        return icon;
    }

    // Setters opcionais
    public void setCategory(String category) {
        this.category = category;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    // Novo método setTitle
    public void setTitle(String title) {
        this.title = title;
    }

    // Método para verificar se o programa está ativo no momento
    public boolean isCurrentlyActive() {
        try {
            long currentTime = System.currentTimeMillis() / 1000; // Unix timestamp
            long start = Long.parseLong(startTime);
            long end = Long.parseLong(endTime);
            return currentTime >= start && currentTime <= end;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Método para obter duração em minutos
    public int getDurationInMinutes() {
        try {
            long start = Long.parseLong(startTime);
            long end = Long.parseLong(endTime);
            return (int) ((end - start) / 60);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public String toString() {
        return "EpgProgram{" +
                "id=\'" + id + "\'" +
                ", title=\'" + title + "\'" +
                ", startTime=\'" + startTime + "\'" +
                ", endTime=\'" + endTime + "\'" +
                ", channelId=\'" + channelId + "\'" +
                "}";
    }
}


