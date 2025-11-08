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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

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

public class EditProfileFragment extends Fragment {

    private static final String TAG = "EditProfileFrag";
    private static final String PREFS_NAME = "app_prefs";
    private static final String PREF_KEY_EMAIL = "logged_email";

    private EditText edtName, edtCCCD, edtPhone, edtEmail, edtStatus;
    private EditText edtType, edtExpired, edtAdress; // Bằng cấp
    private EditText edtHeight, edtWeight, edtDisease, edtLastCheck, edtResult, edtConclusion;
    private ImageView btnBack;
    private Button btnSave;

    private View degreeSection; // <<< container for degree fields

    private Database dbHelper;
    private int userId = -1;
    private String userEmail;
    private String userRole = ""; // VaiTro lấy từ DB

    // Date format shown to user
    private final SimpleDateFormat uiDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    // possible parse formats from DB
    private final String[] parseFormats = new String[]{"yyyy-MM-dd", "dd/MM/yyyy", "yyyy/MM/dd", "dd-MM-yyyy"};

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

        dbHelper = new Database(requireContext());

        // Làm cho các EditText ngày không hiện bàn phím mà mở DatePicker khi bấm
        setupDatePickers();

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

        // Nạp dữ liệu từ DB và hiển thị
        loadUserAndHealth(userEmail);

        // Nút back
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Nút save: validate và lưu
        btnSave.setOnClickListener(v -> saveData());

