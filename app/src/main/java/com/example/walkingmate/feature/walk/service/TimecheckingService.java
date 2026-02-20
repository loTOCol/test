package com.example.walkingmate.feature.walk.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.os.SystemClock;

import com.example.walkingmate.core.constants.Constants;

public class TimecheckingService extends Service {

    long start, now,dif;//ms단위
    ResultReceiver resultReceiver;
    Thread running;
    boolean runbool;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void startTimecheckingService(){
        running=new Thread(new Runnable() {
            @Override
            public void run() {
                while(runbool){
                    now=SystemClock.elapsedRealtime();
                    dif=now-start;
                    Bundle bundle=new Bundle();
                    bundle.putLong("time",dif);
                    resultReceiver.send(15,bundle);
                    try{
                        Thread.sleep(100);
                    }catch (Exception e){}
                }
            }
        });
        running.start();
    }

    public void endTimecheckingService(){
        runbool=false;
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent!=null){
            String action=intent.getAction();
            if(action!=null){
                if(action.equals(Constants.ACTION_START_TIMECEHCKING_SERVICE)){
                    start= SystemClock.elapsedRealtime();
                    runbool=true;
                    resultReceiver=intent.getParcelableExtra("TIMECHECKINGSERVICE");
                    startTimecheckingService();
                }
                else if(action.equals(Constants.ACTION_STOP_TIMECHECKING_SERVICE)){
                    endTimecheckingService();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

}
