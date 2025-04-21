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
import java.util.List;

public class MaintenanceFragment extends Fragment {
    private static final int RC_ADD_MAINT = 1001;

    private RecyclerView recyclerView;
    private MaintenanceAdapter adapter;
    private MaintenanceDao dao;
    private FloatingActionButton fab;
    private Spinner spinnerVehicles;

    private ArrayAdapter<String> spinnerAdapter;
    private List<String> vehicleLabels = new ArrayList<>();
    private List<String> vehicleIds    = new ArrayList<>();

    private String uid;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_maintenance, container, false);

        // Bind views
        recyclerView    = v.findViewById(R.id.recyclerMaintenance);
        fab             = v.findViewById(R.id.fabAddMaintenance);
        spinnerVehicles = v.findViewById(R.id.spinnerVehicle);

        // Obtener usuario actual
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Inicializar DAO
        dao = AppDatabase.getInstance(requireContext()).maintenanceDao();

        // Setup RecyclerView + Adapter con edición y borrado
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MaintenanceAdapter(new MaintenanceAdapter.OnItemActionListener() {
            @Override
            public void onEdit(MaintenanceEntity m) {
                Intent i = new Intent(getContext(), AddMaintenanceActivity.class);
                i.putExtra(AddMaintenanceActivity.EXTRA_MAINT_ID, m.id);
                startActivityForResult(i, RC_ADD_MAINT);
            }
            @Override
            public void onDelete(MaintenanceEntity m) {
                dao.delete(m);
                Toast.makeText(getContext(), "Mantenimiento eliminado", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);

        // Setup spinner de vehículos
        vehicleLabels.clear();
        vehicleIds.clear();
        vehicleLabels.add("Todos los vehículos");
        vehicleIds.add(null);
        spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                vehicleLabels
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicles.setAdapter(spinnerAdapter);

        // Carga vehículos del usuario en el spinner
        loadVehiclesIntoSpinner();

        // Al cambiar selección, recarga lista de mantenimientos
        spinnerVehicles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) { }
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String vehId = vehicleIds.get(pos);
                if (vehId == null) {
                    // Mostrar todos los mantenimientos del usuario
                    dao.loadAllForUser(uid)
                            .observe(getViewLifecycleOwner(), list -> adapter.setItems(list));
                } else {
                    // Mostrar solo mantenimientos de ese vehículo
                    dao.loadForUserVehicle(uid, vehId)
                            .observe(getViewLifecycleOwner(), list -> adapter.setItems(list));
                }
            }
        });

        // FAB lanza AddMaintenanceActivity
        fab.setOnClickListener(x -> {
            Intent i = new Intent(getContext(), AddMaintenanceActivity.class);
            startActivityForResult(i, RC_ADD_MAINT);
        });

        return v;
    }

    /**
     * Carga del nodo "vehicles" filtrado por userId y llena el Spinner
     */
    private void loadVehiclesIntoSpinner() {
        DatabaseReference ref = FirebaseDatabase
                .getInstance("https://autosmart-6e3c3-default-rtdb.firebaseio.com")
                .getReference("vehicles");

        ref.orderByChild("userId")
                .equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        vehicleLabels.clear();
                        vehicleIds.clear();
                        vehicleLabels.add("Todos los vehículos");
                        vehicleIds.add(null);
                        for (DataSnapshot ds : snap.getChildren()) {
                            Vehicle v = ds.getValue(Vehicle.class);
                            if (v != null) {
                                vehicleLabels.add(v.getBrand() + " " + v.getModel() + " (" + v.getYear() + ")");
                                vehicleIds.add(v.getId());
                            }
                        }
                        spinnerAdapter.notifyDataSetChanged();
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
                    "Mantenimiento guardado",
                    Toast.LENGTH_SHORT).show();
            // El LiveData ya refresca automáticamente la lista
        }
    }
}
