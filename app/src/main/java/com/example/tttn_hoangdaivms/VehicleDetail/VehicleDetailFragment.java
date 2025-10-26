package com.example.tttn_hoangdaivms.VehicleDetail;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
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

    public static final String ARG_MA_XE = "MaXe";

    private TextView tvDriverName, tvStatus, tvInspectionDate;
    private TextView tvBienSo, tvLoaiXe, tvHangSX, tvMauSac, tvSoHieu;
    private TextView tvSoHD, tvNgayBatDau, tvNgayKetThuc, tvCongTy;
    private TextView tvNgayGanNhatBT, tvNoiDungBT, tvDonViBT;
    private ImageView ivClose, ivEdit, ivCover;
    private Database database;

    public VehicleDetailFragment() { /* required empty ctor */ }

    /**
     * Tạo instance fragment với MaXe (int)
     */
    public static VehicleDetailFragment newInstance(int maXe) {
        VehicleDetailFragment fragment = new VehicleDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MA_XE, maXe);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vehicle_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // find views
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

        // close button: pop back stack
        ivClose.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
        });

        // Lấy MaXe (hỗ trợ int trong Bundle). Nếu không có -> báo lỗi.
        Bundle args = getArguments();
        int maXe = -1;
        if (args != null) {
            // ưu tiên lấy int
            if (args.containsKey(ARG_MA_XE)) {
                try {
                    maXe = args.getInt(ARG_MA_XE, -1);
                } catch (Exception ignored) { maXe = -1; }
            } else {
                // fallback: có thể người gọi truyền string, thử parse
                String s = args.getString(ARG_MA_XE, null);
                if (!TextUtils.isEmpty(s)) {
                    try {
                        maXe = Integer.parseInt(s);
                    } catch (NumberFormatException ignored) { maXe = -1; }
                }
            }
        }

        if (maXe == -1) {
            Toast.makeText(requireContext(), "ID xe không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        loadVehicleDetail(maXe);
    }

    /**
     * Helper: lấy string an toàn từ cursor, trả null nếu cột không tồn tại hoặc giá trị null
     */
    private String getStringSafe(Cursor c, String colName) {
        if (c == null || c.isClosed()) return null;
        int idx = c.getColumnIndex(colName);
        if (idx == -1) return null;
        return c.isNull(idx) ? null : c.getString(idx);
    }

    /**
     * Helper: lấy int an toàn từ cursor, trả -1 nếu cột không tồn tại hoặc null
     */
    private int getIntSafe(Cursor c, String colName) {
        if (c == null || c.isClosed()) return -1;
        int idx = c.getColumnIndex(colName);
        if (idx == -1) return -1;
        return c.isNull(idx) ? -1 : c.getInt(idx);
    }

    private void loadVehicleDetail(int maXe) {
        SQLiteDatabase db = database.getReadableDatabase();
        Cursor cXe = null;
        Cursor cNguoiDung = null;
        Cursor cBaoTri = null;
        Cursor cBaoHiem = null;

        try {
            // 1) Lấy thông tin xe (và MaNguoiDung)
            cXe = db.rawQuery("SELECT MaNguoiDung, BienSo, LoaiXe, HangSX, MauSac, SoHieu FROM Xe WHERE MaXe = ?",
                    new String[]{String.valueOf(maXe)});

            if (cXe != null && cXe.moveToFirst()) {
                int maNguoiDung = getIntSafe(cXe, "MaNguoiDung");
                String bienSo = getStringSafe(cXe, "BienSo");
                String loaiXe = getStringSafe(cXe, "LoaiXe");
                String hangSX = getStringSafe(cXe, "HangSX");
                String mauSac = getStringSafe(cXe, "MauSac");
                String soHieu = getStringSafe(cXe, "SoHieu");

                // set vehicle fields (null-safe)
                tvBienSo.setText(!TextUtils.isEmpty(bienSo) ? bienSo : "-");
                tvLoaiXe.setText(!TextUtils.isEmpty(loaiXe) ? loaiXe : "-");
                tvHangSX.setText(!TextUtils.isEmpty(hangSX) ? hangSX : "-");
                tvMauSac.setText(!TextUtils.isEmpty(mauSac) ? mauSac : "-");
                tvSoHieu.setText(!TextUtils.isEmpty(soHieu) ? soHieu : "-");

                // 2) Lấy tên tài xế + trạng thái từ NguoiDung theo MaNguoiDung (nếu tồn tại)
                if (maNguoiDung != -1) {
                    cNguoiDung = db.rawQuery("SELECT HoTen, TrangThai FROM NguoiDung WHERE MaNguoiDung = ?",
                            new String[]{String.valueOf(maNguoiDung)});
                    if (cNguoiDung != null && cNguoiDung.moveToFirst()) {
                        String hoTen = getStringSafe(cNguoiDung, "HoTen");
                        String trangThai = getStringSafe(cNguoiDung, "TrangThai");
                        tvDriverName.setText(!TextUtils.isEmpty(hoTen) ? hoTen : "Không rõ");
                        tvStatus.setText(!TextUtils.isEmpty(trangThai) ? trangThai : "-");
                    } else {
                        tvDriverName.setText("Không rõ");
                        tvStatus.setText("-");
                    }
                } else {
                    tvDriverName.setText("Không rõ");
                    tvStatus.setText("-");
                }

                // 3) Lấy thông tin bảo trì mới nhất cho xe (BaoTri) -> ngày kiểm tra
                cBaoTri = db.rawQuery("SELECT NgayGanNhat, NoiDung, DonVi FROM BaoTri WHERE MaXe = ? ORDER BY NgayGanNhat DESC LIMIT 1",
                        new String[]{String.valueOf(maXe)});
                if (cBaoTri != null && cBaoTri.moveToFirst()) {
                    String ngayGanNhat = getStringSafe(cBaoTri, "NgayGanNhat");
                    String noiDung = getStringSafe(cBaoTri, "NoiDung");
                    String donVi = getStringSafe(cBaoTri, "DonVi");

                    tvInspectionDate.setText(!TextUtils.isEmpty(ngayGanNhat) ? ngayGanNhat : "-");
                    tvNgayGanNhatBT.setText(!TextUtils.isEmpty(ngayGanNhat) ? ngayGanNhat : "-");
                    tvNoiDungBT.setText(!TextUtils.isEmpty(noiDung) ? noiDung : "-");
                    tvDonViBT.setText(!TextUtils.isEmpty(donVi) ? donVi : "-");
                } else {
                    tvInspectionDate.setText("Chưa có");
                    tvNgayGanNhatBT.setText("Chưa có");
                    tvNoiDungBT.setText("-");
                    tvDonViBT.setText("-");
                }

                // 4) Lấy thông tin bảo hiểm (nếu có) theo MaTaiXe = MaNguoiDung
                if (maNguoiDung != -1) {
                    cBaoHiem = db.rawQuery("SELECT SoHD, NgayBatDau, NgayKetThuc, CongTy FROM BaoHiem WHERE MaTaiXe = ? ORDER BY MaBaoHiem DESC LIMIT 1",
                            new String[]{String.valueOf(maNguoiDung)});
                    if (cBaoHiem != null && cBaoHiem.moveToFirst()) {
                        String soHD = getStringSafe(cBaoHiem, "SoHD");
                        String ngayBatDau = getStringSafe(cBaoHiem, "NgayBatDau");
                        String ngayKetThuc = getStringSafe(cBaoHiem, "NgayKetThuc");
                        String congTy = getStringSafe(cBaoHiem, "CongTy");

                        tvSoHD.setText(!TextUtils.isEmpty(soHD) ? soHD : "-");
                        tvNgayBatDau.setText(!TextUtils.isEmpty(ngayBatDau) ? ngayBatDau : "-");
                        tvNgayKetThuc.setText(!TextUtils.isEmpty(ngayKetThuc) ? ngayKetThuc : "-");
                        tvCongTy.setText(!TextUtils.isEmpty(congTy) ? congTy : "-");
                    } else {
                        tvSoHD.setText("-");
                        tvNgayBatDau.setText("-");
                        tvNgayKetThuc.setText("-");
                        tvCongTy.setText("-");
                    }
                } else {
                    tvSoHD.setText("-");
                    tvNgayBatDau.setText("-");
                    tvNgayKetThuc.setText("-");
                    tvCongTy.setText("-");
                }

            } else {
                // không tìm thấy xe
                Toast.makeText(requireContext(), "Không tìm thấy thông tin xe (MaXe=" + maXe + ")", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Lỗi khi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (cXe != null && !cXe.isClosed()) cXe.close();
            if (cNguoiDung != null && !cNguoiDung.isClosed()) cNguoiDung.close();
            if (cBaoTri != null && !cBaoTri.isClosed()) cBaoTri.close();
            if (cBaoHiem != null && !cBaoHiem.isClosed()) cBaoHiem.close();
            // Không đóng db ở đây vì Database helper quản lý connection lifecycle
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
