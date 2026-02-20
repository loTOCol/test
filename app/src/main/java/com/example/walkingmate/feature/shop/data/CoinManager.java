package com.example.walkingmate.feature.shop.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class CoinManager {
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    private static ArrayList<Integer> authorizedGifs = new ArrayList<>();

    public static void initialize(Context context) {
        sharedPreferences = context.getSharedPreferences("CoinShopPrefs", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        updateAuthorizedGifs(); // 초기화 시점에 authorizedGifs 업데이트
    }

    public static boolean hasEnoughCoins(int price) {
        int currentCoins = sharedPreferences.getInt("current_coins", 0);
        return currentCoins >= price;
    }

    public static void useCoins(int price) {
        int currentCoins = sharedPreferences.getInt("current_coins", 0);
        editor.putInt("current_coins", currentCoins - price);
        editor.apply();
    }
    public static void addPurchasedGif(int gifResId) {
        Set<String> purchasedGifs = sharedPreferences.getStringSet("purchased_gifs", new HashSet<>());
        purchasedGifs.add(String.valueOf(gifResId));
        editor.putStringSet("purchased_gifs", purchasedGifs);
        editor.apply();
        updateAuthorizedGifs(); // gif 추가 후 authorizedGifs 업데이트
    }

    public static int getCoins() {

        return sharedPreferences.getInt("current_coins", 0);
    }
    public static void clearPurchasedGifs() {
        Set<String> purchasedGifs = new HashSet<>();
        editor.putStringSet("purchased_gifs", purchasedGifs);
        editor.apply();
    }
    public static void updateAuthorizedGifs() {
        authorizedGifs.clear();
        Set<String> purchasedGifs = getPurchasedGifs();
        for (String gif : purchasedGifs) {
            authorizedGifs.add(Integer.parseInt(gif));
        }
    }
    public static ArrayList<Integer> getAuthorizedGifs() {
        return new ArrayList<>(authorizedGifs);
    }

    public static boolean hasPurchasedGif(int gifResId) {
        Set<String> purchasedGifs = sharedPreferences.getStringSet("purchased_gifs", new HashSet<>());
        return purchasedGifs.contains(String.valueOf(gifResId));
    }
    public static boolean isGifPurchased(int gifResId) {
        Set<String> purchasedGifs = sharedPreferences.getStringSet("purchased_gifs", new HashSet<>());
        return purchasedGifs.contains(String.valueOf(gifResId));
    }
    public static Set<String> getPurchasedGifs() {
        return sharedPreferences.getStringSet("purchased_gifs", new HashSet<>());
    }
    public static void addCoins(int amount) {
        int currentCoins = sharedPreferences.getInt("current_coins", 0);
        editor.putInt("current_coins", currentCoins + amount);
        editor.apply();
    }
    public static void addPurchasedEmoji(int emojiResId) {
        Set<String> purchasedEmojis = sharedPreferences.getStringSet("purchased_emojis", new HashSet<>());
        purchasedEmojis.add(String.valueOf(emojiResId));
        editor.putStringSet("purchased_emojis", purchasedEmojis);
        editor.apply();
    }

    public static boolean isEmojiPurchased(int emojiResId) {
        Set<String> purchasedEmojis = sharedPreferences.getStringSet("purchased_emojis", new HashSet<>());
        return purchasedEmojis.contains(String.valueOf(emojiResId));
    }

    public static ArrayList<Integer> getPurchasedEmojis() {
        Set<String> purchasedEmojis = sharedPreferences.getStringSet("purchased_emojis", new HashSet<>());
        ArrayList<Integer> emojiList = new ArrayList<>();
        for (String emoji : purchasedEmojis) {
            emojiList.add(Integer.parseInt(emoji));
        }
        return emojiList;
    }
}

