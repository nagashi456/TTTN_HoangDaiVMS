package com.example.tttn_hoangdaivms.DriverList;


public class VehicleListModel {
    private String plateNumber;
    private String name;
    private int imageResId;
    private int maXe;
    public VehicleListModel(String plateNumber, String name, int imageResId) {
        this.plateNumber = plateNumber;
        this.name = name;
        this.imageResId = imageResId;
    }

    public int getMaXe() { return maXe; }
    public void setMaXe(int maXe) { this.maXe = maXe; }
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

