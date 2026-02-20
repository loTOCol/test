package com.example.walkingmate.feature.auth.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.example.walkingmate.R;
import com.example.walkingmate.feature.feed.ui.WalkingHomeActivity;
import com.example.walkingmate.feature.user.data.UserData;

public class IntroSplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 900L;
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String PREF_USERID = "UserId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_splash);

        new Handler(Looper.getMainLooper()).postDelayed(this::routeNext, SPLASH_DURATION_MS);
    }

    private void routeNext() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String userId = sharedPreferences.getString(PREF_USERID, null);

        if (TextUtils.isEmpty(userId)) {
            UserData localUser = UserData.loadData(this);
            if (localUser != null && !TextUtils.isEmpty(localUser.userid)) {
                userId = localUser.userid;
            }
        }

        Intent nextIntent;
        if (!TextUtils.isEmpty(userId)) {
            nextIntent = new Intent(this, WalkingHomeActivity.class);
        } else {
            nextIntent = new Intent(this, StartActivity.class);
        }
        startActivity(nextIntent);
        finish();
    }
}
