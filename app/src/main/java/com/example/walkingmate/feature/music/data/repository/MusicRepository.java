package com.example.walkingmate.feature.music.data.repository;

import com.example.walkingmate.BuildConfig;
import com.example.walkingmate.feature.music.data.model.BpmResponse;
import com.example.walkingmate.feature.music.data.model.SimilarSongItem;
import com.example.walkingmate.feature.music.data.remote.MusicApi;
import com.google.gson.JsonObject;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MusicRepository {
    private static final String BASE_URL = ensureTrailingSlash(BuildConfig.MUSIC_SERVER_BASE_URL);
    private static MusicRepository instance;

    private final MusicApi api;

    private MusicRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(MusicApi.class);
    }

    private static String ensureTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return "http://10.0.2.2:5000/";
        }
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    public static synchronized MusicRepository getInstance() {
        if (instance == null) {
            instance = new MusicRepository();
        }
        return instance;
    }

    public String getMusicFileUrl(String fileTitle) {
        return BASE_URL + "music/" + fileTitle;
    }

    public String getLowBpmUrl() {
        return BASE_URL + "random_music/low_bpm";
    }

    public String getHighBpmUrl() {
        return BASE_URL + "random_music/high_bpm";
    }

    public Call<BpmResponse> sendBpm(int bpm) {
        return api.sendBpm(bpm);
    }

    public Call<JsonObject> getCurrentTempo() {
        return api.getCurrentTempo();
    }

    public Call<JsonObject> getCurrentFileName() {
        return api.getCurrentFileName();
    }

    public Call<ResponseBody> uploadMusic(MultipartBody.Part file) {
        return api.uploadMusic(file);
    }

    public Call<JsonObject> uploadMusicAsync(MultipartBody.Part file) {
        return api.uploadMusicAsync(file);
    }

    public Call<JsonObject> getJobStatus(String jobId) {
        return api.getJobStatus(jobId);
    }

    public Call<List<String>> getSimilarSongs(String filename) {
        return api.getSimilarSongs(filename);
    }

    public Call<List<SimilarSongItem>> getSimilarSongsDetailed(String filename) {
        return api.getSimilarSongsDetailed(filename);
    }

    public Call<ResponseBody> downloadMusic(String filename) {
        return api.downloadMusic(filename);
    }
}
