package com.example.tttn_hoangdaivms.DriverList;

public class DriverListModel {

    private String name;      // Họ tên
    private String dob;       // Ngày sinh
    private String gender;    // Giới tính
    private String cccd;      // Căn cước công dân
    private String phone;     // Số điện thoại
    private String location;     // Số điện thoại

    private String email;     // Email
    private int avatarResId;  // Avatar hiển thị

    public DriverListModel(String name, String dob, String gender,
                           String cccd, String phone, String email,
                           int avatarResId) {
        this.name = name;
        this.dob = dob;
        this.gender = gender;
        this.cccd = cccd;
        this.phone = phone;
        this.email = email;
        this.avatarResId = avatarResId;
    }

    public String getName() {
        return name;
    }

    public String getDob() {
        return dob;
    }

    public String getGender() {
        return gender;
    }

    public String getCccd() {
        return cccd;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }
    public String getLocation() {
        return location;
    }
    public int getAvatarResId() {
        return avatarResId;
    }
}
