package com.example.walkingmate.feature.map.ui;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.walkingmate.*;
import com.example.walkingmate.core.constants.Constants;
import com.example.walkingmate.feature.feed.data.FeedData;
import com.example.walkingmate.feature.feed.ui.WalkingHomeActivity;
import com.example.walkingmate.feature.misc.ui.ChallengeActivity;
import com.example.walkingmate.feature.music.service.MusicService;
import com.example.walkingmate.feature.music.ui.MainMusicActivity;
import com.example.walkingmate.feature.user.data.UserData;
import com.example.walkingmate.feature.user.ui.AppInfoActivity;
import com.example.walkingmate.feature.user.ui.HelpInfoActivity;
import com.example.walkingmate.feature.user.ui.ManageFriendActivity;
import com.example.walkingmate.feature.walk.service.LocationService;
import com.example.walkingmate.feature.walk.service.StepCounterService;
import com.example.walkingmate.feature.walk.service.TimecheckingService;
import com.example.walkingmate.feature.walk.ui.EndTrackingActivity;
import com.example.walkingmate.feature.walk.ui.MeasureSpeedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapOptions;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Align;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.FusedLocationSource;

import java.nio.MappedByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {
    private FusedLocationSource locationSource;
    public int MyBPM = 60;
    private FusedLocationProviderClient fusedLocationProviderClient;
    FirebaseFirestore fb=FirebaseFirestore.getInstance();
    CollectionReference challenge=fb.collection("challenge");

    private static final int ACCESS_LOCATION_PERMISSION_REQUEST_CODE = 100;

    private NaverMap naverMap;
    private double lat, lon;
    boolean[] IsTracking = new boolean[1]; //경로추적기능 실행중 여부 확인
    float displacement; //이동거리
    int step;//발걸음 수
    long runtime;//걸은 시간(실시간 갱신)

    private int goalSteps = 0; // 목표 걸음수를 저장할 변수


    private TextToSpeech tts;
    private Handler handler4 = new Handler(Looper.getMainLooper());
    private Runnable ttsRunnable;
    private int interval = 30000; // 기본 30초


    LatLng[] tmpcoord;

    double[] startcoord = new double[2];

    ArrayList<LatLng> coordList = new ArrayList<>();//경로추적좌표
    ArrayList<LatLng> markList = new ArrayList<>(); //마크된 좌표 모음
    ArrayList<Marker> markerList = new ArrayList<>();//마커 모음
    HashMap<LatLng, String> markMap = new HashMap<>();//마크된 좌표와 메모 모음
    String[] timecheck = new String[2];//시작과 종료 시간,0이 시작-1이 종료


    ImageButton backBtn, endBtn, goalBtn, musicBtn;
    TextView disTxt, walkTxt, runtimeTxt;

    LinearLayout MapLayout;
    PathOverlay pathOverlay;

    Button intervalControlBtn;
    private boolean isIntervalActive = false;
    private boolean pendingIntervalAfterMeasure = false;

    int selecteditem;

    private boolean isExpanded = false;
    private Animation slideUpAnimation;
    private Animation slideDownAnimation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);


        backBtn = findViewById(R.id.back_tracing);
        endBtn = findViewById(R.id.endBtn);

        disTxt = findViewById(R.id.displacement_walk);
        walkTxt = findViewById(R.id.walk_tracking);
        runtimeTxt = findViewById(R.id.time_tracking);

        TextView goalStepsText = findViewById(R.id.goal_steps_text);
        goalStepsText.setVisibility(View.GONE);

        slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down);

        // Initialize TextToSpeech
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.KOREAN);
            }
        });


        IsTracking[0] = true;

        tmpcoord = new LatLng[2];


        musicBtn = findViewById(R.id.musicBtn); // 음악 버튼 참조 가져오기
        intervalControlBtn = findViewById(R.id.btn_interval_control);
        MyBPM = getSavedBpm();
        refreshIntervalControlUi();
        intervalControlBtn.setOnClickListener(v -> {
            if (isIntervalActive) {
                stopMusicService();
                isIntervalActive = false;
                refreshIntervalControlUi();
                Toast.makeText(MapActivity.this, "인터벌을 종료했습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            int savedBpm = getSavedBpm();
            if (savedBpm <= 0) {
                pendingIntervalAfterMeasure = true;
                Intent measureIntent = new Intent(MapActivity.this, MeasureSpeedActivity.class);
                startActivity(measureIntent);
                Toast.makeText(MapActivity.this, "먼저 10초 BPM 측정을 완료해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            MyBPM = savedBpm;
            showIntervalDialog();
        });

        musicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent musicIntent = new Intent(MapActivity.this, MainMusicActivity.class);
                startActivity(musicIntent);
            }
        });


        //비 동기적으로 네이버 지도 정보 가져옴
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        Log.d("도보 기록-flpc체크",(fusedLocationProviderClient==null)+"");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("도보 기록","권한 체크 에러");
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    //시작위치부터 좌표모음 시작
                    startcoord[0] = location.getLatitude();
                    startcoord[1] = location.getLongitude();
                    coordList.add(new LatLng(startcoord[0], startcoord[1]));
                    tmpcoord[0] = coordList.get(0);
                    Log.d("도보 기록", "시작좌표" + tmpcoord[0]);
                    mapFragment.getMapAsync(MapActivity.this);
                }
                else{
                    mapFragment.getMapAsync(MapActivity.this);
                    Log.d("도보 기록", "시작 null");
                }
            }
        });

        locationSource = new FusedLocationSource(this, ACCESS_LOCATION_PERMISSION_REQUEST_CODE);//현재위치값 받아옴

        //시작 세팅
        coordList.clear();
        markList.clear();
        markMap.clear();
        markerList.clear();
        displacement = 0;
        step = 0;
        walkTxt.setText("0");
        startStepCounterService();
        startTimeCheckingService();
        //시작 세팅

        timecheck[0] = getTime();
        Toast.makeText(MapActivity.this, "경로추적 시작!", Toast.LENGTH_SHORT).show();

        endBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // "산책 경로를 저장하시겠습니까?" 메시지와 함께 다이얼로그를 띄웁니다.
                new AlertDialog.Builder(MapActivity.this)
                        .setTitle("경로 저장")
                        .setMessage("산책 경로를 저장하시겠습니까?")
                        .setPositiveButton("예", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 사용자가 "예"를 선택한 경우
                                String sendfilename = "";

                                // 시작한 상태일 때만 작동
                                if (coordList.size() < 2) {
                                    return;
                                } else if (IsTracking[0]) {
                                    // 산책 경로 저장
                                    saveWalkedRoute(coordList);
                                    timecheck[1] = getTime();

                                    // 좌표가 너무 많으면 줄이기
                                    while (coordList.size() > 5000) {
                                        ArrayList<LatLng> tmp = new ArrayList<>();
                                        Log.d("메이트루트", coordList.size() + "");
                                        for (int i = 0; i < coordList.size(); ++i) {
                                            if (i % 4 != 0) {
                                                tmp.add(coordList.get(i));
                                            }
                                        }
                                        coordList = tmp;
                                    }
                                }

                                // 피드 데이터 내부 저장소에 저장
                                FeedData feedData = new FeedData(coordList, markerList, timecheck, step, displacement);
                                sendfilename = feedData.savefeed(feedData, MapActivity.this);

                                IsTracking[0] = false;

                                stopStepCounterService();
                                stopTimeCheckingService();
                                stopMusicService();
                                // 걸음 수 업데이트
                                challenge.document(UserData.loadData(MapActivity.this).userid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        Long updatestep = task.getResult().getLong("step") + (long) step;
                                        challenge.document(UserData.loadData(MapActivity.this).userid).update("step", updatestep);
                                    }
                                });

                                // EndTrackingActivity로 화면 전환
                                Intent gofeed = new Intent(MapActivity.this, EndTrackingActivity.class);
                                gofeed.putExtra("filename", sendfilename);
                                startActivity(gofeed);
                                finish();
                            }
                        })
                        .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 사용자가 "아니오"를 선택한 경우
                                String sendfilename = "";

                                // 시작한 상태일 때만 작동
                                if (coordList.size() < 2) {
                                    return;
                                } else if (IsTracking[0]) {
                                    // 피드 데이터 내부 저장소에 저장
                                    FeedData feedData = new FeedData(coordList, markerList, timecheck, step, displacement);
                                    sendfilename = feedData.savefeed(feedData, MapActivity.this);

                                    IsTracking[0] = false;

                                    stopStepCounterService();
                                    stopTimeCheckingService();
                                    stopMusicService();
                                    // 걸음 수 업데이트
                                    challenge.document(UserData.loadData(MapActivity.this).userid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            Long updatestep = task.getResult().getLong("step") + (long) step;
                                            challenge.document(UserData.loadData(MapActivity.this).userid).update("step", updatestep);
                                        }
                                    });

                                    // EndTrackingActivity로 화면 전환
                                    Intent gofeed = new Intent(MapActivity.this, EndTrackingActivity.class);
                                    gofeed.putExtra("filename", sendfilename);
                                    startActivity(gofeed);
                                    finish();
                                }
                            }
                        })
                        .show();
            }
        });



        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(IsTracking[0]){
                    Intent backfeed=new Intent(MapActivity.this, WalkingHomeActivity.class);

                    startActivity(backfeed);
                }
                else{
                    finish();
                }

            }
        });

    }

    private void showIntervalDialog() {
        LayoutInflater inflater = LayoutInflater.from(MapActivity.this);
        View dialogView = inflater.inflate(R.layout.dialog_interval, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
        builder.setView(dialogView);

        Button btn30s = dialogView.findViewById(R.id.btn_30s);
        Button btn1m = dialogView.findViewById(R.id.btn_1m);
        Button btn1m30s = dialogView.findViewById(R.id.btn_1m30s);
        Button btnRemeasure = dialogView.findViewById(R.id.btn_remeasure_bpm);
        TextView tvCurrentBpm = dialogView.findViewById(R.id.tv_current_bpm);
        tvCurrentBpm.setText("현재 BPM: " + getSavedBpm());

        AlertDialog intervalDialog = builder.create();

        btn30s.setOnClickListener(v -> {
            startMusicService(30000);  // 30초 간격
            isIntervalActive = true;
            refreshIntervalControlUi();
            intervalDialog.dismiss();
        });

        btn1m.setOnClickListener(v -> {
            startMusicService(60000);  // 60초 간격
            isIntervalActive = true;
            refreshIntervalControlUi();
            intervalDialog.dismiss();
        });

        btn1m30s.setOnClickListener(v -> {
            startMusicService(90000);  // 90초 간격
            isIntervalActive = true;
            refreshIntervalControlUi();
            intervalDialog.dismiss();
        });

        btnRemeasure.setOnClickListener(v -> {
            intervalDialog.dismiss();
            isIntervalActive = false;
            refreshIntervalControlUi();
            resetSavedBpm();
            pendingIntervalAfterMeasure = true;
            Intent measureIntent = new Intent(MapActivity.this, MeasureSpeedActivity.class);
            startActivity(measureIntent);
        });

        intervalDialog.show();

        Window window = intervalDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void startMusicService(int interval) {
        Intent serviceIntent = new Intent(MapActivity.this, MusicService.class);
        serviceIntent.setAction("START_MUSIC");
        serviceIntent.putExtra("BPM", MyBPM);
        serviceIntent.putExtra("INTERVAL", interval);
        startService(serviceIntent);
    }

    private void stopMusicService() {
        // MusicService 중지
        Intent stopIntent = new Intent(MapActivity.this, MusicService.class);
        stopIntent.setAction("STOP_MUSIC");
        stopService(stopIntent);
    }

    private void setInterval(int interval) {
        this.interval = interval;
    }

    private void startTimer() {
        stopTimer(); // 기존 타이머가 있을 경우 중지

        ttsRunnable = new Runnable() {
            private boolean isSlow = true;
            private boolean firstRun = true;
            private int countdown = 5;

            @Override
            public void run() {
                if (firstRun) {
                    // 첫 실행일 때는 바로 메시지를 출력하고 카운트다운 하지 않음
                    tts.speak(isSlow ? "천천히 걸으세요" : "빠르게 걸으세요", TextToSpeech.QUEUE_FLUSH, null, null);
                    firstRun = false;
                    handler4.postDelayed(this, interval); // 첫 주기 대기
                } else if (countdown > 0) {
                    // countdown 값에 따라 한국어 숫자 발음
                    String koreanNumber = getKoreanNumber(countdown);
                    tts.speak(koreanNumber, TextToSpeech.QUEUE_FLUSH, null, null);
                    countdown--;
                    handler4.postDelayed(this, 1000); // 1초 간격으로 카운트다운
                } else {
                    // 카운트다운 후 메시지 출력
                    isSlow = !isSlow; // 상태를 변경하여 교대로 발음하도록 설정
                    tts.speak(isSlow ? "천천히 걸으세요" : "빠르게 걸으세요", TextToSpeech.QUEUE_FLUSH, null, null);
                    countdown = 5; // 카운트다운 초기화
                    handler4.postDelayed(this, interval - 5000); // 다음 주기에서 5초 전에 카운트다운 시작
                }
            }

            // countdown 값을 한국어로 변환해주는 메소드
            private String getKoreanNumber(int number) {
                switch (number) {
                    case 5:
                        return "오";
                    case 4:
                        return "사";
                    case 3:
                        return "삼";
                    case 2:
                        return "이";
                    case 1:
                        return "일";
                    default:
                        return String.valueOf(number);
                }
            }
        };

        handler4.post(ttsRunnable);
    }

    private void stopTimer() {
        if (ttsRunnable != null) {
            handler4.removeCallbacks(ttsRunnable);
            ttsRunnable = null;
        }
    }




    private void saveWalkedRoute(ArrayList<LatLng> route) {
        SharedPreferences sharedPreferences = getSharedPreferences("WalkedRoutes", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        ArrayList<ArrayList<LatLng>> allRoutes = loadAllWalkedRoutes();
        if (allRoutes == null) {
            allRoutes = new ArrayList<>();
        }
        allRoutes.add(route);
        String json = gson.toJson(allRoutes);
        editor.putString("allRoutes", json);
        editor.apply();
    }


    private ArrayList<ArrayList<LatLng>> loadAllWalkedRoutes() {
        SharedPreferences sharedPreferences = getSharedPreferences("WalkedRoutes", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("allRoutes", null);
        Type type = new TypeToken<ArrayList<ArrayList<LatLng>>>() {}.getType();
        return gson.fromJson(json, type);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case ACCESS_LOCATION_PERMISSION_REQUEST_CODE:
                locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults);
                return;
        }
    }


    //지도 로딩 후 호출되는 매소드
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {

        //실시간 이동거리 계산을 위한 위치 변수, idx0이 현재, idx1이 과거 위치
        int pathColor = Color.parseColor("#00FF00");
        this.naverMap = naverMap;
        locationSource.getLastLocation();
        pathOverlay = new PathOverlay();
        pathOverlay.setColor(Color.BLUE);
        pathOverlay.setOutlineColor(Color.BLUE);
        pathOverlay.setWidth(5);
        for (LatLng[] presetPath : MapPresetPaths.all()) {
            PathOverlay overlay = new PathOverlay();
            overlay.setCoords(getCoords(presetPath));
            overlay.setColor(pathColor);
            overlay.setMap(naverMap);
        }

        // Load and display the saved route
        naverMap.setLocationTrackingMode(LocationTrackingMode.NoFollow);
        ArrayList<ArrayList<LatLng>> allRoutes = loadAllWalkedRoutes();
        if (allRoutes != null && !allRoutes.isEmpty()) {
            for (ArrayList<LatLng> route : allRoutes) {
                if (route != null && !route.isEmpty()) {
                    PathOverlay savedPathOverlay = new PathOverlay();
                    savedPathOverlay.setCoords(route);
                    savedPathOverlay.setColor(Color.GREEN);
                    savedPathOverlay.setMap(naverMap);
                }
            }
        }
        CameraPosition cameraPosition=new CameraPosition(new LatLng(startcoord[0],startcoord[1]),17);
        naverMap.setCameraPosition(cameraPosition);

        naverMap.setLocationSource(locationSource);//네이버지도상 위치값을 받아온 현재 위치값으로 설정



        //UI 세팅 코드
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setLocationButtonEnabled(true);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);


        //위치 좌표값이 변경될 때 마다 좌표값을 얻어온 뒤 해당 위치로 카메라 이동
        naverMap.addOnLocationChangeListener(new NaverMap.OnLocationChangeListener() {
            @Override
            public void onLocationChange(@NonNull Location location) {
                lat=location.getLatitude();
                lon=location.getLongitude();

                if(IsTracking[0]){
                    tmpcoord[1]=tmpcoord[0];
                    tmpcoord[0] =new LatLng(lat,lon);
                    coordList.add(tmpcoord[0]);
                    Log.d("GPS TRACKING", tmpcoord[0].toString());

                    //좌표모음에 들어간 좌표가 두개 이상인 경우 거리 측정(단위는 km)
                    if(coordList.size()>1){
                        displacement+=tmpcoord[0].distanceTo(tmpcoord[1])/1000;
                    }
                    disTxt.setText(""+Math.round((displacement*1000))/1000.0);

                    //경로 실시간 업데이트
                    if(coordList.size()>1){
                        pathOverlay.setCoords(coordList);
                        pathOverlay.setMap(naverMap);
                    }
                }
            }
        });

        goalBtn=findViewById(R.id.goalBtn);

        goalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflater = LayoutInflater.from(MapActivity.this);
                View dialogView = inflater.inflate(R.layout.dialog_goal_steps, null);

                AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
                builder.setView(dialogView);

                ListView listView = dialogView.findViewById(R.id.dialog_list);
                ImageView imageView = dialogView.findViewById(R.id.dialog_image);
                imageView.setImageResource(R.drawable.target_iconfinal);

                String[] options = {"1000", "2000", "3000", "4000", "5000", "직접 입력"};
                ArrayAdapter<String> adapter = new ArrayAdapter<>(MapActivity.this, R.layout.item_goal_step_option, android.R.id.text1, options);
                listView.setAdapter(adapter);

                final AlertDialog mainDialog = builder.create(); // 메인 다이얼로그

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (position == options.length - 1) {
                            // 메인 다이얼로그 닫기
                            mainDialog.dismiss();

                            View inputView = LayoutInflater.from(MapActivity.this).inflate(R.layout.dialog_goal_steps_input, null);
                            EditText input = inputView.findViewById(R.id.et_goal_steps);
                            Button cancelBtn = inputView.findViewById(R.id.btn_goal_input_cancel);
                            Button applyBtn = inputView.findViewById(R.id.btn_goal_input_apply);

                            AlertDialog inputDialog = new AlertDialog.Builder(MapActivity.this)
                                    .setView(inputView)
                                    .create();
                            inputDialog.show();

                            Window window = inputDialog.getWindow();
                            if (window != null) {
                                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                            }

                            cancelBtn.setOnClickListener(v1 -> inputDialog.dismiss());
                            applyBtn.setOnClickListener(v12 -> {
                                try {
                                    goalSteps = Integer.parseInt(input.getText().toString().trim());
                                    if (goalSteps <= 0) {
                                        Toast.makeText(MapActivity.this, "1 이상 숫자를 입력하세요.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    setGoalSteps(goalSteps);
                                    checkGoalCompletion();
                                    inputDialog.dismiss();
                                } catch (NumberFormatException e) {
                                    Toast.makeText(MapActivity.this, "유효한 숫자를 입력하세요.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            goalSteps = Integer.parseInt(options[position]);
                            setGoalSteps(goalSteps);
                            checkGoalCompletion(); // 목표 설정 후 바로 체크
                            mainDialog.dismiss(); // 메인 다이얼로그 닫기
                        }
                    }
                });

                mainDialog.show();
                Window window = mainDialog.getWindow();
                if (window != null) {
                    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                }
            }
        });

    }
    private ArrayList<LatLng> getCoords(LatLng[] latLngs) {
        ArrayList<LatLng> coords = new ArrayList<>();
        for (LatLng latLng : latLngs) {
            coords.add(latLng);
        }
        return coords;
    }
    private void setGoalSteps(int goalSteps) {
        TextView goalStepsText = findViewById(R.id.goal_steps_text);
        goalStepsText.setText("목표 걸음 수: " + goalSteps + " 보");

        // 애니메이션 로드
        final Animation slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        final Animation slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down);

        // TextView 초기 상태 설정
        goalStepsText.setVisibility(View.VISIBLE);
        goalStepsText.clearAnimation(); // 초기 상태에서 애니메이션 제거

        goalStepsText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isExpanded) {
                    // 애니메이션을 위로 올린 후 다시 내려오는 동작을 적용
                    goalStepsText.startAnimation(slideUpAnimation);
                    slideUpAnimation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) { }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            goalStepsText.startAnimation(slideDownAnimation);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) { }
                    });
                } else {
                    // 텍스트를 보이게 하고 위로 올라가는 애니메이션을 적용
                    goalStepsText.setVisibility(View.VISIBLE);
                    goalStepsText.startAnimation(slideDownAnimation);
                }
                isExpanded = !isExpanded;
            }
        });
    }





    private void checkGoalCompletion() {
        TextView walkTxt = findViewById(R.id.walk_tracking);
        int currentSteps = Integer.parseInt(walkTxt.getText().toString());

        if (currentSteps >= goalSteps && goalSteps > 0) { // 목표 설정 후 확인
            endBtn.performClick(); // 목표 달성 시 endBtn 자동 클릭
            finish();
        }
    }



    public String getTime(){
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy년_MM월_dd일_HH시_mm분");//날짜시간
        String getTime=sdf.format(date);
        return getTime;
    }

    private int getSavedBpm() {
        SharedPreferences sharedPreferences = getSharedPreferences("BPM_PREFS", Context.MODE_PRIVATE);
        return Math.round(sharedPreferences.getFloat("saved_bpm", 0f));
    }

    private void resetSavedBpm() {
        SharedPreferences sharedPreferences = getSharedPreferences("BPM_PREFS", Context.MODE_PRIVATE);
        sharedPreferences.edit().putFloat("saved_bpm", 0f).apply();
    }

    private void refreshIntervalControlUi() {
        if (intervalControlBtn == null) {
            return;
        }
        intervalControlBtn.setText(isIntervalActive ? "인터벌 종료" : "인터벌 시작");
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Intent intent = buildDrawerIntent(item.getItemId());
        if (intent != null) {
            startActivity(intent);
        }
        return true;
    }

    private Intent buildDrawerIntent(int itemId) {
        if (itemId == R.id.challenge) {
            return new Intent(this, ChallengeActivity.class);
        }
        if (itemId == R.id.managefriends) {
            return new Intent(this, ManageFriendActivity.class);
        }
        if (itemId == R.id.helpinfo) {
            return new Intent(this, HelpInfoActivity.class);
        }
        if (itemId == R.id.appinfo) {
            return new Intent(this, AppInfoActivity.class);
        }
        return null;
    }

    //loaction service를 onPause에서 시작하고 onResume에서 끝냄
    //멈춰있는동안만 서비스 실행, 단 위치 추적 실행중에만(Istracking[]
    //이동거리는 resultreceiver에 사용된 handler에서 실시간으로 갱신

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("멈춤","좌표수: "+coordList.size());
        if(IsTracking[0]){
            startLocationService();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("시작","좌표수: "+coordList.size());
        refreshIntervalControlUi();
        stopLocationService();

        if (pendingIntervalAfterMeasure) {
            int savedBpm = getSavedBpm();
            if (savedBpm > 0) {
                pendingIntervalAfterMeasure = false;
                MyBPM = savedBpm;
                showIntervalDialog();
            }
        }
    }


    //실행중 뒤로가기로 종료 방지
    @Override
    public void onBackPressed() {
        if(IsTracking[0]){
            Intent backfeed=new Intent(MapActivity.this, WalkingHomeActivity.class);

            startActivity(backfeed);
        }
        else{
            finish();
        }

    }

    //이 아래는 서비스 실행을 위한 코드

    private boolean isLocationServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (LocationService.class.getName().equals(service.service.getClassName())) {
                    if (service.foreground) {
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }


    Handler handler=new Handler();
    ResultReceiver resultReceiver= new ResultReceiver(handler){
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            if(resultCode==1){
                if(IsTracking[0]){
                    LatLng tmp=new LatLng(resultData.getDouble("lat"),resultData.getDouble("lon"));
                    coordList.add(tmp);
                    tmpcoord[1]=tmpcoord[0];
                    tmpcoord[0] =tmp;
                    if(coordList.size()>1){
                        displacement+=tmpcoord[0].distanceTo(tmpcoord[1])/1000;
                    }
                }
            }
            //종료 코드
            else if(resultCode==3){
                if (coordList.size() < 2) {
                    return;
                } else if (IsTracking[0]) {

                    timecheck[1] = getTime();

                    while(coordList.size()>5000){
                        ArrayList<LatLng> tmp=new ArrayList();
                        Log.d("메이트루트",coordList.size()+"");
                        for(int i=0; i<coordList.size(); ++i){
                            if(i%4!=0){
                                tmp.add(coordList.get(i));
                            }
                        }
                        coordList=tmp;
                    }


                    //피드데이터 내부저장소에 저장
                    FeedData feedData = new FeedData(coordList, markerList, timecheck, step, displacement);
                    feedData.savefeed(feedData, MapActivity.this);

                    challenge.document(UserData.loadData(MapActivity.this).userid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            Long updatestep=task.getResult().getLong("step")+(long)step;
                            challenge.document(UserData.loadData(MapActivity.this).userid).update("step",updatestep);
                        }
                    });

                    Log.d("백그라운드","중지");
                    IsTracking[0] = false;

                    //화면 초기화 시켜놓고 종료
                    //startActivity(new Intent(getApplicationContext(),MapActivity.class));
                    finish();
                }
            }
            else if(resultCode==2){
                if(coordList.size()<1){
                    return;
                }
                else{
                    LatLng tmpll=coordList.get(coordList.size()-1);

                    //이미 등록된 마커인 경우
                    if(markMap.containsKey(tmpll)){
                        Toast.makeText(getApplicationContext(), "이미 마커를 등록한 위치입니다.",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String[] tmpmark= {""};

                    markList.add(tmpll);
                    markMap.put(tmpll,tmpmark[0]);
                    Marker tmpm=new Marker();
                    tmpm.setPosition(tmpll);
                    tmpm.setCaptionAligns(Align.Top);
                    tmpm.setCaptionText(tmpmark[0]);
                    markerList.add(tmpm);


                    //마커표시 즉시 지도에 반영
                    tmpm.setMap(naverMap);
                    Toast.makeText(getApplicationContext(),"마커 등록 성공!",Toast.LENGTH_SHORT).show();

                    Log.d("백그라운드","마커등록, 좌표수: "+coordList.size());
                }
            }
        }
    };


    private void startLocationService() {
        if (!isLocationServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            intent.setAction(Constants.ACTION_START_LOCATION_SERVICE);
            intent.putExtra("RECEIVER",resultReceiver);
            startService(intent);
        }
    }

    private void stopLocationService() {
        if (isLocationServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            intent.setAction(Constants.ACTION_STOP_LOCATION_SERVICE);
            startService(intent);
        }


    }

    public void startStepCounterService(){
        Intent intent = new Intent(getApplicationContext(), StepCounterService.class);
        intent.setAction(Constants.ACTION_START_STEP_COUNTER_SERVICE);
        intent.putExtra("STEPRECIEVER",resultReceiverStep);
        startService(intent);
    }

    public void stopStepCounterService(){
        Log.d("만보기","정지액션 전송");
        Intent intent = new Intent(getApplicationContext(), StepCounterService.class);
        intent.setAction(Constants.ACTION_STOP_STEP_COUNTER_SERVICE);
        startService(intent);
    }

    private boolean isStepCounterServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (StepCounterService.class.getName().equals(service.service.getClassName())) {
                    if (service.foreground) {
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }




    Handler handler2=new Handler();
    ResultReceiver resultReceiverStep= new ResultReceiver(handler2){
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            if(resultCode==10){
                step=resultData.getInt("step");
                walkTxt.setText(""+step);
                checkGoalCompletion(); // 걸음 수 변경 시 목표 달성 여부 확인
            }
        }
    };

    public void startTimeCheckingService(){
        Intent intent = new Intent(getApplicationContext(), TimecheckingService.class);
        intent.setAction(Constants.ACTION_START_TIMECEHCKING_SERVICE);
        intent.putExtra("TIMECHECKINGSERVICE",resultReceiverTime);
        startService(intent);
    }

    public void stopTimeCheckingService(){
        Log.d("걸은시간","정지액션 전송");
        Intent intent = new Intent(getApplicationContext(), TimecheckingService.class);
        intent.setAction(Constants.ACTION_STOP_TIMECHECKING_SERVICE);
        startService(intent);
    }



    Handler handler3=new Handler();
    ResultReceiver resultReceiverTime= new ResultReceiver(handler3){
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            if(resultCode==15){
                //Log.d("걸은시간","종료 후 작동중 체크용-time");
                runtime=resultData.getLong("time");
                String h=String.format("%02d",runtime/(3600000));
                String m=String.format("%02d",runtime/60000);
                String s=String.format("%02d",(runtime/1000)%60);
                runtimeTxt.setText(h+":"+m+":"+s);
            }
        }
    };

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
        Intent intent1=new Intent(getApplicationContext(),LocationService.class);
        Intent intent2=new Intent(getApplicationContext(),StepCounterService.class);
        Intent intent3=new Intent(getApplicationContext(),TimecheckingService.class);
        Intent intent4=new Intent(getApplicationContext(),MusicService.class);
        stopService(intent1);
        stopService(intent2);
        stopService(intent3);
        stopService(intent4);
    }
}
