package com.example.tttn_hoangdaivms.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Database helper - updated to include TrangThaiUpdatedAt and DateCreated columns and safer upgrade.
 */
public class Database extends SQLiteOpenHelper {

    private static final String TAG = "Database";
    private static final String DATABASE_NAME = "DriverApp.db";
    // tăng version vì đổi schema (thêm cột TrangThaiUpdatedAt và DateCreated)
    private static final int DATABASE_VERSION = 6;

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

        // 2) NguoiDung (thêm TrangThaiUpdatedAt, DateCreated)
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
                "TrangThaiUpdatedAt TEXT, " +
                "DateCreated TEXT, " +
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
// ---------------------------
// Dữ liệu mẫu (với TrangThaiUpdatedAt và DateCreated cụ thể)
// ---------------------------

// Admin (đã tạo và duyệt từ lâu)
        db.execSQL("INSERT OR IGNORE INTO TaiKhoan (MaTaiKhoan, Email, MatKhau) VALUES (1, 'admin@vms.com', '123456');");
        db.execSQL("INSERT OR IGNORE INTO NguoiDung (MaNguoiDung, MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai, TrangThaiUpdatedAt, DateCreated) " +
                "VALUES (1, 1, 'Quản trị viên hệ thống', '1990-01-01', 'Nam', '0123456789', '0909123456', 'Admin', 'Đã duyệt', '2025-10-01 09:00:00', '2025-10-01 09:00:00');");

// Tài khoản mẫu 2: đã duyệt cách đây vài ngày (older than 24h)
        db.execSQL("INSERT OR IGNORE INTO TaiKhoan (MaTaiKhoan, Email, MatKhau) VALUES (2, 'driver3@vms.com', 'driver123');");
        db.execSQL("INSERT OR IGNORE INTO NguoiDung (MaNguoiDung, MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai, TrangThaiUpdatedAt, DateCreated) " +
                "VALUES (2, 2, 'Phạm Văn D', '1991-03-18', 'Nam', '555666777888', '0911222333', 'Nhân viên', 'Đã duyệt', '2025-11-10 08:30:00', '2025-10-20 14:15:00');");

// Tài khoản mẫu 3: đã duyệt cách đây nhiều ngày (older than 24h)
        db.execSQL("INSERT OR IGNORE INTO TaiKhoan (MaTaiKhoan, Email, MatKhau) VALUES (3, 'driver4@vms.com', 'driver123');");
        db.execSQL("INSERT OR IGNORE INTO NguoiDung (MaNguoiDung, MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai, TrangThaiUpdatedAt, DateCreated) " +
                "VALUES (3, 3, 'Ngô Thị E', '1993-12-22', 'Nữ', '666777888999', '0988111222', 'Nhân viên', 'Đã duyệt', '2025-11-07 10:45:00', '2025-10-25 11:00:00');");

// Tài khoản mẫu 4: vừa mới được duyệt (ví dụ trong vòng 24 giờ -> dùng để test rule)
        db.execSQL("INSERT OR IGNORE INTO TaiKhoan (MaTaiKhoan, Email, MatKhau) VALUES (4, 'staff2@vms.com', 'staff123');");
        db.execSQL("INSERT OR IGNORE INTO NguoiDung (MaNguoiDung, MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai, TrangThaiUpdatedAt, DateCreated) " +
                "VALUES (4, 4, 'Hoàng Văn F', '1989-08-15', 'Nam', '777888999000', '0900777888', 'Nhân viên', 'Đã duyệt', '2025-11-14 07:30:00', '2025-11-02 16:20:00');");

// Xe mẫu
        db.execSQL("INSERT OR IGNORE INTO Xe (MaXe, MaNguoiDung, BienSo, LoaiXe, HangSX, MauSac, SoHieu, NhienLieu, SoKmTong, TrangThai) " +
                "VALUES (1, 2, '51C-444.44', 'Xe tải', 'HINO', 'Vàng', 'HX-04', 'Diesel', 60000, 'Đang sử dụng');");
        db.execSQL("INSERT OR IGNORE INTO Xe (MaXe, MaNguoiDung, BienSo, LoaiXe, HangSX, MauSac, SoHieu, NhienLieu, SoKmTong, TrangThai) " +
                "VALUES (2, 3, '52D-555.55', 'Đầu kéo', 'HYUNDAI', 'Xám', 'DK-05', 'Diesel', 75000, 'Bảo trì');");
        db.execSQL("INSERT OR IGNORE INTO Xe (MaXe, MaNguoiDung, BienSo, LoaiXe, HangSX, MauSac, SoHieu, NhienLieu, SoKmTong, TrangThai) " +
                "VALUES (3, NULL, '53E-666.66', 'Xe tải', 'ISUZU', 'Trắng', 'IS-06', 'Diesel', 45000, 'Sẵn sàng');");

// BaoTri mẫu
        db.execSQL("INSERT OR IGNORE INTO BaoTri (MaBaoTri, MaXe, NgayGanNhat, NoiDung, DonVi) " +
                "VALUES (1, 2, '2025-05-15', 'Thay dầu, kiểm tra lốp', 'Xưởng C');");
        db.execSQL("INSERT OR IGNORE INTO BaoTri (MaBaoTri, MaXe, NgayGanNhat, NoiDung, DonVi) " +
                "VALUES (2, 3, '2024-10-01', 'Thay má phanh, kiểm tra động cơ', 'Xưởng D');");

// BaoHiem mẫu
        db.execSQL("INSERT OR IGNORE INTO BaoHiem (MaBaoHiem, MaXe, SoHD, CongTy, NgayBatDau, NgayKetThuc) " +
                "VALUES (1, 2, 'BH-003', 'GHI Insurance', '2025-02-01', '2026-01-31');");
        db.execSQL("INSERT OR IGNORE INTO BaoHiem (MaBaoHiem, MaXe, SoHD, CongTy, NgayBatDau, NgayKetThuc) " +
                "VALUES (2, 3, 'BH-004', 'JKL Insurance', '2025-03-01', '2026-02-28');");

// SucKhoe mẫu
        db.execSQL("INSERT OR IGNORE INTO SucKhoe (MaSucKhoe, MaNguoiDung, ChieuCao, CanNang, BenhNen, NgayKham, MaTuy, KetLuan) " +
                "VALUES (1, 1, 172, 68, 'Không', '2025-04-01', '0', 'Đạt');");
        db.execSQL("INSERT OR IGNORE INTO SucKhoe (MaSucKhoe, MaNguoiDung, ChieuCao, CanNang, BenhNen, NgayKham, MaTuy, KetLuan) " +
                "VALUES (2, 2, 165, 58, 'Không', '2025-04-05', '0', 'Đạt');");

// BangCap mẫu
        db.execSQL("INSERT OR IGNORE INTO BangCap (MaBangCap, MaNguoiDung, Loai, SoBang, NgayCap, NgayHetHan, NoiCap, TinhTrang) " +
                "VALUES (1, 2, 'FE', 'FE-003', '2021-01-01', '2031-01-01', 'Hà Nội', 'Hợp lệ');");
        db.execSQL("INSERT OR IGNORE INTO BangCap (MaBangCap, MaNguoiDung, Loai, SoBang, NgayCap, NgayHetHan, NoiCap, TinhTrang) " +
                "VALUES (2, 3, 'CE', 'CE-004', '2022-05-01', '2032-05-01', 'Đà Nẵng', 'Hợp lệ');");

// ThietBi mẫu
        db.execSQL("INSERT OR IGNORE INTO ThietBi (MaThietBi, MaXe, GPS_TrangThai, LastThoiDiemPhatHien) " +
                "VALUES (1, 1, 'Online', '2025-11-10 09:00:00');");
        db.execSQL("INSERT OR IGNORE INTO ThietBi (MaThietBi, MaXe, GPS_TrangThai, LastThoiDiemPhatHien) " +
                "VALUES (2, 2, 'Offline', '2025-11-09 15:00:00');");

// PhienLaiXe mẫu
        db.execSQL("INSERT OR IGNORE INTO PhienLaiXe (MaPhien, MaXe, ThoiDiemBatDau, ThoiDiemKetThuc, TongGioLai, TongKmTrongNgay) " +
                "VALUES (1, 1, '2025-11-10 06:30:00', '2025-11-10 16:30:00', 10, 220);");
        db.execSQL("INSERT OR IGNORE INTO PhienLaiXe (MaPhien, MaXe, ThoiDiemBatDau, ThoiDiemKetThuc, TongGioLai, TongKmTrongNgay) " +
                "VALUES (2, 2, '2025-11-09 07:00:00', '2025-11-09 17:00:00', 10, 240);");

// Telemetry mẫu
        db.execSQL("INSERT OR IGNORE INTO Telemetry (MaTelemetry, MaXe, ThoiGian, Lat, Lon, TocDo, ViTri, TrangThaiXe) " +
                "VALUES (1, 1, '2025-11-10 08:30:00', 10.762622, 106.660172, 50, 'Quận 3, TP.HCM', 'Đang chạy');");
        db.execSQL("INSERT OR IGNORE INTO Telemetry (MaTelemetry, MaXe, ThoiGian, Lat, Lon, TocDo, ViTri, TrangThaiXe) " +
                "VALUES (2, 2, '2025-11-09 14:30:00', 16.054406, 108.202167, 0, 'Hải Châu, Đà Nẵng', 'Đỗ');");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Nếu upgrade từ version cũ, cố gắng chỉ ALTER để thêm cột thiếu (giữ dữ liệu)
        try {
            // Kiểm tra bảng NguoiDung có các cột TrangThaiUpdatedAt / DateCreated chưa
            Cursor c = db.rawQuery("PRAGMA table_info(NguoiDung);", null);
            boolean hasTrangThaiUpdatedAt = false;
            boolean hasDateCreated = false;
            if (c != null) {
                while (c.moveToNext()) {
                    String name = c.getString(c.getColumnIndexOrThrow("name"));
                    if ("TrangThaiUpdatedAt".equalsIgnoreCase(name)) hasTrangThaiUpdatedAt = true;
                    if ("DateCreated".equalsIgnoreCase(name)) hasDateCreated = true;
                }
                c.close();
            }
            if (!hasTrangThaiUpdatedAt) {
                db.execSQL("ALTER TABLE NguoiDung ADD COLUMN TrangThaiUpdatedAt TEXT;");
            }
            if (!hasDateCreated) {
                db.execSQL("ALTER TABLE NguoiDung ADD COLUMN DateCreated TEXT;");
            }
        } catch (Exception e) {
            Log.w(TAG, "onUpgrade migration failed, falling back to recreate DB: " + e.getMessage(), e);
            // fallback: drop & recreate (dev)
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

            // set DateCreated = now
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            nguoiDungValues.put("DateCreated", now);

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
     * Cập nhật trạng thái kèm timestamp TrangThaiUpdatedAt.
     */
    public boolean setUserStatus(String email, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("TrangThai", status);

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        values.put("TrangThaiUpdatedAt", now);

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
