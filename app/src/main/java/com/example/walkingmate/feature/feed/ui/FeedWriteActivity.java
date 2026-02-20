package com.example.walkingmate.feature.feed.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
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
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.MarkerIcons;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


public class FeedWriteActivity extends AppCompatActivity implements OnMapReadyCallback {

    FirebaseFirestore fb=FirebaseFirestore.getInstance();
    CollectionReference feeddata=fb.collection("feed");
    CollectionReference feedlist=fb.collection("feedlist");
    FirebaseStorage storage=FirebaseStorage.getInstance();
    StorageReference storageReference=storage.getReference();

    private NaverMap naverMap;
    LinearLayout FeedMapLayout;
    PathOverlay pathOverlay;

    FeedData feedData;

    TextView distxt,steptxt,timetxt, datetxt, curpage, loading;
    ImageView imageback;
    ListView listView;
    EditText record;
    Switch openset;

    FrameLayout imagebacklayout;

    ArrayList<Marker> markers;

    int selected=-1;

    MapFragment mapFragmentFeed;
    ArrayList<ImageFragment> fragments=new ArrayList<>();


    ViewPager2 viewPager2;
    FragmentStateAdapter fragmentStateAdapter;

    int curposition=0;

    Bitmap bmp;

    String weather="sunny";
    String emotion="smile";
    String content="";
    ImageButton weatherbtn,emotionbtn;

    ArrayList<String> imgurls=new ArrayList<>();

    String fileName;

