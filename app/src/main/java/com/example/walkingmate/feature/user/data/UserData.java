package com.example.walkingmate.feature.user.data;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.firestore.auth.User;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;


//로컬 저장소내 유저 데이터 관리

public class UserData {
    public String userid;
    public String profileImagebig;
    public String profileImagesmall;
    public String appname;

    public String nickname;
    public String name;
    public String age;
    public String gender;
    public String birthyear;

    //타이틀은 없으면 "없음"으로
    public String title;
    public double reliability;

    public String password;

    public UserData(String userid, String profileImagebig,String profileImagesmall, String appname, String nickname, String name, String age, String gender, String birthyear, String title, double reliability) {
        this.userid = userid;
        this.profileImagebig = profileImagebig;
        this.profileImagesmall=profileImagesmall;
        this.appname = appname;
        this.nickname = nickname;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.birthyear = birthyear;
        this.title=title;
        this.reliability=reliability;
    }

    public UserData(String userid, String profileImagebig,String profileImagesmall, String appname, String nickname, String name, String age, String gender, String birthyear, String title, double reliability, String password) {
        this.userid = userid;
        this.profileImagebig = profileImagebig;
        this.profileImagesmall=profileImagesmall;
        this.appname = appname;
        this.nickname = nickname;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.birthyear = birthyear;
        this.title=title;
        this.reliability=reliability;
        this.password=password;
    }

