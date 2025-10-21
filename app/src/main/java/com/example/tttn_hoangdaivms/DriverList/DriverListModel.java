package com.example.tttn_hoangdaivms.DriverList;

public class DriverListModel {
    private String name;
    private String location;
    private int avatarResId;

    public DriverListModel(String name, String location, int avatarResId) {
        this.name = name;
        this.location = location;
        this.avatarResId = avatarResId;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public int getAvatarResId() {
        return avatarResId;
    }
}