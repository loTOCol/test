package com.example.walkingmate.feature.user.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.example.walkingmate.R;

public class HelpInfoActivity extends AppCompatActivity {

    private ImageButton back;

    private final String[] titles = {
            "마이 페이지", "도전 과제", "차단 유저 관리", "프로필 설정", "약속 목록",
            "게시판", "메이트 글쓰기", "기록(도보)", "캘린더/피드", "채팅", "사용자 프로필"
    };

    private final String[] contents = {
            "- 왼쪽 사이드 메뉴에서 계정 기능으로 이동합니다.\n" +
                    "- 코인 샵, 도전 과제, 차단 유저 관리, 프로필 설정, 약속 목록, 도움말, 계정 관리를 열 수 있습니다.\n" +
                    "- 현재 프로필 이미지와 닉네임을 빠르게 확인할 수 있습니다.",

            "- 4개 항목(건강한/믿음직한/꾸준한/사교적인 워커)으로 구성됩니다.\n" +
                    "- 각 항목은 5단계 뱃지로 진행 상황이 표시됩니다.\n" +
                    "- 달성한 칭호는 프로필 설정에서 대표 칭호로 선택할 수 있습니다.",

            "- 내가 차단한 유저 목록을 확인하는 화면입니다.\n" +
                    "- 각 유저 우측의 해제 버튼으로 즉시 차단 해제가 가능합니다.\n" +
                    "- 차단 유저의 게시글/신청은 서비스에서 자동 제외됩니다.",

            "- 프로필 이미지, 닉네임, 대표 칭호를 수정할 수 있습니다.\n" +
                    "- 닉네임은 중복 확인 후 저장하는 방식입니다.\n" +
                    "- 변경 사항은 저장 즉시 앱 전반에 반영됩니다.",

            "- 내가 신청하거나 수락한 약속을 모아 보는 화면입니다.\n" +
                    "- 상태별로 약속을 확인하고 정리할 수 있습니다.\n" +
                    "- 완료된 약속은 리뷰 작성으로 이어져 활동 이력 관리가 가능합니다.",

            "- 메인 게시판에서 메이트 모집 글을 탐색합니다.\n" +
                    "- 필터를 통해 성별/연령/기간/보기 조건을 적용할 수 있습니다.\n" +
                    "- 글 상세에서 메이트 신청, 작성자 프로필 보기, 신청자 목록 확인이 가능합니다.",

            "- + 버튼으로 새 모집 글을 작성합니다.\n" +
                    "- 제목, 출발 시간, 이동 관련 정보와 내용을 입력해 등록합니다.\n" +
                    "- 목적지 추가에서 경로 후보를 넣고 정렬해 글의 이동 흐름을 구성할 수 있습니다.",

            "- 하단 기록 탭에서 산책 측정(걸음, 칼로리, 진행률)을 확인합니다.\n" +
                    "- 목표 걸음 수는 직접 설정할 수 있습니다.\n" +
                    "- 목표 달성 시 코인 보상이 지급되며, 구매한 메인 GIF가 함께 표시됩니다.",

            "- 캘린더에서 날짜별 기록을 확인합니다.\n" +
                    "- + 버튼으로 해당 날짜 기록 작성 화면으로 이동할 수 있습니다.\n" +
                    "- 피드 목록에서 작성된 기록을 열람하고 관리할 수 있습니다.",

            "- 채팅방 목록은 제목/마지막 대화/시간 중심으로 표시됩니다.\n" +
                    "- 채팅방 이름은 변경 가능하며, 나가기 시 확인창이 표시됩니다.\n" +
                    "- 채팅 화면에서 사진 전송, 랭킹 탭, 참가자 프로필 확인을 지원합니다.",

            "- 게시글/채팅에서 상대 프로필로 이동할 수 있습니다.\n" +
                    "- 프로필에서 기본 정보와 활동 지표를 확인합니다.\n" +
                    "- 차단 및 신고 기능은 이 화면에서 사용할 수 있습니다."
    };

    private Button[] btns;
    private ListView listView;
    private HelpAdapter helpAdapter;

    private int curitem = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_info);

        back = findViewById(R.id.back_helpinfo);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Intent getintent = getIntent();
        curitem = getintent.getIntExtra("curitem", 0);
        if (curitem < 0 || curitem >= titles.length) {
            curitem = 0;
        }

        btns = new Button[]{
                findViewById(R.id.mypagebtn), findViewById(R.id.challengebtn), findViewById(R.id.managefriendsbtn),
                findViewById(R.id.setprofilebtn), findViewById(R.id.schedulebtn), findViewById(R.id.walkbtn),
                findViewById(R.id.matebtn), findViewById(R.id.recordbtn), findViewById(R.id.feedbtn),
                findViewById(R.id.chatbtn), findViewById(R.id.profilebtn)
        };

        listView = findViewById(R.id.helplist);
        helpAdapter = new HelpAdapter();
        listView.setAdapter(helpAdapter);

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int section = Math.min(titles.length - 1, firstVisibleItem / 2);
                updateCategoryHighlight(section);
            }
        });

        for (int i = 0; i < btns.length; ++i) {
            int finalI = i;
            btns[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listView.setSelectionFromTop(finalI * 2, 0);
                    updateCategoryHighlight(finalI);
                }
            });
        }

        btns[curitem].performClick();
    }

    private void updateCategoryHighlight(int selectedIndex) {
        for (int i = 0; i < btns.length; ++i) {
            if (i == selectedIndex) {
                btns[i].setBackgroundResource(R.drawable.orangebtn);
                btns[i].setTextColor(getResources().getColor(R.color.white));
            } else {
                btns[i].setBackgroundResource(R.drawable.plusbtn);
                btns[i].setTextColor(getResources().getColor(R.color.black));
            }
        }
    }

    public class HelpAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return titles.length * 2;
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
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return position % 2 == 0 ? 0 : 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int type = getItemViewType(position);
            LayoutInflater inflater = LayoutInflater.from(HelpInfoActivity.this);

            if (type == 0) {
                View view = convertView;
                if (view == null) {
                    view = inflater.inflate(R.layout.item_help_title, parent, false);
                }
                TextView title = view.findViewById(R.id.helptitle_txt);
                title.setText(titles[position / 2]);
                return view;
            } else {
                View view = convertView;
                if (view == null) {
                    view = inflater.inflate(R.layout.item_help_content, parent, false);
                }
                TextView content = view.findViewById(R.id.helpcontent_txt);
                content.setText(contents[position / 2]);
                return view;
            }
        }
    }
}