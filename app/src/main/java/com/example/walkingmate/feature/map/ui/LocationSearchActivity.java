package com.example.walkingmate.feature.map.ui;



import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.walkingmate.BuildConfig;
import com.example.walkingmate.R;
import com.example.walkingmate.feature.misc.ui.SearchActivity;
import com.example.walkingmate.feature.mate.ui.MateWriteActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Align;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.util.MarkerIcons;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

//메이트게시물 등록 activity

public class LocationSearchActivity extends AppCompatActivity implements OnMapReadyCallback{

    private FusedLocationSource locationSource;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private NaverMap naverMap;
    PathOverlay pathOverlay;
    private EditText mEtAddress;
    Button sync,add,reqroute;
    String location, x, y;//x-lon, y-lat(반대임)
    double xval,yval;

    boolean searchmove;

    Marker marker;
    ArrayList<Marker> markers;
    ArrayList<LatLng> locList=new ArrayList<>();//선택지 좌표모음
    LatLng cur=null;
    String curName;
    ArrayList<String> nameList;//선택지 주소명 모음

    ArrayList<LatLng> coordlist;//이동경로 좌표모음


    ListView destList;

    DestAdapter destAdapter;

    boolean setmarkers;

    private static final int ACCESS_LOCATION_PERMISSION_REQUEST_CODE = 100;

    int selecteditem=7;//index 7은 존재할 수 없으므로 미클릭된 상태 값으로 사용

