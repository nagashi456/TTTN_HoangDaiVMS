package com.example.tttn_hoangdaivms.Profile;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tttn_hoangdaivms.Database.Database;
import com.example.tttn_hoangdaivms.R;

public class EditProfileFragment extends Fragment {

    private static final String PREFS_NAME = "app_prefs";
    private static final String PREF_KEY_EMAIL = "logged_email";

    private EditText edtName, edtCCCD, edtPhone, edtEmail, edtStatus;
    private EditText edtHeight, edtWeight, edtDisease, edtLastCheck, edtResult, edtConclusion;
    private ImageView btnBack;
    private Button btnSave;

    private Database dbHelper;
    private int userId = -1;
    private String userEmail;

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

        edtHeight = view.findViewById(R.id.etHeight);
        edtWeight = view.findViewById(R.id.etWeight);
        edtDisease = view.findViewById(R.id.etDisease);
        edtLastCheck = view.findViewById(R.id.etLastCheckup);
        edtResult = view.findViewById(R.id.etDrugTest);
        edtConclusion = view.findViewById(R.id.etConclusion);

        dbHelper = new Database(requireContext());

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

        // Nếu bạn không muốn cho chỉnh email trực tiếp, có thể khóa:
        // edtEmail.setEnabled(false);
        // Mình để mở để bạn có thể sửa nếu muốn (nhưng lưu ý unique email constraint trong TaiKhoan)

        return view;
    }

    /**
     * Nạp thông tin người dùng từ DB (NguoiDung + TaiKhoan -> email) và SucKhoe (nếu có)
     */
    private void loadUserAndHealth(String email) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();

            // Lấy MaNguoiDung theo email (join TaiKhoan -> NguoiDung)
            cursor = db.rawQuery(
                    "SELECT ND.MaNguoiDung, ND.HoTen, ND.CCCD, ND.SDT, ND.TrangThai, ND.VaiTro, ND.NgaySinh, TK.Email " +
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
                String ngaySinh = safeGet(cursor, 6);
                String emailFromDb = safeGet(cursor, 7);

                // Gán lên UI
                edtName.setText(notEmpty(hoTen, ""));
                edtCCCD.setText(notEmpty(cccd, ""));
                edtPhone.setText(notEmpty(sdt, ""));
                edtEmail.setText(notEmpty(emailFromDb, email)); // ưu tiên DB email
                edtStatus.setText(notEmpty(trangThai, ""));

            } else {
                Toast.makeText(requireContext(), "Không tìm thấy hồ sơ người dùng.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (cursor != null) { cursor.close(); cursor = null; }

            // Kiểm tra SucKhoe theo MaNguoiDung
            cursor = db.rawQuery(
                    "SELECT MaSucKhoe, ChieuCao, CanNang, BenhNen, NgayKham, MaTuy, KetLuan " +
                            "FROM SucKhoe WHERE MaNguoiDung = ?",
                    new String[]{String.valueOf(userId)}
            );

            if (cursor != null && cursor.moveToFirst()) {
                // Nếu có, hiển thị
                String chieuCao = safeGet(cursor, 1);
                String canNang = safeGet(cursor, 2);
                String benhNen = safeGet(cursor, 3);
                String ngayKham = safeGet(cursor, 4);
                String maTuy = safeGet(cursor, 5);
                String ketLuan = safeGet(cursor, 6);

                edtHeight.setText(notEmpty(chieuCao, ""));
                edtWeight.setText(notEmpty(canNang, ""));
                edtDisease.setText(notEmpty(benhNen, ""));
                edtLastCheck.setText(notEmpty(ngayKham, ""));
                edtResult.setText(notEmpty(maTuy, ""));
                edtConclusion.setText(notEmpty(ketLuan, ""));
            } else {
                // Nếu không có SucKhoe, để trống (user có thể nhập)
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Lỗi đọc DB: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }

    /**
     * Lưu / Cập nhật dữ liệu:
     * - Cập nhật NguoiDung (HoTen, CCCD, SDT, TrangThai nếu muốn)
     * - Kiểm tra SucKhoe: nếu đã có -> update, nếu chưa -> insert
     */
    private void saveData() {
        String name = edtName.getText().toString().trim();
        String cccd = edtCCCD.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String trangThai = edtStatus.getText().toString().trim();

        String chieuCaoStr = edtHeight.getText().toString().trim();
        String canNangStr = edtWeight.getText().toString().trim();
        String benhNen = edtDisease.getText().toString().trim();
        String ngayKham = edtLastCheck.getText().toString().trim();
        String maTuy = edtResult.getText().toString().trim();
        String ketLuan = edtConclusion.getText().toString().trim();

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

            // --- Cập nhật NguoiDung ---
            ContentValues userValues = new ContentValues();
            userValues.put("HoTen", name);
            userValues.put("CCCD", cccd);
            userValues.put("SDT", phone);
            if (!TextUtils.isEmpty(trangThai)) userValues.put("TrangThai", trangThai);

            int updated = db.update("NguoiDung", userValues, "MaNguoiDung = ?", new String[]{String.valueOf(userId)});

            // --- SucKhoe: kiểm tra tồn tại ---
            cursor = db.rawQuery("SELECT MaSucKhoe FROM SucKhoe WHERE MaNguoiDung = ?", new String[]{String.valueOf(userId)});
            if (cursor != null && cursor.moveToFirst()) {
                // Update existing
                ContentValues healthValues = new ContentValues();
                // convert height/weight to numeric if possible, else store null
                Double chieuCao = parseDoubleOrNull(chieuCaoStr);
                Double canNang = parseDoubleOrNull(canNangStr);
                if (chieuCao != null) healthValues.put("ChieuCao", chieuCao);
                else healthValues.putNull("ChieuCao");
                if (canNang != null) healthValues.put("CanNang", canNang);
                else healthValues.putNull("CanNang");

                healthValues.put("BenhNen", benhNen);
                healthValues.put("NgayKham", ngayKham);
                // MaTuy có vẻ integer flag; try parse
                Integer maTuyInt = parseIntOrNull(maTuy);
                if (maTuyInt != null) healthValues.put("MaTuy", maTuyInt);
                else if (!maTuy.isEmpty()) healthValues.put("MaTuy", maTuy); // fallback store as text if schema accepts
                healthValues.put("KetLuan", ketLuan);

                int healthUpdated = db.update("SucKhoe", healthValues, "MaNguoiDung = ?", new String[]{String.valueOf(userId)});
                if (healthUpdated < 1) {
                    // fallback: try insert
                    healthValues.put("MaNguoiDung", userId);
                    db.insert("SucKhoe", null, healthValues);
                }
            } else {
                // Insert new SucKhoe
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

            // Đóng cursor nếu mở
            if (cursor != null) { cursor.close(); cursor = null; }

            String msg = (updated > 0) ? "Cập nhật thông tin thành công." : "Đã lưu (tạo mới) thông tin người dùng.";
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();

            // Gửi result nếu các fragment khác cần lắng nghe
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

    // Helpers
    private Double parseDoubleOrNull(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        // Remove non-digit like "m" or "kg" commonly present in UI
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
