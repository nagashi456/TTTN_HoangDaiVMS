package com.example.tttn_hoangdaivms.EditDriver;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tttn_hoangdaivms.Database.Database;
import com.example.tttn_hoangdaivms.R;

public class EditDriverFragment extends Fragment {
    private static final String ARG_ID = "MaNguoiDungStr";

    // Views from edit_driver.xml
    private ImageView ivBack;
    private EditText etName, etCCCD, etPhone, etEmail, etWorkStatus;
    private EditText etHeight, etWeight, etDisease, etLastCheckup, etDrugTest, etConclusion;
    private EditText etType, etDateExpired, etAddress;
    private View btnSave;

    private Database dbHelper;

    // internal ids
    private String idNguoiDungStr = null;
    private int maNguoiDung = -1;
    private int maTaiKhoan = -1;

    public static EditDriverFragment newInstance(String idNguoiDungStr) {
        EditDriverFragment f = new EditDriverFragment();
        Bundle b = new Bundle();
        b.putString(ARG_ID, idNguoiDungStr);
        f.setArguments(b);
        return f;
    }

    public EditDriverFragment() { /* required */ }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.edit_driver, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // map views
        ivBack = view.findViewById(R.id.ivBack);
        etName = view.findViewById(R.id.etName);
        etCCCD = view.findViewById(R.id.etCCCD);
        etPhone = view.findViewById(R.id.etPhone);
        etEmail = view.findViewById(R.id.etEmail);
        etWorkStatus = view.findViewById(R.id.etWorkStatus);

        etHeight = view.findViewById(R.id.etHeight);
        etWeight = view.findViewById(R.id.etWeight);
        etDisease = view.findViewById(R.id.etDisease);
        etLastCheckup = view.findViewById(R.id.etLastCheckup);
        etDrugTest = view.findViewById(R.id.etDrugTest);
        etConclusion = view.findViewById(R.id.etConclusion);

        etType = view.findViewById(R.id.etType);
        etDateExpired = view.findViewById(R.id.etDateExpired);
        etAddress = view.findViewById(R.id.etAddress);

        btnSave = view.findViewById(R.id.btnSave);

        dbHelper = new Database(requireContext());

