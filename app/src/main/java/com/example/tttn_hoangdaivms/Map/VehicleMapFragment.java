package com.example.tttn_hoangdaivms.Map;

import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.database.Cursor;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import com.example.tttn_hoangdaivms.Database.Database;
import com.example.tttn_hoangdaivms.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * VehicleMapFragment: uses custom car icons and animates moving vehicles.
 *
 * Required drawables (put in res/drawable):
 *  - ic_car_blue.xml / png
 *  - ic_car_red.xml
 *  - ic_car_pink.xml
 *  - ic_car_green.xml
 *
 * (We'll also reuse existing bg/search icons from previous steps.)
 */
public class VehicleMapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "VehicleMapFragment";
    // cache cho icon (key = status.name() + "|" + plate)
    private final Map<String, BitmapDescriptor> iconCache = new HashMap<>();

    private MapView mapView;
    private GoogleMap mMap;
    private Database db;

    private EditText etSearch;
    private ImageView ivFilter;
    private Handler refreshHandler = new Handler();
    private final long REFRESH_INTERVAL_MS = 5000;

    // markers keyed by MaXe
    private final Map<Integer, Marker> markers = new HashMap<>();
    // cache last known VehiclePoint (used to animate from -> to)
    private final Map<Integer, VehiclePoint> vehicleCache = new HashMap<>();

    private boolean filterMoving = true, filterStopped = true, filterParked = true, filterNoGps = true;
    private String searchQuery = "";

    private final SimpleDateFormat isoParse = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    // UI detail card
    private CardView cardDetail;
    private ImageView ivDetailClose;
    private android.widget.TextView tvDetailContent, tvDetailTitle;

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadAndUpdateMarkers();
            refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

    public VehicleMapFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_vehicle_map, container, false);

        // programmatically create MapView into container
        mapView = new MapView(requireContext());
        ViewGroup mapContainer = v.findViewById(R.id.map_container);
        mapContainer.addView(mapView, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        etSearch = v.findViewById(R.id.etSearch);
        ivFilter = v.findViewById(R.id.ivFilter);

        cardDetail = v.findViewById(R.id.card_vehicle_detail);
        ivDetailClose = v.findViewById(R.id.ivDetailClose);
        tvDetailContent = v.findViewById(R.id.tvDetailContent);
        tvDetailTitle = v.findViewById(R.id.tvDetailTitle);

        db = new Database(requireContext());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                searchQuery = s.toString().trim().toLowerCase();
                loadAndUpdateMarkers();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        ivFilter.setOnClickListener(v1 -> showFilterDialog());

        ivDetailClose.setOnClickListener(v1 -> cardDetail.setVisibility(View.GONE));

        return v;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        MapsInitializer.initialize(requireContext());
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(16.0, 108.0), 6f));

        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Integer) {
                int maXe = (Integer) tag;
                VehiclePoint vp = vehicleCache.get(maXe);
                if (vp != null) {
                    showDetailCard(vp);
                    if (!Double.isNaN(vp.lat) && !Double.isNaN(vp.lon)) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(vp.lat, vp.lon)));
                    }
                }
            }
            return true; // consume
        });

        refreshHandler.post(refreshRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        refreshHandler.post(refreshRunnable);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mapView != null) mapView.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null) {
            mapView.onDestroy();
            mapView = null;
        }
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }

    // ----------------- DB read & update -----------------
    private void loadAndUpdateMarkers() {
        new Thread(() -> {
            String sql =
                    "SELECT X.MaXe, X.BienSo, X.LoaiXe, X.SoHieu, N.HoTen, " +
                            "T.ThoiGian, T.Lat, T.Lon, T.TocDo, T.ViTri, X.NhienLieu, X.SoKmTong, X.TrangThai " +
                            "FROM Xe X " +
                            "LEFT JOIN NguoiDung N ON X.MaNguoiDung = N.MaNguoiDung " +
                            "LEFT JOIN Telemetry T ON X.MaXe = T.MaXe " +
                            "AND T.ThoiGian = (SELECT MAX(ThoiGian) FROM Telemetry WHERE MaXe = X.MaXe)";

            Cursor c = null;
            final List<VehiclePoint> list = new ArrayList<>();
            try {
                c = db.getReadableDatabase().rawQuery(sql, null);
                if (c != null && c.moveToFirst()) {
                    do {
                        int maXe;
                        try { maXe = c.getInt(c.getColumnIndex("MaXe")); }
                        catch (Exception ex) { maXe = c.getInt(0); }

                        String bienSo = safeGet(c, "BienSo");
                        String loai = safeGet(c, "LoaiXe");
                        String vin = safeGet(c, "SoHieu");
                        String tenLai = safeGet(c, "HoTen");
                        String thoiGian = safeGet(c, "ThoiGian");

                        double lat = Double.NaN, lon = Double.NaN, tocDo = 0.0, soKmTong = 0.0;
                        try { int idxLat = c.getColumnIndex("Lat"); if (idxLat >= 0 && !c.isNull(idxLat)) lat = c.getDouble(idxLat); } catch (Exception ignored) {}
                        try { int idxLon = c.getColumnIndex("Lon"); if (idxLon >= 0 && !c.isNull(idxLon)) lon = c.getDouble(idxLon); } catch (Exception ignored) {}
                        try { int idxT = c.getColumnIndex("TocDo"); if (idxT >= 0 && !c.isNull(idxT)) tocDo = c.getDouble(idxT); } catch (Exception ignored) {}
                        try { int idxKm = c.getColumnIndex("SoKmTong"); if (idxKm >= 0 && !c.isNull(idxKm)) soKmTong = c.getDouble(idxKm); } catch (Exception ignored) {}

                        String viTri = safeGet(c, "ViTri");
                        String nhienLieu = safeGet(c, "NhienLieu");
                        String trangThaiXe = safeGet(c, "TrangThai");

                        VehiclePoint vp = new VehiclePoint(maXe, bienSo, loai, vin, tenLai,
                                thoiGian, lat, lon, tocDo, viTri, nhienLieu, soKmTong, trangThaiXe);
                        list.add(vp);
                    } while (c.moveToNext());
                }
            } catch (Exception e) {
                Log.e(TAG, "load telemetry error", e);
            } finally {
                if (c != null) c.close();
            }

            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> updateMarkersOnMap(list));
            }
        }).start();
    }

    private void updateMarkersOnMap(List<VehiclePoint> points) {
        List<Integer> seen = new ArrayList<>();
        long nowMs = System.currentTimeMillis();

        // For each incoming point
        for (VehiclePoint vp : points) {
            if (!passesSearchAndFilter(vp, nowMs)) continue;

            seen.add(vp.maXe);

            Marker marker = markers.get(vp.maXe);
            boolean hasPos = !(Double.isNaN(vp.lat) || Double.isNaN(vp.lon));
            LatLng newPos = hasPos ? new LatLng(vp.lat, vp.lon) : null;

            // store new vp into cache AFTER we capture old for animation
            VehiclePoint oldVp = vehicleCache.get(vp.maXe);

            // choose icon based on status
            BitmapDescriptor icon = getCarIconForStatus(computeStatus(vp, nowMs), vp.bienSo);

            if (marker == null) {
                if (newPos != null && mMap != null) {
                    Marker m = mMap.addMarker(new MarkerOptions()
                            .position(newPos)
                            .title(vp.bienSo)
                            .icon(icon)
                            .anchor(0.5f, 0.5f));
                    if (m != null) {
                        m.setTag(vp.maXe);
                        markers.put(vp.maXe, m);
                    }
                }
            } else {
                // if marker exists and vehicle is moving -> animate from old pos to new pos
                VehicleStatus vs = computeStatus(vp, nowMs);
                if (marker != null && newPos != null && vs == VehicleStatus.MOVING && oldVp != null
                        && !Double.isNaN(oldVp.lat) && !Double.isNaN(oldVp.lon)
                        && (oldVp.lat != vp.lat || oldVp.lon != vp.lon)) {
                    // animate marker
                    animateMarkerToPosition(marker, new LatLng(oldVp.lat, oldVp.lon), newPos, 1200L);
                } else {
                    // otherwise set position immediately
                    if (newPos != null) marker.setPosition(newPos);
                }
                marker.setIcon(icon);
                marker.setTitle(vp.bienSo);
                marker.setVisible(true);
            }

            // update cache to new vp (needed for next animation)
            vehicleCache.put(vp.maXe, vp);
        }

        // hide markers not seen
        for (Integer id : new ArrayList<>(markers.keySet())) {
            if (!seen.contains(id)) {
                Marker m = markers.get(id);
                if (m != null) m.setVisible(false);
            }
        }
    }

    // ---------- animation helper ----------
    private void animateMarkerToPosition(final Marker marker, final LatLng start, final LatLng end, long durationMs) {
        // Use ValueAnimator of fraction 0..1, evaluate lat/lng
        ValueAnimator v = ValueAnimator.ofFloat(0f, 1f);
        v.setDuration(durationMs);
        final LatLngInterpolator latLngInterpolator = new LatLngInterpolator.Linear();
        v.addUpdateListener(animation -> {
            float frac = (float) animation.getAnimatedValue();
            LatLng newPos = latLngInterpolator.interpolate(frac, start, end);
            marker.setPosition(newPos);
            // rotate marker to bearing
            float bearing = (float)bearingBetweenLocations(start, end);
            marker.setRotation(bearing);
        });
        v.start();
    }

    // bearing helper (deg)
    private double bearingBetweenLocations(LatLng from, LatLng to) {
        double lat1 = Math.toRadians(from.latitude);
        double lon1 = Math.toRadians(from.longitude);
        double lat2 = Math.toRadians(to.latitude);
        double lon2 = Math.toRadians(to.longitude);
        double dLon = lon2 - lon1;
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1)*Math.sin(lat2) - Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLon);
        double brng = Math.toDegrees(Math.atan2(y, x));
        return (brng + 360.0) % 360.0;
    }

    // LatLng interpolator (linear)
    private interface LatLngInterpolator {
        LatLng interpolate(float fraction, LatLng a, LatLng b);

        class Linear implements LatLngInterpolator {
            @Override
            public LatLng interpolate(float fraction, LatLng a, LatLng b) {
                double lat = (b.latitude - a.latitude) * fraction + a.latitude;
                double lng = (b.longitude - a.longitude) * fraction + a.longitude;
                return new LatLng(lat, lng);
            }
        }
    }

    // ---------- icon helpers ----------
    private enum VehicleStatus { MOVING, STOPPED_SHORT, PARKED_LONG, NO_GPS }

    private VehicleStatus computeStatus(VehiclePoint vp, long nowMs) {
        if (Double.isNaN(vp.lat) || Double.isNaN(vp.lon)) return VehicleStatus.NO_GPS;
        if (vp.trangThaiXe != null && vp.trangThaiXe.toLowerCase().contains("gps") && vp.trangThaiXe.toLowerCase().contains("lost"))
            return VehicleStatus.NO_GPS;

        long tms = parseTimeToMillis(vp.thoiGian);
        long diffSec = (tms <= 0) ? Long.MAX_VALUE : (nowMs - tms) / 1000L;
        double speed = vp.tocDo;
        if (speed > 0.1) return VehicleStatus.MOVING;
        if (diffSec <= 3600) return VehicleStatus.STOPPED_SHORT;
        return VehicleStatus.PARKED_LONG;
    }

    // convert drawable resource into BitmapDescriptor; also centers anchor in markerOptions above
    private BitmapDescriptor getCarIconForStatus(VehicleStatus vs, String plate) {
        if (plate == null) plate = "";
        String key = vs.name() + "|" + plate;
        BitmapDescriptor cached = iconCache.get(key);
        if (cached != null) return cached;

        int resId;
        switch (vs) {
            case MOVING: resId = R.drawable.ic_car_blue; break;
            case STOPPED_SHORT: resId = R.drawable.ic_car_red; break;
            case PARKED_LONG: resId = R.drawable.ic_car_pink; break;
            case NO_GPS: resId = R.drawable.ic_car_green; break;
            default: resId = R.drawable.ic_car_blue; break;
        }

        // kích thước icon cố định: 48dp icon + 18dp text area (tổng height ~ 66dp)
        int iconDp = 24;
        int textAreaDp = 18;
        int extraPaddingDp = 4;
        int iconPx = dpToPx(iconDp);
        int textPx = dpToPx(textAreaDp);
        int padPx = dpToPx(extraPaddingDp);

        Bitmap bmp = createMarkerBitmap(resId, plate, iconPx, textPx, padPx);
        BitmapDescriptor bd = BitmapDescriptorFactory.fromBitmap(bmp);
        iconCache.put(key, bd);
        return bd;
    }


    // utility: convert vector drawable (or png) to Bitmap to use as marker icon
    private Bitmap vectorToBitmap(int drawableId) {
        try {
            Drawable drawable = requireContext().getDrawable(drawableId);
            if (drawable == null) return Bitmap.createBitmap(1,1, Bitmap.Config.ARGB_8888);
            int w = (int) (drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 64);
            int h = (int) (drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 64);
            Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bm);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bm;
        } catch (Exception e) {
            Log.e(TAG, "vectorToBitmap error", e);
            return Bitmap.createBitmap(1,1, Bitmap.Config.ARGB_8888);
        }
    }

    // ---------- UI detail & helpers ----------
    private boolean passesSearchAndFilter(VehiclePoint vp, long nowMs) {
        if (searchQuery != null && !searchQuery.isEmpty()) {
            String q = searchQuery;
            boolean matches = (vp.bienSo != null && vp.bienSo.toLowerCase().contains(q))
                    || (vp.tenLai != null && vp.tenLai.toLowerCase().contains(q));
            if (!matches) return false;
        }
        VehicleStatus vs = computeStatus(vp, nowMs);
        switch (vs) {
            case MOVING: return filterMoving;
            case STOPPED_SHORT: return filterStopped;
            case PARKED_LONG: return filterParked;
            case NO_GPS: return filterNoGps;
            default: return true;
        }
    }

    private void showDetailCard(VehiclePoint vp) {
        if (vp == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("\u2022 Biển số: ").append(nonEmpty(vp.bienSo)).append("\n");
        sb.append("\u2022 Loại xe: ").append(nonEmpty(vp.loai)).append("\n");
        sb.append("\u2022 Số khung (VIN): ").append(nonEmpty(vp.vin)).append("\n");
        sb.append("\u2022 Tình trạng: ").append(nonEmpty(vp.trangThaiXe)).append("\n");
        sb.append("\u2022 Tốc độ hiện tại: ").append((int)vp.tocDo).append(" km/h").append("\n");
        sb.append("\u2022 Nhiên liệu: ").append(nonEmpty(vp.nhienLieu)).append("\n");
        sb.append("\u2022 Số km đã đi trong ngày: ").append("-").append("\n");
        sb.append("\u2022 Số km tổng cộng: ").append(vp.soKmTong).append(" km").append("\n\n");

        sb.append("Thông tin lái xe\n");
        sb.append("\u2022 Họ tên: ").append(nonEmpty(vp.tenLai)).append("\n");
        sb.append("\u2022 GPLX: ").append("-").append("\n");
        sb.append("\u2022 Số điện thoại: ").append("-").append("\n");
        sb.append("\u2022 Thời gian lái liên tục: ").append("-").append("\n");
        sb.append("\u2022 Thời gian lái trong ngày: ").append("-").append("\n");
        sb.append("\u2022 Thời gian lái trong tuần: ").append("-").append("\n");
        sb.append("\u2022 Thời điểm xe dừng/chạy gần nhất: ").append(nonEmpty(vp.thoiGian)).append("\n\n");

        sb.append("Thông tin thiết bị\n");
        sb.append("\u2022 GPS: ").append((Double.isNaN(vp.lat) || Double.isNaN(vp.lon)) ? "Không hoạt động" : "Hoạt động bình thường").append("\n");


        tvDetailContent.setText(sb.toString());
        tvDetailTitle.setText("Thông tin phương tiện:");
        cardDetail.setVisibility(View.VISIBLE);
    }

    private long parseTimeToMillis(String s) {
        if (s == null || s.isEmpty()) return -1;
        try {
            Date d = isoParse.parse(s.replace('T', ' '));
            return d != null ? d.getTime() : -1;
        } catch (ParseException e) {
            try {
                SimpleDateFormat alt = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
                Date d2 = alt.parse(s);
                return d2 != null ? d2.getTime() : -1;
            } catch (Exception ex) {
                return -1;
            }
        }
    }

    private String safeGet(Cursor c, String col) {
        try {
            int idx = c.getColumnIndex(col);
            if (idx == -1) return "";
            return c.isNull(idx) ? "" : c.getString(idx);
        } catch (Exception e) {
            try {
                return c.getString(0);
            } catch (Exception ex) {
                return "";
            }
        }
    }

    private String nonEmpty(String s) {
        return (s == null || s.isEmpty()) ? "-" : s;
    }

    // ---------- data holder ----------
    private static class VehiclePoint {
        int maXe;
        String bienSo, loai, vin, tenLai, thoiGian, viTri, nhienLieu, trangThaiXe;
        double lat, lon, tocDo, soKmTong;

        VehiclePoint(int maXe, String bienSo, String loai, String vin, String tenLai,
                     String thoiGian, double lat, double lon, double tocDo, String viTri,
                     String nhienLieu, double soKmTong, String trangThaiXe) {
            this.maXe = maXe; this.bienSo = bienSo; this.loai = loai; this.vin = vin; this.tenLai = tenLai;
            this.thoiGian = thoiGian; this.lat = lat; this.lon = lon; this.tocDo = tocDo; this.viTri = viTri;
            this.nhienLieu = nhienLieu; this.soKmTong = soKmTong; this.trangThaiXe = trangThaiXe;
        }
    }

    private void showFilterDialog() {
        final String[] items = new String[]{"Di chuyển", "Dừng (<3600s)", "Đỗ (>3600s)", "Mất GPS"};
        final boolean[] checked = new boolean[]{filterMoving, filterStopped, filterParked, filterNoGps};
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Lọc phương tiện")
                .setMultiChoiceItems(items, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("Áp dụng", (d, w) -> {
                    filterMoving = checked[0];
                    filterStopped = checked[1];
                    filterParked = checked[2];
                    filterNoGps = checked[3];
                    loadAndUpdateMarkers();
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private Bitmap createMarkerBitmap(int drawableResId, String plateText, int iconPx, int textPx, int paddingPx) {
        try {
            Drawable drawable = requireContext().getDrawable(drawableResId);
            if (drawable == null) {
                return Bitmap.createBitmap(1,1, Bitmap.Config.ARGB_8888);
            }

            // width = max(iconPx, text width estimate)
            // To compute text width we use Paint.measureText
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextSize((float) (textPx * 0.6f)); // scale text size inside text area
            textPaint.setColor(0xFF222222);
            textPaint.setTextAlign(Paint.Align.CENTER);

            float textWidth = textPaint.measureText(plateText);
            int bmpWidth = Math.max(iconPx, (int) textWidth) + paddingPx * 2;
            int bmpHeight = iconPx + textPx + paddingPx * 2;

            Bitmap bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            // draw icon centered horizontally at top
            int left = (bmpWidth - iconPx) / 2;
            int top = paddingPx;
            drawable.setBounds(left, top, left + iconPx, top + iconPx);
            drawable.draw(canvas);

            // draw plate text centered below icon (vertical center of text area)
            float x = bmpWidth / 2f;
            // compute baseline for text to be vertically centered in text area
            float textAreaTop = top + iconPx;
            float textAreaBottom = bmpHeight - paddingPx;
            float textAreaCenter = (textAreaTop + textAreaBottom) / 2f;
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float textBaseline = textAreaCenter - (fm.ascent + fm.descent) / 2f;
            // truncate long plate text if necessary
            String drawText = plateText != null ? plateText : "";
            if (drawText.length() > 12) drawText = drawText.substring(0, 12) + "...";
            canvas.drawText(drawText, x, textBaseline, textPaint);

            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return Bitmap.createBitmap(1,1, Bitmap.Config.ARGB_8888);
        }
    }

    private int dpToPx(int dp) {
        float dens = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * dens);
    }

}
