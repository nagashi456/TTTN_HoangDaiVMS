package com.example.tttn_hoangdaivms.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * Database helper - updated to match new schema and include SucKhoe table.
 */
public class Database extends SQLiteOpenHelper {

    private static final String TAG = "Database";
    private static final String DATABASE_NAME = "DriverApp.db";
    // tăng version vì đổi schema
    private static final int DATABASE_VERSION = 4;

    public Database(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1) TaiKhoan
        db.execSQL("CREATE TABLE IF NOT EXISTS TaiKhoan (" +
                "MaTaiKhoan INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "Email TEXT UNIQUE NOT NULL, " +
                "MatKhau TEXT NOT NULL" +
                ");");

        // 2) NguoiDung
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

        // 3) Xe (mở rộng cột)
        db.execSQL("CREATE TABLE IF NOT EXISTS Xe (" +
                "MaXe INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "MaNguoiDung INTEGER, " + // nullable chủ sở hữu/đăng ký xe
                "BienSo TEXT UNIQUE NOT NULL, " +
                "LoaiXe TEXT, " +
                "HangSX TEXT, " +
                "MauSac TEXT, " +
                "SoHieu TEXT, " +
                "NhienLieu TEXT, " +
                "SoKmTong REAL DEFAULT 0, " +
                "TrangThai TEXT DEFAULT '', " +
                "FOREIGN KEY (MaNguoiDung) REFERENCES NguoiDung(MaNguoiDung) ON DELETE SET NULL ON UPDATE CASCADE" +
                ");");

        // 4) SucKhoe (mới)
        db.execSQL("CREATE TABLE IF NOT EXISTS SucKhoe (" +
                "MaSucKhoe INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "MaNguoiDung INTEGER NOT NULL, " +
                "ChieuCao REAL, " +
                "CanNang REAL, " +
                "BenhNen TEXT, " +
                "NgayKham TEXT, " +
                "MaTuy TEXT, " +
                "KetLuan TEXT, " +
                "FOREIGN KEY (MaNguoiDung) REFERENCES NguoiDung(MaNguoiDung) ON DELETE CASCADE ON UPDATE CASCADE" +
                ");");

        // 5) BangCap (bây giờ tham chiếu MaNguoiDung)
        db.execSQL("CREATE TABLE IF NOT EXISTS BangCap (" +
                "MaBangCap INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "MaNguoiDung INTEGER NOT NULL, " +
                "Loai TEXT, " +
                "SoBang TEXT, " +
                "NgayCap TEXT, " +
                "NgayHetHan TEXT, " +
                "NoiCap TEXT, " +
                "TinhTrang TEXT, " +
                "FOREIGN KEY (MaNguoiDung) REFERENCES NguoiDung(MaNguoiDung) ON DELETE CASCADE ON UPDATE CASCADE" +
                ");");

        // 6) BaoHiem (tham chiếu MaXe)
        db.execSQL("CREATE TABLE IF NOT EXISTS BaoHiem (" +
                "MaBaoHiem INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "MaXe INTEGER NOT NULL, " +
                "SoHD TEXT, " +
                "CongTy TEXT, " +
                "NgayBatDau TEXT, " +
                "NgayKetThuc TEXT, " +
                "FOREIGN KEY (MaXe) REFERENCES Xe(MaXe) ON DELETE CASCADE ON UPDATE CASCADE" +
                ");");

        // 7) BaoTri (MaXe)
        db.execSQL("CREATE TABLE IF NOT EXISTS BaoTri (" +
                "MaBaoTri INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "MaXe INTEGER NOT NULL, " +
                "NgayGanNhat TEXT, " +
                "NoiDung TEXT, " +
                "DonVi TEXT, " +
                "FOREIGN KEY (MaXe) REFERENCES Xe(MaXe) ON DELETE CASCADE ON UPDATE CASCADE" +
                ");");

