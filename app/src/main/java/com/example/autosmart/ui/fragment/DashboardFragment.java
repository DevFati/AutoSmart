package com.example.autosmart.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autosmart.R;
import com.example.autosmart.data.db.AppDatabase;
import com.example.autosmart.data.db.MaintenanceEntity;
import com.example.autosmart.data.db.UserEntity;
import com.example.autosmart.data.db.VehicleEntity;
import com.example.autosmart.ui.activity.AddVehicleActivity;
import com.example.autosmart.ui.activity.DashboardActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardFragment extends Fragment {
    private TextView tvGreeting;
    private TextView tvVehiclesCount;
    private TextView tvMaintenanceCount;
    private RecyclerView rvUpcomingMaintenance;
    private View btnAddVehicle;
    private View btnDiagnosis;
    private View btnMaintenance;
    private TextView tvNoUpcoming;
    private UpcomingMaintenanceAdapter upcomingAdapter;
    private RecyclerView rvGlobalMaintenance;
    private GlobalMaintenanceAdapter globalAdapter;
    private android.widget.Spinner spinnerHistoryVehicle;
    private java.util.List<VehicleEntity> vehicleList = new java.util.ArrayList<>();
    private java.util.List<MaintenanceEntity> allMaintenances = new java.util.ArrayList<>();

    public DashboardFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Infla el layout para este fragment
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Inicializar vistas
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvVehiclesCount = view.findViewById(R.id.tvVehiclesCount);
        tvMaintenanceCount = view.findViewById(R.id.tvMaintenanceCount);
        rvUpcomingMaintenance = view.findViewById(R.id.rvUpcomingMaintenance);
        btnAddVehicle = view.findViewById(R.id.btnAddVehicle);
        btnDiagnosis = view.findViewById(R.id.btnDiagnosis);
        btnMaintenance = view.findViewById(R.id.btnMaintenance);
        tvNoUpcoming = new TextView(getContext());
        tvNoUpcoming.setText("No hay próximos mantenimientos");
        tvNoUpcoming.setTextSize(16);
        tvNoUpcoming.setTextColor(getResources().getColor(R.color.purple_700));
        tvNoUpcoming.setPadding(32, 32, 32, 32);
        rvGlobalMaintenance = view.findViewById(R.id.rvGlobalMaintenance);
        spinnerHistoryVehicle = view.findViewById(R.id.spinnerHistoryVehicle);

        // Configurar RecyclerView
        rvUpcomingMaintenance.setLayoutManager(new LinearLayoutManager(getContext()));
        upcomingAdapter = new UpcomingMaintenanceAdapter();
        rvUpcomingMaintenance.setAdapter(upcomingAdapter);
        rvGlobalMaintenance.setLayoutManager(new LinearLayoutManager(getContext()));
        globalAdapter = new GlobalMaintenanceAdapter();
        rvGlobalMaintenance.setAdapter(globalAdapter);

        // Configurar listeners de botones
        setupButtonListeners();
        setupHistoryVehicleSpinner();

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Recuperar el usuario de la base de datos local
        AppDatabase db = AppDatabase.getInstance(requireContext());
        UserEntity user = db.userDao().getUser();

        if (user != null) {
            // Forzar descifrado del nombre
            String nombre;
            try {
                nombre = com.example.autosmart.utils.EncryptionUtils.decrypt(user.getName());
            } catch (Exception e) {
                nombre = user.getName();
            }
            tvGreeting.setText("Hola, " + nombre);
        } else {
            tvGreeting.setText("Hola, conductor");
        }

        // Actualizar estadísticas
        updateStatistics();
        showNextMaintenance();
        updateGlobalMaintenance();
    }

    private void setupButtonListeners() {
        btnAddVehicle.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddVehicleActivity.class);
            startActivity(intent);
        });

        btnDiagnosis.setOnClickListener(v -> {
            ((DashboardActivity) requireActivity()).openMenuSection(R.id.nav_diagnostic);
        });

        btnMaintenance.setOnClickListener(v -> {
            ((DashboardActivity) requireActivity()).openMenuSection(R.id.nav_maintenance);
        });
    }

    private void updateStatistics() {
        AppDatabase db = AppDatabase.getInstance(requireContext());
        
        // Obtener conteo de vehículos
        int vehiclesCount = db.vehicleDao().getVehicleCount();
        tvVehiclesCount.setText(String.valueOf(vehiclesCount));

        // Obtener conteo de mantenimientos
        int maintenanceCount = db.maintenanceDao().getMaintenanceCount();
        tvMaintenanceCount.setText(String.valueOf(maintenanceCount));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Actualizar estadísticas cuando el fragmento se reanuda
        updateStatistics();
    }

    private void showNextMaintenance() {
        // Obtener la fecha de hoy en formato yyyy-MM-dd
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        AppDatabase db = AppDatabase.getInstance(requireContext());
        MaintenanceEntity next = db.maintenanceDao().getNextMaintenance(today);
        if (next != null) {
            rvUpcomingMaintenance.setVisibility(View.VISIBLE);
            tvNoUpcoming.setVisibility(View.GONE);
            upcomingAdapter.setMaintenance(next);
        } else {
            rvUpcomingMaintenance.setVisibility(View.GONE);
            // Añadir el mensaje si no está ya en el layout
            ViewGroup parent = (ViewGroup) rvUpcomingMaintenance.getParent();
            if (tvNoUpcoming.getParent() == null) {
                parent.addView(tvNoUpcoming, parent.indexOfChild(rvUpcomingMaintenance));
            }
            tvNoUpcoming.setText("No hay próximos mantenimientos");
            tvNoUpcoming.setVisibility(View.VISIBLE);
        }
    }

    private void setupHistoryVehicleSpinner() {
        AppDatabase db = AppDatabase.getInstance(requireContext());
        UserEntity user = db.userDao().getUser();
        if (user == null) return;
        String userId = user.getFirebaseUid();
        db.vehicleDao().loadAll(userId).observe(getViewLifecycleOwner(), vehicles -> {
            vehicleList = vehicles != null ? vehicles : new java.util.ArrayList<>();
            java.util.List<String> options = new java.util.ArrayList<>();
            options.add("Todos los vehículos");
            for (VehicleEntity v : vehicleList) {
                options.add(v.getBrand() + " " + v.getModel() + " (" + v.getYear() + ")");
            }
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                    requireContext(), android.R.layout.simple_spinner_item, options);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerHistoryVehicle.setAdapter(adapter);
            spinnerHistoryVehicle.setSelection(0);
        });
        spinnerHistoryVehicle.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                filterHistoryByVehicle(position);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void updateGlobalMaintenance() {
        AppDatabase db = AppDatabase.getInstance(requireContext());
        UserEntity user = db.userDao().getUser();
        if (user == null) return;
        String userId = user.getFirebaseUid();
        allMaintenances = db.maintenanceDao().getAllForUser(userId);
        filterHistoryByVehicle(spinnerHistoryVehicle != null ? spinnerHistoryVehicle.getSelectedItemPosition() : 0);
    }

    private void filterHistoryByVehicle(int pos) {
        if (pos == 0 || vehicleList == null || vehicleList.isEmpty()) {
            globalAdapter.setItems(allMaintenances);
        } else if (pos > 0 && vehicleList.size() >= pos) {
            String selectedVehicleId = vehicleList.get(pos - 1).getId();
            java.util.List<MaintenanceEntity> filtered = new java.util.ArrayList<>();
            for (MaintenanceEntity m : allMaintenances) {
                if (m.vehicleId != null && m.vehicleId.equals(selectedVehicleId)) {
                    filtered.add(m);
                }
            }
            globalAdapter.setItems(filtered);
        } else {
            globalAdapter.setItems(new java.util.ArrayList<>());
        }
    }
}

