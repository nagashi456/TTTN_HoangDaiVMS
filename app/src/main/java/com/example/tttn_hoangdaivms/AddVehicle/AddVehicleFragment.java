package com.example.tttn_hoangdaivms.AddVehicle;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tttn_hoangdaivms.Database.Database;
import com.example.tttn_hoangdaivms.Database.User;
import com.example.tttn_hoangdaivms.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

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
    private final List<String> driverIds = new ArrayList<>(); // parallel list

    private boolean isEdit = false;
    private String vehicleId = null;

    private static final String PREFS_NAME = "user_prefs";
    private static final String PREF_KEY_EMAIL = "email";

    // Validation helpers
    private static final Pattern PLATE_ALLOWED = Pattern.compile("^[A-Za-z0-9\\s-]+$");
    private static final int PLATE_MAX_LENGTH = 50;
    private static final SimpleDateFormat UI_DATE_FORMAT = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());

    public AddVehicleFragment() { /* required */ }

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
        edtNhienLieu = view.findViewById(R.id.edtNhienLieu); // fuel

        edtSoHopDong = view.findViewById(R.id.edtSoHopDong);
        edtNgayBatDau = view.findViewById(R.id.edtNgayBatDau);
        edtNgayKetThuc = view.findViewById(R.id.edtNgayKetThuc);
        edtCongTyBH = view.findViewById(R.id.edtCongTyBH);

        edtNoiDung = view.findViewById(R.id.edtNoiDung);
        edtNgayGanNhat = view.findViewById(R.id.edtNgayGanNhat);
        edtDonViThucHien = view.findViewById(R.id.edtDonViThucHien);

        btnSave = view.findViewById(R.id.btnSave);
        ivBack = view.findViewById(R.id.ivBack);

        actvAssignDriver = view.findViewById(R.id.actvAssignDriver);

        readArgumentsAndPopulate();

        // Date pickers: set InputType none and open DatePicker on click
        prepareDatePickers();

        loadDriversForDropdown();

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

    private void prepareDatePickers() {
        // ensure edittexts open date picker and don't show keyboard
        if (edtNgayBatDau != null) {
            edtNgayBatDau.setInputType(InputType.TYPE_NULL);
            edtNgayBatDau.setOnClickListener(v -> showDatePicker(edtNgayBatDau));
            edtNgayBatDau.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) showDatePicker(edtNgayBatDau); });
        }
        if (edtNgayKetThuc != null) {
            edtNgayKetThuc.setInputType(InputType.TYPE_NULL);
            edtNgayKetThuc.setOnClickListener(v -> showDatePicker(edtNgayKetThuc));
            edtNgayKetThuc.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) showDatePicker(edtNgayKetThuc); });
        }
        if (edtNgayGanNhat != null) {
            edtNgayGanNhat.setInputType(InputType.TYPE_NULL);
            edtNgayGanNhat.setOnClickListener(v -> showDatePicker(edtNgayGanNhat));
            edtNgayGanNhat.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) showDatePicker(edtNgayGanNhat); });
        }
    }

    private void setupListeners() {
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> {
                hideKeyboard();
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }

        if (actvAssignDriver != null) {
            actvAssignDriver.setThreshold(0);
            actvAssignDriver.setOnClickListener(v -> {
                if (driverAdapter != null && driverNames.size() > 0) {
                    actvAssignDriver.showDropDown();
                } else {
                    setFieldErrorAbove(actvAssignDriver, "Không có tài xế để chọn");
                }
            });
            actvAssignDriver.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && driverAdapter != null && driverNames.size() > 0) actvAssignDriver.showDropDown();
            });
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                hideKeyboard();
                // clear previous inline errors
                if (edtBienSo != null) removeFieldErrorAbove(edtBienSo);
                if (edtNgayBatDau != null) removeFieldErrorAbove(edtNgayBatDau);
                if (edtNgayKetThuc != null) removeFieldErrorAbove(edtNgayKetThuc);
                if (edtNgayGanNhat != null) removeFieldErrorAbove(edtNgayGanNhat);
                if (actvAssignDriver != null) removeFieldErrorAbove(actvAssignDriver);

                // run validation; if invalid, errors already shown above fields
                if (!validateInputs()) return;

                // proceed with save (existing logic)
                final String selectedDriverName = actvAssignDriver == null ? null : actvAssignDriver.getText().toString().trim();

                final String bienSo = getText(edtBienSo);
                final String loaiXe = getText(edtLoaiXe);
                final String hangSX = getText(edtHangSX);
                final String mauSac = getText(edtMauSac);
                final String soHieu = getText(edtSoHieuLop);
                final String nhienLieu = getText(edtNhienLieu);
                final String soHopDong = getText(edtSoHopDong);
                final String ngayBatDau = getText(edtNgayBatDau);
                final String ngayKetThuc = getText(edtNgayKetThuc);
                final String congTy = getText(edtCongTyBH);
                final String noiDung = getText(edtNoiDung);
                final String ngayGanNhat = getText(edtNgayGanNhat);
                final String donVi = getText(edtDonViThucHien);

                // lấy email hiện tại
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
                        int maTaiXeToAssign = -1; // -1 => not set (will fallback)
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

                        // if user left driver empty (maTaiXeToAssign == -1) => fallback to current user if available
                        if (maTaiXeToAssign == -1 && finalCurrentEmail != null) {
                            User currentUser = db.getUserByEmail(finalCurrentEmail);
                            if (currentUser != null) maTaiXeToAssign = currentUser.getMaNguoiDung();
                        }

                        // if maTaiXeToAssign still -1 => no assignment and no fallback, we treat as error
                        // BUT if user explicitly chose "Không gán" we set maTaiXeToAssign == 0 (means explicit no-assign)
                        // Note: we added "Không gán" with id "0" in loadDriversForDropdown()
                        // So maTaiXeToAssign == 0 means user selected explicit "Không gán"
                        if (maTaiXeToAssign == -1) {
                            final int finalVal = maTaiXeToAssign;
                            requireActivity().runOnUiThread(() ->
                                    setFieldErrorAbove(actvAssignDriver, "Vui lòng chọn tài xế để gán (hoặc chọn 'Không gán')")
                            );
                            return;
                        }

                        final int assignedId = maTaiXeToAssign; // may be >0 (real id) or 0 (explicit no-assign)

                        final Double soKmTong = null;
                        final String trangThaiXe = "";

                        boolean inserted = db.insertXeWithBaoTriAndBaoHiem(
                                bienSo,
                                loaiXe,
                                hangSX,
                                mauSac,
                                soHieu,
                                nhienLieu,
                                soKmTong,
                                trangThaiXe,
                                ngayGanNhat,
                                noiDung,
                                donVi,
                                assignedId, // pass 0 if explicit no-assign; DB should handle 0 as null if needed
                                soHopDong,
                                congTy,
                                ngayBatDau,
                                ngayKetThuc
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
                android.database.Cursor cursor = db.getDriversCursor();
                if (cursor == null) {
                    cursor = db.getReadableDatabase().rawQuery(
                            "SELECT MaNguoiDung, HoTen FROM NguoiDung " +
                                    "WHERE lower(COALESCE(VaiTro,'')) LIKE ? " +
                                    "OR lower(COALESCE(VaiTro,'')) LIKE ? " +
                                    "OR lower(COALESCE(VaiTro,'')) LIKE ? " +
                                    "OR lower(COALESCE(VaiTro,'')) LIKE ?",
                            new String[]{"%nhân viên%", "%tài xế%", "%nhan vien%", "%tai xe%"}
                    );
                }

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

                // add explicit "Không gán" option at the top with id = "0"
                driverNames.add("Không gán");
                driverIds.add("0");

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

    // ---------------- inline-error helpers (show TextView above EditText / View) ----------------

    private void setFieldErrorAbove(final EditText editText, final String message) {
        if (editText == null) return;
        requireActivity().runOnUiThread(() -> {
            ViewParent p = editText.getParent();
            if (!(p instanceof ViewGroup)) return;
            ViewGroup row = (ViewGroup) p;

            ViewParent gp = row.getParent();
            ViewGroup container = (gp instanceof ViewGroup) ? (ViewGroup) gp : row;

            String tag = "error_above_" + editText.getId();
            View existing = container.findViewWithTag(tag);
            if (existing instanceof TextView) {
                ((TextView) existing).setText(message);
                existing.setVisibility(View.VISIBLE);
                return;
            }

            TextView tv = new TextView(requireContext());
            tv.setTag(tag);
            tv.setText(message);
            tv.setTextColor(Color.parseColor("#D32F2F"));
            tv.setTextSize(12);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 6, 0, 6);

            int insertIdx = -1;
            if (container != null) {
                try {
                    insertIdx = container.indexOfChild(row);
                } catch (Exception ignored) { insertIdx = -1; }
            }
            if (insertIdx >= 0) container.addView(tv, insertIdx, lp);
            else {
                int idx = row.indexOfChild(editText);
                if (idx >= 0) row.addView(tv, idx, lp);
                else row.addView(tv, lp);
            }
        });
    }

    private void removeFieldErrorAbove(final EditText editText) {
        if (editText == null) return;
        requireActivity().runOnUiThread(() -> {
            ViewParent p = editText.getParent();
            if (!(p instanceof ViewGroup)) return;
            ViewGroup row = (ViewGroup) p;
            ViewParent gp = row.getParent();
            ViewGroup container = (gp instanceof ViewGroup) ? (ViewGroup) gp : row;

            String tag = "error_above_" + editText.getId();
            View existing = container.findViewWithTag(tag);
            if (existing != null) container.removeView(existing);
        });
    }

    /**
     * Validate inputs and show inline errors above fields.
     * Returns true if all valid.
     */
    private boolean validateInputs() {
        boolean ok = true;

        // Plate required
        String plate = getText(edtBienSo);
        if (TextUtils.isEmpty(plate)) {
            setFieldErrorAbove(edtBienSo, "Vui lòng điền biển số xe");
            ok = false;
        } else {
            if (plate.length() > PLATE_MAX_LENGTH || !PLATE_ALLOWED.matcher(plate).matches()) {
                setFieldErrorAbove(edtBienSo, "Định dạng biển số không hợp lệ");
                ok = false;
            }
        }

        // Driver selection:
        // If user chooses "Không gán" (id==0) it's fine.
        // If user typed empty -> we'll fallback to current user later.
        // But if user typed something not in list -> error.
        String selDriver = actvAssignDriver == null ? "" : actvAssignDriver.getText().toString().trim();
        if (!TextUtils.isEmpty(selDriver)) {
            boolean found = false;
            synchronized (driverNames) {
                for (String n : driverNames) {
                    if (n.equals(selDriver)) { found = true; break; }
                }
            }
            if (!found) {
                setFieldErrorAbove(actvAssignDriver, "Vui lòng chọn một tài xế");
                ok = false;
            }
        }

        // Date comparisons
        String sStart = getText(edtNgayBatDau);
        String sEnd = getText(edtNgayKetThuc);
        Date start = parseDateSafe(sStart);
        Date end = parseDateSafe(sEnd);
        if (!TextUtils.isEmpty(sStart) && !TextUtils.isEmpty(sEnd)) {
            if (start == null) { setFieldErrorAbove(edtNgayBatDau, "Ngày bắt đầu không hợp lệ"); ok = false; }
            if (end == null) { setFieldErrorAbove(edtNgayKetThuc, "Ngày kết thúc không hợp lệ"); ok = false; }
            if (start != null && end != null) {
                if (removeTime(start).after(removeTime(end))) {
                    setFieldErrorAbove(edtNgayBatDau, "Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc");
                    setFieldErrorAbove(edtNgayKetThuc, "Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu");
                    ok = false;
                }
            }
        }

        // Recent date must not be future
        String sRecent = getText(edtNgayGanNhat);
        if (!TextUtils.isEmpty(sRecent)) {
            Date recent = parseDateSafe(sRecent);
            if (recent == null) { setFieldErrorAbove(edtNgayGanNhat, "Ngày gần nhất không hợp lệ"); ok = false; }
            else if (removeTime(recent).after(removeTime(new Date()))) {
                setFieldErrorAbove(edtNgayGanNhat, "Ngày gần nhất không được là ngày tương lai");
                ok = false;
            }
        }

        return ok;
    }

    private Date parseDateSafe(String s) {
        if (TextUtils.isEmpty(s)) return null;
        try {
            SimpleDateFormat sdf1 = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
            sdf1.setLenient(false);
            return sdf1.parse(s);
        } catch (ParseException ignored) { }
        try {
            SimpleDateFormat sdf2 = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            sdf2.setLenient(false);
            return sdf2.parse(s);
        } catch (ParseException ignored) { }
        return null;
    }

    private Date removeTime(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private void showDatePicker(final EditText target) {
        final Calendar c = Calendar.getInstance();
        String curText = target.getText() == null ? "" : target.getText().toString().trim();
        Date parsed = parseDateSafe(curText);
        if (parsed != null) c.setTime(parsed);

        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH);
        int d = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String date = dayOfMonth + "/" + (month + 1) + "/" + year;
                    target.setText(date);
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