        return view;
    }

    // Thiết lập hành vi cho các EditText ngày: disable keyboard, mở DatePicker khi bấm/focus
    private void setupDatePickers() {
        if (edtLastCheck != null) {
            edtLastCheck.setInputType(InputType.TYPE_NULL);
            edtLastCheck.setFocusable(false);
            edtLastCheck.setOnClickListener(v -> showDatePickerForEditText(edtLastCheck));
            edtLastCheck.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) showDatePickerForEditText(edtLastCheck);
            });
        }
        if (edtExpired != null) {
            edtExpired.setInputType(InputType.TYPE_NULL);
            edtExpired.setFocusable(false);
            edtExpired.setOnClickListener(v -> showDatePickerForEditText(edtExpired));
            edtExpired.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) showDatePickerForEditText(edtExpired);
            });
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
                    "SELECT ND.MaNguoiDung, ND.HoTen, ND.CCCD, ND.SDT, ND.TrangThai, COALESCE(ND.VaiTro, ''), ND.NgaySinh, TK.Email " +
                            "FROM NguoiDung ND " +
                            "LEFT JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan " +
                            "WHERE TK.Email = ?",
                    new String[]{email}
            );

            if (cursor != null && cursor.moveToFirst()) {
                userId = cursor.getInt(0);
                String hoTen = safeGet(cursor, 1);
                String cccd = safeGet(cursor, 2);
                String sdt = safeGet(cursor, 3);
                String trangThai = safeGet(cursor, 4);
                String vaiTro = safeGet(cursor, 5);
                String emailFromDb = safeGet(cursor, 7);

                userRole = vaiTro != null ? vaiTro.trim() : "";

                if (edtName != null) edtName.setText(notEmpty(hoTen, ""));
                if (edtCCCD != null) edtCCCD.setText(notEmpty(cccd, ""));
                if (edtPhone != null) edtPhone.setText(notEmpty(sdt, ""));
                if (edtEmail != null) edtEmail.setText(notEmpty(emailFromDb, email));
                if (edtStatus != null) edtStatus.setText(notEmpty(trangThai, ""));
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
                if (edtLastCheck != null) { edtLastCheck.setText(notEmpty(ngayKham, "")); edtLastCheck.setHint(""); }
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
                        "SELECT MaBangCap, Loai, NgayHetHan, NoiCap " +
                                "FROM BangCap WHERE MaTaiXe = ? LIMIT 1",
                        new String[]{String.valueOf(userId)}
                );

                if (cursor != null && cursor.moveToFirst()) {
                    String loai = safeGet(cursor, 1);
                    String ngayHetHan = safeGet(cursor, 2);
                    String noiCap = safeGet(cursor, 3);

                    if (edtType != null) edtType.setText(notEmpty(loai, ""));
                    if (edtExpired != null) edtExpired.setText(notEmpty(ngayHetHan, ""));
                    if (edtAdress != null) edtAdress.setText(notEmpty(noiCap, ""));
                } else {
                    if (edtType != null) { edtType.setText(""); edtType.setHint("Chưa có bằng cấp"); }
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

    private void saveData() {
        String name = edtName.getText().toString().trim();
        String cccd = edtCCCD.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String trangThai = edtStatus.getText().toString().trim();

        String chieuCaoStr = edtHeight.getText().toString().trim();
        String canNangStr = edtWeight.getText().toString().trim();
        String benhNen = edtDisease.getText().toString().trim();
        String ngayKham = edtLastCheck.getText().toString().trim(); // dd/MM/yyyy
        String maTuy = edtResult.getText().toString().trim();
        String ketLuan = edtConclusion.getText().toString().trim();

        String loai = edtType != null ? edtType.getText().toString().trim() : "";
        String ngayHetHan = edtExpired != null ? edtExpired.getText().toString().trim() : ""; // dd/MM/yyyy
        String noiCap = edtAdress != null ? edtAdress.getText().toString().trim() : "";

        if (userId == -1) {
            Toast.makeText(requireContext(), "Không tìm thấy user để lưu.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(cccd)) {
            Toast.makeText(requireContext(), "Vui lòng nhập tên và CCCD.", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getWritableDatabase();

            ContentValues userValues = new ContentValues();
            userValues.put("HoTen", name);
            userValues.put("CCCD", cccd);
            userValues.put("SDT", phone);
            if (!TextUtils.isEmpty(trangThai)) userValues.put("TrangThai", trangThai);

            int updated = db.update("NguoiDung", userValues, "MaNguoiDung = ?", new String[]{String.valueOf(userId)});

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

            if (!isAdminRole(userRole)) {
                cursor = db.rawQuery("SELECT MaBangCap FROM BangCap WHERE MaTaiXe = ?", new String[]{String.valueOf(userId)});
                ContentValues bcValues = new ContentValues();
                bcValues.put("MaTaiXe", userId);
                if (!TextUtils.isEmpty(loai)) bcValues.put("Loai", loai);
                else bcValues.putNull("Loai");
                if (!TextUtils.isEmpty(ngayHetHan)) bcValues.put("NgayHetHan", ngayHetHan);
                else bcValues.putNull("NgayHetHan");
                if (!TextUtils.isEmpty(noiCap)) bcValues.put("NoiCap", noiCap);
                else bcValues.putNull("NoiCap");

                if (cursor != null && cursor.moveToFirst()) {
                    int rows = db.update("BangCap", bcValues, "MaTaiXe = ?", new String[]{String.valueOf(userId)});
                    if (rows > 0) {
                        Toast.makeText(requireContext(), "Cập nhật bằng cấp thành công.", Toast.LENGTH_SHORT).show();
                    } else {
                        long id = db.insert("BangCap", null, bcValues);
                        if (id != -1) Toast.makeText(requireContext(), "Lưu bằng cấp (tạo mới).", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    long id = db.insert("BangCap", null, bcValues);
                    if (id != -1) {
                        Toast.makeText(requireContext(), "Tạo bằng cấp thành công.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Lỗi khi tạo bằng cấp.", Toast.LENGTH_SHORT).show();
                    }
                }
                if (cursor != null) { cursor.close(); cursor = null; }
            }

            String msg = (updated > 0) ? "Cập nhật thông tin thành công." : "Đã lưu (tạo mới) thông tin người dùng.";
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();

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
        // fallback: chỉ set trực tiếp lên từng edittext (không chạm parent)
        if (edtType != null) edtType.setVisibility(v);
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
}
