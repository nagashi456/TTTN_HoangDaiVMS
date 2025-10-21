package com.example.tttn_hoangdaivms.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

public class Database extends SQLiteOpenHelper {

    private static final String TAG = "Database";
    private static final String DATABASE_NAME = "DriverApp.db";
    private static final int DATABASE_VERSION = 2;

    public Database(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Bật hỗ trợ khóa ngoại
        db.execSQL("PRAGMA foreign_keys = ON;");

        // Tạo bảng TaiKhoan
        db.execSQL("CREATE TABLE TaiKhoan (" +
                "MaTaiKhoan INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "Email TEXT UNIQUE NOT NULL, " +
                "MatKhau TEXT NOT NULL" +
                ");");

        // Tạo bảng NguoiDung, thêm cột TrangThai
        db.execSQL("CREATE TABLE NguoiDung (" +
                "MaNguoiDung INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "MaTaiKhoan INTEGER NOT NULL, " +
                "HoTen TEXT NOT NULL, " +
                "NgaySinh TEXT, " +
                "GioiTinh TEXT, " +
                "CCCD TEXT UNIQUE, " +
                "SDT TEXT, " +
                "VaiTro TEXT, " +
                "TrangThai TEXT DEFAULT 'Đang yêu cầu', " +
                "FOREIGN KEY (MaTaiKhoan) REFERENCES TaiKhoan(MaTaiKhoan) ON DELETE CASCADE ON UPDATE CASCADE" +
                ");");

        // Các bảng khác
        db.execSQL("CREATE TABLE SucKhoe (" +
                "MaSucKhoe INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "MaNguoiDung INTEGER NOT NULL, " +
                "ChieuCao REAL, " +
                "CanNang REAL, " +
                "BenhNen TEXT, " +
                "NgayKham TEXT, " +
                "MaTuy INTEGER, " +
                "KetLuan TEXT, " +
                "FOREIGN KEY (MaNguoiDung) REFERENCES NguoiDung(MaNguoiDung) ON DELETE CASCADE ON UPDATE CASCADE" +
                ");");

        db.execSQL("CREATE TABLE BangCap (" +
                "MaBangCap INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "MaTaiXe INTEGER NOT NULL, " +
                "Loai TEXT, " +
                "NgayCap TEXT, " +
                "NgayHetHan TEXT, " +
                "NoiCap TEXT, " +
                "TinhTrang TEXT, " +
                "GiayPhep TEXT, " +
                "FOREIGN KEY (MaTaiXe) REFERENCES NguoiDung(MaNguoiDung) ON DELETE CASCADE ON UPDATE CASCADE" +
                ");");

        db.execSQL("CREATE TABLE BaoHiem (" +
                "MaBaoHiem INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "MaTaiXe INTEGER NOT NULL, " +
                "SoHD TEXT, " +
                "NgayBatDau TEXT, " +
                "NgayKetThuc TEXT, " +
                "CongTy TEXT, " +
                "FOREIGN KEY (MaTaiXe) REFERENCES NguoiDung(MaNguoiDung) ON DELETE CASCADE ON UPDATE CASCADE" +
                ");");

        db.execSQL("CREATE TABLE Xe (" +
                "MaXe INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "BienSo TEXT UNIQUE NOT NULL, " +
                "LoaiXe TEXT, " +
                "HangSX TEXT, " +
                "MauSac TEXT, " +
                "SoHieu TEXT" +
                ");");

        db.execSQL("CREATE TABLE BaoTri (" +
                "MaBaoTri INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "MaXe INTEGER NOT NULL, " +
                "NgayGanNhat TEXT, " +
                "NoiDung TEXT, " +
                "DonVi TEXT, " +
                "FOREIGN KEY (MaXe) REFERENCES Xe(MaXe) ON DELETE CASCADE ON UPDATE CASCADE" +
                ");");

        // Dữ liệu mặc định: admin
        db.execSQL("INSERT INTO TaiKhoan (Email, MatKhau) VALUES ('admin@vms.com', '123456');");
        db.execSQL("INSERT INTO NguoiDung (MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai) " +
                "VALUES (1, 'Quản trị viên hệ thống', '1990-01-01', 'Nam', '0123456789', '0909123456', 'Admin', 'Đã duyệt');");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Xóa và tạo lại (đơn giản). Trong production bạn nên migrate dữ liệu.
        db.execSQL("DROP TABLE IF EXISTS BaoTri");
        db.execSQL("DROP TABLE IF EXISTS Xe");
        db.execSQL("DROP TABLE IF EXISTS BaoHiem");
        db.execSQL("DROP TABLE IF EXISTS BangCap");
        db.execSQL("DROP TABLE IF EXISTS SucKhoe");
        db.execSQL("DROP TABLE IF EXISTS NguoiDung");
        db.execSQL("DROP TABLE IF EXISTS TaiKhoan");
        onCreate(db);
    }