    boolean startreq,clickreq,finishreq,clickfin;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_search);

        startreq=false;
        clickreq=false;
        finishreq=false;
        clickfin=false;

        setmarkers=false;

        searchmove=true;

        nameList=new ArrayList<>();

        coordlist=new ArrayList<>();

        destList=findViewById(R.id.destList);

        markers=new ArrayList<>();


        sync=findViewById(R.id.sync);

        add=findViewById(R.id.addloc);

        reqroute=findViewById(R.id.reqRoute);

        MapFragment mapFragmentFeed=(MapFragment)getSupportFragmentManager().findFragmentById(R.id.testmap);

        locationSource = new FusedLocationSource(this, ACCESS_LOCATION_PERMISSION_REQUEST_CODE);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    yval = location.getLatitude();
                    xval = location.getLongitude();
                    marker=new Marker();
                    marker.setPosition(new LatLng(yval,xval));
                    cur=new LatLng(yval,xval);
                    mapFragmentFeed.getMapAsync(LocationSearchActivity.this);
                }
                else{
                    yval = 37.5666103;
                    xval = 126.9783882;
                    marker=new Marker();
                    marker.setPosition(new LatLng(yval,xval));
                    cur=new LatLng(yval,xval);
                    mapFragmentFeed.getMapAsync(LocationSearchActivity.this);
                }
            }
        });


        mEtAddress=findViewById(R.id.edit_address);
        mEtAddress.setFocusable(false); //입력막음

        //주소입력창 이동, getsearchResult로 주소명 받아옴
        mEtAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(LocationSearchActivity.this, SearchActivity.class);
                getsearchResult.launch(intent);
            }
        });

        sync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                marker.setPosition(new LatLng(yval,xval));
                cur=marker.getPosition();
                searchmove=true;
                mapFragmentFeed.getMapAsync(LocationSearchActivity.this);
                searchmove=false;
            }
        });


        destAdapter=new DestAdapter(getApplicationContext());
        destList.setAdapter(destAdapter);

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(locList.size()==7){
                    Toast.makeText(getApplicationContext(),"더 이상 추가 할 수 없습니다.",Toast.LENGTH_SHORT).show();
                }
                else if(cur!=null){
                    if(locList.contains(cur)){
                        Toast.makeText(getApplicationContext(),"이미 등록한 위치입니다.",Toast.LENGTH_SHORT).show();
                    }
                    else{
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                requestName(cur);
                                if(locList.size()>1){
                                    RequestTmap();
                                }
                            }
                        }).start();
                    }

                }
            }
        });

        reqroute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("경로탐색","startreq:"+startreq+", coordsize:"+coordlist.size());
                if(startreq){
                    clickreq=true;
                    findViewById(R.id.loading_mate).setVisibility(View.VISIBLE);
                    return;
                }
                if(coordlist==null||coordlist.size()==0){
                    if(nameList.size()<2){
                        Toast.makeText(getApplicationContext(),"2개 이상의 목적지를 선택해 주세요.",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getApplicationContext(),"경로 탐색이 지원되지 않는 목적지가 포함되어 있습니다.",Toast.LENGTH_SHORT).show();
                    }
                }
                setmarkers=true;
                for(int i=0; i<markers.size(); ++i){
                    markers.get(i).setMap(null);
                }
                mapFragmentFeed.getMapAsync(LocationSearchActivity.this);

            }
        });



        findViewById(R.id.finish_setroute).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(locList.size()<2){
                    Toast.makeText(getApplicationContext(),"2개 이상의 목적지를 추가해 주세요.",Toast.LENGTH_SHORT).show();
                    return;
                }
                else{
                    if(startreq){
                        clickfin=true;
                        findViewById(R.id.loading_mate).setVisibility(View.VISIBLE);
                        return;
                    }
                    while(coordlist.size()>5000){
                        ArrayList<LatLng> tmp=new ArrayList();
                        Log.d("메이트루트",coordlist.size()+"");
                        for(int i=0; i<coordlist.size(); ++i){
                            if(i%4!=0){
                                tmp.add(coordlist.get(i));
                            }
                        }
                        coordlist=tmp;
                    }
                    Log.d("메이트루트_최종",coordlist.size()+"");
                    Intent finintent=new Intent(LocationSearchActivity.this,MateWriteActivity.class);
                    finintent.putExtra("routecoords",coordlist);
                    finintent.putExtra("loccoords",locList);
                    finintent.putExtra("locnames",nameList);
                    setResult(RESULT_OK,finintent);
                    finish();
                }

            }
        });

    }

    public class DestAdapter extends BaseAdapter{

        Context context;
        LayoutInflater layoutInflater;


        public DestAdapter(Context context){
            this.context = context;
            this.layoutInflater = LayoutInflater.from(context);

        }

        @Override
        public int getCount() {
            return nameList.size();
        }

        @Override
        public Object getItem(int i) {
            return nameList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertview, ViewGroup viewGroup) {
            View view=layoutInflater.inflate(R.layout.item_map_destination, null);
            View emptyview=layoutInflater.inflate(R.layout.item_common_empty,null);
            if(nameList.size()==0){
                return null;
            }
            else{
                TextView destorder=view.findViewById(R.id.destOrder);
                TextView destItem=view.findViewById(R.id.destlist);
                Button delbtn=view.findViewById(R.id.destdelbtn);

                String destorderString="";
                switch (i){
                    case 0:
                        destorderString+="출발지";
                        destorder.setTextColor(Color.RED);
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
                destorder.setText(destorderString);
                destItem.setText(nameList.get(i));

                delbtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        nameList.remove(i);
                        locList.remove(i);
                        notifyDataSetChanged();
                        if(nameList.size()<2){
                            coordlist.clear();
                        }
                        else{
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    RequestTmap();
                                }
                            }).start();
                        }
                    }
                });

                //순서바꾸기. 추가, 삭제로 리스트 변경시 선택아이템은 초기화
                destorder.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(locList.size()<2){
                            return;
                        }
                        else if(selecteditem==7){
                            selecteditem=i;
                            destorder.setBackgroundColor(Color.GRAY);
                        }
                        else if(selecteditem==i){
                            selecteditem=7;
                            destorder.setBackgroundColor(Color.TRANSPARENT);
                        }
                        else{
                            String tmpname=nameList.get(selecteditem);
                            String destname=nameList.get(i);
                            LatLng tmploc=locList.get(selecteditem);
                            LatLng destloc=locList.get(i);

                            locList.remove(tmploc);
                            nameList.remove(tmpname);

                            int tmpidx=locList.indexOf(destloc);
                            locList.add(tmpidx,tmploc);
                            nameList.add(tmpidx,tmpname);

                            notifyDataSetChanged();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    RequestTmap();
                                }
                            }).start();
                            selecteditem=7;
                            destorder.setBackgroundColor(Color.TRANSPARENT);
                        }
                    }
                });

                return view;
            }
        }
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap=naverMap;

        marker.setMap(naverMap);

        if(setmarkers){
            markers.clear();
            for(int i=0; i<locList.size(); ++i){
                Marker tmp=new Marker();
                tmp.setPosition(locList.get(i));
                tmp.setIcon(MarkerIcons.BLACK);
                tmp.setCaptionAligns(Align.Top);
                markers.add(tmp);
            }
            for(int i=0; i<markers.size(); ++i){
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
                    markers.get(i).setCaptionColor(Color.RED);
                    markers.get(i).setIconTintColor(Color.RED);
                }
                else{
                    markers.get(i).setCaptionColor(Color.BLUE);
                    markers.get(i).setIconTintColor(Color.BLUE);
                }
                markers.get(i).setCaptionText(destorderString);
                markers.get(i).setMap(naverMap);
            }
            setmarkers=false;
        }


        //이동 버튼이 아닌 탐색버튼으로 갱신시 새 경로 그리기전에 초기화
        if(pathOverlay!=null&&(!searchmove)){
            pathOverlay.setMap(null);
        }

        //2개이상 위치선택한 상태에서 이동버튼이 아닌 탐색으로 지도 갱신시 새로 경로 탐색 진행
        if(locList.size()>1&&coordlist!=null&&(!searchmove)&&coordlist.size()>1){
            pathOverlay=new PathOverlay();
            pathOverlay.setColor(Color.BLUE);
            pathOverlay.setOutlineColor(Color.BLUE);
            pathOverlay.setCoords(coordlist);
            pathOverlay.setMap(naverMap);
            LatLng[] range=getMiddle(coordlist);
            CameraUpdate cameraUpdate =CameraUpdate.fitBounds(new LatLngBounds(range[0],range[1]),200);
            naverMap.moveCamera(cameraUpdate);
        }

        if(searchmove){
            CameraPosition cameraPosition=new CameraPosition(new LatLng(yval,xval),17);
            naverMap.setCameraPosition(cameraPosition);
            searchmove=false;
        }

        naverMap.setLocationSource(locationSource);
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setLocationButtonEnabled(true);
        naverMap.setLocationTrackingMode(LocationTrackingMode.None);


        naverMap.setOnMapClickListener(new NaverMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
                marker.setPosition(latLng);
                cur=latLng;
            }
        });



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



    private final ActivityResultLauncher<Intent> getsearchResult= registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() ==RESULT_OK){
                    if(result.getData()!=null){
                        location=result.getData().getStringExtra("data");
                        mEtAddress.setText(location);
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
                //장소 좌표와 이름 추가
                nameList.add(curName);
                locList.add(cur);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                destAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }).start();

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

    public void RequestTmap(){
        startreq=true;
        double startX, startY, endX, endY;
        String startName, endName;
        startX=locList.get(0).longitude;
        startY=locList.get(0).latitude;

        int loclistsize=locList.size();

        endX=locList.get(loclistsize-1).longitude;
        endY=locList.get(loclistsize-1).latitude;
        startName="출발";
        endName="도착";

        int searchoption=0; //0은 추천, 4는 대로 우선, 10은 최단

        ArrayList<LatLng> passList=new ArrayList<>();

        if(loclistsize>2){
            for(int i=1; i<loclistsize-1; ++i){
                passList.add(locList.get(i));
            }
        }
        String passListstr="";
        if(passList.size()!=0){
            passListstr=",\"passList\":\"" + passlistString(passList) + "\"";
        }
        try{
            Log.d("루트 검색","시작");

            OkHttpClient client = new OkHttpClient();

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, "{\"startX\":" +
                    startX +
                    ",\"startY\":" +
                    startY +
                    "\"endX\":" +
                    endX +
                    ",\"endY\":" +
                    endY +
                    passListstr +
                    ",\"reqCoordType\":\"WGS84GEO\",\"startName\":\"" +
                    URLEncoder.encode(startName,"UTF-8")+
                    "\",\"endName\":\"" +
                    URLEncoder.encode(endName,"UTF-8") +
                    "\",\"searchOption\":\"" +
                    searchoption +
                    "\",\"resCoordType\":\"WGS84GEO\",\"sort\":\"index\"}");
            Request request = new Request.Builder()
                    .url("https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1&callback=function")
                    .post(body)
                    .addHeader("accept", "application/json")
                    .addHeader("content-type", "application/json")
                    .addHeader("appKey", "l7xxab136fb0e58b4e84a4a923596d9b17e6")
                    .build();

            Response response = client.newCall(request).execute();

            String mresponse=response.body().string();
            Log.d("JSON원본",mresponse);
            Log.d("JSON입력좌표",locList.toString());

            coordlist=getPoints(mresponse);
            Log.d("JSON-최종 좌표모음",coordlist.toString());
            Log.d("JSON-최종 좌표 수",coordlist.size()+"");

            startreq=false;
            findViewById(R.id.loading_mate).setVisibility(View.INVISIBLE);
            if(clickfin){
                clickfin=false;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                findViewById(R.id.finish_setroute).performClick();
                            }
                        });
                    }
                }).start();
            }
            if(clickreq){
                clickreq=false;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                reqroute.performClick();
                            }
                        });
                    }
                }).start();
            }



        }catch (Exception e){
            e.printStackTrace();
        }
    }



    public ArrayList<LatLng> getPoints(String json){
        ArrayList<LatLng> points=new ArrayList<>();

        try{
            JSONObject jsonObject=new JSONObject(json);

            JSONArray jsonArray=jsonObject.getJSONArray("features");

            for(int i=0; i< jsonArray.length(); ++i){
                JSONObject object=jsonArray.getJSONObject(i);
                JSONObject innerobject=object.getJSONObject("geometry");
                String type=innerobject.getString("type");
                JSONArray coords=innerobject.getJSONArray("coordinates");
                Log.d("JSON테스트-객체",type);
                Log.d("JSON테스트-포인트",coords.toString());


                //1이 latitude, 0이 longitude
                if(type.equals("Point")){
                    double lat=coords.getDouble(1);
                    double lon=coords.getDouble(0);
                    points.add(new LatLng(lat,lon));
                }
                else {
                    for(int j=0; j<coords.length(); ++j){
                        JSONArray tmpob=coords.getJSONArray(j);

                        double lat=tmpob.getDouble(1);
                        double lon=tmpob.getDouble(0);
                        points.add(new LatLng(lat,lon));
                    }
                }
            }

        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        return points;
    }

    public String passlistString(ArrayList<LatLng> list){
        String result="";
        for(int i=0; i<list.size(); ++i){
            result+=list.get(i).longitude+","+list.get(i).latitude+"_";
        }
        result=result.substring(0,result.length()-1);
        return result;
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
