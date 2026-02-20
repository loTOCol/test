package com.example.walkingmate.feature.walk.ui;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.walkingmate.R;
import com.example.walkingmate.feature.user.data.UserData;
import com.example.walkingmate.core.network.WeatherManager;
import com.example.walkingmate.feature.walk.receiver.ResetStepCountReceiver;
import com.example.walkingmate.feature.shop.data.CoinManager;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import in.akshit.horizontalcalendar.HorizontalCalendarView;
import in.akshit.horizontalcalendar.Tools;

public class WalkFragment extends Fragment implements SensorEventListener, android.location.LocationListener {
    DatabaseReference dr = FirebaseDatabase.getInstance().getReference("walk");
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String PREFS_NAME = "WalkingMatePrefs";
    private static final String STEP_COUNT_KEY = "currentStepCount";
    private static final String STEP_COUNT_DATE_KEY_PREFIX = "stepCount_";
    private static final String GOAL_STEP_COUNT_KEY = "goalStepCount";
    UserData userData;
    private CircularProgressBar circularProgressBar;
    private TextView pointsTextView;
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    public static int currentStepCount = 0;
    private int targetStepCount = 1000;

    private TextView currentStepsTextView;
    private TextView goalStepsTextView;
    private LocationManager locationManager;
    private TextView caloriesTextView;
    private ImageView gifImageView;
    private ImageView weatherImageView;
    private TextView temperatureTextView;
    private SharedPreferences sharedPreferences;

    HorizontalCalendarView calendarView;

    private TextView comparisonTextView;
    private TextView stepsDifferenceTextView;

