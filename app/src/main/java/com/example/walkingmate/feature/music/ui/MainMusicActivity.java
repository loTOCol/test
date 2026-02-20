package com.example.walkingmate.feature.music.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.example.walkingmate.R;

public class MainMusicActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_home);

        ImageButton firstFloorButton = findViewById(R.id.image_button_first_floor);
        ImageButton secondFloorButton = findViewById(R.id.image_button_second_floor);
        ImageButton thirdFloorButton = findViewById(R.id.image_button_third_floor);
        firstFloorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("MainMusicActivity", "First floor button clicked");
                Intent intent = new Intent(MainMusicActivity.this, MusicActivity.class);
                startActivity(intent);
            }
        });

        secondFloorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMusicActivity.this, IntervalActivity.class);
                startActivity(intent);
            }
        });

        thirdFloorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMusicActivity.this, MusicRecommendActivity.class);
                startActivity(intent);
            }
        });
    }
}
