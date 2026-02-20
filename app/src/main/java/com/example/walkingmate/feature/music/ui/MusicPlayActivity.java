package com.example.walkingmate.feature.music.ui;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.walkingmate.R;
import com.bumptech.glide.Glide;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MusicPlayActivity extends AppCompatActivity {

    SeekBar progressBar;
    CircularImageView musicPlay_img;
    TextView tv_title, tv_playingTime, tv_playTime, tv_category, tvPaceStamp, tvPaceLabel, tvPaceDesc;
    ImageView ivMascotWalk;
    ImageButton btnPlay, btnPlayNext, btnPlayPre, btnPrevious;

    MediaPlayer mediaPlayer;
    int currentPos = 0, category = 0;
    boolean isPlaying = false;
    boolean isUserSeeking = false;
    private ObjectAnimator mascotPulseAnimator;
    private ObjectAnimator mascotPulseAnimatorY;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null) {
                int time = mediaPlayer.getCurrentPosition();
                if (!isUserSeeking) {
                    progressBar.setProgress(time);
                }
                tv_playingTime.setText(formatTime(time));
            }
            progressHandler.postDelayed(this, 500);
        }
    };

    private static final String CHANNEL_ID = "music_channel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_play);

        // Initialize views
        progressBar = findViewById(R.id.music_progressbar);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnPlay = findViewById(R.id.btn_music_play);
        btnPlayNext = findViewById(R.id.btn_music_next);
        btnPlayPre = findViewById(R.id.btn_music_pre);
        musicPlay_img = findViewById(R.id.musicPlay_img);
        ivMascotWalk = findViewById(R.id.ivMascotWalk);

        tv_title = findViewById(R.id.music_name);
        tv_title.setSingleLine(true);
        tv_title.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        tv_title.setSelected(true);

        tv_playingTime = findViewById(R.id.tv_playingTime);
        tv_playTime = findViewById(R.id.tv_playTime);
        tv_category = findViewById(R.id.music_category);
        tvPaceStamp = findViewById(R.id.tv_pace_stamp);
        tvPaceLabel = findViewById(R.id.tv_pace_label);
        tvPaceDesc = findViewById(R.id.tv_pace_desc);

        // Load data from Intent
        Intent intent = getIntent();
        category = intent.getIntExtra("category", 0);
        currentPos = intent.getIntExtra("position", 0);
        String title = intent.getStringExtra("title");

        // Start playing music
        setupMediaPlayer();

        btnPrevious.setOnClickListener(view -> {
            stopMusic();
            finish();
        });

        btnPlay.setOnClickListener(view -> {
            if (isPlaying) {
                pauseMusic();
            } else {
                startMusic();
            }
        });

        btnPlayPre.setOnClickListener(view -> {
            playPrevious();
        });

        btnPlayNext.setOnClickListener(view -> {
            playNext();
        });

        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tv_playingTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(seekBar.getProgress());
                }
            }
        });

        createNotificationChannel();
        showNotification();

        // BroadcastReceiver 등록
        registerReceiver(musicControlReceiver, new IntentFilter("PLAY_PAUSE"));
        registerReceiver(musicControlReceiver, new IntentFilter("NEXT"));
        registerReceiver(musicControlReceiver, new IntentFilter("PREV"));
        registerReceiver(musicControlReceiver, new IntentFilter("STOP"));
        progressHandler.post(progressRunnable);
    }

    private void setupMediaPlayer() {
        int[] currentSongs = getCurrentSongs();
        Integer[] currentImages = getCurrentImages();
        String[] currentTitles = getCurrentTitles();

        mediaPlayer = MediaPlayer.create(getApplicationContext(), currentSongs[currentPos]);
        tv_title.setText(currentTitles[currentPos]);
        tv_category.setText(getCategoryString());
        Glide.with(this).load(currentImages[currentPos]).into(musicPlay_img);
        applyPaceStampAndMascot();

        progressBar.setProgress(0);
        progressBar.setMax(mediaPlayer.getDuration());
        tv_playTime.setText(formatTime(mediaPlayer.getDuration()));

        mediaPlayer.setOnCompletionListener(mp -> playNext());

        startMusic();
    }

    private void applyPaceStampAndMascot() {
        int gifRes;
        String stamp;
        String desc;
        long pulseDurationMs;
        switch (category) {
            case 0:
                stamp = "LOW";
                gifRes = R.drawable.animal_walk1_with;
                desc = "천천히 걷기 템포";
                pulseDurationMs = 1400L;
                break;
            case 1:
                stamp = "MID";
                gifRes = R.drawable.animal_walk2_with;
                desc = "리듬 유지 걷기 템포";
                pulseDurationMs = 1100L;
                break;
            case 2:
                stamp = "FAST";
                gifRes = R.drawable.animal_walk3_with;
                desc = "빠르게 걷기 템포";
                pulseDurationMs = 850L;
                break;
            case 3:
                stamp = "MAX";
                gifRes = R.drawable.walkgif;
                desc = "고강도 파워 워킹 템포";
                pulseDurationMs = 650L;
                break;
            default:
                stamp = "PACE";
                gifRes = R.drawable.animal_walk1_with;
                desc = "현재 템포 안내";
                pulseDurationMs = 1200L;
                break;
        }
        tvPaceStamp.setText(stamp);
        tvPaceLabel.setText("현재 페이스: " + stamp);
        tvPaceDesc.setText(desc);
        Glide.with(this).asGif().load(gifRes).into(ivMascotWalk);
        startMascotPulse(pulseDurationMs);
    }

    private void startMascotPulse(long durationMs) {
        if (mascotPulseAnimator != null) {
            mascotPulseAnimator.cancel();
        }
        if (mascotPulseAnimatorY != null) {
            mascotPulseAnimatorY.cancel();
        }
        mascotPulseAnimator = ObjectAnimator.ofFloat(ivMascotWalk, "scaleX", 1.0f, 1.08f, 1.0f);
        mascotPulseAnimator.setDuration(durationMs);
        mascotPulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        mascotPulseAnimator.start();

        mascotPulseAnimatorY = ObjectAnimator.ofFloat(ivMascotWalk, "scaleY", 1.0f, 1.08f, 1.0f);
        mascotPulseAnimatorY.setDuration(durationMs);
        mascotPulseAnimatorY.setRepeatCount(ObjectAnimator.INFINITE);
        mascotPulseAnimatorY.start();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Music Playback";
            String description = "Channel for music playback controls";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification() {
        Intent contentIntent = new Intent(this, MusicPlayActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent playPauseIntent = PendingIntent.getBroadcast(this, 0, new Intent("PLAY_PAUSE"), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent nextIntent = PendingIntent.getBroadcast(this, 0, new Intent("NEXT"), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent prevIntent = PendingIntent.getBroadcast(this, 0, new Intent("PREV"), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stopIntent = PendingIntent.getBroadcast(this, 0, new Intent("STOP"), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.start_tracking)
                .setContentTitle(tv_title.getText())
                .setContentText(tv_category.getText())
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(1, 2, 3) // Show Play/Pause, Next, and Stop buttons in compact view
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(stopIntent)) // Change to stopIntent
                .addAction(R.drawable.btn_play_pre, "Previous", prevIntent)
                .addAction(isPlaying ? R.drawable.pause_icon : R.drawable.btn_play, "Play/Pause", playPauseIntent)
                .addAction(R.drawable.btn_play_next, "Next", nextIntent)
                .addAction(R.drawable.stop_icon, "Stop", stopIntent) // Add stop action
                .setPriority(NotificationCompat.PRIORITY_LOW);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private int[] getCurrentSongs() {
        return LocalMusicCatalog.songsFor(category);
    }

    private Integer[] getCurrentImages() {
        return LocalMusicCatalog.imagesFor(category);
    }

    private String[] getCurrentTitles() {
        return LocalMusicCatalog.titlesFor(category);
    }

    private String getCategoryString() {
        switch (category) {
            case 0:
                return "느린 템포";
            case 1:
                return "중간 템포";
            case 2:
                return "빠른 템포";
            case 3:
                return "매우 빠른 템포";
            default:
                return "";
        }
    }

    private void startMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            isPlaying = true;
            btnPlay.setImageResource(R.drawable.btn_pause);
            showNotification(); // Update notification
        }
    }


    private void pauseMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            isPlaying = false;
            btnPlay.setImageResource(R.drawable.btn_play);
            showNotification(); // Update notification
        }
    }

    private void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
            isPlaying = false;
            btnPlay.setImageResource(R.drawable.btn_play);
            NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID); // Remove the notification
        }
    }


    private void playPrevious() {
        if (mediaPlayer != null) {
            stopMusic();
            currentPos = (currentPos - 1 + getCurrentSongs().length) % getCurrentSongs().length;
            setupMediaPlayer();
        }
    }

    private void playNext() {
        if (mediaPlayer != null) {
            stopMusic();
            currentPos = (currentPos + 1) % getCurrentSongs().length;
            setupMediaPlayer();
        }
    }

    private String formatTime(int milliseconds) {
        SimpleDateFormat sdf = new SimpleDateFormat("m:ss");
        Date time = new Date(milliseconds);
        return sdf.format(time);
    }

    @Override
    public void onBackPressed() {
        stopMusic();
        super.onBackPressed();
    }

    private final BroadcastReceiver musicControlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "PLAY_PAUSE":
                        if (isPlaying) {
                            pauseMusic();
                        } else {
                            startMusic();
                        }
                        break;
                    case "NEXT":
                        playNext();
                        break;
                    case "PREV":
                        playPrevious();
                        break;
                    case "STOP":
                        stopMusic();
                        break;
                }
            }
        }
    };



    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressHandler.removeCallbacksAndMessages(null);
        if (mascotPulseAnimator != null) {
            mascotPulseAnimator.cancel();
            mascotPulseAnimator = null;
        }
        if (mascotPulseAnimatorY != null) {
            mascotPulseAnimatorY.cancel();
            mascotPulseAnimatorY = null;
        }
        // Unregister BroadcastReceivers
        unregisterReceiver(musicControlReceiver);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        // Cancel the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(NOTIFICATION_ID);
    }



}


