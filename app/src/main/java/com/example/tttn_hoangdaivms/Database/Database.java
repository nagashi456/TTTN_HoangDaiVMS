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
// Dữ liệu mẫu
// ---------------------------

// Admin (đã tạo và duyệt từ lâu)
        db.execSQL("INSERT OR IGNORE INTO TaiKhoan (MaTaiKhoan, Email, MatKhau) VALUES (1, 'admin@vms.com', '123456');");
        db.execSQL("INSERT OR IGNORE INTO NguoiDung (MaNguoiDung, MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai, TrangThaiUpdatedAt, DateCreated) " +
                "VALUES (1, 1, 'Quản trị viên hệ thống', '01/01/1991', 'Nam', '0123456789', '0909123456', 'Admin', 'Đã duyệt', '01/10/2025 09:00:00', '2025-10-01 09:00:00');");

// Tài khoản mẫu 2: đã duyệt cách đây vài ngày (older than 24h)
        db.execSQL("INSERT OR IGNORE INTO TaiKhoan (MaTaiKhoan, Email, MatKhau) VALUES (2, 'driver3@vms.com', 'driver123');");
        db.execSQL("INSERT OR IGNORE INTO NguoiDung (MaNguoiDung, MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai, TrangThaiUpdatedAt, DateCreated) " +
                "VALUES (2, 2, 'Phạm Văn D', '18/03/1991', 'Nam', '555666777888', '0911222333', 'Nhân viên', 'Đã duyệt', '10/11/2025 08:30:00', '20/10/2025 14:15:00');");

// Tài khoản mẫu 3: đã duyệt cách đây nhiều ngày (older than 24h)
        db.execSQL("INSERT OR IGNORE INTO TaiKhoan (MaTaiKhoan, Email, MatKhau) VALUES (3, 'driver4@vms.com', 'driver123');");
        db.execSQL("INSERT OR IGNORE INTO NguoiDung (MaNguoiDung, MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai, TrangThaiUpdatedAt, DateCreated) " +
                "VALUES (3, 3, 'Ngô Thị E', '22/12/1993', 'Nữ', '666777888999', '0988111222', 'Nhân viên', 'Đã duyệt', '07/11/2025 10:45:00', '25/10/2025 11:00:00');");

// Tài khoản mẫu 4: vừa mới được duyệt (ví dụ trong vòng 24 giờ -> dùng để test rule)
        db.execSQL("INSERT OR IGNORE INTO TaiKhoan (MaTaiKhoan, Email, MatKhau) VALUES (4, 'staff2@vms.com', 'staff123');");
        db.execSQL("INSERT OR IGNORE INTO NguoiDung (MaNguoiDung, MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai, TrangThaiUpdatedAt, DateCreated) " +
                "VALUES (4, 4, 'Hoàng Văn F', '15/08/1989', 'Nam', '777888999000', '0900777888', 'Nhân viên', 'Đang yêu cầu', '', '02/11/2025 16:20:00');");

// Xe mẫu
        db.execSQL("INSERT OR IGNORE INTO Xe (MaXe, MaNguoiDung, BienSo, LoaiXe, HangSX, MauSac, SoHieu, NhienLieu, SoKmTong, TrangThai) " +
                "VALUES (1, 2, '51C-444.44', 'Xe tải', 'HINO', 'Vàng', 'HX-04', 'Diesel', 60000, 'Đang sử dụng');");
        db.execSQL("INSERT OR IGNORE INTO Xe (MaXe, MaNguoiDung, BienSo, LoaiXe, HangSX, MauSac, SoHieu, NhienLieu, SoKmTong, TrangThai) " +
                "VALUES (2, 3, '52D-555.55', 'Đầu kéo', 'HYUNDAI', 'Xám', 'DK-05', 'Diesel', 75000, 'Bảo trì');");
        db.execSQL("INSERT OR IGNORE INTO Xe (MaXe, MaNguoiDung, BienSo, LoaiXe, HangSX, MauSac, SoHieu, NhienLieu, SoKmTong, TrangThai) " +
                "VALUES (3, NULL, '53E-666.66', 'Xe tải', 'ISUZU', 'Trắng', 'IS-06', 'Diesel', 45000, 'Sẵn sàng');");

