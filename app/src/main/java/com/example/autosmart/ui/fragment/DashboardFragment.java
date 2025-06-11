package com.example.autosmart.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.util.Log;

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
    private AutoCompleteTextView spinnerHistoryVehicle;
    private java.util.List<VehicleEntity> vehicleList = new java.util.ArrayList<>();
    private java.util.List<MaintenanceEntity> allMaintenances = new java.util.ArrayList<>();
    private TextView emptyStateTextView;
    private ArrayAdapter<String> vehicleAdapter;

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
        tvNoUpcoming.setText("No hay próximo mantenimiento");
        tvNoUpcoming.setTextSize(16);
        tvNoUpcoming.setTextColor(getResources().getColor(R.color.purple_700));
        tvNoUpcoming.setPadding(32, 32, 32, 32);
        rvGlobalMaintenance = view.findViewById(R.id.rvGlobalMaintenance);
        spinnerHistoryVehicle = view.findViewById(R.id.spinnerHistoryVehicle);

        // NUEVO: Inicializar el mensaje de estado vacío
        emptyStateTextView = new TextView(getContext());
        emptyStateTextView.setText("No hay mantenimientos registrados para este vehículo");
        emptyStateTextView.setTextSize(16);
        emptyStateTextView.setTextColor(getResources().getColor(R.color.purple_700));
        emptyStateTextView.setGravity(android.view.Gravity.CENTER);
        emptyStateTextView.setVisibility(View.GONE);
        // Añadir el mensaje al layout si no está ya
        ViewGroup parent = (ViewGroup) rvGlobalMaintenance.getParent();
        if (emptyStateTextView.getParent() == null) {
            parent.addView(emptyStateTextView, parent.indexOfChild(rvGlobalMaintenance) + 1);
        }

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
        updateGreeting();
        // Actualizar estadísticas
        updateStatistics();
        showNextMaintenance();
        updateGlobalMaintenance();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateGreeting();
    }

    private void updateGreeting() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE);
        String name = prefs.getString("username", "conductor");
        tvGreeting.setText("Hola, " + name);
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

    private void showNextMaintenance() {
        // Obtener el timestamp de hoy a las 00:00
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        long todayTimestamp = cal.getTimeInMillis();
        AppDatabase db = AppDatabase.getInstance(requireContext());
        MaintenanceEntity next = db.maintenanceDao().getNextMaintenance(todayTimestamp);
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
            tvNoUpcoming.setText("No hay próximo mantenimiento");
            tvNoUpcoming.setVisibility(View.VISIBLE);
        }
    }

    private void setupHistoryVehicleSpinner() {
        AppDatabase db = AppDatabase.getInstance(requireContext());
        String userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.vehicleDao().loadAll(userId).observe(getViewLifecycleOwner(), vehicles -> {
            vehicleList = vehicles != null ? vehicles : new java.util.ArrayList<>();
            Log.d("DASHBOARD", "Vehículos cargados: " + vehicleList.size());
            java.util.List<String> options = new java.util.ArrayList<>();
            options.add("Todos los vehículos");
            for (VehicleEntity v : vehicleList) {
                String label = v.getBrand() + " " + v.getModel() + " (" + v.getYear() + ")";
                options.add(label);
            }
            Log.d("DASHBOARD", "Opciones en el spinner: " + options);
            if (vehicleAdapter == null) {
                vehicleAdapter = new ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_dropdown_item_1line, options);
                spinnerHistoryVehicle.setAdapter(vehicleAdapter);
            } else {
                vehicleAdapter.clear();
                vehicleAdapter.addAll(options);
                vehicleAdapter.notifyDataSetChanged();
            }
            spinnerHistoryVehicle.setText(options.get(0), false);
        });
        spinnerHistoryVehicle.setOnItemClickListener((parent, view, position, id) -> {
            filterHistoryByVehicle(position);
        });
    }

    private void updateGlobalMaintenance() {
        AppDatabase db = AppDatabase.getInstance(requireContext());
        String userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        allMaintenances = db.maintenanceDao().getAllForUser(userId);
        Log.d("DASHBOARD", "Mantenimientos cargados: " + allMaintenances.size());
        int pos = 0;
        if (spinnerHistoryVehicle != null && vehicleAdapter != null) {
            String selected = spinnerHistoryVehicle.getText().toString();
            pos = vehicleAdapter.getPosition(selected);
        }
        filterHistoryByVehicle(pos);
    }

    private void filterHistoryByVehicle(int pos) {
        java.util.List<MaintenanceEntity> filtered;
        if (pos == 0 || vehicleList == null || vehicleList.isEmpty()) {
            filtered = allMaintenances;
        } else if (pos > 0 && vehicleList.size() >= pos) {
            String selectedVehicleId = vehicleList.get(pos - 1).getId();
            filtered = new java.util.ArrayList<>();
            for (MaintenanceEntity m : allMaintenances) {
                if (m.vehicleId != null && m.vehicleId.equals(selectedVehicleId)) {
                    filtered.add(m);
                }
            }
        } else {
            filtered = new java.util.ArrayList<>();
        }
        Log.d("DASHBOARD", "Filtrando mantenimientos para pos=" + pos + ", encontrados: " + filtered.size());
        globalAdapter.setItems(filtered);
        // Mostrar/ocultar según si hay datos
        if (filtered == null || filtered.isEmpty()) {
            rvGlobalMaintenance.setVisibility(View.GONE);
            emptyStateTextView.setVisibility(View.VISIBLE);
        } else {
            rvGlobalMaintenance.setVisibility(View.VISIBLE);
            emptyStateTextView.setVisibility(View.GONE);
        }
        // El spinner siempre debe estar visible (ya está en el layout)
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
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String formattedDate = sdf.format(new Date(maintenance.date));
            holder.title.setText(maintenance.type + " - " + maintenance.vehiclePlate);
            holder.subtitle.setText(formattedDate + (maintenance.description != null && !maintenance.description.isEmpty() ? ("\n" + maintenance.description) : ""));
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
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String formattedDate = sdf.format(new Date(m.date));
        h.tvDate.setText(formattedDate);
        // Centrado y alineación
        h.tvType.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
        h.tvMileage.setGravity(android.view.Gravity.CENTER);
        h.tvDate.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.END);
        // Elimina cualquier estilo especial para eliminados
        h.tvType.setTextColor(android.graphics.Color.parseColor("#6A1B9A")); // purple_700
        h.tvMileage.setTextColor(android.graphics.Color.parseColor("#6A1B9A"));
        h.tvDate.setTextColor(android.graphics.Color.parseColor("#6A1B9A"));
        h.tvType.setPaintFlags(h.tvType.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
        h.tvMileage.setPaintFlags(h.tvMileage.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
        h.tvDate.setPaintFlags(h.tvDate.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
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
