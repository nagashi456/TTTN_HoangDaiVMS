package com.example.tttn_hoangdaivms.Login;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
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

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnLogin;
    private TextView tabLogin, tabRegister;
    private Database dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        if (st.equals("đang duyệt") || st.equals("dang duyet") || st.equals("pending")) {
            Toast.makeText(this, "Tài khoản đang được duyệt. Vui lòng chờ.", Toast.LENGTH_LONG).show();
            return;
        }

        if (st.equals("đã từ chối") || st.equals("da tu choi") || st.equals("rejected") || st.equals("tu choi")) {
            Toast.makeText(this, "Tài khoản đã bị từ chối.", Toast.LENGTH_LONG).show();
            return;
        }

        // Nếu đến đây => trạng thái là Đã duyệt (hoặc khác cho phép)
        String role = dbHelper.getUserRole(email);
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
