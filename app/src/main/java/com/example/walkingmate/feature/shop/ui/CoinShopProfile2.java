package com.example.walkingmate.feature.shop.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.walkingmate.R;
import com.example.walkingmate.feature.user.data.UserData;
import com.example.walkingmate.feature.shop.data.CoinManager;
import com.example.walkingmate.feature.shop.model.Item;

import java.util.ArrayList;

public class CoinShopProfile2 extends Fragment {
    ListView listView;
    ArrayList<Item> listItems;
    ItemAdapter adapter;
    ImageButton back;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_coinshop_profile, container, false);

        CoinManager.initialize(getContext()); // CoinManager 초기화

        back = root.findViewById(R.id.back_coinshopPro);
        back.setOnClickListener(view -> getActivity().finish());

        listView = root.findViewById(R.id.productview);
        listItems = new ArrayList<>();

        // 이모티콘 아이템 추가
        listItems.add(new Item(R.drawable.emoji_4, "10"));
        listItems.add(new Item(R.drawable.emoji_5, "20"));

        adapter = new ItemAdapter(getContext(), listItems, false);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Item selectedItem = listItems.get(position);
            showPurchaseDialog(selectedItem);
        });

        // 유저 이름과 코인 수를 화면 아래에 표시
        updateUserInfo(root);

        return root;
    }

    private void updateUserInfo(View root) {
        TextView userNameTextView = root.findViewById(R.id.userNameTextView);
        TextView coinCountTextView = root.findViewById(R.id.coinCountTextView);

        UserData userData = UserData.loadData(getActivity());
        if (userData != null) {
            userNameTextView.setText(userData.nickname);
            coinCountTextView.setText("x " + CoinManager.getCoins());
        }
    }

    private void showPurchaseDialog(Item item) {
        if (CoinManager.isEmojiPurchased(item.getImageResId())) {
            Toast.makeText(getContext(), "이미 구매한 이모티콘입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("구매 확인")
                .setMessage(item.getPrice() + " 코인입니다. 구매하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> {
                    if (CoinManager.hasEnoughCoins(Integer.parseInt(item.getPrice()))) {
                        CoinManager.useCoins(Integer.parseInt(item.getPrice()));
                        CoinManager.addPurchasedEmoji(item.getImageResId());
                        updateUserCoins();
                        showDialog("감사합니다");
                    } else {
                        showDialog("코인이 부족합니다");
                    }
                })
                .setNegativeButton("아니오", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void showDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(message)
                .setPositiveButton("닫기", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void updateUserCoins() {
        TextView coinCountTextView = getView().findViewById(R.id.coinCountTextView);
        coinCountTextView.setText("x " + CoinManager.getCoins());
        updatePurchasedEmojis();
    }

    private void updatePurchasedEmojis() {
        ArrayList<Integer> purchasedEmojis = CoinManager.getPurchasedEmojis();
        // 구매한 이모티콘 목록을 사용하여 UI를 업데이트하는 로직을 추가할 수 있습니다.
        // 예를 들어, 구매한 이모티콘을 리스트에서 비활성화하는 등의 처리
    }
}
