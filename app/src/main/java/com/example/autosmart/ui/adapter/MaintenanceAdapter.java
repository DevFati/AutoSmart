package com.example.autosmart.ui.adapter;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autosmart.R;
import com.example.autosmart.data.db.MaintenanceEntity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Adapter para mostrar la lista de mantenimientos en un RecyclerView.
 */
public class MaintenanceAdapter
        extends RecyclerView.Adapter<MaintenanceAdapter.MaintViewHolder> {

    /**
     * Interfaz para manejar acciones sobre los ítems de mantenimiento.
     */
    public interface OnItemActionListener {
        /**
         * Se llama cuando se edita un mantenimiento.
         * @param m Entidad de mantenimiento.
         */
        void onEdit(MaintenanceEntity m);
        /**
         * Se llama cuando se elimina un mantenimiento.
         * @param m Entidad de mantenimiento.
         */
        void onDelete(MaintenanceEntity m);
    }

    private Context context;
    private Map<String,String> labelById;
    private final OnItemActionListener listener;
    private List<MaintenanceEntity> items;

    public MaintenanceAdapter(Context context, Map<String,String> labelById,
                              OnItemActionListener listener) {
        this.context = context;
        this.labelById = labelById;
        this.listener  = listener;
    }

    /**
     * Establece la lista de mantenimientos a mostrar.
     * @param list Lista de mantenimientos.
     */
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

    /**
     * Establece el mapa de etiquetas por ID.
     * @param newLabelById Mapa de etiquetas.
     */
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
        String vehLabel = "Desconocido";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            vehLabel = labelById.getOrDefault(m.vehicleId, vehLabel);
        }
        h.tvVehicle.setText(vehLabel);

        // Fecha, tipo, coste y matrícula sin emojis
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String formattedDate = sdf.format(new Date(m.date));
        h.tvDate.setText(formattedDate);
        h.tvType.setText(m.type);
        h.tvCost.setText(String.format("%.2f €", m.cost));
        h.tvPlate.setText(m.vehiclePlate);

        // Botones de acción
        h.btnEdit.setOnClickListener(v -> listener.onEdit(m));
        h.btnDelete.setOnClickListener(v -> {
            m.isDeleted = true;
            DatabaseReference maintRef = FirebaseDatabase.getInstance().getReference("maintenances").child(String.valueOf(m.id));
            maintRef.child("isDeleted").setValue(true);
            Toast.makeText(context, "Mantenimiento eliminado (historial permanente)", Toast.LENGTH_SHORT).show();
            listener.onDelete(m);
        });
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
