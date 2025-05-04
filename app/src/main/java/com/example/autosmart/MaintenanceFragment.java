package com.example.autosmart;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaintenanceFragment extends Fragment {
    private static final int RC_ADD_MAINT = 1001;

    private Spinner spinnerVehicles;
    private RecyclerView rv;
    private FloatingActionButton fab;
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

        View root = inflater.inflate(R.layout.fragment_maintenance, container, false);

        spinnerVehicles = root.findViewById(R.id.spinnerVehicle);
        rv              = root.findViewById(R.id.recyclerMaintenance);
        fab             = root.findViewById(R.id.fabAddMaintenance);

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
                dao.delete(m);
                Toast.makeText(getContext(), "🗑️ Mantenimiento eliminado", Toast.LENGTH_SHORT).show();
            }
        });
        rv.setAdapter(adapter);

        // Spinner: "Todos los vehículos" + setup
        vehicleLabels.clear();
        vehicleIds.clear();
        vehicleLabelById.clear();
        vehicleLabels.add("🔧 Todos los vehículos");
        vehicleIds   .add(null);
        vehicleSpinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                vehicleLabels
        );
        vehicleSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicles.setAdapter(vehicleSpinnerAdapter);

        // Listener de filtrado
        spinnerVehicles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) { }
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String vehId = vehicleIds.get(pos);
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                
                // Debug: Mostrar información
                int count = dao.countMaintenanceForUser(userId);
                Toast.makeText(getContext(), 
                    "Debug: UserID=" + userId + 
                    ", VehID=" + vehId + 
                    ", Total mantenimientos=" + count, 
                    Toast.LENGTH_LONG).show();

                if (vehId == null) {
                    // TODOS
                    dao.loadAll(userId).observe(getViewLifecycleOwner(), list -> {
                        adapter.setItems(list);
                        // Debug: Mostrar tamaño de la lista
                        Toast.makeText(getContext(), 
                            "Debug: Total registros cargados=" + (list != null ? list.size() : 0), 
                            Toast.LENGTH_SHORT).show();
                    });
                } else {
                    // SOLO ESE VEHÍCULO
                    dao.loadForVehicle(userId, vehId).observe(getViewLifecycleOwner(), list -> {
                        adapter.setItems(list);
                        // Debug: Mostrar tamaño de la lista
                        Toast.makeText(getContext(), 
                            "Debug: Registros para vehículo=" + (list != null ? list.size() : 0), 
                            Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });

        // Trae tus vehículos para el filtro
        loadVehiclesIntoSpinner();

        // Forzar selección inicial para disparar "Todos los vehículos"
        spinnerVehicles.post(() -> spinnerVehicles.setSelection(0, true));

        // FAB → añadir nuevo mantenimiento
        fab.setOnClickListener(v ->
                startActivityForResult(
                        new Intent(getContext(), AddMaintenanceActivity.class),
                        RC_ADD_MAINT
                )
        );

        return root;
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
                                String label = "🚗 " + v.getBrand()
                                        + " " + v.getModel()
                                        + " (" + v.getYear() + ")";
                                vehicleLabels.add(label);
                                vehicleIds   .add(v.getId());
                                vehicleLabelById.put(v.getId(), label);
                            }
                        }
                        vehicleSpinnerAdapter.notifyDataSetChanged();
                        adapter.setLabelById(new HashMap<>(vehicleLabelById));
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
            Toast.makeText(getContext(),
                    "✅ Mantenimiento guardado",
                    Toast.LENGTH_SHORT
            ).show();
            // LiveData vuelve a dispararse automáticamente
        }
    }
}
