package com.example.tttn_hoangdaivms.Login;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
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

    // trạng thái hiển thị mật khẩu
    private boolean isPasswordVisible = false;

    // định dạng dùng cho TrangThaiUpdatedAt
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    // khoảng 24 giờ tính bằng milliseconds
    private static final long MILLIS_24H = 24L * 60L * 60L * 1000L;

    // SharedPreferences để lưu attempt count tạm thời
    private static final String PREFS_NAME = "login_prefs";
    private static final int MAX_FAILED_ATTEMPTS = 5;

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

        // --- NEW: xử lý nhấn vào icon cuối EditText để toggle show/hide password ---
        // lưu ý: layout của bạn đã có android:drawableEnd="@drawable/ic_eye_off"
        edtPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                // getCompoundDrawablesRelative(): [start, top, end, bottom]
                Drawable drawableEnd = edtPassword.getCompoundDrawablesRelative()[2];
                if (drawableEnd != null) {
                    // tính vùng chạm (dựa trên paddingEnd + width drawable)
                    float touchX = event.getX();
                    int width = edtPassword.getWidth();
                    int paddingEnd = edtPassword.getPaddingEnd();
                    int drawableWidth = drawableEnd.getBounds().width();

                    if (touchX >= (width - paddingEnd - drawableWidth)) {
                        togglePasswordVisibility();
                        // consume event
                        return true;
                    }
                }
            }
            return false;
        });

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

    /**
     * Toggle show/hide password and swap icon.
     */
    private void togglePasswordVisibility() {
        int selection = edtPassword.getSelectionStart();
        if (isPasswordVisible) {
            // hiện tại đang hiển thị -> ẩn
            edtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            // đổi icon về "eye off"
            edtPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_eye_off, 0);
            // đặt inputType tương ứng
            edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            isPasswordVisible = false;
        } else {
            // hiện tại đang ẩn -> hiển thị
            edtPassword.setTransformationMethod(null);
            // đổi icon sang "eye on"
            edtPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_eye_on, 0);
            // đặt inputType visible password
            edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            isPasswordVisible = true;
        }
        // Giữ con trỏ ở đúng vị trí
        if (selection >= 0) {
            edtPassword.setSelection(selection);
        } else {
            edtPassword.setSelection(edtPassword.getText().length());
        }
    }

    private void handleLogin() {
        String email = edtEmail.getText().toString().trim().toLowerCase(Locale.getDefault());
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Vui lòng nhập email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Vui lòng nhập mật khẩu");
            return;
        }

        // Trước hết: kiểm tra trạng thái tài khoản (nếu có) - nếu đã khóa thì không tăng counter, chỉ thông báo
        String currentStatus = dbHelper.getUserStatus(email);
        if (currentStatus != null) {
            String stCheck = currentStatus.trim().toLowerCase();
            if (stCheck.equals("đã khóa") || stCheck.equals("da khoa") || stCheck.equals("locked")) {
                Toast.makeText(this, "Tài khoản đã bị khóa. Liên hệ quản trị để mở.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // 1) Kiểm tra credentials (email + password)
        boolean credentialsOk = dbHelper.validateCredentials(email, password);
        if (!credentialsOk) {
            // tăng counter và kiểm tra khóa (chỉ tăng nếu tài khoản hiện chưa bị khóa)
            int attempts = incrementFailedAttempts(email);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                // Khóa tài khoản trong DB và cập nhật TrangThaiUpdatedAt
                boolean locked = dbHelper.setUserStatus(email, "Đã khóa");
                if (locked) {
                    Toast.makeText(this, "Tài khoản đã bị khóa, vui lòng liên hệ quản lý", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Tài khoản bị khóa do đăng nhập sai nhiều lần", Toast.LENGTH_LONG).show();
                }
            } else {
                int remaining = MAX_FAILED_ATTEMPTS - attempts;
                Toast.makeText(this, "Sai thông tin đăng nhập! Còn " + remaining + " lần thử trước khi khóa.", Toast.LENGTH_LONG).show();
            }
            return;
        }

        // Nếu credentials ok, trước khi cho phép đăng nhập cần kiểm tra trạng thái trong DB
        String status = dbHelper.getUserStatus(email);
        if (status == null) {
            // Không tìm thấy hồ sơ người dùng (cấu trúc DB không nhất quán)
            Toast.makeText(this, "Tài khoản chưa có hồ sơ, liên hệ quản trị.", Toast.LENGTH_LONG).show();
            return;
        }

        // Chuẩn hoá status để so sánh (bỏ khoảng trắng, không phân biệt hoa thường)
        String st = status.trim().toLowerCase();

        // Nếu đã bị khóa thì chặn
        if (st.equals("đã khóa") || st.equals("da khoa") || st.equals("locked")) {
            Toast.makeText(this, "Tài khoản đã bị khóa. Liên hệ quản trị để mở.", Toast.LENGTH_LONG).show();
            return;
        }

        if (st.equals("đang yêu cầu") || st.equals("dang yeu cau") || st.equals("pending")) {
            Toast.makeText(this, "Tài khoản đang được duyệt. Vui lòng chờ.", Toast.LENGTH_LONG).show();
            return;
        }

        if (st.equals("đã từ chối") || st.equals("da tu choi") || st.equals("rejected") || st.equals("tu choi")) {
            Toast.makeText(this, "Tài khoản đã bị từ chối.", Toast.LENGTH_LONG).show();
            return;
        }

        // Nếu trạng thái là "Đã duyệt" (hoặc tên tiếng khác đã được chuẩn hóa), cần kiểm tra TrangThaiUpdatedAt (24h)
        if (st.equals("đã duyệt") || st.equals("da duyet") || st.equals("approved")) {
            // Lấy TrangThaiUpdatedAt từ DB (cột trong NguoiDung)
            SQLiteDatabase dbRead = null;
            Cursor cursor = null;
            try {
                dbRead = dbHelper.getReadableDatabase();
                cursor = dbRead.rawQuery(
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
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) cursor.close();
                if (dbRead != null) dbRead.close();
            }
        }

        // Nếu đến đây => cho phép đăng nhập
        // reset attempt counter vì login thành công
        resetFailedAttempts(email);

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

    // helpers for SharedPreferences attempt counting
    private int getFailedAttempts(String email) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt("failed_" + email, 0);
    }

    private int incrementFailedAttempts(String email) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int cur = prefs.getInt("failed_" + email, 0);
        cur++;
        prefs.edit().putInt("failed_" + email, cur).apply();
        return cur;
    }

    private void resetFailedAttempts(String email) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().remove("failed_" + email).apply();
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
