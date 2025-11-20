package com.example.tttn_hoangdaivms.DriverList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tttn_hoangdaivms.R;

import java.util.ArrayList;
import java.util.List;

public class DriverListAdapter extends RecyclerView.Adapter<DriverListAdapter.DriverViewHolder> {

    private final Context context;
    private final List<DriverListModel> driverList;   // dữ liệu hiển thị hiện tại
    private final List<String> driverIds;            // id song song với driverList
    private final OnDriverActionListener actionListener;

    public interface OnDriverActionListener {
        // Khi click vào item (mở chi tiết)
        void onDriverClick(DriverListModel driver, int position, String id);

        // Khi user bấm edit icon
        void onEditRequested(DriverListModel driver, int position, String id);

        // Khi user xác nhận xóa: adapter chỉ thông báo fragment để fragment thực hiện xóa DB
        // Fragment nên xóa DB rồi gọi adapter.removeAt(position) và cập nhật các danh sách tương ứng.
        void onDeleteRequested(DriverListModel driver, int position, String id);
    }

    public DriverListAdapter(Context context,
                             List<DriverListModel> driverList,
                             List<String> driverIds,
                             OnDriverActionListener listener) {
        this.context = context;
        // Use copies to avoid accidental external mutation issues, but keep references so fragment's lists remain source-of-truth.
        this.driverList = driverList != null ? driverList : new ArrayList<>();
        this.driverIds = driverIds != null ? driverIds : new ArrayList<>();
        this.actionListener = listener;
        setHasStableIds(false);
    }

    @NonNull
    @Override
    public DriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.driver_listitem, parent, false);
        return new DriverViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DriverViewHolder holder, int position) {
        DriverListModel d = driverList.get(position);
        String id = (position < driverIds.size()) ? driverIds.get(position) : null;
        holder.bind(d, position, id);
    }

    @Override
    public int getItemCount() {
        return driverList.size();
    }

    @Override
    public long getItemId(int position) {
        // nếu bạn muốn stable id, parse driverIds.get(position) sang long nếu được
        return position;
    }

    /**
     * Cập nhật lại dữ liệu (dùng khi filter/restore) — fragment gọi method này thay vì recreate adapter
     */
    public void updateData(List<DriverListModel> newDrivers, List<String> newIds) {
        driverList.clear();
        driverIds.clear();
        if (newDrivers != null) driverList.addAll(newDrivers);
        if (newIds != null) driverIds.addAll(newIds);
        notifyDataSetChanged();
    }

    /**
     * Xóa item ở vị trí (UI only). Fragment phải cập nhật các list full tương ứng trước nếu cần.
     */
    public void removeAt(int position) {
        if (position < 0 || position >= driverList.size()) return;
        driverList.remove(position);
        if (position < driverIds.size()) driverIds.remove(position);
        notifyItemRemoved(position);
    }

    class DriverViewHolder extends RecyclerView.ViewHolder {
        private final ImageView avatar;
        private final TextView nameTv;
        private final TextView phoneTv;        // đổi tên từ locationTv -> phoneTv
        private final ImageView editIcon;
        private final ImageView deleteIcon;

        public DriverViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.driverAvatar);
            nameTv = itemView.findViewById(R.id.driverName);
            phoneTv = itemView.findViewById(R.id.driverLocation); // id mới: driverPhone
            editIcon = itemView.findViewById(R.id.editIcon);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);
        }

        public void bind(final DriverListModel driver, final int position, final String id) {
            // Guard
            if (driver == null) return;

            nameTv.setText(driver.getName());

            // lấy phone từ model (nếu null hiển thị rỗng)
            String phone = driver.getPhone() != null ? driver.getPhone() : "";
            phoneTv.setText(phone);

            avatar.setImageResource(driver.getAvatarResId());

            // click toàn item -> onDriverClick
            itemView.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onDriverClick(driver, position, id);
            });

            // edit -> delegate to fragment (or show toast if not implemented)
            editIcon.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onEditRequested(driver, position, id);
            });

            // delete -> show confirmation dialog here and then delegate actual deletion
            deleteIcon.setOnClickListener(v -> {
                // capture final variables (position and id already final)
                new AlertDialog.Builder(context)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc chắn muốn xóa nhân viên này không?")
                        .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            if (actionListener != null) {
                                actionListener.onDeleteRequested(driver, position, id);
                            }
                        })
                        .show();
            });
        }
    }
}
