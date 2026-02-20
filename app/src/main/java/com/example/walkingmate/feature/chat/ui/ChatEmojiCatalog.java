package com.example.walkingmate.feature.chat.ui;

import com.example.walkingmate.R;
import com.example.walkingmate.feature.shop.data.CoinManager;

import java.util.ArrayList;
import java.util.List;

final class ChatEmojiCatalog {
    private static final int[] DEFAULT_EMOJI_RES_IDS = {
            R.drawable.emoji_1,
            R.drawable.emoji_2,
            R.drawable.emoji_3
    };

    private static final int[] PURCHASED_EMOJI_RES_IDS = {
            R.drawable.emoji_4,
            R.drawable.emoji_5
    };

    private ChatEmojiCatalog() {
    }

    static int[] buildAvailableEmojiResIds() {
        List<Integer> emojiResIdsList = new ArrayList<>();
        for (int resId : DEFAULT_EMOJI_RES_IDS) {
            emojiResIdsList.add(resId);
        }
        for (int resId : PURCHASED_EMOJI_RES_IDS) {
            if (CoinManager.isEmojiPurchased(resId)) {
                emojiResIdsList.add(resId);
            }
        }
        int[] emojiResIds = new int[emojiResIdsList.size()];
        for (int i = 0; i < emojiResIdsList.size(); i++) {
            emojiResIds[i] = emojiResIdsList.get(i);
        }
        return emojiResIds;
    }
}
