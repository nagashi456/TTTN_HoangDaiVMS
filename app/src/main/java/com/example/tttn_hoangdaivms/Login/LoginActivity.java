package com.example.tttn_hoangdaivms.Login;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tttn_hoangdaivms.Database.Database;
import com.example.tttn_hoangdaivms.Database.User;
import com.example.tttn_hoangdaivms.MainActivity;
import com.example.tttn_hoangdaivms.R;
import com.example.tttn_hoangdaivms.Register.RegisterActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnLogin;
    private TextView tabLogin, tabRegister;
    private Database dbHelper;

    // định dạng dùng cho TrangThaiUpdatedAt
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    // khoảng 24 giờ tính bằng milliseconds
    private static final long MILLIS_24H = 24L * 60L * 60L * 1000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        // Khởi tạo Database 1 lần
        dbHelper = new Database(this);
        // đảm bảo DB được tạo (onCreate sẽ chạy nếu DB chưa tồn tại)
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.close();

        // Ánh xạ view
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tabLogin = findViewById(R.id.tabLogin);
        tabRegister = findViewById(R.id.tabRegister);

        // Xử lý nút Đăng nhập
        btnLogin.setOnClickListener(v -> handleLogin());

        // Tab chuyển đăng ký
        tabRegister.setOnClickListener(v -> {
            setActiveTab(false);
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        tabLogin.setOnClickListener(v -> setActiveTab(true));
    }

    private void handleLogin() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Vui lòng nhập email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Vui lòng nhập mật khẩu");
            return;
        }

        // 1) Kiểm tra credentials (email + password)
        boolean credentialsOk = dbHelper.validateCredentials(email, password);
        if (!credentialsOk) {
            Toast.makeText(this, "Sai thông tin đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2) Lấy trạng thái tài khoản từ bảng NguoiDung
        String status = dbHelper.getUserStatus(email);
        if (status == null) {
            // Không tìm thấy hồ sơ người dùng (cấu trúc DB không nhất quán)
            Toast.makeText(this, "Tài khoản chưa có hồ sơ, liên hệ quản trị.", Toast.LENGTH_LONG).show();
            return;
        }

        // Chuẩn hoá status để so sánh (bỏ khoảng trắng, không phân biệt hoa thường)
        String st = status.trim().toLowerCase();

        if (st.equals("đang yêu cầu") || st.equals("dang yeu cau") || st.equals("pending")) {
            Toast.makeText(this, "Tài khoản đang được duyệt. Vui lòng chờ.", Toast.LENGTH_LONG).show();
            return;
        }

        if (st.equals("đã từ chối") || st.equals("da tu choi") || st.equals("rejected") || st.equals("tu choi")) {
            Toast.makeText(this, "Tài khoản đã bị từ chối.", Toast.LENGTH_LONG).show();
            return;
        }

        // Nếu trạng thái là "Đã duyệt" (hoặc tên tiếng khác đã được chuẩn hóa), cần kiểm tra TrangThaiUpdatedAt
        if (st.equals("đã duyệt") || st.equals("da duyet") || st.equals("approved")) {
            // Lấy TrangThaiUpdatedAt từ DB (cột trong NguoiDung)
            SQLiteDatabase db = null;
            Cursor cursor = null;
            try {
                db = dbHelper.getReadableDatabase();
                cursor = db.rawQuery(
                        "SELECT ND.TrangThaiUpdatedAt FROM NguoiDung ND " +
                                "JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan WHERE TK.Email = ?",
                        new String[]{email}
                );

                String ts = null;
                if (cursor != null && cursor.moveToFirst()) {
                    ts = cursor.isNull(0) ? null : cursor.getString(0);
                }

                if (ts != null && !ts.trim().isEmpty()) {
                    // parse và kiểm tra 24h
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(DATETIME_FORMAT, Locale.getDefault());
                        sdf.setLenient(false);
                        Date updatedAt = sdf.parse(ts);
                        if (updatedAt != null) {
                            long diff = System.currentTimeMillis() - updatedAt.getTime();
                            if (diff < MILLIS_24H) {
                                // chưa đủ 24h -> chặn đăng nhập
                                long remainMs = MILLIS_24H - diff;
                                long remainMinutes = remainMs / (60L * 1000L);
                                long hours = remainMinutes / 60L;
                                long minutes = remainMinutes % 60L;
                                String waitMsg = "Tài khoản đang trong thời gian duyệt. Vui lòng chờ khoảng ";
                                if (hours > 0) waitMsg += hours + " giờ ";
                                if (minutes > 0) waitMsg += minutes + " phút";
                                Toast.makeText(this, waitMsg.trim(), Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                    } catch (ParseException e) {
                        // nếu parse lỗi: log/ignore và cho phép đăng nhập (không chặn)
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // nếu có lỗi đọc DB thì không block (tùy bạn muốn strict hay permissive)
            } finally {
                if (cursor != null) cursor.close();
                if (db != null) db.close();
            }
        }

        // Nếu đến đây => cho phép đăng nhập
        User currentUser = dbHelper.getUserByEmail(email);
        if (currentUser == null) {
            Toast.makeText(this, "Không lấy được thông tin người dùng!", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("current_user", currentUser);

        Toast.makeText(this, "Đăng nhập thành công (" + currentUser.getVaiTro() + ")", Toast.LENGTH_SHORT).show();
        startActivity(intent);
        finish();
    }

    private void setActiveTab(boolean isLoginTab) {
        if (isLoginTab) {
            tabLogin.setBackgroundResource(R.drawable.bg_tab_active);
            tabLogin.setTextColor(Color.parseColor("#1E63E9"));

            tabRegister.setBackgroundResource(0);
            tabRegister.setTextColor(Color.parseColor("#A0A0A0"));
        } else {
            tabRegister.setBackgroundResource(R.drawable.bg_tab_active);
            tabRegister.setTextColor(Color.parseColor("#1E63E9"));

            tabLogin.setBackgroundResource(0);
            tabLogin.setTextColor(Color.parseColor("#A0A0A0"));
        }
    }
}
