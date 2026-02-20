package com.example.walkingmate.feature.music.data.model;

public class SimilarSongItem {
    private String filename;
    private int similarity_percent;
    private int tempo_similarity_percent;
    private int energy_similarity_percent;
    private int brightness_similarity_percent;
    private int rhythm_similarity_percent;

    public String getFilename() {
        return filename;
    }

    public int getSimilarityPercent() {
        return similarity_percent;
    }

    public int getTempoSimilarityPercent() {
        return tempo_similarity_percent;
    }

    public int getEnergySimilarityPercent() {
        return energy_similarity_percent;
    }

    public int getBrightnessSimilarityPercent() {
        return brightness_similarity_percent;
    }

    public int getRhythmSimilarityPercent() {
        return rhythm_similarity_percent;
    }
}
