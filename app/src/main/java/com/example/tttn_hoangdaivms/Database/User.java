package com.example.tttn_hoangdaivms.Database;


import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable {
    private int maNguoiDung;
    private String email;
    private String hoTen;
    private String vaiTro;
    private String trangThai;

    public User(int maNguoiDung, String email, String hoTen, String vaiTro, String trangThai) {
        this.maNguoiDung = maNguoiDung;
        this.email = email;
        this.hoTen = hoTen;
        this.vaiTro = vaiTro;
        this.trangThai = trangThai;
    }

    protected User(Parcel in) {
        maNguoiDung = in.readInt();
        email = in.readString();
        hoTen = in.readString();
        vaiTro = in.readString();
        trangThai = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public int getMaNguoiDung() { return maNguoiDung; }
    public String getEmail() { return email; }
    public String getHoTen() { return hoTen; }
    public String getVaiTro() { return vaiTro; }
    public String getTrangThai() { return trangThai; }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(maNguoiDung);
        parcel.writeString(email);
        parcel.writeString(hoTen);
        parcel.writeString(vaiTro);
        parcel.writeString(trangThai);
    }
}

