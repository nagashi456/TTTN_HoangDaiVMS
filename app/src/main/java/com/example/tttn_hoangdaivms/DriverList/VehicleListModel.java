package com.example.tttn_hoangdaivms.DriverList;

public class VehicleListModel {
    private int maXe;
    private String plateNumber;
    private String name;
    private int imageResId;

    // Constructor cũ (giữ cho tương thích)
    public VehicleListModel(String plateNumber, String name, int imageResId) {
        this.plateNumber = plateNumber;
        this.name = name;
        this.imageResId = imageResId;
    }

    // Constructor mới: đầy đủ (cân đối cho chỗ bạn gọi new VehicleListModel(maXe, ...))
    public VehicleListModel(int maXe, String plateNumber, String name, int imageResId) {
        this.maXe = maXe;
        this.plateNumber = plateNumber;
        this.name = name;
        this.imageResId = imageResId;
    }

    // Getter / Setter
    public int getMaXe() {
        return maXe;
    }

    public void setMaXe(int maXe) {
        this.maXe = maXe;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getImageResId() {
        return imageResId;
    }

    public void setImageResId(int imageResId) {
        this.imageResId = imageResId;
    }

    @Override
    public String toString() {
        return "VehicleListModel{" +
                "maXe=" + maXe +
                ", plateNumber='" + plateNumber + '\'' +
                ", name='" + name + '\'' +
                ", imageResId=" + imageResId +
                '}';
    }
}
