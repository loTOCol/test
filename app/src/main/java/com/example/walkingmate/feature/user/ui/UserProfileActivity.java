package com.example.walkingmate.feature.user.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.walkingmate.R;
import com.example.walkingmate.feature.user.data.UserData;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.example.walkingmate.feature.feed.ui.FeedActivity;
import com.example.walkingmate.feature.schedule.ui.ReportActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.checkerframework.checker.units.qual.A;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileActivity extends AppCompatActivity {
    FirebaseFirestore fb=FirebaseFirestore.getInstance();
    CollectionReference users=fb.collection("users");
    CollectionReference blocklist=fb.collection("blocklist");

    String userid;

    CircleImageView profileImage;
    TextView appname,gender,age,title;
    Button block,feed;
    ImageButton back;
    View reportGroup;

    UserData userData;

    // 일간 걸음수와 총 걸음수 저장 변수
    long dailySteps = 0;
    long totalSteps = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        Intent getIntent=getIntent();
        userid=getIntent.getStringExtra("userid");
        userData=UserData.loadData(this);

        profileImage=findViewById(R.id.profileImage_userprofile);
        appname=findViewById(R.id.appname_userprofile);
        gender=findViewById(R.id.gender_profile);
        age=findViewById(R.id.age_profile);
        title=findViewById(R.id.title_userprofile);
        block=findViewById(R.id.block_profile);
        feed=findViewById(R.id.feed_profile);
        back=findViewById(R.id.back_profile);
        reportGroup=findViewById(R.id.report_group_profile);

        setprofile();

        if(userData.userid.equals(userid)){
            block.setVisibility(View.INVISIBLE);
            feed.setVisibility(View.INVISIBLE);
            reportGroup.setVisibility(View.INVISIBLE);
        }


        DatabaseReference realtimeDbRef = FirebaseDatabase.getInstance().getReference("walk").child(userid);
        realtimeDbRef.child("currentSteps").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long stepValue = snapshot.getValue(Long.class);
                    dailySteps = stepValue == null ? 0 : stepValue;
                    updateBarChart(dailySteps, totalSteps); // 총 걸음수를 가져온 후 업데이트
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("RealtimeDB", "Error fetching daily steps", error.toException());
            }
        });

        DocumentReference firestoreRef = FirebaseFirestore.getInstance().collection("challenge").document(userid);
        firestoreRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        totalSteps = document.getLong("step");
                        updateBarChart(dailySteps, totalSteps); // 일간 걸음수를 가져온 후 업데이트
                    }
                } else {
                    Log.e("FirestoreDB", "Error fetching total steps", task.getException());
                }
            }
        });





        block.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                blocklist.document(userData.userid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        ArrayList<String> blocklisttmp=new ArrayList<>();
                        if(task.getResult().exists()){
                            blocklisttmp= (ArrayList<String>) task.getResult().get("userid");
                            Log.d("차단체크","존재");
                        }
                        if(!task.getResult().exists()){
                            Map<String, ArrayList<String>> users=new HashMap<>();
                            ArrayList<String> blockstr=new ArrayList<>();
                            blockstr.add(userid);
                            users.put("userid",blockstr);
                            blocklist.document(userData.userid).set(users).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Toast.makeText(UserProfileActivity.this,"차단되었습니다.",Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        else if(!blocklisttmp.contains(userid)){
                            blocklist.document(userData.userid).update("userid", FieldValue.arrayUnion(userid)).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Toast.makeText(UserProfileActivity.this,"차단되었습니다.",Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        else{
                            Toast.makeText(UserProfileActivity.this,"이미 차단한 유저입니다.",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        feed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(UserProfileActivity.this, FeedActivity.class);
                intent.putExtra("iswrite",2);
                intent.putExtra("others",userid);
                startActivity(intent);
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        reportGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(UserProfileActivity.this,"신고 화면으로 이동합니다.",Toast.LENGTH_SHORT).show();
                Intent intent=new Intent(UserProfileActivity.this,ReportActivity.class);
                intent.putExtra("userid",userid);
                startActivity(intent);
            }
        });

    }

    public void setprofile(){

        users.document(userid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(!task.isSuccessful() || task.getResult()==null || !task.getResult().exists()){
                    profileImage.setImageResource(R.drawable.blank_profile);
                    setrel(0);
                    findViewById(R.id.profile_main).setVisibility(View.VISIBLE);
                    findViewById(R.id.loading_profile).setVisibility(View.INVISIBLE);
                    return;
                }

                DocumentSnapshot document=task.getResult();

                String appNameStr=document.getString("appname");
                appname.setText((appNameStr==null||appNameStr.equals(""))?"알 수 없는 사용자":appNameStr);

                String genderRaw=document.getString("gender");
                String genderstr;
                if("M".equals(genderRaw)){
                    genderstr="남성";
                }
                else if("F".equals(genderRaw)){
                    genderstr="여성";
                }
                else{
                    genderstr="미설정";
                }
                gender.setText(genderstr);

                String ageStr=document.getString("age");
                age.setText((ageStr==null||ageStr.equals(""))?"미설정":ageStr);

                String titleStr=document.getString("title");
                if(titleStr==null||titleStr.equals("")||titleStr.equals("없음")){
                    title.setText("사용 중인 칭호가 없습니다.");
                }
                else{
                    title.setText(titleStr);
                }

                double relValue=document.getDouble("reliability")==null?0.0:document.getDouble("reliability");
                Long rel=Math.round(relValue);
                setrel(Math.toIntExact(rel));

                // Show profile UI immediately with text and cached image while network image loads.
                Bitmap cached = loadCachedProfileImage(userid);
                if(cached != null){
                    profileImage.setImageBitmap(cached);
                } else {
                    profileImage.setImageResource(R.drawable.blank_profile);
                }
                findViewById(R.id.profile_main).setVisibility(View.VISIBLE);
                findViewById(R.id.loading_profile).setVisibility(View.INVISIBLE);

                String urlstr=document.getString("profileImagebig");
                if((urlstr!=null)&&(!urlstr.equals(""))){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            HttpURLConnection connection = null;
                            try {
                                URL imgUrl = new URL(urlstr);
                                connection = (HttpURLConnection) imgUrl.openConnection();
                                connection.setDoInput(true);
                                connection.connect();
                                InputStream is = connection.getInputStream();
                                Bitmap retBitmap = BitmapFactory.decodeStream(is);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(retBitmap!=null){
                                            profileImage.setImageBitmap(retBitmap);
                                            saveCachedProfileImage(userid, retBitmap);
                                        }
                                        else{
                                            profileImage.setImageResource(R.drawable.blank_profile);
                                        }
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        profileImage.setImageResource(R.drawable.blank_profile);
                                    }
                                });
                            } finally {
                                if (connection != null) {
                                    connection.disconnect();
                                }
                            }
                        }
                    }).start();
                }
                else{
                    profileImage.setImageResource(R.drawable.blank_profile);
                }

            }
        });

    }

    private File getProfileCacheFile(String targetUserId){
        File cacheDir = new File(getCacheDir(), "profile_cache");
        if(!cacheDir.exists()){
            cacheDir.mkdirs();
        }
        return new File(cacheDir, targetUserId + ".jpg");
    }

    private Bitmap loadCachedProfileImage(String targetUserId){
        try{
            File cacheFile = getProfileCacheFile(targetUserId);
            if(cacheFile.exists()){
                FileInputStream fis = new FileInputStream(cacheFile);
                Bitmap bmp = BitmapFactory.decodeStream(fis);
                fis.close();
                return bmp;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private void saveCachedProfileImage(String targetUserId, Bitmap bitmap){
        if(bitmap==null){
            return;
        }
        try{
            File cacheFile = getProfileCacheFile(targetUserId);
            FileOutputStream fos = new FileOutputStream(cacheFile, false);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.flush();
            fos.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void updateBarChart(long dailySteps, long totalSteps) {
        BarChart barChart = findViewById(R.id.barChart);

        // 기존 데이터와 설정을 초기화
        barChart.clear();
        barChart.invalidate();

        // 확대/축소 기능 비활성화
        barChart.setScaleEnabled(false);
        barChart.setPinchZoom(false);
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.setTouchEnabled(false);

        barChart.setDrawValueAboveBar(true);

        barChart.getDescription().setEnabled(false);

        // 일간 걸음수 막대 데이터 생성
        List<BarEntry> dailyEntries = new ArrayList<>();
        dailyEntries.add(new BarEntry(0, dailySteps));  // 일간 걸음수

        BarDataSet dailyDataSet = new BarDataSet(dailyEntries, "일일 걸음 수");
        dailyDataSet.setColor(Color.parseColor("#8F6C44"));
        dailyDataSet.setValueFormatter(new IntegerValueFormatter()); // 값 포매터 설정

        // 총 걸음수 막대 데이터 생성
        List<BarEntry> totalEntries = new ArrayList<>();
        totalEntries.add(new BarEntry(1, totalSteps));  // 총 걸음수

        BarDataSet totalDataSet = new BarDataSet(totalEntries, "총 걸음 수");
        totalDataSet.setColor(Color.parseColor("#D5B88B"));
        totalDataSet.setValueFormatter(new IntegerValueFormatter()); // 값 포매터 설정

        // 데이터 세트를 BarData 객체에 추가
        BarData barData = new BarData(dailyDataSet, totalDataSet);
        barData.setBarWidth(0.5f); // 막대 너비를 조절 (0.1f ~ 1f 범위로 설정 가능)
        barData.setValueTextSize(12f);
        barData.setValueTextColor(Color.parseColor("#2A2117"));

        // X축 레이블 설정
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"오늘", "누적"}));
        xAxis.setTextColor(Color.parseColor("#6A5A45"));
        xAxis.setTextSize(11f);
        xAxis.setDrawGridLines(false); // X축의 그리드 라인 제거
        xAxis.setAxisMinimum(-0.5f); // X축 최소값 설정
        xAxis.setAxisMaximum(1.5f); // X축 최대값 설정

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setDrawGridLines(false);
        leftAxis.setValueFormatter(new IntegerValueFormatter());

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);

        // 범례 설정
        Legend legend = barChart.getLegend();
        legend.setEnabled(false);  // 범례 표시 비활성화
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);  // 범례의 수평 정렬
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);  // 범례의 수직 정렬
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);  // 범례의 방향
        legend.setDrawInside(true);  // 범례를 그래프 내부에 표시
        legend.setXEntrySpace(10f);  // 항목 간의 수평 간격
        legend.setYEntrySpace(5f);   // 항목 간의 수직 간격
        legend.setFormSize(10f);     // 범례 항목의 크기

        // 데이터 적용
        barChart.setData(barData);

        // 애니메이션 추가
        barChart.animateXY(1000, 1000);  // X축과 Y축 방향으로 각각 1000ms 동안 애니메이션

        // 그래프 업데이트
        barChart.invalidate();  // 그래프를 다시 그리도록 요청
    }




    public class IntegerValueFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            return String.format("%d", (int) value);
        }
    }




    public void setrel(int rel){
        int Max=250;
        int relhor=(Max*rel)/100;
        int relvermarlef=relhor-4;
        int heartmarlef=relhor-17;

        FrameLayout.LayoutParams lp0=new FrameLayout.LayoutParams(getdp(relhor),getdp(30));
        lp0.setMargins(0,getdp(60),0,0);
        lp0.gravity= Gravity.BOTTOM;
        findViewById(R.id.reliable_mainred_pro).setLayoutParams(lp0);

        FrameLayout.LayoutParams lp1=new FrameLayout.LayoutParams(getdp(4),getdp(30));
        lp1.setMargins(getdp(relvermarlef),0,0,getdp(15));
        lp1.gravity= Gravity.BOTTOM;
        findViewById(R.id.reliable_vertical_pro).setLayoutParams(lp1);

        FrameLayout.LayoutParams lp2=new FrameLayout.LayoutParams(getdp(30),getdp(30));
        lp2.setMargins(getdp(heartmarlef),0,0,getdp(40));
        lp2.gravity= Gravity.BOTTOM;
        TextView relnum=findViewById(R.id.reliable_number_pro);
        relnum.setLayoutParams(lp2);
        relnum.setText(rel+"");
    }

    public int getdp(int a){
        DisplayMetrics displayMetrics=getResources().getDisplayMetrics();
        return Math.round(a*displayMetrics.density);
    }

}
