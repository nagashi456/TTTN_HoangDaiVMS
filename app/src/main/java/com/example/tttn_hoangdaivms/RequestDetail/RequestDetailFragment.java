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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RequestDetailFragment extends Fragment {

    private TextView tvFullName, tvCCCD, tvPhone, tvEmail, tvStatus;
    private Button btnCancel, btnApprove;
    private ImageView btnBack;
    private RequestModel requestModel;
    private int userId = -1;

    // định dạng thời gian: dd/MM/yyyy HH:mm:ss
    private static final String DATETIME_FORMAT = "dd/MM/yyyy HH:mm:ss";

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
            btnApprove.setOnClickListener(v -> {
                // disable ngay để tránh bấm nhiều lần
                setButtonDisabled(btnApprove, true);
                updateRequestStatusPreserveTimestamp(userId, "Đã duyệt");
            });
        }

        // Nút Hủy -> cập nhật status = "Đã từ chối"
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                // disable ngay để tránh bấm nhiều lần
                setButtonDisabled(btnCancel, true);
                updateRequestStatusPreserveTimestamp(userId, "Đã từ chối");
            });
        }

        // Nút quay lại
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        return view;
    }

    /**
     * Load dữ liệu người dùng từ SQLite (bổ sung TrangThaiUpdatedAt).
     * SELECT trả: 0:MaNguoiDung, 1:HoTen, 2:CCCD, 3:SDT, 4:Email, 5:VaiTro, 6:TrangThai, 7:NgaySinh, 8:TrangThaiUpdatedAt (nếu có)
     */
    private void loadUserDetail(int userId) {
        Database dbHelper = new Database(requireContext());
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery(
                    "SELECT ND.MaNguoiDung, ND.HoTen, ND.CCCD, ND.SDT, TK.Email, ND.VaiTro, ND.TrangThai, ND.NgaySinh, ND.TrangThaiUpdatedAt " +
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
                String trangThaiUpdatedAt = safeGet(cursor, 8);

                if (tvFullName != null) tvFullName.setText(notEmpty(hoTen, "—"));
                if (tvCCCD != null) tvCCCD.setText(notEmpty(cccd, "—"));
                if (tvPhone != null) tvPhone.setText(notEmpty(sdt, "—"));
                if (tvEmail != null) tvEmail.setText(notEmpty(email, "—"));

                // Hiển thị trạng thái từ DB (ưu tiên đồng bộ với DB)
                if (tvStatus != null) {
                    tvStatus.setText(notEmpty(trangThai, "—"));
                    setStatusColor(trangThai);
                }

                // CẬP NHẬT trạng thái enable/disable nút theo trạng thái trong DB
                updateButtonsStateFromStatus(trangThai);

                // NOTE: không disable nút nữa — chúng luôn có thể bấm. Chỉ hiển thị thông tin thời điểm xử lý nếu bạn muốn
                // (bạn có thể thêm TextView hiển thị trangThaiUpdatedAt nếu cần)
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
     * Cập nhật trạng thái nhưng **không gán lại** TrangThaiUpdatedAt nếu nó đã tồn tại.
     * - Nếu TrangThaiUpdatedAt NULL/empty: update cả TrangThai và TrangThaiUpdatedAt = now.
     * - Nếu đã có TrangThaiUpdatedAt: chỉ update TrangThai (không chạm TrangThaiUpdatedAt).
     */
    private void updateRequestStatusPreserveTimestamp(int userId, String newStatus) {
        Database dbHelper = new Database(requireContext());
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getWritableDatabase();

            // Kiểm tra trường TrangThaiUpdatedAt hiện tại
            cursor = db.rawQuery("SELECT TrangThaiUpdatedAt, TrangThai FROM NguoiDung WHERE MaNguoiDung = ?", new String[]{String.valueOf(userId)});
            String existingTimestamp = null;
            String currentStatus = null;
            if (cursor != null && cursor.moveToFirst()) {
                existingTimestamp = cursor.isNull(0) ? null : cursor.getString(0);
                currentStatus = cursor.isNull(1) ? null : cursor.getString(1);
            }
            if (cursor != null) { cursor.close(); cursor = null; }

            ContentValues values = new ContentValues();
            values.put("TrangThai", newStatus);

            boolean hadTimestamp = existingTimestamp != null && !existingTimestamp.trim().isEmpty();

            if (!hadTimestamp) {
                // Nếu chưa có timestamp -> set luôn timestamp hiện tại
                String now = new SimpleDateFormat(DATETIME_FORMAT, Locale.getDefault()).format(new Date());
                values.put("TrangThaiUpdatedAt", now);
            } else {
                // Nếu đã có timestamp -> không thay đổi TrangThaiUpdatedAt
            }

            int rows = db.update("NguoiDung", values, "MaNguoiDung = ?", new String[]{String.valueOf(userId)});

            if (rows > 0) {
                // Thông báo rõ ràng cho người dùng
                if (!hadTimestamp) {
                    Toast.makeText(requireContext(), "Đã cập nhật trạng thái và ghi thời điểm xử lý.", Toast.LENGTH_SHORT).show();
                } else {
                    // Nếu trạng thái không thay đổi với giá trị hiện tại, thông báo tương ứng
                    if (currentStatus != null && currentStatus.equalsIgnoreCase(newStatus)) {
                        Toast.makeText(requireContext(), "Trạng thái không thay đổi (đã là \"" + newStatus + "\").", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Đã cập nhật trạng thái.", Toast.LENGTH_SHORT).show();
                    }
                }

                // Refresh UI từ DB (will also set button states)
                loadUserDetail(userId);

                // Thông báo cho list fragment reload (dù timestamp không đổi)
                Bundle result = new Bundle();
                result.putBoolean("needRefresh", true);
                getParentFragmentManager().setFragmentResult("request_update", result);
            } else {
                Toast.makeText(requireContext(), "Cập nhật thất bại!", Toast.LENGTH_SHORT).show();
                // nếu thất bại (ví dụ row = 0), re-enable nút tương ứng để user thử lại
                reenableButtonForStatus(newStatus);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Lỗi khi cập nhật DB: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            // có lỗi thì đảm bảo re-enable nút tương ứng để user thử lại
            reenableButtonForStatus(newStatus);
        } finally {
            if (cursor != null) cursor.close();
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
            tvStatus.setTextColor(Color.parseColor("#00C853")); // xanh
        } else if (status.equalsIgnoreCase("Đã từ chối")) {
            tvStatus.setTextColor(Color.parseColor("#FF0D0D")); // đỏ
        } else {
            tvStatus.setTextColor(Color.parseColor("#005DFF")); // màu mặc định cho 'Đang yêu cầu'
        }
    }

    /**
     * Disable/enable button và set alpha để nhìn nhạt hơn khi disabled
     * disabled = true -> nút **không** hoạt động (enabled = false)
     */
    private void setButtonDisabled(Button b, boolean disabled) {
        if (b == null) return;
        b.setEnabled(!disabled);
        b.setClickable(!disabled);
        b.setAlpha(disabled ? 0.5f : 1f);
    }

    /**
     * Update button states based on DB status:
     * - Nếu status == "Đã duyệt" -> disable btnApprove
     * - Nếu status == "Đã từ chối" -> disable btnCancel
     * - Ngược lại -> cả hai enable
     */
    private void updateButtonsStateFromStatus(String status) {
        if (btnApprove == null || btnCancel == null) return;
        if (status == null) {
            setButtonDisabled(btnApprove, false);
            setButtonDisabled(btnCancel, false);
            return;
        }
        if (status.equalsIgnoreCase("Đã duyệt")) {
            setButtonDisabled(btnApprove, true);
            setButtonDisabled(btnCancel, false);
        } else if (status.equalsIgnoreCase("Đã từ chối")) {
            setButtonDisabled(btnCancel, true);
            setButtonDisabled(btnApprove, false);
        } else {
            setButtonDisabled(btnApprove, false);
            setButtonDisabled(btnCancel, false);
        }
    }

    /**
     * Nếu cập nhật DB thất bại hoặc lỗi thì re-enable nút tương ứng (sử dụng trên UI thread).
     */
    private void reenableButtonForStatus(String status) {
        if (status == null) return;
        requireActivity().runOnUiThread(() -> {
            if (status.equalsIgnoreCase("Đã duyệt")) {
                setButtonDisabled(btnApprove, false);
            } else if (status.equalsIgnoreCase("Đã từ chối")) {
                setButtonDisabled(btnCancel, false);
            }
        });
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
