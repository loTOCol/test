package com.example.walkingmate.feature.chat.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.walkingmate.R;
import com.example.walkingmate.feature.misc.fragment.RankingFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class LastFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ViewPagerAdapter viewPagerAdapter;

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_last, container, false);

        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);

        // ViewPager2에 Fragment 어댑터 설정
        viewPagerAdapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(viewPagerAdapter);

        // TabLayout과 ViewPager2 연결
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("채팅");
            } else if (position == 1) {
                tab.setText("랭킹");
            }
        }).attach();

        return view;
    }

    // ViewPager2 어댑터
    private static class ViewPagerAdapter extends FragmentStateAdapter {
        public ViewPagerAdapter(Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // 첫 번째 탭이면 ChatFragment, 두 번째 탭이면 RankingFragment를 반환
            if (position == 0) {
                return new ChatFragment();  // 첫 번째 탭: ChatFragment
            } else if (position == 1) {
                return new RankingFragment();  // 두 번째 탭: RankingFragment
            } else {
                // 혹시 position이 0이나 1이 아니게 된다면 (예: 이상 상황)
                throw new IllegalStateException("Unexpected position: " + position);
            }
        }

        @Override
        public int getItemCount() {
            return 2;  // 총 탭의 개수
        }
    }

}
