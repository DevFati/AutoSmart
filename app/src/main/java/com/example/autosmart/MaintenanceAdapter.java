package com.example.autosmart;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class MaintenanceAdapter
        extends RecyclerView.Adapter<MaintenanceAdapter.MaintViewHolder> {

    public interface OnItemActionListener {
        void onEdit(MaintenanceEntity m);
        void onDelete(MaintenanceEntity m);
    }

    private Map<String,String> labelById;
    private final OnItemActionListener listener;
    private List<MaintenanceEntity> items;

    public MaintenanceAdapter(Map<String,String> labelById,
                              OnItemActionListener listener) {
        this.labelById = labelById;
        this.listener  = listener;
    }

    public void setItems(List<MaintenanceEntity> list){
        this.items = list;
        android.util.Log.d("MaintenanceAdapter", "setItems: " + (list != null ? list.size() : 0) + " items");
        if (list != null) {
            for (MaintenanceEntity m : list) {
                android.util.Log.d("MaintenanceAdapter", 
                    "Item: id=" + m.id + 
                    ", vehicleId=" + m.vehicleId + 
                    ", date=" + m.date);
            }
        }
        notifyDataSetChanged();
    }

    public void setLabelById(Map<String, String> newLabelById) {
        this.labelById = newLabelById;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public MaintViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_maintenance, parent, false);
        return new MaintViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MaintViewHolder h, int pos) {
        MaintenanceEntity m = items.get(pos);
        android.util.Log.d("MaintenanceAdapter", "Binding item at pos " + pos + ": " + m.vehicleId);

        // Vehículo (o "Desconocido" si no está en el mapa)
        String vehLabel = "🚗 Desconocido";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            vehLabel = labelById.getOrDefault(m.vehicleId, vehLabel);
        }
        h.tvVehicle.setText(vehLabel);

        // Fecha, tipo, coste y matrícula con emojis
        h.tvDate .setText("🗓 " + m.date);
        h.tvType .setText("🔧 " + m.type);
        h.tvCost .setText("💶 " + String.format("%.2f", m.cost));
        h.tvPlate.setText("🪪 " + m.vehiclePlate);

        // Botones de acción
        h.btnEdit .setOnClickListener(v -> listener.onEdit(m));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(m));
    }

    @Override public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class MaintViewHolder extends RecyclerView.ViewHolder {
        TextView    tvVehicle, tvDate, tvType, tvCost, tvPlate;
        ImageButton btnEdit, btnDelete;

        MaintViewHolder(View itemView) {
            super(itemView);
            tvVehicle = itemView.findViewById(R.id.tvMaintVehicle);
            tvDate    = itemView.findViewById(R.id.tvMaintDate);
            tvType    = itemView.findViewById(R.id.tvMaintType);
            tvCost    = itemView.findViewById(R.id.tvMaintCost);
            tvPlate   = itemView.findViewById(R.id.tvMaintPlate);
            btnEdit   = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
