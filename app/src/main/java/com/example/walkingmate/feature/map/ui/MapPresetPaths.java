package com.example.walkingmate.feature.map.ui;

import com.naver.maps.geometry.LatLng;

final class MapPresetPaths {
    private static final LatLng[] PARKING_LOT_PATH = new LatLng[]{
            new LatLng(37.3767503, 126.7814504),
            new LatLng(37.3768867, 126.7816154),
            new LatLng(37.3771790, 126.7813542),
            new LatLng(37.3770034, 126.7811158),
            new LatLng(37.3767644, 126.7813203)
    };

    private static final LatLng[] BACKYARD_PATH = new LatLng[]{
            new LatLng(37.3769091, 126.7817273),
            new LatLng(37.3767708, 126.7817904),
            new LatLng(37.3764588, 126.7813180)
    };

    private static final LatLng[] SHORTCUT_PATH = new LatLng[]{
            new LatLng(37.3757768, 126.7808613),
            new LatLng(37.3776753, 126.7786384)
    };

    private static final LatLng[] BUS_STOP_SHORTCUT_PATH = new LatLng[]{
            new LatLng(37.3776431, 126.7804788),
            new LatLng(37.3771457, 126.7849591)
    };

    private MapPresetPaths() {
    }

    static LatLng[][] all() {
        return new LatLng[][]{
                PARKING_LOT_PATH,
                BACKYARD_PATH,
                SHORTCUT_PATH,
                BUS_STOP_SHORTCUT_PATH
        };
    }
}
