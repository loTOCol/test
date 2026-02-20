package com.example.walkingmate.feature.feed.ui;

import static com.example.walkingmate.R.id.feedListView;
import static com.example.walkingmate.R.id.month;
import static com.example.walkingmate.R.id.year;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.LinearGradient;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import com.example.walkingmate.R;
import com.example.walkingmate.feature.user.data.UserData;
import com.example.walkingmate.feature.feed.data.FeedData;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.auth.User;

import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;

public class FeedActivity extends AppCompatActivity {
    FirebaseFirestore fb=FirebaseFirestore.getInstance();
    CollectionReference walklist=fb.collection("feedlist");

    ListView feedListView;

    boolean isFeedexist;

    String date;

    int iswrite;

    UserData userData;

    BtnAdapter btnAdapter;
    FeedAdapter feedAdapter;

    int y,m,d; String ys,ms,ds;

    String others=null;

    boolean backfromfeedwrite=false;
    boolean backfromfeedview=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        userData=UserData.loadData(this);

        feedListView=findViewById(R.id.feedListView);

        isFeedexist=false;

        Intent getIntent=getIntent();

        //미작성인지 작성인지 체크, 1이면 미작성, 2이면 작성
        iswrite=getIntent.getIntExtra("iswrite",1);
        Log.d("피드_종류",iswrite+"");

        others=getIntent.getStringExtra("others");



        y=getIntent.getIntExtra("year",9999);
        m=getIntent.getIntExtra("month",0);
        d=getIntent.getIntExtra("day",0);
        ys=y+"";
        if(m<10){
            ms="0"+m;
        }
        else{
            ms=m+"";
        }
        if(d<10){
            ds="0"+d;
        }
        else{
            ds=""+d;
        }

        date=ys+"년_"+ms+"월_"+ds+"일_";

        Log.d("피드_종류",iswrite+"/"+date);


        Log.d("checkDate",date);


