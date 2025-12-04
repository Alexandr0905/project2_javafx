package com.example.project2_javafx.service;

public class AnimationSpec {
    private String type; // e.g. "fade", "translate"
    private long durationMillis;
    private long delayMillis;

    public AnimationSpec() {}

    public AnimationSpec(String type, long durationMillis, long delayMillis) {
        this.type = type;
        this.durationMillis = durationMillis;
        this.delayMillis = delayMillis;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getDurationMillis() { return durationMillis; }
    public void setDurationMillis(long durationMillis) { this.durationMillis = durationMillis; }

    public long getDelayMillis() { return delayMillis; }
    public void setDelayMillis(long delayMillis) { this.delayMillis = delayMillis; }
}