package com.example.tttn_hoangdaivms.BaoTriDetail;

import android.database.Cursor;
import android.graphics.Color;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * BaoTriDetailFragment - hiển thị chi tiết 1 record bảo trì và export sang .xlsx
 *
 * Cách dùng:
 * 1) Truyền maBaoTri:
 *    Fragment f = BaoTriDetailFragment.newInstanceWithId(maBaoTri);
 *
 * 2) Hoặc truyền BaoTriModel (nếu bạn đã có và class implements Serializable):
 *    Fragment f = BaoTriDetailFragment.newInstance(baoTriModel);
 */
public class BaoTriDetailFragment extends Fragment {

    private static final String ARG_BAOTRI = "arg_baotri";   // BaoTriModel as Serializable (optional)
    private static final String ARG_MABAOTRI = "arg_maBaoTri";

    private Database db;

    // Views
    private ImageView btnBack;
    private TextView toolbarTitle;
    private TextView tvMaBaoTri, tvMaXe, tvBienSo, tvNgayBaoTri,
            tvNoiDung, tvDonVi,  tvTrangThaiBaoTri, tvChuXeHoTen, tvChuXeSDT;
    private Button btnExport;

    // Model
    private BaoTriModel baoTri;

    public BaoTriDetailFragment() { /* required empty */ }

    // create with BaoTriModel (if your BaoTriModel implements Serializable)
    public static BaoTriDetailFragment newInstance(BaoTriModel model) {
        BaoTriDetailFragment f = new BaoTriDetailFragment();
        Bundle b = new Bundle();
        b.putSerializable(ARG_BAOTRI, model);
        f.setArguments(b);
        return f;
    }

    // create with id
    public static BaoTriDetailFragment newInstanceWithId(int maBaoTri) {
        BaoTriDetailFragment f = new BaoTriDetailFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_MABAOTRI, maBaoTri);
        f.setArguments(b);
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_baotri_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new Database(requireContext());

        // bind views
        btnBack = view.findViewById(R.id.btnBack);
        toolbarTitle = view.findViewById(R.id.toolbarTitle);

        tvMaBaoTri = view.findViewById(R.id.tvMaBaoTri);
        tvMaXe = view.findViewById(R.id.tvMaXe);
        tvBienSo = view.findViewById(R.id.tvBienSo);
        tvNgayBaoTri = view.findViewById(R.id.tvNgayBaoTri);
        tvNoiDung = view.findViewById(R.id.tvNoiDung);
        tvDonVi = view.findViewById(R.id.tvDonVi);
        tvTrangThaiBaoTri = view.findViewById(R.id.tvTrangThaiBaoTri);
        tvChuXeHoTen = view.findViewById(R.id.tvChuXeHoTen);
        tvChuXeSDT = view.findViewById(R.id.tvChuXeSDT);

        btnExport = view.findViewById(R.id.btnExport);

