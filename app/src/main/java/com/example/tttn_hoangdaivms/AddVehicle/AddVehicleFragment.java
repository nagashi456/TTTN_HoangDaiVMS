package com.example.tttn_hoangdaivms.AddVehicle;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tttn_hoangdaivms.Database.Database;
import com.example.tttn_hoangdaivms.Database.User;
import com.example.tttn_hoangdaivms.R;
import com.google.android.material.button.MaterialButton;

import java.util.Calendar;

/**
 * Fragment dùng cho Thêm / Chỉnh sửa xe.
 * Nếu muốn chỉnh sửa, truyền args vào fragment (optional):
 *  bundle.putString("action","edit");
 *  bundle.putString("id", id);
 *  bundle.putString("bienSo", "UKW 1234"); ... các trường khác
 *
 * Khi lưu fragment sẽ trả về kết quả bằng FragmentResult:
 *  key: "vehicle_result"
 *  bundle contains: "action" ("add" or "edit"), and all fields.
 *
 * Parent fragment/activity nên listen:
 * getSupportFragmentManager().setFragmentResultListener("vehicle_result", this, (key, result) -> { ... });
 */
public class AddVehicleFragment extends Fragment {

    private EditText edtBienSo, edtLoaiXe, edtHangSX, edtMauSac,
            edtSoHieuLop, edtNhienLieu, edtSoHopDong, edtNgayBatDau,
            edtNgayKetThuc, edtCongTyBH, edtNoiDung, edtNgayGanNhat, edtDonViThucHien;

    private MaterialButton btnSave;
    private ImageView ivBack;

    // nếu chỉnh sửa, có thể có id hoặc flag
    private boolean isEdit = false;
    private String vehicleId = null;

