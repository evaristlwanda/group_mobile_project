package com.example.myapplication;

public class ListItem {
    private final String text;
    private final int imageResId;

    public ListItem(String text, int imageResId) {
        this.text = text;
        this.imageResId = imageResId;
    }

    public String getText() {
        return text;
    }

    public int getImageResId() {
        return imageResId;
    }
}