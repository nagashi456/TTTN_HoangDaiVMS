package com.example.tttn_hoangdaivms.EditVehicle;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tttn_hoangdaivms.Database.Database;
import com.example.tttn_hoangdaivms.DriverList.DriverListModel;
import com.example.tttn_hoangdaivms.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditVehicleFragment extends Fragment {

    public static final String ARG_MA_XE = "MaXe";
    private int maXe = -1;

    private ImageView ivBack;
    private EditText edtBienSo, edtLoaiXe, edtHangSX, edtMauSac, edtSoHieuLop, edtNhienLieu;
    private MaterialAutoCompleteTextView actvAssignDriver;
    private EditText edtSoHopDong, edtNgayBatDau, edtNgayKetThuc, edtCongTyBH;
    private EditText edtNoiDung, edtNgayGanNhat, edtDonViThucHien;
    private MaterialButton btnSave;

    private Database dbHelper;

    // driver list mapping (name -> id). We keep parallel arrays to show.
    private final List<String> driverNames = new ArrayList<>();
    private final List<String> driverIds = new ArrayList<>();
    private final Map<String, Integer> driverNameToId = new HashMap<>();

    // date format
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public EditVehicleFragment() { /* required empty ctor */ }

    public static EditVehicleFragment newInstance(int maXe) {
        EditVehicleFragment f = new EditVehicleFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_MA_XE, maXe);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.edit_vehicle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new Database(requireContext());

        // get views
        ivBack = view.findViewById(R.id.ivBack);
        edtBienSo = view.findViewById(R.id.edtBienSo);
        edtLoaiXe = view.findViewById(R.id.edtLoaiXe);
        edtHangSX = view.findViewById(R.id.edtHangSX);
        edtMauSac = view.findViewById(R.id.edtMauSac);
        edtSoHieuLop = view.findViewById(R.id.edtSoHieuLop);
        edtNhienLieu = view.findViewById(R.id.edtNhienLieu);
        actvAssignDriver = view.findViewById(R.id.actvAssignDriver);

        edtSoHopDong = view.findViewById(R.id.edtSoHopDong);
        edtNgayBatDau = view.findViewById(R.id.edtNgayBatDau);
        edtNgayKetThuc = view.findViewById(R.id.edtNgayKetThuc);
        edtCongTyBH = view.findViewById(R.id.edtCongTyBH);

        edtNoiDung = view.findViewById(R.id.edtNoiDung);
        edtNgayGanNhat = view.findViewById(R.id.edtNgayGanNhat);
        edtDonViThucHien = view.findViewById(R.id.edtDonViThucHien);

        btnSave = view.findViewById(R.id.btnSave);

        // back
        ivBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
        });

        // parse maXe (nếu có)
        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_MA_XE)) {
            maXe = args.getInt(ARG_MA_XE, -1);
        }

        // load drivers for dropdown
        loadDriversForDropdown();

        // setup date pickers
        edtNgayBatDau.setOnClickListener(v -> showDatePicker(edtNgayBatDau));
        edtNgayKetThuc.setOnClickListener(v -> showDatePicker(edtNgayKetThuc));
        edtNgayGanNhat.setOnClickListener(v -> showDatePicker(edtNgayGanNhat));

        // if maXe provided -> load data
        if (maXe != -1) {
            loadVehicleData(maXe);
        } else {
            prefillEmptyPlaceholders();
        }

        // save click
        btnSave.setOnClickListener(v -> saveVehicle());

    }

    private void prefillEmptyPlaceholders() {
        // show "Chưa có thông tin" as hint where appropriate
        edtBienSo.setHint("Chưa có thông tin");
        edtLoaiXe.setHint("Chưa có thông tin");
        edtHangSX.setHint("Chưa có thông tin");
        edtMauSac.setHint("Chưa có thông tin");
        edtSoHieuLop.setHint("Chưa có thông tin");
//        edtNhienLieu.setHint("Chưa có thông tin");

        edtSoHopDong.setHint("Chưa có thông tin");
        edtNgayBatDau.setHint("Chưa có thông tin");
        edtNgayKetThuc.setHint("Chưa có thông tin");
        edtCongTyBH.setHint("Chưa có thông tin");

        edtNoiDung.setHint("Chưa có thông tin");
        edtNgayGanNhat.setHint("Chưa có thông tin");
        edtDonViThucHien.setHint("Chưa có thông tin");
    }

    private void showDatePicker(final EditText target) {
        final Calendar c = Calendar.getInstance();
        DatePickerDialog dpd = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(year, month, dayOfMonth);
                    target.setText(dateFormat.format(picked.getTime()));
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dpd.show();
    }

    private void loadDriversForDropdown() {
        driverNames.clear();
        driverIds.clear();
        driverNameToId.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = null;
        try {
            // Lấy tất cả NguoiDung có VaiTro chứa "tài" hoặc "nhân viên" (tuỳ schema)
            c = db.rawQuery("SELECT MaNguoiDung, HoTen FROM NguoiDung ORDER BY HoTen COLLATE NOCASE", null);
            if (c != null && c.moveToFirst()) {
                do {
                    int id = -1;
                    try {
                        id = c.getInt(0);
                    } catch (Exception ignored) {}
                    String name = "";
                    try {
                        name = c.getString(1);
                    } catch (Exception ignored) {}
                    if (TextUtils.isEmpty(name)) name = "Không rõ (" + id + ")";
                    driverNames.add(name);
                    driverIds.add(String.valueOf(id));
                    driverNameToId.put(name, id);
                } while (c.moveToNext());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (c != null) c.close();
            // set adapter
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line, driverNames);
            actvAssignDriver.setAdapter(adapter);
        }

        // if empty, hide rowAssignDriver view (optional)
        View rowAssign = getView() != null ? getView().findViewById(R.id.rowAssignDriver) : null;
        if (driverNames.isEmpty() && rowAssign != null) rowAssign.setVisibility(View.GONE);
    }

    private void loadVehicleData(int maXe) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cXe = null;
        Cursor cBaoTri = null;
        Cursor cBaoHiem = null;

        try {
            cXe = db.rawQuery("SELECT MaXe, MaNguoiDung, BienSo, LoaiXe, HangSX, MauSac, SoHieu FROM Xe WHERE MaXe = ?",
                    new String[]{String.valueOf(maXe)});
            if (cXe != null && cXe.moveToFirst()) {
                int maNguoiDung = -1;
                try { maNguoiDung = cXe.getInt(cXe.getColumnIndexOrThrow("MaNguoiDung")); } catch (Exception ignored) {}
                String bienSo = safeGet(cXe, "BienSo");
                String loaiXe = safeGet(cXe, "LoaiXe");
                String hangSX = safeGet(cXe, "HangSX");
                String mauSac = safeGet(cXe, "MauSac");
                String soHieu = safeGet(cXe, "SoHieu");
//                String nhienLieu = safeGet(cXe, "NhiênLieu"); // nếu cột tên khác thì điều chỉnh

                edtBienSo.setText(!TextUtils.isEmpty(bienSo) ? bienSo : "");
                edtLoaiXe.setText(!TextUtils.isEmpty(loaiXe) ? loaiXe : "");
                edtHangSX.setText(!TextUtils.isEmpty(hangSX) ? hangSX : "");
                edtMauSac.setText(!TextUtils.isEmpty(mauSac) ? mauSac : "");
                edtSoHieuLop.setText(!TextUtils.isEmpty(soHieu) ? soHieu : "");
//                edtNhienLieu.setText(!TextUtils.isEmpty(nhienLieu) ? nhienLieu : "");

                // set driver selection if available
                if (maNguoiDung != -1) {
                    // tìm tên tương ứng trong driverIds
                    String idStr = String.valueOf(maNguoiDung);
                    int idx = driverIds.indexOf(idStr);
                    if (idx >= 0) {
                        actvAssignDriver.setText(driverNames.get(idx), false);
                    } else {
                        // nếu driver list không chứa (có thể do filter), truy vấn tên trực tiếp
                        Cursor cName = null;
                        try {
                            cName = db.rawQuery("SELECT HoTen FROM NguoiDung WHERE MaNguoiDung = ?", new String[]{idStr});
                            if (cName != null && cName.moveToFirst()) {
                                String name = safeGet(cName, "0");
                                if (!TextUtils.isEmpty(name)) {
                                    actvAssignDriver.setText(name, false);
                                    driverNameToId.put(name, maNguoiDung);
                                    driverNames.add(name);
                                    driverIds.add(idStr);
                                }
                            }
                        } finally {
                            if (cName != null) cName.close();
                        }
                    }
                }

                // BaoTri: lấy bản mới nhất (MaXe)
                cBaoTri = db.rawQuery("SELECT MaBaoTri, NgayGanNhat, NoiDung, DonVi FROM BaoTri WHERE MaXe = ? ORDER BY NgayGanNhat DESC, MaBaoTri DESC LIMIT 1",
                        new String[]{String.valueOf(maXe)});
                if (cBaoTri != null && cBaoTri.moveToFirst()) {
                    String ngay = safeGet(cBaoTri, "NgayGanNhat");
                    String noiDung = safeGet(cBaoTri, "NoiDung");
                    String donVi = safeGet(cBaoTri, "DonVi");

                    edtNgayGanNhat.setText(!TextUtils.isEmpty(ngay) ? ngay : "");
                    edtNoiDung.setText(!TextUtils.isEmpty(noiDung) ? noiDung : "");
                    edtDonViThucHien.setText(!TextUtils.isEmpty(donVi) ? donVi : "");
                }

                // BaoHiem: lấy bản mới nhất theo MaTaiXe = MaNguoiDung
                if (maNguoiDung != -1) {
                    cBaoHiem = db.rawQuery("SELECT MaBaoHiem, SoHD, NgayBatDau, NgayKetThuc, CongTy FROM BaoHiem WHERE MaTaiXe = ? ORDER BY MaBaoHiem DESC LIMIT 1",
                            new String[]{String.valueOf(maNguoiDung)});
                    if (cBaoHiem != null && cBaoHiem.moveToFirst()) {
                        String soHD = safeGet(cBaoHiem, "SoHD");
                        String ngayBD = safeGet(cBaoHiem, "NgayBatDau");
                        String ngayKT = safeGet(cBaoHiem, "NgayKetThuc");
                        String congTy = safeGet(cBaoHiem, "CongTy");

                        edtSoHopDong.setText(!TextUtils.isEmpty(soHD) ? soHD : "");
                        edtNgayBatDau.setText(!TextUtils.isEmpty(ngayBD) ? ngayBD : "");
                        edtNgayKetThuc.setText(!TextUtils.isEmpty(ngayKT) ? ngayKT : "");
                        edtCongTyBH.setText(!TextUtils.isEmpty(congTy) ? congTy : "");
                    }
                }

            } else {
                // không tìm thấy -> thông báo, vẫn cho người dùng tạo mới
                Toast.makeText(requireContext(), "Không tìm thấy thông tin xe. Bạn có thể tạo mới.", Toast.LENGTH_SHORT).show();
                prefillEmptyPlaceholders();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(requireContext(), "Lỗi khi tải dữ liệu: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (cXe != null) cXe.close();
            if (cBaoTri != null) cBaoTri.close();
            if (cBaoHiem != null) cBaoHiem.close();
        }
    }

    private String safeGet(Cursor c, String col) {
        if (c == null) return "";
        try {
            int idx = c.getColumnIndex(col);
            if (idx == -1) return "";
            if (c.isNull(idx)) return "";
            String s = c.getString(idx);
            return s != null ? s : "";
        } catch (Exception e) {
            try {
                return c.getString(0);
            } catch (Exception ex) {
                return "";
            }
        }
    }

    private void saveVehicle() {
        // Gather values from UI
        String bienSo = edtBienSo.getText() == null ? "" : edtBienSo.getText().toString().trim();
        String loaiXe = edtLoaiXe.getText() == null ? "" : edtLoaiXe.getText().toString().trim();
        String hangSX = edtHangSX.getText() == null ? "" : edtHangSX.getText().toString().trim();
        String mauSac = edtMauSac.getText() == null ? "" : edtMauSac.getText().toString().trim();
        String soHieu = edtSoHieuLop.getText() == null ? "" : edtSoHieuLop.getText().toString().trim();
//        String nhienLieu = edtNhienLieu.getText() == null ? "" : edtNhienLieu.getText().toString().trim();

        String driverName = actvAssignDriver.getText() == null ? "" : actvAssignDriver.getText().toString().trim();
        int maTaiXe = -1;
        if (!TextUtils.isEmpty(driverName) && driverNameToId.containsKey(driverName)) {
            maTaiXe = driverNameToId.get(driverName);
        }

        // Bao hiem
        String soHD = edtSoHopDong.getText() == null ? "" : edtSoHopDong.getText().toString().trim();
        String ngayBatDau = edtNgayBatDau.getText() == null ? "" : edtNgayBatDau.getText().toString().trim();
        String ngayKetThuc = edtNgayKetThuc.getText() == null ? "" : edtNgayKetThuc.getText().toString().trim();
        String congTy = edtCongTyBH.getText() == null ? "" : edtCongTyBH.getText().toString().trim();

        // Bao tri
        String noiDung = edtNoiDung.getText() == null ? "" : edtNoiDung.getText().toString().trim();
        String ngayGanNhat = edtNgayGanNhat.getText() == null ? "" : edtNgayGanNhat.getText().toString().trim();
        String donVi = edtDonViThucHien.getText() == null ? "" : edtDonViThucHien.getText().toString().trim();

        // Basic validation: biển số không rỗng
        if (TextUtils.isEmpty(bienSo)) {
            Toast.makeText(requireContext(), "Vui lòng nhập biển số.", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            long targetMaXe = maXe;

            if (maXe == -1) {
                // Insert new Xe
                ContentValues vXe = new ContentValues();
                vXe.put("MaNguoiDung", maTaiXe != -1 ? maTaiXe : (Integer) null);
                vXe.put("BienSo", bienSo);
                vXe.put("LoaiXe", loaiXe);
                vXe.put("HangSX", hangSX);
                vXe.put("MauSac", mauSac);
                vXe.put("SoHieu", soHieu);
//                vXe.put("NhiênLieu", nhienLieu); // nếu tên column khác thì sửa lại
                long newId = db.insert("Xe", null, vXe);
                if (newId == -1) throw new Exception("Không thể tạo bản ghi Xe mới.");
                targetMaXe = newId;
            } else {
                // Update existing Xe
                ContentValues vXe = new ContentValues();
                if (maTaiXe != -1) vXe.put("MaNguoiDung", maTaiXe); else vXe.putNull("MaNguoiDung");
                vXe.put("BienSo", bienSo);
                vXe.put("LoaiXe", loaiXe);
                vXe.put("HangSX", hangSX);
                vXe.put("MauSac", mauSac);
                vXe.put("SoHieu", soHieu);
//                vXe.put("NhiênLieu", nhienLieu);
                int updated = db.update("Xe", vXe, "MaXe = ?", new String[]{String.valueOf(maXe)});
                if (updated <= 0) {
                    throw new Exception("Cập nhật xe thất bại.");
                }
            }

            // Handle BaoTri (by MaXe): update latest if exists else insert if user provided any info
            boolean hasBaoTriInput = !TextUtils.isEmpty(ngayGanNhat) || !TextUtils.isEmpty(noiDung) || !TextUtils.isEmpty(donVi);
            if (hasBaoTriInput) {
                // try get latest MaBaoTri for this MaXe
                Cursor cbt = null;
                try {
                    cbt = db.rawQuery("SELECT MaBaoTri FROM BaoTri WHERE MaXe = ? ORDER BY NgayGanNhat DESC, MaBaoTri DESC LIMIT 1",
                            new String[]{String.valueOf(targetMaXe)});
                    if (cbt != null && cbt.moveToFirst()) {
                        int maBaoTri = cbt.getInt(0);
                        ContentValues vBT = new ContentValues();
                        vBT.put("NgayGanNhat", ngayGanNhat);
                        vBT.put("NoiDung", noiDung);
                        vBT.put("DonVi", donVi);
                        int up = db.update("BaoTri", vBT, "MaBaoTri = ?", new String[]{String.valueOf(maBaoTri)});
                        if (up <= 0) {
                            // as fallback insert new
                            ContentValues ins = new ContentValues();
                            ins.put("MaXe", targetMaXe);
                            ins.put("NgayGanNhat", ngayGanNhat);
                            ins.put("NoiDung", noiDung);
                            ins.put("DonVi", donVi);
                            db.insert("BaoTri", null, ins);
                        }
                    } else {
                        // insert new BaoTri
                        ContentValues ins = new ContentValues();
                        ins.put("MaXe", targetMaXe);
                        ins.put("NgayGanNhat", ngayGanNhat);
                        ins.put("NoiDung", noiDung);
                        ins.put("DonVi", donVi);
                        db.insert("BaoTri", null, ins);
                    }
                } finally {
                    if (cbt != null) cbt.close();
                }
            }

            // Handle BaoHiem (by MaTaiXe): only if we have a MaTaiXe
            boolean hasBaoHiemInput = !TextUtils.isEmpty(soHD) || !TextUtils.isEmpty(ngayBatDau) || !TextUtils.isEmpty(ngayKetThuc) || !TextUtils.isEmpty(congTy);
            if (maTaiXe != -1 && hasBaoHiemInput) {
                Cursor cbh = null;
                try {
                    cbh = db.rawQuery("SELECT MaBaoHiem FROM BaoHiem WHERE MaTaiXe = ? ORDER BY MaBaoHiem DESC LIMIT 1",
                            new String[]{String.valueOf(maTaiXe)});
                    if (cbh != null && cbh.moveToFirst()) {
                        int maBaoHiem = cbh.getInt(0);
                        ContentValues vBH = new ContentValues();
                        vBH.put("SoHD", soHD);
                        vBH.put("NgayBatDau", ngayBatDau);
                        vBH.put("NgayKetThuc", ngayKetThuc);
                        vBH.put("CongTy", congTy);
                        int up = db.update("BaoHiem", vBH, "MaBaoHiem = ?", new String[]{String.valueOf(maBaoHiem)});
                        if (up <= 0) {
                            ContentValues ins = new ContentValues();
                            ins.put("MaTaiXe", maTaiXe);
                            ins.put("SoHD", soHD);
                            ins.put("NgayBatDau", ngayBatDau);
                            ins.put("NgayKetThuc", ngayKetThuc);
                            ins.put("CongTy", congTy);
                            db.insert("BaoHiem", null, ins);
                        }
                    } else {
                        ContentValues ins = new ContentValues();
                        ins.put("MaTaiXe", maTaiXe);
                        ins.put("SoHD", soHD);
                        ins.put("NgayBatDau", ngayBatDau);
                        ins.put("NgayKetThuc", ngayKetThuc);
                        ins.put("CongTy", congTy);
                        db.insert("BaoHiem", null, ins);
                    }
                } finally {
                    if (cbh != null) cbh.close();
                }
            }

            db.setTransactionSuccessful();

            // success
            Toast.makeText(requireContext(), "Lưu thành công.", Toast.LENGTH_SHORT).show();

            // go back
            if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();

        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(requireContext(), "Lỗi khi lưu: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            try {
                if (db != null) {
                    db.endTransaction();
                    // do not close db here (Database helper manages it), but it's safe to call db.close() if you prefer
                }
            } catch (Exception ignored) {}
        }
    }
}

