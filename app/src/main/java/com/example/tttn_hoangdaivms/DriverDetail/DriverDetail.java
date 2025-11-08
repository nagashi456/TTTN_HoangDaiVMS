package com.example.tttn_hoangdaivms.DriverDetail;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.tttn_hoangdaivms.Database.Database;
import com.example.tttn_hoangdaivms.Profile.EditProfileFragment;
import com.example.tttn_hoangdaivms.R;

public class DriverDetail extends Fragment {

    // Bundle keys (cũ)
    public static final String ARG_NAME = "name";
    public static final String ARG_PHONE = "phone";
    public static final String ARG_EMAIL = "email";
    public static final String ARG_CCCD = "cccd";
    public static final String ARG_BIRTH = "birth";
    public static final String ARG_GENDER = "gender";
    public static final String ARG_ACTIVE = "active";
    public static final String ARG_WEIGHT = "weight";
    public static final String ARG_HEIGHT = "height";
    public static final String ARG_HEALTHNOTES = "healthNotes";
    public static final String ARG_LASTCHECKUP = "lastCheckup";
    public static final String ARG_DRUGTEST = "drugTest";
    public static final String ARG_HEALTHCONCLUSION = "healthConclusion";
    public static final String ARG_LICENSETYPE = "licenseType";
    public static final String ARG_ISSUEDATE = "issueDate";
    public static final String ARG_EXPIRYDATE = "expiryDate";
    public static final String ARG_ADDRESS = "address";

    // New key: id (MaNguoiDung) as String
    public static final String ARG_ID = "idNguoiDungStr";

    // Views
    private ImageView ivClose, btnEdit, ivCall, ivEmail;
    private TextView tvName, tvPhone, tvCccd,tvEmail, tvBirth, tvGender,tvActive;
    private TextView tvWeight, tvHeight, tvHealthNotes, tvLastCheckup, tvDrugTest, tvHealthConclusion;
    private TextView tvLicenseType, tvIssueDate, tvExpiryDate, tvAddress;

    // DB
    private Database dbHelper;

    // Data
    private String name, phone, email;

    // Colors
    private final int COLOR_HINT = Color.parseColor("#A0A0A0");
    private final int COLOR_NORMAL = Color.parseColor("#222222");

    /**
     * Existing factory (giữ để tương thích): newInstance với nhiều tham số
     */
    public static DriverDetail newInstance(@Nullable String name,
                                           @Nullable String phone,
                                           @Nullable String email,
                                           @Nullable String cccd,
                                           @Nullable String birth,
                                           @Nullable String gender,
                                           @Nullable String active,
                                           @Nullable String weight,
                                           @Nullable String height,
                                           @Nullable String healthNotes,
                                           @Nullable String lastCheckup,
                                           @Nullable String drugTest,
                                           @Nullable String healthConclusion,
                                           @Nullable String licenseType,
                                           @Nullable String issueDate,
                                           @Nullable String expiryDate,
                                           @Nullable String address) {
        DriverDetail frag = new DriverDetail();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putString(ARG_PHONE, phone);
        args.putString(ARG_EMAIL, email);
        args.putString(ARG_CCCD, cccd);
        args.putString(ARG_BIRTH, birth);
        args.putString(ARG_GENDER, gender);
        args.putString(ARG_ACTIVE, active);
        args.putString(ARG_WEIGHT, weight);
        args.putString(ARG_HEIGHT, height);
        args.putString(ARG_HEALTHNOTES, healthNotes);
        args.putString(ARG_LASTCHECKUP, lastCheckup);
        args.putString(ARG_DRUGTEST, drugTest);
        args.putString(ARG_HEALTHCONCLUSION, healthConclusion);
        args.putString(ARG_LICENSETYPE, licenseType);
        args.putString(ARG_ISSUEDATE, issueDate);
        args.putString(ARG_EXPIRYDATE, expiryDate);
        args.putString(ARG_ADDRESS, address);
        frag.setArguments(args);
        return frag;
    }

