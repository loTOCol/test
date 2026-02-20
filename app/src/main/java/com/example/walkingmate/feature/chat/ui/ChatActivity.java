package com.example.walkingmate.feature.chat.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.example.walkingmate.feature.user.ui.AppInfoActivity;
import com.example.walkingmate.feature.user.ui.EditUserProfileActivity;
import com.example.walkingmate.feature.user.ui.HelpInfoActivity;
import com.example.walkingmate.feature.user.ui.ManageFriendActivity;
import com.example.walkingmate.R;
import com.example.walkingmate.feature.schedule.ui.ScheduleActivity;
import com.example.walkingmate.feature.user.data.UserData;
import com.example.walkingmate.feature.user.ui.UserProfileActivity;
import com.example.walkingmate.feature.misc.ui.ChallengeActivity;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.example.walkingmate.feature.chat.model.ChatRoom;
import com.example.walkingmate.feature.feed.ui.WalkingHomeActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity implements DrawerLayout.DrawerListener {
    DatabaseReference dr = FirebaseDatabase.getInstance().getReference("Chatrooms");
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    CollectionReference user = db.collection("users");
    private final StorageReference chatImageStorage = FirebaseStorage.getInstance().getReference("chat_images");

    ChildEventListener childEventListener;
    ValueEventListener valueEventListener;
    ValueEventListener roomMetaListener;

    String roomid;
    ArrayList<String> users = new ArrayList<>();
    HashMap<String, Bitmap> userimgs = new HashMap<>();
    HashMap<String, String> usernames = new HashMap<>();

    EditText msg;
    ImageButton sendmsg;
    ListView msglist;

    UserData userData;

    MsgAdapter msgAdapter;
    UserAdapter userAdapter;

    String starttime = "0";

    DrawerLayout drawerLayout;
    ListView userlist;
    TextView roomtitle;
    ImageButton emojiButton;
    Button renameRoomBtn;
    String roomTitleOverride;
    String roomOwnerId = "";
    String customRoomTitle = "";
    Map<String, Boolean> roomUserState = new HashMap<>();

    HorizontalBarChart myBarChart, otherBarChart;
    private static final int REQ_PICK_CHAT_IMAGE = 9001;
    private final HashMap<String, Bitmap> chatImageCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        Log.d("채팅방", "activity시작");


        roomtitle = findViewById(R.id.chat_title);
        myBarChart = findViewById(R.id.myBarChart);
        otherBarChart = findViewById(R.id.otherBarChart);
        emojiButton = findViewById(R.id.emojiButton);
        renameRoomBtn = findViewById(R.id.rename_chatroom);
        drawerLayout = findViewById(R.id.ChatLayout);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        userlist = findViewById(R.id.chatuserlist);

        userData = UserData.loadData(this);

        Intent intent = getIntent();
        roomid = intent.getStringExtra("roomid");
        roomTitleOverride = intent.getStringExtra("room_title");

        if(roomTitleOverride!=null && !roomTitleOverride.trim().equals("")){
            roomtitle.setText(roomTitleOverride.trim());
        }
        settitle(roomid);

        users = intent.getStringArrayListExtra("userids");
        if(users==null){
            users = new ArrayList<>();
        }
        users.remove(userData.userid);

        checkuser();
        loadlocalprofile();

        findViewById(R.id.menu_chat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(Gravity.RIGHT);
            }
        });
        renameRoomBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRenameDialog();
            }
        });

        findViewById(R.id.back_chat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        msg = findViewById(R.id.msg);
        sendmsg = findViewById(R.id.sendmsg);
        msglist = findViewById(R.id.msglist);

        userAdapter = new UserAdapter(this);
        userlist.setAdapter(userAdapter);
        msgAdapter = new MsgAdapter(this, roomid, userData.userid);
        msglist.setAdapter(msgAdapter);
        if (msgAdapter.getCount() > 0) {
            msglist.setSelection(msgAdapter.getCount() - 1);
        }

        loaduserprofile();
        loadRoomMeta();

        sendmsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendmsgs();
                msg.getText().clear();
            }
        });
        emojiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePicker();
            }
        });

        if (myBarChart != null && otherBarChart != null) {
            setupBarChart(myBarChart);
            setupBarChart(otherBarChart);
            updateBarCharts();
        }

    }
    private void showEmojiPicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View emojiPickerView = inflater.inflate(R.layout.view_chat_emoji_picker, null);
        GridView emojiGridView = emojiPickerView.findViewById(R.id.emoji_grid_view);

        int[] emojiResIds = ChatEmojiCatalog.buildAvailableEmojiResIds();

        EmojiAdapter emojiAdapter = new EmojiAdapter(this, emojiResIds);
        emojiGridView.setAdapter(emojiAdapter);

        emojiGridView.setOnItemClickListener((parent, view, position, id) -> {
            int selectedEmoji = emojiResIds[position];
            msg.setText("emoji:" + selectedEmoji);
        });

        builder.setView(emojiPickerView);
        builder.setTitle("이모티콘 선택");
        builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }



    private void sendEmoji(int emojiResId) {
        // 이모티콘을 메시지로 전송하는 로직 추가
        // 예를 들어, 이모티콘 리소스 ID를 메시지로 변환하여 전송
        ChatRoom.Comment tmpcomment = new ChatRoom.Comment();
        tmpcomment.userid = userData.userid;
        tmpcomment.msg = "emoji:" + emojiResId;
        tmpcomment.time = new SimpleDateFormat("yyyyMMddHHmmssSS").format(new Date());

        dr.child("/" + roomid + "/comments/").push().setValue(tmpcomment);
    }
    public class EmojiAdapter extends BaseAdapter {
        private Context context;
        private int[] emojiIds;

        public EmojiAdapter(Context context, int[] emojiIds) {
            this.context = context;
            this.emojiIds = emojiIds;
        }

        @Override
        public int getCount() {
            return emojiIds.length;
        }

        @Override
        public Object getItem(int position) {
            return emojiIds[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.emoji_item, parent, false);
            }

            ImageView imageView = convertView.findViewById(R.id.emoji_image);
            imageView.setImageResource(emojiIds[position]);

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((ChatActivity) context).sendEmoji(emojiIds[position]);
                }
            });

            return convertView;
        }
        private void sendEmoji(int emojiResId) {
            ChatRoom.Comment tmpcomment = new ChatRoom.Comment();
            tmpcomment.userid = userData.userid;
            tmpcomment.msg = "emoji:" + emojiResId;
            tmpcomment.time = new SimpleDateFormat("yyyyMMddHHmmssSS", Locale.getDefault()).format(new Date());

            dr.child("/" + roomid + "/comments/").push().setValue(tmpcomment);
        }
    }
    private void setupBarChart(HorizontalBarChart barChart) {
        barChart.getDescription().setEnabled(false);
        barChart.setTouchEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setScaleEnabled(false);
        barChart.setPinchZoom(false);
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawValueAboveBar(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setEnabled(false);

        YAxis axisLeft = barChart.getAxisLeft();
        axisLeft.setDrawGridLines(false);
        axisLeft.setDrawAxisLine(false);
        axisLeft.setAxisMinimum(0f);
        axisLeft.setAxisMaximum(10000f);
        axisLeft.setDrawLabels(false);

        YAxis axisRight = barChart.getAxisRight();
        axisRight.setDrawLabels(false);
        axisRight.setDrawGridLines(false);
        axisRight.setDrawAxisLine(false);
    }

    private void updateBarCharts() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("walk").child(userData.userid);
        userRef.child("currentSteps").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer myStepsValue = snapshot.getValue(Integer.class);
                int mySteps = myStepsValue == null ? 0 : myStepsValue;
                updateBarChart(myBarChart, mySteps);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        if (users.size() > 0) {
            String otherUserId = users.get(0);
            DatabaseReference otherUserRef = FirebaseDatabase.getInstance().getReference("walk").child(otherUserId);
            otherUserRef.child("currentSteps").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Integer otherStepsValue = snapshot.getValue(Integer.class);
                    int otherSteps = otherStepsValue == null ? 0 : otherStepsValue;
                    updateBarChart(otherBarChart, otherSteps);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }
    }

    private void updateBarChart(HorizontalBarChart barChart, int steps) {
        int goal = 10000;
        int safeSteps = Math.max(0, Math.min(goal, steps));
        ArrayList<BarEntry> yValues = new ArrayList<>();
        yValues.add(new BarEntry(0, new float[]{safeSteps, goal - safeSteps}));

        BarDataSet dataSet = new BarDataSet(yValues, "걸음 수");
        dataSet.setColors(Color.parseColor("#8F6C44"), Color.parseColor("#E5D6C2"));
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.parseColor("#2A2117"));

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.75f);

        barChart.setData(data);
        barChart.invalidate();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "사진 선택"), REQ_PICK_CHAT_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_CHAT_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            sendPhotoMessage(data.getData());
        }
    }

    private void sendPhotoMessage(Uri imageUri) {
        String now = new SimpleDateFormat("yyyyMMddHHmmssSS", Locale.getDefault()).format(new Date());
        StorageReference imageRef = chatImageStorage.child(roomid + "/" + userData.userid + "_" + now + ".jpg");
        imageRef.putFile(imageUri).addOnSuccessListener(taskSnapshot ->
                imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    ChatRoom.Comment tmpcomment = new ChatRoom.Comment();
                    tmpcomment.userid = userData.userid;
                    tmpcomment.msg = "photo:" + downloadUri;
                    tmpcomment.time = now;
                    dr.child("/" + roomid + "/comments/").push().setValue(tmpcomment);
                }));
    }

    public void settitle(String roomid) {
        updateTitleUi();
    }

    private void loadRoomMeta(){
        roomMetaListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ChatRoom room=snapshot.getValue(ChatRoom.class);
                if(room==null){
                    roomOwnerId="";
                    customRoomTitle="";
                    roomUserState.clear();
                    updateTitleUi();
                    updateRenameButtonState();
                    return;
                }
                roomOwnerId=room.ownerId==null?"":room.ownerId;
                customRoomTitle=room.customTitle==null?"":room.customTitle.trim();
                roomUserState.clear();
                if(room.userids!=null){
                    roomUserState.putAll(room.userids);
                }
                if(room.userids!=null){
                    for(String uid:room.userids.keySet()){
                        if(Boolean.TRUE.equals(room.userids.get(uid))&&!uid.equals(userData.userid)&&!users.contains(uid)){
                            users.add(uid);
                        }
                    }
                }
                if(roomOwnerId.equals("")){
                    ensureOwnerAssigned(room);
                }
                updateTitleUi();
                updateRenameButtonState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        dr.child(roomid).addValueEventListener(roomMetaListener);
    }

    private void ensureOwnerAssigned(ChatRoom room){
        String docId=roomid;
        if(roomid.contains("@")){
            docId=roomid.split("@")[0];
        }
        String finalDocId = docId;
        db.collection("matedata").document(finalDocId).get().addOnCompleteListener(task -> {
            if(task.isSuccessful()&&task.getResult()!=null&&task.getResult().exists()){
                String owner=task.getResult().getString("userid");
                if(owner!=null&&!owner.trim().equals("")){
                    dr.child(roomid).child("ownerId").setValue(owner.trim());
                    return;
                }
            }
            db.collection("walkdata").document(finalDocId).get().addOnCompleteListener(task2 -> {
                if(task2.isSuccessful()&&task2.getResult()!=null&&task2.getResult().exists()){
                    String owner2=task2.getResult().getString("userid");
                    if(owner2!=null&&!owner2.trim().equals("")){
                        dr.child(roomid).child("ownerId").setValue(owner2.trim());
                        return;
                    }
                }
                if(room.userids!=null&&room.userids.containsKey(userData.userid)){
                    dr.child(roomid).child("ownerId").setValue(userData.userid);
                }
            });
        });
    }

    private void updateTitleUi(){
        if(customRoomTitle!=null&&!customRoomTitle.equals("")){
            roomtitle.setText(customRoomTitle);
            return;
        }
        if(roomTitleOverride!=null && !roomTitleOverride.trim().equals("")){
            roomtitle.setText(roomTitleOverride.trim());
            return;
        }
        roomtitle.setText(buildDefaultRoomTitle());
    }

    private String buildDefaultRoomTitle(){
        ArrayList<String> names=new ArrayList<>();
        for(String uid:users){
            if(uid==null||uid.equals(userData.userid)){
                continue;
            }
            if(roomUserState.containsKey(uid)&&!Boolean.TRUE.equals(roomUserState.get(uid))){
                continue;
            }
            String name=usernames.get(uid);
            if(name!=null&&!name.trim().equals("")){
                names.add(name.trim());
            }
        }
        names.sort(String::compareTo);
        if(names.size()==0){
            return "채팅방";
        }
        return String.join(", ", names);
    }

    private void updateRenameButtonState(){
        boolean isOwner=roomOwnerId!=null&&!roomOwnerId.equals("")&&roomOwnerId.equals(userData.userid);
        renameRoomBtn.setEnabled(isOwner);
        renameRoomBtn.setAlpha(isOwner?1.0f:0.4f);
        renameRoomBtn.setText(isOwner?"채팅방 이름 변경":"방장만 변경 가능");
    }

    private void showRenameDialog(){
        boolean isOwner=roomOwnerId!=null&&!roomOwnerId.equals("")&&roomOwnerId.equals(userData.userid);
        if(!isOwner){
            return;
        }
        final Dialog dialog=new Dialog(this);
        dialog.setContentView(R.layout.dialog_rename_chatroom);
        if(dialog.getWindow()!=null){
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        EditText input=dialog.findViewById(R.id.rename_input);
        Button defaultBtn=dialog.findViewById(R.id.rename_default);
        Button cancelBtn=dialog.findViewById(R.id.rename_cancel);
        Button saveBtn=dialog.findViewById(R.id.rename_save);

        input.setText(customRoomTitle!=null&&!customRoomTitle.equals("")?customRoomTitle:buildDefaultRoomTitle());
        input.setSelectAllOnFocus(true);

        defaultBtn.setOnClickListener(v -> {
            dr.child(roomid).child("customTitle").setValue("");
            dialog.dismiss();
        });
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        saveBtn.setOnClickListener(v -> {
            String next=input.getText().toString().trim();
            dr.child(roomid).child("customTitle").setValue(next);
            dialog.dismiss();
        });
        dialog.show();
    }

    public ArrayList<ChatRoom.Comment> setChattingroom(String roomid) {
        String folder = getFilesDir().getAbsolutePath() + "/messages/";
        String result = "";
        String[] results;
        ArrayList<ChatRoom.Comment> resultc = new ArrayList<>();

        File check;
        try {
            check = new File(folder);

            if (!check.isDirectory()) {
                return resultc;
            }
            String dir = folder + roomid + "message.txt";
            File file = new File(dir);
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            fis.close();
            result = new String(buffer);
            results = result.split("\n");
            for (int i = 0; i < results.length; i += 3) {
                ChatRoom.Comment tmpc = new ChatRoom.Comment();
                tmpc.userid = results[i];
                tmpc.msg = results[i + 1];
                tmpc.time = results[i + 2];
                resultc.add(tmpc);
                Log.d("채팅 불러오기", tmpc.msg);
            }
            if (resultc.size() != 0) {
                starttime = resultc.get(resultc.size() - 1).time;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultc;
    }

    public void sendmsgs() {
        String tmpmsg = msg.getText().toString();
        if (tmpmsg.equals("")) {
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSS", Locale.getDefault());
        Date date = new Date(System.currentTimeMillis());
        String now = sdf.format(date);

        ChatRoom.Comment tmpcomment = new ChatRoom.Comment();
        tmpcomment.userid = userData.userid;

        if (tmpmsg.startsWith("emoji:")) {
            tmpcomment.msg = tmpmsg;
        } else {
            tmpcomment.msg = msg.getText().toString();
        }

        tmpcomment.time = now;
        Log.d("채팅 전송", tmpcomment.msg);

        Log.d("채팅 룸아이디", roomid + "");
        dr.child("/" + roomid + "/comments/").push().setValue(tmpcomment);
        msg.getText().clear();
    }

    public void savemsg(String msg) {
        String folder = getFilesDir().getAbsolutePath() + "/messages/";
        String filename = roomid + "message.txt";
        File file_path;
        try {
            file_path = new File(folder);
            if (!file_path.isDirectory()) {
                file_path.mkdirs();
                Log.d("채팅 데이터 저장", "경로 생성");
            }
            FileWriter fileWriter = new FileWriter(folder + filename, true);
            fileWriter.write(msg + "\n");
            Log.d("채팅 저장", msg);
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkuser() {
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("채팅방", "새로운 참가자 변동");
                Map<String, Object> tmpusers = new HashMap<>();
                tmpusers = (Map<String, Object>) snapshot.getValue();
                for (String s : tmpusers.keySet()) {
                    if (((Boolean) tmpusers.get(s)) && (!users.contains(s))) {
                        users.add(s);
                    }
                }
                loaduserprofile();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        dr.child(roomid).child("userids").addValueEventListener(valueEventListener);
    }

    public void loadlocalprofile() {
        for (String userid : users) {
            String path = getFilesDir().getAbsolutePath() + "/messages/" + roomid + "@" + userid + ".jpg";
            Bitmap bitmap = null;
            bitmap = BitmapFactory.decodeFile(path);
            if (bitmap != null) {
                userimgs.put(userid, bitmap);
            }
        }
    }

    public void savelocalprofile(Bitmap bitmap, String userid) {
        if (bitmap == null) {
            return;
        }

        String path = getFilesDir().getAbsolutePath() + "/messages/";
        File storage = new File(path);
        String filename = roomid + "@" + userid + ".jpg";
        File tempFile = new File(storage, filename);
        try {

            tempFile.createNewFile();
            FileOutputStream out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loaduserprofile() {
        for (String s : users) {
            user.document(s).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        String urlstr = (String) task.getResult().get("profileImagesmall");
                        if (usernames.get(s) == null) {
                            usernames.put(s, (String) task.getResult().get("appname"));
                        }
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    URL imgUrl = new URL(urlstr);
                                    HttpURLConnection connection = (HttpURLConnection) imgUrl.openConnection();
                                    connection.setDoInput(true);
                                    connection.connect();
                                    InputStream is = connection.getInputStream();
                                    Bitmap retBitmap = BitmapFactory.decodeStream(is);

                                    if (userimgs.get(s) == null || userimgs.get(s) != retBitmap) {
                                        userimgs.put(s, retBitmap);
                                    }

                                    savelocalprofile(retBitmap, s);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            msgAdapter.notifyDataSetChanged();
                                            userAdapter.notifyDataSetChanged();
                                            updateTitleUi();
                                        }
                                    });

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
                }
            });
        }
    }

    public String gettime(String time) {
        String y = time.substring(0, 4);
        String m = time.substring(4, 6);
        String d = time.substring(6, 8);
        String h = time.substring(8, 10);
        String min = time.substring(10, 12);
        return String.format("%s/%s/%s %s:%s", y, m, d, h, min);
    }

    public class MsgAdapter extends BaseAdapter {

        LayoutInflater layoutInflater;
        Context context;
        ArrayList<ChatRoom.Comment> comments = new ArrayList<>();
        String roomid;
        String userid;

        public MsgAdapter(Context context, String roomid, String userid) {
            this.context = context;
            layoutInflater = LayoutInflater.from(context);
            this.roomid = roomid;
            this.userid = userid;
            comments = setChattingroom(roomid);
            getrecentmsg();
        }

        @Override
        public int getCount() {
            return comments.size();
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
            TextView msgitem, timeitem, username;
            ImageView emojiImage = null;  // 초기화
            CircleImageView userimg = null;

            if (comments.get(position).userid.equals(userid)) {
                view = layoutInflater.inflate(R.layout.item_chat_message_mine, null);
                msgitem = view.findViewById(R.id.msg_mine);
                timeitem = view.findViewById(R.id.time_mine);
                emojiImage = view.findViewById(R.id.emoji_image_mine);
            } else {
                if (position == 0 || (!comments.get(position).userid.equals(comments.get(position - 1).userid))) {
                    view = layoutInflater.inflate(R.layout.item_chat_message_other, null);
                    msgitem = view.findViewById(R.id.msg_others);
                    timeitem = view.findViewById(R.id.time_others);
                    username = view.findViewById(R.id.name_others);
                    emojiImage = view.findViewById(R.id.emoji_image_others);
                    userimg = view.findViewById(R.id.userimg_others);

                    username.setText(usernames.get(comments.get(position).userid));
                    if (userimgs.get(comments.get(position).userid) != null) {
                        userimg.setImageBitmap(userimgs.get(comments.get(position).userid));
                    }
                } else {
                    view = layoutInflater.inflate(R.layout.item_chat_message_other_no_profile, null);
                    msgitem = view.findViewById(R.id.msg_othersno);
                    timeitem = view.findViewById(R.id.time_othersno);
                    emojiImage = view.findViewById(R.id.emoji_image_others);
                }
            }

            if (comments.size() == 0) {
                return layoutInflater.inflate(R.layout.item_common_empty, null);
            }

            String message = comments.get(position).msg == null ? "" : comments.get(position).msg;
            String photoUrl = extractPhotoUrl(message);
            if (photoUrl != null) {
                if (emojiImage != null) {
                    ViewGroup.LayoutParams lp = emojiImage.getLayoutParams();
                    lp.width = (int) (180 * getResources().getDisplayMetrics().density);
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    emojiImage.setLayoutParams(lp);
                    emojiImage.setAdjustViewBounds(true);
                    emojiImage.setVisibility(View.VISIBLE);
                    msgitem.setVisibility(View.GONE);
                    loadChatImage(photoUrl, emojiImage);
                }
            } else if (message.startsWith("emoji:")) {
                int emojiResId = Integer.parseInt(message.substring(6));
                if (emojiImage != null) {
                    ViewGroup.LayoutParams lp = emojiImage.getLayoutParams();
                    lp.width = (int) (40 * getResources().getDisplayMetrics().density);
                    lp.height = (int) (40 * getResources().getDisplayMetrics().density);
                    emojiImage.setLayoutParams(lp);
                    emojiImage.setAdjustViewBounds(false);
                    emojiImage.setImageResource(emojiResId);
                    emojiImage.setVisibility(View.VISIBLE);
                    msgitem.setVisibility(View.GONE);
                }
            } else {
                if (emojiImage != null) emojiImage.setVisibility(View.GONE);
                msgitem.setVisibility(View.VISIBLE);
                msgitem.setText(message);
            }

            timeitem.setText(gettime(comments.get(position).time));
            if (userimg != null) {

                userimg.setOnClickListener(new View.OnClickListener() {

                    @Override

                    public void onClick(View view) {

                        Intent intent = new Intent(ChatActivity.this, UserProfileActivity.class);

                        intent.putExtra("userid", comments.get(position).userid);

                        startActivity(intent);

                    }

                });

            }
            return view;
        }


        public void getrecentmsg() {
            childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    ChatRoom.Comment tmpcomment = snapshot.getValue(ChatRoom.Comment.class);
                    comments.add(tmpcomment);
                    savemsg(tmpcomment.userid);
                    savemsg(tmpcomment.msg);
                    savemsg(tmpcomment.time);
                    notifyDataSetChanged();
                    if (tmpcomment.userid.equals(userData.userid)) {
                        msglist.setSelection(msgAdapter.getCount() - 1);
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            dr.child(roomid).child("comments").orderByChild("time").startAfter(starttime).addChildEventListener(childEventListener);
        }
    }

    private void loadChatImage(String photoUrl, ImageView target) {
        Bitmap cached = chatImageCache.get(photoUrl);
        if (cached != null) {
            target.setImageBitmap(cached);
            return;
        }
        new Thread(() -> {
            try {
                URL imgUrl = new URL(photoUrl);
                HttpURLConnection connection = (HttpURLConnection) imgUrl.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream is = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                if (bitmap != null) {
                    chatImageCache.put(photoUrl, bitmap);
                    runOnUiThread(() -> target.setImageBitmap(bitmap));
                }
                connection.disconnect();
            } catch (Exception ignored) {
            }
        }).start();
    }

    private String extractPhotoUrl(String message) {
        if (message == null) {
            return null;
        }
        if (message.startsWith("photo:")) {
            return message.substring(6);
        }
        // Backward compatibility: render plain storage URL messages as images too.
        if ((message.startsWith("http://") || message.startsWith("https://"))
                && (message.contains("firebasestorage") || message.contains("chat_images"))) {
            return message;
        }
        return null;
    }

    public class UserAdapter extends BaseAdapter {

        LayoutInflater layoutInflater;
        Context context;

        public UserAdapter(Context context) {
            this.context = context;
            layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return users.size();
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
            View view = layoutInflater.inflate(R.layout.item_chat_user, null);
            View emptyview = layoutInflater.inflate(R.layout.view_empty, null);
            if (usernames.size() == 0) {
                return emptyview;
            }
            String userid = users.get(position);
            CircleImageView circleImageView = view.findViewById(R.id.userimg_chatlist);
            TextView usernametxt = view.findViewById(R.id.name_chatlist);
            LinearLayout body = view.findViewById(R.id.chatuserlist_body);
            body.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(ChatActivity.this, UserProfileActivity.class);
                    intent.putExtra("userid", userid);
                    startActivity(intent);
                }
            });
            circleImageView.setImageBitmap(userimgs.get(userid));
            usernametxt.setText(usernames.get(userid));

            Log.d("유저리스트 콜", usernames.get(userid) + "");

            return view;
        }
    }

    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

    }

    @Override
    public void onDrawerOpened(@NonNull View drawerView) {
        userAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDrawerClosed(@NonNull View drawerView) {

    }

    @Override
    public void onDrawerStateChanged(int newState) {

    }

    @Override
    protected void onStop() {
        super.onStop();
        dr.child(roomid).child("comments").orderByChild("time").startAfter(starttime).removeEventListener(childEventListener);
        dr.child(roomid).child("userids").removeEventListener(valueEventListener);
        if(roomMetaListener!=null){
            dr.child(roomid).removeEventListener(roomMetaListener);
        }
    }
}