        // back
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
            // try BaoTriModel first (if caller put object)
            Object o = args.getSerializable(ARG_BAOTRI);
            if (o instanceof BaoTriModel) {
                baoTri = (BaoTriModel) o;
            } else if (args.containsKey(ARG_MABAOTRI)) {
                int ma = args.getInt(ARG_MABAOTRI, -1);
                if (ma > 0) loadBaoTriFromDb(ma);
            } else {
                // maybe individual fields passed (compatibility) - try to read common keys
                if (args.containsKey("MaBaoTri")) {
                    baoTri = new BaoTriModel();
                    baoTri.maBaoTri = args.getInt("MaBaoTri", 0);
                    baoTri.maXe = args.getInt("MaXe", 0);
                    baoTri.bienSo = args.getString("BienSo");
                    baoTri.ngayBaoTri = args.getString("NgayBaoTri");
                    baoTri.moTa = args.getString("NoiDung");
                    baoTri.donVi = args.getString("DonVi");
                    baoTri.trangThai = args.getString("TrangThai");
                    baoTri.chuXeHoTen = args.getString("ChuXeHoTen");
                    baoTri.chuXeSDT = args.getString("ChuXeSDT");
                }
            }
        }

        if (baoTri == null) {
            // nothing to show
            Toast.makeText(requireContext(), "Không tìm thấy dữ liệu bảo trì", Toast.LENGTH_SHORT).show();
            return;
        }

        bindDataToViews(baoTri);

        btnExport.setOnClickListener(v -> {
            String path = exportBaoTriToExcel(baoTri);
            if (path != null) {
                Toast.makeText(requireContext(), "Đã xuất: " + path, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(requireContext(), "Xuất lỗi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindDataToViews(BaoTriModel b) {
        toolbarTitle.setText("Bảo trì " + b.maBaoTri);

        tvMaBaoTri.setText(String.valueOf(b.maBaoTri));
        tvMaXe.setText(String.valueOf(b.maXe));
        tvBienSo.setText(notNull(b.bienSo));
        tvNgayBaoTri.setText(notNull(b.ngayBaoTri));
        tvNoiDung.setText(notNull(b.moTa));
        tvDonVi.setText(notNull(b.donVi));


        // trang thai (e.g., "Trễ 10 ngày")
        tvTrangThaiBaoTri.setText(notNull(b.trangThai));

        tvChuXeHoTen.setText(notNull(b.chuXeHoTen));
        tvChuXeSDT.setText(notNull(b.chuXeSDT));
    }

    private String notNull(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    /**
     * Load BaoTriModel by MaBaoTri from DB
     * Query joins Xe and NguoiDung to fetch BienSo and owner info.
     */
    private void loadBaoTriFromDb(int maBaoTri) {
        String sql = "SELECT B.MaBaoTri, B.MaXe, B.NgayGanNhat AS NgayBaoTri, B.NoiDung AS MoTa, B.DonVi, " +
                "X.BienSo, ND.HoTen AS ChuHoTen, ND.SDT AS ChuSDT " +
                "FROM BaoTri B " +
                "LEFT JOIN Xe X ON B.MaXe = X.MaXe " +
                "LEFT JOIN NguoiDung ND ON X.MaNguoiDung = ND.MaNguoiDung " +
                "WHERE B.MaBaoTri = ? LIMIT 1";
        Cursor c = null;
        try {
            c = db.getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(maBaoTri)});
            if (c != null && c.moveToFirst()) {
                BaoTriModel b = new BaoTriModel();
                b.maBaoTri = safeGetInt(c, "MaBaoTri");
                b.maXe = safeGetInt(c, "MaXe");
                b.ngayBaoTri = safeGetString(c, "NgayBaoTri");
                b.moTa = safeGetString(c, "MoTa");
                b.donVi = safeGetString(c, "DonVi");
                b.bienSo = safeGetString(c, "BienSo");
                b.chuXeHoTen = safeGetString(c, "ChuHoTen");
                b.chuXeSDT = safeGetString(c, "ChuSDT");
                b.trangThai = computeLateStatus(b.ngayBaoTri); // compute 'Trễ X ngày' if needed
                this.baoTri = b;
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.baoTri = null;
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Compute late status: if last maintenance older than 6 months -> return "Trễ X ngày"
     * If not late -> return empty string.
     * Note: basic implementation using milliseconds; assumes ngayBaoTri formatted "yyyy-MM-dd" or "yyyy-MM-dd HH:mm:ss"
     */
    /**
     * Tính trạng thái trễ của bảo trì dựa trên ngày bảo trì gần nhất.
     * Nếu ngày hiện tại > ngày bảo trì + 6 tháng -> hiển thị "Trễ X ngày".
     * Nếu chưa quá hạn -> trả về chuỗi rỗng "".
     */
    private String computeLateStatus(String ngayBaoTri) {
        if (ngayBaoTri == null || ngayBaoTri.trim().isEmpty()) return "";

        // Thử parse theo 2 định dạng thường gặp
        String[] patterns = new String[] {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"};
        Date parsedDate = null;

        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
                sdf.setLenient(false);
                parsedDate = sdf.parse(ngayBaoTri);
                if (parsedDate != null) break;
            } catch (ParseException ignored) {}
        }

        if (parsedDate == null) return "";

        // Ngày hết hạn = ngày bảo trì + 6 tháng
        Calendar dueDate = Calendar.getInstance();
        dueDate.setTime(parsedDate);
        dueDate.add(Calendar.MONTH, 6);

        Calendar now = Calendar.getInstance();

        // Nếu đã quá hạn
        if (now.after(dueDate)) {
            long diffMillis = now.getTimeInMillis() - dueDate.getTimeInMillis();
            long daysLate = TimeUnit.MILLISECONDS.toDays(diffMillis);
            if (daysLate <= 0) daysLate = 1; // ít nhất 1 ngày
            return "Trễ " + daysLate + " ngày";
        }
        tvTrangThaiBaoTri.setTextColor(Color.parseColor("#33CC00"));
        // Nếu chưa đến hạn
        return "Đã bảo trì";
    }

    // safe cursor getters
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

    /**
     * Export single BaoTriModel to .xlsx.
     * Creates a "Detail" sheet (Field/Value) and a "BaoTriRow" sheet (header + single row).
     */
    private String exportBaoTriToExcel(BaoTriModel b) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        try {
            // Sheet 1: Detail (key/value)
            XSSFSheet detail = workbook.createSheet("Detail");

            XSSFCellStyle keyStyle = workbook.createCellStyle();
            XSSFFont bold = workbook.createFont();
            bold.setBold(true);
            keyStyle.setFont(bold);
            keyStyle.setAlignment(HorizontalAlignment.LEFT);

            String[][] kv = new String[][]{
                    {"MaBaoTri", String.valueOf(b.maBaoTri)},
                    {"MaXe", String.valueOf(b.maXe)},
                    {"BienSo", notNull(b.bienSo)},
                    {"NgayBaoTri", notNull(b.ngayBaoTri)},
                    {"MoTa", notNull(b.moTa)},
                    {"DonVi", notNull(b.donVi)},
                    {"TrangThai", notNull(b.trangThai)},
                    {"ChuXe_HoTen", notNull(b.chuXeHoTen)},
                    {"ChuXe_SDT", notNull(b.chuXeSDT)}
            };

            int r = 0;
            int maxKey = 0, maxVal = 0;
            for (String[] p : kv) {
                XSSFRow row = detail.createRow(r++);
                XSSFCell c0 = row.createCell(0);
                c0.setCellValue(p[0]);
                c0.setCellStyle(keyStyle);
                row.createCell(1).setCellValue(p[1]);

                if (p[0] != null) maxKey = Math.max(maxKey, p[0].length());
                if (p[1] != null) maxVal = Math.max(maxVal, p[1].length());
            }

            detail.setColumnWidth(0, (maxKey + 4) * 256);
            detail.setColumnWidth(1, (maxVal + 4) * 256);

            // Sheet 2: Row (header + single row)
            XSSFSheet rowSheet = workbook.createSheet("BaoTriRow");
            String[] headers = new String[] {"MaBaoTri", "MaXe", "BienSo", "NgayBaoTri", "MoTa", "DonVi", "TrangThai", "ChuXe_HoTen", "ChuXe_SDT"};
            XSSFRow headerRow = rowSheet.createRow(0);

            XSSFCellStyle headerStyle = workbook.createCellStyle();
            XSSFFont hfont = workbook.createFont();
            hfont.setBold(true);
            headerStyle.setFont(hfont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            int[] maxLens = new int[headers.length];
            for (int i = 0; i < headers.length; i++) {
                XSSFCell hc = headerRow.createCell(i);
                hc.setCellValue(headers[i]);
                hc.setCellStyle(headerStyle);
                maxLens[i] = headers[i].length();
            }

            XSSFRow dataRow = rowSheet.createRow(1);
            int c = 0;

            String s0 = String.valueOf(b.maBaoTri); dataRow.createCell(c).setCellValue(s0); maxLens[c] = Math.max(maxLens[c], s0.length()); c++;
            String s1 = String.valueOf(b.maXe); dataRow.createCell(c).setCellValue(s1); maxLens[c] = Math.max(maxLens[c], s1.length()); c++;
            String s2 = notNull(b.bienSo); dataRow.createCell(c).setCellValue(s2); maxLens[c] = Math.max(maxLens[c], s2.length()); c++;
            String s3 = notNull(b.ngayBaoTri); dataRow.createCell(c).setCellValue(s3); maxLens[c] = Math.max(maxLens[c], s3.length()); c++;
            String s5 = notNull(b.moTa); dataRow.createCell(c).setCellValue(s5); maxLens[c] = Math.max(maxLens[c], s5.length()); c++;
            String s6 = notNull(b.donVi); dataRow.createCell(c).setCellValue(s6); maxLens[c] = Math.max(maxLens[c], s6.length()); c++;
            String s8 = notNull(b.trangThai); dataRow.createCell(c).setCellValue(s8); maxLens[c] = Math.max(maxLens[c], s8.length()); c++;
            String s9 = notNull(b.chuXeHoTen); dataRow.createCell(c).setCellValue(s9); maxLens[c] = Math.max(maxLens[c], s9.length()); c++;
            String s10 = notNull(b.chuXeSDT); dataRow.createCell(c).setCellValue(s10); maxLens[c] = Math.max(maxLens[c], s10.length()); c++;

            // set widths
            for (int i = 0; i < headers.length; i++) {
                int width = maxLens[i] + 4;
                if (width > 255) width = 255;
                rowSheet.setColumnWidth(i, width * 256);
            }

            // Write file to app-specific documents folder
            File docsDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (docsDir == null) docsDir = requireContext().getFilesDir();
            File outDir = new File(docsDir, "exports");
            if (!outDir.exists()) outDir.mkdirs();

            String fileName = "baotri_" + b.maBaoTri + "_" + System.currentTimeMillis() + ".xlsx";
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

    // ---------------------------
    // Simple BaoTriModel (if your project already has one, you can remove this class)
    // ---------------------------
    public static class BaoTriModel implements Serializable {
        public int maBaoTri;
        public int maXe;
        public String bienSo;
        public String ngayBaoTri;
        public String moTa;
        public String donVi;
        public String trangThai;
        public String chuXeHoTen;
        public String chuXeSDT;

        public BaoTriModel() {}
    }
}
