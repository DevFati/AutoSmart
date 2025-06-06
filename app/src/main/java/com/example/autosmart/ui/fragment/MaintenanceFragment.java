package com.example.autosmart.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autosmart.ui.adapter.MaintenanceAdapter;
import com.example.autosmart.R;
import com.example.autosmart.data.dao.MaintenanceDao;
import com.example.autosmart.data.db.AppDatabase;
import com.example.autosmart.data.db.MaintenanceEntity;
import com.example.autosmart.model.Vehicle;
import com.example.autosmart.ui.activity.AddMaintenanceActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaintenanceFragment extends Fragment {
    private static final int RC_ADD_MAINT = 1001;

    private AutoCompleteTextView spinnerVehicles;
    private RecyclerView rv;
    private MaterialButton fab;
    private TextView tvNoVehiclesMaint;
    private CardView emptyState;

    private MaintenanceAdapter adapter;
    private MaintenanceDao dao;

    // Datos del spinner
    private final List<String> vehicleLabels = new ArrayList<>();
    private final List<String> vehicleIds    = new ArrayList<>();
    private final Map<String,String> vehicleLabelById = new HashMap<>();
    private ArrayAdapter<String> vehicleSpinnerAdapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(com.example.autosmart.R.layout.fragment_maintenance, container, false);

        spinnerVehicles = root.findViewById(com.example.autosmart.R.id.spinnerVehicle);
        rv              = root.findViewById(com.example.autosmart.R.id.recyclerMaintenance);
        fab             = root.findViewById(com.example.autosmart.R.id.fabAddMaintenance);
        emptyState      = root.findViewById(com.example.autosmart.R.id.emptyState);
        tvNoVehiclesMaint = root.findViewById(com.example.autosmart.R.id.tvNoVehiclesMaint);

        // DAO + Recycler
        dao = AppDatabase.getInstance(requireContext()).maintenanceDao();
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MaintenanceAdapter(vehicleLabelById, new MaintenanceAdapter.OnItemActionListener() {
            @Override
            public void onEdit(MaintenanceEntity m) {
                Intent i = new Intent(getContext(), AddMaintenanceActivity.class);
                i.putExtra(AddMaintenanceActivity.EXTRA_MAINT_ID, m.id);
                startActivityForResult(i, RC_ADD_MAINT);
            }
            @Override
            public void onDelete(MaintenanceEntity m) {
                m.isDeleted = true;
                dao.update(m);
                Toast.makeText(getContext(), "Mantenimiento eliminado (historial permanente)", Toast.LENGTH_SHORT).show();
            }
        });
        rv.setAdapter(adapter);

        // Setup del AutoCompleteTextView
        vehicleLabels.clear();
        vehicleIds.clear();
        vehicleLabelById.clear();
        vehicleLabels.add("Todos los vehículos");

        vehicleIds   .add(null);
        vehicleSpinnerAdapter = new ArrayAdapter<String>(
                requireContext(),
                com.example.autosmart.R.layout.item_vehicle_dropdown,
                com.example.autosmart.R.id.tvVehicleName,
                vehicleLabels
        ) {
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                ImageView icon = view.findViewById(com.example.autosmart.R.id.ivVehicleIcon);
                // Puedes personalizar el icono según el tipo de vehículo si lo deseas
                // icon.setImageResource(...);
                return view;
            }
        };
        spinnerVehicles.setAdapter(vehicleSpinnerAdapter);

        // Listener de selección
        spinnerVehicles.setOnItemClickListener((parent, view, pos, id) -> {
            filterMaintenancesByVehicle(pos);
        });

        // Trae tus vehículos para el filtro
        loadVehiclesIntoSpinner();

        fab.setOnClickListener(v -> {
            startActivityForResult(
                new Intent(getContext(), AddMaintenanceActivity.class),
                RC_ADD_MAINT
            );
        });

        return root;
    }

    private void filterMaintenancesByVehicle(int pos) {
        String vehId = vehicleIds.get(pos);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (vehId == null) {
            // TODOS
            dao.loadAll(userId).observe(getViewLifecycleOwner(), list -> {
                adapter.setItems(list);
                if (list == null || list.isEmpty()) {
                    rv.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                } else {
                    rv.setVisibility(View.VISIBLE);
                    emptyState.setVisibility(View.GONE);
                }
            });
        } else {
            // SOLO ESE VEHÍCULO
            dao.loadForVehicle(userId, vehId).observe(getViewLifecycleOwner(), list -> {
                adapter.setItems(list);
                if (list == null || list.isEmpty()) {
                    rv.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                } else {
                    rv.setVisibility(View.VISIBLE);
                    emptyState.setVisibility(View.GONE);
                }
            });
        }
    }

    /** Carga vehículos del usuario y mantiene "Todos…" en posición 0 */
    private void loadVehiclesIntoSpinner() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase
                .getInstance("https://autosmart-6e3c3-default-rtdb.firebaseio.com")
                .getReference("vehicles");

        ref.orderByChild("userId").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        // limpia solo índices >=1
                        if (vehicleLabels.size() > 1) {
                            vehicleLabels.subList(1, vehicleLabels.size()).clear();
                            vehicleIds   .subList(1, vehicleIds.size()).clear();
                            vehicleLabelById.clear();
                        }
                        for (DataSnapshot ds : snap.getChildren()) {
                            Vehicle v = ds.getValue(Vehicle.class);
                            if (v != null) {
                                String label = v.getBrand()
                                        + " " + v.getModel()
                                        + " (" + v.getYear() + ")";
                                vehicleLabels.add(label);
                                vehicleIds   .add(v.getId());
                                vehicleLabelById.put(v.getId(), label);
                            }
                        }
                        vehicleSpinnerAdapter.notifyDataSetChanged();
                        adapter.setLabelById(new HashMap<>(vehicleLabelById));
                        if (vehicleLabels.size() <= 1) {
                            tvNoVehiclesMaint.setVisibility(View.VISIBLE);
                            spinnerVehicles.setVisibility(View.GONE);
                            rv.setVisibility(View.GONE);
                            fab.setVisibility(View.GONE);
                            emptyState.setVisibility(View.GONE);
                        } else {
                            tvNoVehiclesMaint.setVisibility(View.GONE);
                            spinnerVehicles.setVisibility(View.VISIBLE);
                            rv.setVisibility(View.VISIBLE);
                            fab.setVisibility(View.VISIBLE);
                            emptyState.setVisibility(View.GONE);
                            // Dispara el filtro inicial correctamente
                            spinnerVehicles.post(() -> {
                                spinnerVehicles.setText("Todos los vehículos", false);
                                filterMaintenancesByVehicle(0);
                            });
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError err) {
                        Toast.makeText(getContext(),
                                "Error cargando vehículos: " + err.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_ADD_MAINT && resultCode == Activity.RESULT_OK) {
            // showSavedSnackbar(); // Eliminado para que no tape el botón
            // LiveData vuelve a dispararse automáticamente
        }
    }

    private void showSavedSnackbar() {
        View rootView = getView();
        if (rootView == null) return;
        Snackbar snackbar = Snackbar.make(rootView, "  Mantenimiento guardado", Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.blue_500));
        snackbar.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        snackbar.setAction("OK", v -> {});
        snackbar.show();
    }
}
