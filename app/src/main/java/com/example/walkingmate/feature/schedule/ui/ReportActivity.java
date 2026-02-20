package com.example.walkingmate.feature.schedule.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.walkingmate.R;
import com.example.walkingmate.feature.user.data.UserData;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ReportActivity extends Activity {
    FirebaseFirestore fb=FirebaseFirestore.getInstance();
    CollectionReference reportcr=fb.collection("report");

    Button b1,b2,b3,b4,send;
    EditText reason;

    int reportcase=0;

    String userid;
    UserData userData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_report);

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Intent getintent=getIntent();
        userid=getintent.getStringExtra("userid");
        userData=UserData.loadData(this);

        b1=findViewById(R.id.report_reason1);
        b2=findViewById(R.id.report_reason2);
        b3=findViewById(R.id.report_reason3);
        b4=findViewById(R.id.reason_extra);
        reason=findViewById(R.id.reportreason);
        send=findViewById(R.id.send_report);

        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectReason(1);
            }
        });
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectReason(2);
            }
        });
        b3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectReason(3);
            }
        });
        b4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectReason(4);
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String reports="";
                if(reportcase==0){
                    Toast.makeText(ReportActivity.this,"신고사유를 선택해주세요.",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(reportcase==4){
                    reports=reason.getText().toString().trim();
                    if(reports.equals("")){
                        Toast.makeText(ReportActivity.this,"기타 사유를 입력해주세요.",Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                else{
                    reports=getReasonLabel(reportcase);
                }

                Map<String,String> data=new HashMap<>();
                data.put(userData.userid,reports);


                reportcr.document(userid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.getResult().exists()){
                            DocumentSnapshot document=task.getResult();
                            if(document.get(userData.userid)!=null){
                                Toast.makeText(ReportActivity.this,"이미 신고한 유저입니다.",Toast.LENGTH_SHORT).show();
                            }
                            else{
                                reportcr.document(userid).set(data, SetOptions.merge()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        Toast.makeText(ReportActivity.this,"신고 접수 완료되었습니다.",Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                });
                            }
                        }
                        else{
                            reportcr.document(userid).set(data, SetOptions.merge()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Toast.makeText(ReportActivity.this,"신고 접수 완료되었습니다.",Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            });
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("신고",e.toString());
                    }
                });

            }
        });

    }

    private void selectReason(int selected){
        reportcase=selected;
        b1.setBackgroundResource(selected==1?R.drawable.bg_report_option_selected:R.drawable.bg_report_option);
        b2.setBackgroundResource(selected==2?R.drawable.bg_report_option_selected:R.drawable.bg_report_option);
        b3.setBackgroundResource(selected==3?R.drawable.bg_report_option_selected:R.drawable.bg_report_option);
        b4.setBackgroundResource(selected==4?R.drawable.bg_report_option_selected:R.drawable.bg_report_option);
        reason.setVisibility(selected==4?View.VISIBLE:View.GONE);
    }

    private String getReasonLabel(int reasonCase){
        if(reasonCase==1){
            return "상의 없이 약속 취소";
        }
        if(reasonCase==2){
            return "약속 장소 불참";
        }
        if(reasonCase==3){
            return "욕설/성희롱 등 불쾌한 언행";
        }
        return "";
    }
}
