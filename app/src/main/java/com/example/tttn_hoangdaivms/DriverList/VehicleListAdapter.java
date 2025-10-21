package com.example.tttn_hoangdaivms.DriverList;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tttn_hoangdaivms.R;

import java.util.List;

public class VehicleListAdapter extends RecyclerView.Adapter<VehicleListAdapter.VehicleViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(VehicleListModel vehicle);
    }

    private List<VehicleListModel> vehicleList;
    private OnItemClickListener listener;

    public VehicleListAdapter(List<VehicleListModel> vehicleList, OnItemClickListener listener) {
        this.vehicleList = vehicleList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.car_listitem, parent, false);
        return new VehicleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        VehicleListModel vehicle = vehicleList.get(position);
        holder.vehicleName.setText(vehicle.getName());
        holder.vehiclePlate.setText(vehicle.getPlateNumber());
        holder.vehicleImage.setImageResource(vehicle.getImageResId());
        holder.itemView.setOnClickListener(v -> listener.onItemClick(vehicle));
    }

    @Override
    public int getItemCount() {
        return vehicleList.size();
    }

    static class VehicleViewHolder extends RecyclerView.ViewHolder {
        ImageView vehicleImage;
        TextView vehicleName, vehiclePlate;

        VehicleViewHolder(View itemView) {
            super(itemView);
            vehicleImage = itemView.findViewById(R.id.vehicleImage);
            vehicleName = itemView.findViewById(R.id.vehicleName);
            vehiclePlate = itemView.findViewById(R.id.vehiclePlate);
        }
    }
}
