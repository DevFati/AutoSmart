package com.example.autosmart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder> {

    private List<Vehicle> vehicleList;

    public VehicleAdapter(List<Vehicle> vehicleList) {
        this.vehicleList = vehicleList;
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
    }

    @Override
    public int getItemCount() {
        return vehicleList.size();
    }

    // Clase interna para manejar cada item del RecyclerView
    public static class VehicleViewHolder extends RecyclerView.ViewHolder {

        private TextView tvBrand, tvModel, tvYear;

        public VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBrand = itemView.findViewById(R.id.tvBrand);
            tvModel = itemView.findViewById(R.id.tvModel);
            tvYear = itemView.findViewById(R.id.tvYear);
        }

        // Vincula los datos de un vehículo a los controles del item
        public void bind(Vehicle vehicle) {
            tvBrand.setText(vehicle.getBrand());
            tvModel.setText(vehicle.getModel());
            tvYear.setText(vehicle.getYear());
        }
    }
}
