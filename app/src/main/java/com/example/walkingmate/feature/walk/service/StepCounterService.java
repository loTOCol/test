package com.example.walkingmate.feature.walk.service;

import static com.example.walkingmate.core.constants.Constants.ACTION_START_LOCATION_SERVICE;
import static com.example.walkingmate.core.constants.Constants.ACTION_STOP_LOCATION_SERVICE;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import com.example.walkingmate.core.constants.Constants;
import com.example.walkingmate.feature.walk.ui.WalkFragment;

public class StepCounterService extends Service implements SensorEventListener {

    int step;
    SensorManager sensorManager;
    Sensor stepCountSensor;
    ResultReceiver resultReceiver;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void startStepCounterService(){

        Log.d("만보기","start");

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        if (stepCountSensor != null) {
            sensorManager.registerListener((SensorEventListener) this, stepCountSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        if (stepCountSensor == null) {
            Toast.makeText(getApplicationContext(), "이 디바이스는 만보기 기능을 지원하기 않습니다.", Toast.LENGTH_SHORT).show();
        }

    }

    public void stopStepCounterService(){

        Log.d("만보기","end");

        sensorManager.unregisterListener(this);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_STEP_DETECTOR){
            //경로추적 시작시 만보기기능 시작
            step++;
            WalkFragment.currentStepCount++;
            Bundle bundle=new Bundle();
            bundle.putInt("step",step);
            resultReceiver.send(10,bundle);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if(action.equals(Constants.ACTION_START_STEP_COUNTER_SERVICE)){
                    resultReceiver=intent.getParcelableExtra("STEPRECIEVER");
                    step=0;
                    startStepCounterService();
                }
                else if(action.equals(Constants.ACTION_STOP_STEP_COUNTER_SERVICE)){
                    stopStepCounterService();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

}