// BaoTri mẫu
        db.execSQL("INSERT OR IGNORE INTO BaoTri (MaBaoTri, MaXe, NgayGanNhat, NoiDung, DonVi) " +
                "VALUES (1, 2, '15/05/2025', 'Thay dầu, kiểm tra lốp', 'Xưởng C');");
        db.execSQL("INSERT OR IGNORE INTO BaoTri (MaBaoTri, MaXe, NgayGanNhat, NoiDung, DonVi) " +
                "VALUES (2, 3, '01/10/2025', 'Thay má phanh, kiểm tra động cơ', 'Xưởng D');");

// BaoHiem mẫu
        db.execSQL("INSERT OR IGNORE INTO BaoHiem (MaBaoHiem, MaXe, SoHD, CongTy, NgayBatDau, NgayKetThuc) " +
                "VALUES (1, 2, 'BH-003', 'GHI Insurance', '01/02/2025', '31/01/2026');");
        db.execSQL("INSERT OR IGNORE INTO BaoHiem (MaBaoHiem, MaXe, SoHD, CongTy, NgayBatDau, NgayKetThuc) " +
                "VALUES (2, 3, 'BH-004', 'JKL Insurance', '01/03/2025', '28/02/2026');");

// SucKhoe mẫu
        db.execSQL("INSERT OR IGNORE INTO SucKhoe (MaSucKhoe, MaNguoiDung, ChieuCao, CanNang, BenhNen, NgayKham, MaTuy, KetLuan) " +
                "VALUES (1, 1, 172, 68, 'Không', '  01/04/2025', '0', 'Đạt');");
        db.execSQL("INSERT OR IGNORE INTO SucKhoe (MaSucKhoe, MaNguoiDung, ChieuCao, CanNang, BenhNen, NgayKham, MaTuy, KetLuan) " +
                "VALUES (2, 2, 165, 58, 'Không', '05/04/2025', '0', 'Đạt');");

// BangCap mẫu
        db.execSQL("INSERT OR IGNORE INTO BangCap (MaBangCap, MaNguoiDung, Loai, SoBang, NgayCap, NgayHetHan, NoiCap, TinhTrang) " +
                "VALUES (1, 2, 'FE', 'FE-003', '01/01/2021', '01/01/2031', 'Hà Nội', 'Hợp lệ');");
        db.execSQL("INSERT OR IGNORE INTO BangCap (MaBangCap, MaNguoiDung, Loai, SoBang, NgayCap, NgayHetHan, NoiCap, TinhTrang) " +
                "VALUES (2, 3, 'CE', 'CE-004', '05/01/2022', '05/01/2032', 'Đà Nẵng', 'Hợp lệ');");

// ThietBi mẫu
        db.execSQL("INSERT OR IGNORE INTO ThietBi (MaThietBi, MaXe, GPS_TrangThai, LastThoiDiemPhatHien) " +
                "VALUES (1, 1, 'Online', '10/11/2025 09:00:00');");
        db.execSQL("INSERT OR IGNORE INTO ThietBi (MaThietBi, MaXe, GPS_TrangThai, LastThoiDiemPhatHien) " +
                "VALUES (2, 2, 'Offline', '09/11/2025 15:00:00');");

// PhienLaiXe mẫu
        db.execSQL("INSERT OR IGNORE INTO PhienLaiXe (MaPhien, MaXe, ThoiDiemBatDau, ThoiDiemKetThuc, TongGioLai, TongKmTrongNgay) " +
                "VALUES (1, 1, '10/11/2025 06:30:00', '10/11/2025 16:30:00', 10, 220);");
        db.execSQL("INSERT OR IGNORE INTO PhienLaiXe (MaPhien, MaXe, ThoiDiemBatDau, ThoiDiemKetThuc, TongGioLai, TongKmTrongNgay) " +
                "VALUES (2, 2, '09/11/2025 07:00:00', '09/11/2025 17:00:00', 10, 240);");