    private int previousDayStepCount = 0; // 전날 걸음 수

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_walk, container, false);

        CoinManager.initialize(getContext());
        circularProgressBar = root.findViewById(R.id.pieChart);
        gifImageView = root.findViewById(R.id.gifImageView);
        currentStepsTextView = root.findViewById(R.id.currentSteps);
        goalStepsTextView = root.findViewById(R.id.goalSteps);
        caloriesTextView = root.findViewById(R.id.calories);
        weatherImageView = root.findViewById(R.id.weatherImageView);

        pointsTextView = root.findViewById(R.id.points);
        temperatureTextView = root.findViewById(R.id.temperatureTextView);

        calendarView = root.findViewById(R.id.calendar);

        comparisonTextView = root.findViewById(R.id.comparisonTextView); // 새로 추가된 TextView
        stepsDifferenceTextView = root.findViewById(R.id.stepsDifferenceTextView); // 새로 추가된 TextView

        Calendar starttime = Calendar.getInstance();
        starttime.add(Calendar.MONTH, -6);

        Calendar endtime = Calendar.getInstance();
        endtime.add(Calendar.MONTH, 6);

        ArrayList<String> datesToBeColored = new ArrayList<>();
        datesToBeColored.add(Tools.getFormattedDateToday());



        calendarView.setUpCalendar(starttime.getTimeInMillis(),
                endtime.getTimeInMillis(),
                datesToBeColored,
                new HorizontalCalendarView.OnCalendarListener() {
                    @Override
                    public void onDateSelected(String date) {

                        // 날짜 선택 시 걸음 수와 소모 칼로리 업데이트
                        int selectedDaySteps = getStepCountForDate(date);
                        int selectedDayCalories = calculateCaloriesBurned(selectedDaySteps);
                        currentStepsTextView.setText(String.valueOf(selectedDaySteps));
                        caloriesTextView.setText(String.valueOf(selectedDayCalories));


                    }
                });

        sharedPreferences = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        targetStepCount = sharedPreferences.getInt(GOAL_STEP_COUNT_KEY, 1000);
        if (targetStepCount <= 0) {
            targetStepCount = 1000;
        }
        updateGoalStepsLabel();
        goalStepsTextView.setOnClickListener(v -> showGoalStepInputDialog());
        loadSelectedGif();

        setupCircularProgressBar();
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
        }

        loadStepCount();
        loadPreviousDayStepCount(); // 전날 걸음 수 로드
        setupDailyResetAlarm();
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        if (locationManager != null) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        // userdata 가져오기
        userData = UserData.loadData(getActivity());
        String userid = userData.userid;

        // Firebase에 초기 값 설정
        updateFirebase(userid, currentStepCount, calculateCaloriesBurned(currentStepCount));
        updateCoinBalance();

        return root;
    }

    private void loadSelectedGif() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("CoinShopPrefs", Context.MODE_PRIVATE);
        int selectedGifResId = sharedPreferences.getInt("selected_walking_gif", R.drawable.walkgif);
        Glide.with(this).asGif().load(selectedGifResId).into(gifImageView);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
        loadStepCount();  // 현재 날짜의 걸음 수 불러오기
        loadPreviousDayStepCount(); // 이전 날의 걸음 수 불러오기
        updateComparisonTextView(); // 걸음 수 비교 업데이트
        updateCoinBalance();


    }


    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        saveStepCount();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        saveStepCount();
    }

    private void setupCircularProgressBar() {
        circularProgressBar.setProgressMax(targetStepCount);
        circularProgressBar.setProgress(currentStepCount);
        circularProgressBar.setBackgroundProgressBarColor(Color.parseColor("#FFE9D8C0"));
        circularProgressBar.setBackgroundProgressBarWidth(14f);
        circularProgressBar.setProgressBarColor(Color.parseColor("#FFCD8A49"));
        circularProgressBar.setProgressBarWidth(16f);
        circularProgressBar.setRoundBorder(true);

        updateCircularProgressBar(currentStepCount);
    }

    private void updateGoalStepsLabel() {
        if (goalStepsTextView != null) {
            goalStepsTextView.setText("목표 걸음 수: " + targetStepCount);
        }
    }

    private void showGoalStepInputDialog() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_goal_steps_input);
        EditText input = dialog.findViewById(R.id.et_goal_steps);
        Button cancel = dialog.findViewById(R.id.btn_goal_input_cancel);
        Button apply = dialog.findViewById(R.id.btn_goal_input_apply);

        input.setText(String.valueOf(targetStepCount));
        input.setSelection(input.getText().length());

        cancel.setOnClickListener(v -> dialog.dismiss());
        apply.setOnClickListener(v -> {
            String raw = input.getText().toString().trim();
            if (raw.isEmpty()) {
                Toast.makeText(context, "목표 걸음 수를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            int newGoal;
            try {
                newGoal = Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                Toast.makeText(context, "숫자만 입력해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newGoal < 100 || newGoal > 100000) {
                Toast.makeText(context, "100~100000 사이로 입력해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            targetStepCount = newGoal;
            sharedPreferences.edit().putInt(GOAL_STEP_COUNT_KEY, targetStepCount).apply();
            updateGoalStepsLabel();
            setupCircularProgressBar();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateCircularProgressBar(int steps) {
        circularProgressBar.setProgress(steps);
        currentStepsTextView.setText(String.valueOf(steps)); // 숫자만 표시

        // 칼로리 계산 및 텍스트 뷰 업데이트
        int caloriesBurned = calculateCaloriesBurned(steps);
        caloriesTextView.setText(String.valueOf(caloriesBurned));

        if (steps >= targetStepCount) {
            CoinManager.addCoins(100);
            updateCoinBalance(); // 코인 잔액 업데이트
        }

        // userdata 가져오기
        userData = UserData.loadData(getActivity());
        String userid = userData.userid;

        // Firebase에 업데이트
        updateFirebase(userid, steps, calculateCaloriesBurned(steps));
    }

    private int calculateCaloriesBurned(int steps) {
        // 1,000 걸음당 약 45 칼로리 소모
        return (int) (steps / 1000.0 * 45);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            if (event.values[0] == 1.0f) {
                currentStepCount++;
            }
            String todayDateStr = Tools.getFormattedDateToday();
            saveStepCountForDate(todayDateStr, currentStepCount);
            updateCircularProgressBar(currentStepCount);
            updateComparisonTextView(); // 어제와 비교하여 업데이트

        }
    }
    private void loadPreviousDayStepCount() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1); // 전날
        String previousDayDateStr = getFormattedDate(calendar); // 전날 날짜를 문자열로 변환
        previousDayStepCount = getStepCountForDate(previousDayDateStr);
    }

    private void updateComparisonTextView() {
        // 걸음 수 차이 계산
        int stepsDifference = currentStepCount - previousDayStepCount;

        if (stepsDifference > 0) {
            stepsDifferenceTextView.setText("+" + stepsDifference + " 걸음");
        } else if (stepsDifference < 0) {
            stepsDifferenceTextView.setText("-" + Math.abs(stepsDifference) + " 걸음");
        } else {
            stepsDifferenceTextView.setText("0 걸음");
        }
    }


    private String getFormattedDate(Calendar calendar) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(calendar.getTime());
    }

    private String getFormattedDateToday() {
        Calendar calendar = Calendar.getInstance();
        return getFormattedDate(calendar);
    }





    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Context context = getActivity(); // Use getActivity() to get the parent Activity context
        if (context != null) {
            WeatherManager.getWeather(context, location.getLatitude(), location.getLongitude(), new WeatherManager.WeatherCallback() {
                @Override
                public void onWeatherLoaded(String weatherIcon, String temperature) {
                    int weatherIconResId = getWeatherIconResId(weatherIcon);
                    weatherImageView.setImageResource(weatherIconResId);
                    temperatureTextView.setText(temperature);
                }
            });
        }
    }

    private void updateCoinBalance() {
        int coinBalance = CoinManager.getCoins();
        pointsTextView.setText(String.valueOf(coinBalance));
    }

    private int getWeatherIconResId(String weatherIcon) {
        switch (weatherIcon) {
            case "01d": // 맑은 하늘 낮
            case "01n": // 맑은 하늘 밤
                return R.drawable.weather_sunny;
            case "02d": // 구름 조금 낮
            case "02n": // 구름 조금 밤
            case "03d": // 구름 많음 낮
            case "03n": // 구름 많음 밤
            case "04d": // 구름 많음 낮
            case "04n": // 구름 많음 밤
                return R.drawable.weather_cloudy;
            case "09d": // 소나기 낮
            case "09n": // 소나기 밤
            case "10d": // 비 낮
            case "10n": // 비 밤
                return R.drawable.weather_rainy;
            case "11d": // 뇌우 낮
            case "11n": // 뇌우 밤
                return R.drawable.weather_windy;
            case "13d": // 눈 낮
            case "13n": // 눈 밤
                return R.drawable.weather_snow;
            case "50d": // 안개 낮
            case "50n": // 안개 밤
                return R.drawable.weather_fog;
            case "800": // 바람
            case "801": // 바람 약
            case "802": // 바람 강함
                return R.drawable.weather_windy;
            default:
                return R.drawable.default_weather;
        }
    }

    private void saveStepCount() {
        String todayDateStr = Tools.getFormattedDateToday();
        saveStepCountForDate(todayDateStr, currentStepCount);
    }

    private void loadStepCount() {
        String todayDateStr = Tools.getFormattedDateToday();
        currentStepCount = getStepCountForDate(todayDateStr);
        updateCircularProgressBar(currentStepCount);
    }

    private void setupDailyResetAlarm() {
        Context context = getActivity();
        Intent intent = new Intent(context, ResetStepCountReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    private void updateFirebase(String userid, int steps, int caloriesBurned) {
        DatabaseReference userRef = dr.child(userid); // userid에 해당하는 레퍼런스 가져오기
        userRef.child("currentSteps").setValue(steps);
        userRef.child("caloriesBurned").setValue(caloriesBurned);
    }

    private void saveStepCountForDate(String date, int stepCount) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(STEP_COUNT_DATE_KEY_PREFIX + date, stepCount);
        editor.apply();
    }

    private int getStepCountForDate(String date) {
        return sharedPreferences.getInt(STEP_COUNT_DATE_KEY_PREFIX + date, 0);
    }
}
