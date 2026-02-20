package com.example.walkingmate.feature.common.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TimePicker;

import com.example.walkingmate.R;

public class TimeSelector extends Activity {

    int mHour=0,mMinute=0;

    TimePicker time_picker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_time_selector);

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        time_picker=findViewById(R.id.time_picker);
        mHour=time_picker.getHour();
        mMinute=time_picker.getMinute();
        time_picker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker timePicker, int i, int i1) {
                mHour=i;
                mMinute=i1;
            }
        });


    }

    public void mtimeOnClick(View v){

        Intent intent = new Intent();

        intent.putExtra("mHour",mHour);

        intent.putExtra("mMinute", mMinute);

        setResult(RESULT_OK, intent);

        finish();

    }

}
