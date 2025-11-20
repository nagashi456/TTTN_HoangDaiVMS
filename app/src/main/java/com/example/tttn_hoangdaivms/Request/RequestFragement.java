package com.example.tttn_hoangdaivms.Request;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tttn_hoangdaivms.Database.Database;
import com.example.tttn_hoangdaivms.R;
import com.example.tttn_hoangdaivms.RequestDetail.RequestDetailFragment;

import java.util.ArrayList;
import java.util.List;

public class RequestFragement extends Fragment {

    private RecyclerView recyclerViewRequests;
    private RequestAdapter requestAdapter;
    private List<RequestModel> requestList;
    private Database dbHelper;

    // --- cho search ---
    private EditText searchEditText;
    private List<RequestModel> requestListFull; // bản đầy đủ để restore/filter

    // text hiển thị khi không có kết quả
    private TextView tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_request, container, false);

        recyclerViewRequests = view.findViewById(R.id.requestRecyclerView);
        recyclerViewRequests.setLayoutManager(new LinearLayoutManager(requireContext()));

        searchEditText = view.findViewById(R.id.searchEditText); // đảm bảo layout có EditText với id này
        tvEmpty = view.findViewById(R.id.tvEmpty); // đảm bảo layout có TextView với id này

        dbHelper = new Database(requireContext());

        // Load lần đầu
        loadRequestsFromDatabase();

        // Lưu bản đầy đủ để filter/restore (hiển thị sẽ dùng DateCreated)
        requestListFull = new ArrayList<>(requestList);

        // Tạo adapter lần đầu với requestList hiện tại
        requestAdapter = new RequestAdapter(requireContext(), requestList, request -> openDetailFragment(request));
        recyclerViewRequests.setAdapter(requestAdapter);

        // cập nhật hiển thị empty view sau khi gán adapter lần đầu
        updateEmptyView(requestList);

        // set up search watcher
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s == null ? "" : s.toString();
                filterRequests(q);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // no-op
            }
        });

        // Lắng nghe result từ RequestDetailFragment: khi có needRefresh => tải lại dữ liệu và cập nhật adapter
        getParentFragmentManager().setFragmentResultListener("request_update", this, (requestKey, bundle) -> {
            boolean needRefresh = bundle.getBoolean("needRefresh", false);
            if (needRefresh) {
                // Load lại dữ liệu từ DB
                loadRequestsFromDatabase();

                // Cập nhật bản full
                requestListFull = new ArrayList<>(requestList);

                // Áp filter hiện tại (nếu có)
                String currentQuery = searchEditText.getText() == null ? "" : searchEditText.getText().toString();
                filterRequests(currentQuery);
            }
        });

        return view;
    }

    /**
     * Load danh sách người dùng có vai trò là nhân viên hoặc tài xế.
     * Chú ý: query loại bỏ những record đã "Đã duyệt"/"Đã từ chối" nếu TrangThaiUpdatedAt <= now - 24 hours.
     * Ngày hiển thị sẽ là DateCreated (không phải NgaySinh).
     */
// ... bên trong class RequestFragement

    private void loadRequestsFromDatabase() {
        requestList = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();

            // Chúng ta chuyển ND.TrangThaiUpdatedAt (dd/MM/yyyy HH:mm:ss) sang ISO yyyy-MM-dd HH:mm:ss tạm thời trong query
            String sql =
                    "SELECT ND.MaNguoiDung, ND.HoTen, ND.CCCD, ND.SDT, TK.Email, COALESCE(ND.TrangThai, 'Đang yêu cầu'), COALESCE(ND.DateCreated, '') " +
                            "FROM NguoiDung ND " +
                            "LEFT JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan " +
                            "WHERE ( lower(COALESCE(ND.VaiTro, '')) LIKE ? OR lower(COALESCE(ND.VaiTro, '')) LIKE ? ) " +
                            "AND NOT ( (ND.TrangThai IN ('Đã duyệt','Đã từ chối')) " +
                            "          AND (ND.TrangThaiUpdatedAt IS NOT NULL) " +
                            // chuyển dd/MM/yyyy HH:MM:SS -> yyyy-MM-dd HH:MM:SS rồi so sánh
                            "          AND ( datetime( substr(ND.TrangThaiUpdatedAt,7,4) || '-' || substr(ND.TrangThaiUpdatedAt,4,2) || '-' || substr(ND.TrangThaiUpdatedAt,1,2) || ' ' || substr(ND.TrangThaiUpdatedAt,12) ) <= datetime('now','-24 hours') ) ) " +
                            "ORDER BY ND.MaNguoiDung DESC";

            cursor = db.rawQuery(sql, new String[]{"%nhân viên%", "%tài%"});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int userId = cursor.isNull(0) ? -1 : cursor.getInt(0);
                    String name = cursor.isNull(1) ? "" : cursor.getString(1);
                    String cccd = cursor.isNull(2) ? "" : cursor.getString(2);
                    String phone = cursor.isNull(3) ? "" : cursor.getString(3);
                    String email = cursor.isNull(4) ? "" : cursor.getString(4);
                    String status = cursor.isNull(5) ? "Đang yêu cầu" : cursor.getString(5);
                    String dateCreated = cursor.isNull(6) ? "" : cursor.getString(6);

                    RequestModel model = new RequestModel(userId, name, cccd, phone, email, status, dateCreated);
                    requestList.add(model);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }


    // Mở màn chi tiết; truyền user_id để fragment chi tiết có thể cập nhật/xóa đúng bản ghi
    // NOTE: không gửi toàn object qua bundle để tránh lỗi nếu model không implement Serializable
    private void openDetailFragment(RequestModel request) {
        RequestDetailFragment detailFragment = new RequestDetailFragment();

        Bundle bundle = new Bundle();
        bundle.putInt("user_id", request.getUserId());
        detailFragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.containerMain, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    // ===== FILTER =====
    private void filterRequests(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        List<RequestModel> filtered = new ArrayList<>();

        if (requestListFull == null) requestListFull = new ArrayList<>();

        if (q.isEmpty()) {
            filtered.addAll(requestListFull);
        } else {
            for (RequestModel r : requestListFull) {
                String name = r.getName() == null ? "" : r.getName().toLowerCase();
                String cccd = r.getCccd() == null ? "" : r.getCccd().toLowerCase();
                String phone = r.getPhone() == null ? "" : r.getPhone().toLowerCase();
                String email = r.getEmail() == null ? "" : r.getEmail().toLowerCase();
                String status = r.getStatus() == null ? "" : r.getStatus().toLowerCase();
                String date = r.getDate() == null ? "" : r.getDate().toLowerCase();

                if (name.contains(q) || cccd.contains(q) || phone.contains(q) || email.contains(q) || status.contains(q) || date.contains(q)) {
                    filtered.add(r);
                }
            }
        }

        // Cập nhật adapter (tạo mới an toàn)
        requestAdapter = new RequestAdapter(requireContext(), filtered, request -> openDetailFragment(request));
        recyclerViewRequests.setAdapter(requestAdapter);

        // cập nhật empty view
        updateEmptyView(filtered);
    }

    /**
     * Hiển thị hoặc ẩn TextView thông báo khi danh sách rỗng.
     */
    private void updateEmptyView(List<RequestModel> list) {
        if (tvEmpty == null || recyclerViewRequests == null) return;
        if (list == null || list.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerViewRequests.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerViewRequests.setVisibility(View.VISIBLE);
        }
    }
}
