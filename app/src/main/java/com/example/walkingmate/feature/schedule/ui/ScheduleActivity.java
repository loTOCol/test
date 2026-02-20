package com.example.walkingmate.feature.schedule.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Rating;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.walkingmate.R;
import com.example.walkingmate.feature.user.data.UserData;
import com.example.walkingmate.feature.user.ui.UserProfileActivity;
import com.example.walkingmate.feature.chat.model.ChatRoom;
import com.example.walkingmate.feature.chat.ui.ChatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Align;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.util.MarkerIcons;

import org.checkerframework.checker.units.qual.A;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ScheduleActivity extends AppCompatActivity implements OnMapReadyCallback {
    FirebaseFirestore fb=FirebaseFirestore.getInstance();
    CollectionReference users=fb.collection("users");
    CollectionReference schedule=fb.collection("schedule");

    private static final int ACCESS_LOCATION_PERMISSION_REQUEST_CODE = 100;

    private FusedLocationSource locationSource;

    Map<String, Scheduledocu> schedules=new HashMap<>();
    ArrayList<String> docuids=new ArrayList<>();

    UserData userData;

    ScheduleAdapter scheduleAdapter;
    UserAdapter userAdapter;
    ListView schedulelist,userlist;
    ImageButton back,closemap,closeuser;
    LinearLayout maplayout,userlayout;
    TextView maptitle,usertitle,loadinguser;

    MapFragment mapFragment;
    NaverMap naverMap;
    ArrayList<Marker> markers=new ArrayList<>();
    Scheduledocu curSchdule;
    String curdocuid;

    HashMap<String,String>usernames=new HashMap<>();
    HashMap<String, Bitmap>userimgs=new HashMap<>();

    int checkloadprofile=0;

    boolean useroption=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        userData=UserData.loadData(this);

        schedulelist=findViewById(R.id.schedule_list);
        userlist=findViewById(R.id.userlist_schedule);
        back=findViewById(R.id.back_schedule);
        closemap=findViewById(R.id.close_map_schedule);
        closeuser=findViewById(R.id.close_user_schedule);
        maplayout=findViewById(R.id.maplayout_schedule);
        userlayout=findViewById(R.id.userlistlayout_schedule);
        maptitle=findViewById(R.id.title_map_schedule);
        usertitle=findViewById(R.id.title_user_schedule);
        loadinguser=findViewById(R.id.loading_userlist_schedule);

        mapFragment=(MapFragment)getSupportFragmentManager().findFragmentById(R.id.map_schedule);
        locationSource = new FusedLocationSource(this, ACCESS_LOCATION_PERMISSION_REQUEST_CODE);

        closemap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                maplayout.setVisibility(View.INVISIBLE);
            }
        });
        closeuser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getschedule();
                userlayout.setVisibility(View.INVISIBLE);
            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        scheduleAdapter=new ScheduleAdapter(this);
        schedulelist.setAdapter(scheduleAdapter);
        userAdapter=new UserAdapter(this);
        userlist.setAdapter(userAdapter);
        getschedule();
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap=naverMap;

        ArrayList<LatLng> locations=new ArrayList<>();
        for(int i=0; i<curSchdule.locations.size(); ++i){
            Map<String,Object> locs=curSchdule.locations.get(i);
            LatLng tmp=new LatLng(UserData.setdouble(locs.get("latitude")), UserData.setdouble(locs.get("longitude")));
            locations.add(tmp);
        }

        naverMap.setLocationSource(locationSource);//네이버지도상 위치값을 받아온 현재 위치값으로 설정
        naverMap.setLocationTrackingMode(LocationTrackingMode.None);

        LatLng[] range=getMiddle(locations);
        CameraUpdate cameraUpdate =CameraUpdate.fitBounds(new LatLngBounds(range[0],range[1]),50,150,50,50);
        naverMap.moveCamera(cameraUpdate);

        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setLocationButtonEnabled(true);
        uiSettings.setZoomControlEnabled(false);



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
            tmp.setPosition(locations.get(i));
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

    public class ScheduleAdapter extends BaseAdapter {

        LayoutInflater layoutInflater;
        Context context;

        public ScheduleAdapter(Context context){
            this.context=context;
            layoutInflater=LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return docuids.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view=layoutInflater.inflate(R.layout.item_schedule_before,null);
            View emptyview=layoutInflater.inflate(R.layout.item_common_empty,null);

            TextView title,time;
            Button review,chat,location, user,delete;

            String docuid=docuids.get(position);
            Scheduledocu tmpdocu=schedules.get(docuid);
            String name;
            if(tmpdocu.type){
                name="[메이트]";
            }
            else{
                name="[산책]";
            }
            name+= tmpdocu.start;
            try {
                if(checkend(tmpdocu.end)){
                    Log.d("약속 완료아이템",docuid);
                    view=layoutInflater.inflate(R.layout.item_schedule_after,null);
                    review=view.findViewById(R.id.review_schedule);
                    title=view.findViewById(R.id.title_scheduleaf);
                    delete=view.findViewById(R.id.delete_schedule);

                    title.setText(name);

                    String finalName1 = name;
                    review.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            useroption=false;
                            curSchdule=tmpdocu;
                            curdocuid=docuid;
                            usertitle.setText(finalName1);
                            userlayout.setVisibility(View.VISIBLE);
                            maplayout.setVisibility(View.INVISIBLE);
                            loadinguser.setVisibility(View.VISIBLE);
                            getuserdata();
                        }
                    });

                    delete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            removeschedule(docuid);
                        }
                    });
                }
                else{
                    view=layoutInflater.inflate(R.layout.item_schedule_before,null);
                    time=view.findViewById(R.id.date_schedule);
                    chat=view.findViewById(R.id.gochat_schedule);
                    location=view.findViewById(R.id.golocation_schedule);
                    user=view.findViewById(R.id.gouser_schedule);
                    title=view.findViewById(R.id.title_schedulebef);

                    title.setText(name);
                    time.setText(tmpdocu.start+" \n~ "+tmpdocu.end);

                    String finalName = name;
                    location.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            curSchdule=tmpdocu;
                            curdocuid=docuid;
                            maptitle.setText(finalName);
                            mapFragment.getMapAsync(ScheduleActivity.this);
                            maplayout.setVisibility(View.VISIBLE);
                            userlayout.setVisibility(View.INVISIBLE);
                        }
                    });
                    user.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            useroption=true;
                            curSchdule=tmpdocu;
                            curdocuid=docuid;
                            usertitle.setText(finalName);
                            userlayout.setVisibility(View.VISIBLE);
                            maplayout.setVisibility(View.INVISIBLE);
                            loadinguser.setVisibility(View.VISIBLE);
                            getuserdata();
                        }
                    });

                    chat.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            curSchdule=tmpdocu;
                            curdocuid=docuid;
                            gochatroom();
                        }
                    });
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if(docuids.size()==0){
                return emptyview;
            }

            return view;
        }
    }

    public void removeschedule(String docuid){
        docuids.remove(docuid);
        schedules.remove(docuid);
        schedule.document(userData.userid).update(docuid,FieldValue.delete());
        scheduleAdapter.notifyDataSetChanged();
    }

    public void getuserdata(){
        usernames.clear();
        userimgs.clear();
        checkloadprofile=0;
        userAdapter.notifyDataSetChanged();

        for(String s:curSchdule.users){
            users.document(s).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){
                        String urlstr= (String) task.getResult().get("profileImagesmall");
                        usernames.put(s, (String) task.getResult().get("appname"));
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
                                    //없거나 프로필이 바뀐경우 업데이트
                                    if(userimgs.get(s)==null||userimgs.get(s)!=retBitmap){
                                        userimgs.put(s,retBitmap);
                                    }
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            checkloadprofile++;
                                            if(checkloadprofile==curSchdule.users.size()){
                                                userAdapter.notifyDataSetChanged();
                                                loadinguser.setVisibility(View.INVISIBLE);
                                            }
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
    }

    public void gochatroom(){
        DatabaseReference dr= FirebaseDatabase.getInstance().getReference("Chatrooms");

        String roomid="";
        for(String s:schedules.keySet()){
            if(schedules.get(s)==curSchdule){
                roomid=s;
            }
        }


        Log.d("채팅방 아이디",roomid);
        //채팅방 존재여부 체크 후 이동
        String finalRoomid = roomid;
        dr.child(roomid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.getValue(ChatRoom.class)!=null){
                    Map<String, Boolean> userstmp= snapshot.getValue(ChatRoom.class).userids;
                    Log.d("스케줄 채팅",userstmp.toString()+", 챗룸체크"+snapshot.getValue(ChatRoom.class).roomname);
                    ArrayList<String> enteredusers=new ArrayList<>();
                    for(String s:userstmp.keySet()){
                        if(userstmp.get(s)){
                            enteredusers.add(s);
                        }
                    }
                    //전에 나갔던 채팅방이면 다시 true로 바꾸며 들어옴
                    if(snapshot.getValue(ChatRoom.class).userids.get(userData.userid).equals(false)){
                        Map<String,Object> tmpuser=new HashMap<>();
                        tmpuser.put(userData.userid,true);
                        dr.child(finalRoomid).child("userids").updateChildren(tmpuser).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                enteredusers.add(userData.userid);
                                Intent intent=new Intent(ScheduleActivity.this,ChatActivity.class);
                                intent.putExtra("roomid",finalRoomid);
                                intent.putExtra("userids",enteredusers);
                                startActivity(intent);
                            }
                        });


                    }
                    else{
                        Intent intent=new Intent(ScheduleActivity.this,ChatActivity.class);
                        intent.putExtra("roomid",finalRoomid);
                        intent.putExtra("userids",enteredusers);
                        startActivity(intent);
                    }
                }
                else{
                    Toast.makeText(ScheduleActivity.this, "더 이상 존재하지 않는 채팅방입니다.",Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public class UserAdapter extends BaseAdapter {

        LayoutInflater layoutInflater;
        Context context;

        public UserAdapter(Context context){
            this.context=context;
            layoutInflater=LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return userimgs.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if(useroption){
                view=layoutInflater.inflate(R.layout.item_chat_user,null);
                String userid=curSchdule.users.get(position);
                CircleImageView circleImageView=view.findViewById(R.id.userimg_chatlist);
                TextView usernametxt=view.findViewById(R.id.name_chatlist);
                LinearLayout body=view.findViewById(R.id.chatuserlist_body);
                body.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent=new Intent(ScheduleActivity.this, UserProfileActivity.class);
                        intent.putExtra("userid",userid);
                        startActivity(intent);
                    }
                });
                circleImageView.setImageBitmap(userimgs.get(userid));
                usernametxt.setText(usernames.get(userid));
            }
            else{
                view=layoutInflater.inflate(R.layout.item_schedule_review,null);
                String userid=curSchdule.users.get(position);
                CircleImageView circleImageView=view.findViewById(R.id.userimg_review);
                TextView usernametxt=view.findViewById(R.id.name_review);
                LinearLayout body=view.findViewById(R.id.body_review);
                body.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent=new Intent(ScheduleActivity.this, UserProfileActivity.class);
                        intent.putExtra("userid",userid);
                        startActivity(intent);
                    }
                });
                circleImageView.setImageBitmap(userimgs.get(userid));
                usernametxt.setText(usernames.get(userid));
                RatingBar ratingBar=view.findViewById(R.id.rating_review);

                Button finish=view.findViewById(R.id.finish_review);
                finish.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        double rate=ratingBar.getRating();
                        sendreivew(rate,userid,curSchdule.total);
                    }
                });

                if(userid.equals(userData.userid)){
                    return layoutInflater.inflate(R.layout.view_empty,null);
                }
            }



            return view;
        }
    }

    public void sendreivew(double rate, String userid, Long total){
        users.document(userid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                double rel=task.getResult().getLong("reliability");
                if(rate<=1){
                    rel-=3.0/total;
                }
                else if(rate<=2){
                    rel-=2.0/total;
                }
                else if(rate<=3){
                    rel-=1.0/total;
                }
                else if(rate<=4){
                    rel+=0;
                }
                else if(rate<=4.5){
                    rel+=1.0/total;
                }
                else{
                    rel+=2.0/total;
                }
                if(rel>100){
                    rel=100;
                }
                if(rel<0){
                    rel=0;
                }
                users.document(userid).update("reliability",rel).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        ArrayList<String> userstmp=curSchdule.users;
                        userstmp.remove(userid);
                        if(userstmp.size()==1){
                            removeschedule(curdocuid);
                            closeuser.performClick();
                        }
                        else{
                            schedule.document(userData.userid).update(curdocuid+".users",userstmp);
                            curSchdule.users=userstmp;
                            userimgs.remove(userid);
                            usernames.remove(userid);
                            userAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        });
    }


    public void setorder() throws ParseException {
        Scheduledocu[] scheduledocus=new Scheduledocu[docuids.size()];
        Scheduledocu[] tmpsche=new Scheduledocu[docuids.size()];
        for(int i=0; i<docuids.size(); ++i){
            scheduledocus[i]=schedules.get(docuids.get(i));
        }
        Arrays.sort(scheduledocus);
        int k=0;
        for(int i=0; i<docuids.size(); ++i){
            if(checkend(scheduledocus[i].end)){
                tmpsche[k]=scheduledocus[i];
                scheduledocus[i]=null;
                ++k;
            }
        }
        for(int i=0; i<docuids.size(); ++i){
            if(scheduledocus[i]!=null){
                tmpsche[k]=scheduledocus[i];
                ++k;
            }
        }

        ArrayList<String> newdocuids=new ArrayList<>();
        for(int i=0; i<tmpsche.length; ++i){
            for(String s:schedules.keySet()){
                if(schedules.get(s)==tmpsche[i]){
                    newdocuids.add(s);
                }
            }
        }
        docuids=newdocuids;
    }

    public void getschedule(){
        docuids.clear();
        schedules.clear();
        schedule.document(userData.userid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot documents=task.getResult();
                if(documents.exists()&&documents.getData().size()>0){
                    for(String s:documents.getData().keySet()){
                        docuids.add(s);
                    }
                    Log.d("문서 목록",docuids.toString());
                    for(String s:docuids){
                        HashMap<String,Object> document= (HashMap<String, Object>) documents.get(s);
                        Scheduledocu tmpschedule= new Scheduledocu(document);
                        schedules.put(s,tmpschedule);
                    }
                    try {
                        setorder();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    scheduleAdapter.notifyDataSetChanged();
                }
                else{
                    findViewById(R.id.nulltext_schedule).setVisibility(View.VISIBLE);
                }

            }
        });
    }

    public boolean checkend(String end) throws ParseException {
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy/MM/dd HH:mm");
        Date now=new Date();
        Date enddate=sdf.parse(end);
        int result=enddate.compareTo(now);
        if(result<0){
            return true;//종료시간 지남
        }
        else{
            return false;//종료 이전
        }
    }

    public class Scheduledocu implements Comparable<Scheduledocu>{
        ArrayList<Map<String,Object>> locations;
        String start;
        String end;
        boolean type;//true 메이트, false 산책
        ArrayList<String> users;
        String name;
        Long total;


        public Scheduledocu(HashMap<String,Object> tmp) {
            this.locations = (ArrayList<Map<String, Object>>) tmp.get("locations");
            this.start = (String) tmp.get("start");
            this.end = (String) tmp.get("end");
            this.type = (boolean) tmp.get("type");
            this.users = (ArrayList<String>) tmp.get("users");
            this.name = (String) tmp.get("name");
            this.total= (Long) tmp.get("total");
        }

        @Override
        public int compareTo(Scheduledocu scheduledocu) {
            return this.start.compareTo(scheduledocu.start);
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
}
