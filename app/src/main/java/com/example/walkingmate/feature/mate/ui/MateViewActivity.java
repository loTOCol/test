package com.example.walkingmate.feature.mate.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.walkingmate.R;
import com.example.walkingmate.feature.mate.data.repository.MateRepository;
import com.example.walkingmate.feature.user.data.UserData;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import com.google.firebase.firestore.SetOptions;
import com.example.walkingmate.feature.walk.ui.WalkUserListActivity;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Align;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.MarkerIcons;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class MateViewActivity extends AppCompatActivity implements OnMapReadyCallback {
    private final MateRepository mateRepository = MateRepository.getInstance();
    CollectionReference matedataRef = mateRepository.getMateDataRef();
    CollectionReference matedatalistRef = mateRepository.getMateDataListRef();
    CollectionReference users = mateRepository.getUsersRef();
    CollectionReference materequestRef = mateRepository.getMateRequestRef();
    CollectionReference mateuserRef = mateRepository.getMateUserRef();

    UserData userData;

    ArrayList<String> locations=new ArrayList<>();
    ArrayList<LatLng> coords=new ArrayList<>();
    ArrayList<LatLng> routes=new ArrayList<>();
    String title, content;

    MapFragment mapFragment;
    NaverMap naverMap;
    PathOverlay pathOverlay=new PathOverlay();
    ArrayList<Marker> markers=new ArrayList<>();

    String docuid, date,userid;
    String prefillTitle, prefillWriter;
    ArrayList<String> prefillLocations;

    TextView usertitle, userinfo, datetxt,locationtxt,titletxt,contenttxt;
    CircleImageView userimg;
    Button mate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mate_view);

        usertitle=findViewById(R.id.usertitle_mateview);
        userinfo=findViewById(R.id.userinfo_mateview);
        datetxt=findViewById(R.id.date_mateview);
        locationtxt=findViewById(R.id.locations_mateview);
        titletxt=findViewById(R.id.title_mateview);
        contenttxt=findViewById(R.id.content_mateview);
        userimg=findViewById(R.id.userimg_mateview);
        mate=findViewById(R.id.mate_mateview);

        Intent getintent=getIntent();
        docuid=getintent.getStringExtra("docuid");
        String dateExtra = getintent.getStringExtra("date");
        date = dateExtra == null ? "" : " " + dateExtra;
        userid=getintent.getStringExtra("userid");
        prefillTitle = getintent.getStringExtra("prefill_title");
        prefillWriter = getintent.getStringExtra("prefill_writer");
        prefillLocations = getintent.getStringArrayListExtra("prefill_locations");

        applyPrefillFromIntent();
        findViewById(R.id.loading_mateview).setVisibility(View.INVISIBLE);
        mate.setVisibility(View.VISIBLE);

        userData=UserData.loadData(this);
        if(userid!=null && userid.equals(userData.userid)){
            mate.setText("신청자 목록 확인");
        }



        mapFragment=(MapFragment)getSupportFragmentManager().findFragmentById(R.id.map_mateview);
        try{
            getMateData();
        }catch (Exception e){
            e.printStackTrace();
        }


        findViewById(R.id.back_mateview).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(userid!=null && userid.equals(userData.userid)){
                    Intent intent=new Intent(MateViewActivity.this,WalkUserListActivity.class);
                    intent.putExtra("mydocu",docuid);
                    intent.putExtra("walkname",getintent.getStringExtra("date").split("~")[0]);
                    intent.putExtra("ismate",true);
                    startActivity(intent);
                }
                else{
                    matedatalistRef.document(docuid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if(task.getResult().exists()){
                                checkandsendreq();
                            }
                            else{
                                Toast.makeText(getApplicationContext(),"삭제된 게시물입니다.",Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                }
            }
        });

    }

    public void checkandsendreq(){
        materequestRef.document(userData.userid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                ArrayList<String> tmps= (ArrayList<String>) task.getResult().get("requestlist");
                //요청한적이 없어 문서가 없거나 요청을 안한경우
                if(tmps==null||!tmps.contains(docuid)){
                    sendreq();
                }
                else{
                    Toast.makeText(MateViewActivity.this,"이미 요청을 보낸 게시물입니다.",Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    public void sendreq(){
        HashMap<String,Object> data=new HashMap<>();

        HashMap<String, Integer> myreq=new HashMap<>();
        myreq.put(userData.userid,0);

        //userlist는 map-setoption.merge로 업데이트
        data.put("userlist",myreq);
        mateuserRef.document(docuid).set(data, SetOptions.merge()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Log.d("산책 메이트 신청","성공");
            }
        });

        data.clear();
        data.put("requestlist", Arrays.asList(docuid));

        //requestlist는 list-arrayunion으로 업데이트
        materequestRef.document(userData.userid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(!task.getResult().exists()){
                    materequestRef.document(userData.userid).set(data).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(MateViewActivity.this,"메이트 신청 완료",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else{
                    materequestRef.document(userData.userid).update("requestlist", FieldValue.arrayUnion(docuid)).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(MateViewActivity.this,"메이트 신청 완료",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        matedatalistRef.document(docuid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(!task.getResult().exists()){
                    finish();
                }
            }
        });
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap=naverMap;

        if(routes!=null){
            Log.d("메이트게시물",routes.size()+"");
            LatLng[] range;
            pathOverlay.setMap(null);
            if(routes.size()>0){
                pathOverlay=new PathOverlay();
                pathOverlay.setColor(Color.BLUE);
                pathOverlay.setOutlineColor(Color.BLUE);
                pathOverlay.setWidth(5);

                pathOverlay.setCoords(routes);
                pathOverlay.setMap(naverMap);
                range=getMiddle(routes);
            }
            else{
                range=getMiddle(coords);
            }
            CameraUpdate cameraUpdate =CameraUpdate.fitBounds(new LatLngBounds(range[0],range[1]),50,150,50,50);
            naverMap.moveCamera(cameraUpdate);
        }



        if(markers.size()>0){
            //마커 초기화
            for(int i=0; i<markers.size(); ++i){
                markers.get(i).setMap(null);
            }
            markers.clear();

        }
        //markers세팅
        for(int i=0; i<locations.size(); ++i){
            Marker tmp=new Marker();
            tmp.setIcon(MarkerIcons.BLACK);
            tmp.setCaptionAligns(Align.Top);
            tmp.setPosition(coords.get(i));
            tmp.setWidth(63);
            tmp.setHeight(84);

            String destorderString="";
            switch (i){
                case 0:
                    destorderString+="출발지";
                    break;
                case 1:
                    destorderString+="1st"; break;
                case 2:
                    destorderString+="2nd"; break;
                case 3:
                    destorderString+="3rd"; break;
                default:
                    destorderString+=i+"th"; break;
            }
            if(i==0){
                tmp.setCaptionColor(Color.RED);
                tmp.setIconTintColor(Color.RED);
            }
            else{
                tmp.setCaptionColor(Color.BLUE);
                tmp.setIconTintColor(Color.BLUE);
            }
            tmp.setCaptionText(destorderString);
            markers.add(tmp);
            tmp.setMap(naverMap);
        }

    }

    public LatLng[] getMiddle(ArrayList<LatLng> coords){
        float maxlat=0,minlat=1000,maxlon=0,minlon=1000;
        float midlat, midlon;
        for(int i=0; i<coords.size(); ++i){
            if(maxlat<(float)coords.get(i).latitude){
                maxlat= (float) coords.get(i).latitude;
            }
            if(minlat>(float)coords.get(i).latitude){
                minlat=(float) coords.get(i).latitude;
            }
            if(maxlon<(float)coords.get(i).longitude){
                maxlon= (float) coords.get(i).longitude;
            }
            if(minlon>(float)coords.get(i).longitude){
                minlon=(float) coords.get(i).longitude;
            }
        }
        midlat=(maxlat+minlat)/2;
        midlon=(maxlon+minlon)/2;
        LatLng result[]=new LatLng[3];
        result[0]=new LatLng(minlat,minlon);
        result[1]=new LatLng(maxlat,maxlon);
        result[2]=new LatLng((maxlat+minlat)/2, (maxlon+minlon)/2);

        Log.d("카메라 좌표","min: "+result[0]+", max: "+result[1]);

        return result;
    }

    public void getMateData(){
        matedataRef.document(docuid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot document=task.getResult();
                if(!document.exists()){
                    Toast.makeText(MateViewActivity.this,"삭제되었거나 없는 게시물입니다.",Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                else{
                    locations= (ArrayList<String>) document.get("locations_name");
                    for(Map<String,Object> map:(ArrayList<Map<String,Object>>)document.get("locations_coordinate")){
                        LatLng tmpll=new LatLng(UserData.setdouble(map.get("latitude")) , UserData.setdouble(map.get("longitude")));
                        coords.add(tmpll);
                    }
                    ArrayList<Map<String,Object>> tmproute= (ArrayList<Map<String, Object>>) document.get("route");
                    if(tmproute.size()>0){
                        for(Map<String,Object> map:tmproute){
                            LatLng tmpll=new LatLng(UserData.setdouble(map.get("latitude")), UserData.setdouble(map.get("longitude")));
                            routes.add(tmpll);
                        }
                    }

                    title= (String) document.get("title");
                    content= (String) document.get("content");

                    titletxt.setText(title);
                    contenttxt.setText(content);
                    String[] tmpdate=date.split("~");
                    if(tmpdate.length>=2){
                        datetxt.setText(tmpdate[0]+" ~\n "+tmpdate[1]);
                    } else {
                        datetxt.setText(date.trim());
                    }
                    String locationlist="";
                    for(int i=0; i<locations.size(); ++i){
                        String order;
                        if(i==0){
                            order="출발지";
                        }
                        else{
                            order=i+"";
                        }
                        locationlist+="("+order+")"+locations.get(i)+"\n";
                    }
                    if(locationlist.length()>0){
                        locationlist=locationlist.substring(0,locationlist.length()-1);
                        locationtxt.setText(locationlist);
                    }

                    mapFragment.getMapAsync(MateViewActivity.this);

                    setuserdata((String) document.get("userid"));
                }
            }
        });
    }

    public void setuserdata(String userid){
        users.document(userid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot document=task.getResult();
                String gender="";
                if("M".equals(document.getString("gender"))){
                    gender="남성";
                }
                else{
                    gender="여성";
                }
                String info=String.format("%s (%s/%s)",document.getString("appname"),gender,document.getString("age"));
                userinfo.setText(info);

                if(!document.getString("title").equals("없음")){
                    usertitle.setText(document.getString("title"));
                }
                else{
                    usertitle.setText("");
                }
                String urlstr= (String) document.get("profileImagebig");
                if(urlstr!=null && !urlstr.equals("")){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            HttpURLConnection connection = null;
                            InputStream is = null;
                            try {
                                URL imgUrl = new URL(urlstr);
                                connection = (HttpURLConnection) imgUrl.openConnection();
                                connection.setDoInput(true); //url로 input받는 flag 허용
                                connection.connect(); //연결
                                is = connection.getInputStream(); // get inputstream
                                Bitmap retBitmap = BitmapFactory.decodeStream(is);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        userimg.setImageBitmap(retBitmap);
                                    }
                                });

                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                if (connection != null) {
                                    connection.disconnect();
                                }
                            }
                        }
                    }).start();
                }


            }
        });
    }

    private void applyPrefillFromIntent(){
        if(prefillTitle!=null && !prefillTitle.isEmpty()){
            titletxt.setText(prefillTitle);
        }
        if(prefillWriter!=null && !prefillWriter.isEmpty()){
            userinfo.setText(prefillWriter);
        }
        if(date!=null && !date.trim().isEmpty()){
            datetxt.setText(date.trim());
        }
        if(prefillLocations!=null && prefillLocations.size()>0){
            StringBuilder sb = new StringBuilder();
            for(int i=0; i<prefillLocations.size(); ++i){
                String order = (i==0) ? "출발지" : String.valueOf(i);
                sb.append("(").append(order).append(")").append(prefillLocations.get(i));
                if(i<prefillLocations.size()-1){
                    sb.append("\n");
                }
            }
            locationtxt.setText(sb.toString());
        }
    }
}
