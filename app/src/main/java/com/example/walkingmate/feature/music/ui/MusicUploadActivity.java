package com.example.walkingmate.feature.music.ui;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.walkingmate.R;

public class MusicUploadActivity extends AppCompatActivity {

    private static final int PICK_AUDIO_REQUEST = 1;
    private static final int REQUEST_PERMISSION_CODE = 100;
    private static final String PREFS_NAME = "MusicUploadActivityPrefs";
    private static final String KEY_MUSIC_URI = "music_uri";

    private Uri currentMusicUri = null;
    private MediaPlayer mediaPlayer;
    private TextView musicInfo;

    private String getReadAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_AUDIO;
        }
        return Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_upload);

        musicInfo = findViewById(R.id.music_info);
        Button uploadButton = findViewById(R.id.upload_music_button);
        Button playButton = findViewById(R.id.play_music_button);

        // 권한 확인 및 요청
        String readAudioPermission = getReadAudioPermission();
        if (ContextCompat.checkSelfPermission(this, readAudioPermission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{readAudioPermission}, REQUEST_PERMISSION_CODE);
        }

        // SharedPreferences에서 음악 URI를 복원
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String musicUriString = preferences.getString(KEY_MUSIC_URI, null);
        if (musicUriString != null) {
            currentMusicUri = Uri.parse(musicUriString);
            String musicTitle = getMusicTitle(currentMusicUri);
            musicInfo.setText("업로드된 음악: " + musicTitle);
        }

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("audio/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                startActivityForResult(intent, PICK_AUDIO_REQUEST);
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentMusicUri != null) {
                    // MusicRecommendActivity의 메서드를 직접 호출
                    MusicRecommendActivity.setMusicUri(currentMusicUri);
                } else {
                    Toast.makeText(MusicUploadActivity.this, "음악이 업로드되지 않았습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });
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

            final int takeFlags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
            try {
                getContentResolver().takePersistableUriPermission(selectedAudioUri, takeFlags);
            } catch (SecurityException ignored) {
            }

            if (currentMusicUri != null) {
                // 이전 음악 파일 삭제 로직 (필요시)
            }
            currentMusicUri = selectedAudioUri;
            String musicTitle = getMusicTitle(selectedAudioUri);
            musicInfo.setText("업로드된 음악: " + musicTitle);

            // SharedPreferences에 음악 URI 저장
            SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_MUSIC_URI, currentMusicUri.toString());
            editor.apply();
        }
    }

    private String getMusicTitle(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                if (titleIndex >= 0) {
                    return cursor.getString(titleIndex);
                }
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    return cursor.getString(nameIndex);
                }
            }
        } catch (SecurityException ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "제목을 가져올 수 없습니다";
    }

    private void playMusic(Uri musicUri) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(this, musicUri);
        mediaPlayer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용됨
                Toast.makeText(this, "권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                // 권한이 거부됨
                Toast.makeText(this, "권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
