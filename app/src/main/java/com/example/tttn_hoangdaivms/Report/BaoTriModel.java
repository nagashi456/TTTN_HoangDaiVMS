package com.example.tttn_hoangdaivms.Report;

public class BaoTriModel {
    public int maBaoTri;
    public int maXe;
    public String bienSo;
    public String ngayBaoTri;
    public String loaiBaoTri;
    public String moTa;
    public double chiPhi;
    public String trangThai;

    public boolean isSelected = false;

    public BaoTriModel(int maBaoTri, int maXe, String bienSo, String ngayBaoTri,
                       String loaiBaoTri, String moTa, double chiPhi, String trangThai) {
        this.maBaoTri = maBaoTri;
        this.maXe = maXe;
        this.bienSo = bienSo;
        this.ngayBaoTri = ngayBaoTri;
        this.loaiBaoTri = loaiBaoTri;
        this.moTa = moTa;
        this.chiPhi = chiPhi;
        this.trangThai = trangThai;
    }
}