        // 8) ThietBi
        db.execSQL("CREATE TABLE IF NOT EXISTS ThietBi (" +
                "MaThietBi INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "MaXe INTEGER NOT NULL, " +
                "GPS_TrangThai TEXT, " +
                "LastThoiDiemPhatHien TEXT, " +
                "FOREIGN KEY (MaXe) REFERENCES Xe(MaXe) ON DELETE CASCADE ON UPDATE CASCADE" +
                ");");

        // 9) PhienLaiXe
        db.execSQL("CREATE TABLE IF NOT EXISTS PhienLaiXe (" +
                "MaPhien INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "MaXe INTEGER NOT NULL, " +
                "ThoiDiemBatDau TEXT, " +
                "ThoiDiemKetThuc TEXT, " +
                "TongGioLai REAL, " +
                "TongKmTrongNgay REAL, " +
                "FOREIGN KEY (MaXe) REFERENCES Xe(MaXe) ON DELETE CASCADE ON UPDATE CASCADE" +
                ");");

        // 10) Telemetry
        db.execSQL("CREATE TABLE IF NOT EXISTS Telemetry (" +
                "MaTelemetry INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "MaXe INTEGER NOT NULL, " +
                "ThoiGian TEXT, " +
                "Lat REAL, " +
                "Lon REAL, " +
                "TocDo REAL, " +
                "ViTri TEXT, " +
                "TrangThaiXe TEXT, " +
                "FOREIGN KEY (MaXe) REFERENCES Xe(MaXe) ON DELETE CASCADE ON UPDATE CASCADE" +
                ");");

        // Dữ liệu mặc định admin
        db.execSQL("INSERT OR IGNORE INTO TaiKhoan (MaTaiKhoan, Email, MatKhau) VALUES (1, 'admin@vms.com', '123456');");
        db.execSQL("INSERT OR IGNORE INTO NguoiDung (MaNguoiDung, MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai) " +
                "VALUES (1, 1, 'Quản trị viên hệ thống', '1990-01-01', 'Nam', '0123456789', '0909123456', 'Admin', 'Đã duyệt');");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Dev-only: drop tất cả và tạo lại. (Production: viết migration)
        db.execSQL("DROP TABLE IF EXISTS Telemetry");
        db.execSQL("DROP TABLE IF EXISTS PhienLaiXe");
        db.execSQL("DROP TABLE IF EXISTS ThietBi");
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

    public boolean isApprovedLogin(String email, String password) {
        if (!validateCredentials(email, password)) return false;
        String status = getUserStatus(email);
        return status != null && status.trim().equalsIgnoreCase("Đã duyệt");
    }

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