// Telemetry mẫu
        db.execSQL("INSERT OR IGNORE INTO Telemetry (MaTelemetry, MaXe, ThoiGian, Lat, Lon, TocDo, ViTri, TrangThaiXe) " +
                "VALUES (1, 1, '10/11/2025 08:30:00', 10.762622, 106.660172, 50, 'Quận 3, TP.HCM', 'Đang di chuyển');");
        db.execSQL("INSERT OR IGNORE INTO Telemetry (MaTelemetry, MaXe, ThoiGian, Lat, Lon, TocDo, ViTri, TrangThaiXe) " +
                "VALUES (2, 2, '09/11/2025 14:30:00', 16.054406, 108.202167, 0, 'Hải Châu, Đà Nẵng', 'Đỗ');");


        // --- Tài khoản / Người dùng (mẫu bổ sung) ---
        db.execSQL("INSERT OR IGNORE INTO TaiKhoan (MaTaiKhoan, Email, MatKhau) VALUES (5, 'driver5@vms.com', 'driver123');");
        db.execSQL("INSERT OR IGNORE INTO NguoiDung (MaNguoiDung, MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai, TrangThaiUpdatedAt, DateCreated) " +
                "VALUES (5, 5, 'Lê Thị G', '05/05/1995', 'Nữ', '111222333444', '0977000111', 'Nhân viên', 'Đã duyệt', '20/11/2025 10:15:00', '2025-11-15 09:00:00');");

        db.execSQL("INSERT OR IGNORE INTO TaiKhoan (MaTaiKhoan, Email, MatKhau) VALUES (6, 'manager1@vms.com', 'mgr12345');");
        db.execSQL("INSERT OR IGNORE INTO NguoiDung (MaNguoiDung, MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai, TrangThaiUpdatedAt, DateCreated) " +
                "VALUES (6, 6, 'Trần Văn H', '12/02/1985', 'Nam', '222333444555', '0919000222', 'Nhân viên', 'Đã duyệt', '15/11/2025 08:00:00', '2025-10-20 08:30:00');");

        db.execSQL("INSERT OR IGNORE INTO TaiKhoan (MaTaiKhoan, Email, MatKhau) VALUES (7, 'accounting@vms.com', 'acct2025');");
        db.execSQL("INSERT OR IGNORE INTO NguoiDung (MaNguoiDung, MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai, TrangThaiUpdatedAt, DateCreated) " +
                "VALUES (7, 7, 'Phùng Thị I', '30/09/1990', 'Nữ', '333444555666', '0908111223', 'Nhân viên', 'Đã duyệt', '05/11/2025 11:20:00', '2025-09-01 10:00:00');");

        db.execSQL("INSERT OR IGNORE INTO TaiKhoan (MaTaiKhoan, Email, MatKhau) VALUES (8, 'driver6@vms.com', 'drv456');");
        db.execSQL("INSERT OR IGNORE INTO NguoiDung (MaNguoiDung, MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai, TrangThaiUpdatedAt, DateCreated) " +
                "VALUES (8, 8, 'Nguyễn Văn K', '09/07/1992', 'Nam', '444555666777', '0987000333', 'Nhân viên', 'Đang yêu cầu', '', '2025-11-22 14:45:00');");

        db.execSQL("INSERT OR IGNORE INTO TaiKhoan (MaTaiKhoan, Email, MatKhau) VALUES (9, 'driver7@vms.com', 'drv789');");
        db.execSQL("INSERT OR IGNORE INTO NguoiDung (MaNguoiDung, MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai, TrangThaiUpdatedAt, DateCreated) " +
                "VALUES (9, 9, 'Bùi Thị L', '11/11/1996', 'Nữ', '555666777000', '0977000444', 'Nhân viên', 'Đã từ chối', '18/11/2025 09:30:00', '2025-11-18 09:00:00');");

        db.execSQL("INSERT OR IGNORE INTO TaiKhoan (MaTaiKhoan, Email, MatKhau) VALUES (10, 'staff3@vms.com', 'staff456');");
        db.execSQL("INSERT OR IGNORE INTO NguoiDung (MaNguoiDung, MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai, TrangThaiUpdatedAt, DateCreated) " +
                "VALUES (10, 10, 'Đặng Văn M', '25/12/1988', 'Nam', '666777888111', '0909000555', 'Nhân viên', 'Đã duyệt', '12/11/2025 17:00:00', '2025-10-10 12:00:00');");

        db.execSQL("INSERT OR IGNORE INTO TaiKhoan (MaTaiKhoan, Email, MatKhau) VALUES (11, 'driver8@vms.com', 'driver888');");
        db.execSQL("INSERT OR IGNORE INTO NguoiDung (MaNguoiDung, MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai, TrangThaiUpdatedAt, DateCreated) " +
                "VALUES (11, 11, 'Hoàng Thị N', '02/03/1994', 'Nữ', '777888999222', '0911222444', 'Nhân viên', 'Đã duyệt', '21/11/2025 07:10:00', '2025-11-12 08:00:00');");

        db.execSQL("INSERT OR IGNORE INTO TaiKhoan (MaTaiKhoan, Email, MatKhau) VALUES (12, 'ops1@vms.com', 'ops2025');");
        db.execSQL("INSERT OR IGNORE INTO NguoiDung (MaNguoiDung, MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai, TrangThaiUpdatedAt, DateCreated) " +
                "VALUES (12, 12, 'Võ Văn O', '17/06/1987', 'Nam', '888999000333', '0901222333', 'Nhân viên', 'Đã duyệt', '10/11/2025 13:00:00', '2025-10-05 09:30:00');");

        db.execSQL("INSERT OR IGNORE INTO TaiKhoan (MaTaiKhoan, Email, MatKhau) VALUES (13, 'driver9@vms.com', 'drv999');");
        db.execSQL("INSERT OR IGNORE INTO NguoiDung (MaNguoiDung, MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai, TrangThaiUpdatedAt, DateCreated) " +
                "VALUES (13, 13, 'Trịnh Văn P', '21/01/1984', 'Nam', '999000111222', '0988111333', 'Nhân viên', 'Đã duyệt', '22/11/2025 18:00:00', '2025-11-01 10:00:00');");

        db.execSQL("INSERT OR IGNORE INTO TaiKhoan (MaTaiKhoan, Email, MatKhau) VALUES (14, 'tempuser@vms.com', 'temp123');");
        db.execSQL("INSERT OR IGNORE INTO NguoiDung (MaNguoiDung, MaTaiKhoan, HoTen, NgaySinh, GioiTinh, CCCD, SDT, VaiTro, TrangThai, TrangThaiUpdatedAt, DateCreated) " +
                "VALUES (14, 14, 'Test User Q', '01/01/2000', 'Khác', '000111222333', '0900000111', 'Nhân viên', 'Đang yêu cầu', '', '2025-11-23 09:30:00');");

