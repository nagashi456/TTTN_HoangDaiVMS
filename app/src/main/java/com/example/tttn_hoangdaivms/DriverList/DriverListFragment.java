package com.example.tttn_hoangdaivms.DriverList;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
        loadDriversFromDatabase();   // sẽ điền driverList & driverIds
        vehicleList = loadVehiclesFromDatabase();

        // lưu bản đầy đủ để restore/filter
        driverListFull = new ArrayList<>(driverList);
        driverIdsFull = new ArrayList<>(driverIds);
        vehicleListFull = new ArrayList<>(vehicleList);

        // Adapter driver: khi click -> nhận DriverListModel (như adapter cũ của bạn)
// 1) khởi tạo adapter 1 lần (sau khi loadDriversFromDatabase())
        driverAdapter = new DriverListAdapter(requireContext(), driverList, driverIds, new DriverListAdapter.OnDriverActionListener() {
            @Override
            public void onDriverClick(DriverListModel driver, int position, String id) {
                // mở chi tiết
                if (position == -1 || id == null) {
                    // fallback tìm lại index nếu cần
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
                // tạm thời show toast (hoặc mở fragment edit nếu có)
                Toast.makeText(requireContext(), "Chức năng sửa tạm thời chưa có.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteRequested(DriverListModel driver, int position, String id) {
                if (id == null || id.trim().isEmpty()) {
                    Toast.makeText(requireContext(), "Không xác định được ID để xóa.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Xóa DB (đơn giản, chạy trên UI thread; nếu DB nặng, chạy background)
                SQLiteDatabase db = null;
                try {
                    db = dbHelper.getWritableDatabase();

                    // XÓA các bảng liên quan nếu cần (tuỳ schema)
                    // db.delete("SucKhoe", "MaNguoiDung = ?", new String[]{id});
                    // db.delete("BangCap", "MaTaiXe = ?", new String[]{id});
                    // ... (xóa TaiKhoan nếu cần, phải lấy MaTaiKhoan trước)

                    int rows = db.delete("NguoiDung", "MaNguoiDung = ?", new String[]{id});
                    if (rows > 0) {
                        // --- CHÚ Ý: KHÔNG xóa trực tiếp driverList/driverIds ở đây ---
                        // Chỉ gọi adapter.removeAt(...) để adapter xử lý và thông báo RecyclerView.
                        int removeIndex = -1;

                        // ưu tiên dùng position nếu hợp lệ và id trùng khớp
                        if (position >= 0 && position < driverIds.size() && id.equals(driverIds.get(position))) {
                            removeIndex = position;
                        } else {
                            // fallback: tìm index theo id trong driverIds (dữ liệu hiển thị hiện tại)
                            removeIndex = driverIds.indexOf(id);
                        }

                        if (removeIndex >= 0) {
                            // Cập nhật cả các "full lists" dùng cho filter
                            // (xóa khỏi driverListFull/driverIdsFull để lần filter sau không show item đã xóa)
                            for (int i = 0; i < driverIdsFull.size(); i++) {
                                if (driverIdsFull.get(i).equals(id)) {
                                    driverIdsFull.remove(i);
                                    driverListFull.remove(i);
                                    break;
                                }
                            }

                            // Gọi adapter để xóa (adapter giữ reference tới driverList & driverIds)
                            driverAdapter.removeAt(removeIndex);

                            // Nếu bạn muốn đảm bảo vị trí index của các item sau được cập nhật:
                            // driverAdapter.notifyItemRangeChanged(removeIndex, driverAdapter.getItemCount() - removeIndex);

                            Toast.makeText(requireContext(), "Xóa thành công.", Toast.LENGTH_SHORT).show();
                        } else {
                            // Nếu không tìm thấy vị trí trong adapter (có thể do filter khác), chỉ update full lists
                            // và refresh lại adapter dữ liệu từ full lists:
                            for (int i = 0; i < driverIdsFull.size(); i++) {
                                if (driverIdsFull.get(i).equals(id)) {
                                    driverIdsFull.remove(i);
                                    driverListFull.remove(i);
                                    break;
                                }
                            }
                            // Rebuild display lists from full lists
                            // Nếu bạn đang áp filter, dễ nhất là gọi filterDrivers(currentQuery) hoặc:
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

// 2) set adapter cho recyclerview
        vehicleAdapter = new VehicleListAdapter(vehicleList, maXe -> {
            Toast.makeText(requireContext(), "Mã xe: " + maXe, Toast.LENGTH_SHORT).show();

            // mở VehicleDetailFragment
            VehicleDetailFragment    frag = VehicleDetailFragment.newInstance(maXe);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.containerMain, frag)
                    .addToBackStack(null)
                    .commit();
        });

        // Mặc định hiển thị danh sách tài xế
        driverRecyclerView.setAdapter(driverAdapter);
        setSelectedSegment(true);
        fabAdd.setVisibility(View.GONE);

        // set up search watcher
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s == null ? "" : s.toString();
                if (isDriverSelected) {
                    filterDrivers(q);
                } else {
                    filterVehicles(q);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // no-op
            }
        });

        btnDriver.setOnClickListener(v -> {
            setSelectedSegment(true);
            driverRecyclerView.setAdapter(driverAdapter);
            fabAdd.setVisibility(View.GONE);
            // áp lại filter với nội dung hiện có trong search
            String q = searchEditText.getText() == null ? "" : searchEditText.getText().toString();
            filterDrivers(q);
        });

        btnVehicle.setOnClickListener(v -> {
            setSelectedSegment(false);
            driverRecyclerView.setAdapter(vehicleAdapter);
            fabAdd.setVisibility(View.VISIBLE);
            // áp lại filter với nội dung hiện có trong search
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
            // Lấy MaNguoiDung đầu tiên (để dễ map), sau đó các trường hiển thị
            cursor = db.rawQuery(
                    "SELECT ND.MaNguoiDung, ND.HoTen, ND.NgaySinh, ND.SDT, ND.CCCD, ND.GioiTinh, TK.Email " +
                            "FROM NguoiDung ND " +
                            "JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan OR ND.MaTaiKhoan = TK.MaTaiKhoan " + // phòng trường hợp tên cột khác
                            "WHERE lower(COALESCE(ND.VaiTro, '')) LIKE ? OR lower(COALESCE(ND.VaiTro, '')) LIKE ?",
                    new String[]{"%nhân viên%", "%tai xe%"}
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    // Lấy MaNguoiDung (int) rồi chuyển thành String
                    String idStr = "";
                    try {
                        int idInt = cursor.getInt(0);
                        idStr = String.valueOf(idInt);
                    } catch (Exception e) {
                        // nếu cột không phải int, thử lấy string
                        idStr = safeGet(cursor, 0);
                    }

                    String name = safeGet(cursor, 1);
                    String ngaySinh = safeGet(cursor, 2);

                    String addrOrBirth = (ngaySinh != null && !ngaySinh.isEmpty()) ? ngaySinh : "Chưa cập nhật";

                    // Thêm model hiển thị
                    DriverListModel model = new DriverListModel(
                            (name != null && !name.isEmpty()) ? name : "Không rõ",
                            addrOrBirth,
                            R.drawable.avatar1
                    );
                    driverList.add(model);
                    driverIds.add(idStr); // cùng index với driverList
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    // ===============================
    // 🔹 TRUY VẤN DANH SÁCH XE
    // ===============================
    private List<VehicleListModel> loadVehiclesFromDatabase() {
        List<VehicleListModel> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT BienSo, LoaiXe FROM Xe", null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String plate = safeGet(cursor, 0);
                    String name = safeGet(cursor, 1);
                    list.add(new VehicleListModel(plate, name != null ? name : "Không rõ", R.drawable.avatar1));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        return list;
    }

    /**
     * Fallback: tìm index trong driverList bằng name + location (nếu indexOf() không tìm thấy)
     */
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

    /**
     * Hàm tiện ích lấy string an toàn từ cursor theo index:
     * - trả "" khi column null hoặc lỗi
     */
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
                // tìm theo tên hoặc location (bạn có thể thêm tìm theo số điện thoại, cccd nếu bổ sung vào model)
                if (name.contains(q) || loc.contains(q)) {
                    driverList.add(d);
                    driverIds.add(driverIdsFull.get(i));
                }
            }
        }

        // Cập nhật adapter
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
