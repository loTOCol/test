package com.example.walkingmate.feature.common.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.DatePicker;

import com.example.walkingmate.R;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class DateSelector extends Activity {

    int mYear =0, mMonth=0, mDay=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_date_selector);

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Calendar calendar = new GregorianCalendar();

        mYear = calendar.get(Calendar.YEAR);

        mMonth = calendar.get(Calendar.MONTH);

        mDay = calendar.get(Calendar.DAY_OF_MONTH);

        DatePicker datePicker = findViewById(R.id.vDatePicker);

        datePicker.init(mYear, mMonth, mDay,mOnDateChangedListener);
    }

    public void mOnClick(View v){

        Intent intent = new Intent();

        intent.putExtra("mYear",mYear);

        intent.putExtra("mMonth", mMonth+1);

        intent.putExtra("mDay", mDay);

        setResult(RESULT_OK, intent);

        finish();

    }

    DatePicker.OnDateChangedListener mOnDateChangedListener = new DatePicker.OnDateChangedListener(){

        @Override

        public void onDateChanged(DatePicker datePicker, int yyyy, int mm, int dd) {

            mYear = yyyy;

            mMonth = mm;

            mDay = dd;

        }

    };
}
