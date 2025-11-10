package com.example.tttn_hoangdaivms.Report;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.tttn_hoangdaivms.R;
import com.example.tttn_hoangdaivms.Report.ReportModel;

import java.util.ArrayList;
import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.VH> {

    public interface OnItemAction {
        void onItemClicked(ReportModel item);
        void onSelectionChanged();
    }

    private List<ReportModel> originalList;
    private List<ReportModel> displayList;
    private OnItemAction listener;

    public ReportAdapter(List<ReportModel> items, OnItemAction listener) {
        this.originalList = new ArrayList<>(items);
        this.displayList = new ArrayList<>(items);
        this.listener = listener;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.trip_item_report, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        final ReportModel item = displayList.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvStatus.setText(item.getStatusText());
        holder.checkBox.setChecked(item.isSelected);

        holder.itemView.setOnClickListener(v -> {
            item.isSelected = !item.isSelected;
            holder.checkBox.setChecked(item.isSelected);
            if (listener != null) listener.onSelectionChanged();
            if (listener != null) listener.onItemClicked(item);
        });

        holder.checkBox.setOnClickListener(v -> {
            item.isSelected = holder.checkBox.isChecked();
            if (listener != null) listener.onSelectionChanged();
        });

    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    public class VH extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView tvTitle, tvStatus;


        public VH(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkSelect);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }

    public void filter(String query) {
        displayList.clear();
        if (query == null || query.trim().isEmpty()) {
            displayList.addAll(originalList);
        } else {
            query = query.toLowerCase();
            for (ReportModel r : originalList) {
                if ((r.bienSo != null && r.bienSo.toLowerCase().contains(query))
                        || r.getDisplayId().toLowerCase().contains(query)
                        || r.getTitle().toLowerCase().contains(query)) {
                    displayList.add(r);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void setItems(List<ReportModel> items) {
        this.originalList = new ArrayList<>(items);
        filter(null);
    }

    public List<ReportModel> getSelectedItems() {
        List<ReportModel> sel = new ArrayList<>();
        for (ReportModel r : originalList) if (r.isSelected) sel.add(r);
        return sel;
    }

    // MỚI: trả về tất cả items (bản sao)
    public List<ReportModel> getAllItems() {
        return new ArrayList<>(originalList);
    }
}