// Adapter para mostrar un solo mantenimiento
class UpcomingMaintenanceAdapter extends RecyclerView.Adapter<UpcomingMaintenanceAdapter.ViewHolder> {
    private MaintenanceEntity maintenance;

    public void setMaintenance(MaintenanceEntity m) {
        this.maintenance = m;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (maintenance != null) {
            holder.title.setText(maintenance.type + " - " + maintenance.vehiclePlate);
            holder.subtitle.setText(maintenance.date + (maintenance.description != null && !maintenance.description.isEmpty() ? ("\n" + maintenance.description) : ""));
        }
    }

    @Override
    public int getItemCount() {
        return maintenance == null ? 0 : 1;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        ViewHolder(View v) {
            super(v);
            title = v.findViewById(android.R.id.text1);
            subtitle = v.findViewById(android.R.id.text2);
        }
    }
}

// Adapter bonito para historial global
class GlobalMaintenanceAdapter extends RecyclerView.Adapter<GlobalMaintenanceAdapter.ViewHolder> {
    private java.util.List<MaintenanceEntity> items = new java.util.ArrayList<>();
    public void setItems(java.util.List<MaintenanceEntity> list) {
        this.items = list != null ? list : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_maintenance_table_row, parent, false);
        return new ViewHolder(v);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        MaintenanceEntity m = items.get(pos);
        h.tvType.setText(m.type);
        h.tvMileage.setText(String.format("%d km", m.kilometraje));
        h.tvDate.setText(m.date != null && m.date.length() >= 10 ? m.date.substring(0, 10) : m.date);
        // Centrado y alineación
        h.tvType.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
        h.tvMileage.setGravity(android.view.Gravity.CENTER);
        h.tvDate.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.END);
        // Línea divisoria
        if (h.getAdapterPosition() < getItemCount() - 1) {
            h.itemView.setBackgroundResource(android.R.color.darker_gray);
            h.itemView.setPadding(0, 0, 0, 1);
        } else {
            h.itemView.setBackgroundResource(android.R.color.transparent);
            h.itemView.setPadding(0, 0, 0, 0);
        }
    }
    @Override
    public int getItemCount() { return items.size(); }
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvMileage, tvDate;
        ViewHolder(View v) {
            super(v);
            tvType = v.findViewById(R.id.tvType);
            tvMileage = v.findViewById(R.id.tvMileage);
            tvDate = v.findViewById(R.id.tvDate);
        }
    }
}