// --- Xe (mẫu bổ sung) ---
        db.execSQL("INSERT OR IGNORE INTO Xe (MaXe, MaNguoiDung, BienSo, LoaiXe, HangSX, MauSac, SoHieu, NhienLieu, SoKmTong, TrangThai) " +
                "VALUES (4, 5, '50H-111.11', 'Xe tải', 'ISUZU', 'Xanh', 'IS-11', 'Diesel', 32000, 'Sẵn sàng');");
        db.execSQL("INSERT OR IGNORE INTO Xe (MaXe, MaNguoiDung, BienSo, LoaiXe, HangSX, MauSac, SoHieu, NhienLieu, SoKmTong, TrangThai) " +
                "VALUES (5, 8, '49C-222.22', 'Đầu kéo', 'DAF', 'Đỏ', 'DK-22', 'Diesel', 98000, 'Đang sử dụng');");
        db.execSQL("INSERT OR IGNORE INTO Xe (MaXe, MaNguoiDung, BienSo, LoaiXe, HangSX, MauSac, SoHieu, NhienLieu, SoKmTong, TrangThai) " +
                "VALUES (6, 11, '48B-333.33', 'Xe tải', 'HINO', 'Trắng', 'HX-33', 'Diesel', 41000, 'Bảo trì');");
        db.execSQL("INSERT OR IGNORE INTO Xe (MaXe, MaNguoiDung, BienSo, LoaiXe, HangSX, MauSac, SoHieu, NhienLieu, SoKmTong, TrangThai) " +
                "VALUES (7, 6, '47A-444.44', 'Xe tải', 'MITSUBISHI', 'Xám', 'MT-44', 'Diesel', 150000, 'Đang sử dụng');");
        db.execSQL("INSERT OR IGNORE INTO Xe (MaXe, MaNguoiDung, BienSo, LoaiXe, HangSX, MauSac, SoHieu, NhienLieu, SoKmTong, TrangThai) " +
                "VALUES (8, NULL, '46D-555.55', 'Xe tải', 'VOLVO', 'Đen', 'VL-55', 'Diesel', 20000, 'Sẵn sàng');");
        db.execSQL("INSERT OR IGNORE INTO Xe (MaXe, MaNguoiDung, BienSo, LoaiXe, HangSX, MauSac, SoHieu, NhienLieu, SoKmTong, TrangThai) " +
                "VALUES (9, 13, '45E-666.66', 'Đầu kéo', 'SCANIA', 'Bạc', 'SC-66', 'Diesel', 123000, 'Đã nghỉ');");

