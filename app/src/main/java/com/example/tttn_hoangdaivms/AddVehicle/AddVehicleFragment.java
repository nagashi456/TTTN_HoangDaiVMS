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
import android.widget.ArrayAdapter;
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
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AddVehicleFragment extends Fragment {

    private EditText edtBienSo, edtLoaiXe, edtHangSX, edtMauSac,
            edtSoHieuLop, edtNhienLieu, edtSoHopDong, edtNgayBatDau,
            edtNgayKetThuc, edtCongTyBH, edtNoiDung, edtNgayGanNhat, edtDonViThucHien;

    private MaterialButton btnSave;
    private ImageView ivBack;

    // Dropdown for drivers
    private MaterialAutoCompleteTextView actvAssignDriver;
    private ArrayAdapter<String> driverAdapter;
    private final List<String> driverNames = new ArrayList<>();
    private final List<String> driverIds = new ArrayList<>(); // parallel list: same index -> MaNguoiDung (String)

    // nếu chỉnh sửa, có thể có id hoặc flag
    private boolean isEdit = false;
    private String vehicleId = null;

    // SharedPrefs keys
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

        // CHILD view: AutoCompleteTextView (không map TextInputLayout)
        actvAssignDriver = view.findViewById(R.id.actvAssignDriver);

        // Nếu có args (chỉnh sửa) -> điền dữ liệu lên fields
        readArgumentsAndPopulate();

        // load danh sách tài xế cho dropdown
        loadDriversForDropdown();

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

        // Các trường khác (nếu có) sẽ được đổ vào EditText
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

        // Nếu fragment nhận sẵn driver id/name (edit case) — set text
        if (args != null) {
            String assignedDriverName = args.getString("assignedDriverName", null);
            if (assignedDriverName != null && actvAssignDriver != null) {
                actvAssignDriver.setText(assignedDriverName, false);
            }
        }
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
        if (edtNgayBatDau != null) edtNgayBatDau.setOnClickListener(v -> showDatePicker(edtNgayBatDau));
        if (edtNgayKetThuc != null) edtNgayKetThuc.setOnClickListener(v -> showDatePicker(edtNgayKetThuc));
        if (edtNgayGanNhat != null) edtNgayGanNhat.setOnClickListener(v -> showDatePicker(edtNgayGanNhat));

        // ensure the AutoCompleteTextView is focusable and will show dropdown
        if (actvAssignDriver != null) {
            actvAssignDriver.setThreshold(0); // show even with 0 chars
            actvAssignDriver.setOnClickListener(v -> {
                if (driverAdapter != null && driverNames.size() > 0) {
                    actvAssignDriver.showDropDown();
                } else {
                    Toast.makeText(requireContext(), "Chưa có tài xế để chọn", Toast.LENGTH_SHORT).show();
                }
            });
            actvAssignDriver.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && driverAdapter != null && driverNames.size() > 0) {
                    actvAssignDriver.showDropDown();
                }
            });
        }

        // Save
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                hideKeyboard();
                if (!validateInputs()) return;

                // Lấy selected driver name ở UI thread
                final String selectedDriverName = actvAssignDriver == null ? null : actvAssignDriver.getText().toString().trim();

                // Lấy dữ liệu từ form
                final String bienSo = getText(edtBienSo);
                final String loaiXe = getText(edtLoaiXe);
                final String hangSX = getText(edtHangSX);
                final String mauSac = getText(edtMauSac);
                final String soHieu = getText(edtSoHieuLop);

                final String soHopDong = getText(edtSoHopDong);
                final String ngayBatDau = getText(edtNgayBatDau);
                final String ngayKetThuc = getText(edtNgayKetThuc);
                final String congTy = getText(edtCongTyBH);

                final String noiDung = getText(edtNoiDung);
                final String ngayGanNhat = getText(edtNgayGanNhat);
                final String donVi = getText(edtDonViThucHien);

                // Lấy email người dùng hiện tại: ưu tiên arguments -> SharedPreferences
                String currentEmail = null;
                Bundle args = getArguments();
                if (args != null) currentEmail = args.getString("currentEmail", null);

                if (currentEmail == null && getActivity() != null) {
                    SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    currentEmail = prefs.getString(PREF_KEY_EMAIL, null);
                }

                final String finalCurrentEmail = currentEmail;

                new Thread(() -> {
                    Database db = new Database(requireContext());
                    try {
                        int maTaiXeToAssign = -1;

                        // try find selectedDriverName index in driverNames
                        if (selectedDriverName != null && !selectedDriverName.isEmpty()) {
                            int idx = -1;
                            synchronized (driverNames) {
                                for (int i = 0; i < driverNames.size(); i++) {
                                    if (selectedDriverName.equals(driverNames.get(i))) {
                                        idx = i;
                                        break;
                                    }
                                }
                            }
                            if (idx >= 0 && idx < driverIds.size()) {
                                try {
                                    maTaiXeToAssign = Integer.parseInt(driverIds.get(idx));
                                } catch (NumberFormatException ignored) {
                                    maTaiXeToAssign = -1;
                                }
                            }
                        }

                        // fallback to current user if none selected
                        if (maTaiXeToAssign == -1 && finalCurrentEmail != null) {
                            User currentUser = db.getUserByEmail(finalCurrentEmail);
                            if (currentUser != null) maTaiXeToAssign = currentUser.getMaNguoiDung();
                        }

                        if (maTaiXeToAssign == -1) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "Vui lòng chọn tài xế để gán (hoặc đăng nhập trước).", Toast.LENGTH_LONG).show()
                            );
                            return;
                        }

                        final int assignedId = maTaiXeToAssign;

                        boolean inserted = db.insertXeWithBaoTriAndBaoHiem(
                                bienSo,
                                loaiXe,
                                hangSX,
                                mauSac,
                                soHieu,
                                ngayGanNhat, // ngayGanNhat bao tri
                                noiDung,
                                donVi,
                                assignedId,
                                soHopDong,
                                ngayBatDau,
                                ngayKetThuc,
                                congTy
                        );

                        if (inserted) {
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

                                result.putString("assignedDriverId", String.valueOf(assignedId));
                                if (selectedDriverName != null && !selectedDriverName.isEmpty())
                                    result.putString("assignedDriverName", selectedDriverName);

                                getParentFragmentManager().setFragmentResult("vehicle_result", result);

                                Toast.makeText(requireContext(), (isEdit ? "Cập nhật" : "Thêm") + " xe thành công", Toast.LENGTH_SHORT).show();
                                if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
                            });
                        } else {
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

    private void loadDriversForDropdown() {
        new Thread(() -> {
            Database db = new Database(requireContext());
            List<String> names = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            try {
                // Tìm cả các biến thể có dấu & không dấu để tăng khả năng khớp
                android.database.Cursor cursor = db.getReadableDatabase().rawQuery(
                        "SELECT MaNguoiDung, HoTen FROM NguoiDung " +
                                "WHERE lower(COALESCE(VaiTro,'')) LIKE ? " +
                                "OR lower(COALESCE(VaiTro,'')) LIKE ? " +
                                "OR lower(COALESCE(VaiTro,'')) LIKE ? " +
                                "OR lower(COALESCE(VaiTro,'')) LIKE ?",
                        new String[]{"%nhân viên%", "%tài xế%", "%nhan vien%", "%tai xe%"}
                );

                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        int id = cursor.getInt(0);
                        String hoTen = cursor.getString(1);
                        if (hoTen == null || hoTen.trim().isEmpty()) hoTen = "Người dùng " + id;
                        names.add(hoTen);
                        ids.add(String.valueOf(id));
                    } while (cursor.moveToNext());
                    cursor.close();
                }
            } finally {
                db.close();
            }

            requireActivity().runOnUiThread(() -> {
                driverNames.clear();
                driverIds.clear();
                driverNames.addAll(names);
                driverIds.addAll(ids);

                if (actvAssignDriver != null) {
                    if (driverNames.isEmpty()) {
                        actvAssignDriver.setHint("Không có tài xế (nhân viên) nào");
                        actvAssignDriver.setEnabled(false);
                    } else {
                        actvAssignDriver.setEnabled(true);
                        driverAdapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_dropdown_item_1line,
                                driverNames
                        );
                        actvAssignDriver.setAdapter(driverAdapter);
                        actvAssignDriver.setThreshold(0);
                    }
                }
            });
        }).start();
    }

    private String getText(EditText edt) {
        return edt != null ? edt.getText().toString().trim() : "";
    }

    private boolean validateInputs() {
        if (edtBienSo == null || TextUtils.isEmpty(edtBienSo.getText())) {
            if (edtBienSo != null) {
                edtBienSo.setError("Vui lòng nhập biển số");
                edtBienSo.requestFocus();
            }
            return false;
        }
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
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
