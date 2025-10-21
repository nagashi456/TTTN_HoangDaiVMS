package com.example.tttn_hoangdaivms.Request;

import java.io.Serializable;

public class RequestModel implements Serializable {
    private int userId; // 🆔 ID người dùng
    private String name, cccd, phone, email, status, date;

    // Constructor đầy đủ
    public RequestModel(int userId, String name, String cccd, String phone, String email, String status, String date) {
        this.userId = userId;
        this.name = name;
        this.cccd = cccd;
        this.phone = phone;
        this.email = email;
        this.status = status;
        this.date = date;
    }

    // --- Getter ---
    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name;
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

    public String getStatus() {
        return status;
    }

    public String getDate() {
        return date;
    }

    // --- Setter ---
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCccd(String cccd) {
        this.cccd = cccd;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
