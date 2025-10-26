package com.example.tttn_hoangdaivms.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * Database helper - updated to match schema:
 * TaiKhoan (MaTaiKhoan, Email, MatKhau)
 * NguoiDung (MaNguoiDung, MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai)
 * SucKhoe (MaSucKhoe, MaNguoiDung, ChieuCao, CanNang, BenhNen, NgayKham, MaTuy, KetLuan)
 * BangCap (MaBangCap, MaTaiXe, Loai, NgayCap, NgayHetHan, NoiCap, TinhTrang, GiayPhep)
 * BaoHiem (MaBaoHiem, MaTaiXe, SoHD, NgayBatDau, NgayKetThuc, CongTy)
 * Xe (MaXe, MaNguoiDung, BienSo, LoaiXe, HangSX, MauSac, SoHieu)
 * BaoTri (MaBaoTri, MaXe, NgayGanNhat, NoiDung, DonVi)
 */
public class Database extends SQLiteOpenHelper {

    private static final String TAG = "Database";
    private static final String DATABASE_NAME = "DriverApp.db";
    // Nếu bạn đã deploy DB cũ, tăng version để onUpgrade chạy; thử với 3
    private static final int DATABASE_VERSION = 3;

    public Database(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // Bật khóa ngoại cho tất cả kết nối
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tạo bảng theo thứ tự đúng để các FK hợp lệ

        // 1) TaiKhoan
        db.execSQL("CREATE TABLE IF NOT EXISTS TaiKhoan (" +
                "MaTaiKhoan INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "Email TEXT UNIQUE NOT NULL, " +
                "MatKhau TEXT NOT NULL" +
                ");");

        // 2) NguoiDung (tham chiếu MaTaiKhoan)
        db.execSQL("CREATE TABLE IF NOT EXISTS NguoiDung (" +
                "MaNguoiDung INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "MaTaiKhoan INTEGER NOT NULL, " +
                "HoTen TEXT NOT NULL, " +
                "NgaySinh TEXT, " +
                "GioiTinh TEXT, " +
                "CCCD TEXT UNIQUE, " +
                "SDT TEXT, " +
                "VaiTro TEXT, " +
                "TrangThai TEXT DEFAULT 'Đang duyệt', " +
                "FOREIGN KEY (MaTaiKhoan) REFERENCES TaiKhoan(MaTaiKhoan) ON DELETE CASCADE ON UPDATE CASCADE" +
                ");");

        // 3) Xe (tham chiếu MaNguoiDung)
        db.execSQL("CREATE TABLE IF NOT EXISTS Xe (" +
                "MaXe INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "MaNguoiDung INTEGER, " +              // người sở hữu/đăng ký xe (nullable)
                "BienSo TEXT UNIQUE NOT NULL, " +
                "LoaiXe TEXT, " +
                "HangSX TEXT, " +
                "MauSac TEXT, " +
                "SoHieu TEXT, " +
                "FOREIGN KEY (MaNguoiDung) REFERENCES NguoiDung(MaNguoiDung) ON DELETE SET NULL ON UPDATE CASCADE" +
                ");");

        // 4) SucKhoe (tham chiếu MaNguoiDung)
        db.execSQL("CREATE TABLE IF NOT EXISTS SucKhoe (" +
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

        // 5) BangCap (MaTaiXe -> MaNguoiDung)
        db.execSQL("CREATE TABLE IF NOT EXISTS BangCap (" +
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

        // 6) BaoHiem (MaTaiXe -> MaNguoiDung)
        db.execSQL("CREATE TABLE IF NOT EXISTS BaoHiem (" +
                "MaBaoHiem INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "MaTaiXe INTEGER NOT NULL, " +
                "SoHD TEXT, " +
                "NgayBatDau TEXT, " +
                "NgayKetThuc TEXT, " +
                "CongTy TEXT, " +
                "FOREIGN KEY (MaTaiXe) REFERENCES NguoiDung(MaNguoiDung) ON DELETE CASCADE ON UPDATE CASCADE" +
                ");");

        // 7) BaoTri (MaXe -> MaXe)
        db.execSQL("CREATE TABLE IF NOT EXISTS BaoTri (" +
                "MaBaoTri INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "MaXe INTEGER NOT NULL, " +
                "NgayGanNhat TEXT, " +
                "NoiDung TEXT, " +
                "DonVi TEXT, " +
                "FOREIGN KEY (MaXe) REFERENCES Xe(MaXe) ON DELETE CASCADE ON UPDATE CASCADE" +
                ");");

        // Dữ liệu mặc định: admin (chỉ khi chưa có)
        db.execSQL("INSERT OR IGNORE INTO TaiKhoan (MaTaiKhoan, Email, MatKhau) VALUES (1, 'admin@vms.com', '123456');");
        db.execSQL("INSERT OR IGNORE INTO NguoiDung (MaNguoiDung, MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai) " +
                "VALUES (1, 1, 'Quản trị viên hệ thống', '1990-01-01', 'Nam', '0123456789', '0909123456', 'Admin', 'Đã duyệt');");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Đơn giản: drop + create (dev only). Trong production cần migrate.
        db.execSQL("DROP TABLE IF EXISTS BaoTri");
        db.execSQL("DROP TABLE IF EXISTS BaoHiem");
        db.execSQL("DROP TABLE IF EXISTS BangCap");
        db.execSQL("DROP TABLE IF EXISTS SucKhoe");
        db.execSQL("DROP TABLE IF EXISTS Xe");
        db.execSQL("DROP TABLE IF EXISTS NguoiDung");
        db.execSQL("DROP TABLE IF EXISTS TaiKhoan");
        onCreate(db);
    }

    // ---------------------------
    // CRUD / helper methods
    // ---------------------------

    /**
     * Kiểm tra credentials (email + password)
     */
    public boolean validateCredentials(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT MaTaiKhoan FROM TaiKhoan WHERE Email = ? AND MatKhau = ?", new String[]{email, password});
            return cursor != null && cursor.moveToFirst();
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /**
     * Lấy trạng thái người dùng (TrangThai) theo email.
     */
    public String getUserStatus(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT ND.TrangThai FROM NguoiDung ND JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan WHERE TK.Email = ?",
                    new String[]{email});
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
            return null;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /**
     * Kiểm tra login hợp lệ và đã được duyệt.
     */
    public boolean isApprovedLogin(String email, String password) {
        if (!validateCredentials(email, password)) return false;
        String status = getUserStatus(email);
        return status != null && status.trim().equalsIgnoreCase("Đã duyệt");
    }

    /**
     * Lấy vai trò người dùng theo email.
     */
    public String getUserRole(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT ND.VaiTro FROM NguoiDung ND JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan WHERE TK.Email = ?",
                    new String[]{email});
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
            return null;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /**
     * Thêm người dùng mới (tạo TaiKhoan + NguoiDung).
     */
    public boolean insertNguoiDung(String email, String matKhau, String hoTen,
                                   String ngaySinh, String gioiTinh, String cccd,
                                   String sdt, String vaiTro, String trangThai) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = null;
        db.beginTransaction();
        try {
            // kiểm tra email tồn tại
            cursor = db.rawQuery("SELECT MaTaiKhoan FROM TaiKhoan WHERE Email = ?", new String[]{email});
            if (cursor != null && cursor.moveToFirst()) {
                return false; // email đã tồn tại
            }
            if (cursor != null) { cursor.close(); cursor = null; }

            ContentValues taiKhoanValues = new ContentValues();
            taiKhoanValues.put("Email", email);
            taiKhoanValues.put("MatKhau", matKhau);
            long maTaiKhoan = db.insert("TaiKhoan", null, taiKhoanValues);
            if (maTaiKhoan == -1) {
                return false;
            }

            ContentValues nguoiDungValues = new ContentValues();
            nguoiDungValues.put("MaTaiKhoan", maTaiKhoan);
            nguoiDungValues.put("HoTen", hoTen);
            nguoiDungValues.put("NgaySinh", ngaySinh);
            nguoiDungValues.put("GioiTinh", gioiTinh);
            nguoiDungValues.put("CCCD", cccd);
            nguoiDungValues.put("SDT", sdt);
            nguoiDungValues.put("VaiTro", vaiTro);
            nguoiDungValues.put("TrangThai", trangThai != null ? trangThai : "Đang duyệt");

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
     * Cập nhật trạng thái người dùng theo email.
     */
    public boolean setUserStatus(String email, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("TrangThai", status);
        int updated = db.update("NguoiDung", values,
                "MaTaiKhoan = (SELECT MaTaiKhoan FROM TaiKhoan WHERE Email = ?)",
                new String[]{email});
        return updated > 0;
    }

    public boolean approveUser(String email) {
        return setUserStatus(email, "Đã duyệt");
    }

    public boolean rejectUser(String email) {
        return setUserStatus(email, "Đã từ chối");
    }

    /**
     * Thêm xe kèm bảo trì + bảo hiểm (trong 1 transaction).
     * maTaiXe là MaNguoiDung (chủ xe) - truyền dưới dạng int.
     */
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
            // kiểm tra biển số
            checkCursor = db.rawQuery("SELECT MaXe FROM Xe WHERE BienSo = ?", new String[]{bienSo});
            if (checkCursor != null && checkCursor.moveToFirst()) {
                return false;
            }
            if (checkCursor != null) { checkCursor.close(); checkCursor = null; }

            // insert Xe (gắn MaNguoiDung)
            ContentValues xeValues = new ContentValues();
            xeValues.put("MaNguoiDung", maTaiXe);
            xeValues.put("BienSo", bienSo);
            xeValues.put("LoaiXe", loaiXe);
            xeValues.put("HangSX", hangSX);
            xeValues.put("MauSac", mauSac);
            xeValues.put("SoHieu", soHieu);
            long maXe = db.insert("Xe", null, xeValues);
            if (maXe == -1) return false;

            // insert BaoTri
            ContentValues baoTriValues = new ContentValues();
            baoTriValues.put("MaXe", maXe);
            baoTriValues.put("NgayGanNhat", ngayGanNhatBaoTri);
            baoTriValues.put("NoiDung", noiDungBaoTri);
            baoTriValues.put("DonVi", donViBaoTri);
            long insertBaoTri = db.insert("BaoTri", null, baoTriValues);
            if (insertBaoTri == -1) return false;

            // insert BaoHiem (MaTaiXe = MaNguoiDung)
            ContentValues baoHiemValues = new ContentValues();
            baoHiemValues.put("MaTaiXe", maTaiXe);
            baoHiemValues.put("SoHD", soHDBaoHiem);
            baoHiemValues.put("NgayBatDau", ngayBatDauBaoHiem);
            baoHiemValues.put("NgayKetThuc", ngayKetThucBaoHiem);
            baoHiemValues.put("CongTy", congTyBaoHiem);
            long insertBaoHiem = db.insert("BaoHiem", null, baoHiemValues);
            if (insertBaoHiem == -1) return false;

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

    /**
     * Lấy MaNguoiDung theo email - trả về String (để bạn dễ truyền qua bundle).
     * Trả về null nếu không tìm thấy.
     */
    public String getUserIdByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT ND.MaNguoiDung FROM NguoiDung ND JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan WHERE TK.Email = ?",
                    new String[]{email});
            if (cursor != null && cursor.moveToFirst()) {
                int id = cursor.getInt(0);
                return String.valueOf(id);
            }
            return null;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /**
     * Lấy thông tin người dùng cơ bản theo email -> User object (nếu bạn có class User)
     * Trả về null nếu không tìm thấy.
     */
    public User getUserByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT ND.MaNguoiDung, TK.Email, ND.HoTen, ND.VaiTro, ND.TrangThai, ND.SDT, ND.CCCD " +
                            "FROM NguoiDung ND JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan WHERE TK.Email = ?",
                    new String[]{email});
            if (cursor != null && cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("MaNguoiDung"));
                String mail = cursor.getString(cursor.getColumnIndexOrThrow("Email"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("HoTen"));
                String role = cursor.getString(cursor.getColumnIndexOrThrow("VaiTro"));
                String status = cursor.getString(cursor.getColumnIndexOrThrow("TrangThai"));

                // Bạn cần có constructor User(int id, String email, String name, String role, String status, String sdt, String cccd)
                // Nếu class User khác, hãy chỉnh lại phần tạo object này.
                return new User(id, mail, name, role, status);
            }
            return null;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    // Các helper nhỏ (nếu cần bạn có thể thêm nhiều hàm truy vấn tiện ích)
}