    /**
     * New factory: chỉ truyền id (String) — đúng theo yêu cầu của bạn.
     * Sử dụng khi bạn muốn fragment tự truy vấn DB theo MaNguoiDung.
     */
    public static DriverDetail newInstance(String idNguoiDungStr) {
        DriverDetail frag = new DriverDetail();
        Bundle args = new Bundle();
        args.putString(ARG_ID, idNguoiDungStr);
        frag.setArguments(args);
        return frag;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_detail, container, false);
        dbHelper = new Database(requireContext());

        // Ánh xạ view
        ivClose = view.findViewById(R.id.ivClose);
        btnEdit = view.findViewById(R.id.btnEdit);
        ivCall = view.findViewById(R.id.ivCall);
        ivEmail = view.findViewById(R.id.ivEmail);

        tvName = view.findViewById(R.id.tvName);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvCccd = view.findViewById(R.id.tvCccd);
        tvBirth = view.findViewById(R.id.tvBirth);
        tvGender = view.findViewById(R.id.tvGender);
        tvActive = view.findViewById(R.id.tvActive);

        tvWeight = view.findViewById(R.id.tvWeight);
        tvHeight = view.findViewById(R.id.tvHeight);
        tvHealthNotes = view.findViewById(R.id.tvHealthNotes);
        tvLastCheckup = view.findViewById(R.id.tvLastCheckup);
        tvDrugTest = view.findViewById(R.id.tvDrugTest);
        tvHealthConclusion = view.findViewById(R.id.tvHealthConclusion);

        tvLicenseType = view.findViewById(R.id.tvLicenseType);
        tvIssueDate = view.findViewById(R.id.tvIssueDate);
        tvExpiryDate = view.findViewById(R.id.tvExpiryDate);
        tvAddress = view.findViewById(R.id.tvAddress);

        // Lấy args
        Bundle args = getArguments();
        String idArg = null;
        if (args != null) {
            // Nếu có id => dùng id và query DB
            if (args.containsKey(ARG_ID)) {
                idArg = args.getString(ARG_ID);
            } else {
                name = args.getString(ARG_NAME);
                phone = args.getString(ARG_PHONE);
                email = args.getString(ARG_EMAIL);

                setTextOrHint(tvName, args.getString(ARG_NAME), "Chưa có tên");
                setTextOrHint(tvPhone, args.getString(ARG_PHONE), "Chưa có số điện thoại");
                setTextOrHint(tvEmail, args.getString(ARG_EMAIL), "—");
                setTextOrHint(tvCccd, args.getString(ARG_CCCD), "—");
                setTextOrHint(tvBirth, args.getString(ARG_BIRTH), "—");
                setTextOrHint(tvGender, args.getString(ARG_GENDER), "—");
                setTextOrHint(tvActive, args.getString(ARG_ACTIVE), "—");

                setTextOrHint(tvWeight, args.getString(ARG_WEIGHT), "—");
                setTextOrHint(tvHeight, args.getString(ARG_HEIGHT), "—");
                setTextOrHint(tvHealthNotes, args.getString(ARG_HEALTHNOTES), "—");
                setTextOrHint(tvLastCheckup, args.getString(ARG_LASTCHECKUP), "—");
                setTextOrHint(tvDrugTest, args.getString(ARG_DRUGTEST), "—");
                setTextOrHint(tvHealthConclusion, args.getString(ARG_HEALTHCONCLUSION), "—");

                setTextOrHint(tvLicenseType, args.getString(ARG_LICENSETYPE), "—");
                setTextOrHint(tvIssueDate, args.getString(ARG_ISSUEDATE), "—");
                setTextOrHint(tvExpiryDate, args.getString(ARG_EXPIRYDATE), "—");
                setTextOrHint(tvAddress, args.getString(ARG_ADDRESS), "—");
            }
        }

        // Nếu có id -> load theo id; else nếu thiếu dữ liệu và có email -> load theo email
        if (!isEmptyString(idArg)) {
            loadFromDatabaseById(idArg);
        } else if ((isEmptyString(name) || isEmptyString(phone)) && !isEmptyString(email)) {
            loadFromDatabaseByEmail(email);
        }

