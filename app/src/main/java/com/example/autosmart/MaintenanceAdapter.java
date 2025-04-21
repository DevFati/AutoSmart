package com.example.autosmart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MaintenanceAdapter
        extends RecyclerView.Adapter<MaintenanceAdapter.MaintViewHolder> {

    private List<MaintenanceEntity> items = new ArrayList<>();
    private OnItemActionListener listener;

    public interface OnItemActionListener {
        void onEdit(MaintenanceEntity m);
        void onDelete(MaintenanceEntity m);
    }

    public MaintenanceAdapter(OnItemActionListener listener){
        this.listener = listener;
    }

    public void setItems(List<MaintenanceEntity> list){
        items = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MaintViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_maintenance, parent, false);
        return new MaintViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MaintViewHolder h, int pos) {
        h.bind(items.get(pos), listener);
    }

    @Override public int getItemCount() { return items.size(); }

    static class MaintViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvType, tvCost;
        ImageButton btnEdit, btnDelete;

        MaintViewHolder(View iv) {
            super(iv);
            tvDate    = iv.findViewById(R.id.tvMaintDate);
            tvType    = iv.findViewById(R.id.tvMaintType);
            tvCost    = iv.findViewById(R.id.tvMaintCost);
            btnEdit   = iv.findViewById(R.id.btnEdit);
            btnDelete = iv.findViewById(R.id.btnDelete);
        }

        void bind(MaintenanceEntity m, OnItemActionListener l) {
            tvDate.setText(m.date);
            tvType.setText(m.type);
            tvCost.setText(String.format(Locale.getDefault(),"€%.2f", m.cost));

            btnEdit.setOnClickListener(v -> l.onEdit(m));
            btnDelete.setOnClickListener(v -> l.onDelete(m));
        }
    }
}

