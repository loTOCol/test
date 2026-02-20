package com.example.walkingmate.feature.music.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import com.example.walkingmate.feature.music.data.model.BpmResponse;
import com.example.walkingmate.feature.music.data.repository.MusicRepository;
import com.example.walkingmate.feature.walk.ui.MeasureSpeedActivity;

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
public class MusicService extends Service implements TextToSpeech.OnInitListener {

    private MediaPlayer mediaPlayer;
    private TextToSpeech textToSpeech;
    private Handler handler = new Handler();
    private int interval = 30000; // 기본값 30초
    private double MyBPM = MeasureSpeedActivity.bpm; // 현재 BPM 값
    private boolean isLowBpm = true;

    private MusicRepository musicRepository;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        textToSpeech = new TextToSpeech(this, this);
        musicRepository = MusicRepository.getInstance();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getAction().equals("START_MUSIC")) {
                Log.d("MusicService", "START_MUSIC action received");
                MyBPM = intent.getDoubleExtra("BPM", MyBPM);
                interval = intent.getIntExtra("INTERVAL", 30000);  // 사용자가 선택한 간격을 설정
                startMusicCycle();
            }
            else if (intent.getAction().equals("STOP_MUSIC")) {
                Log.d("MusicService", "STOP_MUSIC action received");
                stopMusicCycle();
                stopSelf(); // 서비스 자체도 종료
            }
        }
        return START_NOT_STICKY;
    }


    private void startMusicCycle() {
        handler.post(new Runnable() {
            private boolean isSlow = true;
            private boolean firstRun = true;

            @Override
            public void run() {
                if (firstRun) {
                    speakAndPlay(isSlow ? "천천히 걸으세요" : "빠르게 걸으세요");
                    firstRun = false;
                } else {
                    isSlow = !isSlow;  // 상태 변경
                    if (isSlow) {
                        MyBPM -= 50;  // BPM 감소
                        speakAndPlay("천천히 걸으세요");
                    } else {
                        MyBPM += 50;  // BPM 증가
                        speakAndPlay("빠르게 걸으세요");
                    }
                }
                handler.postDelayed(this, interval);  // 사용자가 선택한 간격으로 반복 실행
            }
        });
    }

    private void speakAndPlay(String message) {
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);

        handler.postDelayed(() -> {
            sendBpmToServer(MyBPM);
        }, 3000); // TTS 후 3초 대기
    }

    private void stopMusicCycle() {
        handler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            Log.d("MusicService", "Stopping media player");
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        Log.d("MusicService", "Music cycle stopped");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        ioExecutor.shutdownNow();
        Log.d("MusicService", "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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

    private void sendBpmToServer(double bpm) {
        Call<BpmResponse> call = musicRepository.sendBpm((int) bpm);
        call.enqueue(new Callback<BpmResponse>() {
            @Override
            public void onResponse(Call<BpmResponse> call, Response<BpmResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String fileTitle = response.body().getFileTitle();
                    if (fileTitle != null && !fileTitle.isEmpty()) {
                        String fileUrl = musicRepository.getMusicFileUrl(fileTitle);
                        downloadAndPlayMusic(fileUrl);
                    } else {
                        Toast.makeText(MusicService.this, "Error: No file title returned", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MusicService.this, "Request failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BpmResponse> call, Throwable t) {
                Toast.makeText(MusicService.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadAndPlayMusic(String fileUrl) {
        ioExecutor.execute(() -> {
            File musicFile = downloadMusicFile(fileUrl);
            handler.post(() -> {
                if (musicFile != null && musicFile.exists()) {
                    playMusic(musicFile);
                } else {
                    Toast.makeText(MusicService.this, "Failed to download music", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private File downloadMusicFile(String urlString) {
        File musicFile = new File(getCacheDir(), "downloaded_music.wav");
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            InputStream inputStream = connection.getInputStream();
            FileOutputStream outputStream = new FileOutputStream(musicFile);

            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }

            outputStream.close();
            inputStream.close();

        } catch (Exception e) {
            Log.e("MusicService", "Error downloading music", e);
            return null;
        }
        return musicFile;
    }

    private void playMusic(File musicFile) {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(mp -> {
                if (interval > 30000) {  // 60초 또는 90초인 경우
                    sendBpmToServer(MyBPM);  // 동일한 BPM으로 추가 재생
                }
            });
        }

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(musicFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(MusicService.this, "Playing music", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("MusicService", "Error playing music", e);
            Toast.makeText(MusicService.this, "Failed to play music", Toast.LENGTH_SHORT).show();
        }
    }

}
