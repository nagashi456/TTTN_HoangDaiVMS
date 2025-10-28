package com.example.tttn_hoangdaivms.VehicleDetail;

import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tttn_hoangdaivms.Database.Database;
import com.example.tttn_hoangdaivms.R;

public class VehicleDetailFragment extends Fragment {
    private static final String TAG = "VehicleDetailFragment";
    public static final String ARG_MA_XE = "MaXe";

    private TextView tvDriverName, tvStatus, tvInspectionDate;
    private TextView tvBienSo, tvLoaiXe, tvHangSX, tvMauSac, tvSoHieu;
    private TextView tvSoHD, tvNgayBatDau, tvNgayKetThuc, tvCongTy;
    private TextView tvNgayGanNhatBT, tvNoiDungBT, tvDonViBT;
    private ImageView ivClose, ivEdit, ivCover;
    private Database database;

    public VehicleDetailFragment() { /* required */ }

    public static VehicleDetailFragment newInstance(int maXe) {
        VehicleDetailFragment f = new VehicleDetailFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_MA_XE, maXe);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public android.view.View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                          @Nullable android.os.Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vehicle_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find views
        tvDriverName = view.findViewById(R.id.tvDriverName);
        tvStatus = view.findViewById(R.id.tvStatus);
        tvInspectionDate = view.findViewById(R.id.tvInspectionDate);

        tvBienSo = view.findViewById(R.id.tvBienSo);
        tvLoaiXe = view.findViewById(R.id.tvLoaiXe);
        tvHangSX = view.findViewById(R.id.tvHangSX);
        tvMauSac = view.findViewById(R.id.tvMauSac);
        tvSoHieu = view.findViewById(R.id.tvSoHieu);

        tvSoHD = view.findViewById(R.id.tvSoHD);
        tvNgayBatDau = view.findViewById(R.id.tvNgayBatDau);
        tvNgayKetThuc = view.findViewById(R.id.tvNgayKetThuc);
        tvCongTy = view.findViewById(R.id.tvCongTy);

        tvNgayGanNhatBT = view.findViewById(R.id.tvNgayGanNhatBT);
        tvNoiDungBT = view.findViewById(R.id.tvNoiDungBT);
        tvDonViBT = view.findViewById(R.id.tvDonViBT);

        ivClose = view.findViewById(R.id.ivClose);
        ivEdit = view.findViewById(R.id.ivEdit);
        ivCover = view.findViewById(R.id.ivCover);

        database = new Database(requireContext());

