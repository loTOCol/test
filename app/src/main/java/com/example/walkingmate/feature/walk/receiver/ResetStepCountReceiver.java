package com.example.walkingmate.feature.walk.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class ResetStepCountReceiver extends BroadcastReceiver {
    private static final String PREFS_NAME = "WalkingMatePrefs";
    private static final String STEP_COUNT_KEY = "currentStepCount";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(STEP_COUNT_KEY, 0);
        editor.apply();
    }
}
