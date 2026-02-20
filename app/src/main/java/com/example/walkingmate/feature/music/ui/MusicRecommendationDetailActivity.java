package com.example.walkingmate.feature.music.ui;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.walkingmate.R;
import com.example.walkingmate.feature.music.data.repository.MusicRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MusicRecommendationDetailActivity extends AppCompatActivity {

    private MusicRepository musicRepository;
    private MediaPlayer mediaPlayer;
    private String filename;
    private boolean isPlaying = false;
    private boolean isActivityActive = false;
    private Call<ResponseBody> pendingDownloadCall;

    private TextView tvTitle;
    private TextView tvSimilarity;
    private TextView tvGuide;
    private ProgressBar progressBar;
    private Button btnToggleDetails;
    private LinearLayout detailMetricsContainer;
    private ProgressBar pbTempo;
    private ProgressBar pbEnergy;
    private ProgressBar pbBrightness;
    private ProgressBar pbRhythm;
    private TextView tvTempoValue;
    private TextView tvEnergyValue;
    private TextView tvBrightnessValue;
    private TextView tvRhythmValue;
    private Button btnPlayPause;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_recommendation_detail);

        musicRepository = MusicRepository.getInstance();
        filename = getIntent().getStringExtra("filename");
        String displayName = getIntent().getStringExtra("displayName");
        int similarityPercent = getIntent().getIntExtra("similarityPercent", 0);
        int tempoSimilarityPercent = getIntent().getIntExtra("tempoSimilarityPercent", 0);
        int energySimilarityPercent = getIntent().getIntExtra("energySimilarityPercent", 0);
        int brightnessSimilarityPercent = getIntent().getIntExtra("brightnessSimilarityPercent", 0);
        int rhythmSimilarityPercent = getIntent().getIntExtra("rhythmSimilarityPercent", 0);

        tvTitle = findViewById(R.id.tvDetailTitle);
        tvSimilarity = findViewById(R.id.tvDetailSimilarity);
        tvGuide = findViewById(R.id.tvDetailGuide);
        progressBar = findViewById(R.id.pbDetailSimilarity);
        btnToggleDetails = findViewById(R.id.btnToggleDetails);
        detailMetricsContainer = findViewById(R.id.layoutDetailMetrics);
        pbTempo = findViewById(R.id.pbTempoSimilarity);
        pbEnergy = findViewById(R.id.pbEnergySimilarity);
        pbBrightness = findViewById(R.id.pbBrightnessSimilarity);
        pbRhythm = findViewById(R.id.pbRhythmSimilarity);
        tvTempoValue = findViewById(R.id.tvTempoSimilarityValue);
        tvEnergyValue = findViewById(R.id.tvEnergySimilarityValue);
        tvBrightnessValue = findViewById(R.id.tvBrightnessSimilarityValue);
        tvRhythmValue = findViewById(R.id.tvRhythmSimilarityValue);
        btnPlayPause = findViewById(R.id.btnDetailPlayPause);
        btnBack = findViewById(R.id.btnDetailBack);

        tvTitle.setText(displayName != null ? displayName : "추천 음악");
        tvSimilarity.setText("총 유사도 " + similarityPercent + "%");
        progressBar.setMax(100);
        progressBar.setProgress(similarityPercent);
        bindDetailMetric(pbTempo, tvTempoValue, tempoSimilarityPercent);
        bindDetailMetric(pbEnergy, tvEnergyValue, energySimilarityPercent);
        bindDetailMetric(pbBrightness, tvBrightnessValue, brightnessSimilarityPercent);
        bindDetailMetric(pbRhythm, tvRhythmValue, rhythmSimilarityPercent);

        detailMetricsContainer.setVisibility(LinearLayout.GONE);
        btnToggleDetails.setOnClickListener(v -> {
            if (detailMetricsContainer.getVisibility() == LinearLayout.VISIBLE) {
                detailMetricsContainer.setVisibility(LinearLayout.GONE);
                btnToggleDetails.setText("자세히 보기");
            } else {
                detailMetricsContainer.setVisibility(LinearLayout.VISIBLE);
                btnToggleDetails.setText("간단히 보기");
            }
        });

        if (similarityPercent >= 85) {
            tvGuide.setText("내 음악과 분위기가 매우 비슷해요.");
        } else if (similarityPercent >= 70) {
            tvGuide.setText("내 음악과 분위기가 꽤 비슷해요.");
        } else {
            tvGuide.setText("조금 다른 분위기지만 새로운 느낌으로 추천해요.");
        }

        btnPlayPause.setOnClickListener(v -> {
            if (mediaPlayer == null) {
                downloadAndPlayMusic();
                return;
            }
            if (isPlaying) {
                mediaPlayer.pause();
                isPlaying = false;
                btnPlayPause.setText("재생");
            } else {
                mediaPlayer.start();
                isPlaying = true;
                btnPlayPause.setText("일시정지");
            }
        });

        btnBack.setOnClickListener(v -> finish());

        // 상세 화면 진입 시 자동 재생
        downloadAndPlayMusic();
    }

    @Override
    protected void onStart() {
        super.onStart();
        isActivityActive = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActivityActive = false;
        if (pendingDownloadCall != null && !pendingDownloadCall.isCanceled()) {
            pendingDownloadCall.cancel();
        }
        stopAndReleasePlayer();
    }

    private void bindDetailMetric(ProgressBar bar, TextView value, int percent) {
        bar.setMax(100);
        bar.setProgress(percent);
        value.setText(percent + "%");
    }

    private void downloadAndPlayMusic() {
        if (filename == null || filename.trim().isEmpty()) {
            if (canShowUiFeedback()) {
                Toast.makeText(this, "재생할 파일 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (pendingDownloadCall != null && !pendingDownloadCall.isCanceled()) {
            pendingDownloadCall.cancel();
        }
        pendingDownloadCall = musicRepository.downloadMusic(filename);
        pendingDownloadCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!canShowUiFeedback()) {
                    return;
                }
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
                        if (canShowUiFeedback()) {
                            Toast.makeText(MusicRecommendationDetailActivity.this, "음악 재생 준비 실패", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    if (canShowUiFeedback()) {
                        Toast.makeText(MusicRecommendationDetailActivity.this, "파일 다운로드 실패", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (call.isCanceled()) {
                    return;
                }
                if (canShowUiFeedback()) {
                    Toast.makeText(MusicRecommendationDetailActivity.this, "서버와 연결 실패", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void playMusic(Uri uri) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = MediaPlayer.create(this, uri);
            if (mediaPlayer != null) {
                mediaPlayer.start();
                isPlaying = true;
                btnPlayPause.setText("일시정지");
                mediaPlayer.setOnCompletionListener(mp -> {
                    isPlaying = false;
                    btnPlayPause.setText("재생");
                });
            } else {
                Toast.makeText(this, "음악 재생 실패", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "음악 재생 실패", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean canShowUiFeedback() {
        return isActivityActive && !isFinishing() && !isDestroyed();
    }

    private void stopAndReleasePlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (Exception ignored) {
            }
            mediaPlayer.release();
            mediaPlayer = null;
            isPlaying = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pendingDownloadCall != null && !pendingDownloadCall.isCanceled()) {
            pendingDownloadCall.cancel();
        }
        stopAndReleasePlayer();
    }
}
