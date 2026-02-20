package com.example.walkingmate.feature.shop.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.walkingmate.R;

public class CoinShopActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_coin);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.coinShopContainer, new CoinShopProfile1())
                    .commit();
        }
    }
}