        ivClose.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
        });

        // Get MaXe
        int maXe = -1;
        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_MA_XE)) {
            maXe = args.getInt(ARG_MA_XE, -1);
        }
        if (maXe == -1) {
            Toast.makeText(requireContext(), "ID xe không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        final int finalMaXe = maXe;
        // Run DB operations in background thread
        new Thread(() -> loadVehicleDetailBackground(finalMaXe)).start();
//        View bottomNav = requireActivity().findViewById(R.id.bottomNavigation);
//        if (bottomNav != null) {
//            bottomNav.setVisibility(View.GONE);
//        }
    }

    // Helper lấy string an toàn (cursor local only)
    private String getStringSafeLocal(Cursor c, String col) {
        if (c == null || c.isClosed()) return null;
        int idx = c.getColumnIndex(col);
        if (idx == -1) return null;
        return c.isNull(idx) ? null : c.getString(idx);
    }

    private int getIntSafeLocal(Cursor c, String col) {
        if (c == null || c.isClosed()) return -1;
        int idx = c.getColumnIndex(col);
        if (idx == -1) return -1;
        return c.isNull(idx) ? -1 : c.getInt(idx);
    }

    /**
     * Đọc toàn bộ dữ liệu cần thiết trong background thread, đóng Cursor ngay,
     * sau đó truyền các giá trị thuần (String/int) lên UI thread để cập nhật.
     */
    private void loadVehicleDetailBackground(int maXe) {
        Cursor cXe = null;
        Cursor cBaoTri = null;
        Cursor cBaoHiem = null;
        // Variables to hold values read from DB
        int maNguoiDung = -1;
        String bienSo = null, loaiXe = null, hangSX = null, mauSac = null, soHieu = null;
        String hoTen = null, trangThai = null;
        String ngayGanNhat = null, noiDung = null, donVi = null;
        String soHD = null, ngayBD = null, ngayKT = null, congTy = null;

        try {
            // 1) Lấy thông tin xe
            try {
                // try helper if exists
                cXe = database.getXeById(maXe);
            } catch (Exception e) {
                cXe = database.getReadableDatabase().rawQuery(
                        "SELECT MaXe, MaNguoiDung, BienSo, LoaiXe, HangSX, MauSac, SoHieu FROM Xe WHERE MaXe = ?",
                        new String[]{String.valueOf(maXe)});
            }

            if (cXe != null && cXe.moveToFirst()) {
                maNguoiDung = getIntSafeLocal(cXe, "MaNguoiDung");
                bienSo = getStringSafeLocal(cXe, "BienSo");
                loaiXe = getStringSafeLocal(cXe, "LoaiXe");
                hangSX = getStringSafeLocal(cXe, "HangSX");
                mauSac = getStringSafeLocal(cXe, "MauSac");
                soHieu = getStringSafeLocal(cXe, "SoHieu");
            } else {
                // no vehicle found -> update UI and return
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Không tìm thấy thông tin xe (MaXe=" + maXe + ")", Toast.LENGTH_SHORT).show()
                );
                return;
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error reading Xe", ex);
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "Lỗi khi tải thông tin xe: " + ex.getMessage(), Toast.LENGTH_LONG).show()
            );
            return;
        } finally {
            try { if (cXe != null && !cXe.isClosed()) cXe.close(); } catch (Exception ignored) {}
        }

        // 2) Lấy thông tin người (nếu có)
        if (maNguoiDung != -1) {
            Cursor cNguoi = null;
            try {
                cNguoi = database.getReadableDatabase().rawQuery(
                        "SELECT HoTen, TrangThai FROM NguoiDung WHERE MaNguoiDung = ?",
                        new String[]{String.valueOf(maNguoiDung)});
                if (cNguoi != null && cNguoi.moveToFirst()) {
                    hoTen = getStringSafeLocal(cNguoi, "HoTen");
                    trangThai = getStringSafeLocal(cNguoi, "TrangThai");
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error reading NguoiDung", ex);
            } finally {
                try { if (cNguoi != null && !cNguoi.isClosed()) cNguoi.close(); } catch (Exception ignored) {}
            }
        }

        // 3) Lấy BaoTri mới nhất cho MaXe
        try {
            try {
                cBaoTri = database.getLatestBaoTriByMaXe(maXe);
            } catch (Exception e) {
                cBaoTri = database.getReadableDatabase().rawQuery(
                        "SELECT NgayGanNhat, NoiDung, DonVi FROM BaoTri WHERE MaXe = ? ORDER BY NgayGanNhat DESC, MaBaoTri DESC LIMIT 1",
                        new String[]{String.valueOf(maXe)});
            }
            if (cBaoTri != null && cBaoTri.moveToFirst()) {
                ngayGanNhat = getStringSafeLocal(cBaoTri, "NgayGanNhat");
                noiDung = getStringSafeLocal(cBaoTri, "NoiDung");
                donVi = getStringSafeLocal(cBaoTri, "DonVi");
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error reading BaoTri", ex);
        } finally {
            try { if (cBaoTri != null && !cBaoTri.isClosed()) cBaoTri.close(); } catch (Exception ignored) {}
        }

        // 4) Lấy BaoHiem mới nhất theo MaTaiXe = MaNguoiDung (nếu có)
        if (maNguoiDung != -1) {
            try {
                try {
                    cBaoHiem = database.getLatestBaoHiemByMaTaiXe(maNguoiDung);
                } catch (Exception e) {
                    cBaoHiem = database.getReadableDatabase().rawQuery(
                            "SELECT SoHD, NgayBatDau, NgayKetThuc, CongTy FROM BaoHiem WHERE MaTaiXe = ? ORDER BY MaBaoHiem DESC LIMIT 1",
                            new String[]{String.valueOf(maNguoiDung)});
                }
                if (cBaoHiem != null && cBaoHiem.moveToFirst()) {
                    soHD = getStringSafeLocal(cBaoHiem, "SoHD");
                    ngayBD = getStringSafeLocal(cBaoHiem, "NgayBatDau");
                    ngayKT = getStringSafeLocal(cBaoHiem, "NgayKetThuc");
                    congTy = getStringSafeLocal(cBaoHiem, "CongTy");
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error reading BaoHiem", ex);
            } finally {
                try { if (cBaoHiem != null && !cBaoHiem.isClosed()) cBaoHiem.close(); } catch (Exception ignored) {}
            }
        }

        // Tất cả dữ liệu đã được đọc và cursors đã đóng — giờ cập nhật UI bằng các biến thuần
        final String finalBienSo = bienSo, finalLoaiXe = loaiXe, finalHangSX = hangSX,
                finalMauSac = mauSac, finalSoHieu = soHieu;
        final String finalHoTen = hoTen, finalTrangThai = trangThai;
        final String finalNgayGanNhat = ngayGanNhat, finalNoiDung = noiDung, finalDonVi = donVi;
        final String finalSoHD = soHD, finalNgayBD = ngayBD, finalNgayKT = ngayKT, finalCongTy = congTy;

        requireActivity().runOnUiThread(() -> {
            // set vehicle info
            tvBienSo.setText(!TextUtils.isEmpty(finalBienSo) ? finalBienSo : "-");
            tvLoaiXe.setText(!TextUtils.isEmpty(finalLoaiXe) ? finalLoaiXe : "-");
            tvHangSX.setText(!TextUtils.isEmpty(finalHangSX) ? finalHangSX : "-");
            tvMauSac.setText(!TextUtils.isEmpty(finalMauSac) ? finalMauSac : "-");
            tvSoHieu.setText(!TextUtils.isEmpty(finalSoHieu) ? finalSoHieu : "-");

            // set nguoi dung
            tvDriverName.setText(!TextUtils.isEmpty(finalHoTen) ? finalHoTen : "Không rõ");
            tvStatus.setText(!TextUtils.isEmpty(finalTrangThai) ? finalTrangThai : "-");

            // set bao tri
            if (!TextUtils.isEmpty(finalNgayGanNhat) || !TextUtils.isEmpty(finalNoiDung) || !TextUtils.isEmpty(finalDonVi)) {
                tvInspectionDate.setText(!TextUtils.isEmpty(finalNgayGanNhat) ? finalNgayGanNhat : "-");
                tvNgayGanNhatBT.setText(!TextUtils.isEmpty(finalNgayGanNhat) ? finalNgayGanNhat : "-");
                tvNoiDungBT.setText(!TextUtils.isEmpty(finalNoiDung) ? finalNoiDung : "-");
                tvDonViBT.setText(!TextUtils.isEmpty(finalDonVi) ? finalDonVi : "-");
            } else {
                tvInspectionDate.setText("Chưa có");
                tvNgayGanNhatBT.setText("Chưa có");
                tvNoiDungBT.setText("-");
                tvDonViBT.setText("-");
            }

            // set bao hiem
            tvSoHD.setText(!TextUtils.isEmpty(finalSoHD) ? finalSoHD : "-");
            tvNgayBatDau.setText(!TextUtils.isEmpty(finalNgayBD) ? finalNgayBD : "-");
            tvNgayKetThuc.setText(!TextUtils.isEmpty(finalNgayKT) ? finalNgayKT : "-");
            tvCongTy.setText(!TextUtils.isEmpty(finalCongTy) ? finalCongTy : "-");
        });
    }
}
