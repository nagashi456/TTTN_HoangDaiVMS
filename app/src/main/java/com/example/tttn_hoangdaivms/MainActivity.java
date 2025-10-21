package com.example.tttn_hoangdaivms;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.tttn_hoangdaivms.DriverList.DriverListFragment;
import com.example.tttn_hoangdaivms.Home.HomeFragement;
import com.example.tttn_hoangdaivms.Database.User;   // giữ import nếu bạn dùng kiểu này
import com.example.tttn_hoangdaivms.Request.RequestFragement;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private User currentUser; // vẫn giữ nếu bạn muốn dùng ở Activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Tắt night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Lấy intent + extras (nếu LoginActivity đã putExtra gì vào intent, ta giữ nguyên bundle đó)
        Intent intent = getIntent();
        Bundle extras = (intent != null) ? intent.getExtras() : null;

        // Nếu bạn vẫn muốn tham chiếu object currentUser trong Activity (tuỳ bạn LoginActivity đã putParcelable hay putSerializable)
        // Thử lấy Parcelable trước (nếu có)
        if (intent != null) {
            try {
                currentUser = intent.getParcelableExtra("current_user");
            } catch (Exception e) {
                // nếu không phải Parcelable thì bỏ qua (nếu User là Serializable, bạn có thể lấy bằng getSerializable)
                try {
                    Object obj = intent.getSerializableExtra("current_user");
                    if (obj instanceof User) {
                        currentUser = (User) obj;
                    }
                } catch (Exception ignored) {}
            }
        }

        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Mặc định load HomeFragment khi mở app
        if (savedInstanceState == null) {
            // Tạo fragment bằng constructor mặc định, setArguments bằng bundle extras (nếu có)
            HomeFragement homeFragment = new HomeFragement();
            if (extras != null) {
                homeFragment.setArguments(extras);
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.containerMain, homeFragment)
                    .commit();
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;

            if (item.getItemId() == R.id.nav_home) {
                HomeFragement home = new HomeFragement();
                if (extras != null) {
                    // truyền tiếp cùng bundle (giữ thông tin đăng nhập)
                    home.setArguments(extras);
                }
                fragment = home;
            } else if (item.getItemId() == R.id.nav_transport) {
                fragment = new DriverListFragment();
            } else if (item.getItemId() == R.id.nav_request) {
                fragment = new RequestFragement();
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

    // Nếu cần cập nhật user sau này (ví dụ gọi khi login xong trong app), bạn có thể gọi method này
    public void updateCurrentUser(User user) {
        this.currentUser = user;
    }
}