// --- BaoTri (mẫu bổ sung) ---
        db.execSQL("INSERT OR IGNORE INTO BaoTri (MaBaoTri, MaXe, NgayGanNhat, NoiDung, DonVi) " +
                "VALUES (3, 4, '05/11/2025', 'Thay dầu, kiểm tra dây curoa', 'Xưởng A');");
        db.execSQL("INSERT OR IGNORE INTO BaoTri (MaBaoTri, MaXe, NgayGanNhat, NoiDung, DonVi) " +
                "VALUES (4, 5, '18/11/2025', 'Thay lon, cân chỉnh phanh', 'Xưởng B');");
        db.execSQL("INSERT OR IGNORE INTO BaoTri (MaBaoTri, MaXe, NgayGanNhat, NoiDung, DonVi) " +
                "VALUES (5, 6, '10/10/2025', 'Bảo dưỡng định kỳ 10.000 km', 'Xưởng C');");
        db.execSQL("INSERT OR IGNORE INTO BaoTri (MaBaoTri, MaXe, NgayGanNhat, NoiDung, DonVi) " +
                "VALUES (6, 7, '01/09/2025', 'Thay lọc nhiên liệu, vệ sinh kim phun', 'Xưởng D');");
        db.execSQL("INSERT OR IGNORE INTO BaoTri (MaBaoTri, MaXe, NgayGanNhat, NoiDung, DonVi) " +
                "VALUES (7, 8, '20/11/2025', 'Kiểm tra điện, cập nhật firmware thiết bị', 'Xưởng E');");
        db.execSQL("INSERT OR IGNORE INTO BaoTri (MaBaoTri, MaXe, NgayGanNhat, NoiDung, DonVi) " +
                "VALUES (8, 9, '12/11/2025', 'Thay má phanh', 'Xưởng F');");

// --- BaoHiem (mẫu bổ sung) ---
        db.execSQL("INSERT OR IGNORE INTO BaoHiem (MaBaoHiem, MaXe, SoHD, CongTy, NgayBatDau, NgayKetThuc) " +
                "VALUES (3, 4, 'BH-005', 'ABC Insurance', '01/06/2025', '31/05/2026');");
        db.execSQL("INSERT OR IGNORE INTO BaoHiem (MaBaoHiem, MaXe, SoHD, CongTy, NgayBatDau, NgayKetThuc) " +
                "VALUES (4, 5, 'BH-006', 'DEF Insurance', '15/07/2025', '14/07/2026');");
        db.execSQL("INSERT OR IGNORE INTO BaoHiem (MaBaoHiem, MaXe, SoHD, CongTy, NgayBatDau, NgayKetThuc) " +
                "VALUES (5, 6, 'BH-007', 'GHI Insurance', '01/03/2025', '28/02/2026');");
        db.execSQL("INSERT OR IGNORE INTO BaoHiem (MaBaoHiem, MaXe, SoHD, CongTy, NgayBatDau, NgayKetThuc) " +
                "VALUES (6, 7, 'BH-008', 'JKL Insurance', '01/12/2024', '30/11/2025');");
        db.execSQL("INSERT OR IGNORE INTO BaoHiem (MaBaoHiem, MaXe, SoHD, CongTy, NgayBatDau, NgayKetThuc) " +
                "VALUES (7, 9, 'BH-009', 'MNO Insurance', '01/08/2025', '31/07/2026');");

