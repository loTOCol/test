package com.example.walkingmate.feature.chat.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.SystemClock;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import com.example.walkingmate.feature.user.ui.AppInfoActivity;
import com.example.walkingmate.feature.user.ui.EditUserProfileActivity;
import com.example.walkingmate.feature.user.ui.HelpInfoActivity;
import com.example.walkingmate.feature.user.ui.ManageFriendActivity;
import com.example.walkingmate.R;
import com.example.walkingmate.feature.schedule.ui.ScheduleActivity;
import com.example.walkingmate.feature.user.data.UserData;
import com.example.walkingmate.feature.misc.ui.ChallengeActivity;
import com.example.walkingmate.feature.chat.model.ChatRoom;
import com.example.walkingmate.feature.feed.ui.WalkingHomeActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.checkerframework.checker.units.qual.A;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class ChatFragment extends Fragment {
    DatabaseReference dr= FirebaseDatabase.getInstance().getReference("Chatrooms");
    FirebaseFirestore fb=FirebaseFirestore.getInstance();
    UserData userData;

    ListView chatrooms;
    ChatroomAdapter chatroomAdapter;
    ArrayList<ChatRoom> chatRooms=new ArrayList<>();
    private static final int PAGE_SIZE = 5;
    int currentPage = 0;
    ImageButton prevPageBtn, nextPageBtn;
    TextView pageIndicatorTxt;
    HashMap<String,String> userNameCache=new HashMap<>();
    Set<String> requestedUserName=new HashSet<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("테스트","oc");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("테스트","ocv");
        View view=inflater.inflate(R.layout.fragment_chat, container, false);

        userData=UserData.loadData(getActivity());


        chatrooms=view.findViewById(R.id.ChatroomList);
        prevPageBtn=view.findViewById(R.id.chat_prev_page);
        nextPageBtn=view.findViewById(R.id.chat_next_page);
        pageIndicatorTxt=view.findViewById(R.id.chat_page_indicator);
        chatroomAdapter=new ChatroomAdapter(getActivity());
        chatrooms.setAdapter(chatroomAdapter);
        getlocalChatRooms();
        setupPagingButtons();
        refreshPaging();

        getChatrooms();

        return view;
    }


    //우선 로컬에서 채팅방가져오고 난 뒤 로컬에 없는 신규 채팅방을 추가
    //채팅이 올때마다도 업데이트 되므로 챗룸객체도 겹치는게 존재시 실시간 업데이트
    public void getChatrooms(){

        dr.orderByChild("userids/"+userData.userid).equalTo(true).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot dataSnapshot:snapshot.getChildren()){
                    ChatRoom tmpc=dataSnapshot.getValue(ChatRoom.class);
                    boolean checkroom=false;
                    int idx=-1;
                    //업데이트
                    for(ChatRoom chatRoom: chatRooms){
                        if(chatRoom.roomid.equals(tmpc.roomid)){
                            //새 유저가 들어온 경우
                            if(tmpc.userids.size()!=chatRoom.userids.size()){
                                Log.d("챗룸 이름",tmpc.roomname+",인원 체크"+tmpc.userids.size()+","+chatRoom.userids.size());
                                updateroom(tmpc);

                            }
                            checkroom=true;
                            idx=chatRooms.indexOf(chatRoom);
                            chatRooms.set(idx,tmpc);
                        }
                    }
                    //룸아이디 체크해 존재하지 않는 경우만 추가
                    //이 경우 로컬에도 저장
                    if(!checkroom){
                        chatRooms.add(tmpc);
                        saverooms(tmpc);
                    }
                    Log.d("채팅 추가",chatRooms.size()+", "+chatRooms.get(chatRooms.size()-1).roomid);
                }
                refreshPaging();
                chatroomAdapter.notifyDataSetChanged();
                Log.d("채팅방수",chatroomAdapter.getCount()+"");



            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("채팅읽기 실패",error.toString());
            }
        });
    }

    //나중에 사용자들 목록을 usertmp에 넣는식으로만 수정하면 될듯
    //추가 되므로 로컬에도 저장
    /*public void addChatrooms(){
        SimpleDateFormat sdf=new SimpleDateFormat("HHmmss");
        Date date=new Date(System.currentTimeMillis());

        ChatRoom tmp=new ChatRoom();
        tmp.roomid=sdf.format(date);
        Log.d("채팅룸 아이디",tmp.roomid);
        tmp.roomname="test";
        Map<String,Boolean> usertmp=new HashMap<>();
        usertmp.put("ob_ua6RyFxqm66pBjej9gJ0VDyatPHLDu81RRis__xY",true);
        usertmp.put("C7VynmLzbvX9yXxViYZZxMQQpqeASDbQKg6XFuAnivY",true);
        tmp.userids=usertmp;

        saverooms(tmp);

        dr.child(tmp.roomid).setValue(tmp);
    }*/

    public ArrayList<String> getlocalChatroomslist(){
        String path=getActivity().getFilesDir().getAbsolutePath() + "/messages/";
        File f;
        File[] files;
        ArrayList<String> filenames=new ArrayList<>();

        try{
            f=new File(path);

            //디렉토리가 없으면 null반환
            if(!f.isDirectory()){
                return null;
            }
            files = f.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().toLowerCase(Locale.US).endsWith("room.txt");
                }
            });
            //파일이 없으면 null반환
            if(files.length == 0){
                return null;
            }

            for(int i=0; i<files.length; ++i){
                filenames.add(files[i].getName());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return filenames;
    }


    //로컬에 존재하는 챗룸 불러오기
    public void getlocalChatRooms(){
        ArrayList<ChatRoom> resultrooms=new ArrayList<>();
        ArrayList<String> filelist=getlocalChatroomslist();
        if(filelist==null){
            return;
        }
        Log.d("로컬채팅룸 파일목록",filelist.toString());
        for(String s:filelist){
            String folder= getActivity().getFilesDir().getAbsolutePath() + "/messages/";
            String result="";
            String[] results;


            File check;
            try{
                check=new File(folder);

                String dir=folder+s;
                File file=new File(dir);
                FileInputStream fis=new FileInputStream(file);
                byte[] buffer=new byte[fis.available()];
                fis.read(buffer);
                fis.close();
                result=new String(buffer);
                results=result.split("\n");
                String[] roominfo=results[0].split("@");
                ChatRoom tmpchatroom=new ChatRoom();
                tmpchatroom.roomname=roominfo[0];
                Map<String,Boolean> users=new HashMap<>();
                for(int i=0; i<Integer.parseInt(roominfo[1]); ++i){
                    users.put(roominfo[i+2],true);
                }
                tmpchatroom.userids=users;
                Log.d("챗룸 생성 인원수",users.size()+": "+users.toString());
                tmpchatroom.roomid=s.replace("room.txt","");
                resultrooms.add(tmpchatroom);
                Log.d("로컬채팅룸 불러오기",tmpchatroom.roomid);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        chatRooms= resultrooms;
        refreshPaging();
        chatroomAdapter.notifyDataSetChanged();
    }

    //수락으로 새 유저가 들어오는 경우 업데이트
    public void updateroom(ChatRoom chatRoom){
        try{
            Log.d("챗룸 업데이트 시작",chatRoom.roomname);
            String folder= getActivity().getFilesDir().getAbsolutePath() + "/messages/";
            String filename=chatRoom.roomid+"room.txt";
            File file_path;
            file_path=new File(folder);
            if(!file_path.isDirectory()){
                file_path.mkdirs();
                Log.d("채팅 데이터 저장","경로 생성");
            }
            File files=new File(folder+filename);

            FileWriter fileWriter=new FileWriter(folder+filename,false);
            String firstline=chatRoom.roomname+"@"+chatRoom.userids.size()+"@";
            for(String s:chatRoom.userids.keySet()){
                firstline+=s+"@";
            }
            Log.d("챗룸 저장",firstline);
            firstline+="\n";
            fileWriter.write(firstline);
            Log.d("채팅룸 저장",chatRoom.roomid);
            fileWriter.close();

        }catch(Exception e){
            e.printStackTrace();
            Log.d("챗룸 업데이트 에러",e.toString());
        }
    }

    public void saverooms(ChatRoom chatRoom){
        try{
            String folder= getActivity().getFilesDir().getAbsolutePath() + "/messages/";
            String filename=chatRoom.roomid+"room.txt";
            File file_path;
            file_path=new File(folder);
            if(!file_path.isDirectory()){
                file_path.mkdirs();
                Log.d("채팅 데이터 저장","경로 생성");
            }
            File files=new File(folder+filename);
            if(!files.exists()){
                FileWriter fileWriter=new FileWriter(folder+filename,true);
                String firstline=chatRoom.roomname+"@"+chatRoom.userids.size()+"@";
                for(String s:chatRoom.userids.keySet()){
                    firstline+=s+"@";
                }
                firstline+="\n";
                fileWriter.write(firstline);
                Log.d("채팅룸 저장",chatRoom.roomid);
                fileWriter.close();
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void Outroom(ChatRoom chatRoom){
        String roomid=chatRoom.roomid;
        Map<String,Object> tmp=new HashMap<>();
        tmp.put(userData.userid,false);
        dr.child(roomid).child("userids").updateChildren(tmp).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                checkDel(roomid);
            }
        });
        deleteroom(roomid);
        chatRooms.remove(chatRoom);
        refreshPaging();
        chatroomAdapter.notifyDataSetChanged();
    }

    private void setupPagingButtons(){
        prevPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentPage>0){
                    currentPage--;
                    refreshPaging();
                    chatroomAdapter.notifyDataSetChanged();
                }
            }
        });

        nextPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int totalPages=getTotalPages();
                if(currentPage<totalPages-1){
                    currentPage++;
                    refreshPaging();
                    chatroomAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private int getTotalPages(){
        if(chatRooms.size()==0){
            return 1;
        }
        return (chatRooms.size()+PAGE_SIZE-1)/PAGE_SIZE;
    }

    private void refreshPaging(){
        int totalPages=getTotalPages();
        if(currentPage>totalPages-1){
            currentPage=Math.max(0,totalPages-1);
        }
        pageIndicatorTxt.setText((currentPage+1)+" / "+totalPages);
        prevPageBtn.setEnabled(currentPage>0);
        nextPageBtn.setEnabled(currentPage<totalPages-1);
        prevPageBtn.setAlpha(currentPage>0?1.0f:0.35f);
        nextPageBtn.setAlpha(currentPage<totalPages-1?1.0f:0.35f);
    }

    private void requestUserName(String userid){
        if(userid==null||userid.equals("")||requestedUserName.contains(userid)){
            return;
        }
        requestedUserName.add(userid);
        fb.collection("users").document(userid).get().addOnCompleteListener(task -> {
            if(task.isSuccessful()&&task.getResult()!=null&&task.getResult().exists()){
                String name=task.getResult().getString("appname");
                if(name!=null&&!name.trim().equals("")){
                    userNameCache.put(userid,name.trim());
                }else{
                    userNameCache.put(userid,"알 수 없는 사용자");
                }
            }else{
                userNameCache.put(userid,"알 수 없는 사용자");
            }
            if(getActivity()!=null){
                getActivity().runOnUiThread(() -> chatroomAdapter.notifyDataSetChanged());
            }
        });
    }

    private String resolveDisplayTitle(ChatRoom room){
        if(room==null){
            return "채팅방";
        }
        if(room.customTitle!=null&&!room.customTitle.trim().equals("")){
            return room.customTitle.trim();
        }

        ArrayList<String> names=new ArrayList<>();
        if(room.userids!=null){
            for(Map.Entry<String,Boolean> entry: room.userids.entrySet()){
                String uid=entry.getKey();
                Boolean active=entry.getValue();
                if(uid==null||uid.equals(userData.userid)||active==null||!active){
                    continue;
                }
                String cached=userNameCache.get(uid);
                if(cached==null){
                    requestUserName(uid);
                }else{
                    names.add(cached);
                }
            }
        }
        names.sort(String::compareTo);
        if(names.size()==0){
            return "채팅방";
        }
        return String.join(", ", names);
    }

    private String previewMessage(String raw){
        if(raw==null||raw.equals("")){
            return "";
        }
        if(raw.startsWith("photo:")){
            return "[사진]";
        }
        if(raw.startsWith("emoji:")){
            return "[이모지]";
        }
        if((raw.startsWith("http://") || raw.startsWith("https://"))
                && (raw.contains("firebasestorage") || raw.contains("chat_images"))){
            return "[사진]";
        }
        return raw;
    }

    private String formatListTime(String time){
        if(time==null||time.length()<12){
            return "";
        }
        String y=time.substring(2,4);
        String m=time.substring(4,6);
        String d=time.substring(6,8);
        String h=time.substring(8,10);
        String min=time.substring(10,12);
        return String.format("%s/%s/%s %s:%s",y,m,d,h,min);
    }

    public void checkDel(String roomid){
        dr.child(roomid).child("userids").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count=0;
                Map<String,Boolean> users= (Map<String, Boolean>) snapshot.getValue();
                for(String s:users.keySet()){
                    if(users.get(s)){
                        ++count;
                    }
                }
                if(count==0){
                    dr.child(roomid).removeValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void deleteroom(String roomid){
        String folder= getActivity().getFilesDir().getAbsolutePath() + "/messages/";
        String pathroom=folder+roomid+"room.txt";
        String pathmessage=folder+roomid+"message.txt";
        ArrayList<String> pathimgs=scanimgs(roomid);
        Log.d("챗룸 삭제",pathimgs.toString());

        try{
            File fileroom=new File(pathroom);
            if(fileroom.exists()){
                fileroom.delete();
            }
            File filemsg=new File(pathmessage);
            if(filemsg.exists()){
                filemsg.delete();
            }
            if(pathimgs.size()>0){
                for(int i=0; i<pathimgs.size(); ++i){
                    File fileimg=new File(folder+pathimgs.get(i));
                    if(fileimg.exists()){
                        fileimg.delete();
                        Log.d("파일 삭제",fileimg.getName());
                    }
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public ArrayList<String> scanimgs(String roomid){
        String path=getActivity().getFilesDir().getAbsolutePath()+ "/messages/";
        File f;
        File[] files;
        ArrayList<String> filenames=new ArrayList<>();

        try{
            f=new File(path);

            //디렉토리가 없으면 null반환
            if(!f.isDirectory()){
                return filenames;
            }
            files = f.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().toLowerCase(Locale.US).startsWith(roomid.toLowerCase(Locale.US));
                }
            });
            //파일이 없으면 null반환
            if(files.length == 0){
                Log.d("챗룸 삭제 스캔","파일없음");
                return filenames;
            }

            for(int i=0; i<files.length; ++i){
                if(files[i].getName().contains(".jpg")){
                    filenames.add(files[i].getName());
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return filenames;
    }

    public class ChatroomAdapter extends BaseAdapter {

        LayoutInflater layoutInflater;
        Context context;

        public ChatroomAdapter(Context context){
            this.context=context;
            layoutInflater=LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            if(chatRooms.size()==0){
                return 1;
            }
            int start=currentPage*PAGE_SIZE;
            int remain=chatRooms.size()-start;
            if(remain<=0){
                return 0;
            }
            return Math.min(PAGE_SIZE, remain);
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
            View view=layoutInflater.inflate(R.layout.item_chat_room,null);
            View emptyview=layoutInflater.inflate(R.layout.item_common_empty,null);
            if(chatRooms.size()==0){
                return emptyview;
            }
            int realPosition=currentPage*PAGE_SIZE+position;
            if(realPosition>=chatRooms.size()){
                return emptyview;
            }
            LinearLayout chatroombody=view.findViewById(R.id.body_chatroom);
            TextView chatroomname=view.findViewById(R.id.chatroomname);
            TextView chatroommeta=view.findViewById(R.id.chatroommeta);
            ImageButton outrooms=view.findViewById(R.id.outroom);
            TextView lastmsg=view.findViewById(R.id.lastmsg);
            TextView usernum=view.findViewById(R.id.usernum);

            ChatRoom currentRoom=chatRooms.get(realPosition);
            Log.d("채팅방 세팅",currentRoom.roomname);
            chatroomname.setText(resolveDisplayTitle(currentRoom));
            chatroombody.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ArrayList<String> useridslist=new ArrayList<>();
                    for(String s:currentRoom.userids.keySet()){
                        useridslist.add(s);
                    }
                    Intent intent=new Intent(getActivity(),ChatActivity.class);
                    intent.putExtra("roomid",currentRoom.roomid);
                    intent.putExtra("userids", useridslist);
                    intent.putExtra("room_title", resolveDisplayTitle(currentRoom));
                    startActivity(intent);
                }
            });

            if(currentRoom.comments!=null&&currentRoom.comments.size()!=0){
                Map<String, ChatRoom.Comment> cmap= currentRoom.comments;
                long lasttime=0;
                String laststr="";
                for(String s:cmap.keySet()){
                    long tmptime=Long.parseLong(cmap.get(s).time);
                    if(tmptime>lasttime){
                        lasttime=tmptime;
                        laststr=cmap.get(s).msg;
                    }
                }
                laststr=previewMessage(laststr);
                if(laststr.length()>50){
                    laststr=laststr.substring(0,50)+"...";
                }
                lastmsg.setText(laststr);
                chatroommeta.setText(formatListTime(lasttime+""));
                chatroommeta.setVisibility(View.VISIBLE);
            }
            else{
                lastmsg.setText("대화를 시작해보세요.");
                chatroommeta.setText("");
                chatroommeta.setVisibility(View.GONE);
            }

            Map<String,Boolean> users=currentRoom.userids;
            int usern=0;
            for(String s:users.keySet()){
                if(users.get(s)){
                    usern++;
                }
            }
            usernum.setText(usern+"");

            outrooms.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showLeaveRoomDialog(currentRoom);
                }
            });



            return view;
        }
    }

    private void showLeaveRoomDialog(ChatRoom room){
        if(getActivity()==null){
            return;
        }
        final Dialog dialog=new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_leave_chat);
        if(dialog.getWindow()!=null){
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        Button cancel=dialog.findViewById(R.id.leave_cancel);
        Button confirm=dialog.findViewById(R.id.leave_confirm);
        cancel.setOnClickListener(v -> dialog.dismiss());
        confirm.setOnClickListener(v -> {
            dialog.dismiss();
            Outroom(room);
        });
        dialog.show();
    }

    public String returntime(String time){
        String y=time.substring(2,4);
        String m=time.substring(4,6);
        String d=time.substring(6,8);
        String h=time.substring(8,10);
        String min=time.substring(10,12);
        return String.format(" (%s/%s/%s %s:%s)",y,m,d,h,min);

    }

}
