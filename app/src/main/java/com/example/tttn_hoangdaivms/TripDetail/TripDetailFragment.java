package com.example.tttn_hoangdaivms.TripDetail;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tttn_hoangdaivms.Database.Database;
import com.example.tttn_hoangdaivms.R;
import com.example.tttn_hoangdaivms.Report.ReportModel;

import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TripDetailFragment extends Fragment {

    private static final String ARG_REPORT = "arg_report";   // ReportModel as Serializable (preferred)
    private static final String ARG_MAPHIEN = "arg_maPhien"; // fallback: maPhien int

    private ReportModel report;
    private int maPhienFallback = -1;

    private Database db;

    // Views
    private TextView tvMaPhien, tvMaXe, tvBienSo, tvThoiGianBatDau, tvThoiGianKetThuc,
            tvDiemBatDau, tvDiemDen, tvQuangDuong, tvTongGioLai, tvTongKmTrongNgay,
            tvLoaiXe, tvHangSX, tvMauSac, tvSoHieu, tvNhienLieu, tvSoKmTong, tvTrangThaiXe,
            tvChuXeHoTen, tvChuXeSDT, tvChuXeCCCD, tvChuXeVaiTro, tvGhiChu, toolbarTitle;
    private ImageView btnBack;
    private Button btnExport;

    // trong TripDetailFragment class
    public static TripDetailFragment newInstance(int maPhien) {
        TripDetailFragment f = new TripDetailFragment();
        Bundle b = new Bundle();
        b.putInt("maPhien", maPhien);
        f.setArguments(b);
        return f;
    }


    public static TripDetailFragment newInstance(ReportModel r) {
        TripDetailFragment f = new TripDetailFragment();
        Bundle b = new Bundle();
        b.putSerializable(ARG_REPORT, (Serializable) r);
        f.setArguments(b);
        return f;
    }

    public static TripDetailFragment newInstanceWithId(int maPhien) {
        TripDetailFragment f = new TripDetailFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_MAPHIEN, maPhien);
        f.setArguments(b);
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trip_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new Database(requireContext());

        // bind views (IDs from your XML)
        toolbarTitle = view.findViewById(R.id.toolbarTitle);
        btnBack = view.findViewById(R.id.btnBack);

        tvMaPhien = view.findViewById(R.id.tvMaPhien);
        tvMaXe = view.findViewById(R.id.tvMaXe);
        tvBienSo = view.findViewById(R.id.tvBienSo);
        tvThoiGianBatDau = view.findViewById(R.id.tvThoiGianBatDau);
        tvThoiGianKetThuc = view.findViewById(R.id.tvThoiGianKetThuc);
        tvDiemBatDau = view.findViewById(R.id.tvDiemBatDau);
        tvDiemDen = view.findViewById(R.id.tvDiemDen);
        tvQuangDuong = view.findViewById(R.id.tvQuangDuong);
        tvTongGioLai = view.findViewById(R.id.tvTongGioLai);
        tvTongKmTrongNgay = view.findViewById(R.id.tvTongKmTrongNgay);

        tvLoaiXe = view.findViewById(R.id.tvLoaiXe);
        tvHangSX = view.findViewById(R.id.tvHangSX);
        tvMauSac = view.findViewById(R.id.tvMauSac);
        tvSoHieu = view.findViewById(R.id.tvSoHieu);
        tvNhienLieu = view.findViewById(R.id.tvNhienLieu);
        tvSoKmTong = view.findViewById(R.id.tvSoKmTong);
        tvTrangThaiXe = view.findViewById(R.id.tvTrangThaiXe);

        tvChuXeHoTen = view.findViewById(R.id.tvChuXeHoTen);
        tvChuXeSDT = view.findViewById(R.id.tvChuXeSDT);
        tvChuXeCCCD = view.findViewById(R.id.tvChuXeCCCD);
        tvChuXeVaiTro = view.findViewById(R.id.tvChuXeVaiTro);


        btnExport = view.findViewById(R.id.btnExport);

        // back button
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                requireActivity().onBackPressed();
            }
        });

        // get args
        Bundle args = getArguments();
        if (args != null) {
            Object o = args.getSerializable(ARG_REPORT);
            if (o instanceof ReportModel) {
                report = (ReportModel) o;
            } else if (args.containsKey(ARG_MAPHIEN)) {
                maPhienFallback = args.getInt(ARG_MAPHIEN, -1);
            }
        }

        // if no object, try load from DB by maPhien
        if (report == null && maPhienFallback > 0) {
            loadReportFromDb(maPhienFallback);
        }

        if (report == null) {
            Toast.makeText(requireContext(), "Không tìm thấy dữ liệu hành trình", Toast.LENGTH_SHORT).show();
            return;
        }

        // fill UI
        bindDataToViews(report);

        // export click
        btnExport.setOnClickListener(v -> {
            String path = exportTripToExcel(report);
            if (path != null) {
                Toast.makeText(requireContext(), "Đã xuất: " + path, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(requireContext(), "Xuất lỗi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindDataToViews(ReportModel r) {
        toolbarTitle.setText("Hành trình " + r.maPhien);

        tvMaPhien.setText(String.valueOf(r.maPhien));
        tvMaXe.setText(String.valueOf(r.maXe));
        tvBienSo.setText(notNull(r.bienSo));
        tvThoiGianBatDau.setText(notNull(r.thoiDiemBatDau));
        tvThoiGianKetThuc.setText(notNull(r.thoiDiemKetThuc));

        // diemBatDau/diemDen/ghiChu may be null if not in ReportModel; guard
//        tvDiemBatDau.setText(notNull(r.diemBatDau));
//        tvDiemDen.setText(notNull(r.diemDen));
        tvQuangDuong.setText(String.valueOf(r.tongKmTrongNgay));
        tvTongGioLai.setText(String.valueOf(r.tongGioLai));
        tvTongKmTrongNgay.setText(String.valueOf(r.tongKmTrongNgay));

        tvLoaiXe.setText(notNull(r.loaiXe));
        tvHangSX.setText(notNull(r.hangSX));
        tvMauSac.setText(notNull(r.mauSac));
        tvSoHieu.setText(notNull(r.soHieu));
        tvNhienLieu.setText(notNull(r.nhienLieu));
        tvSoKmTong.setText(String.valueOf(r.soKmTong));
        tvTrangThaiXe.setText(notNull(r.trangThaiXe));

        tvChuXeHoTen.setText(notNull(r.chuXeHoTen));
        tvChuXeSDT.setText(notNull(r.chuXeSDT));
        tvChuXeCCCD.setText(notNull(r.chuXeCCCD));
        tvChuXeVaiTro.setText(notNull(r.chuXeVaiTro));
    }

    private String notNull(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    // load ReportModel from DB by MaPhien (join same as in ReportListFragment)
    private void loadReportFromDb(int maPhien) {
        String sql = "SELECT P.MaPhien, P.MaXe, P.ThoiDiemBatDau, P.ThoiDiemKetThuc, P.TongGioLai, P.TongKmTrongNgay, " +
                "X.BienSo, X.LoaiXe, X.HangSX, X.MauSac, X.SoHieu, X.NhienLieu, X.SoKmTong AS X_SoKmTong, X.TrangThai AS X_TrangThai, " +
                "ND.HoTen AS ChuHoTen, ND.SDT AS ChuSDT, ND.CCCD AS ChuCCCD, ND.VaiTro AS ChuVaiTro " +
                "FROM PhienLaiXe P " +
                "LEFT JOIN Xe X ON P.MaXe = X.MaXe " +
                "LEFT JOIN NguoiDung ND ON X.MaNguoiDung = ND.MaNguoiDung " +
                "WHERE P.MaPhien = ? LIMIT 1";
        Cursor c = null;
        try {
            c = db.getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(maPhien)});
            if (c != null && c.moveToFirst()) {
                int id = safeGetInt(c, "MaPhien");
                int maXe = safeGetInt(c, "MaXe");
                String thoiDiemBatDau = safeGetString(c, "ThoiDiemBatDau");
                String thoiDiemKetThuc = safeGetString(c, "ThoiDiemKetThuc");
                double tongGioLai = safeGetDouble(c, "TongGioLai");
                double tongKm = safeGetDouble(c, "TongKmTrongNgay");
                String bienSo = safeGetString(c, "BienSo");

                ReportModel item = new ReportModel(id, maXe, bienSo, thoiDiemBatDau, thoiDiemKetThuc, tongGioLai, tongKm);

                item.loaiXe = safeGetString(c, "LoaiXe");
                item.hangSX = safeGetString(c, "HangSX");
                item.mauSac = safeGetString(c, "MauSac");
                item.soHieu = safeGetString(c, "SoHieu");
                item.nhienLieu = safeGetString(c, "NhienLieu");
                item.soKmTong = safeGetDouble(c, "X_SoKmTong");
                item.trangThaiXe = safeGetString(c, "X_TrangThai");

                item.chuXeHoTen = safeGetString(c, "ChuHoTen");
                item.chuXeSDT = safeGetString(c, "ChuSDT");
                item.chuXeCCCD = safeGetString(c, "ChuCCCD");
                item.chuXeVaiTro = safeGetString(c, "ChuVaiTro");

                // diemBatDau/diemDen/ghiChu likely not in PhienLaiXe table -> left empty
                report = item;
            }
        } catch (Exception e) {
            e.printStackTrace();
            report = null;
        } finally {
            if (c != null) c.close();
        }
    }

    // safe getters for Cursor
    private int safeGetInt(Cursor c, String col) {
        try {
            int idx = c.getColumnIndex(col);
            if (idx >= 0 && !c.isNull(idx)) return c.getInt(idx);
        } catch (Exception ignored) {}
        return 0;
    }

    private String safeGetString(Cursor c, String col) {
        try {
            int idx = c.getColumnIndex(col);
            if (idx >= 0 && !c.isNull(idx)) return c.getString(idx);
        } catch (Exception ignored) {}
        return null;
    }

    private double safeGetDouble(Cursor c, String col) {
        try {
            int idx = c.getColumnIndex(col);
            if (idx >= 0 && !c.isNull(idx)) return c.getDouble(idx);
        } catch (Exception ignored) {}
        return 0;
    }

    /**
     * Export single trip to Excel (.xlsx).
     * Creates:
     *  - Sheet "Detail" with two columns (Field / Value)
     *  - Sheet "Row" with header row same as multi-export and one data row
     */
    private String exportTripToExcel(ReportModel r) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        try {
            // ---------- Sheet 1: Detail (key/value) ----------
            XSSFSheet detail = workbook.createSheet("Detail");
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            XSSFFont font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.LEFT);

            String[][] kv = new String[][]{
                    {"MaPhien", String.valueOf(r.maPhien)},
                    {"MaXe", String.valueOf(r.maXe)},
                    {"BienSo", notNull(r.bienSo)},
                    {"ThoiDiemBatDau", notNull(r.thoiDiemBatDau)},
                    {"ThoiDiemKetThuc", notNull(r.thoiDiemKetThuc)},
                    {"TongGioLai", String.valueOf(r.tongGioLai)},
                    {"TongKmTrongNgay", String.valueOf(r.tongKmTrongNgay)},
                    {"LoaiXe", notNull(r.loaiXe)},
                    {"HangSX", notNull(r.hangSX)},
                    {"MauSac", notNull(r.mauSac)},
                    {"SoHieu", notNull(r.soHieu)},
                    {"NhienLieu", notNull(r.nhienLieu)},
                    {"SoKmTong", String.valueOf(r.soKmTong)},
                    {"TrangThaiXe", notNull(r.trangThaiXe)},
                    {"ChuXe_HoTen", notNull(r.chuXeHoTen)},
                    {"ChuXe_SDT", notNull(r.chuXeSDT)},
                    {"ChuXe_CCCD", notNull(r.chuXeCCCD)},
                    {"ChuXe_VaiTro", notNull(r.chuXeVaiTro)},
//                    {"DiemBatDau", notNull(r.diemBatDau)},
//                    {"DiemDen", notNull(r.diemDen)},
            };

            for (int i = 0; i < kv.length; i++) {
                XSSFRow row = detail.createRow(i);
                XSSFCell c0 = row.createCell(0);
                c0.setCellValue(kv[i][0]);
                c0.setCellStyle(headerStyle);
                row.createCell(1).setCellValue(kv[i][1]);
            }

            // set column widths (safe approach)
            int maxCol0 = 0, maxCol1 = 0;
            for (String[] pair : kv) {
                maxCol0 = Math.max(maxCol0, pair[0].length());
                maxCol1 = Math.max(maxCol1, pair[1] != null ? pair[1].length() : 0);
            }
            detail.setColumnWidth(0, (maxCol0 + 4) * 256);
            detail.setColumnWidth(1, (maxCol1 + 4) * 256);

            // ---------- Sheet 2: Row (header + single row) ----------
            XSSFSheet rowSheet = workbook.createSheet("HanhTrinhRow");
            String[] headers = new String[] {
                    "MaPhien", "MaXe", "BienSo", "ThoiDiemBatDau", "ThoiDiemKetThuc", "TongGioLai", "TongKmTrongNgay",
                    "LoaiXe", "HangSX", "MauSac", "SoHieu", "NhienLieu", "SoKmTong", "TrangThaiXe",
                    "ChuXe_HoTen", "ChuXe_SDT", "ChuXe_CCCD", "ChuXe_VaiTro",
                    "DiemBatDau", "DiemDen", "GhiChu"
            };
            XSSFRow headerRow = rowSheet.createRow(0);
            XSSFCellStyle headerStyle2 = workbook.createCellStyle();
            XSSFFont font2 = workbook.createFont(); font2.setBold(true);
            headerStyle2.setFont(font2);
            headerStyle2.setAlignment(HorizontalAlignment.CENTER);

            for (int i = 0; i < headers.length; i++) {
                XSSFCell hc = headerRow.createCell(i);
                hc.setCellValue(headers[i]);
                hc.setCellStyle(headerStyle2);
            }

            XSSFRow dataRow = rowSheet.createRow(1);
            int c = 0;
            dataRow.createCell(c++).setCellValue(String.valueOf(r.maPhien));
            dataRow.createCell(c++).setCellValue(String.valueOf(r.maXe));
            dataRow.createCell(c++).setCellValue(notNull(r.bienSo));
            dataRow.createCell(c++).setCellValue(notNull(r.thoiDiemBatDau));
            dataRow.createCell(c++).setCellValue(notNull(r.thoiDiemKetThuc));
            dataRow.createCell(c++).setCellValue(r.tongGioLai);
            dataRow.createCell(c++).setCellValue(r.tongKmTrongNgay);

            dataRow.createCell(c++).setCellValue(notNull(r.loaiXe));
            dataRow.createCell(c++).setCellValue(notNull(r.hangSX));
            dataRow.createCell(c++).setCellValue(notNull(r.mauSac));
            dataRow.createCell(c++).setCellValue(notNull(r.soHieu));
            dataRow.createCell(c++).setCellValue(notNull(r.nhienLieu));
            dataRow.createCell(c++).setCellValue(r.soKmTong);
            dataRow.createCell(c++).setCellValue(notNull(r.trangThaiXe));

            dataRow.createCell(c++).setCellValue(notNull(r.chuXeHoTen));
            dataRow.createCell(c++).setCellValue(notNull(r.chuXeSDT));
            dataRow.createCell(c++).setCellValue(notNull(r.chuXeCCCD));
            dataRow.createCell(c++).setCellValue(notNull(r.chuXeVaiTro));
//
//            dataRow.createCell(c++).setCellValue(notNull(r.diemBatDau));
//            dataRow.createCell(c++).setCellValue(notNull(r.diemDen));
//            dataRow.createCell(c++).setCellValue(notNull(r.ghiChu));

            // set widths for rowSheet based on header lengths + padding
            for (int i = 0; i < headers.length; i++) {
                int width = headers[i].length() + 6;
                if (width > 255) width = 255;
                rowSheet.setColumnWidth(i, width * 256);
            }

            // Write file
            File docsDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (docsDir == null) docsDir = requireContext().getFilesDir();
            File outDir = new File(docsDir, "exports");
            if (!outDir.exists()) outDir.mkdirs();

            String fileName = "hanhtrinh_" + r.maPhien + "_" + System.currentTimeMillis() + ".xlsx";
            File outFile = new File(outDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                workbook.write(fos);
            }

            return outFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try { workbook.close(); } catch (Exception ignored) {}
        }
    }
}
