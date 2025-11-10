package com.example.tttn_hoangdaivms;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.tttn_hoangdaivms.Map.VehicleMapFragment;
import com.example.tttn_hoangdaivms.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.tttn_hoangdaivms.Database.Database;
import com.example.tttn_hoangdaivms.Database.User;
import com.example.tttn_hoangdaivms.DriverList.DriverListFragment;
import com.example.tttn_hoangdaivms.Home.HomeFragement;
import com.example.tttn_hoangdaivms.Report.ReportListFragment;
import com.example.tttn_hoangdaivms.Request.RequestFragement;
import com.example.tttn_hoangdaivms.VehicleDetail.VehicleDetailFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private User currentUser;
    private String userRole = ""; // Vai trò: "Admin" hoặc "Tài xế"...
    private Database dbHelper; // <-- thêm Database helper

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Tắt night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottomNavigation);
        dbHelper = new Database(this); // khởi tạo db helper

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
        MenuItem tripItem = menu.findItem(R.id.nav_trip);
        MenuItem reportItem = menu.findItem(R.id.nav_report);
        if (!"Admin".equalsIgnoreCase(userRole)) {
            requestItem.setVisible(false);
            tripItem.setVisible(false);
            reportItem.setVisible(false);
        }

        // Mặc định load HomeFragment khi mở app
        if (savedInstanceState == null) {
            HomeFragement homeFragment = new HomeFragement();
            if (extras != null) homeFragment.setArguments(extras);
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
                // Nếu là Admin -> mở DriverList (giao diện quản lý danh sách xe/tài xế)
                if ("Admin".equalsIgnoreCase(userRole)) {
                    fragment = new DriverListFragment();
                } else {
                    // Nếu không phải Admin -> mở VehicleDetail liên kết với user hiện tại (nếu có xe)
                    // 1) tìm MaNguoiDung từ currentUser (qua email) -> dùng Database.getUserIdByEmail(email)
                    String email = null;
                    try {
                        if (currentUser != null) email = currentUser.getEmail();
                    } catch (Exception ignored) {}

                    if ((email == null || email.isEmpty()) && extras != null) {
                        // fallback: thử read email hoặc MaNguoiDung từ extras
                        if (extras.containsKey("Email")) email = extras.getString("Email", null);
                    }

                    String maNguoiDungStr = null;
                    if (email != null && !email.isEmpty()) {
                        maNguoiDungStr = dbHelper.getUserIdByEmail(email); // trả String hoặc null
                    } else if (extras != null && extras.containsKey("MaNguoiDung")) {
                        maNguoiDungStr = extras.getString("MaNguoiDung", null);
                    }

                    if (maNguoiDungStr == null || maNguoiDungStr.isEmpty()) {
                        // Không tìm được MaNguoiDung — hiển thị thông báo hoặc chuyển sang màn NoVehicle
                        fragment = new com.example.tttn_hoangdaivms.VehicleDetail.NoVehicleFragment();
                    } else {
                        // truy vấn Xe theo MaNguoiDung (lấy 1 chiếc nếu có)
                        int maNguoiDung = -1;
                        try { maNguoiDung = Integer.parseInt(maNguoiDungStr); } catch (Exception ignored) {}

                        if (maNguoiDung == -1) {
                            fragment = new com.example.tttn_hoangdaivms.VehicleDetail.NoVehicleFragment();
                        } else {
                            SQLiteDatabase db = dbHelper.getReadableDatabase();
                            Cursor c = null;
                            try {
                                c = db.rawQuery("SELECT MaXe FROM Xe WHERE MaNguoiDung = ? LIMIT 1",
                                        new String[]{String.valueOf(maNguoiDung)});
                                if (c != null && c.moveToFirst()) {
                                    int maXe = c.getInt(0);
                                    fragment = VehicleDetailFragment.newInstance(maXe);
                                } else {
                                    // chưa có xe liên kết
                                    fragment = new com.example.tttn_hoangdaivms.VehicleDetail.NoVehicleFragment();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                fragment = new com.example.tttn_hoangdaivms.VehicleDetail.NoVehicleFragment();
                            } finally {
                                if (c != null) c.close();
                            }
                        }
                    }
                }

            } else if (item.getItemId() == R.id.nav_request) {
                // Chỉ quản lý mới truy cập được
                if ("Admin".equalsIgnoreCase(userRole)) {
                    fragment = new RequestFragement();
                } else {
                    return false;
                }
            } else if (item.getItemId() == R.id.nav_trip) {
                if ("Admin".equalsIgnoreCase(userRole)) {
                    fragment = new VehicleMapFragment();
                } else {
                    return false;
                }
            } else if (item.getItemId() == R.id.nav_report) {
                if ("Admin".equalsIgnoreCase(userRole)) {
                    fragment = new ReportListFragment();
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
        if (user != null && user.getVaiTro() != null) this.userRole = user.getVaiTro();
    }
}
