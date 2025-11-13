package com.example.tttn_hoangdaivms.Report;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tttn_hoangdaivms.BaoTriDetail.BaoTriDetailFragment;
import com.example.tttn_hoangdaivms.Database.Database;
import com.example.tttn_hoangdaivms.R;
import com.example.tttn_hoangdaivms.Report.ReportAdapter;
import com.example.tttn_hoangdaivms.Report.ReportModel;
import com.example.tttn_hoangdaivms.TripDetail.TripDetailFragment;

import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ReportListFragment extends Fragment {

    private Database db;
    private RecyclerView rv;

    // adapters & models
    private ReportAdapter reportAdapter;
    private BaoTriAdapter baoTriAdapter;
    // store full list of BaoTri so we can restore after filtering
    private List<BaoTriModel> allBaoTriList = new ArrayList<>();

    private EditText searchEditText;
    private TextView btnDriver, btnVehicle;
    private Button btnExport;

    // track active tab: false = Hành trình, true = Bảo trì
    private boolean isBaoTriActive = false;

    public ReportListFragment() { }

    public static ReportListFragment newInstance() { return new ReportListFragment(); }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new Database(requireContext());

        rv = view.findViewById(R.id.driverRecyclerView);
        searchEditText = view.findViewById(R.id.searchEditText);
        btnDriver = view.findViewById(R.id.btnDriver);
        btnVehicle = view.findViewById(R.id.btnVehicle);
        btnExport = view.findViewById(R.id.btnExport);

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Report (Hành trình) adapter
        reportAdapter = new ReportAdapter(new ArrayList<>(), new ReportAdapter.OnItemAction() {
            @Override
            public void onItemClicked(ReportModel item) {
                // mở TripDetailFragment theo MaPhien (int)
                TripDetailFragment detailFragment = TripDetailFragment.newInstance(item.maPhien);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.containerMain, detailFragment)
                        .addToBackStack(null)
                        .commit();
            }

            @Override public void onSelectionChanged() {}
        });

        // default: load Hành trình
        rv.setAdapter(reportAdapter);
        isBaoTriActive = false;
        loadReports();

        // Search watcher: adapt to current adapter
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString();
                if (!isBaoTriActive && reportAdapter != null) {
                    reportAdapter.filter(q);
                } else {
                    // For BaoTri we filter from the master list (allBaoTriList)
                    filterBaoTriByQuery(q);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnDriver.setOnClickListener(v -> {
            isBaoTriActive = false;
            btnDriver.setBackgroundResource(R.drawable.bg_segment_selected);
            btnVehicle.setBackgroundResource(R.drawable.bg_segment_unselected);
            btnDriver.setTextColor(Color.WHITE);
            btnVehicle.setTextColor(Color.parseColor("#666666"));
            // set recycler to reportAdapter
            rv.setAdapter(reportAdapter);
            loadReports();
            // restore search results if search text exists
            String q = searchEditText.getText() != null ? searchEditText.getText().toString() : "";
            reportAdapter.filter(q);
        });

        btnVehicle.setOnClickListener(v -> {
            isBaoTriActive = true;
            btnVehicle.setBackgroundResource(R.drawable.bg_segment_selected);
            btnDriver.setBackgroundResource(R.drawable.bg_segment_unselected);
            btnDriver.setTextColor(Color.parseColor("#666666"));
            btnVehicle.setTextColor(Color.WHITE);
            loadBaoTriList(); // this will set baoTriAdapter and rv.setAdapter
            // apply current search (if any) to new BaoTri list
            String q = searchEditText.getText() != null ? searchEditText.getText().toString() : "";
            filterBaoTriByQuery(q);
        });

        btnExport.setOnClickListener(v -> {
            if (!isBaoTriActive) {
                // export Hành trình
                List<ReportModel> selected = reportAdapter.getSelectedItems();
                if (selected == null || selected.isEmpty()) selected = reportAdapter.getAllItems();
                if (selected == null || selected.isEmpty()) {
                    Toast.makeText(requireContext(), "Không có dữ liệu để xuất", Toast.LENGTH_SHORT).show();
                    return;
                }
                String path = exportSelectedToExcel(selected);
                if (path != null) {
                    Toast.makeText(requireContext(), "Đã xuất: " + path, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), "Xuất lỗi", Toast.LENGTH_SHORT).show();
                }
            } else {
                // export BaoTri
                if (baoTriAdapter == null) {
                    Toast.makeText(requireContext(), "Không có dữ liệu bảo trì", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<BaoTriModel> selected = baoTriAdapter.getSelectedItems();
                if (selected == null || selected.isEmpty()) selected = baoTriAdapter.getAllItems();
                if (selected == null || selected.isEmpty()) {
                    Toast.makeText(requireContext(), "Không có dữ liệu bảo trì để xuất", Toast.LENGTH_SHORT).show();
                    return;
                }
                String path = exportBaoTriToExcel(selected);
                if (path != null) {
                    Toast.makeText(requireContext(), "Đã xuất bảo trì: " + path, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), "Xuất lỗi", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // -------------------------
    // Load Hành trình
    // -------------------------
    private void loadReports() {
        List<ReportModel> list = new ArrayList<>();
        String sql = "SELECT P.MaPhien, P.MaXe, P.ThoiDiemBatDau, P.ThoiDiemKetThuc, P.TongGioLai, P.TongKmTrongNgay, " +
                "X.BienSo, X.LoaiXe, X.HangSX, X.MauSac, X.SoHieu, X.NhienLieu, X.SoKmTong AS X_SoKmTong, X.TrangThai AS X_TrangThai, " +
                "ND.HoTen AS ChuHoTen, ND.SDT AS ChuSDT, ND.CCCD AS ChuCCCD, ND.VaiTro AS ChuVaiTro " +
                "FROM PhienLaiXe P " +
                "LEFT JOIN Xe X ON P.MaXe = X.MaXe " +
                "LEFT JOIN NguoiDung ND ON X.MaNguoiDung = ND.MaNguoiDung " +
                "ORDER BY P.ThoiDiemBatDau DESC";
        Cursor c = null;
        try {
            c = db.getReadableDatabase().rawQuery(sql, null);
            if (c != null && c.moveToFirst()) {
                do {
                    int maPhien = safeGetInt(c, "MaPhien");
                    int maXe = safeGetInt(c, "MaXe");
                    String thoiDiemBatDau = safeGetString(c, "ThoiDiemBatDau");
                    String thoiDiemKetThuc = safeGetString(c, "ThoiDiemKetThuc");
                    double tongGioLai = safeGetDouble(c, "TongGioLai");
                    double tongKm = safeGetDouble(c, "TongKmTrongNgay");
                    String bienSo = safeGetString(c, "BienSo");

                    ReportModel item = new ReportModel(maPhien, maXe, bienSo, thoiDiemBatDau, thoiDiemKetThuc, tongGioLai, tongKm);

                    // Xe fields
                    item.loaiXe = safeGetString(c, "LoaiXe");
                    item.hangSX = safeGetString(c, "HangSX");
                    item.mauSac = safeGetString(c, "MauSac");
                    item.soHieu = safeGetString(c, "SoHieu");
                    item.nhienLieu = safeGetString(c, "NhienLieu");
                    item.soKmTong = safeGetDouble(c, "X_SoKmTong");
                    item.trangThaiXe = safeGetString(c, "X_TrangThai");

                    // Chủ xe
                    item.chuXeHoTen = safeGetString(c, "ChuHoTen");
                    item.chuXeSDT = safeGetString(c, "ChuSDT");
                    item.chuXeCCCD = safeGetString(c, "ChuCCCD");
                    item.chuXeVaiTro = safeGetString(c, "ChuVaiTro");

                    list.add(item);
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) c.close();
        }

        reportAdapter.setItems(list);
    }

    // -------------------------
    // Load Bảo trì (show plate + late status)
    // -------------------------
    private void loadBaoTriList() {
        List<BaoTriModel> list = new ArrayList<>();
        String sql = "SELECT B.MaBaoTri, B.MaXe, X.BienSo, B.NgayGanNhat AS NgayBaoTri, B.NoiDung AS MoTa, 0 AS ChiPhi, '' AS TrangThai " +
                "FROM BaoTri B LEFT JOIN Xe X ON B.MaXe = X.MaXe " +
                "ORDER BY B.NgayGanNhat DESC";
        Cursor c = null;
        try {
            c = db.getReadableDatabase().rawQuery(sql, null);
            if (c != null && c.moveToFirst()) {
                do {
                    int maBaoTri = safeGetInt(c, "MaBaoTri");
                    int maXe = safeGetInt(c, "MaXe");
                    String bienSo = safeGetString(c, "BienSo");
                    String ngayBaoTri = safeGetString(c, "NgayBaoTri");
                    String moTa = safeGetString(c, "MoTa");
                    double chiPhi = safeGetDouble(c, "ChiPhi");
                    String trangThai = safeGetString(c, "TrangThai");

                    BaoTriModel b = new BaoTriModel(maBaoTri, maXe, bienSo, ngayBaoTri, /*loaiBaoTri*/ "", moTa, chiPhi, trangThai);
                    list.add(b);
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) c.close();
        }

        // store master list so filter can restore when query cleared
        allBaoTriList = new ArrayList<>(list);

        // tạo adapter và gán cho RecyclerView
        baoTriAdapter = new BaoTriAdapter(list, new BaoTriAdapter.OnItemAction() {
            @Override
            public void onSelectionChanged() {
                // selection changed - you can update UI if needed
            }

            @Override
            public void onItemClicked(BaoTriModel item) {
                // mở BaoTriDetailFragment truyền MaBaoTri (an toàn)
                BaoTriDetailFragment detail = BaoTriDetailFragment.newInstanceWithId(item.maBaoTri);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.containerMain, detail)
                        .addToBackStack(null)
                        .commit();
            }
        });
        rv.setAdapter(baoTriAdapter);
    }

    // quick filter for BaoTri by plate substring (uses allBaoTriList as source)
    private void filterBaoTriByQuery(String q) {
        if (baoTriAdapter == null) return;
        if (q == null || q.trim().isEmpty()) {
            // restore all from master list
            baoTriAdapter.setItems(new ArrayList<>(allBaoTriList));
            return;
        }
        String ql = q.toLowerCase();
        List<BaoTriModel> filtered = new ArrayList<>();
        for (BaoTriModel b : allBaoTriList) {
            if (b.bienSo != null && b.bienSo.toLowerCase().contains(ql)) filtered.add(b);
        }
        baoTriAdapter.setItems(filtered);
    }

    // -------------------------
    // Safe getters
    // -------------------------
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

    // -------------------------
    // Export Hành trình -> Excel (.xlsx)
    // -------------------------
    private String exportSelectedToExcel(List<ReportModel> selected) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        try {
            XSSFSheet sheet = workbook.createSheet("HanhTrinh");

            // Header style
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            XSSFFont font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Header row
            XSSFRow header = sheet.createRow(0);
            String[] headers = new String[] {
                    "MaPhien", "MaXe", "BienSo", "ThoiDiemBatDau", "ThoiDiemKetThuc", "TongGioLai", "TongKmTrongNgay",
                    "LoaiXe", "HangSX", "MauSac", "SoHieu", "NhienLieu", "SoKmTong", "TrangThaiXe",
                    "ChuXe_HoTen", "ChuXe_SDT", "ChuXe_CCCD", "ChuXe_VaiTro"
            };
            for (int i = 0; i < headers.length; i++) {
                XSSFCell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // track max length for column width
            int[] maxLens = new int[headers.length];
            for (int i = 0; i < headers.length; i++) maxLens[i] = headers[i].length();

            int rowIndex = 1;
            for (ReportModel r : selected) {
                XSSFRow row = sheet.createRow(rowIndex++);

                int c = 0;
                String v0 = String.valueOf(r.maPhien);
                row.createCell(c).setCellValue(v0); maxLens[c] = Math.max(maxLens[c], v0.length()); c++;

                String v1 = String.valueOf(r.maXe);
                row.createCell(c).setCellValue(v1); maxLens[c] = Math.max(maxLens[c], v1.length()); c++;

                String v2 = r.bienSo != null ? r.bienSo : "";
                row.createCell(c).setCellValue(v2); maxLens[c] = Math.max(maxLens[c], v2.length()); c++;

                String v3 = r.thoiDiemBatDau != null ? r.thoiDiemBatDau : "";
                row.createCell(c).setCellValue(v3); maxLens[c] = Math.max(maxLens[c], v3.length()); c++;

                String v4 = r.thoiDiemKetThuc != null ? r.thoiDiemKetThuc : "";
                row.createCell(c).setCellValue(v4); maxLens[c] = Math.max(maxLens[c], v4.length()); c++;

                String v5 = String.valueOf(r.tongGioLai);
                row.createCell(c).setCellValue(r.tongGioLai); maxLens[c] = Math.max(maxLens[c], v5.length()); c++;

                String v6 = String.valueOf(r.tongKmTrongNgay);
                row.createCell(c).setCellValue(r.tongKmTrongNgay); maxLens[c] = Math.max(maxLens[c], v6.length()); c++;

                String v7 = r.loaiXe != null ? r.loaiXe : "";
                row.createCell(c).setCellValue(v7); maxLens[c] = Math.max(maxLens[c], v7.length()); c++;

                String v8 = r.hangSX != null ? r.hangSX : "";
                row.createCell(c).setCellValue(v8); maxLens[c] = Math.max(maxLens[c], v8.length()); c++;

                String v9 = r.mauSac != null ? r.mauSac : "";
                row.createCell(c).setCellValue(v9); maxLens[c] = Math.max(maxLens[c], v9.length()); c++;

                String v10 = r.soHieu != null ? r.soHieu : "";
                row.createCell(c).setCellValue(v10); maxLens[c] = Math.max(maxLens[c], v10.length()); c++;

                String v11 = r.nhienLieu != null ? r.nhienLieu : "";
                row.createCell(c).setCellValue(v11); maxLens[c] = Math.max(maxLens[c], v11.length()); c++;

                String v12 = String.valueOf(r.soKmTong);
                row.createCell(c).setCellValue(r.soKmTong); maxLens[c] = Math.max(maxLens[c], v12.length()); c++;

                String v13 = r.trangThaiXe != null ? r.trangThaiXe : "";
                row.createCell(c).setCellValue(v13); maxLens[c] = Math.max(maxLens[c], v13.length()); c++;

                String v14 = r.chuXeHoTen != null ? r.chuXeHoTen : "";
                row.createCell(c).setCellValue(v14); maxLens[c] = Math.max(maxLens[c], v14.length()); c++;

                String v15 = r.chuXeSDT != null ? r.chuXeSDT : "";
                row.createCell(c).setCellValue(v15); maxLens[c] = Math.max(maxLens[c], v15.length()); c++;

                String v16 = r.chuXeCCCD != null ? r.chuXeCCCD : "";
                row.createCell(c).setCellValue(v16); maxLens[c] = Math.max(maxLens[c], v16.length()); c++;

                String v17 = r.chuXeVaiTro != null ? r.chuXeVaiTro : "";
                row.createCell(c).setCellValue(v17); maxLens[c] = Math.max(maxLens[c], v17.length()); c++;
            }

            // set column widths (based on char count)
            for (int i = 0; i < headers.length; i++) {
                int charWidth = maxLens[i] + 2;
                if (charWidth > 255) charWidth = 255;
                sheet.setColumnWidth(i, charWidth * 256);
            }

            // Write file
            File docsDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (docsDir == null) docsDir = requireContext().getFilesDir();
            File outDir = new File(docsDir, "exports");
            if (!outDir.exists()) outDir.mkdirs();

            String fileName = "hanhtrinh_export_" + System.currentTimeMillis() + ".xlsx";
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

    // -------------------------
    // Export BaoTri -> Excel (.xlsx)
    // -------------------------
    private String exportBaoTriToExcel(List<BaoTriModel> list) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        try {
            XSSFSheet sheet = workbook.createSheet("BaoTri");

            String[] headers = new String[] {"MaBaoTri", "MaXe", "BienSo", "NgayBaoTri", "LoaiBaoTri", "MoTa", "ChiPhi", "TrangThai"};
            XSSFRow header = sheet.createRow(0);

            XSSFCellStyle headerStyle = workbook.createCellStyle();
            XSSFFont font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            for (int i = 0; i < headers.length; i++) {
                XSSFCell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int[] maxLens = new int[headers.length];
            for (int i = 0; i < headers.length; i++) maxLens[i] = headers[i].length();

            int rowIndex = 1;
            for (BaoTriModel b : list) {
                XSSFRow row = sheet.createRow(rowIndex++);
                int c = 0;

                String s0 = String.valueOf(b.maBaoTri);
                row.createCell(c).setCellValue(s0); maxLens[c] = Math.max(maxLens[c], s0.length()); c++;

                String s1 = String.valueOf(b.maXe);
                row.createCell(c).setCellValue(s1); maxLens[c] = Math.max(maxLens[c], s1.length()); c++;

                String s2 = b.bienSo != null ? b.bienSo : "";
                row.createCell(c).setCellValue(s2); maxLens[c] = Math.max(maxLens[c], s2.length()); c++;

                String s3 = b.ngayBaoTri != null ? b.ngayBaoTri : "";
                row.createCell(c).setCellValue(s3); maxLens[c] = Math.max(maxLens[c], s3.length()); c++;

                String s4 = b.loaiBaoTri != null ? b.loaiBaoTri : "";
                row.createCell(c).setCellValue(s4); maxLens[c] = Math.max(maxLens[c], s4.length()); c++;

                String s5 = b.moTa != null ? b.moTa : "";
                row.createCell(c).setCellValue(s5); maxLens[c] = Math.max(maxLens[c], s5.length()); c++;

                String s6 = String.valueOf(b.chiPhi);
                row.createCell(c).setCellValue(b.chiPhi); maxLens[c] = Math.max(maxLens[c], s6.length()); c++;

                String s7 = b.trangThai != null ? b.trangThai : "";
                row.createCell(c).setCellValue(s7); maxLens[c] = Math.max(maxLens[c], s7.length()); c++;
            }

            // set widths
            for (int i = 0; i < headers.length; i++) {
                int charWidth = maxLens[i] + 2;
                if (charWidth > 255) charWidth = 255;
                sheet.setColumnWidth(i, charWidth * 256);
            }

            // Write file
            File docsDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (docsDir == null) docsDir = requireContext().getFilesDir();
            File outDir = new File(docsDir, "exports");
            if (!outDir.exists()) outDir.mkdirs();

            String fileName = "baotri_export_" + System.currentTimeMillis() + ".xlsx";
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
