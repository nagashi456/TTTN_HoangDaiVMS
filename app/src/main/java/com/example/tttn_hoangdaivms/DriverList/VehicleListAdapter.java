package com.example.tttn_hoangdaivms.DriverList;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tttn_hoangdaivms.R;

import java.util.ArrayList;
import java.util.List;

/**
 * VehicleListAdapter - thiết kế theo style của DriverAdapter:
 * - An toàn (null-check)
 * - Có methods updateList/addItem/removeItem
 * - Listener nhận MaXe (int) và xử lý xóa giống DriverAdapter
 */
public class VehicleListAdapter extends RecyclerView.Adapter<VehicleListAdapter.VehicleViewHolder> {

    /**
     * Listener: nhận sự kiện click và delete.
     * Tương tự DriverAdapter, cung cấp vị trí để fragment/Activity có thể thao tác danh sách.
     */
    public interface OnItemActionListener {
        void onItemClick(int maXe);
        void onDeleteRequested(int maXe, int position);
        void onEditRequested(int maXe, int position);
    }

    private final List<VehicleListModel> vehicleList;
    private OnItemActionListener listener;

    // constructor mặc định
    public VehicleListAdapter(List<VehicleListModel> vehicleList, OnItemActionListener listener) {
        this.vehicleList = vehicleList != null ? new ArrayList<>(vehicleList) : new ArrayList<>();
        this.listener = listener;
        setHasStableIds(false); // nếu bạn muốn stable ids, override getItemId & set true
    }

    // thay đổi listener sau khi khởi tạo (tuỳ chọn)
    public void setOnItemActionListener(OnItemActionListener listener) {
        this.listener = listener;
    }

    // cập nhật toàn bộ list
    public void updateList(List<VehicleListModel> newList) {
        vehicleList.clear();
        if (newList != null) vehicleList.addAll(newList);
        notifyDataSetChanged();
    }

    // thêm 1 item
    public void addItem(VehicleListModel item) {
        if (item == null) return;
        vehicleList.add(item);
        notifyItemInserted(vehicleList.size() - 1);
    }

    // xoá 1 item theo position
    public void removeItem(int position) {
        if (position < 0 || position >= vehicleList.size()) return;
        vehicleList.remove(position);
        notifyItemRemoved(position);
    }

    // alias: removeAt giống driverAdapter
    public void removeAt(int position) {
        removeItem(position);
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.car_listitem, parent, false);
        return new VehicleViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        if (position < 0 || position >= vehicleList.size()) return;

        VehicleListModel model = vehicleList.get(position);
        if (model == null) return;

        // Hiển thị text null-safe
        String name = model.getName() != null ? model.getName() : "Không tên";
        String plate = model.getPlateNumber() != null ? model.getPlateNumber() : "-";

        holder.tvName.setText(name);
        holder.tvPlate.setText(plate);

        // Ảnh: nếu model cung cấp resource id (>0) thì dùng, ngược lại dùng placeholder an toàn
        int imgRes = model.getImageResId();
        if (imgRes != 0) {
            holder.ivImage.setImageResource(imgRes);
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Accessibility
        holder.itemView.setContentDescription(name + " - " + plate);

        // Click: truyền MaXe (int) cho listener
        holder.itemView.setOnClickListener(v -> {
            if (listener == null) return;
            try {
                int maXe = model.getMaXe(); // đảm bảo VehicleListModel có getMaXe()
                listener.onItemClick(maXe);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Delete icon click: gọi onDeleteRequested với maXe + position
        holder.icDelete.setOnClickListener(v -> {
            if (listener == null) return;
            try {
                int maXe = model.getMaXe();
                int pos = holder.getAdapterPosition();
                // adapter position có thể = RecyclerView.NO_POSITION
                if (pos == RecyclerView.NO_POSITION) pos = position;
                listener.onDeleteRequested(maXe, pos);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        holder.icEdit.setOnClickListener(v -> {
            if (listener == null) return;
            try {
                int maXe = model.getMaXe();
                int pos = holder.getAdapterPosition();
                // adapter position có thể = RecyclerView.NO_POSITION
                if (pos == RecyclerView.NO_POSITION) pos = position;
                listener.onEditRequested(maXe, pos);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public int getItemCount() {
        return vehicleList.size();
    }

    // (tuỳ chọn) nếu muốn stable id, override getItemId và setHasStableIds(true)
    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= vehicleList.size()) return RecyclerView.NO_ID;
        try {
            return vehicleList.get(position).getMaXe();
        } catch (Exception e) {
            return RecyclerView.NO_ID;
        }
    }

    // expose underlying list (read-only) nếu cần
    public List<VehicleListModel> getVehicleList() {
        return vehicleList;
    }

    static class VehicleViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvPlate;
        ImageView icDelete;
        ImageView icEdit;

        VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.vehicleImage);
            tvName = itemView.findViewById(R.id.vehicleName);
            tvPlate = itemView.findViewById(R.id.vehiclePlate);
            // !!! đảm bảo car_listitem.xml có view này
            icDelete = itemView.findViewById(R.id.deleteIcon);
            icEdit = itemView.findViewById(R.id.editIcon);
            // nếu icDelete không tồn tại, hãy thêm ImageView vào layout với id tương ứng
        }
    }
}
