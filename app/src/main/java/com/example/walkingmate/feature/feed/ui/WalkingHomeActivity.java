package com.example.walkingmate.feature.feed.ui;

import static android.graphics.Color.*;
import static androidx.annotation.Dimension.DP;
import static com.example.walkingmate.R.drawable.bottom_navigation;
import static com.example.walkingmate.R.drawable.selected_day;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.walkingmate.feature.user.ui.AppInfoActivity;
import com.example.walkingmate.feature.user.ui.EditUserProfileActivity;
import com.example.walkingmate.feature.user.ui.HelpInfoActivity;
import com.example.walkingmate.feature.user.ui.ManageFriendActivity;
import com.example.walkingmate.R;
import com.example.walkingmate.feature.schedule.ui.ScheduleActivity;
import com.example.walkingmate.feature.mate.ui.MateFragment;
import com.example.walkingmate.feature.user.data.UserData;
import com.example.walkingmate.feature.misc.ui.ChallengeActivity;
import com.example.walkingmate.feature.chat.ui.ChatFragment;
import com.example.walkingmate.feature.chat.ui.LastFragment;
import com.example.walkingmate.feature.feed.data.FeedData;
import com.example.walkingmate.feature.map.ui.MapActivity;
import com.example.walkingmate.feature.shop.ui.CoinShopActivity;
import com.example.walkingmate.feature.walk.ui.WalkFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.naver.maps.geometry.LatLng;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateLongClickListener;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;
import com.prolificinteractive.materialcalendarview.OnRangeSelectedListener;
import com.prolificinteractive.materialcalendarview.format.ArrayWeekDayFormatter;
import com.prolificinteractive.materialcalendarview.format.CalendarWeekDayFormatter;
import com.prolificinteractive.materialcalendarview.format.MonthArrayTitleFormatter;
import com.prolificinteractive.materialcalendarview.format.TitleFormatter;
import com.prolificinteractive.materialcalendarview.format.WeekDayFormatter;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import okhttp3.Challenge;

