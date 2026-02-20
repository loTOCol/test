package com.example.walkingmate.feature.music.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.walkingmate.R;
import com.example.walkingmate.feature.music.data.model.SimilarSongItem;
import com.example.walkingmate.feature.music.data.repository.MusicRepository;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MusicRecommendActivity extends AppCompatActivity {

    private static final int PICK_AUDIO_REQUEST = 1001;
    private static final String PREFS_NAME = "MusicUploadActivityPrefs";
    private static final String KEY_MUSIC_URI = "music_uri";
    private static Uri musicUri;
    private MediaPlayer mediaPlayer;
    private MusicRepository musicRepository;
    private TextView musicTitleView;
    private LinearLayout similarMusicContainer;
    private Call<JsonObject> uploadCall;
    private Call<JsonObject> jobStatusCall;
    private Call<List<SimilarSongItem>> similarSongsCall;

    private final String[] VALID_PREFIXES = {"blues", "classical", "country", "disco", "hiphop", "jazz", "metal", "pop", "reggae", "rock"};

    private final List<Runnable> scheduledTasks = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_recommend);

        musicTitleView = findViewById(R.id.music_title);
        similarMusicContainer = findViewById(R.id.similar_music_container);
        Button uploadMusicButton = findViewById(R.id.upload_music_button);
        Button playMusicButton = findViewById(R.id.play_music_button);
        Button getSimilarMusicButton = findViewById(R.id.get_similar_music_button);
        Button stopMusicButton = findViewById(R.id.stop_music_button);

        musicRepository = MusicRepository.getInstance();

        restoreMusicUriIfNeeded();
        if (musicUri != null) {
            String musicTitle = getMusicTitle(musicUri);
            musicTitleView.setText("내 음악: " + musicTitle);
        } else {
            Toast.makeText(this, "음악 파일이 없습니다.", Toast.LENGTH_SHORT).show();
        }

        uploadMusicButton.setOnClickListener(v -> openAudioPicker());

        playMusicButton.setOnClickListener(v -> {
            if (musicUri != null) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                playMusic(musicUri);
                cancelScheduledTasks();
            } else {
                Toast.makeText(MusicRecommendActivity.this, "음악 파일이 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        getSimilarMusicButton.setOnClickListener(v -> {
            if (musicUri != null) {
                uploadMusicAndFetchSimilar(musicUri);
            } else {
                Toast.makeText(MusicRecommendActivity.this, "음악 파일이 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        stopMusicButton.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            cancelScheduledTasks();
        });
    }

    private void openAudioPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_AUDIO_REQUEST);
    }

    private void restoreMusicUriIfNeeded() {
        if (musicUri != null) {
            return;
        }
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedUriString = preferences.getString(KEY_MUSIC_URI, null);
        if (savedUriString != null) {
            Uri savedUri = Uri.parse(savedUriString);
            if (canReadUri(savedUri)) {
                musicUri = savedUri;
            } else {
                preferences.edit().remove(KEY_MUSIC_URI).apply();
            }
        }
    }

    private void saveMusicUri(Uri uri) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        preferences.edit().putString(KEY_MUSIC_URI, uri.toString()).apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cancelScheduledTasks();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (uploadCall != null && !uploadCall.isCanceled()) {
            uploadCall.cancel();
        }
        if (jobStatusCall != null && !jobStatusCall.isCanceled()) {
            jobStatusCall.cancel();
        }
        if (similarSongsCall != null && !similarSongsCall.isCanceled()) {
            similarSongsCall.cancel();
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        cancelScheduledTasks();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedAudioUri = data.getData();
            if (selectedAudioUri == null) {
                Toast.makeText(this, "음악 파일을 선택하지 못했습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                final int takeFlags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
                getContentResolver().takePersistableUriPermission(selectedAudioUri, takeFlags);
            } catch (SecurityException ignored) {
            }

            musicUri = selectedAudioUri;
            saveMusicUri(selectedAudioUri);
            musicTitleView.setText("내 음악: " + getMusicTitle(selectedAudioUri));
            Toast.makeText(this, "음악 파일이 선택되었습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    public static void setMusicUri(Uri uri) {
        musicUri = uri;
    }

    private String getMusicTitle(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                if (titleIndex >= 0) {
                    String title = cursor.getString(titleIndex);
                    if (title != null && !title.trim().isEmpty()) {
                        return title;
                    }
                }
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    String displayName = cursor.getString(nameIndex);
                    if (displayName != null && !displayName.trim().isEmpty()) {
                        return removeFileExtension(displayName);
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "제목을 가져올 수 없습니다";
    }

    private void playMusic(Uri uri) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        try {
            mediaPlayer = MediaPlayer.create(this, uri);
            if (mediaPlayer != null) {
                mediaPlayer.start();
            } else {
                Toast.makeText(this, "음악 재생에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "선택한 음악 파일 접근 권한이 없습니다. 다시 선택해 주세요.", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadMusicAndFetchSimilar(Uri uri) {
        File file = createTempFileFromUri(uri);
        if (file == null || !file.exists()) {
            clearSavedMusicUri();
            Toast.makeText(this, "선택한 음악 파일을 읽을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse("audio/*"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        if (uploadCall != null && !uploadCall.isCanceled()) {
            uploadCall.cancel();
        }
        uploadCall = musicRepository.uploadMusicAsync(body);
        uploadCall.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!canShowUiFeedback()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null && response.body().has("job_id")) {
                    String jobId = response.body().get("job_id").getAsString();
                    pollJobUntilFinished(jobId, 0);
                } else {
                    String detail = "HTTP " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            if (errorBody != null && !errorBody.trim().isEmpty()) {
                                detail = detail + " " + errorBody;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    Toast.makeText(MusicRecommendActivity.this, "파일 업로드 실패: " + detail, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (call.isCanceled() || !canShowUiFeedback()) {
                    return;
                }
                Toast.makeText(MusicRecommendActivity.this, "비동기 업로드 연결 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pollJobUntilFinished(String jobId, int attempt) {
        final int maxAttempts = 120;
        final int delayMs = 1500;

        if (attempt >= maxAttempts) {
            if (canShowUiFeedback()) {
                Toast.makeText(this, "업로드 처리 대기 시간 초과", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (jobStatusCall != null && !jobStatusCall.isCanceled()) {
            jobStatusCall.cancel();
        }

        jobStatusCall = musicRepository.getJobStatus(jobId);
        jobStatusCall.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!canShowUiFeedback()) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null || !response.body().has("status")) {
                    Toast.makeText(MusicRecommendActivity.this, "작업 상태 확인 실패", Toast.LENGTH_SHORT).show();
                    return;
                }

                JsonObject body = response.body();
                String status = body.get("status").getAsString();

                if ("finished".equals(status)) {
                    if (body.has("result") && body.get("result").isJsonObject()) {
                        JsonObject result = body.getAsJsonObject("result");
                        if (result.has("filename")) {
                            String filename = result.get("filename").getAsString();
                            fetchSimilarMusic(filename);
                            return;
                        }
                    }
                    Toast.makeText(MusicRecommendActivity.this, "업로드 결과 파싱 실패", Toast.LENGTH_SHORT).show();
                    return;
                }

                if ("failed".equals(status)) {
                    String error = body.has("error") ? body.get("error").getAsString() : "Upload processing failed";
                    Toast.makeText(MusicRecommendActivity.this, "업로드 처리 실패: " + error, Toast.LENGTH_SHORT).show();
                    return;
                }

                Runnable task = () -> pollJobUntilFinished(jobId, attempt + 1);
                scheduledTasks.add(task);
                handler.postDelayed(task, delayMs);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (call.isCanceled() || !canShowUiFeedback()) {
                    return;
                }
                Runnable task = () -> pollJobUntilFinished(jobId, attempt + 1);
                scheduledTasks.add(task);
                handler.postDelayed(task, delayMs);
            }
        });
    }

    private void fetchSimilarMusic(String filename) {
        final int MAX_RETRIES = 5;
        final int[] retryCount = {0};

        if (similarSongsCall != null && !similarSongsCall.isCanceled()) {
            similarSongsCall.cancel();
        }
        similarSongsCall = musicRepository.getSimilarSongsDetailed(filename);
        similarSongsCall.enqueue(new Callback<List<SimilarSongItem>>() {
            @Override
            public void onResponse(Call<List<SimilarSongItem>> call, Response<List<SimilarSongItem>> response) {
                if (!canShowUiFeedback()) {
                    return;
                }
                if (response.isSuccessful()) {
                    List<SimilarSongItem> similarItems = response.body();

                    if (similarItems == null || similarItems.size() < 5) {
                        if (retryCount[0] < MAX_RETRIES) {
                            retryCount[0]++;
                            fetchSimilarMusic(filename);
                        } else {
                            Toast.makeText(MusicRecommendActivity.this, "최대 재시도 횟수를 초과했습니다", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        List<SimilarSongItem> validItems = similarItems.stream()
                                .filter(item -> {
                                    String f = item.getFilename();
                                    for (String prefix : VALID_PREFIXES) {
                                        if (f.startsWith(prefix)) {
                                            return true;
                                        }
                                    }
                                    return false;
                                })
                                .collect(Collectors.toList());

                        if (validItems.isEmpty()) {
                            if (retryCount[0] < MAX_RETRIES) {
                                retryCount[0]++;
                                fetchSimilarMusic(filename);
                            } else {
                                Toast.makeText(MusicRecommendActivity.this, "최대 재시도 횟수를 초과했습니다", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            playSimilarMusic(validItems);
                        }
                    }
                } else {
                    Toast.makeText(MusicRecommendActivity.this, "유사 음악 가져오기 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<SimilarSongItem>> call, Throwable t) {
                if (call.isCanceled() || !canShowUiFeedback()) {
                    return;
                }
                Toast.makeText(MusicRecommendActivity.this, "서버 요청 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                if (retryCount[0] < MAX_RETRIES) {
                    retryCount[0]++;
                    fetchSimilarMusic(filename);
                } else {
                    Toast.makeText(MusicRecommendActivity.this, "최대 재시도 횟수를 초과했습니다", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void playSimilarMusic(List<SimilarSongItem> items) {
        similarMusicContainer.removeAllViews();

        List<Button> buttons = new ArrayList<>();
        Button firstButton = null;

        for (SimilarSongItem item : items) {
            String filename = item.getFilename();
            int similarityPercent = item.getSimilarityPercent();
            String displayName = formatRecommendationTitle(filename);

            Button musicButton = new Button(this);
            musicButton.setText(displayName + " · 총 유사도 " + similarityPercent + "%");
            musicButton.setBackgroundResource(R.drawable.bg_music_similar_button);
            musicButton.setTextColor(getResources().getColor(R.color.music_text_main));
            int tempoSimilarity = item.getTempoSimilarityPercent();
            int energySimilarity = item.getEnergySimilarityPercent();
            int brightnessSimilarity = item.getBrightnessSimilarityPercent();
            int rhythmSimilarity = item.getRhythmSimilarityPercent();
            musicButton.setOnClickListener(v -> openRecommendationDetail(
                    filename,
                    displayName,
                    similarityPercent,
                    tempoSimilarity,
                    energySimilarity,
                    brightnessSimilarity,
                    rhythmSimilarity
            ));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 16, 0, 16);
            musicButton.setLayoutParams(params);

            similarMusicContainer.addView(musicButton);
            buttons.add(musicButton);

            if (firstButton == null) {
                firstButton = musicButton;
            }
        }

        // 추천 리스트는 사용자가 직접 선택하도록 유지
    }

    private void openRecommendationDetail(
            String filename,
            String displayName,
            int similarityPercent,
            int tempoSimilarity,
            int energySimilarity,
            int brightnessSimilarity,
            int rhythmSimilarity
    ) {
        Intent intent = new Intent(this, MusicRecommendationDetailActivity.class);
        intent.putExtra("filename", filename);
        intent.putExtra("displayName", displayName);
        intent.putExtra("similarityPercent", similarityPercent);
        intent.putExtra("tempoSimilarityPercent", tempoSimilarity);
        intent.putExtra("energySimilarityPercent", energySimilarity);
        intent.putExtra("brightnessSimilarityPercent", brightnessSimilarity);
        intent.putExtra("rhythmSimilarityPercent", rhythmSimilarity);
        startActivity(intent);
    }

    private String removeFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex != -1) {
            return filename.substring(0, lastDotIndex);
        }
        return filename;
    }

    private String formatRecommendationTitle(String filename) {
        String base = removeFileExtension(filename);
        String[] parts = base.split("\\.");
        if (parts.length >= 2) {
            String genre = mapGenreLabel(parts[0]);
            return genre + " 분위기 추천";
        }
        return base;
    }

    private String mapGenreLabel(String genre) {
        switch (genre.toLowerCase()) {
            case "blues":
                return "블루스";
            case "classical":
                return "클래식";
            case "country":
                return "컨트리";
            case "disco":
                return "디스코";
            case "hiphop":
                return "힙합";
            case "jazz":
                return "재즈";
            case "metal":
                return "메탈";
            case "pop":
                return "팝";
            case "reggae":
                return "레게";
            case "rock":
                return "록";
            default:
                return genre;
        }
    }

    private void simulateButtonClicks(List<Button> buttons) {
        cancelScheduledTasks();
        scheduledTasks.clear();

        int delay = 0;

        for (Button button : buttons) {
            final Button currentButton = button;
            Runnable task = currentButton::performClick;
            scheduledTasks.add(task);
            handler.postDelayed(task, delay);
            delay += 32000;
        }
    }

    private void cancelScheduledTasks() {
        for (Runnable task : scheduledTasks) {
            handler.removeCallbacks(task);
        }
    }

    private void downloadAndPlayMusic(String filename) {
        musicRepository.downloadMusic(filename).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        File file = new File(getExternalFilesDir(null), filename);
                        FileOutputStream fos = new FileOutputStream(file);
                        InputStream inputStream = response.body().byteStream();
                        byte[] buffer = new byte[4096];
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                        fos.close();
                        inputStream.close();

                        playMusic(Uri.fromFile(file));
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MusicRecommendActivity.this, "파일 다운로드 및 재생 실패", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MusicRecommendActivity.this, "파일 다운로드 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(MusicRecommendActivity.this, "서버와 연결 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private File createTempFileFromUri(Uri uri) {
        String fileName = getFileNameFromUri(uri);
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = "upload_audio_" + System.currentTimeMillis() + ".mp3";
        }

        File outFile = new File(getCacheDir(), fileName);
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(outFile)) {
            if (inputStream == null) {
                return null;
            }

            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
            return outFile;
        } catch (SecurityException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getFileNameFromUri(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    return cursor.getString(nameIndex);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private boolean canReadUri(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            return inputStream != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void clearSavedMusicUri() {
        musicUri = null;
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().remove(KEY_MUSIC_URI).apply();
        musicTitleView.setText("내 음악");
    }

    private boolean canShowUiFeedback() {
        return !isFinishing() && !isDestroyed();
    }
}
