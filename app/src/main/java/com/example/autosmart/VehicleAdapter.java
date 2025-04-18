package com.example.autosmart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder> {

    public interface OnItemLongClickListener {
        /**
         * @param vehicle  El vehículo sobre el que se hizo long click.
         * @param position Posición en el adapter.
         * @return true si el evento fue consumido.
         */
        boolean onItemLongClick(Vehicle vehicle, int position);
    }


    private List<Vehicle> vehicleList;
    private OnItemLongClickListener longClickListener;

    public VehicleAdapter(List<Vehicle> vehicleList) {
        this.vehicleList = vehicleList;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla el layout para cada item del RecyclerView
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vehicle, parent, false);
        return new VehicleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        // Obtiene el vehículo correspondiente y lo vincula al ViewHolder
        Vehicle vehicle = vehicleList.get(position);
        holder.bind(vehicle);

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                return longClickListener.onItemLongClick(vehicle, holder.getAdapterPosition());
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return vehicleList.size();
    }

    static class VehicleViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvBrandModel, tvYear, tvEngine;
        public VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBrandModel = itemView.findViewById(R.id.tvBrand);
            tvYear       = itemView.findViewById(R.id.tvYear);
            tvEngine     = itemView.findViewById(R.id.tvEngine);
        }
        public void bind(Vehicle v) {
            tvBrandModel.setText(v.getBrand() + " " + v.getModel());
            tvYear.setText(v.getYear());
            tvEngine.setText(v.getEngineType());
        }
    }


}
