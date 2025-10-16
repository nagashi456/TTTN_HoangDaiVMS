package com.example.tttn_hoangdaivms;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tttn_hoangdaivms.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private TextView tvUserName, tvVehicleCount, tvDriverCount;
    private MaterialButton btnLogout;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupData();
        setupListeners();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvVehicleCount = findViewById(R.id.tvVehicleCount);
        tvDriverCount = findViewById(R.id.tvDriverCount);
        btnLogout = findViewById(R.id.btnLogout);
        bottomNavigation = findViewById(R.id.bottomNavigation);

    }



    private void setupData() {
        // Set user data
        tvUserName.setText("Nguyễn Văn A");
        tvVehicleCount.setText("30");
        tvDriverCount.setText("17");
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(v -> {
            Toast.makeText(this, "Đăng xuất", Toast.LENGTH_SHORT).show();
            // Add logout logic here
        });

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Toast.makeText(this, "Trang chủ", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_transport) {
                Toast.makeText(this, "Vận tải", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_request) {
                Toast.makeText(this, "Yêu cầu", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        findViewById(R.id.btnEdit).setOnClickListener(v -> {
            Toast.makeText(this, "Chỉnh sửa", Toast.LENGTH_SHORT).show();
        });
    }
}