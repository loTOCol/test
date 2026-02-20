package com.example.walkingmate.feature.music.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.walkingmate.R;
import com.chibde.visualizer.CircleVisualizer;
import com.example.walkingmate.feature.music.data.repository.MusicRepository;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
public class IntervalActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private MediaPlayer mediaPlayer;
    private TextToSpeech textToSpeech;
    private RadioGroup radioGroup;
    private TextView tempoTextView;
    private TextView filenameView;
    private CircleVisualizer mVisualizer;

    private int interval = 30000;  // 기본값 30초
    private boolean isLowBpm = true;
    private Handler handler = new Handler();

    private boolean isFetchingTempo = false;
    private boolean isFetchingFilename = false;
    private MusicRepository musicRepository;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interval);

        // 권한 요청
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);

        textToSpeech = new TextToSpeech(this, this);
        musicRepository = MusicRepository.getInstance();

        Button btnLowBpm = findViewById(R.id.btnLowBpm);
        Button btnHighBpm = findViewById(R.id.btnHighBpm);
        Button btnStop = findViewById(R.id.btnStop);
        tempoTextView = findViewById(R.id.tempoTextView);
        filenameView = findViewById(R.id.fileNameView);

        radioGroup = findViewById(R.id.radioGroup);
        RadioButton radio30s = findViewById(R.id.radio30s);
        RadioButton radio1m = findViewById(R.id.radio1m);
        RadioButton radio1m30s = findViewById(R.id.radio1m30s);
        mVisualizer = findViewById(R.id.blast);  // Visualizer 참조 가져오기

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio30s) {
                interval = 30000;
            } else if (checkedId == R.id.radio1m) {
                interval = 60000;
            } else if (checkedId == R.id.radio1m30s) {
                interval = 90000;
            }
        });

        btnLowBpm.setOnClickListener(v -> {
            isLowBpm = true;  // bpm 낮은 노래로 설정
            startMusicCycle();
        });

        btnHighBpm.setOnClickListener(v -> {
            isLowBpm = false;  // bpm 높은 노래로 설정
            startMusicCycle();
        });

        btnStop.setOnClickListener(v -> stopMusicCycle());
    }

    private void startMusicCycle() {
        handler.removeCallbacksAndMessages(null);  // 기존 콜백 제거

        // Visualizer 초기화
        if (mVisualizer != null) {
            safeReleaseVisualizer();  // 이전 시각화기를 해제
        }
        mVisualizer = findViewById(R.id.blast);  // 새로운 시각화기를 설정

        handler.post(new Runnable() {
            @Override
            public void run() {
                int repeatCount = interval / 30000;  // 30초 주기로 몇 번 반복할지 계산

                for (int i = 0; i < repeatCount; i++) {
                    final int currentIteration = i;  // i 값을 final로 선언하여 내부에서 사용
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (isLowBpm) {
                                speakAndPlay("천천히 걸으세요", musicRepository.getLowBpmUrl());
                            } else {
                                speakAndPlay("빠르게 걸으세요", musicRepository.getHighBpmUrl());
                            }

                            // 상태 변경은 한 주기가 끝나야만 수행하도록 함
                            if (currentIteration == repeatCount - 1) {
                                isLowBpm = !isLowBpm;
                            }
                        }
                    }, i * 30000);
                }

                // 주기 종료 후 다시 시작
                handler.postDelayed(this, interval);
            }
        });
    }

    private void speakAndPlay(String message, String url) {
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        textToSpeech.playSilentUtterance(3000, TextToSpeech.QUEUE_ADD, null);  // 3초 대기
        handler.postDelayed(() -> {
            fetchAndPlayMusic(url);
        }, 3000);  // TTS 후 3초 대기
    }

    private void fetchAndPlayMusic(String urlString) {
        ioExecutor.execute(() -> {
            File musicFile = downloadMusicFile(urlString);
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (musicFile != null) {
                    playMusic(musicFile);
                } else {
                    Toast.makeText(IntervalActivity.this, "Failed to download music", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private File downloadMusicFile(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            InputStream inputStream = connection.getInputStream();
            File musicFile = new File(getCacheDir(), "temp_music_file.mp3");
            FileOutputStream outputStream = new FileOutputStream(musicFile);

            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }

            outputStream.close();
            inputStream.close();

            return musicFile;
        } catch (Exception e) {
            Log.e("IntervalActivity", "Error fetching music", e);
            return null;
        }
    }

    private void stopMusicCycle() {
        handler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mVisualizer != null) {
            safeReleaseVisualizer();
            mVisualizer = null;
        }
    }

    private void fetchTempo() {
        if (isFetchingTempo) {
            return;  // 이전 요청이 완료되지 않은 경우 새 요청 실행하지 않음
        }

        isFetchingTempo = true;
        musicRepository.getCurrentTempo().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                isFetchingTempo = false;
                if (response.isSuccessful() && response.body() != null) {
                    int tempo = response.body().get("tempo").getAsInt();
                    tempoTextView.setText("Current Tempo: " + tempo);
                } else {
                    Toast.makeText(IntervalActivity.this, "Failed to fetch tempo", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                isFetchingTempo = false;
                if (!call.isCanceled()) {
                    Toast.makeText(IntervalActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 현재 파일 이름을 가져오는 요청
        fetchFileName();
    }

    private void fetchFileName() {
        if (isFetchingFilename) {
            return;  // 이전 요청이 완료되지 않은 경우 새 요청 실행하지 않음
        }

        isFetchingFilename = true;

        musicRepository.getCurrentFileName().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                isFetchingFilename = false;
                if (response.isSuccessful() && response.body() != null) {
                    String filename = response.body().get("filename").getAsString();
                    filenameView.setText("Current filename: " + filename);
                } else {
                    Toast.makeText(IntervalActivity.this, "Failed to fetch filename", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                isFetchingFilename = false;
                if (!call.isCanceled()) {
                    Toast.makeText(IntervalActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void playMusic(File musicFile) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(musicFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            // Visualizer 설정
            int audioSessionId = mediaPlayer.getAudioSessionId();
            if (audioSessionId != -1 && mVisualizer != null) {
                mVisualizer.setPlayer(audioSessionId);
            }

            mediaPlayer.setOnCompletionListener(mp -> { });

        } catch (Exception e) {
            Log.e("IntervalActivity", "Error playing music", e);
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.KOREAN);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Language not supported");
            }
        } else {
            Log.e("TTS", "Initialization failed");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (mVisualizer != null) {
            safeReleaseVisualizer();
        }
        handler.removeCallbacksAndMessages(null);
        ioExecutor.shutdownNow();
    }

    private void safeReleaseVisualizer() {
        try {
            mVisualizer.release();
        } catch (Exception e) {
            Log.w("IntervalActivity", "Visualizer release failed", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용된 경우 수행할 작업
            } else {
                Toast.makeText(this, "Audio record permission is required for visualizer", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

}
