package com.example.tttn_hoangdaivms.Report;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tttn_hoangdaivms.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class BaoTriAdapter extends RecyclerView.Adapter<BaoTriAdapter.VH> {

    public interface OnItemAction {
        void onSelectionChanged();
        void onItemClicked(BaoTriModel item); // gọi khi nhấn item -> mở detail
    }

    private List<BaoTriModel> list;
    private OnItemAction listener;

    public BaoTriAdapter(List<BaoTriModel> items, OnItemAction listener) {
        this.list = items != null ? items : new ArrayList<>();
        this.listener = listener;
    }

    public void setItems(List<BaoTriModel> items) {
        this.list = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<BaoTriModel> getSelectedItems() {
        List<BaoTriModel> sel = new ArrayList<>();
        for (BaoTriModel b : list) if (b.isSelected) sel.add(b);
        return sel;
    }

    public List<BaoTriModel> getAllItems() {
        return list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_baotri, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        BaoTriModel item = list.get(position);

        // Biển số
        String plate = item.bienSo != null ? item.bienSo : "";

        // Kiểm tra ngày bảo trì gần nhất -> nếu đã quá 6 tháng thì hiển thị "- Trễ X ngày"
        String statusSuffix = computeLateSuffix(item.ngayBaoTri);

        // Kết hợp: "60C-45678 - Trễ 10 ngày" hoặc chỉ "60C-45678"
        String display = plate;
        if (statusSuffix != null && !statusSuffix.isEmpty()) {
            display = plate + " - " + statusSuffix;
            holder.tvPlateWithStatus.setTextColor(holder.itemView.getResources().getColor(android.R.color.holo_red_dark));
        } else {
            holder.tvPlateWithStatus.setTextColor(holder.itemView.getResources().getColor(android.R.color.black));
        }
        holder.tvPlateWithStatus.setText(display);

        // Checkbox handling (detach listener trước khi set để tránh callback không mong muốn)
        holder.checkSelect.setOnCheckedChangeListener(null);
        holder.checkSelect.setChecked(item.isSelected);
        holder.checkSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.isSelected = isChecked;
            if (listener != null) listener.onSelectionChanged();
        });

        // Click item -> mở detail (onItemClicked)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClicked(item);
        });

        // Long click -> toggle selection (tiện khi user muốn chọn nhiều)
        holder.itemView.setOnLongClickListener(v -> {
            boolean newVal = !item.isSelected;
            item.isSelected = newVal;
            holder.checkSelect.setChecked(newVal);
            if (listener != null) listener.onSelectionChanged();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox checkSelect;
        TextView tvPlateWithStatus;

        VH(@NonNull View itemView) {
            super(itemView);
            checkSelect = itemView.findViewById(R.id.checkSelect);

            // Hỗ trợ 2 id khác nhau trong layout item: tvPlateWithStatus (mới) hoặc tvTitle (cũ)
            TextView t = itemView.findViewById(R.id.tvPlateWithStatus);
            if (t == null) t = itemView.findViewById(R.id.tvTitle);
            tvPlateWithStatus = t != null ? t : new TextView(itemView.getContext());
        }
    }

    /**
     * Trả về chuỗi trạng thái nếu trễ, ví dụ "Trễ 10 ngày".
     * Nếu không trễ hoặc ngày không parse được -> trả empty string.
     *
     * dateStr: chuỗi ngày từ DB (ví dụ "2025-08-20" hoặc "2025-08-20 10:00:00")
     */
    private String computeLateSuffix(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return "";

        // Thử hai định dạng phổ biến
        String[] patterns = new String[] {"dd/MM/yyyy HH:mm:ss", "dd/MM/yyyy"};
        Date parsed = null;
        for (String p : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(p, Locale.getDefault());
                sdf.setLenient(false);
                parsed = sdf.parse(dateStr);
                if (parsed != null) break;
            } catch (ParseException ignored) {}
        }
        if (parsed == null) return ""; // không parse được -> không hiển thị trạng thái

        // Tính ngày hết hạn: dateParsed + 6 tháng
        Calendar due = Calendar.getInstance();
        due.setTime(parsed);
        due.add(Calendar.MONTH, 6);

        Calendar now = Calendar.getInstance();

        if (now.after(due)) {
            long diffMillis = now.getTimeInMillis() - due.getTimeInMillis();
            long daysLate = TimeUnit.MILLISECONDS.toDays(diffMillis);
            if (daysLate <= 0) daysLate = 1; // tối thiểu 1 ngày nếu vượt chút
            return "Trễ " + daysLate + " ngày";
        } else {
            return "";
        }
    }
}