public class WalkingHomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    MaterialCalendarView calendarView;
    Button feedBtn;
    ImageButton plusBtn;
    private String TAG=this.getClass().getSimpleName();



    TextView yearText, titletxt, usertitle, username;
    TextView streakCurrentValue, streakMonthValue, streakBestValue;
    CalendarDay selectedDay;

    ImageView userimage;

    FeedData feedData;
    ArrayList<String> feedlist;

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    View headerview;

    LinearLayout mainlayout;

    boolean start=true;
    FrameLayout frameLayout;

    private FragmentManager fragmentManager;

    int selected=1;

    UserData userData;
    private boolean photoToggleFirst = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_calendar);

        fragmentManager=getSupportFragmentManager();

        LocationManager LocMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        titletxt=findViewById(R.id.fragtitle);

        userData=UserData.loadData(this);

        mainlayout=findViewById(R.id.mainLayout_calendar);
        frameLayout=findViewById(R.id.container);




        ChatFragment chatFragment = new ChatFragment();
        fragmentManager.beginTransaction().replace(R.id.Chatcontainer, chatFragment, "chat").commitAllowingStateLoss();


        WalkFragment walkFragment=new WalkFragment();
        fragmentManager.beginTransaction().replace(R.id.container, walkFragment,"walk").commitAllowingStateLoss();




        NavigationBarView navigationBarView=findViewById(R.id.bottom_navigation);
        View walkitem=navigationBarView.findViewById(R.id.walk);



        navigationBarView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {


            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                FragmentManager fragmentManager = getSupportFragmentManager();

                int itemId = item.getItemId();
                if (itemId == R.id.walk) {
                    selected = 1;
                    titletxt.setText("기린워킹 메인");
                    frameLayout.removeView(mainlayout);

                    if (fragmentManager.findFragmentByTag("walk") != null) {
                        fragmentManager.beginTransaction().show(fragmentManager.findFragmentByTag("walk")).commit();
                    } else {
                        fragmentManager.beginTransaction().add(R.id.container, new WalkFragment(), "walk").commit();
                    }
                    if (fragmentManager.findFragmentByTag("mate") != null) {
                        fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag("mate")).commit();
                    }
                    if (fragmentManager.findFragmentByTag("last") != null) {
                        fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag("last")).commit();
                    }
                    return true;
                } else if (itemId == R.id.mate) {
                    selected = 2;
                    titletxt.setText("기린워킹 게시판");
                    frameLayout.removeView(mainlayout);

                    if (fragmentManager.findFragmentByTag("walk") != null) {
                        fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag("walk")).commit();
                    }
                    if (fragmentManager.findFragmentByTag("mate") != null) {
                        fragmentManager.beginTransaction().show(fragmentManager.findFragmentByTag("mate")).commit();
                    } else {
                        fragmentManager.beginTransaction().add(R.id.container, new MateFragment(), "mate").commit();
                    }
                    if (fragmentManager.findFragmentByTag("last") != null) {
                        fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag("last")).commit();
                    }
                    return true;
                } else if (itemId == R.id.trace) {
                    selected = 3;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    walkitem.performClick();
                                }
                            });
                        }
                    }).start();
                    if (!LocMan.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        Toast.makeText(WalkingHomeActivity.this, "GPS가 꺼져있습니다.", Toast.LENGTH_LONG).show();
                        Intent gpsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(gpsIntent);
                    } else {
                        Intent GoMap = new Intent(WalkingHomeActivity.this, MapActivity.class);
                        startActivity(GoMap);
                    }
                    return true;
                } else if (itemId == R.id.feed) {
                    if (fragmentManager.findFragmentByTag("walk") != null) {
                        fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag("walk")).commit();
                    }
                    if (fragmentManager.findFragmentByTag("mate") != null) {
                        fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag("mate")).commit();
                    }
                    if (fragmentManager.findFragmentByTag("last") != null) {
                        fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag("last")).commit();
                    }

                    titletxt.setText("기린워킹 캘린더");
                    if (selected != 4) {
                        frameLayout.addView(mainlayout);
                    }
                    selected = 4;
                    return true;
                } else if (itemId == R.id.last) {
                    selected = 5;
                    titletxt.setText("기린워킹 채팅");
                    frameLayout.removeView(mainlayout);
                    if (fragmentManager.findFragmentByTag("walk") != null) {
                        fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag("walk")).commit();
                    }
                    if (fragmentManager.findFragmentByTag("mate") != null) {
                        fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag("mate")).commit();
                    }
                    if (fragmentManager.findFragmentByTag("last") != null) {
                        fragmentManager.beginTransaction().show(fragmentManager.findFragmentByTag("last")).commit();
                    } else {
                        fragmentManager.beginTransaction().add(R.id.container, new LastFragment(), "last").commit();
                    }
                    return true;
                }
                return true;
            }

        });

        drawerLayout=findViewById(R.id.Calendar_Layout);
        navigationView=findViewById(R.id.navigationView_calendar);
        headerview=navigationView.getHeaderView(0);

        usertitle=headerview.findViewById(R.id.title_sidebar);
        username=headerview.findViewById(R.id.username_sidebar);
        userimage=headerview.findViewById(R.id.userimage_sidebar);

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        navigationView.setNavigationItemSelectedListener(this);

        ImageButton menuBtn=findViewById(R.id.menu_calendar);
        menuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(navigationView);
            }
        });

        ImageButton photoToggleBtn = findViewById(R.id.menu_photo_toggle);
        if (photoToggleBtn != null) {
            photoToggleBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    photoToggleFirst = !photoToggleFirst;
                    photoToggleBtn.setImageResource(photoToggleFirst ? R.drawable.photo_toggle_1 : R.drawable.photo_toggle_2);
                }
            });
        }

        calendarView=findViewById(R.id.calendarView);
        yearText=findViewById(R.id.year);
        plusBtn=findViewById(R.id.plusbtn);
        feedBtn=findViewById(R.id.feedbtn);
        streakCurrentValue=findViewById(R.id.streak_current_value);
        streakMonthValue=findViewById(R.id.streak_month_value);
        streakBestValue=findViewById(R.id.streak_best_value);

        selectedDay=null;

        feedlist=new ArrayList<>();

        FeedData feedData=new FeedData();
        feedlist=feedData.scanFeedList(this);


        calendarView.setTitleFormatter(new MonthArrayTitleFormatter(getResources().getTextArray(R.array.custom_months)));
        calendarView.setWeekDayFormatter(new ArrayWeekDayFormatter(getResources().getTextArray(R.array.custom_weekdays)));
        calendarView.setHeaderTextAppearance(R.style.CalendarWidgetHeader);
        calendarView.setLeftArrow(R.drawable.ic_calendar_arrow_left);
        calendarView.setRightArrow(R.drawable.ic_calendar_arrow_right);

        calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_SINGLE);

        calendarView.setTitleFormatter(new TitleFormatter() {
            @Override
            public CharSequence format(CalendarDay day) {
                LocalDate inputDate=day.getDate();
                String[] calenderHeaderElements = inputDate.toString().split("-");
                switch (calenderHeaderElements[1]){
                    case "01":calenderHeaderElements[1]="JANUARY"; break;
                    case "02":calenderHeaderElements[1]="FEBRUARY"; break;
                    case "03":calenderHeaderElements[1]="MARCH"; break;
                    case "04":calenderHeaderElements[1]="APRIL"; break;
                    case "05":calenderHeaderElements[1]="MAY"; break;
                    case "06":calenderHeaderElements[1]="JUNE"; break;
                    case "07":calenderHeaderElements[1]="JULY"; break;
                    case "08":calenderHeaderElements[1]="AUGUST"; break;
                    case "09":calenderHeaderElements[1]="SEPTEMBER"; break;
                    case "10":calenderHeaderElements[1]="OCTOBER"; break;
                    case "11":calenderHeaderElements[1]="NOVEMBER"; break;
                    case "12":calenderHeaderElements[1]="DECEMBER"; break;
                    default:
                }

                yearText.setText(calenderHeaderElements[0]);

                StringBuilder calenderStringBuilder=new StringBuilder();
                calenderStringBuilder.append(calenderHeaderElements[1]);
                return calenderStringBuilder.toString();

            }
        });

        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                Log.d("캘린더 날짜",date.toString());
                if(selectedDay!=date){
                    selectedDay=date;
                }
                else{
                    calendarView.clearSelection();
                    selectedDay=null;
                }

            }
        });

        CheckWrittenDays(CalendarDay.today().getYear(),CalendarDay.today().getMonth());

        //달력로 목록 갱신
        calendarView.setOnMonthChangedListener(new OnMonthChangedListener() {
            @Override
            public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
                CheckWrittenDays(date.getYear(),date.getMonth());
            }
        });

        plusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(selectedDay==null){
                    goToFeedWrite(null);
                }
                else{
                    showWriteConfirmDialog(selectedDay);
                }
            }
        });

        feedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent gofeed=new Intent(WalkingHomeActivity.this, FeedActivity.class);
                if(selectedDay==null){
                    gofeed.putExtra("iswrite",2);
                    startActivity(gofeed);
                }
                else{
                    gofeed.putExtra("year",selectedDay.getYear());
                    gofeed.putExtra("month",selectedDay.getMonth());
                    gofeed.putExtra("day",selectedDay.getDay());
                    gofeed.putExtra("iswrite",2);
                    startActivity(gofeed);
                }
            }
        });



    }

    public void setReliable(){
        userData=UserData.loadData(this);

        String usertitlestr;
        if(userData.title.equals("없음")){
            usertitlestr="[칭호를 설정해주세요]";
        }
        else{
            usertitlestr=userData.title;
        }
        usertitle.setText(usertitlestr);
        username.setText(userData.appname);
        Bitmap userimagebmp=UserData.loadImageToBitmap(this);
        Log.d("유저 프로필",(userimagebmp==null)+"");
        if(userimagebmp!=null){
            userimage.setImageBitmap(userimagebmp);
        }
        else{
            userimage.setImageResource(R.drawable.blank_profile);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        setReliable();
    }

    //뒤로가기로 나갔을시 홈버튼으로 나간것처럼 만들음. 종료로 인한 오류 방지
    @Override
    public void onBackPressed() {
        Intent intent=new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(start){
            frameLayout.removeView(mainlayout);
            start=false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        CheckWrittenDays(CalendarDay.today().getYear(),CalendarDay.today().getMonth());
    }


    private final ActivityResultLauncher<Intent> setProfileResult= registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() ==RESULT_OK){
                    setReliable();
                }
            });


    public void CheckWrittenDays(int year, int month){
        FirebaseFirestore fb=FirebaseFirestore.getInstance();
        CollectionReference feeddata=fb.collection("feedlist");
        ArrayList<CalendarDay> result=new ArrayList<>();

        feeddata.whereEqualTo("year", year).whereEqualTo("month",month).whereEqualTo("userid",userData.userid).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                for(int i=0; i<task.getResult().size(); ++i){
                    String tmp= (String) task.getResult().getDocuments().get(i).get("title");
                    String[] tmps=tmp.split("_");
                    CalendarDay tmpcal=CalendarDay.from(Integer.parseInt(tmps[0].replace("년","")),
                            Integer.parseInt(tmps[1].replace("월","")),
                            Integer.parseInt(tmps[2].replace("일","")));
                    result.add(tmpcal);
                }

                calendarView.clearSelection();
                selectedDay=null;
                feedData=new FeedData();
                feedlist=feedData.scanFeedList(WalkingHomeActivity.this);
                calendarView.removeDecorators();
                int saturdayColor = ContextCompat.getColor(WalkingHomeActivity.this, R.color.blue);
                int sundayColor = ContextCompat.getColor(WalkingHomeActivity.this, R.color.red);
                int futureSaturdayColor = argb(120, 70, 110, 190);
                int futureSundayColor = argb(120, 200, 80, 80);
                int futureWeekdayColor = argb(130, 120, 112, 98);
                int dotColor = ContextCompat.getColor(WalkingHomeActivity.this, R.color.music_primary);
                int todayColor = ContextCompat.getColor(WalkingHomeActivity.this, R.color.music_primary_dark);

                calendarView.addDecorators(new SaturdayDecorator(saturdayColor), new SundayDecorator(sundayColor),
                        new BlurSaturdayDecorator(futureSaturdayColor), new BlurSundayDecorator(futureSundayColor),
                        new BlurDecorator(futureWeekdayColor), new SelectedDecorator(WalkingHomeActivity.this),
                        new DotDecorator(dotColor, findDays(feedlist)), new TodayDecorator(todayColor));
                calendarView.addDecorator(new WrittenDecorator(WalkingHomeActivity.this,result));
                updateStreakWidget(findDays(feedlist), year, month);

            }
        });
    }

    private void goToFeedWrite(CalendarDay day) {
        Intent gofeed = new Intent(WalkingHomeActivity.this, FeedActivity.class);
        if (day != null) {
            gofeed.putExtra("year", day.getYear());
            gofeed.putExtra("month", day.getMonth());
            gofeed.putExtra("day", day.getDay());
        }
        gofeed.putExtra("iswrite", 1);
        startActivity(gofeed);
    }

    private void showWriteConfirmDialog(CalendarDay day) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_feed_write_confirm, null);
        TextView message = dialogView.findViewById(R.id.dialog_message);
        message.setText(String.format(Locale.KOREA, "%d년 %d월 %d일 기록을 남길까요?", day.getYear(), day.getMonth(), day.getDay()));

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        dialog.setCancelable(true);

        Button cancelBtn = dialogView.findViewById(R.id.dialog_cancel);
        Button confirmBtn = dialogView.findViewById(R.id.dialog_confirm);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                goToFeedWrite(day);
            }
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }


    public ArrayList<CalendarDay> findDays(ArrayList<String> feedlist){
        ArrayList<CalendarDay> result=new ArrayList<>();
        if(feedlist==null){
            return result;
        }
        for(int i=0; i<feedlist.size(); ++i){
            CalendarDay tmp=strTocal(feedlist.get(i));
            if(!result.contains(tmp)){
                result.add(tmp);
            }
        }
        return result;
    }

    public CalendarDay strTocal(String dayinput){
        String y,m,d,day;
        day=dayinput.replace("(최근)","");
        y=day.substring(0,4);
        m=day.substring(6,8);
        d=day.substring(10,12);
        Log.d("strTocal",y+m+d);
        return CalendarDay.from(Integer.parseInt(y),Integer.parseInt(m),Integer.parseInt(d));
    }

    private void updateStreakWidget(ArrayList<CalendarDay> dayList, int displayYear, int displayMonth){
        Set<LocalDate> dateSet=new HashSet<>();
        for(int i=0; i<dayList.size(); ++i){
            dateSet.add(dayList.get(i).getDate());
        }

        int monthCount=0;
        for(LocalDate date : dateSet){
            if(date.getYear()==displayYear&&date.getMonthValue()==displayMonth){
                monthCount++;
            }
        }

        int currentStreak=0;
        LocalDate today=LocalDate.now();
        LocalDate cursor=null;
        if(dateSet.contains(today)){
            cursor=today;
        }
        else if(dateSet.contains(today.minusDays(1))){
            cursor=today.minusDays(1);
        }
        if(cursor!=null){
            while(dateSet.contains(cursor)){
                currentStreak++;
                cursor=cursor.minusDays(1);
            }
        }

        int bestStreak=0;
        if(!dateSet.isEmpty()){
            ArrayList<LocalDate> sortedDates=new ArrayList<>(dateSet);
            Collections.sort(sortedDates);
            int run=1;
            bestStreak=1;
            for(int i=1; i<sortedDates.size(); ++i){
                if(sortedDates.get(i-1).plusDays(1).equals(sortedDates.get(i))){
                    run++;
                }
                else{
                    run=1;
                }
                if(run>bestStreak){
                    bestStreak=run;
                }
            }
        }

        streakCurrentValue.setText(currentStreak+"일");
        streakMonthValue.setText(monthCount+"회");
        streakBestValue.setText(bestStreak+"일");
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id=item.getItemId();
        Intent intent=new Intent();
        if(id==R.id.challenge){
            intent=new Intent(this, ChallengeActivity.class);
            startActivity(intent);
        }
        else if(id==R.id.managefriends){
            intent=new Intent(this, ManageFriendActivity.class);
            startActivity(intent);
        }
        else if(id==R.id.helpinfo){
            intent=new Intent(this, HelpInfoActivity.class);
            startActivity(intent);
        }
        else if(id==R.id.appinfo){
            intent=new Intent(this, AppInfoActivity.class);
            startActivity(intent);
        }
        else if(id==R.id.settingprofile){
            intent=new Intent(this, EditUserProfileActivity.class);
            setProfileResult.launch(intent);
        }
        else if(id==R.id.schedules){
            intent=new Intent(this,ScheduleActivity.class);
            startActivity(intent);
        }
        else if(id==R.id.coinshop){
            intent=new Intent(this,CoinShopActivity.class);
            startActivity(intent);
        }
        return true;
    }





}

