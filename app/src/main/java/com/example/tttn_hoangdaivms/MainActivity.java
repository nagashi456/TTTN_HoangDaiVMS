package com.example.tttn_hoangdaivms;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.tttn_hoangdaivms.Database.User;
import com.example.tttn_hoangdaivms.DriverList.DriverListFragment;
import com.example.tttn_hoangdaivms.Home.HomeFragement;
import com.example.tttn_hoangdaivms.Request.RequestFragement;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private User currentUser;
    private String userRole = ""; // Vai trò: "Quản lý" hoặc "Tài xế"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Tắt night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Lấy thông tin người dùng từ Intent
        Intent intent = getIntent();
        Bundle extras = (intent != null) ? intent.getExtras() : null;

        if (intent != null) {
            try {
                currentUser = intent.getParcelableExtra("current_user");
            } catch (Exception e) {
                try {
                    Object obj = intent.getSerializableExtra("current_user");
                    if (obj instanceof User) {
                        currentUser = (User) obj;
                    }
                } catch (Exception ignored) {}
            }
        }

        // Nếu currentUser có vai trò thì lưu lại
        if (currentUser != null && currentUser.getVaiTro() != null) {
            userRole = currentUser.getVaiTro();
        } else if (extras != null && extras.containsKey("VaiTro")) {
            // fallback nếu vai trò được truyền qua bundle
            userRole = extras.getString("VaiTro", "");
        }

        // Ẩn menu "Yêu cầu" nếu không phải quản lý
        Menu menu = bottomNavigation.getMenu();
        MenuItem requestItem = menu.findItem(R.id.nav_request);
        if (!"Admin".equalsIgnoreCase(userRole)) {
            requestItem.setVisible(false);
        }

        // Mặc định load HomeFragment khi mở app
        if (savedInstanceState == null) {
            HomeFragement homeFragment = new HomeFragement();
            if (extras != null) {
                homeFragment.setArguments(extras);
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.containerMain, homeFragment)
                    .commit();
        }

        // Xử lý chọn menu
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;

            if (item.getItemId() == R.id.nav_home) {
                HomeFragement home = new HomeFragement();
                if (extras != null) home.setArguments(extras);
                fragment = home;
            } else if (item.getItemId() == R.id.nav_transport) {
                fragment = new DriverListFragment();
            } else if (item.getItemId() == R.id.nav_request) {
                // Chỉ quản lý mới truy cập được
                if ("Admin".equalsIgnoreCase(userRole)) {
                    fragment = new RequestFragement();
                } else {
                    return false;
                }
            }

            if (fragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.containerMain, fragment)
                        .commit();
                return true;
            }
            return false;
        });
    }

    // Nếu cần cập nhật user sau khi login
    public void updateCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) this.userRole = user.getVaiTro();
    }
}
