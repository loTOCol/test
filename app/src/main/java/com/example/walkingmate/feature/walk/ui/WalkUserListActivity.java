package com.example.walkingmate.feature.walk.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.walkingmate.R;
import com.example.walkingmate.feature.user.data.UserData;
import com.example.walkingmate.feature.user.ui.UserProfileActivity;
import com.example.walkingmate.feature.chat.model.ChatRoom;
import com.example.walkingmate.feature.chat.ui.ChatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WalkUserListActivity extends Activity {
    FirebaseFirestore fb=FirebaseFirestore.getInstance();
    CollectionReference walkuser;
    CollectionReference users=fb.collection("users");
    CollectionReference challenge=fb.collection("challenge");
    CollectionReference schedule=fb.collection("schedule");
    CollectionReference datalist;
    CollectionReference request;
    CollectionReference matedatalistRef=fb.collection("matedatalist");

    String docuid;
    String walkname;
    boolean ismate;

    UserData userData;

    ListView waitlistview,acceptlistview;

    //0은 대기, 1은 수락 ,2는 거절
    ArrayList<String> waituser=new ArrayList<>();
    ArrayList<String> waituserprofile=new ArrayList<>();

    ArrayList<String> acceptuser=new ArrayList<>();
    ArrayList<String> acceptuserprofile=new ArrayList<>();
    WaitAdapter waitAdapter;
    AcceptAdapter acceptAdapter;

    LinearLayout askdel;
    TextView title_askdel;
    Button del,cancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_walk_user_list);

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        userData=UserData.loadData(this);
        walkname=null;

        Intent getintent=getIntent();
        docuid=getintent.getStringExtra("mydocu");
        walkname=getintent.getStringExtra("walkname");
        //false면 산책, true면 메이트
        ismate=getintent.getBooleanExtra("ismate",false);
        if(ismate){
            walkuser=fb.collection("mateuser");
            datalist=fb.collection("matedata");
            request=fb.collection("materequest");
        }
        else{
            walkuser=fb.collection("walkuser");
            datalist=fb.collection("walkdata");
            request=fb.collection("walkrequest");
        }

        waitlistview=findViewById(R.id.walkuserlist_waiting);
        acceptlistview=findViewById(R.id.walkuserlist_accept);

        waitAdapter=new WaitAdapter(this);
        acceptAdapter=new AcceptAdapter(this);

        waitlistview.setAdapter(waitAdapter);
        acceptlistview.setAdapter(acceptAdapter);

        getlist();

        askdel=findViewById(R.id.askdel_walkuserlist);
        del=findViewById(R.id.delete_walkuser);
        cancel=findViewById(R.id.cancel_walkuser);
        title_askdel=findViewById(R.id.title_askdel);

        title_askdel.setText(walkname);

        findViewById(R.id.delete_walk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                askdel.setVisibility(View.VISIBLE);
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                askdel.setVisibility(View.INVISIBLE);
            }
        });

        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deletewalk();
            }
        });


    }

    //신청 대기 혹은 수락 유저가 한명도 없거나 예상 종료시간이 지난 경우 삭제 가능
    public void deletewalk(){
        walkuser.document(docuid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                Map<String,Long> userlist= (Map<String, Long>) task.getResult().get("userlist");
                //신청 유저가 없거나 수락, 대기유저가 없는경우. userlist가 존재하는 경우 삭제
                if(userlist!=null){
                    Log.d("삭제컨디션 확인",userlist.values()+":"+userlist.values().contains(1l)+","+userlist.values().contains(0l));
                }
                if(userlist==null||!(userlist.values().contains(1l)||userlist.values().contains(0l))){
                    walkuser.document(docuid).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            datalist.document(docuid).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(ismate){
                                        matedatalistRef.document(docuid).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                Log.d("메이트 리스트 삭제",docuid);
                                            }
                                        });
                                    }
                                    if(userlist!=null){
                                        int[] checkend={0};
                                        for(String usercontains:userlist.keySet()){
                                            request.document(usercontains).update("requestlist", FieldValue.arrayRemove(docuid)).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    checkend[0]++;
                                                    if(checkend[0]==userlist.size()){
                                                        Log.d("requset삭제",usercontains+","+checkend[0]);
                                                        Toast.makeText(getApplicationContext(),"삭제 완료",Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    }
                                                }
                                            });
                                        }
                                    }
                                    else{
                                        Toast.makeText(getApplicationContext(),"삭제 완료",Toast.LENGTH_SHORT).show();
                                        finish();
                                    }

                                }
                            });
                        }
                    });
                }
                //신청 유저가 존재하는경우
                else{
                    Log.d("삭제 체크","신청유저 존재");
                    datalist.document(docuid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            DocumentSnapshot document=task.getResult();
                            String end="";
                            try {
                                end=getdateEnd(document.get("year"),document.get("month"),document.get("day"),
                                        document.get("hour"),document.get("minute"),document.get("takentime"));

                                //종료된 게시물:requset까지 체크후 삭제
                                if(checkend(end)){
                                    //리스트에서 삭제
                                    datalist.document(docuid).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(ismate){
                                                matedatalistRef.document(docuid).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        Log.d("메이트 리스트 삭제",docuid);
                                                    }
                                                });
                                            }
                                            //게시물 신청유저 목록 삭제
                                            walkuser.document(docuid).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    //신청 유저 목록 기반 삭제 진행
                                                    if(userlist!=null){
                                                        int[] checkend={0};
                                                        for(String usercontains:userlist.keySet()){
                                                            request.document(usercontains).update("requestlist", FieldValue.arrayRemove(docuid)).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    checkend[0]++;
                                                                    if(checkend[0]==userlist.size()){
                                                                        Log.d("requset삭제",usercontains+","+checkend[0]);
                                                                        Toast.makeText(getApplicationContext(),"삭제 완료",Toast.LENGTH_SHORT).show();
                                                                        finish();
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    }
                                                    else{
                                                        Toast.makeText(getApplicationContext(),"삭제 완료",Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    }
                                                }
                                            });
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d("게시물 삭제 실패_리스트",e.toString());
                                        }
                                    });
                                }
                                else{
                                    Toast.makeText(getApplicationContext(),"삭제가 불가능한 게시물입니다.",Toast.LENGTH_SHORT).show();
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    });
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

    public void getlist(){
        waituser.clear();
        acceptuser.clear();
        Log.d("산책 메이트 아이디",docuid);
        walkuser.document(docuid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.getResult().get("userlist")!=null){
                    Map<String,Long> userlist= (Map<String, Long>) task.getResult().get("userlist");
                    for(Map.Entry<String,Long> entry: userlist.entrySet()){
                        String strKey=entry.getKey();
                        Long intValue=entry.getValue();
                        Log.d("산책 유저리스트",intValue+", "+strKey);
                        if(intValue==0){
                            waituser.add(strKey);
                        }
                        else if(intValue==1){
                            acceptuser.add(strKey);
                        }
                    }

                }
                getprofiles();



            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("산책 메이트 실패",e.toString());
            }
        });
    }

    public void getprofiles(){
        waituserprofile.clear();
        acceptuserprofile.clear();
        if(waituser.size()>0){
            for(int i=0; i<waituser.size(); ++i){
                users.document(waituser.get(i)).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot document=task.getResult();
                        String genderstr;
                        if(document.get("gender").equals("M")){
                            genderstr="남성";
                        }
                        else {
                            genderstr="여성";
                        }
                        String result =String.format("%s (%s/%s)",document.get("appname"),genderstr,document.get("age"));
                        waituserprofile.add(result);
                        waitAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
        if(acceptuser.size()>0){
            for(int i=0; i<acceptuser.size(); ++i){
                users.document(acceptuser.get(i)).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot document=task.getResult();
                        String genderstr;
                        if(document.get("gender").equals("M")){
                            genderstr="남성";
                        }
                        else {
                            genderstr="여성";
                        }
                        String result =String.format("%s (%s/%s)",document.get("appname"),genderstr,document.get("age"));
                        acceptuserprofile.add(result);
                        acceptAdapter.notifyDataSetChanged();
                    }
                });
            }
        }


    }

    public void setstate(String userid,int state){
        HashMap<String,Object> data=new HashMap<>();

        HashMap<String, Integer> myreq=new HashMap<>();
        myreq.put(userid,state);
        data.put("userlist",myreq);

        walkuser.document(docuid).set(data, SetOptions.merge()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Log.d("산책 메이트", "업데이트 성공");
            }
        });
    }




    public class WaitAdapter extends BaseAdapter{

        LayoutInflater layoutInflater;
        Context context;

        public WaitAdapter(Context context){
            this.context=context;
            layoutInflater=LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return waituser.size();
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
            View view=layoutInflater.inflate(R.layout.item_walk_request_wait,null);
            View emptyview=layoutInflater.inflate(R.layout.view_empty,null);
            if(waituser.size()==0){
                return emptyview;
            }
            TextView textView=view.findViewById(R.id.user_walkwait);
            ImageButton accept=view.findViewById(R.id.accept_walkwait);
            ImageButton reject=view.findViewById(R.id.reject_walkwait);
            ImageButton chat=view.findViewById(R.id.chat_walkwait);

            LinearLayout body=view.findViewById(R.id.body_walkwait);
            body.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent=new Intent(WalkUserListActivity.this, UserProfileActivity.class);
                    intent.putExtra("userid",waituser.get(position));
                    startActivity(intent);
                }
            });

            if(waituserprofile.size()>=(position+1)){
                textView.setText(waituserprofile.get(position));
            }
            else{
                Log.d("에러","error");
            }


            accept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    //사교적인 워커 본인 반영은 처음 수락시
                    if(acceptuser.size()==0){
                        challenge.document(userData.userid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if(task.isSuccessful()){
                                    Long mymeet= (Long) task.getResult().get("meet");
                                    challenge.document(userData.userid).update("meet",mymeet+1);
                                }
                            }
                        });
                    }


                    //수락한 유저 도전과제 반영
                    challenge.document(waituser.get(position)).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if(task.isSuccessful()){
                                Log.d("문서 체크",task.getResult().get("meet").toString());
                                Long mymeet= (Long) task.getResult().get("meet");
                                Log.d("신청유저",waituser.toString());
                                challenge.document(waituser.get(position)).update("meet",mymeet+1).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        setstate(waituser.get(position),1);
                                        acceptuser.add(waituser.get(position));

                                        Log.d("수락 목록",waituser.get(position));

                                        acceptuserprofile.add(waituserprofile.get(position));
                                        acceptUserChatrooms(docuid,waituser.get(position));

                                        waituser.remove(position);
                                        waituserprofile.remove(position);
                                        notifyDataSetChanged();
                                        acceptAdapter.notifyDataSetChanged();
                                        scheduleGen();
                                    }
                                });
                            }
                        }
                    });

                }
            });

            reject.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setstate(waituser.get(position),2);
                    waituser.remove(position);
                    waituserprofile.remove(position);
                    notifyDataSetChanged();
                }
            });

            //이미 채팅이 생성되었다면 채팅방 열기
            //채팅이 없으면 채팅방 생성
            chat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    addPersonalChatrooms(docuid,waituser.get(position));
                }
            });

            return view;
        }
    }

    //개인 신청자에 대하여 채팅방 생성 및 이동, 이미존재하면 그냥 이동
    public void addPersonalChatrooms(String docuid, String userid){
        DatabaseReference dr= FirebaseDatabase.getInstance().getReference("Chatrooms");
        resolveRoomTitle(docuid, new TitleCallback() {
            @Override
            public void onResult(String resolvedTitle) {
                ChatRoom tmp=new ChatRoom();
                tmp.roomid=docuid+"@"+userid;
                Log.d("채팅룸 아이디",tmp.roomid);
                tmp.roomname=resolvedTitle;
                tmp.ownerId=userData.userid;
                tmp.customTitle="";
                Map<String,Boolean> usertmp=new HashMap<>();
                usertmp.put(userData.userid,true);
                usertmp.put(userid,true);
                tmp.userids=usertmp;

                //채팅방 존재여부 체크 후 이동
                dr.child(tmp.roomid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.getValue(ChatRoom.class)!=null){
                            //전에 나갔던 채팅방이면 다시 true로 바꾸며 들어옴
                            if(snapshot.getValue(ChatRoom.class).userids.get(userData.userid).equals(false)){
                                Map<String,Object> tmpuser=new HashMap<>();
                                tmpuser.put(userData.userid,true);
                                dr.child(tmp.roomid).child("userids").updateChildren(tmpuser).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        Log.d("채팅방 생성","이미 존재하는 채팅방-해당 채팅방으로 이동");
                                        Intent intent=new Intent(WalkUserListActivity.this,ChatActivity.class);
                                        intent.putExtra("roomid",tmp.roomid);
                                        ArrayList<String> tmpid=new ArrayList<>();
                                        tmpid.add(userid);
                                        intent.putExtra("userids",tmpid);
                                        intent.putExtra("room_title", resolvedTitle);
                                        startActivity(intent);
                                    }
                                });
                            }
                            else{
                                Log.d("채팅방 생성","이미 존재하는 채팅방-해당 채팅방으로 이동");
                                Intent intent=new Intent(WalkUserListActivity.this,ChatActivity.class);
                                intent.putExtra("roomid",tmp.roomid);
                                ArrayList<String> tmpid=new ArrayList<>();
                                tmpid.add(userid);
                                intent.putExtra("userids",tmpid);
                                intent.putExtra("room_title", resolvedTitle);
                                startActivity(intent);
                            }
                        }
                        else{
                            Log.d("채팅방 생성","새 채팅방 생성 후 이동");
                            dr.child(tmp.roomid).setValue(tmp).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Intent intent=new Intent(WalkUserListActivity.this,ChatActivity.class);
                                    intent.putExtra("roomid",tmp.roomid);
                                    ArrayList<String> tmpid=new ArrayList<>();
                                    tmpid.add(userid);
                                    intent.putExtra("userids",tmpid);
                                    intent.putExtra("room_title", resolvedTitle);
                                    startActivity(intent);
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });

    }

    //게시물의 수락자들을 모아놓는 채팅방 생성.
    //수락 누를때 마다 실행.
    public void acceptUserChatrooms(String docuid, String acceptuserid){
        DatabaseReference dr= FirebaseDatabase.getInstance().getReference("Chatrooms");
        resolveRoomTitle(docuid, new TitleCallback() {
            @Override
            public void onResult(String resolvedTitle) {
                ChatRoom tmp=new ChatRoom();
                tmp.roomid=docuid;
                Log.d("채팅룸 아이디",tmp.roomid);
                tmp.roomname=resolvedTitle;
                tmp.ownerId=userData.userid;
                tmp.customTitle="";
                Map<String,Boolean> usertmp=new HashMap<>();
                usertmp.put(userData.userid,true);
                usertmp.put(acceptuserid,true);
                tmp.userids=usertmp;

                //채팅방 존재여부 체크 후 이동
                dr.child(tmp.roomid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.getValue(ChatRoom.class)!=null){
                            Log.d("수락채팅방 생성","이미 존재하는 채팅방-수락자 추가");
                            Map<String,Object> adduser=new HashMap<>();
                            adduser.put(acceptuserid,true);
                            dr.child(tmp.roomid).child("userids").updateChildren(adduser);
                        }
                        else{
                            Log.d("수락채팅방 생성","새 채팅방 생성");
                            dr.child(tmp.roomid).setValue(tmp);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });
    }

    private interface TitleCallback{
        void onResult(String resolvedTitle);
    }

    private void resolveRoomTitle(String docuid, TitleCallback callback){
        datalist.document(docuid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                String titleText=walkname==null?"채팅방":walkname;
                if(task.isSuccessful()&&task.getResult()!=null&&task.getResult().exists()){
                    String docTitle=task.getResult().getString("title");
                    if(docTitle!=null&&!docTitle.trim().equals("")){
                        titleText=docTitle.trim();
                    }
                }
                callback.onResult(titleText);
            }
        });
    }

    //acceptuser 변경 이후 시점에 추가 하기: 변경된 값 기반으로 설정하는것이기 때문에
    public void scheduleGen(){
        datalist.document(docuid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot document=task.getResult();
                Map<String,Object> docu=new HashMap<>();
                Map<String,Object> addfield=new HashMap<>();
                try {
                    addfield.put("start",getdateStart(document.get("year"),document.get("month"),document.get("day"),document.get("hour"),document.get("minute")));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                try {
                    addfield.put("end",getdateEnd(document.get("year"),document.get("month"),document.get("day"),document.get("hour"),document.get("minute"),document.get("takentime")));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                String name;
                if(ismate){
                    name="[메이트]"+walkname;
                }
                else{
                    name="[산책]"+walkname;
                }
                addfield.put("name",name);
                if(ismate){
                    addfield.put("locations",document.get("locations_coordinate"));
                }
                else{
                    ArrayList<Object> tmp=new ArrayList<>();
                    tmp.add(document.get("location_coord"));
                    addfield.put("locations",tmp);
                }

                ArrayList<String> acceptuserrefix=new ArrayList<>();
                acceptuserrefix.add(userData.userid);
                acceptuserrefix.addAll(acceptuser);

                addfield.put("users",acceptuserrefix);
                addfield.put("type",ismate);
                addfield.put("total",acceptuserrefix.size()-1); //나 제외 평가자수
                docu.put(docuid,addfield);

                for(String s: acceptuser){
                    schedule.document(s).set(docu, SetOptions.merge());
                }
                schedule.document(userData.userid).set(docu, SetOptions.merge());

            }
        });

    }

    public String getdateStart(Object y, Object m, Object d, Object h, Object min) throws ParseException {
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy/MM/dd HH:mm");
        String start=String.format("%04d/%02d/%02d %02d:%02d",y,m,d,h,min);
        return start;
    }

    public String getdateEnd(Object y, Object m, Object d, Object h, Object min, Object taken) throws ParseException {
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy/MM/dd HH:mm");
        String start=String.format("%04d/%02d/%02d %02d:%02d",y,m,d,h,min);
        Date date=sdf.parse(start);
        Calendar calendar=Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE,Long.valueOf((Long) Optional.ofNullable(taken).orElse(0L)).intValue());
        return sdf.format(calendar.getTime());
    }


    public class AcceptAdapter extends BaseAdapter{

        LayoutInflater layoutInflater;
        Context context;

        public AcceptAdapter(Context context){
            this.context=context;
            layoutInflater=LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return acceptuser.size();
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
            View view=layoutInflater.inflate(R.layout.item_walk_request_wait,null);
            View emptyview=layoutInflater.inflate(R.layout.view_empty,null);
            if(acceptuser.size()==0){
                return emptyview;
            }
            TextView textView=view.findViewById(R.id.user_walkwait);
            ImageButton accept=view.findViewById(R.id.accept_walkwait);
            ImageButton reject=view.findViewById(R.id.reject_walkwait);
            ImageButton chat=view.findViewById(R.id.chat_walkwait);

            LinearLayout body=view.findViewById(R.id.body_walkwait);
            body.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent=new Intent(WalkUserListActivity.this, UserProfileActivity.class);
                    intent.putExtra("userid",acceptuser.get(position));
                    startActivity(intent);
                }
            });

            textView.setText(acceptuserprofile.get(position));

            accept.setVisibility(View.INVISIBLE);

            reject.setVisibility(View.INVISIBLE);

            chat.setVisibility(View.INVISIBLE);

            return view;
        }
    }
}