class SaturdayDecorator implements DayViewDecorator{

    private final int color;

    public SaturdayDecorator(int color){
        this.color=color;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day){
        DayOfWeek dayOfWeek=day.getDate().getDayOfWeek();
        return dayOfWeek.getValue()==DayOfWeek.SATURDAY.getValue();
    }

    @Override
    public void decorate(DayViewFacade view){
        view.addSpan(new ForegroundColorSpan(color));
    }
}

class SundayDecorator implements DayViewDecorator{

    private final int color;

    public SundayDecorator(int color){
        this.color=color;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day){
        DayOfWeek dayOfWeek=day.getDate().getDayOfWeek();
        return dayOfWeek.getValue()==DayOfWeek.SUNDAY.getValue();
    }

    @Override
    public void decorate(DayViewFacade view){
        view.addSpan(new ForegroundColorSpan(color));
    }
}

class BlurSaturdayDecorator implements DayViewDecorator{

    private CalendarDay date;
    private final int color;

    public BlurSaturdayDecorator(int color){
        this.color=color;
        date=CalendarDay.today();
    }

    @Override
    public boolean shouldDecorate(CalendarDay day){
        DayOfWeek dayOfWeek=day.getDate().getDayOfWeek();
        return day.isAfter(date)&&dayOfWeek.getValue()==DayOfWeek.SATURDAY.getValue();
    }

