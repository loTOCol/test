package com.example.walkingmate.feature.feed.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.Window;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.example.walkingmate.R;

import java.util.ArrayList;

public class SelectFeedActivity extends Activity {

    LinearLayout weather,emotion;
    ImageButton[] emotions;
    ImageButton[] weathers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_select_feed);

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        weather=findViewById(R.id.weatherlayout);
        emotion=findViewById(R.id.emotionlayout);

        Intent intent=getIntent();
        int action=intent.getIntExtra("action",-1);
        if(action==2){
            weather.setVisibility(View.INVISIBLE);
        }
        else{
            emotion.setVisibility(View.INVISIBLE);
        }

        emotions=new ImageButton[]{findViewById(R.id.angry),findViewById(R.id.crying),findViewById(R.id.heart),
                findViewById(R.id.neutral),findViewById(R.id.smile),findViewById(R.id.tired)};

        weathers= new ImageButton[]{findViewById(R.id.cloudy), findViewById(R.id.fog), findViewById(R.id.moon), findViewById(R.id.rainbow),
                findViewById(R.id.rainy), findViewById(R.id.snow), findViewById(R.id.sunny), findViewById(R.id.windy)};

        for(int i=0; i<emotions.length; ++i){
            ImageButton tmp=emotions[i];
            int tmpnum=i;
            tmp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent emotionintent=new Intent();
                    emotionintent.putExtra("emotion",tmpnum);
                    setResult(RESULT_OK,emotionintent);
                    finish();
                }
            });
        }

        for(int i=0; i<weathers.length; ++i){
            ImageButton tmp=weathers[i];
            int tmpnum=i;
            tmp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent weatherintent=new Intent();
                    weatherintent.putExtra("weather",tmpnum);
                    setResult(RESULT_OK,weatherintent);
                    finish();
                }
            });
        }

    }
}
