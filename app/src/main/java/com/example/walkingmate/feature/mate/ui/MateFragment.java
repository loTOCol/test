package com.example.walkingmate.feature.mate.ui;

import static android.app.Activity.RESULT_OK;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.walkingmate.feature.common.ui.DateSelector;
import com.example.walkingmate.R;
import com.example.walkingmate.feature.mate.data.repository.MateRepository;
import com.example.walkingmate.feature.user.data.UserData;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MateFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private final MateRepository mateRepository = MateRepository.getInstance();
    CollectionReference mateDataListRef = mateRepository.getMateDataListRef();
    CollectionReference users = mateRepository.getUsersRef();
    CollectionReference blocklist = mateRepository.getBlockListRef();
    CollectionReference mateRequestRef = mateRepository.getMateRequestRef();

    SwipeRefreshLayout swipeRefreshLayout;

    ListView mateListView;
    MateBoardAdapter mateBoardAdapter;
    MyMatePostsAdapter myMatePostsAdapter;
    MyMateRequestsAdapter myMateRequestsAdapter;

    ImageButton addMatePostButton;
    String cursorWriteTime="30001112093121";

    ArrayList<String> allDocIds=new ArrayList<>();//전체 게시물 가져올떄 모든 아이디
    ArrayList<String> mateDocIds=new ArrayList<>();//필터링된 전체게시물

    ArrayList<String> myDocIds=new ArrayList<>();//내 게시물

    ArrayList<String> myRequestDocIds=new ArrayList<>();//내가 신청한 게시물

    HashMap<String, String> userids=new HashMap<>();
    HashMap<String,String> titles=new HashMap<>();
    HashMap<String, String> dates=new HashMap<>();
    HashMap<String, ArrayList<String>> locations=new HashMap<>();
    HashMap<String, String> writetimes=new HashMap<>();
    HashMap<String, String> writers=new HashMap<>();
    HashMap<String, String> writerByUserId=new HashMap<>();

    boolean isLoadingMore=false;//최하단 스크롤시 getlist여러번 실행되는 오류막기위한 불리안
    boolean shouldRefreshOnResume=false;//게시물 추가후 돌아왔을때 새로고침시 중복실행 막기위한 불리안
    boolean needsDeletedCheck=false;

    UserData userData;

    View rootview;

    boolean searchopen=false;

    ArrayList<String> selectedLocations=new ArrayList<>();

    CheckBox[] checkBoxes=new CheckBox[16];

    Spinner sex,age;

    boolean nomore=true;

    ImageButton startcalendar,endcalendar, searchopenbtn;
    EditText syear,smonth,sday,eyear,emonth,eday;

    Button clearSetting;

    String startDateFilter,endDateFilter;

    Spinner viewmode;
    int viewModeIndex=2;
    private static final int PAGE_SIZE = 5;
    private final MatePager matePager = new MatePager(PAGE_SIZE);
    private int currentPage = 0;
    Button prevPageBtn, nextPageBtn;
    TextView pageIndicatorTxt;
    Spinner inlineSearchModeSpinner;
    EditText inlineSearchInput;
    ImageButton inlineSearchApplyBtn, inlineSearchClearBtn;
    String inlineSearchKeyword = "";
    int inlineSearchMode = 0; // 0: writer, 1: title

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootview= inflater.inflate(R.layout.fragment_mate, container, false);

        swipeRefreshLayout=rootview.findViewById(R.id.refresh_matelist);
        swipeRefreshLayout.setOnRefreshListener(this::onRefresh);

        startDateFilter="1000/01/01";
        endDateFilter="9999/12/31";

        sex=rootview.findViewById(R.id.spinner_sex_matefrag);
        age=rootview.findViewById(R.id.spinner_age_matefrag);

        startcalendar=rootview.findViewById(R.id.start_matefrag);
        endcalendar=rootview.findViewById(R.id.end_matefrag);
        syear=rootview.findViewById(R.id.year_start);
        smonth=rootview.findViewById(R.id.month_start);
        sday=rootview.findViewById(R.id.day_start);
        eyear=rootview.findViewById(R.id.year_end);
        emonth=rootview.findViewById(R.id.month_end);
        eday=rootview.findViewById(R.id.day_end);
        viewmode=rootview.findViewById(R.id.spinner_viewmode_matefrag);

        viewmode.setSelection(2);



        startcalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getstartResult.launch(new Intent(getActivity(),DateSelector.class));
            }
        });

        endcalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getendResult.launch(new Intent(getActivity(),DateSelector.class));
            }
        });




        userData=UserData.loadData(getActivity());

        searchopenbtn=rootview.findViewById(R.id.search_matefrag);
        searchopenbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int visible;
                if(searchopen){
                    visible=View.INVISIBLE;
                    searchopen=false;
                }
                else{
                    visible=View.VISIBLE;
                    searchopen=true;
                }
                rootview.findViewById(R.id.searchlayout_matefrag).setVisibility(visible);
            }
        });


        checkBoxes[0]=rootview.findViewById(R.id.radio1);
        checkBoxes[0].setChecked(true);
        selectedLocations.add(checkBoxes[0].getText().toString());

        checkBoxes[1]=rootview.findViewById(R.id.radio2);checkBoxes[8]=rootview.findViewById(R.id.radio9);
        checkBoxes[2]=rootview.findViewById(R.id.radio3);checkBoxes[9]=rootview.findViewById(R.id.radio10);
        checkBoxes[3]=rootview.findViewById(R.id.radio4);checkBoxes[10]=rootview.findViewById(R.id.radio11);
        checkBoxes[4]=rootview.findViewById(R.id.radio5);checkBoxes[11]=rootview.findViewById(R.id.radio12);
        checkBoxes[5]=rootview.findViewById(R.id.radio6);checkBoxes[12]=rootview.findViewById(R.id.radio13);
        checkBoxes[6]=rootview.findViewById(R.id.radio7);checkBoxes[13]=rootview.findViewById(R.id.radio14);
        checkBoxes[7]=rootview.findViewById(R.id.radio8);checkBoxes[14]=rootview.findViewById(R.id.radio15);
        checkBoxes[15]=rootview.findViewById(R.id.radio16);
        for(CheckBox checkBox:checkBoxes){
            checkBox.setOnClickListener(checkboxlistener);
        }

        mateListView=rootview.findViewById(R.id.matelist);
        addMatePostButton=rootview.findViewById(R.id.add_matelist);
        addMatePostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(),MateWriteActivity.class));
                shouldRefreshOnResume=true;
            }
        });

        prevPageBtn=rootview.findViewById(R.id.btn_prev_page_mate);
        nextPageBtn=rootview.findViewById(R.id.btn_next_page_mate);
        pageIndicatorTxt=rootview.findViewById(R.id.tv_page_indicator_mate);
        inlineSearchModeSpinner=rootview.findViewById(R.id.spinner_inline_search_mode_mate);
        inlineSearchInput=rootview.findViewById(R.id.et_inline_search_mate);
        inlineSearchApplyBtn=rootview.findViewById(R.id.btn_inline_search_apply);
        inlineSearchClearBtn=rootview.findViewById(R.id.btn_inline_search_clear);

        ArrayAdapter<String> searchModeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                Arrays.asList("작성자", "게시글")
        );
        searchModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        inlineSearchModeSpinner.setAdapter(searchModeAdapter);
        inlineSearchModeSpinner.setSelection(0);
        inlineSearchModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                inlineSearchMode = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        inlineSearchApplyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyInlineSearch();
            }
        });
        inlineSearchClearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inlineSearchKeyword = "";
                inlineSearchInput.setText("");
                currentPage = 0;
                mateBoardAdapter.notifyDataSetChanged();
                updateMatePagingUi();
            }
        });

        prevPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentPage>0){
                    currentPage--;
                    mateBoardAdapter.notifyDataSetChanged();
                    updateMatePagingUi();
                }
            }
        });
        nextPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int totalPages = getMatePageCount();
                if(currentPage < totalPages-1){
                    currentPage++;
                    mateBoardAdapter.notifyDataSetChanged();
                    updateMatePagingUi();
                }
                else if(viewModeIndex==2 && nomore && !isLoadingMore && inlineSearchKeyword.trim().isEmpty()){
                    isLoadingMore=true;
                    getlist();
                }
            }
        });

        mateBoardAdapter=new MateBoardAdapter(getContext());
        myMatePostsAdapter=new MyMatePostsAdapter(getContext());
        myMateRequestsAdapter=new MyMateRequestsAdapter(getContext());
        mateListView.setAdapter(mateBoardAdapter);
        getlist();
        getMyList();
        getMyRequestList();
        updateMatePagingUi();

        viewmode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                viewModeIndex=i;
                if(viewModeIndex!=2){
                    if(searchopen){
                        searchopenbtn.performClick();
                    }
                    searchopenbtn.setVisibility(View.INVISIBLE);
                    rootview.findViewById(R.id.paging_matelist).setVisibility(View.GONE);
                    rootview.findViewById(R.id.inline_search_matelist).setVisibility(View.GONE);
                }
                else{
                    searchopenbtn.setVisibility(View.VISIBLE);
                    rootview.findViewById(R.id.paging_matelist).setVisibility(View.VISIBLE);
                    rootview.findViewById(R.id.inline_search_matelist).setVisibility(View.VISIBLE);
                }
                switch (i){
                    case 2:
                        currentPage=0;
                        mateListView.setAdapter(mateBoardAdapter); break;
                    case 1:
                        mateListView.setAdapter(myMateRequestsAdapter); break;
                    case 0:
                        Log.d("내 게시물",myDocIds.toString());
                        mateListView.setAdapter(myMatePostsAdapter); break;
                    default:
                        mateListView.setAdapter(mateBoardAdapter); break;

                }
                updateMatePagingUi();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mateListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                if(viewModeIndex!=2){
                    return;
                }
                if(!inlineSearchKeyword.trim().isEmpty()){
                    return;
                }
                int totalPages = getMatePageCount();
                boolean isLastPage = currentPage >= Math.max(totalPages-1,0);
                if(isLastPage && !mateListView.canScrollVertically(1)){
                    if(!isLoadingMore){
                        isLoadingMore=true;
                        getlist();
                    }

                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {

            }
        });

        rootview.findViewById(R.id.finishSetting_matefrag).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String tmps,tmpe;
                tmps=String.format("%s/%s/%s",syear.getText().toString(),smonth.getText().toString(),sday.getText().toString());
                tmpe=String.format("%s/%s/%s",eyear.getText().toString(),emonth.getText().toString(),eday.getText().toString());
                //비어있으면 제한선 없는것으로 만들음
                Log.d("날짜 체크",tmps+","+tmpe);
                if(tmps.equals("//")){
                    tmps="1000/01/01";
                }
                if(tmpe.equals("//")){
                    tmpe="9999/12/31";
                }
                if(!checkdate(tmps,tmpe)){
                    Toast.makeText(getActivity(),"잘못된 날짜를 입력하셨습니다.",Toast.LENGTH_SHORT).show();
                    return;
                }
                else{
                    startDateFilter=tmps;
                    endDateFilter=tmpe;
                }

                if(!isLoadingMore){
                    Toast.makeText(getActivity(),"설정 완료되었습니다.",Toast.LENGTH_SHORT).show();
                    isLoadingMore=true;
                    refreshs();
                }


            }
        });

        clearSetting=rootview.findViewById(R.id.clearSetting_matefrag);
        clearSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDateFilter="1000/01/01";
                endDateFilter="9999/12/31";
                for(CheckBox checkBox:checkBoxes){
                    checkBox.setChecked(false);
                }
                checkBoxes[0].setChecked(true);
                selectedLocations.clear();
                selectedLocations.add("전체");
                syear.setText("");
                smonth.setText("");
                sday.setText("");
                eyear.setText("");
                emonth.setText("");
                eday.setText("");
                sex.setSelection(0);
                age.setSelection(0);
            }
        });



        return rootview;
    }

    //포멧 형식과 전후 관계 체크(시작일과 끝일이 같으면 true;)
    public boolean checkdate(String start, String end){
        try{
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy/MM/dd");
            sdf.setLenient(false);
            Date sdate,edate;

            sdate=sdf.parse(start);
            edate=sdf.parse(end);

            if(edate.before(sdate)){
                return false;
            }

        }catch (Exception e){
            return false;
        }
        return true;

    }


    private final ActivityResultLauncher<Intent> getstartResult= registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() ==RESULT_OK){
                    if(result.getData()!=null){
                        syear.setText(result.getData().getIntExtra("mYear",2022)+"");
                        smonth.setText(result.getData().getIntExtra("mMonth",12)+"");
                        sday.setText(result.getData().getIntExtra("mDay",1)+"");
                    }
                }
            });

    private final ActivityResultLauncher<Intent> getendResult= registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() ==RESULT_OK){
                    if(result.getData()!=null){
                        eyear.setText(result.getData().getIntExtra("mYear",2022)+"");
                        emonth.setText(result.getData().getIntExtra("mMonth",12)+"");
                        eday.setText(result.getData().getIntExtra("mDay",1)+"");
                    }
                }
            });



    CheckBox.OnClickListener checkboxlistener=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            boolean ischecked=((CheckBox)view).isChecked();
            String location=((CheckBox)view).getText().toString();
            if(ischecked){
                if(!selectedLocations.contains(location)){
                    selectedLocations.add(location);
                }

            }
            else{
                if(selectedLocations.contains(location)){
                    selectedLocations.remove(location);
                }
            }
            Log.d("검색 필터",selectedLocations.toString());
        }
    };


    public void refreshs(){
        nomore=true;
        cursorWriteTime="30001112093121";

        userids.clear();
        titles.clear();
        dates.clear();
        locations.clear();
        writetimes.clear();
        writers.clear();

        mateDocIds.clear();
        allDocIds.clear();
        myDocIds.clear();
        myRequestDocIds.clear();

        getlist();
        getMyList();
        getMyRequestList();
    }

    //차단, 연령대, 성별(상대 기준), 날짜 체크
    //파이어베이스 중복실행 에러로 여러번 들어오는 경우 체크
    public boolean checkfilter(DocumentSnapshot document, ArrayList<String> myblockuser){
        //내 차단목록 체크
        if(myblockuser.contains(document.getString("userid"))){
            return false;
        }

        if(mateDocIds.contains(document.getId())){
            return false;
        }

        //연령대 체크
        String agefilter=document.getString("age");
        if(!agefilter.equals("무관")){
            if(!agefilter.equals(userData.age)){
                return false;
            }
        }

        //성별 체크
        String mygender;
        if(userData.gender.equals("M")){
            mygender="남성";
        }
        else{
            mygender="여성";
        }
        String genderfilter=document.getString("blockgender");
        if(genderfilter.equals(mygender)){
            return false;
        }

        //시작일 체크:설정시작일<=문서 시작일
        if(!checkdate(startDateFilter,document.getString("starttime"))){
            return false;
        }

        //종료일 체크: 문서 종료일<=설정종료일
        if(!checkdate(document.getString("endtime"),endDateFilter)){
            return false;
        }

        return true;

    }

    //필터 추가시 쿼리적용
    public void getlist(){

        if(mateDocIds.contains("last")){
            mateDocIds.remove("last");
        }
        Query query=mateDataListRef;

        if(selectedLocations.size()==0){
            Toast.makeText(getActivity(),"지역을 선택해주세요.",Toast.LENGTH_SHORT).show();
            return;
        }

        if(!selectedLocations.contains("전체")){
            query=query.whereIn("startlocation",selectedLocations);
            Log.d("지역 필터",selectedLocations.toString());
        }
        else{
            Log.d("지역 필터","없음");
        }

        String gender=sex.getSelectedItem().toString(),ages=age.getSelectedItem().toString();

        //내 필터
        if(!gender.equals("무관")){
            query=query.whereEqualTo("usergender",gender);
            Log.d("성별 필터",gender);
        }
        else{
            Log.d("성별 필터","없음");
        }
        if(!ages.equals("무관")){
            query=query.whereEqualTo("userage",ages);
            Log.d("연령 필터",ages);
        }
        else{
            Log.d("연령 필터","없음");
        }

        Log.d("메이트 쿼리","gen:"+gender+", age:"+ages+", locations:"+selectedLocations.toString());
        Log.d("메이트 최하단 게시물 시작전",cursorWriteTime);

        ArrayList<String> myblockusers=new ArrayList<>();
        Query finalQuery = query;
        swipeRefreshLayout.setRefreshing(true);

        Task<DocumentSnapshot> myBlockedTask = blocklist.document(userData.userid).get();
        Task<QuerySnapshot> blockedMeTask = blocklist.whereArrayContains("userid",userData.userid).get();

        Tasks.whenAllComplete(myBlockedTask, blockedMeTask).addOnCompleteListener(new OnCompleteListener<java.util.List<Task<?>>>() {
            @Override
            public void onComplete(@NonNull Task<java.util.List<Task<?>>> combinedTask) {
                if(myBlockedTask.isSuccessful() && myBlockedTask.getResult()!=null && myBlockedTask.getResult().exists()){
                    DocumentSnapshot document=myBlockedTask.getResult();
                    ArrayList<String> blockedIds = (ArrayList<String>) document.get("userid");
                    if(blockedIds!=null){
                        myblockusers.addAll(blockedIds);
                    }
                }

                if(blockedMeTask.isSuccessful() && blockedMeTask.getResult()!=null){
                    for(DocumentSnapshot document: blockedMeTask.getResult().getDocuments()){
                        myblockusers.add(document.getId());
                    }
                }

                finalQuery.whereLessThan("writetime",cursorWriteTime).orderBy("writetime", Query.Direction.DESCENDING).limit(50).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        swipeRefreshLayout.setRefreshing(false);
                        if(!task.isSuccessful()){
                            Log.d("파이어베이스에러_메이트",1+"");
                            if(isLoadingMore){
                                isLoadingMore=false;
                            }
                            return;
                        }
                        if(!task.getResult().isEmpty()){
                            QuerySnapshot documents=task.getResult();
                            for(DocumentSnapshot document: documents){

                                boolean req=checkfilter(document,myblockusers);

                                if(req){
                                    mateDocIds.add(document.getId());
                                }
                                allDocIds.add(document.getId());

                                titles.put(document.getId(), (String) document.get("title"));
                                userids.put(document.getId(), (String) document.get("userid"));
                                try {
                                    dates.put(document.getId(),getDateRangeText(document.get("year"),document.get("month"),document.get("day"),document.get("hour"),document.get("minute"),document.get("takentime")));
                                } catch (ParseException e) {
                                    dates.put(document.getId()," ");
                                }
                                locations.put(document.getId(), (ArrayList<String>) document.get("locations_name"));
                                writetimes.put(document.getId(), (String) document.get("writetime"));
                            }
                            cursorWriteTime=writetimes.get(allDocIds.get(allDocIds.size()-1));
                            Log.d("메이트 최하단 게시물",cursorWriteTime);

                            preloadWritersForMateDocs();
                            mateBoardAdapter.notifyDataSetChanged();
                            if(isLoadingMore){
                                isLoadingMore=false;
                            }
                            updateMatePagingUi();
                            if(shouldRefreshOnResume){
                                shouldRefreshOnResume=false;
                            }
                            Log.d("메이트리스트수",mateDocIds.size()+"");
                        }
                        else{
                            nomore=false;
                            if(isLoadingMore){
                                isLoadingMore=false;
                            }
                            mateBoardAdapter.notifyDataSetChanged();
                            updateMatePagingUi();
                        }
                    }
                });
            }
        });
    }

    private void preloadWritersForMateDocs(){
        HashSet<String> needUserIds = new HashSet<>();
        for(String docId: mateDocIds){
            String uid = userids.get(docId);
            if(uid==null){
                continue;
            }
            if(writerByUserId.containsKey(uid)){
                writers.put(docId, writerByUserId.get(uid));
            } else {
                needUserIds.add(uid);
            }
        }

        for(String uid: needUserIds){
            users.document(uid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(!task.isSuccessful() || task.getResult()==null || !task.getResult().exists()){
                        return;
                    }
                    String appname = (String) task.getResult().get("appname");
                    if(appname==null){
                        appname = "";
                    }
                    writerByUserId.put(uid, appname);
                    for(String docId : mateDocIds){
                        if(uid.equals(userids.get(docId))){
                            writers.put(docId, appname);
                        }
                    }
                    mateBoardAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    public void getMyList(){
        Log.d("내 리스트 진입","1");
        mateDataListRef.whereEqualTo("userid",userData.userid).orderBy("writetime", Query.Direction.DESCENDING).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(!task.isSuccessful()){
                    Log.d("파이어베이스에러_메이트",1+"");
                    return;
                }
                if(!task.getResult().isEmpty()){
                    Log.d("내 리스트 진입","2");
                    QuerySnapshot documents=task.getResult();
                    for(DocumentSnapshot document: documents){
                        if(myDocIds.contains(document.getId())){
                            continue;
                        }
                        else{
                            myDocIds.add(document.getId());
                            titles.put(document.getId(), (String) document.get("title"));
                            userids.put(document.getId(), (String) document.get("userid"));
                            try {
                                dates.put(document.getId(),getDateRangeText(document.get("year"),document.get("month"),document.get("day"),document.get("hour"),document.get("minute"),document.get("takentime")));
                            } catch (ParseException e) {
                                dates.put(document.getId()," ");
                            }
                            locations.put(document.getId(), (ArrayList<String>) document.get("locations_name"));
                            writetimes.put(document.getId(), (String) document.get("writetime"));
                        }
                        myMatePostsAdapter.notifyDataSetChanged();

                    }
                }
                else{
                    Log.d("내 리스트 진입","3");
                    if(myDocIds.size()==0){
                        myDocIds.add("last");
                        myMatePostsAdapter.notifyDataSetChanged();
                        Log.d("내 리스트 진입","4");
                    }
                }
            }
        });
    }

    public void getMyRequestList(){
        Log.d("내 신청 메이트 진입체크","0");
        mateRequestRef.document(userData.userid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(!task.isSuccessful()){
                    Log.d("파이어베이스에러",1+"");
                    return;
                }
                DocumentSnapshot document=task.getResult();
                ArrayList<String> docuids= (ArrayList<String>) document.get("requestlist");
                if(docuids==null||docuids.size()==0){
                    if(myRequestDocIds.size()==0){
                        Log.d("내 신청 메이트 진입체크","1");
                        myRequestDocIds.add("last");
                        myMateRequestsAdapter.notifyDataSetChanged();
                    }
                }
                if(docuids!=null){
                    if(docuids.size()>0){
                        Log.d("내 신청 메이트 진입체크","2");
                        for(String s:docuids){
                            mateDataListRef.document(s).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if(task.isSuccessful()){
                                        DocumentSnapshot document=task.getResult();

                                        if(!myRequestDocIds.contains(document.getId())){
                                            myRequestDocIds.add(document.getId());
                                            titles.put(document.getId(), (String) document.get("title"));
                                            userids.put(document.getId(), (String) document.get("userid"));
                                            try {
                                                dates.put(document.getId(),getDateRangeText(document.get("year"),document.get("month"),document.get("day"),document.get("hour"),document.get("minute"),document.get("takentime")));
                                            } catch (ParseException e) {
                                                dates.put(document.getId()," ");
                                            }
                                            locations.put(document.getId(), (ArrayList<String>) document.get("locations_name"));
                                            writetimes.put(document.getId(), (String) document.get("writetime"));
                                            myMateRequestsAdapter.notifyDataSetChanged();
                                            Log.d("내 신청 메이트 진입체크","3:"+document.getString("title"));

                                            users.document(document.getString("userid")).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                    Log.d("메이트 유저 추가", (String) task.getResult().get("appname"));
                                                    writers.put(document.getId(), (String) task.getResult().get("appname"));
                                                    myMateRequestsAdapter.notifyDataSetChanged();
                                                }
                                            });
                                        }
                                    }
                                }
                            });
                        }
                    }
                }

            }
        });
    }

    public String getDateRangeText(Object y, Object m, Object d, Object h, Object min, Object taken) throws ParseException {
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy/MM/dd HH:mm");
        String start=String.format("%04d/%02d/%02d %02d:%02d",y,m,d,h,min);
        Date date=sdf.parse(start);
        Calendar calendar=Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE,Long.valueOf((Long) Optional.ofNullable(taken).orElse(0L)).intValue());
        return start +"~"+ sdf.format(calendar.getTime());
    }

    @Override
    public void onRefresh() {
        refreshs();
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(shouldRefreshOnResume){
            refreshs();
        }
        if(needsDeletedCheck){
            checkDeletedPosts();
            needsDeletedCheck=false;
        }

    }

    public void checkDeletedPosts(){
        for(String s:mateDocIds){
            if(s.equals("last")){
                continue;
            }
            mateDataListRef.document(s).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(!task.getResult().exists()){
                        mateDocIds.remove(s);
                        mateBoardAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
        for(String s:myDocIds){
            if(s.equals("last")){
                continue;
            }
            mateDataListRef.document(s).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(!task.getResult().exists()){
                        myDocIds.remove(s);
                        if(myDocIds.size()==0){
                            myDocIds.add("last");
                        }
                        myMatePostsAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
        for(String s:myRequestDocIds){
            if(s.equals("last")){
                continue;
            }
            mateDataListRef.document(s).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(!task.getResult().exists()){
                        myRequestDocIds.remove(s);
                        if(myRequestDocIds.size()==0){
                            myRequestDocIds.add("last");
                        }
                        myMateRequestsAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    public class MateBoardAdapter extends BaseAdapter {

        LayoutInflater layoutInflater;


        public MateBoardAdapter(Context context){
            this.layoutInflater = LayoutInflater.from(context);

        }
        @Override
        public int getCount() {
            ArrayList<String> filtered = getFilteredMateDocIds();
            int totalPages = matePager.pageCount(filtered.size());
            currentPage = matePager.clampPage(currentPage, totalPages);
            return matePager.pageItemCount(filtered.size(), currentPage);
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = layoutInflater.inflate(R.layout.item_mate_post, null);
            View emptyview=layoutInflater.inflate(R.layout.view_empty,null);
            if(mateDocIds.size()==0){
                return emptyview;
            }
            ArrayList<String> filtered = getFilteredMateDocIds();
            if(filtered.size()==0){
                return emptyview;
            }
            int realPosition = matePager.toRealPosition(currentPage, position);
            String docId = filtered.get(realPosition);

            View bodyView = view.findViewById(R.id.mate_body);
            try{
                bodyView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        needsDeletedCheck=true;
                        Intent intent=new Intent(getActivity(),MateViewActivity.class);
                        intent.putExtra("docuid",docId);
                        intent.putExtra("date",dates.get(docId));
                        intent.putExtra("userid",userids.get(docId));
                        intent.putExtra("prefill_title",titles.get(docId));
                        intent.putExtra("prefill_writer",writers.get(docId));
                        intent.putStringArrayListExtra("prefill_locations",locations.get(docId));
                        startActivity(intent);

                    }
                });
            }catch (Exception e){
                e.printStackTrace();
            }


            TextView title=view.findViewById(R.id.mate_title);
            TextView datetxt=view.findViewById(R.id.mate_date);
            TextView writetime=view.findViewById(R.id.mate_writetime);
            TextView writer=view.findViewById(R.id.mate_writer);

            title.setText(titles.get(docId));
            datetxt.setText(dates.get(docId));
            String timetmp=writetimes.get(docId);
            writetime.setText(String.format("%s/%s/%s %s:%s",timetmp.substring(0,4),timetmp.substring(4,6),timetmp.substring(6,8),timetmp.substring(8,10),timetmp.substring(10,12)));
            writer.setText(writers.get(docId));

            return view;

        }
    }

    private int getMatePageCount(){
        return matePager.pageCount(getFilteredMateDocIds().size());
    }

    private ArrayList<String> getFilteredMateDocIds(){
        return matePager.filterMateDocIds(mateDocIds, inlineSearchKeyword, inlineSearchMode, writers, titles);
    }

    private void applyInlineSearch(){
        inlineSearchKeyword = inlineSearchInput.getText().toString();
        currentPage = 0;
        mateBoardAdapter.notifyDataSetChanged();
        updateMatePagingUi();
    }

    private void updateMatePagingUi(){
        if(pageIndicatorTxt==null || prevPageBtn==null || nextPageBtn==null){
            return;
        }
        int totalPages = getMatePageCount();
        currentPage = matePager.clampPage(currentPage, totalPages);
        pageIndicatorTxt.setText((currentPage+1) + " / " + totalPages);
        prevPageBtn.setEnabled(currentPage>0);
        nextPageBtn.setEnabled(currentPage<totalPages-1 || nomore);
    }

    public class MyMatePostsAdapter extends BaseAdapter {

        LayoutInflater layoutInflater;


        public MyMatePostsAdapter(Context context){
            this.layoutInflater = LayoutInflater.from(context);

        }
        @Override
        public int getCount() {
            return myDocIds.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = layoutInflater.inflate(R.layout.item_mate_post, null);
            View emptyview=layoutInflater.inflate(R.layout.view_empty,null);
            View lastview=layoutInflater.inflate(R.layout.item_mate_list_footer,null);
            if(myDocIds.size()==0){
                return emptyview;
            }
            if(myDocIds.get(position).equals("last")){
                TextView lasttxt=lastview.findViewById(R.id.lastviewtxt);
                lasttxt.setText("게시물이 존재하지 않습니다.");
                return lastview;
            }

            View bodyView = view.findViewById(R.id.mate_body);
            try{
                bodyView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String selectedDocId = myDocIds.get(position);
                        needsDeletedCheck=true;
                        Intent intent=new Intent(getActivity(),MateViewActivity.class);
                        intent.putExtra("docuid",selectedDocId);
                        intent.putExtra("date",dates.get(selectedDocId));
                        intent.putExtra("userid",userids.get(selectedDocId));
                        intent.putExtra("prefill_title",titles.get(selectedDocId));
                        intent.putExtra("prefill_writer",userData.appname);
                        intent.putStringArrayListExtra("prefill_locations",locations.get(selectedDocId));
                        startActivity(intent);

                    }
                });
            }catch (Exception e){
                e.printStackTrace();
            }


            TextView title=view.findViewById(R.id.mate_title);
            TextView datetxt=view.findViewById(R.id.mate_date);
            TextView writetime=view.findViewById(R.id.mate_writetime);
            TextView writer=view.findViewById(R.id.mate_writer);

            title.setText(titles.get(myDocIds.get(position)));
            datetxt.setText(dates.get(myDocIds.get(position)));
            String timetmp=writetimes.get(myDocIds.get(position));
            writetime.setText(String.format("%s/%s/%s %s:%s",timetmp.substring(0,4),timetmp.substring(4,6),timetmp.substring(6,8),timetmp.substring(8,10),timetmp.substring(10,12)));
            writer.setText(userData.appname);

            return view;

        }
    }

    public class MyMateRequestsAdapter extends BaseAdapter {

        LayoutInflater layoutInflater;


        public MyMateRequestsAdapter(Context context){
            this.layoutInflater = LayoutInflater.from(context);

        }
        @Override
        public int getCount() {
            return myRequestDocIds.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = layoutInflater.inflate(R.layout.item_mate_post, null);
            View emptyview=layoutInflater.inflate(R.layout.view_empty,null);
            View lastview=layoutInflater.inflate(R.layout.item_mate_list_footer,null);
            if(myRequestDocIds.size()==0){
                return emptyview;
            }
            if(myRequestDocIds.get(position).equals("last")){
                TextView lasttxt=lastview.findViewById(R.id.lastviewtxt);
                lasttxt.setText("신청한 게시물이 존재하지 않습니다.");
                return lastview;
            }

            View bodyView = view.findViewById(R.id.mate_body);
            try{
                bodyView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String selectedDocId = myRequestDocIds.get(position);
                        needsDeletedCheck=true;
                        Intent intent=new Intent(getActivity(),MateViewActivity.class);
                        intent.putExtra("docuid",selectedDocId);
                        intent.putExtra("date",dates.get(selectedDocId));
                        intent.putExtra("userid",userids.get(selectedDocId));
                        intent.putExtra("prefill_title",titles.get(selectedDocId));
                        intent.putExtra("prefill_writer",writers.get(selectedDocId));
                        intent.putStringArrayListExtra("prefill_locations",locations.get(selectedDocId));
                        startActivity(intent);

                    }
                });
            }catch (Exception e){
                e.printStackTrace();
            }


            TextView title=view.findViewById(R.id.mate_title);
            TextView datetxt=view.findViewById(R.id.mate_date);
            TextView writetime=view.findViewById(R.id.mate_writetime);
            TextView writer=view.findViewById(R.id.mate_writer);

            title.setText(titles.get(myRequestDocIds.get(position)));
            datetxt.setText(dates.get(myRequestDocIds.get(position)));
            String timetmp=writetimes.get(myRequestDocIds.get(position));
            writetime.setText(String.format("%s/%s/%s %s:%s",timetmp.substring(0,4),timetmp.substring(4,6),timetmp.substring(6,8),timetmp.substring(8,10),timetmp.substring(10,12)));
            writer.setText(writers.get(myRequestDocIds.get(position)));

            return view;

        }
    }

    private static final class MatePager {
        private final int pageSize;

        MatePager(int pageSize) {
            this.pageSize = pageSize;
        }

        ArrayList<String> filterMateDocIds(
                ArrayList<String> mateDocIds,
                String keyword,
                int searchMode,
                HashMap<String, String> writers,
                HashMap<String, String> titles
        ) {
            ArrayList<String> result = new ArrayList<>();
            String normalizedKeyword = keyword == null ? "" : keyword.trim();
            if (normalizedKeyword.isEmpty()) {
                result.addAll(mateDocIds);
                return result;
            }

            String lowerKeyword = normalizedKeyword.toLowerCase();
            for (String docId : mateDocIds) {
                String source = searchMode == 0 ? writers.get(docId) : titles.get(docId);
                if (source != null && source.toLowerCase().contains(lowerKeyword)) {
                    result.add(docId);
                }
            }
            return result;
        }

        int pageCount(int totalItems) {
            if (totalItems <= 0) {
                return 1;
            }
            return (totalItems + pageSize - 1) / pageSize;
        }

        int clampPage(int currentPage, int totalPages) {
            if (currentPage < 0) {
                return 0;
            }
            if (currentPage > totalPages - 1) {
                return totalPages - 1;
            }
            return currentPage;
        }

        int pageItemCount(int totalItems, int currentPage) {
            if (totalItems <= 0) {
                return 0;
            }
            int start = currentPage * pageSize;
            return Math.min(pageSize, totalItems - start);
        }

        int toRealPosition(int page, int positionInPage) {
            return page * pageSize + positionInPage;
        }
    }
}


