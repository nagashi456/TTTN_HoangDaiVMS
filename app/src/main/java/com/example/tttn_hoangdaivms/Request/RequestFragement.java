package com.example.tttn_hoangdaivms.Request;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_request, container, false);

        recyclerViewRequests = view.findViewById(R.id.requestRecyclerView);
        recyclerViewRequests.setLayoutManager(new LinearLayoutManager(requireContext()));

        dbHelper = new Database(requireContext());

        // Load lần đầu
        loadRequestsFromDatabase();

        // Tạo adapter lần đầu với requestList hiện tại
        requestAdapter = new RequestAdapter(requireContext(), requestList, request -> openDetailFragment(request));
        recyclerViewRequests.setAdapter(requestAdapter);

        // Lắng nghe result từ RequestDetailFragment: khi có needRefresh => tải lại dữ liệu và cập nhật adapter
        getParentFragmentManager().setFragmentResultListener("request_update", this, (requestKey, bundle) -> {
            boolean needRefresh = bundle.getBoolean("needRefresh", false);
            if (needRefresh) {
                // Load lại dữ liệu từ DB
                loadRequestsFromDatabase();

                // Cách 1 (đơn giản): tạo adapter mới và set lại
                requestAdapter = new RequestAdapter(requireContext(), requestList, request -> openDetailFragment(request));
                recyclerViewRequests.setAdapter(requestAdapter);

                // Nếu bạn muốn giữ scroll position, thay vì tạo adapter mới hãy gọi:
                // requestAdapter.updateData(requestList);
            }
        });

        return view;
    }

    // Lấy danh sách người dùng có vai trò là nhân viên hoặc tài xế
    private void loadRequestsFromDatabase() {
        requestList = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();

            // Lấy cả TrangThai từ DB (không hardcode)
            cursor = db.rawQuery(
                    "SELECT ND.MaNguoiDung, ND.HoTen, ND.CCCD, ND.SDT, TK.Email, COALESCE(ND.TrangThai, 'Đang yêu cầu'), COALESCE(ND.NgaySinh, '') " +
                            "FROM NguoiDung ND " +
                            "LEFT JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan " +
                            "WHERE lower(COALESCE(ND.VaiTro, '')) LIKE ? OR lower(COALESCE(ND.VaiTro, '')) LIKE ?",
                    new String[]{"%nhân viên%", "%tài%"}
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int userId = cursor.getInt(0);
                    String name = cursor.isNull(1) ? "" : cursor.getString(1);
                    String cccd = cursor.isNull(2) ? "" : cursor.getString(2);
                    String phone = cursor.isNull(3) ? "" : cursor.getString(3);
                    String email = cursor.isNull(4) ? "" : cursor.getString(4);
                    String status = cursor.isNull(5) ? "Đang yêu cầu" : cursor.getString(5);
                    String date = cursor.isNull(6) ? "" : cursor.getString(6);

                    // RequestModel phải có constructor phù hợp và setUserId/getUserId
                    RequestModel model = new RequestModel(userId, name, cccd, phone, email, status, date);
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

    // Mở màn chi tiết; truyền luôn user_id để fragment chi tiết có thể cập nhật/xóa đúng bản ghi
    private void openDetailFragment(RequestModel request) {
        RequestDetailFragment detailFragment = new RequestDetailFragment();

        Bundle bundle = new Bundle();
        bundle.putSerializable("request", request);
        bundle.putInt("user_id", request.getUserId());
        detailFragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.containerMain, detailFragment)
                .addToBackStack(null)
                .commit();
    }
}