    /**
     * Kiểm tra credentials (email + password) - KHÔNG kiểm tra trạng thái.
     */
    public boolean validateCredentials(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT MaTaiKhoan FROM TaiKhoan WHERE Email = ? AND MatKhau = ?",
                    new String[]{email, password});
            return cursor != null && cursor.moveToFirst();
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /**
     * Lấy trạng thái người dùng (TrangThai) theo email.
     * Trả về null nếu không tìm thấy.
     */
    public String getUserStatus(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT ND.TrangThai FROM NguoiDung ND " +
                            "JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan " +
                            "WHERE TK.Email = ?",
                    new String[]{email});
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            } else {
                return null;
            }
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /**
     * Kiểm tra login hợp lệ và đã được duyệt (tương đương: credentials ok AND status == 'Đã duyệt')
     * Trả true chỉ khi cả hai điều kiện đều đúng.
     */
    public boolean isApprovedLogin(String email, String password) {
        if (!validateCredentials(email, password)) return false;
        String status = getUserStatus(email);
        return status != null && status.trim().equalsIgnoreCase("Đã duyệt");
    }

    /**
     * Lấy vai trò người dùng (VaiTro) theo email.
     * Trả về null nếu không tìm thấy.
     */
    public String getUserRole(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT ND.VaiTro FROM NguoiDung ND " +
                            "JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan " +
                            "WHERE TK.Email = ?",
                    new String[]{email});
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            } else {
                return null;
            }
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /**
     * Thêm người dùng mới:
     * - Tạo record trong TaiKhoan
     * - Tạo record trong NguoiDung (với trangThai truyền vào)
     *
     * Trả về true nếu thành công.
     */
    public boolean insertNguoiDung(String email, String matKhau, String hoTen,
                                   String ngaySinh, String gioiTinh, String cccd,
                                   String sdt, String vaiTro, String trangThai) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = null;
        db.beginTransaction();
        try {
            // Kiểm tra email trùng trong TaiKhoan
            cursor = db.rawQuery("SELECT MaTaiKhoan FROM TaiKhoan WHERE Email = ?", new String[]{email});
            if (cursor.moveToFirst()) {
                return false; // email đã tồn tại
            }
            if (cursor != null) { cursor.close(); cursor = null; }

            // Thêm vào TaiKhoan
            ContentValues taiKhoanValues = new ContentValues();
            taiKhoanValues.put("Email", email);
            taiKhoanValues.put("MatKhau", matKhau);
            long maTaiKhoan = db.insert("TaiKhoan", null, taiKhoanValues);
            if (maTaiKhoan == -1) {
                return false;
            }

            // Thêm vào NguoiDung
            ContentValues nguoiDungValues = new ContentValues();
            nguoiDungValues.put("MaTaiKhoan", maTaiKhoan);
            nguoiDungValues.put("HoTen", hoTen);
            nguoiDungValues.put("NgaySinh", ngaySinh);
            nguoiDungValues.put("GioiTinh", gioiTinh);
            nguoiDungValues.put("CCCD", cccd);
            nguoiDungValues.put("SDT", sdt);
            nguoiDungValues.put("VaiTro", vaiTro);
            nguoiDungValues.put("TrangThai", trangThai != null ? trangThai : "Đang yêu cầu");

            long result = db.insert("NguoiDung", null, nguoiDungValues);
            if (result == -1) {
                return false;
            }

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "insertNguoiDung error: " + e.getMessage(), e);
            return false;
        } finally {
            db.endTransaction();
            if (cursor != null) cursor.close();
        }
    }

    /**
     * Cập nhật trạng thái người dùng theo email (dùng khi admin duyệt/từ chối)
     */
    public boolean setUserStatus(String email, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("TrangThai", status);

        int updated = db.update("NguoiDung",
                values,
                "MaTaiKhoan = (SELECT MaTaiKhoan FROM TaiKhoan WHERE Email = ?)",
                new String[]{email});
        return updated > 0;
    }

    /**
     * Hàm tiện ích: approve (Đã duyệt)
     */
    public boolean approveUser(String email) {
        return setUserStatus(email, "Đã duyệt");
    }

    /**
     * Hàm tiện ích: reject (Đã từ chối)
     */
    //Thêm xe
    public boolean insertXeWithBaoTriAndBaoHiem(String bienSo,
                                                String loaiXe,
                                                String hangSX,
                                                String mauSac,
                                                String soHieu,
                                                String ngayGanNhatBaoTri,
                                                String noiDungBaoTri,
                                                String donViBaoTri,
                                                int maTaiXe,
                                                String soHDBaoHiem,
                                                String ngayBatDauBaoHiem,
                                                String ngayKetThucBaoHiem,
                                                String congTyBaoHiem) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        Cursor checkCursor = null;
        try {
            // 1) Kiểm tra biển số đã tồn tại chưa (tránh lỗi UNIQUE)
            checkCursor = db.rawQuery("SELECT MaXe FROM Xe WHERE BienSo = ?", new String[]{bienSo});
            if (checkCursor.moveToFirst()) {
                // biển số đã tồn tại -> hủy
                return false;
            }
            if (checkCursor != null) { checkCursor.close(); checkCursor = null; }

            // 2) Thêm Xe
            ContentValues xeValues = new ContentValues();
            xeValues.put("BienSo", bienSo);
            xeValues.put("LoaiXe", loaiXe);
            xeValues.put("HangSX", hangSX);
            xeValues.put("MauSac", mauSac);
            xeValues.put("SoHieu", soHieu);

            long maXe = db.insert("Xe", null, xeValues);
            if (maXe == -1) {
                return false;
            }

            // 3) Thêm BaoTri (liên kết bằng MaXe)
            ContentValues baoTriValues = new ContentValues();
            baoTriValues.put("MaXe", maXe);
            baoTriValues.put("NgayGanNhat", ngayGanNhatBaoTri);
            baoTriValues.put("NoiDung", noiDungBaoTri);
            baoTriValues.put("DonVi", donViBaoTri);

            long insertBaoTri = db.insert("BaoTri", null, baoTriValues);
            if (insertBaoTri == -1) {
                return false;
            }

            // 4) Thêm BaoHiem (dùng MaTaiXe / MaNguoiDung)
            ContentValues baoHiemValues = new ContentValues();
            baoHiemValues.put("MaTaiXe", maTaiXe);
            baoHiemValues.put("SoHD", soHDBaoHiem);
            baoHiemValues.put("NgayBatDau", ngayBatDauBaoHiem);
            baoHiemValues.put("NgayKetThuc", ngayKetThucBaoHiem);
            baoHiemValues.put("CongTy", congTyBaoHiem);

            long insertBaoHiem = db.insert("BaoHiem", null, baoHiemValues);
            if (insertBaoHiem == -1) {
                return false;
            }

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "insertXeWithBaoTriAndBaoHiem error: " + e.getMessage(), e);
            return false;
        } finally {
            db.endTransaction();
            if (checkCursor != null) checkCursor.close();
        }
    }
    public boolean rejectUser(String email) {
        return setUserStatus(email, "Đã từ chối");
    }
    public User getUserByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        User user = null;
        try {
            cursor = db.rawQuery(
                    "SELECT ND.MaNguoiDung, TK.Email, ND.HoTen, ND.VaiTro, ND.TrangThai " +
                            "FROM NguoiDung ND " +
                            "JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan " +
                            "WHERE TK.Email = ?",
                    new String[]{email}
            );

            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("MaNguoiDung"));
                String em = cursor.getString(cursor.getColumnIndexOrThrow("Email"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("HoTen"));
                String role = cursor.getString(cursor.getColumnIndexOrThrow("VaiTro"));
                String status = cursor.getString(cursor.getColumnIndexOrThrow("TrangThai"));

                user = new User(id, em, name, role, status);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return user;
    }

}
