package com.example.tttn_hoangdaivms.DriverList;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tttn_hoangdaivms.Database.Database;
import com.example.tttn_hoangdaivms.DriverDetail.DriverDetail;
import com.example.tttn_hoangdaivms.R;
import com.example.tttn_hoangdaivms.AddVehicle.AddVehicleFragment;
import com.example.tttn_hoangdaivms.VehicleDetail.VehicleDetailFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class DriverListFragment extends Fragment {
    private static final String TAG = "DriverListFragment";

    private RecyclerView driverRecyclerView;
    private DriverListAdapter driverAdapter;
    private VehicleListAdapter vehicleAdapter;

    private List<DriverListModel> driverList;
    private List<String> driverIds; // MaNguoiDung dưới dạng String, song song với driverList
    private List<VehicleListModel> vehicleList;
    private FloatingActionButton fabAdd;
    private TextView btnDriver, btnVehicle;

    private Database dbHelper;

    // --- cho search ---
    private EditText searchEditText;
    private List<DriverListModel> driverListFull;
    private List<String> driverIdsFull;
    private List<VehicleListModel> vehicleListFull;
    private boolean isDriverSelected = true; // track selected segment

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
        searchEditText = view.findViewById(R.id.searchEditText); // ô search

        driverRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        dbHelper = new Database(requireContext());

        // Khởi tạo danh sách
        driverList = new ArrayList<>();
        driverIds = new ArrayList<>();
        vehicleList = new ArrayList<>();

        // Load dữ liệu từ DB
        loadDriversFromDatabase();   // sẽ điền driverList & driverIds
        vehicleList = loadVehiclesFromDatabase(); // trả VehicleListModel có MaXe

        // lưu bản đầy đủ để restore/filter
        driverListFull = new ArrayList<>(driverList);
        driverIdsFull = new ArrayList<>(driverIds);
        vehicleListFull = new ArrayList<>(vehicleList);

        // ===== Driver adapter (giữ nguyên logic hiện có)
        driverAdapter = new DriverListAdapter(requireContext(), driverList, driverIds, new DriverListAdapter.OnDriverActionListener() {
            @Override
            public void onDriverClick(DriverListModel driver, int position, String id) {
                if (position == -1 || id == null) {
                    int pos = findIndexByNameAndLocation(driver.getName(), driver.getLocation());
                    if (pos >= 0 && pos < driverIds.size()) id = driverIds.get(pos);
                    position = pos;
                }
                if (id != null) {
                    DriverDetail frag = DriverDetail.newInstance(id);
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.containerMain, frag)
                            .addToBackStack(null)
                            .commit();
                } else {
                    Toast.makeText(requireContext(), "Không tìm thấy ID chi tiết.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onEditRequested(DriverListModel driver, int position, String id) {
                Toast.makeText(requireContext(), "Chức năng sửa tạm thời chưa có.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteRequested(DriverListModel driver, int position, String id) {
                if (id == null || id.trim().isEmpty()) {
                    Toast.makeText(requireContext(), "Không xác định được ID để xóa.", Toast.LENGTH_SHORT).show();
                    return;
                }

                SQLiteDatabase db = null;
                try {
                    db = dbHelper.getWritableDatabase();
                    int rows = db.delete("NguoiDung", "MaNguoiDung = ?", new String[]{id});
                    if (rows > 0) {
                        int removeIndex = -1;
                        if (position >= 0 && position < driverIds.size() && id.equals(driverIds.get(position))) {
                            removeIndex = position;
                        } else {
                            removeIndex = driverIds.indexOf(id);
                        }

                        if (removeIndex >= 0) {
                            // remove from full lists too
                            for (int i = 0; i < driverIdsFull.size(); i++) {
                                if (driverIdsFull.get(i).equals(id)) {
                                    driverIdsFull.remove(i);
                                    driverListFull.remove(i);
                                    break;
                                }
                            }
                            driverAdapter.removeAt(removeIndex);
                            Toast.makeText(requireContext(), "Xóa thành công.", Toast.LENGTH_SHORT).show();
                        } else {
                            // fallback: rebuild from full lists
                            for (int i = 0; i < driverIdsFull.size(); i++) {
                                if (driverIdsFull.get(i).equals(id)) {
                                    driverIdsFull.remove(i);
                                    driverListFull.remove(i);
                                    break;
                                }
                            }
                            driverList.clear();
                            driverIds.clear();
                            driverList.addAll(driverListFull);
                            driverIds.addAll(driverIdsFull);
                            driverAdapter.notifyDataSetChanged();
                            Toast.makeText(requireContext(), "Xóa thành công (cập nhật danh sách).", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), "Xóa thất bại.", Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(requireContext(), "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_LONG).show();
                } finally {
                    if (db != null) db.close();
                }
            }
        });

        // ===== Vehicle adapter: listener nhận maXe (int)
// ===== Vehicle adapter: listener nhận VehicleListModel và xử lý delete với confirm dialog
        vehicleAdapter = new VehicleListAdapter(vehicleList, new VehicleListAdapter.OnItemActionListener() {
            @Override
            public void onItemClick(int maXe) {
                // mở chi tiết xe
                VehicleDetailFragment frag = VehicleDetailFragment.newInstance(maXe);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.containerMain, frag)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void onDeleteRequested(int maXe, int position) {
                // Hiển thị dialog xác nhận trước khi xóa
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Xóa xe")
                        .setMessage("Bạn có chắc muốn xóa xe ?")
                        .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            // Thực hiện xóa trong background thread để không block UI
                            new Thread(() -> {
                                SQLiteDatabase db = null;
                                try {
                                    db = dbHelper.getWritableDatabase();
                                    int rows = db.delete("Xe", "MaXe = ?", new String[]{String.valueOf(maXe)});
                                    if (rows > 0) {
                                        // Cập nhật UI trên main thread
                                        requireActivity().runOnUiThread(() -> {
                                            int removeIndex = -1;
                                            // ưu tiên dùng position nếu hợp lệ và khớp
                                            if (position >= 0 && position < vehicleList.size() && vehicleList.get(position).getMaXe() == maXe) {
                                                removeIndex = position;
                                            } else {
                                                // fallback: tìm index theo maXe
                                                for (int i = 0; i < vehicleList.size(); i++) {
                                                    if (vehicleList.get(i).getMaXe() == maXe) { removeIndex = i; break; }
                                                }
                                            }

                                            if (removeIndex >= 0) {
                                                // xóa khỏi vehicleListFull nếu tồn tại
                                                for (int i = 0; i < vehicleListFull.size(); i++) {
                                                    if (vehicleListFull.get(i).getMaXe() == maXe) {
                                                        vehicleListFull.remove(i);
                                                        break;
                                                    }
                                                }
                                                vehicleAdapter.removeAt(removeIndex);
                                                Toast.makeText(requireContext(), "Xóa xe thành công.", Toast.LENGTH_SHORT).show();
                                            } else {
                                                // Nếu không tìm thấy vị trí (vd: do đang filter), rebuild danh sách từ vehicleListFull
                                                vehicleList.clear();
                                                vehicleList.addAll(vehicleListFull);
                                                vehicleAdapter.notifyDataSetChanged();
                                                Toast.makeText(requireContext(), "Xóa thành công (cập nhật danh sách).", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    } else {
                                        requireActivity().runOnUiThread(() ->
                                                Toast.makeText(requireContext(), "Xóa thất bại.", Toast.LENGTH_SHORT).show()
                                        );
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(requireContext(), "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                    );
                                } finally {
                                    if (db != null) db.close();
                                }
                            }).start();
                        })
                        .show();
            }
        });

        // Mặc định hiển thị danh sách tài xế
        driverRecyclerView.setAdapter(driverAdapter);
        setSelectedSegment(true);
        fabAdd.setVisibility(View.GONE);

        // set up search watcher
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s == null ? "" : s.toString();
                if (isDriverSelected) filterDrivers(q); else filterVehicles(q);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnDriver.setOnClickListener(v -> {
            setSelectedSegment(true);
            driverRecyclerView.setAdapter(driverAdapter);
            fabAdd.setVisibility(View.GONE);
            String q = searchEditText.getText() == null ? "" : searchEditText.getText().toString();
            filterDrivers(q);
        });

        btnVehicle.setOnClickListener(v -> {
            setSelectedSegment(false);
            driverRecyclerView.setAdapter(vehicleAdapter);
            fabAdd.setVisibility(View.VISIBLE);
            String q = searchEditText.getText() == null ? "" : searchEditText.getText().toString();
            filterVehicles(q);
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
        this.isDriverSelected = isDriverSelected;
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
    // 🔹 TRUY VẤN DANH SÁCH TÀI XẾ (LẤY MaNguoiDung)
    // ===============================
    private void loadDriversFromDatabase() {
        driverList.clear();
        driverIds.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT ND.MaNguoiDung, ND.HoTen, ND.NgaySinh, ND.SDT, ND.CCCD, ND.GioiTinh, TK.Email " +
                            "FROM NguoiDung ND " +
                            "JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan " +
                            "WHERE lower(COALESCE(ND.VaiTro, '')) LIKE ? OR lower(COALESCE(ND.VaiTro, '')) LIKE ?",
                    new String[]{"%nhân viên%", "%tai xe%"}
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String idStr = "";
                    try {
                        int idInt = cursor.getInt(0);
                        idStr = String.valueOf(idInt);
                    } catch (Exception e) {
                        idStr = safeGet(cursor, 0);
                    }

                    String name = safeGet(cursor, 1);
                    String ngaySinh = safeGet(cursor, 2);
                    String addrOrBirth = (ngaySinh != null && !ngaySinh.isEmpty()) ? ngaySinh : "Chưa cập nhật";

                    DriverListModel model = new DriverListModel(
                            (name != null && !name.isEmpty()) ? name : "Không rõ",
                            addrOrBirth,
                            R.drawable.avatar1
                    );
                    driverList.add(model);
                    driverIds.add(idStr);
                } while (cursor.moveToNext());
            }
        } catch (Exception ex) {
            Log.e(TAG, "loadDriversFromDatabase error", ex);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    // ===============================
    // 🔹 TRUY VẤN DANH SÁCH XE (LẤY MaXe)
    // ===============================
    private List<VehicleListModel> loadVehiclesFromDatabase() {
        List<VehicleListModel> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            // Lấy MaXe để có thể truyền sang VehicleDetailFragment
            cursor = db.rawQuery("SELECT MaXe, BienSo, LoaiXe FROM Xe", null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int maXe = -1;
                    try {
                        maXe = cursor.getInt(0);
                    } catch (Exception e) {
                        // fallback: nếu không parse được, bỏ qua (không hiển thị item đó)
                        Log.w(TAG, "MaXe không hợp lệ tại hàng, bỏ qua item", e);
                        continue;
                    }
                    String plate = safeGet(cursor, 1);
                    String name = safeGet(cursor, 2);

                    // Giả định VehicleListModel có constructor (int maXe, String plate, String name, int imageResId)
                    VehicleListModel v = new VehicleListModel(maXe,
                            (plate != null && !plate.isEmpty()) ? plate : "-",
                            (name != null && !name.isEmpty()) ? name : "Không rõ",
                            R.drawable.avatar1
                    );
                    list.add(v);
                } while (cursor.moveToNext());
            }
        } catch (Exception ex) {
            Log.e(TAG, "loadVehiclesFromDatabase error", ex);
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    // Fallback: tìm index trong driverList bằng name + location (nếu indexOf() không tìm thấy)
    private int findIndexByNameAndLocation(String name, String location) {
        if (name == null) name = "";
        if (location == null) location = "";
        for (int i = 0; i < driverList.size(); i++) {
            DriverListModel d = driverList.get(i);
            String dn = d.getName() != null ? d.getName() : "";
            String dl = d.getLocation() != null ? d.getLocation() : "";
            if (dn.equals(name) && dl.equals(location)) return i;
        }
        return -1;
    }

    // Hàm tiện ích lấy string an toàn từ cursor theo index:
    private String safeGet(Cursor c, int index) {
        if (c == null) return "";
        try {
            if (index < 0 || index >= c.getColumnCount()) return "";
            if (c.isNull(index)) return "";
            String s = c.getString(index);
            return s != null ? s : "";
        } catch (Exception e) {
            try {
                return c.getString(index);
            } catch (Exception ex) {
                return "";
            }
        }
    }

    // ====== FILTER FUNCTIONS ======
    private void filterDrivers(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        driverList.clear();
        driverIds.clear();

        if (q.isEmpty()) {
            driverList.addAll(driverListFull);
            driverIds.addAll(driverIdsFull);
        } else {
            for (int i = 0; i < driverListFull.size(); i++) {
                DriverListModel d = driverListFull.get(i);
                String name = d.getName() == null ? "" : d.getName().toLowerCase();
                String loc = d.getLocation() == null ? "" : d.getLocation().toLowerCase();
                if (name.contains(q) || loc.contains(q)) {
                    driverList.add(d);
                    driverIds.add(driverIdsFull.get(i));
                }
            }
        }

        if (driverAdapter != null) driverAdapter.notifyDataSetChanged();
    }

    private void filterVehicles(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        vehicleList.clear();

        if (q.isEmpty()) {
            vehicleList.addAll(vehicleListFull);
        } else {
            for (VehicleListModel v : vehicleListFull) {
                String plate = v.getPlateNumber() == null ? "" : v.getPlateNumber().toLowerCase();
                String name = v.getName() == null ? "" : v.getName().toLowerCase();
                if (plate.contains(q) || name.contains(q)) {
                    vehicleList.add(v);
                }
            }
        }

        if (vehicleAdapter != null) vehicleAdapter.notifyDataSetChanged();
    }
}