    // tên SharedPreferences chứa email người dùng (bạn có thể đổi)
    private static final String PREFS_NAME = "user_prefs";
    private static final String PREF_KEY_EMAIL = "email";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add_vehicle, container, false);

        // Ánh xạ view
        edtBienSo = view.findViewById(R.id.edtBienSo);
        edtLoaiXe = view.findViewById(R.id.edtLoaiXe);
        edtHangSX = view.findViewById(R.id.edtHangSX);
        edtMauSac = view.findViewById(R.id.edtMauSac);
        edtSoHieuLop = view.findViewById(R.id.edtSoHieuLop);
        edtNhienLieu = view.findViewById(R.id.edtNhienLieu);

        edtSoHopDong = view.findViewById(R.id.edtSoHopDong);
        edtNgayBatDau = view.findViewById(R.id.edtNgayBatDau);
        edtNgayKetThuc = view.findViewById(R.id.edtNgayKetThuc);
        edtCongTyBH = view.findViewById(R.id.edtCongTyBH);

        edtNoiDung = view.findViewById(R.id.edtNoiDung);
        edtNgayGanNhat = view.findViewById(R.id.edtNgayGanNhat);
        edtDonViThucHien = view.findViewById(R.id.edtDonViThucHien);

        btnSave = view.findViewById(R.id.btnSave);
        ivBack = view.findViewById(R.id.ivBack);

        // Nếu có args (chỉnh sửa) -> điền dữ liệu lên fields
        readArgumentsAndPopulate();

        // Gắn listener
        setupListeners();

        return view;
    }

    private void readArgumentsAndPopulate() {
        Bundle args = getArguments();
        if (args == null) return;

        String action = args.getString("action", "");
        if ("edit".equalsIgnoreCase(action)) {
            isEdit = true;
            vehicleId = args.getString("id", null);
        }

        // Các trường khác (nếu có) sẽ được đổ vào EditText (ấn default nếu null)
        putIfNotNull(edtBienSo, args.getString("bienSo", null));
        putIfNotNull(edtLoaiXe, args.getString("loaiXe", null));
        putIfNotNull(edtHangSX, args.getString("hangSX", null));
        putIfNotNull(edtMauSac, args.getString("mauSac", null));
        putIfNotNull(edtSoHieuLop, args.getString("soHieuLop", null));
        putIfNotNull(edtNhienLieu, args.getString("nhienLieu", null));

        putIfNotNull(edtSoHopDong, args.getString("soHopDong", null));
        putIfNotNull(edtNgayBatDau, args.getString("ngayBatDau", null));
        putIfNotNull(edtNgayKetThuc, args.getString("ngayKetThuc", null));
        putIfNotNull(edtCongTyBH, args.getString("congTyBH", null));

        putIfNotNull(edtNoiDung, args.getString("noiDung", null));
        putIfNotNull(edtNgayGanNhat, args.getString("ngayGanNhat", null));
        putIfNotNull(edtDonViThucHien, args.getString("donViThucHien", null));
    }

    private void putIfNotNull(EditText edt, String value) {
        if (edt == null) return;
        if (value != null) edt.setText(value);
    }

    private void setupListeners() {
        // Back: pop
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> {
                hideKeyboard();
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }

        // Date pickers
        if (edtNgayBatDau != null) {
            edtNgayBatDau.setOnClickListener(v -> showDatePicker(edtNgayBatDau));
        }
        if (edtNgayKetThuc != null) {
            edtNgayKetThuc.setOnClickListener(v -> showDatePicker(edtNgayKetThuc));
        }
        if (edtNgayGanNhat != null) {
            edtNgayGanNhat.setOnClickListener(v -> showDatePicker(edtNgayGanNhat));
        }

        // Save
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                hideKeyboard();
                if (!validateInputs()) return;

                // Lấy dữ liệu từ form
                final String bienSo = getText(edtBienSo);
                final String loaiXe = getText(edtLoaiXe);
                final String hangSX = getText(edtHangSX);
                final String mauSac = getText(edtMauSac);
                final String soHieu = getText(edtSoHieuLop);
                // các trường khác
                final String soHopDong = getText(edtSoHopDong);
                final String ngayBatDau = getText(edtNgayBatDau);
                final String ngayKetThuc = getText(edtNgayKetThuc);
                final String congTy = getText(edtCongTyBH);

                final String noiDung = getText(edtNoiDung);
                final String ngayGanNhat = getText(edtNgayGanNhat);
                final String donVi = getText(edtDonViThucHien);

                // Lấy email người dùng hiện tại: ưu tiên arguments -> SharedPreferences
                String currentEmail = "admin@vms.com";
                Bundle args = getArguments();
                if (args != null) currentEmail = args.getString("currentEmail", null);

                if (currentEmail == null && getActivity() != null) {
                    SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    currentEmail = prefs.getString(PREF_KEY_EMAIL, null);
                }

                // Nếu không có email, báo lỗi
                if (currentEmail == null) {
                    Toast.makeText(requireContext(), "Không xác định user hiện tại (email).", Toast.LENGTH_LONG).show();
                    return;
                }

                // Thực hiện insert trong thread nền
                final String finalCurrentEmail = currentEmail;
                new Thread(() -> {
                    Database db = new Database(requireContext());
                    try {
                        // Lấy MaNguoiDung từ email
                        User currentUser = db.getUserByEmail(finalCurrentEmail);
                        if (currentUser == null) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "Không tìm thấy thông tin user để liên kết bảo hiểm.", Toast.LENGTH_LONG).show()
                            );
                            return;
                        }

                        int maTaiXe = currentUser.getMaNguoiDung(); // giả sử User có getId()

                        // Gọi hàm insert (Database phải có insertXeWithBaoTriAndBaoHiem)
                        boolean inserted = db.insertXeWithBaoTriAndBaoHiem(
                                bienSo,
                                loaiXe,
                                hangSX,
                                mauSac,
                                soHieu,
                                ngayGanNhat, // ngayGapNhat bao tri
                                noiDung,
                                donVi,
                                maTaiXe,
                                soHopDong,
                                ngayBatDau,
                                ngayKetThuc,
                                congTy
                        );

                        if (inserted) {
                            // Thành công -> trả kết quả về main thread, setFragmentResult và popBackStack
                            requireActivity().runOnUiThread(() -> {
                                Bundle result = new Bundle();
                                result.putString("action", isEdit ? "edit" : "add");
                                if (vehicleId != null) result.putString("id", vehicleId);

                                result.putString("bienSo", bienSo);
                                result.putString("loaiXe", loaiXe);
                                result.putString("hangSX", hangSX);
                                result.putString("mauSac", mauSac);
                                result.putString("soHieuLop", soHieu);

                                result.putString("soHopDong", soHopDong);
                                result.putString("ngayBatDau", ngayBatDau);
                                result.putString("ngayKetThuc", ngayKetThuc);
                                result.putString("congTyBH", congTy);

                                result.putString("noiDung", noiDung);
                                result.putString("ngayGanNhat", ngayGanNhat);
                                result.putString("donViThucHien", donVi);

                                getParentFragmentManager().setFragmentResult("vehicle_result", result);

                                Toast.makeText(requireContext(), (isEdit ? "Cập nhật" : "Thêm") + " xe thành công", Toast.LENGTH_SHORT).show();
                                if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
                            });
                        } else {
                            // Thất bại (ví dụ: biển số trùng)
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "Lưu thất bại — kiểm tra lại biển số hoặc dữ liệu.", Toast.LENGTH_LONG).show()
                            );
                        }
                    } finally {
                        db.close();
                    }
                }).start();
            });
        }
    }

    private String getText(EditText edt) {
        return edt != null ? edt.getText().toString().trim() : "";
    }

    private boolean validateInputs() {
        // Ví dụ cơ bản: biển số bắt buộc
        if (edtBienSo == null || TextUtils.isEmpty(edtBienSo.getText())) {
            if (edtBienSo != null) {
                edtBienSo.setError("Vui lòng nhập biển số");
                edtBienSo.requestFocus();
            }
            return false;
        }
        // Bạn có thể bổ sung validate khác (số hợp đồng chỉ số, email, ngày hợp lệ, ...)
        return true;
    }

    private void showDatePicker(final EditText target) {
        final Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH);
        int d = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(requireContext(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        String date = dayOfMonth + "/" + (month + 1) + "/" + year;
                        target.setText(date);
                    }
                }, y, m, d);
        dpd.show();
    }

    private void hideKeyboard() {
        if (getActivity() == null) return;
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(getActivity().INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