    boolean isOpen=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_write);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        Intent getFeed=getIntent();
        fileName=getFeed.getStringExtra("filename");

        FeedData fd=new FeedData();
        feedData=fd.loadfeed(fileName,this);

        loading=findViewById(R.id.loading_feed);

        markers=feedData.markerList;

        mapFragmentFeed=(MapFragment)getSupportFragmentManager().findFragmentById(R.id.feedmap);
        mapFragmentFeed.getMapAsync(this);

        record=findViewById(R.id.record_feedwrite);

        record.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        record.clearFocus();
                        InputMethodManager imm =
                                (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(record.getWindowToken(), 0);
                        return true;
                    }
                }
                return false;
            }
        });


        imageback=findViewById(R.id.imagebackground);
        imagebacklayout=findViewById(R.id.image_feedwrite);

        distxt=findViewById(R.id.distext_feedview);
        steptxt=findViewById(R.id.feedviewstep);
        timetxt=findViewById(R.id.feedview_time);
        datetxt=findViewById(R.id.feed_date);

        String diststr=String.format("%.3f km",feedData.displacement);
        String stepstr=String.format("%d 걸음",feedData.step);

        String[] start=feedData.timecheck[0].split("_");
        String[] end=feedData.timecheck[1].split("_");
        String timestr=String.format("%s:%s ~ %s:%s",start[3].replace("시",""),
                start[4].replace("분",""),end[3].replace("시",""),end[4].replace("분",""));

        String datestr=feedData.timecheck[0].replace("_", " ");

        distxt.setText(diststr);
        steptxt.setText(stepstr);
        timetxt.setText(timestr);
        datetxt.setText(datestr);

        openset=findViewById(R.id.openset);
        openset.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isOpen=b;
            }
        });





        findViewById(R.id.back_feedwrite).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        curpage=findViewById(R.id.curpage);


        listView=findViewById(R.id.loclist_feed);
        LocationAdapter LocationAdapter=new LocationAdapter(this);
        listView.setAdapter(LocationAdapter);
        setListViewHeightBasedOnItems(listView);


        viewPager2=findViewById(R.id.viewpager);
        setviewpager();

        findViewById(R.id.finish_feedwrtie).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                content=record.getText().toString();
                if(content.equals("")){
                    Toast.makeText(getApplicationContext(),"내용을 입력해주세요.",Toast.LENGTH_SHORT).show();
                    return;
                }
                else{
                    loading.setVisibility(View.VISIBLE);
                    sendData();
                }
            }
        });

        findViewById(R.id.addimage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Intent galleryintent=new Intent();
                                galleryintent.setType("image/*");
                                galleryintent.setAction(Intent.ACTION_GET_CONTENT);
                                startActivityForResult(galleryintent,1);

                            }
                        });
                    }
                }).start();
            }
        });
        findViewById(R.id.delimage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(fragments.size()<1){
                    return;
                }
                else{
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    fragments.remove(curposition);
                                    setviewpager();
                                    if(fragments.size()==0){
                                        imageback.setVisibility(View.VISIBLE);
                                        imagebacklayout.setBackgroundResource(R.drawable.plusbtn);
                                        curpage.setText("0 / 0");
                                    }
                                    else{
                                        curpage.setText(String.format("%d / %d",curposition+1,fragments.size()));
                                    }
                                    Log.d("피드기록 삭제", ""+fragments.size());
                                }
                            });
                        }
                    }).start();
                }

            }
        });

        emotionbtn=findViewById(R.id.emotion_feedview);
        weatherbtn=findViewById(R.id.weather_feedview);

        emotionbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent goemo=new Intent(FeedWriteActivity.this, SelectFeedActivity.class);
                goemo.putExtra("action",2);
                startActivityForResult(goemo,2);
            }
        });
        weatherbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent goweath=new Intent(FeedWriteActivity.this, SelectFeedActivity.class);
                goweath.putExtra("action",3);
                startActivityForResult(goweath,3);
            }
        });

    }

    public void sendData(){
        HashMap<String,Object> data=new HashMap<>();


        UserData userData=UserData.loadData(FeedWriteActivity.this);

        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String writetime=sdf.format(date);

        Log.d("산책 유저아이디",userData.toString());

        String documentID= writetime+"@"+userData.userid;//나중에 뒤에 유저아이디 추가
        Log.d("테스트",documentID);
        //아래 정보는 보는 사람 입장에서 필터링을 위함.
        data.put("userid", userData.userid);
        data.put("title", feedData.timecheck[0]);
        data.put("writetime",writetime);

        int year= Integer.parseInt(feedData.timecheck[0].split("_")[0].replace("년",""));
        int month=Integer.parseInt(feedData.timecheck[0].split("_")[1].replace("월",""));
        int day=Integer.parseInt(feedData.timecheck[0].split("_")[2].replace("일",""));

        data.put("year",year);
        data.put("month",month);
        data.put("day",day);
        data.put("isOpen",isOpen);

        feedlist.document(documentID).set(data);

        data.put("feeddata",FeedData.encodeFeed(feedData));
        data.put("content",content);
        data.put("weather",weather);
        data.put("emotion",emotion);
        for(int i=0; i<fragments.size(); ++i){
            Bitmap tmp=fragments.get(i).bmp;
            uploadImage(tmp,documentID,i);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try{
                        Thread.sleep(100);
                    }catch (Exception e){}
                    if(imgurls.size()==fragments.size()){
                        data.put("images",imgurls);
                        feeddata.document(documentID).set(data).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d("피드_완료",imgurls.toString());
                                Toast.makeText(getApplicationContext(),"작성 완료되었습니다.",Toast.LENGTH_SHORT).show();
                                FeedData.deletefeed(fileName, FeedWriteActivity.this);
                                finish();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                loading.setVisibility(View.INVISIBLE);
                                Toast.makeText(getApplicationContext(),"작성 실패하였습니다.",Toast.LENGTH_SHORT).show();
                                Log.d("피드_실패",e.toString());
                            }
                        });
                        break;
                    }
                }

            }
        }).start();
    }

    public void uploadImage(Bitmap bitmap, String id, int idx){

        //id는 게시물 id
        StorageReference uploadRef=storageReference.child(id+"_"+idx+".jpg");

        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos);
        byte[] datas=baos.toByteArray();

        UploadTask uploadTask=uploadRef.putBytes(datas);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(),"이미지 업로드 실패",Toast.LENGTH_SHORT).show();
                Log.d("로그인_이미지실패",e.toString());
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        // Continue with the task to get the download URL
                        return uploadRef.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            imgurls.add(downloadUri.toString());
                        } else {
                            Toast.makeText(getApplicationContext(),"다시 시도해 주세요.",Toast.LENGTH_SHORT).show();
                        }
                    }
                });


            }
        });
    }



    public void setviewpager(){

        viewPager2.setAdapter(null);
        fragmentStateAdapter=new MyAdapter(this);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK){
            if(requestCode==2){
                int tmpemonum=data.getIntExtra("emotion",4);
                switch(tmpemonum){
                    case 0:
                        emotion="angry"; emotionbtn.setImageResource(R.drawable.feeling_angry_xml); break;
                    case 1:
                        emotion="crying"; emotionbtn.setImageResource(R.drawable.feeling_crying_xml);break;
                    case 2:
                        emotion="heart"; emotionbtn.setImageResource(R.drawable.feeling_heart_xml);break;
                    case 3:
                        emotion="neutral"; emotionbtn.setImageResource(R.drawable.feeling_neutral_xml);break;
                    case 4:
                        emotion="smile"; emotionbtn.setImageResource(R.drawable.feeling_smiling_xml);break;
                    case 5:
                        emotion="tired"; emotionbtn.setImageResource(R.drawable.feeling_tired_xml);break;
                    default:
                        emotion="smile"; emotionbtn.setImageResource(R.drawable.feeling_smiling_xml);break;
                }

            }
        else if(requestCode==3){
                int tmpweathnum=data.getIntExtra("weather",6);
                switch (tmpweathnum){
                    case 0:
                        weather="cloudy"; weatherbtn.setImageResource(R.drawable.weather_cloudy_xml);break;
                    case 1:
                        weather="fog";weatherbtn.setImageResource(R.drawable.weather_fog_xml);break;
                    case 2:
                        weather="moon";weatherbtn.setImageResource(R.drawable.weather_moon_xml);break;
                    case 3:
                        weather="rainbow";weatherbtn.setImageResource(R.drawable.weather_rainbow_xml);break;
                    case 4:
                        weather="rainy";weatherbtn.setImageResource(R.drawable.weather_rainy_xml);break;
                    case 5:
                        weather="snow";weatherbtn.setImageResource(R.drawable.weather_snow_xml);break;
                    case 6:
                        weather="sunny";weatherbtn.setImageResource(R.drawable.weather_sunny_xml);break;
                    case 7:
                        weather="windy";weatherbtn.setImageResource(R.drawable.weather_windy_xml);break;
                    default:
                        weather="sunny"; weatherbtn.setImageResource(R.drawable.weather_sunny_xml);break;
                }
            }
            else{
                try{
                    InputStream in=getContentResolver().openInputStream(data.getData());
                    Bitmap tmpbmp= BitmapFactory.decodeStream(in);
                    tmpbmp=FeedData.getResizedFeedImage(tmpbmp);

                    Uri uri=data.getData();
                    Log.d("uri체크",uri.toString());
                    Uri PhotoUri;
                    try{
                        PhotoUri=Uri.parse(getRealPathFromURI(uri));
                    }catch (IllegalArgumentException e){
                        PhotoUri=Uri.parse(getRealPathFromURIgal(uri));
                    }

                    ExifInterface exif=new ExifInterface(PhotoUri.getPath());
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                    bmp = rotateBitmap(tmpbmp, orientation);

                    fragments.add(new ImageFragment(fragments.size()+1,bmp));
                    curposition=fragments.size()-1;
                    setviewpager();
                    imageback.setVisibility(View.INVISIBLE);
                    imagebacklayout.setBackgroundResource(R.color.transparent);
                    curpage.setText(String.format("%d / %d",curposition+1,fragments.size()));
                }catch (Exception e){e.printStackTrace();}
            }
        }
    }

    //갤러리에서 동작
    private String getRealPathFromURIgal(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    //최근파일 동작
    private String getRealPathFromURI(Uri contentUri) {

        if (contentUri.getPath().startsWith("/storage")) {
            return contentUri.getPath();
        }

        String id = DocumentsContract.getDocumentId(contentUri).split(":")[1];
        String[] columns = { MediaStore.Files.FileColumns.DATA };
        String selection = MediaStore.Files.FileColumns._ID + " = " + id;
        Cursor cursor = getContentResolver().query(MediaStore.Files.getContentUri("external"), columns, selection, null, null);
        try {
            int columnIndex = cursor.getColumnIndex(columns[0]);
            if (cursor.moveToFirst()) {
                return cursor.getString(columnIndex);
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
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

        FeedMapLayout=findViewById(R.id.FeedMapLayout);
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

    public class LocationAdapter extends BaseAdapter {

        Context context;
        LayoutInflater layoutInflater;


        public LocationAdapter(Context context){
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
                        mapFragmentFeed.getMapAsync(FeedWriteActivity.this);
                    }
                });

                return view;
            }


        }
    }


    public static boolean setListViewHeightBasedOnItems(ListView listView) {

        FeedWriteActivity.LocationAdapter listAdapter = (FeedWriteActivity.LocationAdapter) listView.getAdapter();
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