    public boolean insertNguoiDung(String email, String matKhau, String hoTen,
                                   String ngaySinh, String gioiTinh, String cccd,
                                   String sdt, String vaiTro, String trangThai) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = null;
        db.beginTransaction();
        try {
            cursor = db.rawQuery("SELECT MaTaiKhoan FROM TaiKhoan WHERE Email = ?", new String[]{email});
            if (cursor != null && cursor.moveToFirst()) {
                return false;
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
     * Thêm xe kèm báo trì + bảo hiểm (trong 1 transaction).
     * maNguoiDung là MaNguoiDung (chủ xe) - truyền dưới dạng int.
     * Lưu ý: BaoHiem bây giờ tham chiếu MaXe (không phải MaNguoiDung).
     */
    public boolean insertXeWithBaoTriAndBaoHiem(String bienSo,
                                                String loaiXe,
                                                String hangSX,
                                                String mauSac,
                                                String soHieu,
                                                String nhienLieu,
                                                Double soKmTong,
                                                String trangThaiXe,
                                                String ngayGanNhatBaoTri,
                                                String noiDungBaoTri,
                                                String donViBaoTri,
                                                int maNguoiDung,
                                                String soHDBaoHiem,
                                                String congTyBaoHiem,
                                                String ngayBatDauBaoHiem,
                                                String ngayKetThucBaoHiem) {
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

            // insert Xe
            ContentValues xeValues = new ContentValues();
            xeValues.put("MaNguoiDung", maNguoiDung);
            xeValues.put("BienSo", bienSo);
            xeValues.put("LoaiXe", loaiXe);
            xeValues.put("HangSX", hangSX);
            xeValues.put("MauSac", mauSac);
            xeValues.put("SoHieu", soHieu);
            xeValues.put("NhienLieu", nhienLieu);
            if (soKmTong != null) xeValues.put("SoKmTong", soKmTong);
            if (trangThaiXe != null) xeValues.put("TrangThai", trangThaiXe);

            long maXe = db.insert("Xe", null, xeValues);
            if (maXe == -1) return false;

            // insert BaoTri (gắn MaXe)
            ContentValues baoTriValues = new ContentValues();
            baoTriValues.put("MaXe", maXe);
            baoTriValues.put("NgayGanNhat", ngayGanNhatBaoTri);
            baoTriValues.put("NoiDung", noiDungBaoTri);
            baoTriValues.put("DonVi", donViBaoTri);
            long insertBaoTri = db.insert("BaoTri", null, baoTriValues);
            if (insertBaoTri == -1) return false;

            // insert BaoHiem (bây giờ dùng MaXe)
            ContentValues baoHiemValues = new ContentValues();
            baoHiemValues.put("MaXe", maXe);
            baoHiemValues.put("SoHD", soHDBaoHiem);
            baoHiemValues.put("CongTy", congTyBaoHiem);
            baoHiemValues.put("NgayBatDau", ngayBatDauBaoHiem);
            baoHiemValues.put("NgayKetThuc", ngayKetThucBaoHiem);
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
                return new User(id, mail, name, role, status);
            }
            return null;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public Cursor getXeById(int maXe) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT MaXe, MaNguoiDung, BienSo, LoaiXe, HangSX, MauSac, SoHieu, NhienLieu, SoKmTong, TrangThai FROM Xe WHERE MaXe = ?",
                new String[]{String.valueOf(maXe)});
    }

    public Cursor getLatestBaoTriByMaXe(int maXe) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT MaBaoTri, NgayGanNhat, NoiDung, DonVi FROM BaoTri WHERE MaXe = ? ORDER BY NgayGanNhat DESC, MaBaoTri DESC LIMIT 1",
                new String[]{String.valueOf(maXe)});
    }

    public Cursor getLatestBaoHiemByMaXe(int maXe) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT MaBaoHiem, SoHD, CongTy, NgayBatDau, NgayKetThuc FROM BaoHiem WHERE MaXe = ? ORDER BY MaBaoHiem DESC LIMIT 1",
                new String[]{String.valueOf(maXe)});
    }

    /**
     * Lấy cursor SucKhoe mới nhất cho MaNguoiDung (nếu cần)
     */
    public Cursor getLatestSucKhoeByMaNguoiDung(int maNguoiDung) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT MaSucKhoe, ChieuCao, CanNang, BenhNen, NgayKham, MaTuy, KetLuan " +
                        "FROM SucKhoe WHERE MaNguoiDung = ? ORDER BY NgayKham DESC, MaSucKhoe DESC LIMIT 1",
                new String[]{String.valueOf(maNguoiDung)});
    }

    /**
     * Lấy danh sách tài xế / nhân viên (cursor) để dùng cho dropdown.
     * Lọc dựa vào VaiTro chứa 'tài' hoặc 'nhân viên' (case-insensitive).
     */
    public Cursor getDriversCursor() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT MaNguoiDung, HoTen FROM NguoiDung " +
                        "WHERE lower(COALESCE(VaiTro,'')) LIKE ? " +
                        "OR lower(COALESCE(VaiTro,'')) LIKE ? " +
                        "OR lower(COALESCE(VaiTro,'')) LIKE ? " +
                        "OR lower(COALESCE(VaiTro,'')) LIKE ?",
                new String[]{"%tài%", "%tài xế%", "%nhân viên%", "%nhan vien%"}
        );
    }

    // Bạn có thể thêm helper khác (insertTelemetry, insertPhienLai, getTelemetriesForXe, ...) nếu cần.
}
