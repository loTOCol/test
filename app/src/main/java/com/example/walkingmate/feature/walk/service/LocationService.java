package com.example.walkingmate.feature.walk.service;

import static com.example.walkingmate.core.constants.Constants.ACTION_START_LOCATION_SERVICE;
import static com.example.walkingmate.core.constants.Constants.ACTION_STOP_LOCATION_SERVICE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaSession2Service;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.ResultReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.walkingmate.R;
import com.example.walkingmate.core.constants.Constants;
import com.example.walkingmate.feature.map.ui.MapActivity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.naver.maps.geometry.LatLng;

import java.util.ArrayList;


public class LocationService extends Service {

    Intent mIntent;
    ResultReceiver resultReceiver;




    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if (locationResult != null && locationResult.getLastLocation() != null) {
                double latitude = locationResult.getLastLocation().getLatitude();
                double longitude = locationResult.getLastLocation().getLongitude();

                Bundle bundle=new Bundle();
                bundle.putDouble("lat",latitude);
                bundle.putDouble("lon",longitude);
                resultReceiver.send(1,bundle);

                Log.d("LOCATION_UPDATE", latitude + ", " + longitude);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void notificationSending(){
        //이 아래는 백그라운드 서비스로 전환되어 실행시 상단 알림창이 뜨는것-pendding intent이용
        String channelId = "location_notification_channel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //알림 클릭시 뜨는 화면 얙티비티티
        Intent resultIntent = new Intent(Intent.ACTION_MAIN);
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        resultIntent.setComponent(new ComponentName(getApplicationContext(),MapActivity.class));

        //아래 request code에서 0무음, 1진동, 2소리, 3 진동+소리
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, resultIntent, PendingIntent.FLAG_MUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId);
        builder.setSmallIcon(R.mipmap.app_icon_asset);
        builder.setContentTitle("Walking Mate");
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setOngoing(true);
        builder.setContentText("산책기록중...");
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(false);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        builder.setOnlyAlertOnce(true);
        builder.addAction(makeButton("Mark",R.drawable.add_marker_notification));
        builder.addAction(makeButton("Finish",R.drawable.stop_icon_notification));
        builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0,1));


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager != null && notificationManager.getNotificationChannel(channelId) == null) {
                NotificationChannel notificationChannel = new NotificationChannel(channelId, "Location Service", NotificationManager.IMPORTANCE_LOW);
                notificationChannel.setDescription("This channel is used by location service");

                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
        startForeground(Constants.LOCATION_SERVICE_ID, builder.build());
    }

    private void startLocationService() {

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, mLocationCallback, Looper.getMainLooper());

    }

    private void stopLocationService() {
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(mLocationCallback);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(ACTION_START_LOCATION_SERVICE)) {
                    mIntent=intent;
                    resultReceiver=mIntent.getParcelableExtra("RECEIVER");
                    startLocationService();
                    notificationSending();
                } else if (action.equals(ACTION_STOP_LOCATION_SERVICE)) {
                    notificationSending();
                    stopLocationService();
                }else if(action.equals("Mark")){
                    Bundle bundles=new Bundle();
                    bundles.putDouble("Mark",0);
                    resultReceiver.send(2,bundles);
                }else if(action.equals("Finish")){
                    Bundle bundleend=new Bundle();
                    bundleend.putDouble("Finish",0);
                    resultReceiver.send(3,bundleend);
                    Log.d("백그라운드","종료버튼");
                    stopLocationService();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }



    private NotificationCompat.Action makeButton(String action, int iconId){
        Intent intent=new Intent(getBaseContext(), LocationService.class);

        intent.setAction(action);

        PendingIntent pendingIntent=PendingIntent.getService(getBaseContext(),1,intent,PendingIntent.FLAG_IMMUTABLE);

        String buttonTitle=action;

        NotificationCompat.Action notiaction=new NotificationCompat.Action.Builder(iconId, buttonTitle, pendingIntent).build();

        return notiaction;

    }


}
