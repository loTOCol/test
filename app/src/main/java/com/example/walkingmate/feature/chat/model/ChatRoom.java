package com.example.walkingmate.feature.chat.model;

import androidx.annotation.Keep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//파이어 베이스에서 사용하려면 모든지 무조건 public으로 해야함


//roomid:
// 1. 수락전 일대일 채팅:연동 게시물 이름+상대 이름+@+로 연결
// 2. 수락 후 단채 채팅: 연동게시물 이름
//roomname
// 1. 수락 전: [산책/메이트][개인]
// 2. 수락 후: [산책/메이트][수락]

public class ChatRoom {
    public String roomid;
    public String roomname;
    public String ownerId;
    public String customTitle;
    public Map<String,Boolean> userids=new HashMap<String, Boolean>();
    public Map<String,Comment> comments=new HashMap<>();

    public ChatRoom(){
    }

    public ChatRoom(String roomid, String roomname, Map<String, Boolean> userids) {
        this.roomid = roomid;
        this.roomname = roomname;
        this.userids = userids;
        this.ownerId = "";
        this.customTitle = "";
    }

    public static class Comment{
        public String msg;
        public String time;
        public String userid;

        public Comment(){}
    }

}
