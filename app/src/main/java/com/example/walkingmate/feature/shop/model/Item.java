package com.example.walkingmate.feature.shop.model;

public class Item {
    private final int imageResId;
    private final String price;
    private final String title;
    private final String description;

    public Item(int imageResId, String price) {
        this(imageResId, price, "", "");
    }

    public Item(int imageResId, String price, String title, String description) {
        this.imageResId = imageResId;
        this.price = price;
        this.title = title;
        this.description = description;
    }

    public int getImageResId() {
        return imageResId;
    }

    public String getPrice() {
        return price;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
