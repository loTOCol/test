package com.example.walkingmate.feature.feed.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.walkingmate.R;
import com.example.walkingmate.feature.user.data.UserData;
import com.example.walkingmate.feature.feed.fragment.ImageFragment;
import com.example.walkingmate.feature.feed.data.FeedData;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.auth.User;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.MarkerIcons;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class ViewFeedActivity extends AppCompatActivity implements OnMapReadyCallback {

    FirebaseFirestore fb=FirebaseFirestore.getInstance();
    CollectionReference feeddata=fb.collection("feed");
    CollectionReference feedlist=fb.collection("feedlist");

    FeedData feedData;
    ArrayList<String> imgurls=new ArrayList<>();
    String title,content,weather,emotion, useridcheck, docuid;

    Switch openset;

    boolean checkset=false;
    boolean initmsg=false;


    TextView distxt,steptxt,timetxt, datetxt, curpage, contenttxt;
    ListView listView;
    ImageButton weatherbtn,emotionbtn, back;


    MapFragment mapFragmentFeed;
    ArrayList<ImageFragment> fragments=new ArrayList<>();


    ViewPager2 viewPager2;
    FragmentStateAdapter fragmentStateAdapter;

    FrameLayout imagebacklayout;

    private NaverMap naverMap;
    LinearLayout FeedMapLayout;
    PathOverlay pathOverlay;
    ArrayList<Marker> markers;

    int selected=-1;

    int curposition=0;

    UserData userData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_feed);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        userData=UserData.loadData(this);

        imagebacklayout=findViewById(R.id.image_feedview);

        distxt=findViewById(R.id.distext_feedview);
        steptxt=findViewById(R.id.feedviewstep);
        timetxt=findViewById(R.id.feedview_time);
        datetxt=findViewById(R.id.feedview_date);
        curpage=findViewById(R.id.curpage_feedview);
        contenttxt=findViewById(R.id.record_feedview);

        weatherbtn=findViewById(R.id.weather_feedview);
        emotionbtn=findViewById(R.id.emotion_feedview);
        back=findViewById(R.id.back_feedview);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        findViewById(R.id.delete_viewfeed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final LinearLayout deldialog=(LinearLayout) View.inflate(ViewFeedActivity.this,R.layout.dialog_confirm_delete, null);
                TextView dialogtitle=deldialog.findViewById(R.id.title_deletedialog);
                dialogtitle.setText(title);
                AlertDialog.Builder adbuilder=new AlertDialog.Builder(ViewFeedActivity.this);
                AlertDialog alertDialog=adbuilder.setView(deldialog).setPositiveButton("삭제", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        feeddata.document(docuid).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                feedlist.document(docuid).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        Toast.makeText(getApplicationContext(),"삭제완료",Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                });
                            }
                        });
                    }
                }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        return;
                    }
                }).create();
                alertDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                alertDialog.show();
                alertDialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                alertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            }
        });

        openset=findViewById(R.id.openset_viewfeed);
        openset.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                feeddata.document(docuid).update("isOpen",b).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        feedlist.document(docuid).update("isOpen",b).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                String setting="비공개";
                                if(b){
                                    setting="공개";
                                }
                                if(initmsg){
                                    Toast.makeText(ViewFeedActivity.this, "공개 여부 설정: "+setting,Toast.LENGTH_SHORT).show();
                                }
                                else{
                                    initmsg=true;
                                }

                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("공개설정에러",e.toString());
                    }
                });
            }
        });

        Intent getFeed=getIntent();
        String fileName=getFeed.getStringExtra("filename");

        mapFragmentFeed=(MapFragment)getSupportFragmentManager().findFragmentById(R.id.feedviewmap);

        viewPager2=findViewById(R.id.feedviewpager);
        setviewpager();

        getData(fileName);

    }

    public void getData(String filename){

        feeddata.document(filename).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(!task.isSuccessful()){
                    return;
                }
                DocumentSnapshot documentSnapshot=task.getResult();
                if(documentSnapshot==null){
                    Toast.makeText(getApplicationContext(),"삭제된 게시물입니다.",Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                docuid=documentSnapshot.getId();
                feedData =FeedData.decodeFeed((String) documentSnapshot.get("feeddata"));
                imgurls= (ArrayList<String>) documentSnapshot.get("images");
                title = (String) documentSnapshot.get("title");
                content= (String) documentSnapshot.get("content");
                weather=(String)documentSnapshot.get("weather");
                emotion=(String)documentSnapshot.get("emotion");

                //본인이면 공개여부 수정버튼,삭제버튼 활성화
                useridcheck=documentSnapshot.getString("userid");
                if(userData.userid.equals(useridcheck)){
                    openset.setVisibility(View.VISIBLE);
                    findViewById(R.id.delete_viewfeed).setVisibility(View.VISIBLE);
                }


                Log.d("문서 유저아이디",useridcheck);

                openset.setChecked(documentSnapshot.getBoolean("isOpen"));
                checkset=true;



                if(imgurls.size()==0){
                    findViewById(R.id.loading_feedview).setVisibility(View.INVISIBLE);
                }

                markers=feedData.markerList;
                mapFragmentFeed.getMapAsync(ViewFeedActivity.this);

                String diststr=String.format("%.3f km",feedData.displacement);
                String stepstr=String.format("%d 걸음",feedData.step);
                String[] start=feedData.timecheck[0].split("_");
                String[] end=feedData.timecheck[1].split("_");
                String timestr=String.format("%s:%s ~ %s:%s",start[3].replace("시",""),
                        start[4].replace("분",""),end[3].replace("시",""),end[4].replace("분",""));

                String datestr=feedData.timecheck[0].replace("_", " ");

                listView=findViewById(R.id.loclist_feedview);
                LocAdapter LocationAdapter=new LocAdapter(ViewFeedActivity.this);
                listView.setAdapter(LocationAdapter);
                setListViewHeightBasedOnItems(listView);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(imgurls.size()>0){
                            imagebacklayout.setBackgroundResource(R.color.transparent);
                        }
                        for(int i=0; i<imgurls.size(); ++i){
                            if(i==imgurls.size()-1){
                                getBitmap(imgurls.get(i),true);
                            }
                            else{
                                getBitmap(imgurls.get(i),true);
                            }
                            ;
                        }
                    }
                }).start();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                distxt.setText(diststr);
                                steptxt.setText(stepstr);
                                timetxt.setText(timestr);
                                datetxt.setText(datestr);
                                contenttxt.setText(content);

                            }
                        });
                    }
                }).start();
                setButton(emotion,weather);
            }
        });
    }


    public void setButton(String emotion, String weather){
        switch(emotion){
            case "angry":
                emotionbtn.setImageResource(R.drawable.feeling_angry_xml); break;
            case "crying":
                emotionbtn.setImageResource(R.drawable.feeling_crying_xml);break;
            case "heart":
                emotionbtn.setImageResource(R.drawable.feeling_heart_xml);break;
            case "neutral":
                emotionbtn.setImageResource(R.drawable.feeling_neutral_xml);break;
            case "smile":
                emotionbtn.setImageResource(R.drawable.feeling_smiling_xml);break;
            case "tired":
                emotionbtn.setImageResource(R.drawable.feeling_tired_xml);break;
            default:
                emotionbtn.setImageResource(R.drawable.feeling_smiling_xml);break;
        }
        switch (weather){
            case "cloudy":
                weatherbtn.setImageResource(R.drawable.weather_cloudy_xml);break;
            case "fog":
                weatherbtn.setImageResource(R.drawable.weather_fog_xml);break;
            case "moon":
                weatherbtn.setImageResource(R.drawable.weather_moon_xml);break;
            case "rainbow":
                weatherbtn.setImageResource(R.drawable.weather_rainbow_xml);break;
            case "rainy":
                weatherbtn.setImageResource(R.drawable.weather_rainy_xml);break;
            case "snow":
                weatherbtn.setImageResource(R.drawable.weather_snow_xml);break;
            case "sunny":
                weatherbtn.setImageResource(R.drawable.weather_sunny_xml);break;
            case "windy":
                weatherbtn.setImageResource(R.drawable.weather_windy_xml);break;
            default:
                weatherbtn.setImageResource(R.drawable.weather_sunny_xml);break;
        }

    }

    public void getBitmap(String urlstr, boolean end){
        HttpURLConnection connection = null;
        InputStream is = null;
        Bitmap retBitmap = null;
        try {
            URL imgUrl = new URL(urlstr);
            Log.d("피드이미지url",imgUrl.toString());
            connection = (HttpURLConnection) imgUrl.openConnection();
            connection.setDoInput(true); //url로 input받는 flag 허용
            connection.connect(); //연결
            is = connection.getInputStream(); // get inputstream
            retBitmap = BitmapFactory.decodeStream(is);
        } catch (Exception e) {
            Log.d("피드이미지에러메세지",e.toString());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        if(retBitmap!=null){
            fragments.add(new ImageFragment(fragments.size()+1,retBitmap));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(end){
                                setviewpager();
                                findViewById(R.id.loading_feedview).setVisibility(View.INVISIBLE);
                            }
                        }
                    });
                }
            }).start();
        }
        else{
            Log.d("피드이미지","에러");
        }
    }

    public void setviewpager(){

        viewPager2.setAdapter(null);
        fragmentStateAdapter=new ViewFeedActivity.MyAdapter(this);
        viewPager2.setAdapter(fragmentStateAdapter);

        viewPager2.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        viewPager2.setCurrentItem(curposition,false);
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                curposition=position;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                curpage.setText(String.format("%d / %d",curposition+1,fragments.size()));
                            }
                        });
                    }
                }).start();
                Log.d("피드기록 위치",""+position);
            }
        });

    }

    public class MyAdapter extends FragmentStateAdapter {


        public MyAdapter(FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public ImageFragment createFragment(int position) {
            return fragments.get(position);
        }

        @Override
        public int getItemCount() {
            return fragments.size();
        }


    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        FeedMapLayout=findViewById(R.id.FeedviewMapLayout);
        this.naverMap=naverMap;

        pathOverlay=new PathOverlay();
        pathOverlay.setColor(Color.BLUE);
        pathOverlay.setOutlineColor(Color.BLUE);
        pathOverlay.setWidth(5);

        pathOverlay.setCoords(feedData.coordList);
        pathOverlay.setMap(naverMap);

        if(markers.size()>0){
            for(int i=0; i<markers.size(); ++i){
                markers.get(i).setMap(null);
            }
            for(int i=0; i<markers.size(); ++i){
                markers.get(i).setIcon(MarkerIcons.BLACK);
                if(selected==i){
                    markers.get(i).setIconTintColor(Color.RED);
                }
                else{
                    markers.get(i).setIconTintColor(Color.BLUE);
                }
                markers.get(i).setMap(naverMap);
            }
        }

        LatLng[] range=getMiddle(feedData.coordList);
        CameraUpdate cameraUpdate =CameraUpdate.fitBounds(new LatLngBounds(range[0],range[1]),100,200,100,200);
        naverMap.moveCamera(cameraUpdate);
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

    public class LocAdapter extends BaseAdapter {

        Context context;
        LayoutInflater layoutInflater;


        public LocAdapter(Context context){
            this.context = context;
            this.layoutInflater = LayoutInflater.from(context);

        }

        @Override
        public int getCount() {
            return markers.size();
        }

        @Override
        public Object getItem(int i) {
            return markers.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertview, ViewGroup viewGroup) {
            View view=layoutInflater.inflate(R.layout.item_mate_route_stop, null);
            View emptyview=layoutInflater.inflate(R.layout.item_common_empty,null);
            if(markers.size()==0){
                return null;
            }
            else{
                ImageView imageView=view.findViewById(R.id.mate_image);
                TextView textView=view.findViewById(R.id.matelist);

                if(i==selected){
                    imageView.setImageResource(R.drawable.pin_red);
                }
                else{
                    imageView.setImageResource(R.drawable.pin_blue);
                }

                textView.setText(markers.get(i).getCaptionText());

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(selected==i){
                            selected=-1;
                        }
                        else{
                            selected=i;
                        }
                        notifyDataSetChanged();
                        mapFragmentFeed.getMapAsync(ViewFeedActivity.this);
                    }
                });

                return view;
            }


        }
    }
    public static boolean setListViewHeightBasedOnItems(ListView listView) {

        ViewFeedActivity.LocAdapter listAdapter = (ViewFeedActivity.LocAdapter) listView.getAdapter();
        if (listAdapter != null) {

            int numberOfItems = listAdapter.getCount();

            // Get total height of all items.
            int totalItemsHeight = 0;
            for (int itemPos = 0; itemPos < numberOfItems; itemPos++) {
                View item = listAdapter.getView(itemPos, null, listView);
                float px = 500 * (listView.getResources().getDisplayMetrics().density);
                item.measure(View.MeasureSpec.makeMeasureSpec((int) px, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                totalItemsHeight += item.getMeasuredHeight();
            }

            // Get total height of all item dividers.
            int totalDividersHeight = listView.getDividerHeight() *
                    (numberOfItems - 1);
            // Get padding
            int totalPadding = listView.getPaddingTop() + listView.getPaddingBottom();

            Log.d("리스트뷰",totalItemsHeight+","+totalDividersHeight+","+totalPadding);
            // Set list height.
            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height = (int) ((totalItemsHeight + totalDividersHeight + totalPadding));
            listView.setLayoutParams(params);
            listView.requestLayout();
            //setDynamicHeight(listView);
            return true;

        } else {
            return false;
        }
    }

}