    public String getUserid() { return userid; }
    public void setUserid(String userid) { this.userid = userid; }
    public String getProfileImagebig() { return profileImagebig; }
    public void setProfileImagebig(String profileImagebig) { this.profileImagebig = profileImagebig; }
    public String getProfileImagesmall() { return profileImagesmall; }
    public void setProfileImagesmall(String profileImagesmall) { this.profileImagesmall = profileImagesmall; }
    public String getAppname() { return appname; }
    public void setAppname(String appname) { this.appname = appname; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getBirthyear() { return birthyear; }
    public void setBirthyear(String birthyear) { this.birthyear = birthyear; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public double getReliability() { return reliability; }
    public void setReliability(double reliability) { this.reliability = reliability; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public static HashMap<String,Object> getHashmap(UserData userData){
        HashMap<String, Object> user=new HashMap<>();
        user.put("nickname",userData.nickname);
        user.put("name",userData.name);
        user.put("age",userData.age);
        user.put("gender",userData.gender);
        user.put("birthyear",userData.birthyear);
        user.put("profileImagebig",userData.profileImagebig);
        user.put("profileImagesmall",userData.profileImagesmall);
        user.put("appname",userData.appname);
        user.put("title", userData.title);
        user.put("reliability",userData.reliability);

        if (userData.password != null) {
            user.put("password", userData.password);
        }
        return user;
    }

    public static String scanUserData(Activity activity){
        String path=activity.getFilesDir().getAbsolutePath()+ "/userData/";
        File f;
        File[] files;
        String result=null;
        try{
            f=new File(path);

            //디렉토리가 없으면 false반환
            if(!f.isDirectory()){
                return null;
            }
            files = f.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().toLowerCase(Locale.US).endsWith(".txt");
                }
            });
            //파일이 없으면 false반환
            if(files.length == 0){
                return null;
            }
            result=files[0].getName();

        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }


    //유저 정보 로컬에 저장
    //이미지파일 따로, 유저 데이터 따로.
    public static void saveData(UserData userData, Activity activity){
        String datastr=UserData.encode(userData);

        String folder= activity.getFilesDir().getAbsolutePath() + "/userData/";
        String filename=userData.userid+".txt";

        Log.d("save data",filename);

        File file_path;
        try{
            file_path=new File(folder);
            if(!file_path.isDirectory()){
                file_path.mkdirs();
                Log.d("유저 데이터 저장","경로 생성");
            }
            File file=new File(folder+filename);
            FileOutputStream out=new FileOutputStream(folder+filename,false);
            out.write(datastr.getBytes());
            out.flush();
            out.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    //이미지 파일, 유저데이터 반환
    public static UserData loadData(Activity activity) {
        UserData resultData = null;


        String folder = activity.getFilesDir().getAbsolutePath() + "/userData/";
        String result = "";

        //파일이 없으면 null반환
        String fileName = scanUserData(activity);
        if (fileName == null) {
            return null;
        }

        File check;
        try {
            check = new File(folder);

            //경로가 없는경우 그냥 null반환하고 끝
            if (!check.isDirectory()) {
                return null;
            }
            String dir = folder + fileName;
            File file = new File(dir);
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            fis.close();
            result = new String(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        resultData = UserData.decode(result);

        return resultData;
    }

    public static String encode(UserData userData) {
        StringBuilder sb = new StringBuilder();
        sb.append(userData.userid).append("@")
                .append(userData.profileImagebig).append("@")
                .append(userData.profileImagesmall).append("@")
                .append(userData.appname).append("@")
                .append(userData.nickname).append("@")
                .append(userData.name).append("@")
                .append(userData.age).append("@")
                .append(userData.gender).append("@")
                .append(userData.birthyear).append("@")
                .append(userData.title).append("@")
                .append(userData.reliability);

        // 비밀번호가 null이 아닐 때만 추가
        if (userData.password != null) {
            sb.append("@").append(userData.password);
        }

        return sb.toString();
    }


    public static UserData decode(String userDataString) {
        String[] result = userDataString.split("@");

        // password 필드가 있는지 확인
        if (result.length == 12) {
            return new UserData(result[0], result[1], result[2], result[3], result[4], result[5], result[6], result[7], result[8], result[9], Double.parseDouble(result[10]), result[11]);
        } else {
            return new UserData(result[0], result[1], result[2], result[3], result[4], result[5], result[6], result[7], result[8], result[9], Double.parseDouble(result[10]));
        }
    }

    //thread생성해서 사용하기
    public static Bitmap GetBitmapfromURL(String urlstr){
        HttpURLConnection connection = null;
        InputStream is = null;
        Bitmap retBitmap = null;
        try {
            URL imgUrl = new URL(urlstr);
            connection = (HttpURLConnection) imgUrl.openConnection();
            connection.setDoInput(true); //url로 input받는 flag 허용
            connection.connect(); //연결
            is = connection.getInputStream(); // get inputstream
            retBitmap = BitmapFactory.decodeStream(is);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        Log.d("이미지","로드완료");
        return retBitmap;
    }

    public static void saveBitmapToJpeg(Bitmap bitmapbig,Bitmap bitmapsmall, Activity activity) {
        if(bitmapbig==null||bitmapsmall==null){
            return;
        }

        String path=activity.getFilesDir().getAbsolutePath()+ "/userData/";
        File storage = new File(path);
        String fileNamebig ="profilebig.jpg";
        String fileNamesmall ="profilesmall.jpg";
        File tempFilebig = new File(storage, fileNamebig);
        File tempFilesmall = new File(storage, fileNamesmall);
        try {

            tempFilebig.createNewFile();
            tempFilesmall.createNewFile();
            FileOutputStream outbig = new FileOutputStream(tempFilebig);
            FileOutputStream outsmall = new FileOutputStream(tempFilesmall);
            bitmapbig.compress(Bitmap.CompressFormat.JPEG, 100, outbig);
            bitmapsmall.compress(Bitmap.CompressFormat.JPEG, 100, outsmall);
            outbig.close();
            outsmall.close();

        } catch (FileNotFoundException e) {

        } catch (IOException e) {

        }
    }

    public static Bitmap loadImageToBitmap(Activity activity){
        String path=activity.getFilesDir().getAbsolutePath()+ "/userData/profilebig.jpg";
        Bitmap bitmap=null;
        bitmap=BitmapFactory.decodeFile(path);
        return bitmap;
    }

    public static Bitmap loadSmallImageToBitmap(Activity activity){
        String path=activity.getFilesDir().getAbsolutePath()+ "/userData/profilesmall.jpg";
        Bitmap bitmap=null;
        bitmap=BitmapFactory.decodeFile(path);
        return bitmap;
    }


    //type:true-큰 프로필 이미지, false-작은 프로필 이미지지
    public static Bitmap getResizedImage(Bitmap bitmap, boolean type){

        Bitmap result=null;

        if(bitmap != null) {
            FileOutputStream fout = null;
            try {
                int MAX_IMAGE_SIZE;// max final file size
                if(type){
                    MAX_IMAGE_SIZE=500*500;
                }
                else{
                    MAX_IMAGE_SIZE=150*150;
                }
                int compressQuality = 100; // quality decreasing by 5 every loop. (start from 99)
                int streamLength = MAX_IMAGE_SIZE;

                ByteArrayOutputStream baos=new ByteArrayOutputStream();
                byte[] bytes=null;
                while(streamLength >= MAX_IMAGE_SIZE){
                    baos=new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, baos);
                    bytes=baos.toByteArray();
                    streamLength = (int)bytes.length;
                    compressQuality -= 5;
                    if(compressQuality==0){
                        break;
                    }
                }
                result=BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                baos.flush();
                baos.close();


            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        Log.d("이미지리사이즈",(result==null)+"");
        return result;
    }


    public static Double setdouble(Object num){
        if(num.getClass().getName().equals("java.lang.Long")){
            Double result=Double.parseDouble(num+"");
            return result;
        }
        else{
            return (Double) num;
        }
    }
}
