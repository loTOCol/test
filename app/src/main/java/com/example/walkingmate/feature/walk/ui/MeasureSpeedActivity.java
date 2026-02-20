package com.example.walkingmate.feature.walk.ui;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.walkingmate.R;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class MeasureSpeedActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepDetectorSensor;
    private boolean isTracking = false;
    static public double bpm;  // BPM 값을 저장할 변수
    private TextView tvTimer;
    private TextView tvBPM;
    private Button btnStartWalking;
    private Button btnDoneMeasure;
    private int stepsDuringMeasurement = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure_speed);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        tvTimer = findViewById(R.id.tv_timer);
        tvBPM = findViewById(R.id.tv_bpm);
        btnStartWalking = findViewById(R.id.btn_start_walking);
        btnDoneMeasure = findViewById(R.id.btn_done_measure);

        btnStartWalking.setOnClickListener(v -> startCountdown());
        btnDoneMeasure.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("measured_bpm", bpm);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void startCountdown() {
        // 초기화
        tvBPM.setText("측정된 BPM: -");
        stepsDuringMeasurement = 0;
        btnStartWalking.setEnabled(false);
        btnDoneMeasure.setVisibility(View.GONE);

        new CountDownTimer(10000, 1000) { // 10초 동안 카운트다운
            @Override
            public void onTick(long millisUntilFinished) {
                tvTimer.setText(String.valueOf(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                tvTimer.setText("0");
                stopSpeedTracking();
                calculateBPM(); // BPM 계산
                saveBPM(); // BPM 값 저장
                showBPM(); // BPM 값 표시
                btnStartWalking.setEnabled(true);
                btnDoneMeasure.setVisibility(View.VISIBLE);
            }
        }.start();

        startSpeedTracking(); // 10초 동안 걸음 수 측정 시작
    }

    private void startSpeedTracking() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACTIVITY_RECOGNITION}, 1);
            return;
        }

        isTracking = true;
        sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void stopSpeedTracking() {
        isTracking = false;
        sensorManager.unregisterListener(this);
    }

    private void calculateBPM() {
        long measurementDuration = 10; // 10초
        bpm = (stepsDuringMeasurement / (double) measurementDuration) * 60; // BPM 계산
    }

    private void saveBPM() {
        // SharedPreferences에 BPM 값 저장
        SharedPreferences sharedPreferences = getSharedPreferences("BPM_PREFS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("saved_bpm", (float) bpm);
        editor.apply(); // 비동기적으로 저장
    }

    private void showBPM() {
        // SharedPreferences에서 저장된 BPM 값 불러오기
        SharedPreferences sharedPreferences = getSharedPreferences("BPM_PREFS", Context.MODE_PRIVATE);
        float savedBpm = sharedPreferences.getFloat("saved_bpm", 0); // 기본값은 0
        tvBPM.setText("측정된 BPM: " + savedBpm);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCountdown();
            } else {
                Toast.makeText(this, "권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            if (isTracking) {
                stepsDuringMeasurement++;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