    @Override
    public void decorate(DayViewFacade view){
        view.addSpan(new ForegroundColorSpan(color));
    }
}

class BlurSundayDecorator implements DayViewDecorator{

    private CalendarDay date;
    private final int color;

    public BlurSundayDecorator(int color){
        this.color=color;
        date=CalendarDay.today();
    }

    @Override
    public boolean shouldDecorate(CalendarDay day){
        DayOfWeek dayOfWeek=day.getDate().getDayOfWeek();
        return day.isAfter(date)&&dayOfWeek.getValue()==DayOfWeek.SUNDAY.getValue();
    }

    @Override
    public void decorate(DayViewFacade view){
        view.addSpan(new ForegroundColorSpan(color));
    }
}

class BlurDecorator implements DayViewDecorator{

    private CalendarDay date;
    private final int color;

    public BlurDecorator(int color){
        this.color=color;
        date=CalendarDay.today();
    }

    @Override
    public boolean shouldDecorate(CalendarDay day){
        DayOfWeek dayOfWeek=day.getDate().getDayOfWeek();
        return day.isAfter(date)&&dayOfWeek.getValue()!=DayOfWeek.SATURDAY.getValue()&&dayOfWeek.getValue()!=DayOfWeek.SUNDAY.getValue();
    }

    @Override
    public void decorate(DayViewFacade view){
        view.addSpan(new ForegroundColorSpan(color));
    }
}

