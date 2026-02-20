package com.example.walkingmate.feature.music.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.walkingmate.R;
import com.example.walkingmate.feature.music.adapter.MusicAdapter;


import java.util.ArrayList;

public class MusicItemActivity extends AppCompatActivity {

    ImageButton tv_previous;
    Intent intent;
    Integer number;
    String title;
    MusicAdapter musicAdapter;
    ArrayList<MusicAdapter.MusicItem> musicItems;
    RecyclerView recyclerView;
    TextView tvCategoryTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_item_list);

        tv_previous = findViewById(R.id.btnPrevious);
        tvCategoryTitle = findViewById(R.id.tvCategoryTitle);

        tv_previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        recyclerView = findViewById(R.id.music_list_view);

        //음악 리스트 어댑터 연결
        musicItems = new ArrayList<>();
        musicAdapter =new MusicAdapter(musicItems);
        LinearLayoutManager layoutManager=new LinearLayoutManager(getApplicationContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(musicAdapter);

        intent = getIntent();
        number = intent.getIntExtra("number", -1);
        title = intent.getStringExtra("title");

        switch (number){
            case 0: //느린 템포
                tvCategoryTitle.setText("느린 템포 음악 목록");
                musicItems.add(new MusicAdapter.MusicItem("White Christmas",  R.drawable.chrimas, 0));
                musicItems.add(new MusicAdapter.MusicItem("Memories of the City",  R.drawable.cheer_new, 0));
                musicItems.add(new MusicAdapter.MusicItem("Sea Bottom Segue",  R.drawable.sea, 0));
                musicItems.add(new MusicAdapter.MusicItem("Peaceful Snow",  R.drawable.cheer_sax, 0));
                musicItems.add(new MusicAdapter.MusicItem("Shaboozey",  R.drawable.guitar, 0));
                break;

            case 1: //중간 템포
                tvCategoryTitle.setText("중간 템포 음악 목록");
                musicItems.add(new MusicAdapter.MusicItem("Deltarune Chapter",  R.drawable.delt,1));
                musicItems.add(new MusicAdapter.MusicItem("Feel the Heat",  R.drawable.feel,1));

                musicItems.add(new MusicAdapter.MusicItem("Flower Dance",  R.drawable.flowerdance,1));
                musicItems.add(new MusicAdapter.MusicItem("Friday Night Funkin",  R.drawable.friday,1));

                musicItems.add(new MusicAdapter.MusicItem("Half-Life 2",  R.drawable.exciting_passion,1));
                musicItems.add(new MusicAdapter.MusicItem("In the City",  R.drawable.exciting_rainbow,1));

                musicItems.add(new MusicAdapter.MusicItem("Lesion X",  R.drawable.exciting_passion,1));
                musicItems.add(new MusicAdapter.MusicItem("Nights Pinball",  R.drawable.exciting_rainbow,1));

                musicItems.add(new MusicAdapter.MusicItem("Rusty Ruin Zone",  R.drawable.exciting_passion,1));
                musicItems.add(new MusicAdapter.MusicItem("Satomi Springs",  R.drawable.exciting_rainbow,1));

                musicItems.add(new MusicAdapter.MusicItem("Steel Red",  R.drawable.exciting_passion,1));
                musicItems.add(new MusicAdapter.MusicItem("Super Bell Hill",  R.drawable.exciting_rainbow,1));
                musicItems.add(new MusicAdapter.MusicItem("Training Forest",  R.drawable.exciting_rainbow,1));
                break;

            case 2: //빠른 템포
                tvCategoryTitle.setText("빠른 템포 음악 목록");
                musicItems.add(new MusicAdapter.MusicItem("498 Tokio",  R.drawable.piano_lullaby,2));
                musicItems.add(new MusicAdapter.MusicItem("Deathsmiles",  R.drawable.piano_heaven,2));
                musicItems.add(new MusicAdapter.MusicItem("Demolition_and_Destruction",  R.drawable.piano_dream,2));
                musicItems.add(new MusicAdapter.MusicItem("Casino_Park",  R.drawable.piano_express,2));
                musicItems.add(new MusicAdapter.MusicItem("Frozen Factory",  R.drawable.piano_sonata,2));
                musicItems.add(new MusicAdapter.MusicItem("Infinite",  R.drawable.piano_river,2));
                musicItems.add(new MusicAdapter.MusicItem("Keys the Ruin",  R.drawable.piano_lullaby,2));
                musicItems.add(new MusicAdapter.MusicItem("Metallic Madness Zone",  R.drawable.piano_heaven,2));
                musicItems.add(new MusicAdapter.MusicItem("Nobody",  R.drawable.piano_dream,2));
                musicItems.add(new MusicAdapter.MusicItem("Space Step Flow",  R.drawable.piano_express,2));
                musicItems.add(new MusicAdapter.MusicItem("Thunderzone 2",  R.drawable.piano_sonata,2));
                musicItems.add(new MusicAdapter.MusicItem("Tuning",  R.drawable.piano_river,2));
                break;

            case 3: //매우 빠른 템포
                tvCategoryTitle.setText("매우 빠른 템포 음악 목록");
                musicItems.add(new MusicAdapter.MusicItem("A_Quiet_Thought",  R.drawable.comfort_quiet,3));
                musicItems.add(new MusicAdapter.MusicItem("Hyperspace",  R.drawable.comfort_winter,3));
                musicItems.add(new MusicAdapter.MusicItem("Planet_Belligerence",  R.drawable.comfort_dawn,3));
                musicItems.add(new MusicAdapter.MusicItem("Gaur_Plain",  R.drawable.comfort_rainbow,3));
                break;
            default:
                tvCategoryTitle.setText("음악 목록");
                break;


        }


    }
}
