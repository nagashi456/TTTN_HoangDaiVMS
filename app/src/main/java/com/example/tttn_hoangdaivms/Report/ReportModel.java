package com.example.tttn_hoangdaivms.Report;

public class ReportModel {
    public int maPhien;
    public int maXe;
    public String bienSo;
    public String thoiDiemBatDau;
    public String thoiDiemKetThuc;
    public double tongGioLai;
    public double tongKmTrongNgay;

    // Thông tin Xe
    public String loaiXe;
    public String hangSX;
    public String mauSac;
    public String soHieu;
    public String nhienLieu;
    public double soKmTong;
    public String trangThaiXe;

    // Thông tin chủ xe (NguoiDung)
    public String chuXeHoTen;
    public String chuXeSDT;
    public String chuXeCCCD;
    public String chuXeVaiTro;

    public boolean isSelected = false;

    public ReportModel(int maPhien, int maXe, String bienSo, String thoiDiemBatDau,
                      String thoiDiemKetThuc, double tongGioLai, double tongKmTrongNgay) {
        this.maPhien = maPhien;
        this.maXe = maXe;
        this.bienSo = bienSo;
        this.thoiDiemBatDau = thoiDiemBatDau;
        this.thoiDiemKetThuc = thoiDiemKetThuc;
        this.tongGioLai = tongGioLai;
        this.tongKmTrongNgay = tongKmTrongNgay;
    }

    public String getDisplayId() {
        return String.format("H%03d", maPhien);
    }

    public String getStatusText() {
        if (thoiDiemKetThuc == null || thoiDiemKetThuc.trim().isEmpty()) {
            return "Đang Chạy";
        } else {
            return "Hoàn Thành";
        }
    }

    public String getTitle() {
        return "Hành Trình " + getDisplayId() + " – Xe " + (bienSo != null ? bienSo : "");
    }
}