        // args
        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_ID)) {
            idNguoiDungStr = args.getString(ARG_ID);
        }

        // load data if id provided
        if (!TextUtils.isEmpty(idNguoiDungStr)) {
            loadData(idNguoiDungStr);
        } else {
            // no id => empty form
            showPlaceholders();
        }

        ivBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                requireActivity().finish();
            }
        });

        btnSave.setOnClickListener(v -> {
            // collect values
            final String name = safeText(etName);
            final String cccd = safeText(etCCCD);
            final String phone = safeText(etPhone);
            final String email = safeText(etEmail);
            final String workStatus = safeText(etWorkStatus);

            final String height = safeText(etHeight);
            final String weight = safeText(etWeight);
            final String disease = safeText(etDisease);
            final String lastCheckup = safeText(etLastCheckup);
            final String drugTest = safeText(etDrugTest);
            final String conclusion = safeText(etConclusion);

            final String licenseType = safeText(etType);
            final String dateExpired = safeText(etDateExpired);
            final String address = safeText(etAddress);

            // run save in background
            new Thread(() -> {
                SQLiteDatabase db = null;
                try {
                    db = dbHelper.getWritableDatabase();

                    // If we don't already know maNguoiDung, try to find it by id string (if id provided)
                    if (maNguoiDung == -1 && !TextUtils.isEmpty(idNguoiDungStr)) {
                        try (Cursor c = db.rawQuery("SELECT MaNguoiDung, MaTaiKhoan FROM NguoiDung WHERE MaNguoiDung = ?",
                                new String[]{idNguoiDungStr})) {
                            if (c != null && c.moveToFirst()) {
                                maNguoiDung = c.getInt(0);
                                if (c.getColumnCount() >= 2) {
                                    try { maTaiKhoan = c.getInt(1); } catch (Exception ignored) {}
                                }
                            }
                        }
                    }

                    if (maNguoiDung != -1) {
                        // UPDATE NguoiDung
                        ContentValues cv = new ContentValues();
                        cv.put("HoTen", name);
                        cv.put("CCCD", cccd);
                        cv.put("SDT", phone);
                        cv.put("TrangThai", workStatus);
                        // (bạn có thể thêm các cột khác nếu schema có)
                        int updated = db.update("NguoiDung", cv, "MaNguoiDung = ?", new String[]{String.valueOf(maNguoiDung)});

                        // update TaiKhoan email nếu có maTaiKhoan
                        if (!TextUtils.isEmpty(email) && maTaiKhoan != -1) {
                            ContentValues cv2 = new ContentValues();
                            cv2.put("Email", email);
                            db.update("TaiKhoan", cv2, "MaTaiKhoan = ?", new String[]{String.valueOf(maTaiKhoan)});
                        }

                    } else {
                        // INSERT new NguoiDung (cơ bản) và lấy id mới
                        ContentValues cv = new ContentValues();
                        cv.put("HoTen", name);
                        cv.put("CCCD", cccd);
                        cv.put("SDT", phone);
                        cv.put("TrangThai", workStatus);
                        long newId = db.insert("NguoiDung", null, cv);
                        if (newId != -1) {
                            maNguoiDung = (int) newId;
                        }
                        // NOTE: nếu cần tạo TaiKhoan mới, bạn phải chèn vào TaiKhoan và liên kết MaTaiKhoan -> NguoiDung
                    }

                    // ===== SucKhoe: update latest if exists, else insert new
                    if (maNguoiDung != -1) {
                        // check if there's an existing record (latest)
                        boolean hasSucKhoe = false;
                        int sucKhoeId = -1;
                        try (Cursor c = db.rawQuery("SELECT MaSucKhoe FROM SucKhoe WHERE MaNguoiDung = ? ORDER BY NgayKham DESC LIMIT 1",
                                new String[]{String.valueOf(maNguoiDung)})) {
                            if (c != null && c.moveToFirst()) {
                                hasSucKhoe = true;
                                sucKhoeId = c.getInt(0);
                            }
                        }

                        ContentValues cvSK = new ContentValues();
                        if (!TextUtils.isEmpty(height)) cvSK.put("ChieuCao", height);
                        if (!TextUtils.isEmpty(weight)) cvSK.put("CanNang", weight);
                        if (!TextUtils.isEmpty(disease)) cvSK.put("BenhNen", disease);
                        if (!TextUtils.isEmpty(lastCheckup)) cvSK.put("NgayKham", lastCheckup);
                        if (!TextUtils.isEmpty(conclusion)) cvSK.put("KetLuan", conclusion);
                        // map drug test label -> code (0/1) nếu có
                        if (!TextUtils.isEmpty(drugTest)) {
                            String dt = drugTest.trim().toLowerCase();
                            if (dt.contains("âm") || dt.contains("am")) cvSK.put("MaTuy", 0);
                            else if (dt.contains("dương") || dt.contains("duong")) cvSK.put("MaTuy", 1);
                            else {
                                // nếu không xác định, lưu nguyên text vào KetLuan hoặc bỏ qua
                            }
                        }
                        cvSK.put("MaNguoiDung", maNguoiDung);

                        if (hasSucKhoe && sucKhoeId != -1) {
                            db.update("SucKhoe", cvSK, "MaSucKhoe = ?", new String[]{String.valueOf(sucKhoeId)});
                        } else {
                            db.insert("SucKhoe", null, cvSK);
                        }
                    }

                    // ===== BangCap: update latest or insert
                    if (maNguoiDung != -1) {
                        boolean hasBangCap = false;
                        int bangCapId = -1;
                        try (Cursor c = db.rawQuery("SELECT MaBangCap FROM BangCap WHERE MaNguoiDung = ? ORDER BY MaBangCap DESC LIMIT 1",
                                new String[]{String.valueOf(maNguoiDung)})) {
                            if (c != null && c.moveToFirst()) {
                                hasBangCap = true;
                                bangCapId = c.getInt(0);
                            }
                        }

                        ContentValues cvBC = new ContentValues();
                        if (!TextUtils.isEmpty(licenseType)) cvBC.put("Loai", licenseType);
                        if (!TextUtils.isEmpty(dateExpired)) cvBC.put("NgayHetHan", dateExpired);
                        if (!TextUtils.isEmpty(address)) cvBC.put("NoiCap", address);
                        cvBC.put("MaNguoiDung", maNguoiDung);

                        if (hasBangCap && bangCapId != -1) {
                            db.update("BangCap", cvBC, "MaBangCap = ?", new String[]{String.valueOf(bangCapId)});
                        } else {
                            db.insert("BangCap", null, cvBC);
                        }
                    }

                    // done -> inform user on UI thread
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Lưu thông tin thành công.", Toast.LENGTH_SHORT).show()
                    );

                } catch (Exception e) {
                    e.printStackTrace();
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                } finally {
                    if (db != null && db.isOpen()) db.close();
                }
            }).start();
        });
    }

    private void showPlaceholders() {
        etName.setText("");
        etCCCD.setText("");
        etPhone.setText("");
        etEmail.setText("");
        etWorkStatus.setText("");

        etHeight.setText("");
        etWeight.setText("");
        etDisease.setText("");
        etLastCheckup.setText("");
        etDrugTest.setText("");
        etConclusion.setText("");

        etType.setText("");
        etDateExpired.setText("");
        etAddress.setText("");
    }

    private void loadData(String idStr) {
        new Thread(() -> {
            SQLiteDatabase db = null;
            Cursor c = null;
            try {
                db = dbHelper.getReadableDatabase();

                // 1) NguoiDung + TaiKhoan
                c = db.rawQuery(
                        "SELECT ND.MaNguoiDung, ND.MaTaiKhoan, ND.HoTen, ND.CCCD, ND.SDT, ND.TrangThai, TK.Email " +
                                "FROM NguoiDung ND " +
                                "LEFT JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan " +
                                "WHERE ND.MaNguoiDung = ?",
                        new String[]{idStr}
                );
                String hoTen = null, cccd = null, sdt = null, trangThai = null, email = null;
                if (c != null && c.moveToFirst()) {
                    maNguoiDung = c.getInt(0);
                    try { maTaiKhoan = c.getInt(1); } catch (Exception ignored) {}
                    hoTen = safeGetColumn(c, "HoTen", 2);
                    cccd = safeGetColumn(c, "CCCD", 3);
                    sdt = safeGetColumn(c, "SDT", 4);
                    trangThai = safeGetColumn(c, "TrangThai", 5);
                    email = safeGetColumn(c, "Email", 6);
                }
                if (c != null) { c.close(); c = null; }

                // 2) SucKhoe latest
                String chieuCao = null, canNang = null, benhNen = null, maTuy = null, ngayKham = null, ketLuan = null;
                c = db.rawQuery("SELECT ChieuCao, CanNang, BenhNen, MaTuy, NgayKham, KetLuan " +
                                "FROM SucKhoe WHERE MaNguoiDung = ? ORDER BY NgayKham DESC LIMIT 1",
                        new String[]{idStr});
                if (c != null && c.moveToFirst()) {
                    chieuCao = safeGetColumn(c, "ChieuCao", 0);
                    canNang = safeGetColumn(c, "CanNang", 1);
                    benhNen = safeGetColumn(c, "BenhNen", 2);
                    maTuy = safeGetColumn(c, "MaTuy", 3);
                    ngayKham = safeGetColumn(c, "NgayKham", 4);
                    ketLuan = safeGetColumn(c, "KetLuan", 5);
                }
                if (c != null) { c.close(); c = null; }

                // 3) BangCap latest
                String loai = null, ngayHet = null, noiCap = null;
                c = db.rawQuery("SELECT Loai, NgayCap, NgayHetHan, NoiCap FROM BangCap WHERE MaNguoiDung = ? ORDER BY MaBangCap DESC LIMIT 1",
                        new String[]{idStr});
                if (c != null && c.moveToFirst()) {
                    loai = safeGetColumn(c, "Loai", 0);
                    //String ngayCap = safeGetColumn(c, "NgayCap", 1);
                    ngayHet = safeGetColumn(c, "NgayHetHan", 2);
                    noiCap = safeGetColumn(c, "NoiCap", 3);
                }
                if (c != null) { c.close(); c = null; }

                final String finalHoTen = hoTen, finalCccd = cccd, finalSdt = sdt, finalTrangThai = trangThai, finalEmail = email;
                final String finalChieuCao = chieuCao, finalCanNang = canNang, finalBenhNen = benhNen,
                        finalMaTuy = maTuy, finalNgayKham = ngayKham, finalKetLuan = ketLuan;
                final String finalLoai = loai, finalNgayHet = ngayHet, finalNoiCap = noiCap;

                requireActivity().runOnUiThread(() -> {
                    // populate fields (show empty string if null)
                    etName.setText(!TextUtils.isEmpty(finalHoTen) ? finalHoTen : "");
                    etCCCD.setText(!TextUtils.isEmpty(finalCccd) ? finalCccd : "");
                    etPhone.setText(!TextUtils.isEmpty(finalSdt) ? finalSdt : "");
                    etEmail.setText(!TextUtils.isEmpty(finalEmail) ? finalEmail : "");
                    etWorkStatus.setText(!TextUtils.isEmpty(finalTrangThai) ? finalTrangThai : "");

                    etHeight.setText(!TextUtils.isEmpty(finalChieuCao) ? finalChieuCao : "");
                    etWeight.setText(!TextUtils.isEmpty(finalCanNang) ? finalCanNang : "");
                    etDisease.setText(!TextUtils.isEmpty(finalBenhNen) ? finalBenhNen : "");
                    etLastCheckup.setText(!TextUtils.isEmpty(finalNgayKham) ? finalNgayKham : "");
                    // map MaTuy -> label
                    if (!TextUtils.isEmpty(finalMaTuy)) {
                        try {
                            int code = Integer.parseInt(finalMaTuy);
                            etDrugTest.setText(code == 0 ? "Âm tính" : (code == 1 ? "Dương tính" : finalMaTuy));
                        } catch (Exception e) { etDrugTest.setText(finalMaTuy); }
                    } else etDrugTest.setText("");
                    etConclusion.setText(!TextUtils.isEmpty(finalKetLuan) ? finalKetLuan : "");

                    etType.setText(!TextUtils.isEmpty(finalLoai) ? finalLoai : "");
                    etDateExpired.setText(!TextUtils.isEmpty(finalNgayHet) ? finalNgayHet : "");
                    etAddress.setText(!TextUtils.isEmpty(finalNoiCap) ? finalNoiCap : "");
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Lỗi khi load dữ liệu: " + ex.getMessage(), Toast.LENGTH_LONG).show()
                );
            } finally {
                if (c != null) c.close();
                if (db != null && db.isOpen()) db.close();
            }
        }).start();
    }

    private String safeGetColumn(Cursor c, String colName, int fallbackIdx) {
        try {
            int idx = c.getColumnIndex(colName);
            if (idx >= 0) {
                String s = c.getString(idx);
                return s != null ? s : "";
            } else {
                String s = c.getString(fallbackIdx);
                return s != null ? s : "";
            }
        } catch (Exception e) {
            try {
                String s = c.getString(fallbackIdx);
                return s != null ? s : "";
            } catch (Exception ex) {
                return "";
            }
        }
    }

    private String safeText(EditText et) {
        if (et == null) return "";
        CharSequence cs = et.getText();
        return cs == null ? "" : cs.toString().trim();
    }
}