// --- SucKhoe (mẫu bổ sung) ---
        db.execSQL("INSERT OR IGNORE INTO SucKhoe (MaSucKhoe, MaNguoiDung, ChieuCao, CanNang, BenhNen, NgayKham, MaTuy, KetLuan) " +
                "VALUES (3, 5, 160, 55, 'Không', '15/11/2025', '0', 'Đạt');");
        db.execSQL("INSERT OR IGNORE INTO SucKhoe (MaSucKhoe, MaNguoiDung, ChieuCao, CanNang, BenhNen, NgayKham, MaTuy, KetLuan) " +
                "VALUES (4, 8, 175, 78, 'Huyết áp cao', '21/11/2025', '0', 'Cần theo dõi');");
        db.execSQL("INSERT OR IGNORE INTO SucKhoe (MaSucKhoe, MaNguoiDung, ChieuCao, CanNang, BenhNen, NgayKham, MaTuy, KetLuan) " +
                "VALUES (5, 11, 168, 62, 'Không', '10/11/2025', '0', 'Đạt');");
        db.execSQL("INSERT OR IGNORE INTO SucKhoe (MaSucKhoe, MaNguoiDung, ChieuCao, CanNang, BenhNen, NgayKham, MaTuy, KetLuan) " +
                "VALUES (6, 13, 170, 70, 'Tiểu đường', '12/11/2025', '0', 'Cần điều trị');");

// --- BangCap (mẫu bổ sung) ---
        db.execSQL("INSERT OR IGNORE INTO BangCap (MaBangCap, MaNguoiDung, Loai, SoBang, NgayCap, NgayHetHan, NoiCap, TinhTrang) " +
                "VALUES (3, 5, 'FE', 'FE-010', '01/06/2018', '01/06/2028', 'Hà Nội', 'Hợp lệ');");
        db.execSQL("INSERT OR IGNORE INTO BangCap (MaBangCap, MaNguoiDung, Loai, SoBang, NgayCap, NgayHetHan, NoiCap, TinhTrang) " +
                "VALUES (4, 11, 'CE', 'CE-011', '15/03/2019', '15/03/2029', 'TP.HCM', 'Hợp lệ');");
        db.execSQL("INSERT OR IGNORE INTO BangCap (MaBangCap, MaNguoiDung, Loai, SoBang, NgayCap, NgayHetHan, NoiCap, TinhTrang) " +
                "VALUES (5, 13, 'FE', 'FE-012', '20/07/2017', '20/07/2027', 'Đà Nẵng', 'Hết hạn');");
        db.execSQL("INSERT OR IGNORE INTO BangCap (MaBangCap, MaNguoiDung, Loai, SoBang, NgayCap, NgayHetHan, NoiCap, TinhTrang) " +
                "VALUES (6, 6, 'QL', 'QL-001', '01/01/2015', '01/01/2030', 'Hà Nội', 'Hợp lệ');");

// --- ThietBi (mẫu bổ sung) ---
        db.execSQL("INSERT OR IGNORE INTO ThietBi (MaThietBi, MaXe, GPS_TrangThai, LastThoiDiemPhatHien) " +
                "VALUES (3, 4, 'Online', '20/11/2025 10:10:00');");
        db.execSQL("INSERT OR IGNORE INTO ThietBi (MaThietBi, MaXe, GPS_TrangThai, LastThoiDiemPhatHien) " +
                "VALUES (4, 5, 'Online', '18/11/2025 09:00:00');");
        db.execSQL("INSERT OR IGNORE INTO ThietBi (MaThietBi, MaXe, GPS_TrangThai, LastThoiDiemPhatHien) " +
                "VALUES (5, 6, 'Offline', '12/11/2025 16:30:00');");
        db.execSQL("INSERT OR IGNORE INTO ThietBi (MaThietBi, MaXe, GPS_TrangThai, LastThoiDiemPhatHien) " +
                "VALUES (6, 8, 'Online', '20/11/2025 07:45:00');");

