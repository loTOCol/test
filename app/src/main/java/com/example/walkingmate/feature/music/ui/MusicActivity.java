package com.example.walkingmate.feature.music.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.walkingmate.R;
import com.example.walkingmate.feature.music.adapter.MusicListAdapter;

import java.util.ArrayList;

public class MusicActivity extends AppCompatActivity {

    MusicListAdapter musicListAdapter;
    ArrayList<MusicListAdapter.MusicCategory> music_itemData;
    RecyclerView music_recycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        music_recycler = findViewById(R.id.recycler_music);

        // 음악 리스트 어댑터 연결
        music_itemData = new ArrayList<>();
        musicListAdapter = new MusicListAdapter(music_itemData);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL); // 세로 방향
        music_recycler.setLayoutManager(layoutManager);
        music_recycler.setAdapter(musicListAdapter);

        // 음악 카테고리 목록
        music_itemData.add(new MusicListAdapter.MusicCategory("느린 템포 (BPM 90이하)", R.drawable.bpm90));
        music_itemData.add(new MusicListAdapter.MusicCategory("중간 템포 (BPM 91~120)", R.drawable.bpm120));
        music_itemData.add(new MusicListAdapter.MusicCategory("빠른 템포 (BPM 120~150)", R.drawable.bpm150));
        music_itemData.add(new MusicListAdapter.MusicCategory("매우 빠른 템포 (BPM 151이상)", R.drawable.bpm151));
    }
}
