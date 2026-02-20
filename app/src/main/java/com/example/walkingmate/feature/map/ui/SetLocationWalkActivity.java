package com.example.walkingmate.feature.map.ui;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

import com.example.walkingmate.BuildConfig;
import com.example.walkingmate.R;
import com.example.walkingmate.feature.misc.ui.SearchActivity;
import com.example.walkingmate.feature.walk.ui.WalkWriteActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.util.FusedLocationSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class SetLocationWalkActivity extends AppCompatActivity implements OnMapReadyCallback {

    private FusedLocationSource locationSource;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private NaverMap naverMap;

    EditText searchLoc;

    String location,x,y;

    double xval, yval;

    LatLng cur;
    String curName;
    Marker marker;

    MapFragment mapFragmentFeed;

    private static final int ACCESS_LOCATION_PERMISSION_REQUEST_CODE = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_location_walk);

        mapFragmentFeed=(MapFragment)getSupportFragmentManager().findFragmentById(R.id.SetLocationMap);
        marker=new Marker();

        locationSource = new FusedLocationSource(this, ACCESS_LOCATION_PERMISSION_REQUEST_CODE);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    yval = location.getLatitude();
                    xval = location.getLongitude();
                    cur=new LatLng(yval,xval);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            requestName(cur);
                        }
                    }).start();
                    marker.setPosition(cur);
                    mapFragmentFeed.getMapAsync(SetLocationWalkActivity.this);
                }
                else{
                    yval = 37.5666103;
                    xval = 126.9783882;
                    cur=new LatLng(yval,xval);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            requestName(cur);
                        }
                    }).start();
                    marker.setPosition(cur);
                    mapFragmentFeed.getMapAsync(SetLocationWalkActivity.this);
                }
            }
        });



        searchLoc=findViewById(R.id.search_loc_setloc);
        searchLoc.setFocusable(false); //입력막음

        //주소입력창 이동, getsearchResult로 주소명 받아옴
        searchLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(SetLocationWalkActivity.this, SearchActivity.class);
                getsearchResult.launch(intent);
            }
        });

        findViewById(R.id.goloc_walk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapFragmentFeed.getMapAsync(SetLocationWalkActivity.this);
            }
        });

        findViewById(R.id.finish_setloc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("산책_좌표전송",cur.toString());
                Intent finintent=new Intent(SetLocationWalkActivity.this,WalkWriteActivity.class);
                finintent.putExtra("lat",cur.latitude);
                finintent.putExtra("lon",cur.longitude);
                finintent.putExtra("location",curName);
                setResult(RESULT_OK,finintent);
                finish();
            }
        });


    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {

        this.naverMap=naverMap;


        CameraPosition cameraPosition=new CameraPosition(cur,17);
        naverMap.setCameraPosition(cameraPosition);


        naverMap.setLocationSource(locationSource);
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setLocationButtonEnabled(true);
        naverMap.setLocationTrackingMode(LocationTrackingMode.None);

        marker.setPosition(cur);
        marker.setMap(naverMap);


        naverMap.setOnMapClickListener(new NaverMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
                marker.setPosition(latLng);
                marker.setMap(naverMap);
                cur=latLng;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        requestName(cur);
                    }
                }).start();

            }
        });
    }

    private final ActivityResultLauncher<Intent> getsearchResult= registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() ==RESULT_OK){
                    if(result.getData()!=null){
                        location=result.getData().getStringExtra("data");
                        curName=location;
                        searchLoc.setText(curName);
                        //받아온 뒤 바로 geocoding으로 좌표 추출
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                requestCoord(location);
                            }
                        }).start();
                    }
                }
            });

    //요청 지역명에 대한 위치좌표 반환
    public void requestCoord(String loc){
        try{
            BufferedReader bufferedReader;
            StringBuilder stringBuilder=new StringBuilder();
            String query= "https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode?query=" + URLEncoder.encode(loc, "UTF-8");
            URL url=new URL(query);
            HttpURLConnection conn=(HttpURLConnection) url.openConnection();
            if(conn!=null){
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("X-NCP-APIGW-API-KEY-ID", BuildConfig.NAVER_SEARCH_API_KEY_ID);
                conn.setRequestProperty("X-NCP-APIGW-API-KEY", BuildConfig.NAVER_SEARCH_API_KEY);
                conn.setDoInput(true);

                int responseCode=conn.getResponseCode();
                Log.d("주소 검색","Geocoding결과:"+responseCode);

                if (responseCode == 200) {
                    bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    bufferedReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                String line=null;
                while((line=bufferedReader.readLine())!=null){
                    stringBuilder.append(line+"\n");
                }

                int indexFirst, indexLast;
                indexFirst = stringBuilder.indexOf("\"x\":\"");
                indexLast = stringBuilder.indexOf("\",\"y\":");
                Log.d("주소 검색","x:"+indexFirst+", y:"+indexLast);

                x=stringBuilder.substring(indexFirst+5,indexLast);

                indexFirst = stringBuilder.indexOf("\"y\":\"");
                indexLast = stringBuilder.indexOf("\",\"distance\":");
                y = stringBuilder.substring(indexFirst + 5, indexLast);

                bufferedReader.close();
                conn.disconnect();

                xval=Double.parseDouble(x); //lon
                yval=Double.parseDouble(y);//lat

                cur=new LatLng(yval,xval);

                Log.d("주소 검색","좌표:"+xval+","+yval);

            }
            else{
                Log.d("주소 검색","연결실패");
            }



        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void requestName(LatLng loc){
        try{
            BufferedReader bufferedReader;
            StringBuilder stringBuilder=new StringBuilder();

            String coord=loc.longitude+","+loc.latitude;
            Log.d("주소 검색_Re요청좌표",coord);
            String query="https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc?request=coordsToaddr&coords=" +
                    coord +
                    "&sourcecrs=epsg:4326&output=json&orders=addr";
            URL url=new URL(query);
            HttpURLConnection conn=(HttpURLConnection) url.openConnection();
            if(conn!=null){
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("X-NCP-APIGW-API-KEY-ID", BuildConfig.NAVER_SEARCH_API_KEY_ID);
                conn.setRequestProperty("X-NCP-APIGW-API-KEY", BuildConfig.NAVER_SEARCH_API_KEY);
                conn.setDoInput(true);

                int responseCode=conn.getResponseCode();
                Log.d("주소 검색","Re_Geocoding결과:"+responseCode);

                if (responseCode == 200) {
                    bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    bufferedReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                String line=null;
                while((line=bufferedReader.readLine())!=null){
                    stringBuilder.append(line+"\n");
                }

                Log.d("주소 검색_Re",stringBuilder.toString());

                String json=stringBuilder.toString();

                curName=getNamefromJson(json);
                searchLoc.setText(curName);

                bufferedReader.close();
                conn.disconnect();

            }
            else{
                Log.d("주소 검색","연결실패");
            }



        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public String getNamefromJson(String json){
        String addr="";
        try{
            JSONObject jsonObject=new JSONObject(json);
            JSONArray jsonArray=jsonObject.getJSONArray("results");
            JSONObject addrObject=jsonArray.getJSONObject(0).getJSONObject("region");
            addr+=addrObject.getJSONObject("area1").getString("name")+" "+
                    addrObject.getJSONObject("area2").getString("name")+" "+
                    addrObject.getJSONObject("area3").getString("name")+" "+
                    addrObject.getJSONObject("area4").getString("name")+" "+
                    jsonArray.getJSONObject(0).getJSONObject("land").getString("number1");

            addr.replace("  "," ");

        }catch (JSONException e){
            e.printStackTrace();
        };
        return addr;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //
        switch (requestCode) {
            case ACCESS_LOCATION_PERMISSION_REQUEST_CODE:
                locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults);
                return;
        }
    }
}