class SelectedDecorator implements DayViewDecorator{

    private final Drawable drawable;


    public SelectedDecorator(Activity activity){
        drawable=activity.getResources().getDrawable(selected_day);
    }

    @Override
    public boolean shouldDecorate(CalendarDay day){
        return true;
    }

    @Override
    public void decorate(DayViewFacade view){
        view.setSelectionDrawable(drawable);
    }
}

class DotDecorator implements DayViewDecorator{

    private final int color;
    private final ArrayList<CalendarDay> days;

    public DotDecorator(int color, ArrayList<CalendarDay> days){
        this.color=color;
        this.days=days;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day){
        return days.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view){
        view.addSpan(new DotSpan(7,color));
    }
}

class TodayDecorator implements DayViewDecorator {

    private final int color;

    public TodayDecorator(int color){
        this.color=color;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return day.equals(CalendarDay.today());
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.addSpan(new StyleSpan(Typeface.BOLD));
        view.addSpan(new ForegroundColorSpan(color));
    }
}

class WrittenDecorator implements DayViewDecorator{

    private final Drawable drawablerec;
    private final ArrayList<CalendarDay> wdays;


    public WrittenDecorator(Activity activity,ArrayList<CalendarDay> wdays){
        drawablerec=activity.getResources().getDrawable(R.drawable.written_selector);
        this.wdays=wdays;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day){
        return wdays.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view){
        view.setSelectionDrawable(drawablerec);
    }
}
