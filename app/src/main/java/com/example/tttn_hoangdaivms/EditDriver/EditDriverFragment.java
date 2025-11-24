package com.example.tttn_hoangdaivms.EditDriver;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.TextView;
import android.view.ViewGroup.LayoutParams;
import android.graphics.Color;
import android.app.AlertDialog;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tttn_hoangdaivms.Database.Database;
import com.example.tttn_hoangdaivms.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class EditDriverFragment extends Fragment {
    private static final String ARG_ID = "MaNguoiDungStr";

    private ImageView ivBack;
    private EditText etName, etCCCD, etPhone, etEmail, etWorkStatus;
    private EditText etBirthday, etGender;
    private EditText etHeight, etWeight, etDisease, etLastCheckup, etDrugTest, etConclusion;
    private EditText etType, etDateIssued, etDateExpired, etAddress;
    private View btnSave;

    private Database dbHelper;

    private String idNguoiDungStr = null;
    private int maNguoiDung = -1;
    private int maTaiKhoan = -1;

    // store original password loaded from TK table to avoid overwriting with empty
    private String originalMatKhau = null;

    // Validation patterns
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L} ]{2,}$"); // letters + spaces
    private static final Pattern PHONE_PATTERN_10_11 = Pattern.compile("^\\d{10,11}$");
    private static final Pattern CCCD_PATTERN = Pattern.compile("^\\d{12}$");
    private static final Pattern EMAIL_DOMAIN_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

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

        etBirthday = view.findViewById(R.id.etBirthday);
        etGender = view.findViewById(R.id.etGender);

        etHeight = view.findViewById(R.id.etHeight);
        etWeight = view.findViewById(R.id.etWeight);
        etDisease = view.findViewById(R.id.etDisease);
        etLastCheckup = view.findViewById(R.id.etLastCheckup);
        etDrugTest = view.findViewById(R.id.etDrugTest);
        etConclusion = view.findViewById(R.id.etConclusion);

        etType = view.findViewById(R.id.etType);
        etDateIssued = view.findViewById(R.id.etDateIssued);
        etDateExpired = view.findViewById(R.id.etDateExpired);
        etAddress = view.findViewById(R.id.etAddress);

        btnSave = view.findViewById(R.id.btnSave);

        dbHelper = new Database(requireContext());

        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_ID)) {
            idNguoiDungStr = args.getString(ARG_ID);
        }

        // email & work status are read-only in UI
        if (etEmail != null) {
            etEmail.setEnabled(false);
            etEmail.setFocusable(false);
            etEmail.setClickable(false);
        }
