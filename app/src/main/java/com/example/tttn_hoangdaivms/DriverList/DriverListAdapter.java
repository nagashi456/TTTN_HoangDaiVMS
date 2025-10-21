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

public class DriverListAdapter extends RecyclerView.Adapter<DriverListAdapter.DriverViewHolder> {

    private List<DriverListModel> driverList;
    private OnDriverClickListener listener;

    public interface OnDriverClickListener {
        void onDriverClick(DriverListModel driver);
    }

    public DriverListAdapter(List<DriverListModel> driverList, OnDriverClickListener listener) {
        this.driverList = driverList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.driver_listitem, parent, false);
        return new DriverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DriverViewHolder holder, int position) {
        DriverListModel driver = driverList.get(position);
        holder.bind(driver, listener);
    }

    @Override
    public int getItemCount() {
        return driverList.size();
    }

    static class DriverViewHolder extends RecyclerView.ViewHolder {
        private ImageView driverAvatar;
        private TextView driverName;
        private TextView driverLocation;

        public DriverViewHolder(@NonNull View itemView) {
            super(itemView);
            driverAvatar = itemView.findViewById(R.id.driverAvatar);
            driverName = itemView.findViewById(R.id.driverName);
            driverLocation = itemView.findViewById(R.id.driverLocation);
        }

        public void bind(DriverListModel driver, OnDriverClickListener listener) {
            driverName.setText(driver.getName());
            driverLocation.setText(driver.getLocation());
            driverAvatar.setImageResource(driver.getAvatarResId());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDriverClick(driver);
                }
            });
        }
    }
}