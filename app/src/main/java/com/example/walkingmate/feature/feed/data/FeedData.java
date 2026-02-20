package com.example.walkingmate.feature.feed.data;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.overlay.Align;
import com.naver.maps.map.overlay.Marker;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

//경로 추적을 통해 저장된 데이터
//피드를 올리기 전에 앱 내부저장소에 보관함
public class FeedData extends Activity {
    public ArrayList<LatLng> coordList; //경로추적 좌표
    public ArrayList<Marker> markerList; //마커, 좌표와 캡션(메모)가 포함됨
    public String[] timecheck;//시작, 종료시간. [0]이 시작, [1]이 종료
    public int step; //발걸음 수
    public float displacement; //걸은 거리

    public FeedData(ArrayList<LatLng> coordList, ArrayList<Marker> markerList, String[] timecheck,int step, float displacement){
        this.coordList=coordList;
        this.markerList=markerList;
        this.timecheck=timecheck;
        this.step=step;
        this.displacement=displacement;
    }

    public FeedData(){
        this.coordList=null;
        this.markerList=null;
        this.timecheck=null;
        this.step=0;
        this.displacement=0;
    }

    public ArrayList<LatLng> getCoordList() { return coordList; }
    public void setCoordList(ArrayList<LatLng> coordList) { this.coordList = coordList; }
    public ArrayList<Marker> getMarkerList() { return markerList; }
    public void setMarkerList(ArrayList<Marker> markerList) { this.markerList = markerList; }
    public String[] getTimecheck() { return timecheck; }
    public void setTimecheck(String[] timecheck) { this.timecheck = timecheck; }
    public int getStep() { return step; }
    public void setStep(int step) { this.step = step; }
    public float getDisplacement() { return displacement; }
    public void setDisplacement(float displacement) { this.displacement = displacement; }


    public static String encodeFeed(FeedData data){
        int coordSize=data.coordList.size();
        int markerSize=data.markerList.size();

        Log.d("Encode data","시작");

        String feedString=new String();

        //경로추적좌표수0, 마커수1, 시간(시작2, 종료3), 걸음수4, 걸은 거리5 순
        feedString+=coordSize+" "+markerSize+" "+data.timecheck[0]+" "+data.timecheck[1]+" "+data.step+" "+data.displacement+" ";

        //경로추적 좌표저장 - lat, lon 순으로
        for(int i=0; i<coordSize; ++i){
            feedString+=data.coordList.get(i).latitude+" "+data.coordList.get(i).longitude+" ";
        }

        //마커 데이터 저장 - lat, lon, caption순으로
        //캡션내용이 없으면 다른 택스트로 임시 대치시킴
        if(markerSize>0){
            for(int i=0; i<markerSize; ++i){
                String tmp=data.markerList.get(i).getCaptionText();
                if(tmp.equals("")){
                    tmp="nonetextMark";
                }
                feedString+=data.markerList.get(i).getPosition().latitude+" "
                        +data.markerList.get(i).getPosition().longitude+" "+tmp+" ";
            }
        }
        Log.d("Encode data",feedString);

        return feedString;
    }

    public static FeedData decodeFeed(String data){
        FeedData result;
        String[] arr=data.split(" ");

        int coordsize, markersize;

        ArrayList<LatLng> tmpcoord=new ArrayList<>();
        ArrayList<Marker> tmpmarker=new ArrayList<>();
        String[] tmptime=new String[2];
        int tmpstep;
        float tmpdisplacement;

        coordsize=Integer.parseInt(arr[0]);
        markersize=Integer.parseInt(arr[1]);
        tmptime[0]=arr[2]; tmptime[1]=arr[3];
        tmpstep=Integer.parseInt(arr[4]);
        tmpdisplacement=Float.parseFloat(arr[5]);

        for(int i=6; i<6+(2*coordsize); i+=2){
            tmpcoord.add(new LatLng(Double.parseDouble(arr[i]),Double.parseDouble(arr[i+1])));
        }

        if(markersize>0){
            for(int i=6+(coordsize*2); i<6+(coordsize*2)+(markersize*3); i+=3){
                Marker tmp=new Marker();

                String tmpstr=arr[i+2];
                if(tmpstr.equals("nonetextMark")){
                    tmpstr="";
                }

                tmp.setPosition(new LatLng(Double.parseDouble(arr[i]),Double.parseDouble(arr[i+1])));
                tmp.setCaptionText(tmpstr);
                tmp.setCaptionAligns(Align.Top);

                tmpmarker.add(tmp);
            }
        }

        result=new FeedData(tmpcoord, tmpmarker, tmptime,tmpstep,tmpdisplacement);
        return result;
    }

    //null반환시 저장된 피드가 없는것
    public ArrayList<String> scanFeedList(Activity activity){
        String path=activity.getFilesDir().getAbsolutePath()+ "/walkingmate/";
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
                    return file.getName().toLowerCase(Locale.US).endsWith(".txt");
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

    public String savefeed(FeedData data, Activity activity){
        String folder= activity.getFilesDir().getAbsolutePath() + "/walkingmate/";
        String filename=data.timecheck[0]+".txt";

        Log.d("save data",filename);

        File file_path;
        try{
            file_path=new File(folder);
            if(!file_path.isDirectory()){
                file_path.mkdirs();
                Log.d("피드 데이터 저장","경로 생성");
            }
            File file=new File(folder+filename);
            if(file.exists()){
                filename="(최근)"+filename;
            }
            FileOutputStream out=new FileOutputStream(folder+filename,false);
            out.write(encodeFeed(data).getBytes());
            out.flush();
            out.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        return filename;
    }

    //여기서 fileName은 스캔 메소드로 구한 확장자명이 포함된 것
    public FeedData loadfeed(String fileName, Activity activity){
        String folder= activity.getFilesDir().getAbsolutePath() + "/walkingmate/";
        String result="";

        File check;
        try{
            check=new File(folder);

            //경로가 없는경우 그냥 null반환하고 끝
            if(!check.isDirectory()){
                return null;
            }
            String dir=folder+fileName;
            File file=new File(dir);
            FileInputStream fis=new FileInputStream(file);
            byte[] buffer=new byte[fis.available()];
            fis.read(buffer);
            fis.close();
            result=new String(buffer);
        }catch(Exception e){
            e.printStackTrace();
        }
        return decodeFeed(result);
    }

    static public void deletefeed(String fileName, Activity activity){
        String folder= activity.getFilesDir().getAbsolutePath() + "/walkingmate/";
        String path=folder+fileName;

        try{
            File file=new File(path);
            if(file.exists()){
                file.delete();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static Bitmap getResizedFeedImage(Bitmap bitmap){

        Bitmap result=null;

        if(bitmap != null) {
            FileOutputStream fout = null;
            try {
                int MAX_IMAGE_SIZE=500*500;// max final file size

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
                result= BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                baos.flush();
                baos.close();


            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return result;
    }

}
