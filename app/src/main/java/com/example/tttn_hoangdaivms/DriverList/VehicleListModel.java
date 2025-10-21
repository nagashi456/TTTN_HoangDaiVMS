package com.example.tttn_hoangdaivms.DriverList;


public class VehicleListModel {
    private String plateNumber;
    private String name;
    private int imageResId;

    public VehicleListModel(String plateNumber, String name, int imageResId) {
        this.plateNumber = plateNumber;
        this.name = name;
        this.imageResId = imageResId;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public String getName() {
        return name;
    }

    public int getImageResId() {
        return imageResId;
    }
}