        // Listeners
        ivClose.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                requireActivity().finish();
            }
        });

        ivCall.setOnClickListener(v -> doCall());
        tvPhone.setOnClickListener(v -> doCall());

        ivEmail.setOnClickListener(v -> doCopyEmail());
        tvEmail.setOnClickListener(v -> doCopyEmail());

        btnEdit.setOnClickListener(v -> {
            if (isEmptyString(email)) {
                Toast.makeText(requireContext(), "Không có email để chỉnh sửa", Toast.LENGTH_SHORT).show();
                return;
            }
            EditProfileFragment edit = new EditProfileFragment();
            Bundle b = new Bundle();
            b.putString("email", email);
            edit.setArguments(b);
            FragmentManager fm = getParentFragmentManager();
            fm.beginTransaction()
                    .replace(R.id.containerMain, edit)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    // Mở dialer
    private void  doCall() {
        // Lấy trực tiếp text từ TextView (đảm bảo luôn lấy giá trị hiển thị hiện tại)
        String phoneText = null;
        if (tvPhone != null && tvPhone.getText() != null) {
            phoneText = tvPhone.getText().toString().trim();
        }
        // fallback sang biến member nếu cần
        if ((phoneText == null || phoneText.isEmpty() || phoneText.equals("—")) && phone != null) {
            phoneText = phone.trim();
        }

        if (phoneText == null || phoneText.isEmpty() || phoneText.equals("—")) {
            Toast.makeText(requireContext(), "Không có số điện thoại", Toast.LENGTH_SHORT).show();
            return;
        }

        // Làm sạch: chỉ giữ chữ số và dấu + (ví dụ +8490... hoặc 090...)
        String phoneClean = phoneText.replaceAll("[^0-9+]", "");
        if (phoneClean.isEmpty()) {
            Toast.makeText(requireContext(), "Số điện thoại không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo intent dial
        try {
            Intent dial = new Intent(Intent.ACTION_DIAL);
            // encode để tránh ký tự lạ làm hỏng URI
            dial.setData(Uri.parse("tel:" + Uri.encode(phoneClean)));

            // kiểm tra có Activity xử lý không trước khi start
            if (dial.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(dial);
            } else {
                Toast.makeText(requireContext(), "Không tìm thấy ứng dụng gọi điện", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Không thể mở ứng dụng gọi điện", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    // Copy email
    private void doCopyEmail() {
        // 1) Lấy trực tiếp từ TextView trước (đảm bảo lấy giá trị hiển thị hiện tại)
        String emailText = null;
        if (tvEmail != null && tvEmail.getText() != null) {
            emailText = tvEmail.getText().toString().trim();
        }

        // 2) Nếu TextView hiển thị hint như "—" hoặc rỗng, fallback sang biến member email
        if (emailText == null || emailText.isEmpty() || emailText.equals("—")) {
            if (email != null && !email.trim().isEmpty() && !email.trim().equals("—")) {
                emailText = email.trim();
            }
        }

        // 3) Kiểm tra hợp lệ
        if (emailText == null || emailText.isEmpty() || emailText.equals("—")) {
            Toast.makeText(requireContext(), "Không có email để sao chép", Toast.LENGTH_SHORT).show();
            return;
        }

        // 4) (Tuỳ chọn) kiểm tra định dạng cơ bản của email (có @)
        if (!emailText.contains("@")) {
            // Không bắt buộc nhưng cảnh báo người dùng
            Toast.makeText(requireContext(), "Email có vẻ không hợp lệ: " + emailText, Toast.LENGTH_SHORT).show();
            // vẫn cho phép copy nếu bạn muốn -> không return
        }

        // 5) Copy vào clipboard
        try {
            ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null) {
                Toast.makeText(requireContext(), "Không thể truy cập clipboard", Toast.LENGTH_SHORT).show();
                return;
            }
            ClipData clip = ClipData.newPlainText("email", emailText);
            cm.setPrimaryClip(clip);
            Toast.makeText(requireContext(), "Đã sao chép email: " + emailText, Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            Toast.makeText(requireContext(), "Lỗi khi sao chép email", Toast.LENGTH_SHORT).show();
            ex.printStackTrace();
        }
    }


    /**
     * Load thông tin bằng id MaNguoiDung (ARG_ID)
     */
    private void loadFromDatabaseById(String idStr) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = null;
        try {
            // NguoiDung + Email
            c = db.rawQuery(
                    "SELECT ND.HoTen, ND.SDT, TK.Email, ND.CCCD, ND.NgaySinh, ND.GioiTinh, ND.TrangThai " +
                            "FROM NguoiDung ND " +
                            "JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan " +
                            "WHERE ND.MaNguoiDung = ?",
                    new String[]{ idStr }
            );
            if (c != null && c.moveToFirst()) {
                String hoTen = getColumnSafe(c, "HoTen", 0);
                String sdt = getColumnSafe(c, "SDT", 1);
                String em = getColumnSafe(c, "Email", 2);
                String cccd = getColumnSafe(c, "CCCD", 3);
                String ngaySinh = getColumnSafe(c, "NgaySinh", 4);
                String gioiTinh = getColumnSafe(c, "GioiTinh", 5);
                String active = getColumnSafe(c, "TrangThai",6);
//                name = notEmptyOrDefault(hoTen, null);
//                phone = notEmptyOrDefault(sdt, null);
//                email = notEmptyOrDefault(em, null);

                setTextOrHint(tvName, hoTen, "—");
                setTextOrHint(tvPhone, sdt, "—");
                setTextOrHint(tvEmail, em, "—");
                setTextOrHint(tvCccd, cccd, "—");
                setTextOrHint(tvBirth, ngaySinh, "—");
                setTextOrHint(tvGender, gioiTinh, "—");
                setTextOrHint(tvActive,active,"—");
            }
            if (c != null) { c.close(); c = null; }

            // SucKhoe (mới nhất)
            c = db.rawQuery(
                    "SELECT ChieuCao, CanNang, BenhNen, MaTuy, NgayKham, KetLuan " +
                            "FROM SucKhoe WHERE MaNguoiDung = ? ORDER BY NgayKham DESC LIMIT 1",
                    new String[]{ idStr }
            );
            if (c != null && c.moveToFirst()) {
                String chieuCao = getColumnSafe(c, "ChieuCao", 0);
                String canNang = getColumnSafe(c, "CanNang", 1);
                String benhNen = getColumnSafe(c, "BenhNen", 2);
                String maTuy = getColumnSafe(c, "MaTuy", 3);
                String ngayKham = getColumnSafe(c, "NgayKham", 4);
                String ketLuan = getColumnSafe(c, "KetLuan", 5);

                if (!isEmptyString(canNang)) setNormalStyle(tvWeight, formatNumberOrDefault(canNang, "kg"));
                if (!isEmptyString(chieuCao)) setNormalStyle(tvHeight, formatNumberOrDefault(chieuCao, "cm"));
                if (!isEmptyString(benhNen)) setNormalStyle(tvHealthNotes, benhNen);
                if (!isEmptyString(ngayKham)) setNormalStyle(tvLastCheckup, ngayKham);
                if (!isEmptyString(maTuy)) setNormalStyle(tvDrugTest, mapMaTuyToLabel(maTuy));
                if (!isEmptyString(ketLuan)) setNormalStyle(tvHealthConclusion, ketLuan);
            } else {
                // nếu ko có record, set hint cho các field sức khỏe
                if (tvWeight != null) setHintStyle(tvWeight, "—");
                if (tvHeight != null) setHintStyle(tvHeight, "—");
                if (tvHealthNotes != null) setHintStyle(tvHealthNotes, "—");
                if (tvLastCheckup != null) setHintStyle(tvLastCheckup, "—");
                if (tvDrugTest != null) setHintStyle(tvDrugTest, "—");
                if (tvHealthConclusion != null) setHintStyle(tvHealthConclusion, "—");
            }
            if (c != null) { c.close(); c = null; }

            // BangCap (mới nhất)
            c = db.rawQuery(
                    "SELECT Loai, NgayCap, NgayHetHan, NoiCap FROM BangCap WHERE MaNguoiDung = ? ORDER BY MaBangCap DESC LIMIT 1",
                    new String[]{ idStr }
            );
            if (c != null && c.moveToFirst()) {
                String loai = getColumnSafe(c, "Loai", 0);
                String ngayCap = getColumnSafe(c, "NgayCap", 1);
                String ngayHet = getColumnSafe(c, "NgayHetHan", 2);
                String noiCap = getColumnSafe(c, "NoiCap", 3);

                if (!isEmptyString(loai)) setNormalStyle(tvLicenseType, loai);
                if (!isEmptyString(ngayCap)) setNormalStyle(tvIssueDate, ngayCap);
                if (!isEmptyString(ngayHet)) setNormalStyle(tvExpiryDate, ngayHet);
                if (!isEmptyString(noiCap)) setNormalStyle(tvAddress, noiCap);
            } else {
                if (tvLicenseType != null) setHintStyle(tvLicenseType, "—");
                if (tvIssueDate != null) setHintStyle(tvIssueDate, "—");
                if (tvExpiryDate != null) setHintStyle(tvExpiryDate, "—");
                if (tvAddress != null) setHintStyle(tvAddress, "—");
            }
        } finally {
            if (c != null) c.close();
        }

        // setup click actions (phone/email might have been updated)
        setupContactActionListeners();
    }

    /**
     * Load theo Email (giữ cho tương thích)
     */
    private void loadFromDatabaseByEmail(String emailParam) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = null;
        try {
            // NguoiDung
            c = db.rawQuery(
                    "SELECT ND.HoTen, ND.CCCD, ND.SDT, ND.NgaySinh, ND.GioiTinh " +
                            "FROM NguoiDung ND " +
                            "JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan " +
                            "WHERE TK.Email = ?",
                    new String[]{ emailParam }
            );

            if (c != null && c.moveToFirst()) {
                String hoTen = getColumnSafe(c, "HoTen", 0);
                String cccd = getColumnSafe(c, "CCCD", 1);
                String sdt = getColumnSafe(c, "SDT", 2);
                String ngaySinh = getColumnSafe(c, "NgaySinh", 3);
                String gioiTinh = getColumnSafe(c, "GioiTinh", 4);

                if (isEmptyString(name) && !isEmptyString(hoTen)) {
                    name = hoTen;
                    setNormalStyle(tvName, hoTen);
                }
                if (isEmptyString(phone) && !isEmptyString(sdt)) {
                    phone = sdt;
                    setNormalStyle(tvPhone, sdt);
                }
                if (isEmptyString(getText(tvCccd)) && !isEmptyString(cccd)) {
                    setNormalStyle(tvCccd, cccd);
                }
                if (isEmptyString(getText(tvBirth)) && !isEmptyString(ngaySinh)) {
                    setNormalStyle(tvBirth, ngaySinh);
                }
                if (isEmptyString(getText(tvGender)) && !isEmptyString(gioiTinh)) {
                    setNormalStyle(tvGender, gioiTinh);
                }
            }
            if (c != null) { c.close(); c = null; }

            // SucKhoe
            c = db.rawQuery(
                    "SELECT SK.ChieuCao, SK.CanNang, SK.BenhNen, SK.MaTuy, SK.NgayKham, SK.KetLuan " +
                            "FROM SucKhoe SK " +
                            "JOIN NguoiDung ND ON SK.MaNguoiDung = ND.MaNguoiDung " +
                            "JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan " +
                            "WHERE TK.Email = ? " +
                            "ORDER BY SK.NgayKham DESC LIMIT 1",
                    new String[]{ emailParam }
            );

            if (c != null && c.moveToFirst()) {
                String chieuCao = getColumnSafe(c, "ChieuCao", 0);
                String canNang = getColumnSafe(c, "CanNang", 1);
                String benhNen = getColumnSafe(c, "BenhNen", 2);
                String maTuy = getColumnSafe(c, "MaTuy", 3);
                String ngayKham = getColumnSafe(c, "NgayKham", 4);
                String ketLuan = getColumnSafe(c, "KetLuan", 5);

                if (!isEmptyString(canNang)) setNormalStyle(tvWeight, formatNumberOrDefault(canNang, "kg"));
                if (!isEmptyString(chieuCao)) setNormalStyle(tvHeight, formatNumberOrDefault(chieuCao, "cm"));
                if (!isEmptyString(benhNen)) setNormalStyle(tvHealthNotes, benhNen);
                if (!isEmptyString(ngayKham)) setNormalStyle(tvLastCheckup, ngayKham);
                if (!isEmptyString(maTuy)) setNormalStyle(tvDrugTest, mapMaTuyToLabel(maTuy));
                if (!isEmptyString(ketLuan)) setNormalStyle(tvHealthConclusion, ketLuan);
            }
            if (c != null) { c.close(); c = null; }

            // BangCap
            c = db.rawQuery(
                    "SELECT BC.Loai, BC.NgayCap, BC.NgayHetHan, BC.NoiCap " +
                            "FROM BangCap BC " +
                            "JOIN NguoiDung ND ON BC.MaNguoiDung = ND.MaNguoiDung " +
                            "JOIN TaiKhoan TK ON ND.MaTaiKhoan = TK.MaTaiKhoan " +
                            "WHERE TK.Email = ? " +
                            "ORDER BY BC.MaBangCap DESC LIMIT 1",
                    new String[]{ emailParam }
            );

            if (c != null && c.moveToFirst()) {
                String loai = getColumnSafe(c, "Loai", 0);
                String ngayCap = getColumnSafe(c, "NgayCap", 1);
                String ngayHet = getColumnSafe(c, "NgayHetHan", 2);
                String noiCap = getColumnSafe(c, "NoiCap", 3);

                if (!isEmptyString(loai)) setNormalStyle(tvLicenseType, loai);
                if (!isEmptyString(ngayCap)) setNormalStyle(tvIssueDate, ngayCap);
                if (!isEmptyString(ngayHet)) setNormalStyle(tvExpiryDate, ngayHet);
                if (!isEmptyString(noiCap)) setNormalStyle(tvAddress, noiCap);
            }
        } finally {
            if (c != null) c.close();
        }

        // setup contact listeners
        setupContactActionListeners();
    }

    // Setup ivCall / ivEmail listeners after phone/email are set
    private void setupContactActionListeners() {
        ivCall.setOnClickListener(v -> doCall());
        tvPhone.setOnClickListener(v -> doCall());

        ivEmail.setOnClickListener(v -> doCopyEmail());
        tvEmail.setOnClickListener(v -> doCopyEmail());
    }

    // Helpers
    private void setTextOrHint(TextView tv, String value, String hint) {
        if (tv == null) return;
        if (!isEmptyString(value)) {
            setNormalStyle(tv, value.trim());
        } else {
            setHintStyle(tv, hint);
        }
    }

    private void setHintStyle(TextView tv, String hint) {
        if (tv == null) return;
        tv.setText(hint);
        tv.setTextColor(COLOR_HINT);
    }

    private void setNormalStyle(TextView tv, String text) {
        if (tv == null) return;
        tv.setText(text);
        tv.setTextColor(COLOR_NORMAL);
    }

    private boolean isEmptyString(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String getText(TextView tv) {
        if (tv == null) return null;
        CharSequence cs = tv.getText();
        return cs == null ? null : cs.toString();
    }

    private String getColumnSafe(Cursor c, String colName, int fallbackIndex) {
        try {
            int idx = c.getColumnIndex(colName);
            if (idx >= 0) {
                String s = c.getString(idx);
                return s != null ? s : "";
            } else {
                String s = c.getString(fallbackIndex);
                return s != null ? s : "";
            }
        } catch (Exception e) {
            try {
                String s = c.getString(fallbackIndex);
                return s != null ? s : "";
            } catch (Exception ex) {
                return "";
            }
        }
    }

    private String formatNumberOrDefault(String raw, String unit) {
        if (raw == null || raw.trim().isEmpty()) return "—";
        try {
            double v = Double.parseDouble(raw);
            if (Math.abs(v - Math.round(v)) < 0.0001) {
                return String.format("%d %s", Math.round(v), unit);
            } else {
                return String.format("%.1f %s", v, unit);
            }
        } catch (Exception e) {
            return raw + " " + unit;
        }
    }

    private String mapMaTuyToLabel(String maTuyStr) {
        if (maTuyStr == null) return "—";
        try {
            int code = Integer.parseInt(maTuyStr);
            switch (code) {
                case 0: return "Âm tính";
                case 1: return "Dương tính";
                default: return "Mã: " + code;
            }
        } catch (NumberFormatException e) {
            return maTuyStr;
        }
    }
}
