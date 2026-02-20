package com.example.walkingmate.feature.music.data.remote;

import com.example.walkingmate.feature.music.data.model.BpmResponse;
import com.example.walkingmate.feature.music.data.model.SimilarSongItem;
import com.google.gson.JsonObject;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface MusicApi {
    @FormUrlEncoded
    @POST("/bpm")
    Call<BpmResponse> sendBpm(@Field("bpm") int bpm);

    @GET("current_tempo")
    Call<JsonObject> getCurrentTempo();

    @GET("current_filename")
    Call<JsonObject> getCurrentFileName();

    @Multipart
    @POST("/upload")
    Call<ResponseBody> uploadMusic(@Part MultipartBody.Part file);

    @Multipart
    @POST("/upload_async")
    Call<JsonObject> uploadMusicAsync(@Part MultipartBody.Part file);

    @GET("/jobs/{jobId}")
    Call<JsonObject> getJobStatus(@Path("jobId") String jobId);

    @GET("/similar_songs/{filename}")
    Call<List<String>> getSimilarSongs(@Path("filename") String filename);

    @GET("/similar_songs_detailed/{filename}")
    Call<List<SimilarSongItem>> getSimilarSongsDetailed(@Path("filename") String filename);

    @GET("/download/{filename}")
    Call<ResponseBody> downloadMusic(@Path("filename") String filename);
}
