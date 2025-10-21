package com.example.tttn_hoangdaivms.Home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

public class HomeFragement extends Fragment {

    private TextView tvName, tvCCCD, tvPhone, tvEmail, tvStatus;
    private TextView tvUserName, tvVehicleCount, tvDriverCount;
    private MaterialButton btnLogout;
    private ImageView btnEdit;

    private Database dbHelper;
    private String loggedInEmail;

    private static final String PREFS_NAME = "app_prefs";
    private static final String PREF_KEY_EMAIL = "logged_email";

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

        btnLogout = view.findViewById(R.id.btnLogout);
        btnEdit = view.findViewById(R.id.btnEdit);

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

        // Nạp dữ liệu và gắn listener
        loadUserData();
        loadCounts();
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
        }
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
            // dùng reflection: thử getEmail(), getEmailAddress(), getHoTen()?? (getHoTen là tên họ tên, không email)
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

            // nếu User class có phương thức getHoTen() và bạn trong LoginActivity put email vào khác tên,
            // không thể đảm bảo trích ra; hy vọng LoginActivity đã put "email" chuỗi trong bundle.
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

            // Đếm số người có vai trò tài xế (linh hoạt với từ 'tai' hoặc 'tài')
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
