package com.example.autosmart;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.autosmart.utils.EncryptionUtils;

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

    public interface OnItemClickListener {
        void onItemClick(Vehicle vehicle, int position);
    }

    public interface OnEditClickListener {
        void onEditClick(Vehicle vehicle, int position);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Vehicle vehicle, int position);
    }

    private List<Vehicle> vehicleList;
    private OnItemLongClickListener longClickListener;
    private OnItemClickListener clickListener;
    private OnEditClickListener editClickListener;
    private OnDeleteClickListener deleteClickListener;

    public VehicleAdapter(List<Vehicle> vehicleList) {
        this.vehicleList = vehicleList;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnEditClickListener(OnEditClickListener listener) {
        this.editClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
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
        Vehicle v = vehicleList.get(position);
        holder.bind(v);

        // Construye el dominio a partir de la marca:
        // Ej. "BMW" → "bmw.com"
        String brandRaw = v.getBrand().trim().toLowerCase();
        String brandDomain = brandRaw
                .replaceAll("\\s+", "")               // quita espacios
                .replaceAll("[^a-z0-9\\-]", "");      // solo a–z, 0–9 y guiones
        String domain = brandDomain + ".com";

        // Tu token de RapidAPI para Logo.dev
        String token = "pk_B-lbxINDRYCQG3JiIspqjg";

        String url = "https://img.logo.dev/" + domain
                + "?token=" + token
                + "&retina=true";
        Log.d("VehicleAdapter", "Cargando logo desde: " + url);

        // Carga con Glide
        Glide.with(holder.itemView.getContext())
                .load(url)
                .placeholder(R.drawable.ic_car_placeholder)
                .error(R.drawable.ic_car_placeholder)
                .into(holder.imgBrandLogo);

        holder.itemView.setOnClickListener(view -> {
            if (clickListener != null) {
                clickListener.onItemClick(v, holder.getAdapterPosition());
            }
        });

        holder.itemView.setOnLongClickListener(vw -> {
            if (longClickListener != null) {
                return longClickListener.onItemLongClick(v, holder.getAdapterPosition());
            }
            return false;
        });

        // Botón editar
        holder.btnEdit.setOnClickListener(view -> {
            if (editClickListener != null) {
                editClickListener.onEditClick(v, holder.getAdapterPosition());
            }
        });

        // Botón eliminar
        holder.btnDelete.setOnClickListener(view -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDeleteClick(v, holder.getAdapterPosition());
            }
        });
    }


    @Override
    public int getItemCount() {
        return vehicleList.size();
    }

    static class VehicleViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBrandLogo;
        TextView tvBrandModel, tvYear, tvEngine, tvPlate;
        android.widget.ImageButton btnEdit, btnDelete;


        public VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBrandLogo = itemView.findViewById(R.id.imgBrandLogo);
            tvBrandModel = itemView.findViewById(R.id.tvBrandModel);
            tvYear       = itemView.findViewById(R.id.tvYear);
            tvEngine     = itemView.findViewById(R.id.tvEngine);
            tvPlate      = itemView.findViewById(R.id.tvPlate);
            btnEdit      = itemView.findViewById(R.id.btnEditVehicle);
            btnDelete    = itemView.findViewById(R.id.btnDeleteVehicle);
        }

        public void bind(Vehicle v) {
            tvBrandModel.setText(v.getBrand() + " " + v.getModel());
            tvYear.setText(v.getYear());
            tvEngine.setText(v.getEngineType());
            String plate;
            try {
                plate = EncryptionUtils.decrypt(v.getPlate());
            } catch (Exception e) {
                plate = v.getPlate();
            }
            tvPlate.setText(plate);
        }
    }

}
