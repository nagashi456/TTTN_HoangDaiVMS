package com.example.tttn_hoangdaivms.Request;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tttn_hoangdaivms.R;

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.ViewHolder> {

    private final Context context;
    private final List<RequestModel> items;
    private final LayoutInflater inflater;
    private final OnRequestClickListener listener;

    // Giao diện callback khi bấm vào 1 item
    public interface OnRequestClickListener {
        void onRequestClick(RequestModel request);
    }

    public RequestAdapter(Context context, List<RequestModel> items, OnRequestClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
        this.inflater = LayoutInflater.from(context);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMenu;
        TextView tvName, tvStatus, tvDate;
        CardView cardContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
            ivMenu = itemView.findViewById(R.id.ivMenu);
            cardContainer = (CardView) itemView;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.request_listitem, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RequestModel item = items.get(position);
        if (item == null) return;

        holder.tvName.setText(item.getName());
        holder.tvStatus.setText(item.getStatus());
        holder.tvDate.setText(item.getDate());

        // Đặt màu cho trạng thái
        switch (item.getStatus()) {
            case "Đang yêu cầu":
                holder.tvStatus.setTextColor(Color.parseColor("#005DFF")); // xanh
                break;
            case "Đã từ chối":
                holder.tvStatus.setTextColor(Color.parseColor("#FF0D0D")); // đỏ
                break;
            case "Đã duyệt":
                holder.tvStatus.setTextColor(Color.parseColor("#00C853")); // xanh lá
                break;
            default:
                holder.tvStatus.setTextColor(Color.parseColor("#000000")); // đen
                break;

        }

        // Sự kiện khi bấm vào item
        holder.cardContainer.setOnClickListener(v -> {
            if (listener != null) listener.onRequestClick(item);
        });
        holder.ivMenu.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(context, holder.ivMenu);
            popupMenu.getMenuInflater().inflate(R.menu.menu_request_item, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                int id = menuItem.getItemId();

                if (id == R.id.menu_view_detail) {
                    Toast.makeText(context, "Xem chi tiết: " + item.getName(), Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onRequestClick(item);
                    return true;

                } else if (id == R.id.menu_delete) {
                    Toast.makeText(context, "Đã xóa yêu cầu của " + item.getName(), Toast.LENGTH_SHORT).show();
                    // Xóa item khỏi danh sách
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        items.remove(pos);
                        notifyItemRemoved(pos);
                    }
                    return true;
                }

                return false;
            });
            popupMenu.show();
        });
    }
    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }
}

