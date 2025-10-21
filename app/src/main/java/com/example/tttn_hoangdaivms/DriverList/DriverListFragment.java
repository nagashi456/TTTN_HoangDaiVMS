package com.example.tttn_hoangdaivms.DriverList;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tttn_hoangdaivms.Database.Database;
import com.example.tttn_hoangdaivms.R;
import com.example.tttn_hoangdaivms.AddVehicle.AddVehicleFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class DriverListFragment extends Fragment {

    private RecyclerView driverRecyclerView;
    private DriverListAdapter driverAdapter;
    private VehicleListAdapter vehicleAdapter;

    private List<DriverListModel> driverList;
    private List<VehicleListModel> vehicleList;
    private FloatingActionButton fabAdd;
    private TextView btnDriver, btnVehicle;

    private Database dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.driver_list, container, false);

        btnDriver = view.findViewById(R.id.btnDriver);
        btnVehicle = view.findViewById(R.id.btnVehicle);
        driverRecyclerView = view.findViewById(R.id.driverRecyclerView);
        fabAdd = view.findViewById(R.id.fabAdd);

        driverRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        dbHelper = new Database(requireContext());

        // Tải dữ liệu thực tế từ SQLite
        driverList = loadDriversFromDatabase();
        vehicleList = loadVehiclesFromDatabase();

        driverAdapter = new DriverListAdapter(driverList, driver ->
                Toast.makeText(requireContext(), "Tài xế: " + driver.getName(), Toast.LENGTH_SHORT).show()
        );

        vehicleAdapter = new VehicleListAdapter(vehicleList, vehicle ->
                Toast.makeText(requireContext(), "Xe: " + vehicle.getPlateNumber(), Toast.LENGTH_SHORT).show()
        );

        // Mặc định hiển thị danh sách tài xế
        driverRecyclerView.setAdapter(driverAdapter);
        setSelectedSegment(true);
        fabAdd.setVisibility(View.GONE);

        btnDriver.setOnClickListener(v -> {
            setSelectedSegment(true);
            driverRecyclerView.setAdapter(driverAdapter);
            fabAdd.setVisibility(View.GONE);
        });

        btnVehicle.setOnClickListener(v -> {
            setSelectedSegment(false);
            driverRecyclerView.setAdapter(vehicleAdapter);
            fabAdd.setVisibility(View.VISIBLE);
        });

        fabAdd.setOnClickListener(v -> {
            AddVehicleFragment addFragment = new AddVehicleFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.containerMain, addFragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void setSelectedSegment(boolean isDriverSelected) {
        if (isDriverSelected) {
            btnDriver.setBackgroundResource(R.drawable.bg_segment_selected);
            btnDriver.setTextColor(Color.WHITE);
            btnVehicle.setBackgroundResource(R.drawable.bg_segment_unselected);
            btnVehicle.setTextColor(Color.parseColor("#666666"));
        } else {
            btnVehicle.setBackgroundResource(R.drawable.bg_segment_selected);
            btnVehicle.setTextColor(Color.WHITE);
            btnDriver.setBackgroundResource(R.drawable.bg_segment_unselected);
            btnDriver.setTextColor(Color.parseColor("#666666"));
        }
    }

    // ===============================
    // 🔹 TRUY VẤN DANH SÁCH TÀI XẾ
    // ===============================
    private List<DriverListModel> loadDriversFromDatabase() {
        List<DriverListModel> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT HoTen, NgaySinh FROM NguoiDung WHERE lower(COALESCE(VaiTro, '')) LIKE ? OR lower(COALESCE(VaiTro, '')) LIKE ?",
                new String[]{"%nhân viên%"}
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                String address = cursor.getString(1);
                list.add(new DriverListModel(name, address != null ? address : "Chưa cập nhật", R.drawable.avatar1));
            } while (cursor.moveToNext());
            cursor.close();
        }

        return list;
    }

    // ===============================
    // 🔹 TRUY VẤN DANH SÁCH XE
    // ===============================
    private List<VehicleListModel> loadVehiclesFromDatabase() {
        List<VehicleListModel> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT BienSo, LoaiXe FROM Xe",
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String plate = cursor.getString(0);
                String name = cursor.getString(1);
                list.add(new VehicleListModel(plate, name != null ? name : "Không rõ", R.drawable.avatar1));
            } while (cursor.moveToNext());
            cursor.close();
        }

        return list;
    }
}
