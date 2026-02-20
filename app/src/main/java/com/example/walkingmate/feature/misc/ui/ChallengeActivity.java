package com.example.walkingmate.feature.misc.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.walkingmate.R;
import com.example.walkingmate.feature.user.data.UserData;
import com.example.walkingmate.feature.user.ui.HelpInfoActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ChallengeActivity extends AppCompatActivity {

    FirebaseFirestore fb=FirebaseFirestore.getInstance();
    CollectionReference challenge=fb.collection("challenge");
    CollectionReference user=fb.collection("users");
    CollectionReference feed=fb.collection("feedlist");

    Long step,seq,meet;
    double rel;

    UserData userData;

    ImageButton back,s1,s2,s3,s4,s5,r1,r2,r3,r4,r5,q1,q2,q3,q4,q5,m1,m2,m3,m4,m5;
    TextView stxt,rtxt,qtxt,mtxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge);

        back=findViewById(R.id.back_challenge);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        findViewById(R.id.question_challenge).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(ChallengeActivity.this, HelpInfoActivity.class);
                intent.putExtra("curitem",1);
                startActivity(intent);
            }
        });

        stxt=findViewById(R.id.steptxt);
        rtxt=findViewById(R.id.reltxt);
        qtxt=findViewById(R.id.seqtxt);
        mtxt=findViewById(R.id.meettxt);

        s1=findViewById(R.id.step1);
        s2=findViewById(R.id.step2);
        s3=findViewById(R.id.step3);
        s4=findViewById(R.id.step4);
        s5=findViewById(R.id.step5);

        r1=findViewById(R.id.rel1);
        r2=findViewById(R.id.rel2);
        r3=findViewById(R.id.rel3);
        r4=findViewById(R.id.rel4);
        r5=findViewById(R.id.rel5);

        q1=findViewById(R.id.seq1);
        q2=findViewById(R.id.seq2);
        q3=findViewById(R.id.seq3);
        q4=findViewById(R.id.seq4);
        q5=findViewById(R.id.seq5);

        m1=findViewById(R.id.meet1);
        m2=findViewById(R.id.meet2);
        m3=findViewById(R.id.meet3);
        m4=findViewById(R.id.meet4);
        m5=findViewById(R.id.meet5);

        getdata();

    }

    //우선 저장된 값 가져온뒤 실행. 계산된 값이 더 크면 업데이트
    public void evalseq() throws ParseException {
        SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
        Calendar c=Calendar.getInstance();
        Date today=new Date();
        c.setTime(today);
        today.getYear();
        final int[] thisweek = {c.get(c.WEEK_OF_YEAR)};
        c.setTime(sdf.parse((c.getWeekYear()-1)+"1225"));

        int befyearlastweek=c.get(c.WEEK_OF_YEAR);
        Log.d("도전과제 연도계산",befyearlastweek+"");
        int[] checkweek=new int[befyearlastweek+1];
        final int[] seqtotal = {0};


        feed.whereEqualTo("userid",userData.userid).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                QuerySnapshot documentSnapshot=task.getResult();
                for(DocumentSnapshot document:documentSnapshot){
                    try {
                        Date tmpd=sdf.parse(((String) document.get("writetime")).substring(0,8));
                        c.setTime(tmpd);
                        int tmpweek=c.get(Calendar.WEEK_OF_YEAR);
                        checkweek[tmpweek]+=1;
                        Log.d("각 날짜별 주차값",(String) ((String) document.get("writetime")).substring(0,8)+", 주차:"+tmpweek);
                    } catch (ParseException e) {
                        Log.d("도전과제 날짜에러",e.toString());
                    }
                }
                //이번주는 따로 체크
                if(checkweek[thisweek[0]]>=3){
                    ++seqtotal[0];
                }
                Log.d("이번주 체크",thisweek[0]+":"+seqtotal[0]+"");
                //전주부터 시작
                thisweek[0] -=1;
                while(checkweek[thisweek[0]]>=3){
                    ++seqtotal[0];
                    Log.d("차주 체크",thisweek[0]+"주차:"+seqtotal[0]);
                    --thisweek[0];
                    if(thisweek[0]<0){
                        thisweek[0]=befyearlastweek;
                    }
                }

                //이번주만 만족한 경우

                //최종 결과가 도전과제에 저장된 값보다 크면 갱신
                if(seqtotal[0]>seq){
                    seq= Long.valueOf(seqtotal[0]);
                    challenge.document(userData.userid).update("feedseq",seq);
                }

                Log.d("도전과제 최종","걸음:"+step+", 꾸준:"+seq+", 사교:"+meet);
            }
        });
    }

    public void getdata(){
        userData=UserData.loadData(this);
        user.document(userData.userid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                rel= task.getResult().getDouble("reliability");
                challenge.document(userData.userid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot document=task.getResult();
                        step=document.getLong("step");
                        seq= document.getLong("feedseq");
                        meet=document.getLong("meet");
                        try {
                            evalseq();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        setUi();
                                    }
                                });
                            }
                        }).start();
                    }
                });
            }
        });
    }

    public void setUi(){
        int stepn,reln,seqn,meetn;
        if(step<100000){
            stepn=0;
        }
        else if(step<300000){
            stepn=1;
        }
        else if(step<500000){
            stepn=2;
        }
        else if(step<700000){
            stepn=3;
        }
        else if(step<1000000){
            stepn=4;
        }
        else{
            stepn=5;
        }

        if(rel<55){
            reln=0;
        }
        else if(rel<65){
            reln=1;
        }
        else if(rel<75){
            reln=2;
        }
        else if(rel<85){
            reln=3;
        }
        else if(rel<95){
            reln=4;
        }
        else{
            reln=5;
        }

        if(seq<2){
            seqn=0;
        }
        else if(seq<4){
            seqn=1;
        }
        else if(seq<7){
            seqn=2;
        }
        else if(seq<10){
            seqn=3;
        }
        else if(seq<15){
            seqn=4;
        }
        else{
            seqn=5;
        }

        if(meet<3){
            meetn=0;
        }
        else if(meet<5){
            meetn=1;
        }
        else if(meet<10){
            meetn=2;
        }
        else if(meet<20){
            meetn=3;
        }
        else if(meet<40){
            meetn=4;
        }
        else{
            meetn=5;
        }

        if(stepn>0){
            s1.setBackgroundResource(R.drawable.achieve_bronze_xml);
        }
        if(stepn>1){
            s2.setBackgroundResource(R.drawable.achieve_silver_xml);
        }
        if(stepn>2){
            s3.setBackgroundResource(R.drawable.achieve_gold_xml);
        }
        if(stepn>3){
            s4.setBackgroundResource(R.drawable.achieve_diamond_xml);
        }
        if(stepn>4){
            s5.setBackgroundResource(R.drawable.achieve_champion_xml);
        }

        if(seqn>0){
            q1.setBackgroundResource(R.drawable.achieve_bronze_xml);
        }
        if(seqn>1){
            q2.setBackgroundResource(R.drawable.achieve_silver_xml);
        }
        if(seqn>2){
            q3.setBackgroundResource(R.drawable.achieve_gold_xml);
        }
        if(seqn>3){
            q4.setBackgroundResource(R.drawable.achieve_diamond_xml);
        }
        if(seqn>4){
            q5.setBackgroundResource(R.drawable.achieve_champion_xml);
        }

        if(reln>0){
            r1.setBackgroundResource(R.drawable.achieve_bronze_xml);
        }
        if(reln>1){
            r2.setBackgroundResource(R.drawable.achieve_silver_xml);
        }
        if(reln>2){
            r3.setBackgroundResource(R.drawable.achieve_gold_xml);
        }
        if(reln>3){
            r4.setBackgroundResource(R.drawable.achieve_diamond_xml);
        }
        if(reln>4){
            r5.setBackgroundResource(R.drawable.achieve_champion_xml);
        }

        if(meetn>0){
            m1.setBackgroundResource(R.drawable.achieve_bronze_xml);
        }
        if(meetn>1){
            m2.setBackgroundResource(R.drawable.achieve_silver_xml);
        }
        if(meetn>2){
            m3.setBackgroundResource(R.drawable.achieve_gold_xml);
        }
        if(meetn>3){
            m4.setBackgroundResource(R.drawable.achieve_diamond_xml);
        }
        if(meetn>4){
            m5.setBackgroundResource(R.drawable.achieve_champion_xml);
        }

        stxt.setText(String.format("건강한 워커 (%d/5) - %d걸음",stepn,step));
        qtxt.setText(String.format("꾸준한 워커 (%d/5) - %d주",seqn,seq));
        rtxt.setText(String.format("믿음직한 워커 (%d/5) - %.1f점" ,reln,rel));
        mtxt.setText(String.format("사교적인 워커 (%d/5) - %d번",meetn,meet));

    }
}
