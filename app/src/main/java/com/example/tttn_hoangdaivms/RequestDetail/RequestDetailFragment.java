package com.example.tttn_hoangdaivms.RequestDetail;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tttn_hoangdaivms.Database.Database;
import com.example.tttn_hoangdaivms.R;
import com.example.tttn_hoangdaivms.Request.RequestModel;

public class RequestDetailFragment extends Fragment {

    private TextView tvFullName, tvCCCD, tvPhone, tvEmail, tvStatus, tvRole;
    private Button btnCancel, btnApprove;
    private ImageView btnBack;
    private RequestModel requestModel;
    private int userId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_request_detail, container, false);

        // Ánh xạ View (đảm bảo ID trùng với layout)
        tvFullName = view.findViewById(R.id.tvName);
        tvCCCD = view.findViewById(R.id.tvCCCD);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvStatus = view.findViewById(R.id.tvStatus);
//        tvRole = view.findViewById(R.id.tvRole); // nếu layout không có, tvRole sẽ là null
        btnCancel = view.findViewById(R.id.btnCancel);
        btnApprove = view.findViewById(R.id.btnApprove);
        btnBack = view.findViewById(R.id.btnBack);

        // Nhận dữ liệu từ bundle
        Bundle args = getArguments();
        if (args != null) {
            Object obj = args.getSerializable("request");
            if (obj instanceof RequestModel) {
                requestModel = (RequestModel) obj;
                try {
                    userId = requestModel.getUserId();
                } catch (Exception ignored) {
                    userId = -1;
                }
            }
            if (userId == -1) {
                // fallback: nếu adapter/fragment truyền int trực tiếp
                userId = args.getInt("user_id", -1);
            }
        }

        if (userId == -1) {
            Toast.makeText(requireContext(), "Không có thông tin user để hiển thị.", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Hiển thị dữ liệu (lấy từ DB)
        loadUserDetail(userId);

        // Nút Duyệt -> cập nhật status = "Đã duyệt"
        if (btnApprove != null) {
            btnApprove.setOnClickListener(v -> updateRequestStatus(userId, "Đã duyệt"));
        }

        // Nút Hủy -> cập nhật status = "Đã từ chối"
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> updateRequestStatus(userId, "Đã từ chối"));
        }

        // Nút quay lại
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        return view;
    }

    /**
     * Lấy dữ liệu người dùng từ SQLite (ĐẢM BẢO thứ tự cột tương ứng với SELECT).
     * SELECT sẽ trả: 0:MaNguoiDung, 1:HoTen, 2:CCCD, 3:SDT, 4:Email, 5:VaiTro, 6:TrangThai, 7:NgaySinh (nếu có)
     */
    private void loadUserDetail(int userId) {
        Database dbHelper = new Database(requireContext());
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery(
                    "SELECT ND.MaNguoiDung, ND.HoTen, ND.CCCD, ND.SDT, TK.Email, ND.VaiTro, ND.TrangThai, ND.NgaySinh " +
                            "FROM NguoiDung ND " +
                            "LEFT JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan " +
                            "WHERE ND.MaNguoiDung = ?",
                    new String[]{String.valueOf(userId)}
            );

            if (cursor != null && cursor.moveToFirst()) {
                // Lấy dữ liệu theo index tương ứng
                String hoTen = safeGet(cursor, 1);
                String cccd = safeGet(cursor, 2);
                String sdt = safeGet(cursor, 3);
                String email = safeGet(cursor, 4);
                String vaiTro = safeGet(cursor, 5);
                String trangThai = safeGet(cursor, 6);
                // String ngaySinh = safeGet(cursor, 7); // nếu cần hiển thị

                if (tvFullName != null) tvFullName.setText(notEmpty(hoTen, "—"));
                if (tvCCCD != null) tvCCCD.setText(notEmpty(cccd, "—"));
                if (tvPhone != null) tvPhone.setText(notEmpty(sdt, "—"));
                if (tvEmail != null) tvEmail.setText(notEmpty(email, "—"));
                if (tvRole != null) tvRole.setText(notEmpty(vaiTro, "—"));

                // Hiển thị trạng thái từ DB (ưu tiên đồng bộ với DB)
                if (tvStatus != null) {
                    tvStatus.setText(notEmpty(trangThai, "—"));
                    setStatusColor(trangThai);
                }
            } else {
                Toast.makeText(requireContext(), "Không tìm thấy người dùng!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Lỗi khi đọc DB: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }

    /**
     * Cập nhật trạng thái trong bảng NguoiDung.
     * Sau khi cập nhật thành công sẽ gọi lại loadUserDetail để refresh UI.
     */
    private void updateRequestStatus(int userId, String newStatus) {
        Database dbHelper = new Database(requireContext());
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put("TrangThai", newStatus);

            int rows = db.update("NguoiDung", values, "MaNguoiDung = ?", new String[]{String.valueOf(userId)});

            if (rows > 0) {
                Toast.makeText(requireContext(),
                        "Đã cập nhật trạng thái: " + newStatus, Toast.LENGTH_SHORT).show();

                // Refresh UI từ DB
                loadUserDetail(userId);
                //
                Bundle result = new Bundle();
                result.putBoolean("needRefresh", true);
                getParentFragmentManager().setFragmentResult("request_update", result);
//                requireActivity().getSupportFragmentManager().popBackStack();
                // Nếu muốn sau khi từ chối xóa luôn user/tài khoản, có thể thực hiện ở đây.
                // Ví dụ (KHÔNG BẬT mặc định): nếu (newStatus.equals("Đã từ chối")) { deleteUserAndAccount(userId); }
            } else {
                Toast.makeText(requireContext(), "Cập nhật thất bại!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Lỗi khi cập nhật DB: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (db != null) db.close();
        }
    }

    // Nếu cần: helper xóa user & tài khoản (chỉ gọi khi bạn muốn xóa khi từ chối)
    private void deleteUserAndAccount(int userId) {
        Database dbHelper = new Database(requireContext());
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            // Xóa TaiKhoan trước (nếu FK thiết lập ON DELETE CASCADE thì có thể chỉ xóa trong TaiKhoan hoặc NguoiDung tương ứng)
            db.delete("TaiKhoan", "MaTaiKhoan = (SELECT MaTaiKhoan FROM NguoiDung WHERE MaNguoiDung = ?)", new String[]{String.valueOf(userId)});
            // Xóa NguoiDung
            db.delete("NguoiDung", "MaNguoiDung = ?", new String[]{String.valueOf(userId)});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (db != null) db.close();
        }
    }

    // Đổi màu trạng thái
    private void setStatusColor(String status) {
        if (status == null || tvStatus == null) return;
        if (status.equalsIgnoreCase("Đã duyệt")) {
            tvStatus.setTextColor(Color.parseColor("#00C853"));
        } else if (status.equalsIgnoreCase("Đã từ chối")) {
            tvStatus.setTextColor(Color.parseColor("#FF0D0D"));
        } else {
            tvStatus.setTextColor(Color.parseColor("#005DFF"));
        }
    }

    private String safeGet(Cursor c, int index) {
        try {
            return c.isNull(index) ? "" : c.getString(index);
        } catch (Exception e) {
            return "";
        }
    }

    private String notEmpty(String s, String def) {
        if (s == null) return def;
        s = s.trim();
        return s.isEmpty() ? def : s;
    }
}
