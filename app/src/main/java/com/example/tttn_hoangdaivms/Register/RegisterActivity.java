package com.example.tttn_hoangdaivms.Register;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtName, edtEmail, edtGender, edtBirthday, edtPhone, edtCCCD, edtPassword, edtConfirmPassword;
    private Button btnRequest;
    private TextView tabRegister, tabLogin;
    private Database dbHelper;

    // Regex patterns
    // Name: only letters and spaces (allow unicode letters), min 2 chars
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L} ]{2,}$");
    // Phone: exactly 10 digits
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10}$");
    // CCCD: exactly 12 digits
    private static final Pattern CCCD_PATTERN = Pattern.compile("^\\d{12}$");
    // Password: min 8 chars, at least one digit, one lower, one upper, one special (@#$%&)
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%&]).{8,}$");

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

        // Clear errors while typing
        TextWatcher clearErrorWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                // remove error view and tint for the field that changed
                View focused = getCurrentFocus();
                if (focused instanceof EditText) {
                    removeFieldError((EditText) focused);
                }
            }
        };
        edtName.addTextChangedListener(clearErrorWatcher);
        edtEmail.addTextChangedListener(clearErrorWatcher);
        edtGender.addTextChangedListener(clearErrorWatcher);
        edtBirthday.addTextChangedListener(clearErrorWatcher);
        edtPhone.addTextChangedListener(clearErrorWatcher);
        edtCCCD.addTextChangedListener(clearErrorWatcher);
        edtPassword.addTextChangedListener(clearErrorWatcher);
        edtConfirmPassword.addTextChangedListener(clearErrorWatcher);

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
                (DatePicker view, int y, int m, int d) -> {
                    // format dd/MM/yyyy
                    String s = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y);
                    edtBirthday.setText(s);
                },
                year, month, day
        );
        dialog.show();
    }

    /**
     * Hiển thị lỗi ngay bên dưới editText:
     * - tạo TextView lỗi nếu chưa có (tag = "error_"+editTextId)
     * - set background drawable lỗi cho editText
     */
    private void setFieldError(EditText editText, String message) {
        // set tint đỏ cho EditText (nếu bạn có bg_edittext_error drawable, dùng drawable; nếu không, dùng tint)
        try {
            editText.setBackgroundResource(R.drawable.bg_edittext_error);
        } catch (Exception e) {
            // nếu drawable không tồn tại, dùng tint
            editText.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
        }

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
        // reset tint/background
        try {
            editText.setBackgroundResource(R.drawable.bg_edittext_white);
        } catch (Exception e) {
            editText.setBackgroundTintList(null);
        }

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
        String email = edtEmail.getText() != null ? edtEmail.getText().toString().trim().toLowerCase(Locale.getDefault()) : "";
        String gender = edtGender.getText() != null ? edtGender.getText().toString().trim() : "";
        String birthday = edtBirthday.getText() != null ? edtBirthday.getText().toString().trim() : "";
        String phone = edtPhone.getText() != null ? edtPhone.getText().toString().trim() : "";
        String cccd = edtCCCD.getText() != null ? edtCCCD.getText().toString().trim() : "";
        String pass = edtPassword.getText() != null ? edtPassword.getText().toString() : "";
        String confirm = edtConfirmPassword.getText() != null ? edtConfirmPassword.getText().toString() : "";

        boolean hasError = false;

        // Tên: không để trống
        if (TextUtils.isEmpty(name)) {
            setFieldError(edtName, "Vui lòng điền họ tên");
            hasError = true;
        } else if (!NAME_PATTERN.matcher(name).matches()) {
            setFieldError(edtName, "Họ tên không hợp lệ");
            hasError = true;
        }

        // Email: format hợp lệ
        if (TextUtils.isEmpty(email)) {
            setFieldError(edtEmail, "Vui lòng nhập email");
            hasError = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setFieldError(edtEmail, "Email không hợp lệ");
            hasError = true;
        }

        // Gender: Nam/Nữ
        if (TextUtils.isEmpty(gender)) {
            setFieldError(edtGender, "Vui lòng chọn giới tính (Nam/Nữ)");
            hasError = true;
        } else {
            String low = gender.toLowerCase(Locale.getDefault());
            if (!(low.equals("nam") || low.equals("nữ") || low.equals("nu"))) {
                setFieldError(edtGender, "Vui lòng chọn giới tính (Nam/Nữ)");
                hasError = true;
            }
        }

        // Birthday: phải là ngày quá khứ (format dd/MM/yyyy)
        if (TextUtils.isEmpty(birthday)) {
            setFieldError(edtBirthday, "Vui lòng chọn ngày sinh");
            hasError = true;
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            sdf.setLenient(false);
            try {
                Date bd = sdf.parse(birthday);
                if (bd == null) {
                    setFieldError(edtBirthday, "Ngày sinh không hợp lệ");
                    hasError = true;
                } else {
                    Date today = removeTime(new Date());
                    if (!bd.before(today)) {
                        // không phải ngày quá khứ
                        setFieldError(edtBirthday, "Ngày sinh phải là ngày trong quá khứ");
                        hasError = true;
                    }
                }
            } catch (ParseException e) {
                setFieldError(edtBirthday, "Ngày sinh không hợp lệ");
                hasError = true;
            }
        }

        // Phone: đúng 10 chữ số
        if (TextUtils.isEmpty(phone)) {
            setFieldError(edtPhone, "Vui lòng nhập số điện thoại");
            hasError = true;
        } else if (!PHONE_PATTERN.matcher(phone).matches()) {
            setFieldError(edtPhone, "Số điện thoại không hợp lệ (10 chữ số)");
            hasError = true;
        }

        // CCCD: 12 chữ số
        if (TextUtils.isEmpty(cccd)) {
            setFieldError(edtCCCD, "Vui lòng nhập CCCD");
            hasError = true;
        } else if (!CCCD_PATTERN.matcher(cccd).matches()) {
            setFieldError(edtCCCD, "CCCD phải gồm 12 chữ số");
            hasError = true;
        }

        // Password rules
        if (TextUtils.isEmpty(pass)) {
            setFieldError(edtPassword, "Vui lòng nhập mật khẩu");
            hasError = true;
        } else if (!PASSWORD_PATTERN.matcher(pass).matches()) {
            setFieldError(edtPassword, "Mật khẩu tối thiểu 8 ký tự bao gồm số (0-9), chữ thường (a-z), chữ in hoa (A-Z), ký tự đặc biệt (@#$%&)");
            hasError = true;
        }

        // Confirm password
        if (TextUtils.isEmpty(confirm)) {
            setFieldError(edtConfirmPassword, "Vui lòng xác nhận mật khẩu");
            hasError = true;
        } else if (!pass.equals(confirm)) {
            setFieldError(edtConfirmPassword, "Xác nhận mật khẩu không khớp");
            hasError = true;
        }

        if (hasError) {
            Toast.makeText(this, "Vui lòng kiểm tra các trường màu đỏ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra email đã tồn tại trước khi insert
        if (dbHelper.getUserByEmail(email) != null) {
            setFieldError(edtEmail, "Email đã tồn tại, vui lòng dùng email khác");
            Toast.makeText(this, "Email đã tồn tại, vui lòng dùng email khác", Toast.LENGTH_LONG).show();
            return;
        }

        // --- Thêm người dùng mới vào database ---
        String trangThai = "Đang yêu cầu";
        String vaiTro = "Nhân viên";

        boolean success = dbHelper.insertNguoiDung(email, pass, name, birthday, gender, cccd, phone, vaiTro, trangThai);
        if (success) {
            Toast.makeText(this, "Yêu cầu đăng ký đã được gửi đến quản lý.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            // nếu insert thất bại mà không phải do email tồn tại (thường là do lỗi DB)
            setFieldError(edtEmail, "Email đã tồn tại, vui lòng dùng email khác");
            Toast.makeText(this, "Đăng ký thất bại", Toast.LENGTH_SHORT).show();
        }
    }

    // helper: remove time component from Date
    private Date removeTime(Date d) {
        Calendar c = Calendar.getInstance(Locale.getDefault());
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
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