// --- PhienLaiXe (mẫu bổ sung) ---
        db.execSQL("INSERT OR IGNORE INTO PhienLaiXe (MaPhien, MaXe, ThoiDiemBatDau, ThoiDiemKetThuc, TongGioLai, TongKmTrongNgay) " +
                "VALUES (3, 4, '20/11/2025 06:00:00', '20/11/2025 15:30:00', 9.5, 180);");
        db.execSQL("INSERT OR IGNORE INTO PhienLaiXe (MaPhien, MaXe, ThoiDiemBatDau, ThoiDiemKetThuc, TongGioLai, TongKmTrongNgay) " +
                "VALUES (4, 5, '18/11/2025 07:30:00', '18/11/2025 18:00:00', 10.5, 300);");
        db.execSQL("INSERT OR IGNORE INTO PhienLaiXe (MaPhien, MaXe, ThoiDiemBatDau, ThoiDiemKetThuc, TongGioLai, TongKmTrongNgay) " +
                "VALUES (5, 6, '10/11/2025 08:00:00', '10/11/2025 17:00:00', 9, 150);");
        db.execSQL("INSERT OR IGNORE INTO PhienLaiXe (MaPhien, MaXe, ThoiDiemBatDau, ThoiDiemKetThuc, TongGioLai, TongKmTrongNgay) " +
                "VALUES (6, 7, '12/11/2025 05:30:00', '12/11/2025 16:00:00', 10.5, 420);");
        db.execSQL("INSERT OR IGNORE INTO PhienLaiXe (MaPhien, MaXe, ThoiDiemBatDau, ThoiDiemKetThuc, TongGioLai, TongKmTrongNgay) " +
                "VALUES (7, 9, '22/11/2025 06:00:00', '22/11/2025 14:00:00', 8, 200);");

// --- Telemetry (mẫu bổ sung) ---
        db.execSQL("INSERT OR IGNORE INTO Telemetry (MaTelemetry, MaXe, ThoiGian, Lat, Lon, TocDo, ViTri, TrangThaiXe) " +
                "VALUES (3, 4, '20/11/2025 08:45:00', 10.762245, 106.682165, 60, 'Quận 1, TP.HCM', 'Đang di chuyển');");
        db.execSQL("INSERT OR IGNORE INTO Telemetry (MaTelemetry, MaXe, ThoiGian, Lat, Lon, TocDo, ViTri, TrangThaiXe) " +
                "VALUES (4, 5, '18/11/2025 12:30:00', 30.054200, 110.202500, 0, 'Ngũ Hành Sơn, Đà Nẵng', 'Dừng');");
        db.execSQL("INSERT OR IGNORE INTO Telemetry (MaTelemetry, MaXe, ThoiGian, Lat, Lon, TocDo, ViTri, TrangThaiXe) " +
                "VALUES (5, 6, '10/11/2025 09:15:00', 27.028511, 105.804817, 45, 'Hoàn Kiếm, Hà Nội', 'Mất GPS');");
        db.execSQL("INSERT OR IGNORE INTO Telemetry (MaTelemetry, MaXe, ThoiGian, Lat, Lon, TocDo, ViTri, TrangThaiXe) " +
                "VALUES (6, 7, '12/11/2025 10:00:00', 20.983933, 105.770783, 30, 'Hải Phòng', 'Đang di chuyển');");
        db.execSQL("INSERT OR IGNORE INTO Telemetry (MaTelemetry, MaXe, ThoiGian, Lat, Lon, TocDo, ViTri, TrangThaiXe) " +
                "VALUES (7, 8, '20/11/2025 07:50:00', 19.045162, 105.746857, 0, 'Cần Thơ', 'Đỗ');");
        db.execSQL("INSERT OR IGNORE INTO Telemetry (MaTelemetry, MaXe, ThoiGian, Lat, Lon, TocDo, ViTri, TrangThaiXe) " +
                "VALUES (8, 9, '22/11/2025 06:30:00', 12.2388, 109.1967, 55, 'Nha Trang', 'Mất GPS');");
        db.execSQL("INSERT OR IGNORE INTO Telemetry (MaTelemetry, MaXe, ThoiGian, Lat, Lon, TocDo, ViTri, TrangThaiXe) " +
                "VALUES (9, 1, '21/11/2025 14:00:00', 10.776889, 106.700806, 0, 'Quận 3, TP.HCM', 'Dừng');");
        db.execSQL("INSERT OR IGNORE INTO Telemetry (MaTelemetry, MaXe, ThoiGian, Lat, Lon, TocDo, ViTri, TrangThaiXe) " +
                "VALUES (10, 2, '19/11/2025 15:45:00', 16.0727, 108.2140, 0, 'Hải Châu, Đà Nẵng', 'Đỗ');");
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
            String now = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
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

        String now = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
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
