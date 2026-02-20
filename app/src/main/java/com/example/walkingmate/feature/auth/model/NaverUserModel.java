package com.example.walkingmate.feature.auth.model;

import java.io.Serializable;

public class NaverUserModel implements Serializable {
    private String id;
    private String nickname;
    private String name;
    private String age;
    private String gender;
    private String birthyear;

    public NaverUserModel(String id, String nickname, String name, String age, String gender, String birthyear) {
        this.id = id;
        this.nickname = nickname;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.birthyear = birthyear;
    }
    public String getId(){ return id;}

    public void setId(String id){ this.id = id; }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAge(){
        return age;
    }

    public void setAge(String age){
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthyear() {
        return birthyear;
    }

    public void setBirthyear(String birthyear) {
        this.birthyear = birthyear;
    }
}
