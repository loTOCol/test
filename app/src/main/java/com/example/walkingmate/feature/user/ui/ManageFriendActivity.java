package com.example.walkingmate.feature.user.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.example.walkingmate.R;
import com.example.walkingmate.feature.user.data.UserData;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ManageFriendActivity extends AppCompatActivity {

    FirebaseFirestore fb = FirebaseFirestore.getInstance();
    CollectionReference blocklist = fb.collection("blocklist");
    CollectionReference users = fb.collection("users");

    ImageButton back;
    ListView blocklistview;

    ArrayList<String> blockusers = new ArrayList<>();
    ArrayList<String> blockusersProfile = new ArrayList<>();
    HashMap<String, Bitmap> userimg = new HashMap<>();
    UserData userData;

    BlockAdapter blockAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_friend);

        userData = UserData.loadData(this);

        back = findViewById(R.id.back_managefreind);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        blocklistview = findViewById(R.id.blocklistview);
        blockAdapter = new BlockAdapter(this);
        blocklistview.setAdapter(blockAdapter);

        getlist();
    }

    public void getlist() {
        blocklist.document(userData.userid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.getResult().exists()) {
                    ArrayList<String> blockuserstmp = (ArrayList<String>) task.getResult().get("userid");
                    if (blockuserstmp == null || blockuserstmp.size() == 0) {
                        return;
                    }

                    findViewById(R.id.loading_block).setVisibility(View.VISIBLE);
                    for (String s : blockuserstmp) {
                        users.document(s).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.getResult().exists()) {
                                    blockusers.add(s);
                                    DocumentSnapshot user = task.getResult();
                                    String gender = "M".equals(user.get("gender")) ? "남성" : "여성";
                                    String userprofile = String.format("%s (%s/%s)", user.get("appname"), gender, user.get("age"));
                                    blockusersProfile.add(userprofile);

                                    String urlstr = (String) user.get("profileImagesmall");
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            HttpURLConnection connection = null;
                                            try {
                                                URL imgUrl = new URL(urlstr);
                                                connection = (HttpURLConnection) imgUrl.openConnection();
                                                connection.setDoInput(true);
                                                connection.connect();
                                                InputStream is = connection.getInputStream();
                                                Bitmap retBitmap = BitmapFactory.decodeStream(is);
                                                userimg.put(s, retBitmap);
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        if (userimg.size() == blockuserstmp.size()) {
                                                            findViewById(R.id.loading_block).setVisibility(View.INVISIBLE);
                                                        }
                                                        blockAdapter.notifyDataSetChanged();
                                                    }
                                                });
                                            } catch (Exception ignored) {
                                            } finally {
                                                if (connection != null) {
                                                    connection.disconnect();
                                                }
                                            }
                                        }
                                    }).start();
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    public void removeblock() {
        HashMap<String, Object> fixedlist = new HashMap<>();
        fixedlist.put("userid", blockusers);
        blocklist.document(userData.userid).set(fixedlist);
    }

    public class BlockAdapter extends BaseAdapter {

        LayoutInflater layoutInflater;

        public BlockAdapter(Context context) {
            layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return blockusers.size();
        }

        @Override
        public Object getItem(int i) {
            return blockusers.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = layoutInflater.inflate(R.layout.item_user_blocked, parent, false);
            }

            Button cancel = view.findViewById(R.id.cancel_block);
            TextView profile = view.findViewById(R.id.user_block);
            CircleImageView profileimage = view.findViewById(R.id.profileImage_blockuser);

            profile.setText(blockusersProfile.get(position));
            Bitmap img = userimg.get(blockusers.get(position));
            if (img != null) {
                profileimage.setImageBitmap(img);
            } else {
                profileimage.setImageResource(R.drawable.blank_profile);
            }

            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    userimg.remove(blockusers.get(position));
                    blockusers.remove(position);
                    blockusersProfile.remove(position);
                    notifyDataSetChanged();
                    removeblock();
                }
            });

            return view;
        }
    }

    @Override
    public void onBackPressed() {
        back.performClick();
    }
}