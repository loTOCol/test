package com.example.walkingmate.feature.mate.ui;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.walkingmate.feature.common.ui.DateSelector;
import com.example.walkingmate.R;
import com.example.walkingmate.feature.mate.data.repository.MateRepository;
import com.example.walkingmate.feature.map.ui.LocationSearchActivity;
import com.example.walkingmate.feature.common.ui.TimeSelector;
import com.example.walkingmate.feature.user.data.UserData;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
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

import org.w3c.dom.Text;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;

public class MateWriteActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final MateRepository mateRepository = MateRepository.getInstance();
    CollectionReference matedataRef = mateRepository.getMateDataRef();
    CollectionReference matedatalistRef = mateRepository.getMateDataListRef();

    EditText title,yeartxt,montxt,daytxt,hourtxt,mintxt,takendaytxt,takenhourtxt,takenmintxt,contentstxt;
    Spinner gender,age;
    Button search,finish;
    ImageButton back;
    ListView loclist;
    ScrollView scrollView;

    int year,mon,day,hour,min,taken;
    String titlestr,genderstr,agestr,contentstr;

    ArrayList<LatLng> routecoords;
    ArrayList<LatLng> loccoords=new ArrayList<>();
    ArrayList<String> locnames=new ArrayList<>();
    ArrayList<Marker> markers=new ArrayList<>();

    NaverMap naverMap;
    PathOverlay pathOverlay=new PathOverlay();
    MapFragment mapFragmentFeed;

    LocationAdapter LocationAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mate_write);

        scrollView=findViewById(R.id.scrollView_mate);


        title=findViewById(R.id.title_mate);
        yeartxt=findViewById(R.id.year_mate);
        montxt=findViewById(R.id.month_mate);
        daytxt=findViewById(R.id.day_mate);
        hourtxt=findViewById(R.id.hour_mate);
        mintxt=findViewById(R.id.min_mate);
        takendaytxt=findViewById(R.id.taken_time_day);
        takenhourtxt=findViewById(R.id.taken_time_hour);
        takenmintxt=findViewById(R.id.taken_time_minute);
        contentstxt=findViewById(R.id.contents_box);

        gender=findViewById(R.id.spinner_sex_mate);
        age=findViewById(R.id.spinner_age_mate);

        search=findViewById(R.id.search_mate);
        finish=findViewById(R.id.finish_mate);

        back=findViewById(R.id.back_mate);

        loclist=findViewById(R.id.loclist_mate);

        markers=new ArrayList<>();

        setHintTime();
        // Default expected exercise time to 30 minutes.
        takendaytxt.setText("0");
        takenhourtxt.setText("0");
        takenmintxt.setText("30");

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getsearchResult.launch(new Intent(MateWriteActivity.this,LocationSearchActivity.class));
            }
        });

        findViewById(R.id.selectdate_matewrite).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDateResult.launch(new Intent(MateWriteActivity.this,DateSelector.class));
            }
        });

        findViewById(R.id.selecttime_matewrite).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getTimeResult.launch(new Intent(MateWriteActivity.this,TimeSelector.class));
            }
        });

        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyDefaultStartTimeIfEmpty();
                applyDefaultTakenTimeIfEmpty();
                try{
                    year=Integer.parseInt(yeartxt.getText().toString());
                    mon=Integer.parseInt(montxt.getText().toString());
                    day=Integer.parseInt(daytxt.getText().toString());
                    hour=Integer.parseInt(hourtxt.getText().toString());
                    min=Integer.parseInt(mintxt.getText().toString());

                    taken=(Integer.parseInt(takendaytxt.getText().toString())*60*24)+
                            (Integer.parseInt(takenhourtxt.getText().toString())*60)+
                            Integer.parseInt(takenmintxt.getText().toString());
                    Log.d("메이트 소요시간",Integer.parseInt(takendaytxt.getText().toString())+","+Integer.parseInt(takenhourtxt.getText().toString())+","+Integer.parseInt(takenmintxt.getText().toString()));

                }catch (NumberFormatException e){
                    e.printStackTrace();
                    Toast.makeText(MateWriteActivity.this,"입력값이 잘못되었습니다.",Toast.LENGTH_SHORT).show();
                    return;
                }catch (Exception ee){
                    ee.printStackTrace();
                    return;
                }
                if(CheckValues(year,mon,day,hour,min,taken)){
                    agestr=age.getSelectedItem().toString();
                    genderstr=gender.getSelectedItem().toString();
                    titlestr=title.getText().toString().trim();
                    contentstr=contentstxt.getText().toString();
                    try {
                        sendData();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    finish();
                }
                else{
                    Toast.makeText(MateWriteActivity.this,"입력값이 잘못되었습니다. 입력란을 다시 확인해주세요.",Toast.LENGTH_SHORT).show();
                }
            }
        });

        mapFragmentFeed=(MapFragment)getSupportFragmentManager().findFragmentById(R.id.matewriteMap);

        LocationAdapter=new LocationAdapter(this);
        loclist.setAdapter(LocationAdapter);




    }

    public String getStartlocation(String starts){
        String location=starts.split(" ")[0];
        if(location.equals("서울특별시")||location.equals("경기도")){
            location="서울/경기";
        }
        else if(location.equals("제주특별자치도")){
            location="제주도";
        }
        else if(location.contains("시")){
            location=location.substring(0,2);
        }
        return location;
    }


    public void sendData() throws ParseException {
        HashMap<String, Object> data=new HashMap<>();

        UserData userData=UserData.loadData(MateWriteActivity.this);

        String usergender;
        if(userData.gender.equals("M")){
            usergender="남성";
        }
        else{
            usergender="여성";
        }

        String blockgender;
        if(genderstr.equals("남성")){
            blockgender="여성";
        }
        else if(genderstr.equals("여성")){
            blockgender="남성";
        }
        else{
            blockgender="무관";
        }

        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String writetime=sdf.format(date);

        String documentID= writetime+"@"+userData.userid;
        Log.d("테스트",documentID);
        data.put("userid", userData.userid);
        data.put("userage", userData.age);
        data.put("usergender",usergender);

        data.put("writetime",writetime);
        data.put("title",titlestr);
        data.put("age",agestr);
        data.put("gender",genderstr);
        data.put("year",year);
        data.put("month", mon);
        data.put("day", day);
        data.put("hour", hour);
        data.put("minute", min);
        data.put("takentime", taken);
        data.put("locations_name",locnames);
        data.put("startlocation",getStartlocation(locnames.get(0)));
        data.put("starttime",getstartdatestr(year,mon,day));
        data.put("endtime",getenddatestr(year,mon,day,hour,min,taken));
        data.put("blockgender",blockgender);

        matedatalistRef.document(documentID).set(data);

        data.put("content",contentstr);
        data.put("locations_coordinate",loccoords);
        data.put("route",routecoords);
        matedataRef.document(documentID).set(data).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(getApplicationContext(),"작성 완료되었습니다.",Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(),"작성 실패하였습니다.",Toast.LENGTH_SHORT).show();
            }
        });
    }

    public String getstartdatestr(Object y, Object m, Object d) throws ParseException {
        String start=String.format("%04d/%02d/%02d",y,m,d);
        return start;
    }

    public String getenddatestr(Object y, Object m, Object d, Object h, Object min, Object taken) throws ParseException {
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy/MM/dd HH:mm");
        String start=String.format("%04d/%02d/%02d %02d:%02d",y,m,d,h,min);
        Date date=sdf.parse(start);
        Calendar calendar=Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, (Integer) taken);
        return sdf.format(calendar.getTime()).split(" ")[0];
    }

    //현재 시간을 힌트로 표시
    public void setHintTime(){
        SimpleDateFormat format=new SimpleDateFormat("yyyy MM dd HH mm");
        long now=System.currentTimeMillis();
        Date date=new Date(now);
        String[] getTime=format.format(date).split(" ");
        yeartxt.setHint(getTime[0]);
        montxt.setHint(getTime[1]);
        daytxt.setHint(getTime[2]);
        hourtxt.setHint(getTime[3]);
        mintxt.setHint(getTime[4]);

        yeartxt.setText(getTime[0]);

    }

    private boolean isBlank(EditText editText){
        return editText.getText().toString().trim().equals("");
    }

    // If user leaves all start-time fields blank, use current time.
    private void applyDefaultStartTimeIfEmpty(){
        boolean yearUnset = isBlank(yeartxt) || yeartxt.getText().toString().trim().equals(yeartxt.getHint().toString());
        if(yearUnset && isBlank(montxt) && isBlank(daytxt) && isBlank(hourtxt) && isBlank(mintxt)){
            SimpleDateFormat format=new SimpleDateFormat("yyyy MM dd HH mm");
            long now=System.currentTimeMillis();
            Date date=new Date(now);
            String[] getTime=format.format(date).split(" ");
            yeartxt.setText(getTime[0]);
            montxt.setText(getTime[1]);
            daytxt.setText(getTime[2]);
            hourtxt.setText(getTime[3]);
            mintxt.setText(getTime[4]);
        }
    }

    // If all expected-time fields are blank, default to 30 minutes.
    private void applyDefaultTakenTimeIfEmpty(){
        if(isBlank(takendaytxt) && isBlank(takenhourtxt) && isBlank(takenmintxt)){
            takendaytxt.setText("0");
            takenhourtxt.setText("0");
            takenmintxt.setText("30");
        }
    }

    //입력값 오류체크
    public boolean CheckValues(int year,int mon, int day, int hour, int min, int taken_time){
        boolean result=true;

        //현재 시간 이전값인지 체크
        if(year<Integer.parseInt(yeartxt.getHint().toString())){
            result=false;
        }
        else if(year==Integer.parseInt(yeartxt.getHint().toString())){
            if(mon<Integer.parseInt(montxt.getHint().toString())){
                result=false;
            }
            else if(mon==Integer.parseInt(montxt.getHint().toString())){
                if(day<Integer.parseInt(daytxt.getHint().toString())){
                    result=false;
                }
                else if(day==Integer.parseInt(daytxt.getHint().toString())){
                    if(hour<Integer.parseInt(hourtxt.getHint().toString())){
                        result=false;
                    }
                    else if(hour==Integer.parseInt(hourtxt.getHint().toString())){
                        if(min<Integer.parseInt(mintxt.getHint().toString())){
                            result=false;
                        }
                    }
                }
            }
        }


        //잘못된 날짜값인지 체크
        try{
            String date=String.format("%02d%02d%02d%02d",mon,day,hour,min);
            SimpleDateFormat sdf=new SimpleDateFormat("MMddHHmm");
            sdf.setLenient(false);
            sdf.parse(date);
        }catch (Exception e){
            result=false;
        }

        //걸리는 시간 체크
        if(taken_time<=0){
            result=false;
        }

        //장소 선택여부 체크
        if(loccoords.size()==0){
            result=false;
        }

        //제목 입력 여부 체크 (내용은 공백 허용)
        if(title.getText().toString().trim().equals("")){
            result=false;
        }
        return result;
    }

    private final ActivityResultLauncher<Intent> getsearchResult= registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() ==RESULT_OK){
                    if(result.getData()!=null){
                        routecoords=(ArrayList<LatLng>) result.getData().getSerializableExtra("routecoords");
                        loccoords=(ArrayList<LatLng>) result.getData().getSerializableExtra("loccoords");
                        locnames=(ArrayList<String>) result.getData().getSerializableExtra("locnames");
                        Log.d("메이트","주소검색결과반환:"+loccoords.size());
                        mapFragmentFeed.getMapAsync(this);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        LocationAdapter.notifyDataSetChanged();
                                        setListViewHeightBasedOnItems(loclist);
                                    }
                                });
                            }
                        }).start();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> getDateResult= registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() ==RESULT_OK){
                    if(result.getData()!=null){
                        yeartxt.setText(result.getData().getIntExtra("mYear",2022)+"");
                        montxt.setText(result.getData().getIntExtra("mMonth",12)+"");
                        daytxt.setText(result.getData().getIntExtra("mDay",1)+"");
                    }
                }
            });

    private final ActivityResultLauncher<Intent> getTimeResult= registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() ==RESULT_OK){
                    if(result.getData()!=null){
                        hourtxt.setText(result.getData().getIntExtra("mHour",12)+"");
                        mintxt.setText(result.getData().getIntExtra("mMinute",11)+"");
                    }
                }
            });

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        Log.d("메이트", "onmapready");

        this.naverMap=naverMap;

        naverMap.setOnMapClickListener(new NaverMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
                scrollView.requestDisallowInterceptTouchEvent(true);
            }
        });

        if(routecoords!=null){
            Log.d("메이트게시물",routecoords.size()+"");
            LatLng[] range;
            pathOverlay.setMap(null);
            if(routecoords.size()>0){
                pathOverlay=new PathOverlay();
                pathOverlay.setColor(Color.BLUE);
                pathOverlay.setOutlineColor(Color.BLUE);
                pathOverlay.setWidth(5);

                pathOverlay.setCoords(routecoords);
                pathOverlay.setMap(naverMap);
                range=getMiddle(routecoords);
            }
            else{
                range=getMiddle(loccoords);
            }
            CameraUpdate cameraUpdate =CameraUpdate.fitBounds(new LatLngBounds(range[0],range[1]),50,100,50,50);
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
        for(int i=0; i<locnames.size(); ++i){
            Marker tmp=new Marker();
            tmp.setIcon(MarkerIcons.BLACK);
            tmp.setCaptionAligns(Align.Top);
            tmp.setPosition(loccoords.get(i));
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

    public class LocationAdapter extends BaseAdapter {

        Context context;
        LayoutInflater layoutInflater;


        public LocationAdapter(Context context){
            this.context = context;
            this.layoutInflater = LayoutInflater.from(context);

        }

        @Override
        public int getCount() {
            return locnames.size();
        }

        @Override
        public Object getItem(int i) {
            return locnames.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertview, ViewGroup viewGroup) {
            View view=layoutInflater.inflate(R.layout.item_mate_route_stop, null);
            View emptyview=layoutInflater.inflate(R.layout.item_common_empty,null);
            if(locnames.size()==0){
                return null;
            }
            else{
                ImageView imageView=view.findViewById(R.id.mate_image);
                TextView textView=view.findViewById(R.id.matelist);

                if(i==0){
                    imageView.setImageResource(R.drawable.pin_red);
                }
                else{
                    imageView.setImageResource(R.drawable.pin_blue);
                }
                textView.setText(locnames.get(i));

                return view;
            }
        }
    }

    public static boolean setListViewHeightBasedOnItems(ListView listView) {

        LocationAdapter listAdapter = (MateWriteActivity.LocationAdapter) listView.getAdapter();
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
            params.height = (int) ((totalItemsHeight + totalDividersHeight + totalPadding)*0.7);
            listView.setLayoutParams(params);
            listView.requestLayout();
            //setDynamicHeight(listView);
            return true;

        } else {
            return false;
        }
    }
}
