package com.example.tttn_hoangdaivms.Home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tttn_hoangdaivms.Database.Database;
import com.example.tttn_hoangdaivms.Login.LoginActivity;
import com.example.tttn_hoangdaivms.Profile.EditProfileFragment;
import com.example.tttn_hoangdaivms.R;
import com.google.android.material.button.MaterialButton;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;

public class HomeFragement extends Fragment {

    private TextView tvName, tvCCCD, tvPhone, tvEmail, tvStatus;
    private TextView tvUserName, tvVehicleCount, tvDriverCount;

    // Health-related TextViews
    private TextView tvHeight, tvWeight, tvHealthNotes, tvLastCheckup, tvHealthConclusion, tvDrugTest;
    private TextView tvType, tvDateExpired,tvAddress;

    private LinearLayout stats,certificate;
    private MaterialButton btnLogout;
    private ImageView btnEdit,btnEditProfile;

    private Database dbHelper;
    private String loggedInEmail;

    private View cardDriverView, cardVehicleView; // các view cần ẩn/hiện

    private static final String PREFS_NAME = "app_prefs";
    private static final String PREF_KEY_EMAIL = "logged_email";

    // colors
    private final int COLOR_HINT = Color.parseColor("#A0A0A0"); // nhạt
    private final int COLOR_NORMAL = Color.parseColor("#222222"); // bình thường

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragement_home, container, false);

        // Ánh xạ view (IDs phải trùng với layout)
        tvName = view.findViewById(R.id.tvName);
        tvCCCD = view.findViewById(R.id.tvCCCD);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvStatus = view.findViewById(R.id.tvStatus);

        tvUserName = view.findViewById(R.id.tvUserName);
        tvVehicleCount = view.findViewById(R.id.tvVehicleCount);
        tvDriverCount = view.findViewById(R.id.tvDriverCount);

        // Health fields — ensure these IDs exist in your layout
        tvHeight = view.findViewById(R.id.etHeight);
        tvWeight = view.findViewById(R.id.etWeight);
        tvHealthNotes = view.findViewById(R.id.etDisease);
        tvLastCheckup = view.findViewById(R.id.etLastCheckup);
        tvDrugTest = view.findViewById(R.id.etDrugTest);
        tvHealthConclusion = view.findViewById(R.id.etConclusion);

        tvType = view.findViewById(R.id.type);
        tvDateExpired = view.findViewById(R.id.dateExpired);
        tvAddress = view.findViewById(R.id.address);

        btnLogout = view.findViewById(R.id.btnLogout);
        btnEdit = view.findViewById(R.id.btnEdit);

        // ánh xạ 2 card cần ẩn/hiện (IDs: Driver và Vehicle)
        cardDriverView = view.findViewById(R.id.Driver);
        cardVehicleView = view.findViewById(R.id.Vehicle);
        stats = view.findViewById(R.id.Stats);
        certificate = view.findViewById(R.id.certificate);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        dbHelper = new Database(requireContext());

        // 1) Lấy email từ arguments (nếu MainActivity setBundle/extras)
        Bundle args = getArguments();
        loggedInEmail = extractEmailFromBundle(args);

        // 2) Nếu chưa có trong args, đọc từ SharedPreferences (auto-login case)
        if (loggedInEmail == null) {
            SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            loggedInEmail = prefs.getString(PREF_KEY_EMAIL, null);
        } else {
            // nếu lấy được từ args, lưu lại vào prefs để dùng lần sau
            SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(PREF_KEY_EMAIL, loggedInEmail).apply();
        }

        if (loggedInEmail == null) {
            Toast.makeText(requireContext(), "Chưa có thông tin đăng nhập, vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            goToLoginAndFinishHost();
            return view;
        }

        // Ẩn/hiện card theo quyền trước khi load chi tiết (tránh flicker)
        boolean isAdmin = isAdminUser(loggedInEmail);
        applyAdminVisibility(isAdmin);

        // Nạp dữ liệu và gắn listener
        loadUserData();
        loadCounts();
        loadHealthData();   // <-- load health info
        loadCertificateData();
        setupListeners();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // refresh dữ liệu khi quay lại fragment (ví dụ sau khi edit profile)
        if (loggedInEmail != null) {
            loadUserData();
            loadCounts();
            loadHealthData();

            // mỗi lần resume có thể re-check role (trường hợp role thay đổi runtime)
            boolean isAdmin = isAdminUser(loggedInEmail);
            applyAdminVisibility(isAdmin);
        }
    }

    /**
     * Áp visibility cho 2 card theo isAdmin:
     * - nếu isAdmin == true -> VISIBLE
     * - nếu isAdmin == false -> c
     */
    private void applyAdminVisibility(boolean isAdmin) {
        int visible = isAdmin ? View.VISIBLE : View.GONE;
        int editVisible = isAdmin ? View.GONE: View.VISIBLE;

        if (cardDriverView != null) cardDriverView.setVisibility(visible);
        if (cardVehicleView != null) cardVehicleView.setVisibility(visible);
        if (stats != null) stats.setVisibility(visible);
        if (certificate != null) certificate.setVisibility(editVisible);

        // btnEditProfile chỉ hiển thị khi là admin
        if (btnEditProfile != null) btnEditProfile.setVisibility(editVisible);
    }


    /**
     * Kiểm tra người dùng có phải Admin hay không.
     * - Truy vấn NguoiDung.VaiTro dựa trên TK.Email
     * - Nếu role chứa "admin" hoặc "quản trị"/"quản trị viên"/"administrator" -> coi là admin
     * - Fallback: nếu email == admin@vms.com -> admin
     */
    private boolean isAdminUser(String email) {
        if (email == null) return false;

        // fallback admin email mặc định (bạn có thể đổi)
        if ("admin@vms.com".equalsIgnoreCase(email.trim())) return true;

        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery(
                    "SELECT COALESCE(ND.VaiTro, '') FROM NguoiDung ND " +
                            "LEFT JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan " +
                            "WHERE TK.Email = ? LIMIT 1",
                    new String[]{ email }
            );

            if (cursor != null && cursor.moveToFirst()) {
                String role = cursor.isNull(0) ? "" : cursor.getString(0);
                if (role != null) {
                    String rl = role.trim().toLowerCase();
                    if (rl.contains("admin") || rl.contains("quản trị") || rl.contains("quản trị viên") || rl.contains("administrator")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // nếu truy vấn lỗi, không crash, coi là không phải admin
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        return false;
    }

    /**
     * Cố gắng trích email từ Bundle:
     * - ưu tiên key "email"
     * - key "current_user_email"
     * - nếu có object "current_user" (Parcelable/Serializable) thử reflection lấy getEmail() hoặc field "email"
     */
    private String extractEmailFromBundle(@Nullable Bundle args) {
        if (args == null) return null;

        // 1) trực tiếp là string
        if (args.containsKey("email")) {
            String e = args.getString("email");
            if (e != null && !e.trim().isEmpty()) return e.trim();
        }
        if (args.containsKey("current_user_email")) {
            String e = args.getString("current_user_email");
            if (e != null && !e.trim().isEmpty()) return e.trim();
        }

        // 2) thử object parcelable/serializable với key "current_user"
        Object obj = null;
        try {
            if (args.containsKey("current_user")) {
                // try parcelable
                try {
                    Parcelable p = args.getParcelable("current_user");
                    if (p != null) obj = p;
                } catch (Exception ignored) {}

                // try serializable if not parcelable
                if (obj == null) {
                    try {
                        Serializable s = (Serializable) args.getSerializable("current_user");
                        if (s != null) obj = s;
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}

        if (obj != null) {
            // dùng reflection: thử getEmail(), getEmailAddress()
            try {
                Method m = obj.getClass().getMethod("getEmail");
                Object val = m.invoke(obj);
                if (val instanceof String && !((String) val).trim().isEmpty()) {
                    return ((String) val).trim();
                }
            } catch (Exception ignored) {}

            try {
                Method m2 = obj.getClass().getMethod("getEmailAddress");
                Object val = m2.invoke(obj);
                if (val instanceof String && !((String) val).trim().isEmpty()) {
                    return ((String) val).trim();
                }
            } catch (Exception ignored) {}

            // thử field "email"
            try {
                Field f = obj.getClass().getDeclaredField("email");
                f.setAccessible(true);
                Object val = f.get(obj);
                if (val instanceof String && !((String) val).trim().isEmpty()) {
                    return ((String) val).trim();
                }
            } catch (Exception ignored) {}

        }

        return null;
    }

    private void loadUserData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT ND.HoTen, ND.CCCD, ND.SDT, TK.Email, ND.TrangThai " +
                            "FROM NguoiDung ND " +
                            "JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan " +
                            "WHERE TK.Email = ?",
                    new String[]{loggedInEmail}
            );

            if (cursor != null && cursor.moveToFirst()) {
                String hoTen = getColumnSafe(cursor, "HoTen", 0);
                String cccd = getColumnSafe(cursor, "CCCD", 1);
                String sdt = getColumnSafe(cursor, "SDT", 2);
                String email = getColumnSafe(cursor, "Email", 3);
                String trangThai = getColumnSafe(cursor, "TrangThai", 4);

                tvName.setText(notEmptyOrDefault(hoTen, "—"));
                tvCCCD.setText(notEmptyOrDefault(cccd, "—"));
                tvPhone.setText(notEmptyOrDefault(sdt, "—"));
                tvEmail.setText(notEmptyOrDefault(email, "—"));
                tvStatus.setText(notEmptyOrDefault(trangThai, "—"));

                tvUserName.setText(notEmptyOrDefault(hoTen, "Người dùng"));
            } else {
                // Không tìm thấy user; hiển thị mặc định
                tvName.setText("—");
                tvCCCD.setText("—");
                tvPhone.setText("—");
                tvEmail.setText("—");
                tvStatus.setText("—");
                tvUserName.setText("Người dùng");
            }
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void loadCounts() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c1 = null, c2 = null;
        try {
            // Đếm số xe trong bảng Xe
            c1 = db.rawQuery("SELECT COUNT(*) FROM Xe", null);
            if (c1 != null && c1.moveToFirst()) {
                int countXe = c1.getInt(0);
                tvVehicleCount.setText(String.valueOf(countXe));
            } else {
                tvVehicleCount.setText("0");
            }

            // Đếm số người có vai trò 'nhân viên' (ví dụ)
            c2 = db.rawQuery(
                    "SELECT COUNT(*) FROM NguoiDung WHERE lower(COALESCE(VaiTro, '')) LIKE ?",
                    new String[]{"%nhân viên%"}
            );
            if (c2 != null && c2.moveToFirst()) {
                int countEmployee = c2.getInt(0);
                tvDriverCount.setText(String.valueOf(countEmployee));
            } else {
                tvDriverCount.setText("0");
            }
        } finally {
            if (c1 != null) c1.close();
            if (c2 != null) c2.close();
        }
    }

    /**
     * Load thông tin sức khỏe gần nhất của người dùng (bảng SucKhoe).
     * Nếu không có bản ghi => hiển thị hint (màu nhạt).
     */
    private void loadHealthData() {
        // đặt hint mặc định
        setHintStyle(tvHeight, "Chưa có dữ liệu chiều cao");
        setHintStyle(tvWeight, "Chưa có dữ liệu cân nặng");
        setHintStyle(tvHealthNotes, "Chưa có ghi chú sức khỏe");
        setHintStyle(tvDrugTest, "Chưa có kết quả kiểm tra ma túy");
        setHintStyle(tvLastCheckup, "Chưa có lịch khám");
        setHintStyle(tvHealthConclusion, "Chưa có kết luận");

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT SK.ChieuCao, SK.CanNang, SK.BenhNen, SK.MaTuy, SK.NgayKham, SK.KetLuan " +
                            "FROM SucKhoe SK " +
                            "JOIN NguoiDung ND ON SK.MaNguoiDung = ND.MaNguoiDung " +
                            "JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan " +
                            "WHERE TK.Email = ? " +
                            "ORDER BY SK.NgayKham DESC LIMIT 1",
                    new String[]{ String.valueOf(loggedInEmail) }
            );

            if (cursor != null && cursor.moveToFirst()) {
                String chieuCao = getColumnSafe(cursor, "ChieuCao", 0);
                String canNang = getColumnSafe(cursor, "CanNang", 1);
                String benhNen = getColumnSafe(cursor, "BenhNen", 2);
                String maTuyStr = getColumnSafe(cursor, "MaTuy", 3);
                String ngayKham = getColumnSafe(cursor, "NgayKham", 4);
                String ketLuan = getColumnSafe(cursor, "KetLuan", 5);

                // Hiển thị chiều cao / cân nặng
                if (chieuCao != null && !chieuCao.trim().isEmpty()) {
                    setNormalStyle(tvHeight, formatNumberOrDefault(chieuCao, "cm"));
                } else {
                    setHintStyle(tvHeight, "Chưa có dữ liệu chiều cao");
                }

                if (canNang != null && !canNang.trim().isEmpty()) {
                    setNormalStyle(tvWeight, formatNumberOrDefault(canNang, "kg"));
                } else {
                    setHintStyle(tvWeight, "Chưa có dữ liệu cân nặng");
                }

                // Ghi chú sức khỏe
                if (benhNen != null && !benhNen.trim().isEmpty()) {
                    setNormalStyle(tvHealthNotes, benhNen.trim());
                } else {
                    setHintStyle(tvHealthNotes, "Chưa có ghi chú sức khỏe");
                }

                // Xử lý MaTuy: map int -> label (bạn có thể sửa mapping theo DB)
                if (maTuyStr != null && !maTuyStr.trim().isEmpty()) {
                    String drugLabel = mapMaTuyToLabel(maTuyStr.trim());
                    setNormalStyle(tvDrugTest, drugLabel);
                } else {
                    setHintStyle(tvDrugTest, "Chưa có kết quả kiểm tra ma túy");
                }

                // Ngày khám và kết luận
                if (ngayKham != null && !ngayKham.trim().isEmpty()) {
                    setNormalStyle(tvLastCheckup, ngayKham.trim());
                } else {
                    setHintStyle(tvLastCheckup, "Chưa có lịch khám");
                }

                if (ketLuan != null && !ketLuan.trim().isEmpty()) {
                    setNormalStyle(tvHealthConclusion, ketLuan.trim());
                } else {
                    setHintStyle(tvHealthConclusion, "Chưa có kết luận");
                }
            } else {
                // giữ hint mặc định (đã set ở đầu)
            }
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /**
     * Map giá trị MaTuy từ DB sang nhãn dễ đọc.
     * Giả sử MaTuy = 0 -> Âm tính, 1 -> Dương tính; nếu không parse được trả raw.
     */
    private String mapMaTuyToLabel(String maTuyStr) {
        try {
            int code = Integer.parseInt(maTuyStr);
            switch (code) {
                case 0: return "Âm tính";
                case 1: return "Dương tính";
                // Thêm mapping khác nếu DB dùng mã khác
                default: return "Mã: " + code;
            }
        } catch (NumberFormatException e) {
            // nếu MaTuy trong DB đã là mô tả text rồi thì trả nguyên
            return maTuyStr;
        }
    }
    private void loadCertificateData() {
        // đặt hint mặc định
        setHintStyle(tvType, "Chưa có dữ liệu về bằng");
        setHintStyle(tvDateExpired, "Chưa có dữ liệu về hạn sử dụng");
        setHintStyle(tvAddress, "Chưa có dữ liệu về nơi cấp");

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT BC.Loai, BC.NgayHetHan, BC.NoiCap " +
                            "FROM BangCap BC " +
                            "JOIN NguoiDung ND ON BC.MaTaiXe = ND.MaNguoiDung " +
                            "JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan " +
                            "WHERE TK.Email = ? ",
                    new String[]{ String.valueOf(loggedInEmail) }
            );

            if (cursor != null && cursor.moveToFirst()) {
                String Loai = getColumnSafe(cursor, "Loai", 0);
                String NgayHetHan = getColumnSafe(cursor, "NgayHetHan", 1);
                String NoiCap = getColumnSafe(cursor, "NoiCap", 2);

                // Hiển thị chiều cao / cân nặng
                if (Loai!= null && !Loai.trim().isEmpty()) {
                    setNormalStyle(tvType, Loai.trim());
                } else {
                    setHintStyle(tvType, "Chưa có dữ liệu loại bằng");
                }

                if (NgayHetHan != null && !NgayHetHan.trim().isEmpty()) {
                    setNormalStyle(tvDateExpired, NgayHetHan.trim());
                } else {
                    setHintStyle(tvDateExpired, "Chưa có dữ liệu hạn sử dụng");
                }

                // Ghi chú sức khỏe
                if (NoiCap!= null && !NoiCap.trim().isEmpty()) {
                    setNormalStyle(tvAddress, NoiCap.trim());
                } else {
                    setHintStyle(tvAddress, "Chưa có nơi cấp");
                }

            } else {
                // giữ hint mặc định (đã set ở đầu)
            }
        } finally {
            if (cursor != null) cursor.close();
        }
    }


    private String formatNumberOrDefault(String raw, String unit) {
        if (raw == null || raw.trim().isEmpty()) return "—";
        try {
            double v = Double.parseDouble(raw);
            // nếu integer (ví dụ 170.0) hiển thị không phần thập phân
            if (Math.abs(v - Math.round(v)) < 0.0001) {
                return String.format("%d %s", Math.round(v), unit);
            } else {
                DecimalFormat df = new DecimalFormat("#.#");
                return df.format(v) + " " + unit;
            }
        } catch (Exception e) {
            // không parse được => trả raw + unit
            return raw + " " + unit;
        }
    }

    private void setHintStyle(TextView tv, String hint) {
        if (tv == null) return;
        tv.setText(hint);
        tv.setTextColor(COLOR_HINT);
    }

    private void setNormalStyle(TextView tv, String text) {
        if (tv == null) return;
        tv.setText(text);
        tv.setTextColor(COLOR_NORMAL);
    }

    private void setupListeners() {
        // Edit profile -> mở EditProfileFragment, gửi email
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                EditProfileFragment editFragment = new EditProfileFragment();
                Bundle bundle = new Bundle();
                bundle.putString("email", loggedInEmail);
                editFragment.setArguments(bundle);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.containerMain, editFragment)
                        .addToBackStack(null)
                        .commit();
            });
        }
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                EditProfileFragment editFragment = new EditProfileFragment();
                Bundle bundle = new Bundle();
                bundle.putString("email", loggedInEmail);
                editFragment.setArguments(bundle);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.containerMain, editFragment)
                        .addToBackStack(null)
                        .commit();
            });
        }

        // Logout -> xóa SharedPreferences và về LoginActivity (xoá back stack)
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> doLogout());
        }
    }

    private void doLogout() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(PREF_KEY_EMAIL).apply();
        goToLoginAndFinishHost();
    }

    private void goToLoginAndFinishHost() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }

    // Helper để lấy cột an toàn: ưu tiên dùng tên cột nếu có, fallback theo index
    private String getColumnSafe(Cursor c, String colName, int fallbackIndex) {
        try {
            int idx = c.getColumnIndex(colName);
            if (idx >= 0) {
                String s = c.getString(idx);
                return s != null ? s : "";
            } else {
                // fallback theo index (dùng khi alias/cột không đúng tên)
                String s = c.getString(fallbackIndex);
                return s != null ? s : "";
            }
        } catch (Exception e) {
            try {
                String s = c.getString(fallbackIndex);
                return s != null ? s : "";
            } catch (Exception ex) {
                return "";
            }
        }
    }

    private String notEmptyOrDefault(String s, String def) {
        if (s == null) return def;
        s = s.trim();
        return s.isEmpty() ? def : s;
    }
}