        ArrayList<String> feedList=new ArrayList<>();
        if(iswrite==1){
            FeedData fd=new FeedData();

            if(fd.scanFeedList(this)!=null){
                feedList=fd.scanFeedList(this);
                isFeedexist=true;
            }
            ArrayList<String> tmp=new ArrayList<>();
            for(int i=0; i<feedList.size(); ++i){
                if(feedList.get(i).contains(date)){
                    tmp.add(feedList.get(i));
                }
            }
            //MapActivity체크용-default로 intent받으면 모든 목록 체크 가능하도록 함
            if(!date.contains("9999년")){
                feedList=tmp;
            }

            if(feedList.size()==0){
                feedList.add("기록된 피드가 존재하지 않습니다.");
            }

            btnAdapter=new BtnAdapter(getApplicationContext(),feedList, isFeedexist);
            feedListView.setAdapter(btnAdapter);

        }
        else{
            receiveData(y,m,d);
        }



    }

    @Override
    protected void onResume() {
        super.onResume();
        if(iswrite==1&&backfromfeedwrite){
            Log.d("피드목록 재시작","1");
            backfromfeedwrite=false;
            Intent gofeed=new Intent(this, FeedActivity.class);
            gofeed.putExtra("year",y);
            gofeed.putExtra("month",m);
            gofeed.putExtra("day",d);
            gofeed.putExtra("iswrite",iswrite);
            startActivity(gofeed);
            finish();
        }
        if(iswrite!=1&&backfromfeedview){
            Log.d("피드목록 재시작","2");
            backfromfeedview=false;
            Intent gofeed=new Intent(this, FeedActivity.class);
            gofeed.putExtra("year",y);
            gofeed.putExtra("month",m);
            gofeed.putExtra("day",d);
            gofeed.putExtra("iswrite",iswrite);
            gofeed.putExtra("others",others);
            startActivity(gofeed);
            finish();
        }

    }

    public void receiveData(int year, int month, int day){
        final boolean[] isFeedexist = {true};

        ArrayList<String> result=new ArrayList<>();
        ArrayList<String> resulttime=new ArrayList<>();
        ArrayList<String> docuid=new ArrayList<>();

        String userid=userData.userid;
        if(others!=null){
            userid=others;
        }

        if(year==9999){
            Log.d("피드리스트","모든날 진입");
            walklist.whereEqualTo("userid",userid).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            for(int i=0; i<task.getResult().size(); ++i){
                                //isOpen설정 없는 구 데이터이거나 오픈 설정이 되어 있거나 내 데이터인경우 추가
                                if(task.getResult().getDocuments().get(i).get("isOpen")==null||(Boolean) task.getResult().getDocuments().get(i).get("isOpen")||
                                task.getResult().getDocuments().get(i).get("userid").equals(userData.userid)){
                                    result.add((String) task.getResult().getDocuments().get(i).get("title"));
                                    resulttime.add((String) task.getResult().getDocuments().get(i).get("writetime"));
                                    docuid.add((String) task.getResult().getDocuments().get(i).getId());
                                }

                            }
                            if(result.size()==0){
                                isFeedexist[0] =false;
                                result.add("기록된 피드가 존재하지 않습니다.");
                            }
                            feedAdapter=new FeedAdapter(getApplicationContext(),result, isFeedexist[0],resulttime,docuid);
                            feedListView.setAdapter(feedAdapter);
                        }
                    });
        }
        else{
            Log.d("피드리스트","특정날 진입");
            walklist.whereEqualTo("userid",userData.userid).whereEqualTo("day",day).whereEqualTo("month",month).
                    whereEqualTo("year",year).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            for(int i=0; i<task.getResult().size(); ++i){
                                result.add((String) task.getResult().getDocuments().get(i).get("title"));
                                resulttime.add((String) task.getResult().getDocuments().get(i).get("writetime"));
                                docuid.add((String) task.getResult().getDocuments().get(i).getId());
                            }
                            if(result.size()==0){
                                isFeedexist[0] =false;
                                result.add("기록된 피드가 존재하지 않습니다.");
                            }
                            feedAdapter=new FeedAdapter(getApplicationContext(),result, isFeedexist[0],resulttime,docuid);
                            feedListView.setAdapter(feedAdapter);
                        }
                    });
        }
    }


    public class BtnAdapter extends BaseAdapter{
        Context context;
        LayoutInflater layoutInflater;
        ArrayList<String> data;
        FeedData feedData;
        boolean isFeedexist;

        public BtnAdapter(Context context, ArrayList<String> data, boolean isFeedexist){
            this.context = context;
            this.layoutInflater = LayoutInflater.from(context);
            this.data = data;
            feedData=new FeedData();
            this.isFeedexist=isFeedexist;
        }
        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = layoutInflater.inflate(R.layout.item_common_list, null);
            View emptyview=layoutInflater.inflate(R.layout.item_common_empty,null);
            if(data.get(0).equals("기록된 피드가 존재하지 않습니다.")){
                return emptyview;
            }

            TextView textView = view.findViewById(R.id.datelist);
            textView.setText(convertDateText(data.get(position)));



            View bodyView = view.findViewById(R.id.body);

            bodyView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!isFeedexist){
                        return;
                    }
                    backfromfeedwrite=true;
                    String filename=data.get(position);
                    Intent goFeedwrite=new Intent(FeedActivity.this, FeedWriteActivity.class);
                    goFeedwrite.putExtra("filename",filename);
                    startActivity(goFeedwrite);
                }
            });


            Button button=view.findViewById(R.id.listdelbtn);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!isFeedexist){
                        return;
                    }
                    String filename=data.get(position);
                    feedData.deletefeed(filename,FeedActivity.this);
                    data.remove(filename);
                    if(data.size()==0){
                        isFeedexist=false;
                        data.add("기록된 피드가 존재하지 않습니다.");
                    }
                    notifyDataSetChanged();

                }
            });

            return view;

        }
    }

    public class FeedAdapter extends BaseAdapter{
        Context context;
        LayoutInflater layoutInflater;
        ArrayList<String> data;
        boolean isFeedexist;
        ArrayList<String> times;
        ArrayList<String> docuid;

        public FeedAdapter(Context context, ArrayList<String> data, boolean isFeedexist, ArrayList<String> times,ArrayList<String> docuid){
            this.context = context;
            this.layoutInflater = LayoutInflater.from(context);
            this.data = data;
            this.isFeedexist=isFeedexist;
            this.times=times;
            this.docuid=docuid;
        }
        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = layoutInflater.inflate(R.layout.item_feed_post,null);
            View emptyview=layoutInflater.inflate(R.layout.item_common_empty,null);
            if(data.get(0).equals("기록된 피드가 존재하지 않습니다.")){
                return emptyview;
            }

            TextView textView = view.findViewById(R.id.datelistfeed);
            textView.setText(convertDateText(data.get(position)));

            TextView writetime=view.findViewById(R.id.writedate);
            String wy=times.get(position).substring(0,4);
            String wm=times.get(position).substring(4,6);
            String wd=times.get(position).substring(6,8);
            writetime.setText(wy+"."+wm+"."+wd);



            View bodyView = view.findViewById(R.id.bodyfeed);

            bodyView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!isFeedexist){
                        return;
                    }
                    backfromfeedview=true;
                    String filename=docuid.get(position);
                    walklist.document(filename).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if(task.isSuccessful()&&task.getResult().exists()){
                                Intent goFeedwrite=new Intent(FeedActivity.this, ViewFeedActivity.class);
                                goFeedwrite.putExtra("filename",filename);
                                startActivity(goFeedwrite);
                            }
                            else{
                                Toast.makeText(getApplicationContext(),"삭제된 게시물입니다.",Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                }
            });


            return view;

        }
    }

    public String convertDateText(String day){
        String result=day;
        result=result.replaceAll("_"," ");
        result=result.replace(".txt","");
        return result;
    }


}
