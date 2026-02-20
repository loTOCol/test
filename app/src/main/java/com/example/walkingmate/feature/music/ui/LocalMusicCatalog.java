package com.example.walkingmate.feature.music.ui;

import com.example.walkingmate.R;

final class LocalMusicCatalog {
    private static final int[][] SONGS = {
            {R.raw.whitechristmas, R.raw.memories, R.raw.sea_bottom_segue, R.raw.peaceful, R.raw.shaboozey},
            {R.raw.deltarune, R.raw.feel, R.raw.flowerdance, R.raw.friday, R.raw.halflife2, R.raw.city, R.raw.lesion, R.raw.nights, R.raw.rusty, R.raw.satomisprings, R.raw.steel, R.raw.super1, R.raw.training},
            {R.raw.tokio, R.raw.deathsmiles, R.raw.demolition, R.raw.casino_park, R.raw.frozen_factory, R.raw.infinite, R.raw.keys, R.raw.metallic, R.raw.nobody, R.raw.space, R.raw.thunderzone2, R.raw.tuning},
            {R.raw.sanguine_fountain, R.raw.hyperspace, R.raw.planet_belligerence, R.raw.gaur_plain}
    };

    private static final String[][] TITLES = {
            {"White Christmas", "Memories of the City", "Sea Bottom Segue", "Peaceful Snow", "Shaboozey"},
            {"Deltarune Chapter", "Feel the Heat", "Flower Dance", "Friday Night Funkin", "Half-Life 2", "In the City", "Lesion X", "Nights Pinball", "Rusty Ruin Zone", "Satomi Springs", "Steel Red", "Super Bell Hill", "Training Forest"},
            {"498 Tokio", "Deathsmiles", "Demolition and Destruction", "Casino Park", "Frozen Factory", "Infinite", "Keys the Ruin", "Metallic Madness Zone", "Nobody", "Space Step Flow", "Thunderzone 2", "Tuning"},
            {"Sanguine Fountain", "Hyperspace", "Planet Belligerence", "Gaur Plain"}
    };

    private static final Integer[][] IMAGES = {
            {R.drawable.chrimas, R.drawable.cheer_sax, R.drawable.sea, R.drawable.cheer_new, R.drawable.guitar},
            {R.drawable.delt, R.drawable.feel, R.drawable.flowerdance, R.drawable.friday, R.drawable.exciting_passion, R.drawable.exciting_rainbow, R.drawable.exciting_passion, R.drawable.exciting_rainbow, R.drawable.exciting_passion, R.drawable.exciting_rainbow, R.drawable.exciting_passion, R.drawable.exciting_rainbow, R.drawable.exciting_rainbow},
            {R.drawable.piano_lullaby, R.drawable.piano_heaven, R.drawable.piano_dream, R.drawable.piano_express, R.drawable.piano_sonata, R.drawable.piano_river, R.drawable.piano_lullaby, R.drawable.piano_heaven, R.drawable.piano_dream, R.drawable.piano_express, R.drawable.piano_sonata, R.drawable.piano_river},
            {R.drawable.comfort_quiet, R.drawable.comfort_winter, R.drawable.comfort_dawn, R.drawable.comfort_rainbow}
    };

    private LocalMusicCatalog() {
    }

    static int[] songsFor(int category) {
        return isValidCategory(category) ? SONGS[category] : new int[0];
    }

    static String[] titlesFor(int category) {
        return isValidCategory(category) ? TITLES[category] : new String[0];
    }

    static Integer[] imagesFor(int category) {
        return isValidCategory(category) ? IMAGES[category] : new Integer[0];
    }

    private static boolean isValidCategory(int category) {
        return category >= 0 && category < SONGS.length;
    }
}
