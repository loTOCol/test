package com.example.walkingmate.feature.shop.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.walkingmate.R;
import com.example.walkingmate.feature.shop.data.CoinManager;
import com.example.walkingmate.feature.shop.model.Item;
import com.example.walkingmate.feature.user.data.UserData;

import java.util.ArrayList;

public class CoinShopProfile1 extends Fragment {
    private ListView listView;
    private ArrayList<Item> listItems;
    private ItemAdapter adapter;
    private ImageButton back;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_coinshop_profile, container, false);

        CoinManager.initialize(getContext());

        back = root.findViewById(R.id.back_coinshopPro);
        back.setOnClickListener(view -> requireActivity().finish());

        listView = root.findViewById(R.id.productview);
        listItems = new ArrayList<>();

        listItems.add(new Item(R.drawable.animal_walk1_with, "1000", "WALING ANIMAL 1", ""));
        listItems.add(new Item(R.drawable.animal_walk2_with, "1000", "WALING ANIMAL 2", ""));
        listItems.add(new Item(R.drawable.animal_walk3_with, "1000", "WALING ANIMAL 3", ""));

        adapter = new ItemAdapter(getContext(), listItems, true);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Item selectedItem = listItems.get(position);
            showPurchaseDialog(selectedItem);
        });

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
        if (CoinManager.isGifPurchased(item.getImageResId())) {
            Toast.makeText(getContext(), "이미 구매한 상품입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_shop_purchase);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView itemView = dialog.findViewById(R.id.txt_purchase_item);
        TextView priceView = dialog.findViewById(R.id.txt_purchase_price);
        TextView cancelView = dialog.findViewById(R.id.btn_purchase_cancel);
        TextView confirmView = dialog.findViewById(R.id.btn_purchase_confirm);

        itemView.setText(item.getTitle());
        priceView.setText("가격: " + item.getPrice() + " 코인");

        cancelView.setOnClickListener(v -> dialog.dismiss());
        confirmView.setOnClickListener(v -> {
            int price = Integer.parseInt(item.getPrice());
            if (CoinManager.hasEnoughCoins(price)) {
                CoinManager.useCoins(price);
                CoinManager.addPurchasedGif(item.getImageResId());
                CoinManager.updateAuthorizedGifs();
                updateCoinsText();
                dialog.dismiss();
                showNoticeDialog("구매 완료", "상품 구매가 완료되었습니다.");
            } else {
                dialog.dismiss();
                showNoticeDialog("코인 부족", "코인이 부족합니다.");
            }
        });

        dialog.show();
    }

    private void updateCoinsText() {
        View root = getView();
        if (root == null) {
            return;
        }
        TextView coinCountTextView = root.findViewById(R.id.coinCountTextView);
        coinCountTextView.setText("x " + CoinManager.getCoins());
        adapter.notifyDataSetChanged();
    }

    private void showNoticeDialog(String title, String message) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_shop_notice);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView titleView = dialog.findViewById(R.id.txt_notice_title);
        TextView messageView = dialog.findViewById(R.id.txt_notice_message);
        TextView closeView = dialog.findViewById(R.id.btn_notice_close);

        titleView.setText(title);
        messageView.setText(message);
        closeView.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}