//        if (etWorkStatus != null) {
//            etWorkStatus.setEnabled(false);
//            etWorkStatus.setFocusable(false);
//            etWorkStatus.setClickable(false);
//        }

        // height & weight only numbers
        if (etHeight != null) etHeight.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (etWeight != null) etWeight.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        // date fields: open DatePicker on click/focus
        setDatePicker(etBirthday, true, false);    // birthday must be past
        setDatePicker(etLastCheckup, true, false); // last checkup <= today
        setDatePicker(etDateIssued, true, false);  // date issued < today
        setDatePicker(etDateExpired, false, true); // date expired >= today allowed

        // clear inline error label when user types
        TextWatcher clearWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                View focused = getActivity() != null ? getActivity().getCurrentFocus() : null;
                if (focused instanceof EditText) removeFieldErrorAbove((EditText) focused);
            }
        };
        if (etName != null) etName.addTextChangedListener(clearWatcher);
        if (etCCCD != null) etCCCD.addTextChangedListener(clearWatcher);
        if (etPhone != null) etPhone.addTextChangedListener(clearWatcher);
        if (etBirthday != null) etBirthday.addTextChangedListener(clearWatcher);
        if (etGender != null) etGender.addTextChangedListener(clearWatcher);
        if (etDateIssued != null) etDateIssued.addTextChangedListener(clearWatcher);
        if (etDateExpired != null) etDateExpired.addTextChangedListener(clearWatcher);
        if (etLastCheckup != null) etLastCheckup.addTextChangedListener(clearWatcher);

        // load data
        if (!TextUtils.isEmpty(idNguoiDungStr)) loadData(idNguoiDungStr);
        else showPlaceholders();

        ivBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) getParentFragmentManager().popBackStack();
            else requireActivity().finish();
        });

        // Confirmation then validate then save
        btnSave.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Xác nhận")
                    .setMessage("Bạn có chắc chắn muốn lưu không?")
                    .setNegativeButton("Không", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("Có", (dialog, which) -> {
                        boolean allOk = validateAllInputsAndShowErrors();
                        if (allOk) {
                            doSave();
                        } else {
                            Toast.makeText(requireContext(), "Vui lòng sửa các trường có lỗi", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
        });
    }

    // helper to attach DatePicker to EditText
    private void setDatePicker(EditText et, boolean allowPastOnly, boolean allowFutureOrToday) {
        if (et == null) return;
        et.setFocusable(false);
        et.setClickable(true);
        et.setOnClickListener(v -> showDatePickerFor(et));
        et.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) showDatePickerFor(et); });
    }

    private void showDatePickerFor(EditText et) {
        final Calendar calendar = Calendar.getInstance();
        Date cur = parseDateStrict(safeText(et));
        if (cur != null) calendar.setTime(cur);

        int y = calendar.get(Calendar.YEAR);
        int m = calendar.get(Calendar.MONTH);
        int d = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(requireContext(),
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    Calendar sel = Calendar.getInstance();
                    sel.set(year, month, dayOfMonth, 0, 0, 0);
                    et.setText(DISPLAY_DATE_FORMAT.format(sel.getTime()));
                }, y, m, d);
        dpd.show();
    }

    /**
     * Lưu dữ liệu:
     * - Không ghi MatKhau vào bảng NguoiDung.
     * - Nếu maTaiKhoan tồn tại -> update TaiKhoan.
     * - Nếu maTaiKhoan chưa có và user nhập mật khẩu/email -> insert TaiKhoan trước, lấy id -> gán vào NguoiDung.
     * Tất cả thực hiện trong transaction.
     */
    private void doSave() {
        final String name = safeText(etName);
        final String cccd = safeText(etCCCD);
        final String phone = safeText(etPhone);
        final String birthday = safeText(etBirthday);
        final String gender = safeText(etGender);
        // matkhau và email thuộc TaiKhoan
        final String matkhau = safeText(etWorkStatus); // thường read-only
        final String email = safeText(etEmail);
        final String height = safeText(etHeight);
        final String weight = safeText(etWeight);
        final String disease = safeText(etDisease);
        final String lastCheckup = safeText(etLastCheckup);
        final String drugTest = safeText(etDrugTest);
        final String conclusion = safeText(etConclusion);
        final String licenseType = safeText(etType);
        final String dateIssued = safeText(etDateIssued);
        final String dateExpired = safeText(etDateExpired);
        final String address = safeText(etAddress);

        new Thread(() -> {
            SQLiteDatabase db = null;
            try {
                db = dbHelper.getWritableDatabase();
                db.beginTransaction();
                try {
                    // ensure maNguoiDung if id provided
                    if (maNguoiDung == -1 && !TextUtils.isEmpty(idNguoiDungStr)) {
                        try (Cursor c = db.rawQuery("SELECT MaNguoiDung, MaTaiKhoan FROM NguoiDung WHERE MaNguoiDung = ?",
                                new String[]{idNguoiDungStr})) {
                            if (c != null && c.moveToFirst()) {
                                maNguoiDung = c.getInt(0);
                                try { maTaiKhoan = c.getInt(1); } catch (Exception ignored) {}
                            }
                        }
                    }

                    // --- Handle TaiKhoan (create/update) ---
                    // If maTaiKhoan exists, update it with matkhau/email if provided
                    if (maTaiKhoan != -1) {
                        ContentValues cvTK = new ContentValues();
                        boolean tkNeedsUpdate = false;
                        if (!TextUtils.isEmpty(matkhau)) { cvTK.put("MatKhau", matkhau); tkNeedsUpdate = true; }
                        if (!TextUtils.isEmpty(email)) { cvTK.put("Email", email); tkNeedsUpdate = true; }
                        if (tkNeedsUpdate) {
                            int rows = db.update("TaiKhoan", cvTK, "MaTaiKhoan = ?", new String[]{String.valueOf(maTaiKhoan)});
                            if (rows <= 0) Log.w("EditDriver", "Update TaiKhoan returned 0 rows for id=" + maTaiKhoan);
                        }
                    } else {
                        // no maTaiKhoan yet: if user provided password/email (or we have originalMatKhau), create TaiKhoan
                        String pwToUse = null;
                        if (!TextUtils.isEmpty(matkhau)) pwToUse = matkhau;
                        else if (!TextUtils.isEmpty(originalMatKhau)) pwToUse = originalMatKhau;

                        if (!TextUtils.isEmpty(pwToUse) || !TextUtils.isEmpty(email)) {
                            ContentValues cvTK = new ContentValues();
                            if (!TextUtils.isEmpty(pwToUse)) cvTK.put("MatKhau", pwToUse);
                            if (!TextUtils.isEmpty(email)) cvTK.put("Email", email);
                            long tkId = db.insert("TaiKhoan", null, cvTK);
                            if (tkId == -1) {
                                throw new RuntimeException("Insert TaiKhoan failed (insert returned -1). Kiểm tra ràng buộc trên bảng TaiKhoan.");
                            }
                            maTaiKhoan = (int) tkId;
                        }
                    }

                    // --- Insert or update NguoiDung (without MatKhau) ---
                    if (maNguoiDung != -1) {
                        ContentValues cv = new ContentValues();
                        cv.put("HoTen", name);
                        cv.put("CCCD", cccd);
                        cv.put("SDT", phone);
                        if (!TextUtils.isEmpty(birthday)) cv.put("NgaySinh", birthday);
                        if (!TextUtils.isEmpty(gender)) cv.put("GioiTinh", gender);
                        // ensure MaTaiKhoan set if available
                        if (maTaiKhoan != -1) cv.put("MaTaiKhoan", maTaiKhoan);

                        int rows = db.update("NguoiDung", cv, "MaNguoiDung = ?", new String[]{String.valueOf(maNguoiDung)});
                        if (rows <= 0) {
                            Log.w("EditDriver", "Update NguoiDung returned 0 rows for id=" + maNguoiDung);
                        }
                    } else {
                        ContentValues cv = new ContentValues();
                        cv.put("HoTen", name);
                        cv.put("CCCD", cccd);
                        cv.put("SDT", phone);
                        if (!TextUtils.isEmpty(birthday)) cv.put("NgaySinh", birthday);
                        if (!TextUtils.isEmpty(gender)) cv.put("GioiTinh", gender);
                        if (maTaiKhoan != -1) cv.put("MaTaiKhoan", maTaiKhoan);

                        long newId = db.insert("NguoiDung", null, cv);
                        if (newId == -1) {
                            throw new RuntimeException("Insert NguoiDung failed (insert returned -1). Kiểm tra ràng buộc trên bảng NguoiDung.");
                        }
                        maNguoiDung = (int) newId;
                    }

                    // --- SucKhoe (unchanged logic) ---
                    if (maNguoiDung != -1) {
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
                        if (!TextUtils.isEmpty(drugTest)) {
                            String dt = drugTest.trim().toLowerCase();
                            if (dt.contains("âm") || dt.contains("am")) cvSK.put("MaTuy", 0);
                            else if (dt.contains("dương") || dt.contains("duong")) cvSK.put("MaTuy", 1);
                        }
                        cvSK.put("MaNguoiDung", maNguoiDung);

                        if (hasSucKhoe && sucKhoeId != -1) {
                            int rows = db.update("SucKhoe", cvSK, "MaSucKhoe = ?", new String[]{String.valueOf(sucKhoeId)});
                            if (rows <= 0) Log.w("EditDriver", "Update SucKhoe returned 0 rows id=" + sucKhoeId);
                        } else {
                            long id = db.insert("SucKhoe", null, cvSK);
                            if (id == -1) Log.w("EditDriver", "Insert SucKhoe returned -1");
                        }
                    }

                    // --- BangCap (unchanged logic) ---
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
                        if (!TextUtils.isEmpty(dateIssued)) cvBC.put("NgayCap", dateIssued);
                        if (!TextUtils.isEmpty(dateExpired)) cvBC.put("NgayHetHan", dateExpired);
                        if (!TextUtils.isEmpty(address)) cvBC.put("NoiCap", address);
                        cvBC.put("MaNguoiDung", maNguoiDung);

                        if (hasBangCap && bangCapId != -1) {
                            int rows = db.update("BangCap", cvBC, "MaBangCap = ?", new String[]{String.valueOf(bangCapId)});
                            if (rows <= 0) Log.w("EditDriver", "Update BangCap returned 0 rows id=" + bangCapId);
                        } else {
                            long id = db.insert("BangCap", null, cvBC);
                            if (id == -1) Log.w("EditDriver", "Insert BangCap returned -1");
                        }
                    }

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Lưu thành công.", Toast.LENGTH_SHORT).show()
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
    }

    private void showPlaceholders() {
        if (etName != null) etName.setText("");
        if (etCCCD != null) etCCCD.setText("");
        if (etPhone != null) etPhone.setText("");
        if (etEmail != null) etEmail.setText("");
        if (etWorkStatus != null) etWorkStatus.setText("");

        if (etBirthday != null) etBirthday.setText("");
        if (etGender != null) etGender.setText("");

        if (etHeight != null) etHeight.setText("");
        if (etWeight != null) etWeight.setText("");
        if (etDisease != null) etDisease.setText("");
        if (etLastCheckup != null) etLastCheckup.setText("");
        if (etDrugTest != null) etDrugTest.setText("");
        if (etConclusion != null) etConclusion.setText("");

        if (etType != null) etType.setText("");
        if (etDateIssued != null) etDateIssued.setText("");
        if (etDateExpired != null) etDateExpired.setText("");
        if (etAddress != null) etAddress.setText("");
    }

    private void loadData(String idStr) {
        new Thread(() -> {
            SQLiteDatabase db = null;
            Cursor c = null;
            try {
                db = dbHelper.getReadableDatabase();

                c = db.rawQuery(
                        "SELECT ND.MaNguoiDung, ND.MaTaiKhoan, ND.HoTen, ND.CCCD, ND.SDT, TK.MatKhau, TK.Email, ND.NgaySinh, ND.GioiTinh " +
                                "FROM NguoiDung ND " +
                                "LEFT JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan " +
                                "WHERE ND.MaNguoiDung = ?",
                        new String[]{idStr}
                );
                String hoTen = null, cccd = null, sdt = null, matkhau = null, email = null, ngaySinh = null, gioiTinh = null;
                if (c != null && c.moveToFirst()) {
                    maNguoiDung = c.getInt(0);
                    try { maTaiKhoan = c.getInt(1); } catch (Exception ignored) {}
                    hoTen = safeGetColumn(c, "HoTen", 2);
                    cccd = safeGetColumn(c, "CCCD", 3);
                    sdt = safeGetColumn(c, "SDT", 4);
                    matkhau = safeGetColumn(c, "MatKhau", 5); // from TK
                    email = safeGetColumn(c, "Email", 6);     // from TK
                    ngaySinh = safeGetColumn(c, "NgaySinh", 7);
                    gioiTinh = safeGetColumn(c, "GioiTinh", 8);
                }
                if (c != null) { c.close(); c = null; }

                // SucKhoe
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

                // BangCap
                String loai = null, ngayCap = null, ngayHet = null, noiCap = null;
                c = db.rawQuery("SELECT Loai, NgayCap, NgayHetHan, NoiCap FROM BangCap WHERE MaNguoiDung = ? ORDER BY MaBangCap DESC LIMIT 1",
                        new String[]{idStr});
                if (c != null && c.moveToFirst()) {
                    loai = safeGetColumn(c, "Loai", 0);
                    ngayCap = safeGetColumn(c, "NgayCap", 1);
                    ngayHet = safeGetColumn(c, "NgayHetHan", 2);
                    noiCap = safeGetColumn(c, "NoiCap", 3);
                }
                if (c != null) { c.close(); c = null; }

                // save original password/email to avoid overwriting with empty later
                this.originalMatKhau = matkhau;

                final String finalHoTen = hoTen, finalCccd = cccd, finalSdt = sdt, finalMatKhau = matkhau, finalEmail = email;
                final String finalNgaySinh = ngaySinh, finalGioiTinh = gioiTinh;
                final String finalChieuCao = chieuCao, finalCanNang = canNang, finalBenhNen = benhNen,
                        finalMaTuy = maTuy, finalNgayKham = ngayKham, finalKetLuan = ketLuan;
                final String finalLoai = loai, finalNgayCap = ngayCap, finalNgayHet = ngayHet, finalNoiCap = noiCap;

                requireActivity().runOnUiThread(() -> {
                    if (etName != null) etName.setText(!TextUtils.isEmpty(finalHoTen) ? finalHoTen : "");
                    if (etCCCD != null) etCCCD.setText(!TextUtils.isEmpty(finalCccd) ? finalCccd : "");
                    if (etPhone != null) etPhone.setText(!TextUtils.isEmpty(finalSdt) ? finalSdt : "");
                    if (etEmail != null) etEmail.setText(!TextUtils.isEmpty(finalEmail) ? finalEmail : "");
                    // show password from TaiKhoan (read-only)
                    if (etWorkStatus != null) etWorkStatus.setText(!TextUtils.isEmpty(finalMatKhau) ? finalMatKhau : "");

                    if (etBirthday != null) etBirthday.setText(!TextUtils.isEmpty(finalNgaySinh) ? finalNgaySinh : "");
                    if (etGender != null) etGender.setText(!TextUtils.isEmpty(finalGioiTinh) ? finalGioiTinh : "");

                    if (etHeight != null) etHeight.setText(!TextUtils.isEmpty(finalChieuCao) ? finalChieuCao : "");
                    if (etWeight != null) etWeight.setText(!TextUtils.isEmpty(finalCanNang) ? finalCanNang : "");
                    if (etDisease != null) etDisease.setText(!TextUtils.isEmpty(finalBenhNen) ? finalBenhNen : "");
                    if (etLastCheckup != null) etLastCheckup.setText(!TextUtils.isEmpty(finalNgayKham) ? finalNgayKham : "");
                    if (etDrugTest != null) {
                        if (!TextUtils.isEmpty(finalMaTuy)) {
                            try {
                                int code = Integer.parseInt(finalMaTuy);
                                etDrugTest.setText(code == 0 ? "Âm tính" : (code == 1 ? "Dương tính" : finalMaTuy));
                            } catch (Exception e) { etDrugTest.setText(finalMaTuy); }
                        } else etDrugTest.setText("");
                    }
                    if (etConclusion != null) etConclusion.setText(!TextUtils.isEmpty(finalKetLuan) ? finalKetLuan : "");

                    if (etType != null) etType.setText(!TextUtils.isEmpty(finalLoai) ? finalLoai : "");
                    if (etDateIssued != null) etDateIssued.setText(!TextUtils.isEmpty(finalNgayCap) ? finalNgayCap : "");
                    if (etDateExpired != null) etDateExpired.setText(!TextUtils.isEmpty(finalNgayHet) ? finalNgayHet : "");
                    if (etAddress != null) etAddress.setText(!TextUtils.isEmpty(finalNoiCap) ? finalNoiCap : "");
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

    // show error text above EditText (not change background)
    private void setFieldErrorAbove(final EditText editText, final String message) {
        if (editText == null || editText.getParent() == null) return;
        requireActivity().runOnUiThread(() -> {
            ViewGroup parent = (ViewGroup) editText.getParent();
            String tag = "error_above_" + editText.getId();
            View existing = parent.findViewWithTag(tag);
            if (existing instanceof TextView) {
                ((TextView) existing).setText(message);
                existing.setVisibility(View.VISIBLE);
                return;
            }
            TextView tv = new TextView(requireContext());
            tv.setTag(tag);
            tv.setText(message);
            tv.setTextColor(Color.parseColor("#D32F2F"));
            tv.setTextSize(12);
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            int idx = parent.indexOfChild(editText);
            if (idx >= 0) parent.addView(tv, idx, lp);
            else parent.addView(tv, lp);
        });
    }

    private void removeFieldErrorAbove(final EditText editText) {
        if (editText == null || editText.getParent() == null) return;
        requireActivity().runOnUiThread(() -> {
            ViewGroup parent = (ViewGroup) editText.getParent();
            String tag = "error_above_" + editText.getId();
            View existing = parent.findViewWithTag(tag);
            if (existing != null) parent.removeView(existing);
        });
    }

    /**
     * Validate all fields and show error messages above each invalid field.
     * Returns true only if everything is valid.
     */
    private boolean validateAllInputsAndShowErrors() {
        // clear previous inline errors
        if (etName != null) removeFieldErrorAbove(etName);
        if (etCCCD != null) removeFieldErrorAbove(etCCCD);
        if (etPhone != null) removeFieldErrorAbove(etPhone);
        if (etBirthday != null) removeFieldErrorAbove(etBirthday);
        if (etGender != null) removeFieldErrorAbove(etGender);
        if (etDateIssued != null) removeFieldErrorAbove(etDateIssued);
        if (etDateExpired != null) removeFieldErrorAbove(etDateExpired);
        if (etLastCheckup != null) removeFieldErrorAbove(etLastCheckup);
        if (etEmail != null) removeFieldErrorAbove(etEmail);

        boolean ok = true;

        // NAME
        String name = safeText(etName);
        if (TextUtils.isEmpty(name)) {
            setFieldErrorAbove(etName, "Vui lòng điền họ tên");
            ok = false;
        } else if (!NAME_PATTERN.matcher(name).matches()) {
            setFieldErrorAbove(etName, "Họ tên không hợp lệ");
            ok = false;
        }

        // CCCD
        String cccd = safeText(etCCCD);
        if (TextUtils.isEmpty(cccd)) {
            setFieldErrorAbove(etCCCD, "Vui lòng nhập CCCD");
            ok = false;
        } else if (!CCCD_PATTERN.matcher(cccd).matches()) {
            setFieldErrorAbove(etCCCD, "CCCD phải gồm 12 chữ số");
            ok = false;
        }

        // Gender
        if (etGender != null) {
            String gender = safeText(etGender);
            if (TextUtils.isEmpty(gender)) {
                setFieldErrorAbove(etGender, "Vui lòng chọn giới tính (Nam/Nữ)");
                ok = false;
            } else {
                String low = gender.toLowerCase(Locale.getDefault());
                if (!(low.equals("nam") || low.equals("nữ") || low.equals("nu"))) {
                    setFieldErrorAbove(etGender, "Vui lòng chọn giới tính (Nam/Nữ)");
                    ok = false;
                }
            }
        }

        // Phone
        String phone = safeText(etPhone);
        if (TextUtils.isEmpty(phone)) {
            setFieldErrorAbove(etPhone, "Vui lòng nhập số điện thoại");
            ok = false;
        } else if (!PHONE_PATTERN_10_11.matcher(phone).matches()) {
            setFieldErrorAbove(etPhone, "Số điện thoại không hợp lệ(10 chữ số)");
            ok = false;
        }

        // Email (validate format but not editable)
        String email = safeText(etEmail);
        if (!TextUtils.isEmpty(email) && !EMAIL_DOMAIN_PATTERN.matcher(email).matches()) {
            setFieldErrorAbove(etEmail, "Email không hợp lệ");
            ok = false;
        }

        // Birthday must be past if provided
        if (etBirthday != null) {
            String s = safeText(etBirthday);
            if (!TextUtils.isEmpty(s)) {
                Date d = parseDateStrict(s);
                if (d == null) { setFieldErrorAbove(etBirthday, "Ngày sinh không hợp lệ"); ok = false; }
                else if (!isBeforeToday(d)) { setFieldErrorAbove(etBirthday, "Ngày sinh phải là ngày trong quá khứ"); ok = false; }
            }
        }

        // LastCheckup <= today
        if (etLastCheckup != null) {
            String s = safeText(etLastCheckup);
            if (!TextUtils.isEmpty(s)) {
                Date d = parseDateStrict(s);
                if (d == null) { setFieldErrorAbove(etLastCheckup, "Ngày khám không hợp lệ"); ok = false; }
                else if (isAfterToday(d)) { setFieldErrorAbove(etLastCheckup, "Ngày khám không hợp lệ"); ok = false; }
            }
        }

        // DateIssued < today
        if (etDateIssued != null) {
            String s = safeText(etDateIssued);
            if (!TextUtils.isEmpty(s)) {
                Date d = parseDateStrict(s);
                if (d == null) { setFieldErrorAbove(etDateIssued, "Ngày cấp không hợp lệ"); ok = false; }
                else if (!isBeforeToday(d)) { setFieldErrorAbove(etDateIssued, "Ngày cấp không hợp lệ"); ok = false; }
            }
        }

        // DateExpired >= today
        if (etDateExpired != null) {
            String s = safeText(etDateExpired);
            if (!TextUtils.isEmpty(s)) {
                Date d = parseDateStrict(s);
                if (d == null) { setFieldErrorAbove(etDateExpired, "Ngày hết hạn không hợp lệ"); ok = false; }
                else if (isBeforeToday(d)) { setFieldErrorAbove(etDateExpired, "Bằng đã hết hạn, không hợp lệ"); ok = false; }
            }
        }

        return ok;
    }

    private Date parseDateStrict(String s) {
        if (TextUtils.isEmpty(s)) return null;
        try {
            return DISPLAY_DATE_FORMAT.parse(s);
        } catch (ParseException e) {
            return null;
        }
    }

    private boolean isBeforeToday(Date d) {
        Date today = removeTime(new Date());
        return d.before(today);
    }

    private boolean isAfterToday(Date d) {
        Date today = removeTime(new Date());
        return d.after(today);
    }

    private Date removeTime(Date d) {
        Calendar c = Calendar.getInstance(Locale.getDefault());
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
