package com.example.tttn_hoangdaivms.Profile;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.graphics.Color;

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

public class EditProfileFragment extends Fragment {

    private static final String TAG = "EditProfileFrag";
    private static final String PREFS_NAME = "app_prefs";
    private static final String PREF_KEY_EMAIL = "logged_email";

    private EditText edtName, edtCCCD, edtPhone, edtEmail, edtStatus;
    private EditText edtType, edtDateIssued, edtExpired, edtAdress; // Bằng cấp (thêm edtDateIssued)
    private EditText edtHeight, edtWeight, edtDisease, edtLastCheck, edtResult, edtConclusion;
    private EditText edtBirthday, edtGender; // thêm
    private ImageView btnBack;
    private Button btnSave;

    private View degreeSection; // container for degree fields

    private Database dbHelper;
    private int userId = -1;
    private int maTaiKhoan = -1; // thêm để lưu id tài khoản
    private String userEmail;
    private String userRole = ""; // VaiTro lấy từ DB

    // Date format shown to user
    private final SimpleDateFormat uiDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    // possible parse formats from DB
    private final String[] parseFormats = new String[]{"yyyy-MM-dd", "dd/MM/yyyy", "yyyy/MM/dd", "dd-MM-yyyy"};

    // Validation patterns
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L} ]{2,}$"); // letters + spaces, min 2
    private static final Pattern PHONE_PATTERN_10_11 = Pattern.compile("^\\d{10,11}$");
    private static final Pattern CCCD_PATTERN = Pattern.compile("^\\d{12}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    // password rule: min 8 chars, at least one digit, one lower, one upper, one special from @#$%&
    private static final Pattern PASSWORD_STRONG_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\\$%&]).{8,}$");

    public EditProfileFragment() { /* required */ }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        // Ánh xạ view
        btnBack = view.findViewById(R.id.ivBack);
        btnSave = view.findViewById(R.id.btnSave);

        edtName = view.findViewById(R.id.etName);
        edtCCCD = view.findViewById(R.id.etCCCD);
        edtPhone = view.findViewById(R.id.etPhone);
        edtEmail = view.findViewById(R.id.etEmail);
        edtStatus = view.findViewById(R.id.etWorkStatus);

        // Bằng cấp
        edtType = view.findViewById(R.id.etType); // Loại bằng cấp
        edtDateIssued = view.findViewById(R.id.etDateIssued); // Ngày cấp (thêm)
        edtExpired = view.findViewById(R.id.etDateExpired); // Ngày hết hạn
        edtAdress = view.findViewById(R.id.etAddress); // Nơi cấp

        // map container (degree section) — use id from your XML
        degreeSection = view.findViewById(R.id.degreeSection);

        // Sức khỏe
        edtHeight = view.findViewById(R.id.etHeight);
        edtWeight = view.findViewById(R.id.etWeight);
        edtDisease = view.findViewById(R.id.etDisease);
        edtLastCheck = view.findViewById(R.id.etLastCheckup);
        edtResult = view.findViewById(R.id.etDrugTest);
        edtConclusion = view.findViewById(R.id.etConclusion);

        // Thêm birthday + gender
        edtBirthday = view.findViewById(R.id.etBirthday);
        edtGender = view.findViewById(R.id.etGender);

        dbHelper = new Database(requireContext());

        // Làm cho các EditText ngày không hiện bàn phím mà mở DatePicker khi bấm
        setupDatePickers();

        // chi/nhập số cho height/weight
        if (edtHeight != null) edtHeight.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (edtWeight != null) edtWeight.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        // Lấy email từ bundle (ưu tiên) hoặc SharedPreferences
        Bundle args = getArguments();
        if (args != null && args.getString("email") != null) {
            userEmail = args.getString("email");
        } else {
            SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            userEmail = prefs.getString(PREF_KEY_EMAIL, null);
        }

        if (userEmail == null) {
            Toast.makeText(requireContext(), "Không tìm thấy email người dùng. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            return view;
        }

        // load
        loadUserAndHealth(userEmail);

        // disable edit on email (ui)
        if (edtEmail != null) {
            edtEmail.setEnabled(false);
            edtEmail.setFocusable(false);
            edtEmail.setClickable(false);
        }

        // clear inline error label when user types
        TextWatcher clearWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                View focused = getActivity() != null ? getActivity().getCurrentFocus() : null;
                if (focused instanceof EditText) removeFieldErrorAbove((EditText) focused);
            }
        };
        if (edtName != null) edtName.addTextChangedListener(clearWatcher);
        if (edtCCCD != null) edtCCCD.addTextChangedListener(clearWatcher);
        if (edtPhone != null) edtPhone.addTextChangedListener(clearWatcher);
        if (edtLastCheck != null) edtLastCheck.addTextChangedListener(clearWatcher);
        if (edtExpired != null) edtExpired.addTextChangedListener(clearWatcher);
        if (edtBirthday != null) edtBirthday.addTextChangedListener(clearWatcher);
        if (edtGender != null) edtGender.addTextChangedListener(clearWatcher);
        if (edtDateIssued != null) edtDateIssued.addTextChangedListener(clearWatcher);
        if (edtAdress != null) edtAdress.addTextChangedListener(clearWatcher);

        // Nút back
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Nút save: show confirmation dialog, then validate all and save
        btnSave.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Xác nhận")
                    .setMessage("Bạn có chắc chắn muốn lưu không?")
                    .setNegativeButton("Không", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("Có", (dialog, which) -> {
                        boolean ok = validateAllInputsAndShowErrors();
                        if (ok) {
                            saveData(); // save with TaiKhoan logic
                        } else {
                            Toast.makeText(requireContext(), "Vui lòng sửa các trường có lỗi", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
        });

        return view;
    }

    // Thiết lập hành vi cho các EditText ngày: disable keyboard, mở DatePicker khi bấm/focus
    private void setupDatePickers() {
        if (edtBirthday != null) {
            edtBirthday.setInputType(InputType.TYPE_NULL);
            edtBirthday.setFocusable(false);
            edtBirthday.setOnClickListener(v -> showDatePickerForEditText(edtBirthday));
            edtBirthday.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) showDatePickerForEditText(edtBirthday); });
        }
        if (edtLastCheck != null) {
            edtLastCheck.setInputType(InputType.TYPE_NULL);
            edtLastCheck.setFocusable(false);
            edtLastCheck.setOnClickListener(v -> showDatePickerForEditText(edtLastCheck));
            edtLastCheck.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) showDatePickerForEditText(edtLastCheck); });
        }
        if (edtDateIssued != null) {
            edtDateIssued.setInputType(InputType.TYPE_NULL);
            edtDateIssued.setFocusable(false);
            edtDateIssued.setOnClickListener(v -> showDatePickerForEditText(edtDateIssued));
            edtDateIssued.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) showDatePickerForEditText(edtDateIssued); });
        }
        if (edtExpired != null) {
            edtExpired.setInputType(InputType.TYPE_NULL);
            edtExpired.setFocusable(false);
            edtExpired.setOnClickListener(v -> showDatePickerForEditText(edtExpired));
            edtExpired.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) showDatePickerForEditText(edtExpired); });
        }
    }

    private void showDatePickerForEditText(EditText target) {
        if (target == null) return;
        final Calendar cal = Calendar.getInstance();
        String cur = target.getText() == null ? "" : target.getText().toString().trim();
        Date parsed = tryParseDate(cur);
        if (parsed != null) cal.setTime(parsed);

        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(requireContext(), (DatePicker view, int year, int month, int dayOfMonth) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(year, month, dayOfMonth);
            String formatted = uiDateFormat.format(sel.getTime());
            target.setText(formatted);
        }, y, m, d);

        dpd.show();
    }

    private Date tryParseDate(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        s = s.trim();
        for (String fmt : parseFormats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.getDefault());
                sdf.setLenient(false);
                return sdf.parse(s);
            } catch (ParseException ignored) { }
        }
        try {
            return uiDateFormat.parse(s);
        } catch (ParseException ignored) { }
        return null;
    }

    private void loadUserAndHealth(String email) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();

            cursor = db.rawQuery(
                    "SELECT ND.MaNguoiDung, ND.MaTaiKhoan, ND.HoTen, ND.CCCD, ND.SDT, TK.MatKhau, COALESCE(ND.VaiTro, ''), ND.NgaySinh, ND.GioiTinh, TK.Email " +
                            "FROM NguoiDung ND " +
                            "LEFT JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan " +
                            "WHERE TK.Email = ?",
                    new String[]{email}
            );

            if (cursor != null && cursor.moveToFirst()) {
                userId = cursor.getInt(0);
                try { maTaiKhoan = cursor.getInt(1); } catch (Exception ignored) { maTaiKhoan = -1; }
                String hoTen = safeGet(cursor, 2);
                String cccd = safeGet(cursor, 3);
                String sdt = safeGet(cursor, 4);
                String matkhau = safeGet(cursor, 5);
                String vaiTro = safeGet(cursor, 6);
                String ngaySinh = safeGet(cursor, 7);
                String gioiTinh = safeGet(cursor, 8);
                String emailFromDb = safeGet(cursor, 9);

                userRole = vaiTro != null ? vaiTro.trim() : "";

                if (edtName != null) edtName.setText(notEmpty(hoTen, ""));
                if (edtCCCD != null) edtCCCD.setText(notEmpty(cccd, ""));
                if (edtPhone != null) edtPhone.setText(notEmpty(sdt, ""));
                if (edtEmail != null) edtEmail.setText(notEmpty(emailFromDb, email));
                if (edtStatus != null) edtStatus.setText(notEmpty(matkhau, ""));
                if (edtBirthday != null) {
                    Date d = tryParseDate(notEmpty(ngaySinh, ""));
                    if (d != null) edtBirthday.setText(uiDateFormat.format(d));
                    else edtBirthday.setText(notEmpty(ngaySinh, ""));
                }
                if (edtGender != null) edtGender.setText(notEmpty(gioiTinh, ""));
            } else {
                Toast.makeText(requireContext(), "Không tìm thấy hồ sơ người dùng.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (cursor != null) { cursor.close(); cursor = null; }

            cursor = db.rawQuery(
                    "SELECT MaSucKhoe, ChieuCao, CanNang, BenhNen, NgayKham, MaTuy, KetLuan " +
                            "FROM SucKhoe WHERE MaNguoiDung = ? ORDER BY NgayKham DESC",
                    new String[]{String.valueOf(userId)}
            );

            if (cursor != null && cursor.moveToFirst()) {
                String chieuCao = safeGet(cursor, 1);
                String canNang = safeGet(cursor, 2);
                String benhNen = safeGet(cursor, 3);
                String ngayKham = safeGet(cursor, 4);
                String maTuy = safeGet(cursor, 5);
                String ketLuan = safeGet(cursor, 6);

                if (edtHeight != null) { edtHeight.setText(notEmpty(chieuCao, "")); edtHeight.setHint(""); }
                if (edtWeight != null) { edtWeight.setText(notEmpty(canNang, "")); edtWeight.setHint(""); }
                if (edtDisease != null) { edtDisease.setText(notEmpty(benhNen, "")); edtDisease.setHint(""); }
                if (edtLastCheck != null) {
                    Date d = tryParseDate(notEmpty(ngayKham, ""));
                    if (d != null) edtLastCheck.setText(uiDateFormat.format(d));
                    else edtLastCheck.setText(notEmpty(ngayKham, ""));
                    edtLastCheck.setHint("");
                }
                if (edtResult != null) { edtResult.setText(notEmpty(maTuy, "")); edtResult.setHint(""); }
                if (edtConclusion != null) { edtConclusion.setText(notEmpty(ketLuan, "")); edtConclusion.setHint(""); }
            } else {
                if (edtHeight != null) { edtHeight.setText(""); edtHeight.setHint("Chưa có chiều cao"); }
                if (edtWeight != null) { edtWeight.setText(""); edtWeight.setHint("Chưa có cân nặng"); }
                if (edtDisease != null) { edtDisease.setText(""); edtDisease.setHint("Chưa có ghi chú bệnh nền"); }
                if (edtLastCheck != null) { edtLastCheck.setText(""); edtLastCheck.setHint("Chưa có ngày khám (chạm để chọn)"); }
                if (edtResult != null) { edtResult.setText(""); edtResult.setHint("Chưa có kết quả kiểm tra ma túy"); }
                if (edtConclusion != null) { edtConclusion.setText(""); edtConclusion.setHint("Chưa có kết luận"); }
            }
            if (cursor != null) { cursor.close(); cursor = null; }

            // --- BẰNG CẤP: chỉ hiển thị với user KHÔNG phải Admin ---
            boolean showDegree = !isAdminRole(userRole);
            Log.d(TAG, "userRole='" + userRole + "' showDegree=" + showDegree);

            showDegreeFields(showDegree);

            if (showDegree) {
                cursor = db.rawQuery(
                        "SELECT MaBangCap, Loai, NgayCap, NgayHetHan, NoiCap " +
                                "FROM BangCap WHERE MaNguoiDung = ? LIMIT 1",
                        new String[]{String.valueOf(userId)}
                );

                if (cursor != null && cursor.moveToFirst()) {
                    String loai = safeGet(cursor, 1);
                    String ngayCap = safeGet(cursor, 2);
                    String ngayHetHan = safeGet(cursor, 3);
                    String noiCap = safeGet(cursor, 4);

                    if (edtType != null) edtType.setText(notEmpty(loai, ""));
                    if (edtDateIssued != null) {
                        Date d = tryParseDate(notEmpty(ngayCap, ""));
                        if (d != null) edtDateIssued.setText(uiDateFormat.format(d));
                        else edtDateIssued.setText(notEmpty(ngayCap, ""));
                    }
                    if (edtExpired != null) {
                        Date d = tryParseDate(notEmpty(ngayHetHan, ""));
                        if (d != null) edtExpired.setText(uiDateFormat.format(d));
                        else edtExpired.setText(notEmpty(ngayHetHan, ""));
                    }
                    if (edtAdress != null) edtAdress.setText(notEmpty(noiCap, ""));
                } else {
                    if (edtType != null) { edtType.setText(""); edtType.setHint("Chưa có bằng cấp"); }
                    if (edtDateIssued != null) { edtDateIssued.setText(""); edtDateIssued.setHint("Chưa có ngày cấp (chạm để chọn)"); }
                    if (edtExpired != null) { edtExpired.setText(""); edtExpired.setHint("Chưa có ngày hết hạn (chạm để chọn)"); }
                    if (edtAdress != null) { edtAdress.setText(""); edtAdress.setHint("Chưa có nơi cấp"); }
                }
                if (cursor != null) { cursor.close(); cursor = null; }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Lỗi đọc DB: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }

    // ---------- Save (chỉnh: MatKhau/Email xử lý trong TaiKhoan; NguoiDung không lưu MatKhau) ----------
    private void saveData() {
        String name = edtName.getText().toString().trim();
        String cccd = edtCCCD.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String matkhau = edtStatus.getText().toString().trim();

        String chieuCaoStr = edtHeight.getText().toString().trim();
        String canNangStr = edtWeight.getText().toString().trim();
        String benhNen = edtDisease.getText().toString().trim();
        String ngayKham = edtLastCheck.getText().toString().trim(); // dd/MM/yyyy
        String maTuy = edtResult.getText().toString().trim();
        String ketLuan = edtConclusion.getText().toString().trim();

        String loai = edtType != null ? edtType.getText().toString().trim() : "";
        String ngayCap = edtDateIssued != null ? edtDateIssued.getText().toString().trim() : ""; // dd/MM/yyyy
        String ngayHetHan = edtExpired != null ? edtExpired.getText().toString().trim() : ""; // dd/MM/yyyy
        String noiCap = edtAdress != null ? edtAdress.getText().toString().trim() : "";

        String ngaySinh = edtBirthday != null ? edtBirthday.getText().toString().trim() : "";
        String gioiTinh = edtGender != null ? edtGender.getText().toString().trim() : "";

        if (userId == -1) {
            Toast.makeText(requireContext(), "Không tìm thấy user để lưu.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(cccd)) {
            Toast.makeText(requireContext(), "Vui lòng nhập tên và CCCD.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isAdmin = isAdminRole(userRole);

        // Nếu user không phải admin và user nhập mật khẩu (không rỗng) -> validate theo rule
        if (!isAdmin && !TextUtils.isEmpty(matkhau)) {
            if (!PASSWORD_STRONG_PATTERN.matcher(matkhau).matches()) {
                setFieldErrorAbove(edtStatus, "Mật khẩu không hợp lệ");
                Toast.makeText(requireContext(), "Mật khẩu không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                // 1) Xử lý TaiKhoan: cập nhật hoặc tạo mới nếu cần
                if (maTaiKhoan != -1) {
                    ContentValues tkValues = new ContentValues();
                    boolean needUpdateTK = false;
                    // Only update if provided (email field is read-only, but still handle if changed programmatically)
                    if (!TextUtils.isEmpty(matkhau)) { tkValues.put("MatKhau", matkhau); needUpdateTK = true; }
                    if (!TextUtils.isEmpty(email)) { tkValues.put("Email", email); needUpdateTK = true; }
                    if (needUpdateTK) {
                        int rows = db.update("TaiKhoan", tkValues, "MaTaiKhoan = ?", new String[]{String.valueOf(maTaiKhoan)});
                        if (rows <= 0) Log.w(TAG, "Update TaiKhoan returned 0 rows for id=" + maTaiKhoan);
                    }
                } else {
                    // no maTaiKhoan: create one if password/email provided
                    if (!TextUtils.isEmpty(matkhau) || !TextUtils.isEmpty(email)) {
                        ContentValues tkValues = new ContentValues();
                        if (!TextUtils.isEmpty(matkhau)) tkValues.put("MatKhau", matkhau);
                        if (!TextUtils.isEmpty(email)) tkValues.put("Email", email);
                        long tkId = db.insert("TaiKhoan", null, tkValues);
                        if (tkId == -1) {
                            throw new RuntimeException("Insert TaiKhoan failed (insert returned -1). Kiểm tra ràng buộc trên bảng TaiKhoan.");
                        }
                        maTaiKhoan = (int) tkId;
                        // gán MaTaiKhoan vào NguoiDung sau
                    }
                }

                // 2) Cập nhật NguoiDung (không lưu MatKhau)
                ContentValues userValues = new ContentValues();
                userValues.put("HoTen", name);
                userValues.put("CCCD", cccd);
                userValues.put("SDT", phone);
                if (!TextUtils.isEmpty(ngaySinh)) userValues.put("NgaySinh", ngaySinh);
                if (!TextUtils.isEmpty(gioiTinh)) userValues.put("GioiTinh", gioiTinh);
                if (maTaiKhoan != -1) userValues.put("MaTaiKhoan", maTaiKhoan);

                int updated = db.update("NguoiDung", userValues, "MaNguoiDung = ?", new String[]{String.valueOf(userId)});
                if (updated <= 0) {
                    Log.w(TAG, "Update NguoiDung returned 0 rows for id=" + userId);
                }

                // 3) SucKhoe (giữ logic cũ)
                cursor = db.rawQuery("SELECT MaSucKhoe FROM SucKhoe WHERE MaNguoiDung = ?", new String[]{String.valueOf(userId)});
                if (cursor != null && cursor.moveToFirst()) {
                    ContentValues healthValues = new ContentValues();
                    Double chieuCao = parseDoubleOrNull(chieuCaoStr);
                    Double canNang = parseDoubleOrNull(canNangStr);
                    if (chieuCao != null) healthValues.put("ChieuCao", chieuCao);
                    else healthValues.putNull("ChieuCao");
                    if (canNang != null) healthValues.put("CanNang", canNang);
                    else healthValues.putNull("CanNang");

                    healthValues.put("BenhNen", benhNen);
                    healthValues.put("NgayKham", ngayKham);
                    Integer maTuyInt = parseIntOrNull(maTuy);
                    if (maTuyInt != null) healthValues.put("MaTuy", maTuyInt);
                    else if (!maTuy.isEmpty()) healthValues.put("MaTuy", maTuy);
                    healthValues.put("KetLuan", ketLuan);

                    int healthUpdated = db.update("SucKhoe", healthValues, "MaNguoiDung = ?", new String[]{String.valueOf(userId)});
                    if (healthUpdated < 1) {
                        healthValues.put("MaNguoiDung", userId);
                        db.insert("SucKhoe", null, healthValues);
                    }
                } else {
                    ContentValues healthValues = new ContentValues();
                    healthValues.put("MaNguoiDung", userId);
                    Double chieuCao = parseDoubleOrNull(chieuCaoStr);
                    Double canNang = parseDoubleOrNull(canNangStr);
                    if (chieuCao != null) healthValues.put("ChieuCao", chieuCao);
                    if (canNang != null) healthValues.put("CanNang", canNang);
                    healthValues.put("BenhNen", benhNen);
                    healthValues.put("NgayKham", ngayKham);
                    Integer maTuyInt = parseIntOrNull(maTuy);
                    if (maTuyInt != null) healthValues.put("MaTuy", maTuyInt);
                    healthValues.put("KetLuan", ketLuan);

                    db.insert("SucKhoe", null, healthValues);
                }
                if (cursor != null) { cursor.close(); cursor = null; }

                // 4) BangCap (chỉ cho non-admin)
                if (!isAdmin) {
                    cursor = db.rawQuery("SELECT MaBangCap FROM BangCap WHERE MaNguoiDung = ?", new String[]{String.valueOf(userId)});
                    ContentValues bcValues = new ContentValues();
                    bcValues.put("MaNguoiDung", userId);
                    if (!TextUtils.isEmpty(loai)) bcValues.put("Loai", loai);
                    else bcValues.putNull("Loai");
                    if (!TextUtils.isEmpty(ngayCap)) bcValues.put("NgayCap", ngayCap);
                    else bcValues.putNull("NgayCap");
                    if (!TextUtils.isEmpty(ngayHetHan)) bcValues.put("NgayHetHan", ngayHetHan);
                    else bcValues.putNull("NgayHetHan");
                    if (!TextUtils.isEmpty(noiCap)) bcValues.put("NoiCap", noiCap);
                    else bcValues.putNull("NoiCap");

                    if (cursor != null && cursor.moveToFirst()) {
                        int rows = db.update("BangCap", bcValues, "MaNguoiDung = ?", new String[]{String.valueOf(userId)});
                        if (rows > 0) {
                            Log.d(TAG, "Cập nhật bằng cấp thành công.");
                        } else {
                            long id = db.insert("BangCap", null, bcValues);
                            if (id != -1) Log.d(TAG, "Lưu bằng cấp (tạo mới). id=" + id);
                        }
                    } else {
                        long id = db.insert("BangCap", null, bcValues);
                        if (id != -1) Log.d(TAG, "Tạo bằng cấp thành công id=" + id);
                    }
                    if (cursor != null) { cursor.close(); cursor = null; }
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            Toast.makeText(requireContext(), "Lưu thông tin thành công.", Toast.LENGTH_SHORT).show();

            Bundle result = new Bundle();
            result.putBoolean("profile_updated", true);
            getParentFragmentManager().setFragmentResult("profile_update", result);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Lỗi lưu dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }

    // NEW: show/hide only the degree section (safer)
    private void showDegreeFields(boolean show) {
        int v = show ? View.VISIBLE : View.GONE;
        if (degreeSection != null) {
            degreeSection.setVisibility(v);
            return;
        }
        if (edtType != null) edtType.setVisibility(v);
        if (edtDateIssued != null) edtDateIssued.setVisibility(v);
        if (edtExpired != null) edtExpired.setVisibility(v);
        if (edtAdress != null) edtAdress.setVisibility(v);
    }

    // Kiểm tra role có phải admin không (so sánh lowercase, chứa "admin" hoặc "quản trị")
    private boolean isAdminRole(String role) {
        if (role == null) return false;
        String r = role.trim().toLowerCase();
        return r.contains("admin") || r.contains("quản trị") || r.contains("quản trị viên") || r.contains("administrator");
    }

    // Helpers (unchanged)
    private Double parseDoubleOrNull(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        s = s.replaceAll("[^0-9\\.,-]", "");
        s = s.replace(',', '.');
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseIntOrNull(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String safeGet(Cursor c, int idx) {
        try {
            return c.isNull(idx) ? "" : c.getString(idx);
        } catch (Exception e) {
            return "";
        }
    }

    private String notEmpty(String s, String def) {
        if (s == null) return def;
        s = s.trim();
        return s.isEmpty() ? def : s;
    }

    // ---------------- inline-error helpers (show TextView above EditText) ----------------
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
        if (edtName != null) removeFieldErrorAbove(edtName);
        if (edtCCCD != null) removeFieldErrorAbove(edtCCCD);
        if (edtPhone != null) removeFieldErrorAbove(edtPhone);
        if (edtLastCheck != null) removeFieldErrorAbove(edtLastCheck);
        if (edtExpired != null) removeFieldErrorAbove(edtExpired);
        if (edtEmail != null) removeFieldErrorAbove(edtEmail);
        if (edtBirthday != null) removeFieldErrorAbove(edtBirthday);
        if (edtGender != null) removeFieldErrorAbove(edtGender);
        if (edtDateIssued != null) removeFieldErrorAbove(edtDateIssued);
        if (edtAdress != null) removeFieldErrorAbove(edtAdress);
        if (edtStatus != null) removeFieldErrorAbove(edtStatus);

        boolean ok = true;

        boolean isAdmin = isAdminRole(userRole);

        // NAME
        String name = edtName == null ? "" : edtName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            setFieldErrorAbove(edtName, "Vui lòng điền họ tên");
            ok = false;
        } else if (!NAME_PATTERN.matcher(name).matches()) {
            setFieldErrorAbove(edtName, "Họ tên không hợp lệ");
            ok = false;
        }

        // CCCD
        String cccd = edtCCCD == null ? "" : edtCCCD.getText().toString().trim();
        if (TextUtils.isEmpty(cccd)) {
            setFieldErrorAbove(edtCCCD, "Vui lòng nhập CCCD");
            ok = false;
        } else if (!CCCD_PATTERN.matcher(cccd).matches()) {
            setFieldErrorAbove(edtCCCD, "CCCD phải gồm 12 chữ số");
            ok = false;
        }

        // PHONE
        String phone = edtPhone == null ? "" : edtPhone.getText().toString().trim();
        if (TextUtils.isEmpty(phone)) {
            setFieldErrorAbove(edtPhone, "Vui lòng nhập số điện thoại");
            ok = false;
        } else if (!PHONE_PATTERN_10_11.matcher(phone).matches()) {
            setFieldErrorAbove(edtPhone, "Số điện thoại không hợp lệ (10 chữ số)");
            ok = false;
        }

        // EMAIL (validate format but not editable)
        String email = edtEmail == null ? "" : edtEmail.getText().toString().trim();
        if (!TextUtils.isEmpty(email) && !EMAIL_PATTERN.matcher(email).matches()) {
            setFieldErrorAbove(edtEmail, "Email không hợp lệ");
            ok = false;
        }

        // Birthday must be past if provided
        if (edtBirthday != null) {
            String s = edtBirthday.getText() == null ? "" : edtBirthday.getText().toString().trim();
            if (!TextUtils.isEmpty(s)) {
                Date d = tryParseDate(s);
                if (d == null) { setFieldErrorAbove(edtBirthday, "Ngày sinh không hợp lệ"); ok = false; }
                else if (!isBeforeToday(d)) { setFieldErrorAbove(edtBirthday, "Ngày sinh phải là ngày trong quá khứ"); ok = false; }
            }
        }

        // LastCheck <= today
        if (edtLastCheck != null) {
            String s = edtLastCheck.getText() == null ? "" : edtLastCheck.getText().toString().trim();
            if (!TextUtils.isEmpty(s)) {
                Date d = tryParseDate(s);
                if (d == null) { setFieldErrorAbove(edtLastCheck, "Ngày khám không hợp lệ"); ok = false; }
                else if (isAfterToday(d)) { setFieldErrorAbove(edtLastCheck, "Ngày khám không hợp lệ"); ok = false; }
            }
        }

        // Validate degree fields only for non-admin users
        if (!isAdmin) {
            // DateIssued < today (if provided)
            if (edtDateIssued != null) {
                String s = edtDateIssued.getText() == null ? "" : edtDateIssued.getText().toString().trim();
                if (!TextUtils.isEmpty(s)) {
                    Date d = tryParseDate(s);
                    if (d == null) { setFieldErrorAbove(edtDateIssued, "Ngày cấp không hợp lệ"); ok = false; }
                    else if (!isBeforeToday(d)) { setFieldErrorAbove(edtDateIssued, "Ngày cấp không hợp lệ"); ok = false; }
                }
            }

            // DateExpired >= today
            if (edtExpired != null) {
                String s = edtExpired.getText() == null ? "" : edtExpired.getText().toString().trim();
                if (!TextUtils.isEmpty(s)) {
                    Date d = tryParseDate(s);
                    if (d == null) { setFieldErrorAbove(edtExpired, "Ngày hết hạn không hợp lệ"); ok = false; }
                    else if (isBeforeToday(d)) { setFieldErrorAbove(edtExpired, "Bằng đã hết hạn, không hợp lệ"); ok = false; }
                }
            }
        }

        // Gender (if provided) must be Nam/Nữ
        if (edtGender != null) {
            String g = edtGender.getText() == null ? "" : edtGender.getText().toString().trim();
            if (!TextUtils.isEmpty(g)) {
                String low = g.toLowerCase(Locale.getDefault());
                if (!(low.equals("nam") || low.equals("nữ") || low.equals("nu"))) {
                    setFieldErrorAbove(edtGender, "Vui lòng chọn giới tính (Nam/Nữ)"); ok = false;
                }
            }
        }

        // PASSWORD rule for non-admin: if password not empty -> must match strong pattern
        if (!isAdmin) {
            String pwd = edtStatus == null ? "" : edtStatus.getText().toString().trim();
            if (!TextUtils.isEmpty(pwd) && !PASSWORD_STRONG_PATTERN.matcher(pwd).matches()) {
                setFieldErrorAbove(edtStatus, "Mật khẩu tối thiểu 8 ký tự, gồm số, chữ thường, chữ in hoa, ký tự @#$%&");
                ok = false;
            }
        }

        return ok;
    }

    private Date parseDateStrict(String s) {
        if (TextUtils.isEmpty(s)) return null;
        try {
            return uiDateFormat.parse(s);
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
