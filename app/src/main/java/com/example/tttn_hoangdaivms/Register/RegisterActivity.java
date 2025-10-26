package com.example.tttn_hoangdaivms.Register;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tttn_hoangdaivms.Database.Database;
import com.example.tttn_hoangdaivms.Login.LoginActivity;
import com.example.tttn_hoangdaivms.R;

import java.util.Calendar;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtName, edtEmail, edtGender, edtBirthday, edtPhone, edtCCCD, edtPassword, edtConfirmPassword;
    private Button btnRequest;
    private TextView tabRegister, tabLogin;
    private Database dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        // Ánh xạ
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtGender = findViewById(R.id.edtGender);
        edtBirthday = findViewById(R.id.edtBirthday);
        edtPhone = findViewById(R.id.edtPhone);
        edtCCCD = findViewById(R.id.edtCCCD);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRequest = findViewById(R.id.btnRequest);
        tabRegister = findViewById(R.id.tabRegister);
        tabLogin = findViewById(R.id.tabLogin);

        dbHelper = new Database(this);

        // Mở lịch chọn ngày sinh
        edtBirthday.setOnClickListener(v -> showDatePicker());

        // Gửi yêu cầu đăng ký
        btnRequest.setOnClickListener(v -> handleRegister());

        tabLogin.setOnClickListener(v -> {
            setActiveTab(false);
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        });

        tabRegister.setOnClickListener(v -> setActiveTab(true));
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (DatePicker view, int y, int m, int d) -> edtBirthday.setText(d + "/" + (m + 1) + "/" + y),
                year, month, day
        );
        dialog.show();
    }

    /**
     * Hiển thị lỗi ngay bên dưới editText:
     * - tạo TextView lỗi nếu chưa có (tag = "error_"+editTextId)
     * - set background tint đỏ cho editText
     */
    private void setFieldError(EditText editText, String message) {
        // set tint đỏ cho EditText (API 21+)
        editText.setBackgroundResource(R.drawable.bg_edittext_error);

        // Lấy parent container (mình dùng LinearLayout theo XML)
        ViewGroup parent = (ViewGroup) editText.getParent();
        if (parent == null) return;

        String tag = "error_" + editText.getId();
        View existing = parent.findViewWithTag(tag);
        if (existing instanceof TextView) {
            ((TextView) existing).setText(message);
            existing.setVisibility(View.VISIBLE);
            return;
        }

        // Nếu chưa có, tạo TextView mới và chèn ngay dưới editText
        TextView tvError = new TextView(this);
        tvError.setTag(tag);
        tvError.setText(message);
        tvError.setTextColor(Color.parseColor("#D32F2F"));
        tvError.setTextSize(12);
        // margin top nhỏ
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(8, 8, 0, 0);

        int insertIndex = parent.indexOfChild(editText) + 1;
        parent.addView(tvError, insertIndex, lp);
    }

    private void removeFieldError(EditText editText) {
        // reset tint
        editText.setBackgroundTintList(null);

        ViewGroup parent = (ViewGroup) editText.getParent();
        if (parent == null) return;

        String tag = "error_" + editText.getId();
        View existing = parent.findViewWithTag(tag);
        if (existing != null) {
            parent.removeView(existing);
        }
    }

    private void clearAllErrors() {
        removeFieldError(edtName);
        removeFieldError(edtEmail);
        removeFieldError(edtGender);
        removeFieldError(edtBirthday);
        removeFieldError(edtPhone);
        removeFieldError(edtCCCD);
        removeFieldError(edtPassword);
        removeFieldError(edtConfirmPassword);
    }

    private void handleRegister() {
        // xóa các lỗi cũ trước khi validate
        clearAllErrors();

        String name = edtName.getText() != null ? edtName.getText().toString().trim() : "";
        String email = edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";
        String gender = edtGender.getText() != null ? edtGender.getText().toString().trim() : "";
        String birthday = edtBirthday.getText() != null ? edtBirthday.getText().toString().trim() : "";
        String phone = edtPhone.getText() != null ? edtPhone.getText().toString().trim() : "";
        String cccd = edtCCCD.getText() != null ? edtCCCD.getText().toString().trim() : "";
        String pass = edtPassword.getText() != null ? edtPassword.getText().toString().trim() : "";
        String confirm = edtConfirmPassword.getText() != null ? edtConfirmPassword.getText().toString().trim() : "";

        boolean hasError = false;

        // Kiểm tra từng trường, đánh dấu lỗi nếu có (không return ngay)
        if (TextUtils.isEmpty(name)) {
            setFieldError(edtName, "Vui lòng nhập họ và tên");
            hasError = true;
        }

        if (TextUtils.isEmpty(email)) {
            setFieldError(edtEmail, "Vui lòng nhập email");
            hasError = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setFieldError(edtEmail, "Email không hợp lệ");
            hasError = true;
        }

        // Giới tính: nếu bạn dùng EditText, bắt buộc nhập Nam/Nữ; khuyến nghị dùng RadioGroup/Spinner
        if (TextUtils.isEmpty(gender)) {
            setFieldError(edtGender, "Vui lòng chọn giới tính (Nam/Nữ)");
            hasError = true;
        } else {
            String lower = gender.toLowerCase();
            if (!(lower.equals("nam") || lower.equals("nữ") || lower.equals("nu"))) {
                setFieldError(edtGender, "Chỉ chấp nhận 'Nam' hoặc 'Nữ'");
                hasError = true;
            }
        }

        if (TextUtils.isEmpty(birthday)) {
            setFieldError(edtBirthday, "Vui lòng chọn ngày sinh");
            hasError = true;
        }

        if (TextUtils.isEmpty(phone)) {
            setFieldError(edtPhone, "Vui lòng nhập số điện thoại");
            hasError = true;
        } else if (!phone.matches("\\d{10,11}")) {
            setFieldError(edtPhone, "Số điện thoại không hợp lệ (10-11 chữ số)");
            hasError = true;
        }

        if (TextUtils.isEmpty(cccd)) {
            setFieldError(edtCCCD, "Vui lòng nhập CCCD");
            hasError = true;
        } else if (!cccd.matches("\\d{12}")) {
            setFieldError(edtCCCD, "CCCD phải gồm 12 chữ số");
            hasError = true;
        }

        if (TextUtils.isEmpty(pass)) {
            setFieldError(edtPassword, "Vui lòng nhập mật khẩu");
            hasError = true;
        }

        if (TextUtils.isEmpty(confirm)) {
            setFieldError(edtConfirmPassword, "Vui lòng xác nhận mật khẩu");
            hasError = true;
        } else if (!pass.equals(confirm)) {
            setFieldError(edtConfirmPassword, "Mật khẩu xác nhận không khớp");
            hasError = true;
        }

        if (hasError) {
            // cuộn lên đầu form hoặc để người dùng thấy lỗi tự xử lý (tuỳ UI)
            Toast.makeText(this, "Vui lòng kiểm tra các trường màu đỏ", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Thêm người dùng mới vào database ---
        String trangThai = "Đang yêu cầu";
        String vaiTro = "Nhân viên";

        boolean success = dbHelper.insertNguoiDung(email, pass, name, birthday, gender, cccd, phone, vaiTro, trangThai);
        if (success) {
            Toast.makeText(this, "Đăng ký thành công, vui lòng chờ duyệt!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            // email đã tồn tại -> hiển thị lỗi bên dưới email
            setFieldError(edtEmail, "Email đã tồn tại hoặc lỗi khi thêm dữ liệu");
            Toast.makeText(this, "Đăng ký thất bại", Toast.LENGTH_SHORT).show();
        }
    }

    private void setActiveTab(boolean isLoginTab) {
        if (isLoginTab) {
            tabRegister.setBackgroundResource(R.drawable.bg_tab_active);
            tabRegister.setTextColor(Color.parseColor("#1E63E9"));

            tabLogin.setBackgroundResource(0);
            tabLogin.setTextColor(Color.parseColor("#A0A0A0"));
        } else {
            tabLogin.setBackgroundResource(R.drawable.bg_tab_active);
            tabLogin.setTextColor(Color.parseColor("#1E63E9"));

            tabRegister.setBackgroundResource(0);
            tabRegister.setTextColor(Color.parseColor("#A0A0A0"));
        }
    }
}
