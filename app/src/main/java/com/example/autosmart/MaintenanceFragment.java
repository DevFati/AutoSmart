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

    private Spinner spinnerVehicles;
    private RecyclerView rv;
    private FloatingActionButton fab;
    private MaintenanceAdapter adapter;
    private MaintenanceDao dao;

    // Para el spinner de vehículos
    private List<String> vehicleLabels = new ArrayList<>();
    private List<String> vehicleIds    = new ArrayList<>();
    private ArrayAdapter<String> vehicleSpinnerAdapter;
    private static final int RC_EDIT_MAINT = 1002;
    public static final String EXTRA_MAINT_ID   = "maintenanceId";

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inf,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inf.inflate(R.layout.fragment_maintenance, container, false);
        dao = AppDatabase.getInstance(requireContext()).maintenanceDao();

        spinnerVehicles = v.findViewById(R.id.spinnerVehicle);
        rv              = v.findViewById(R.id.recyclerMaintenance);
        fab             = v.findViewById(R.id.fabAddMaintenance);

        // 1) Configura RecyclerView
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MaintenanceAdapter(new MaintenanceAdapter.OnItemActionListener() {
            @Override
            public void onEdit(MaintenanceEntity m) {
                // 1) Prepara el Intent pasándole el ID del mantenimiento
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

        rv.setAdapter(adapter);



        // 2) Prepara spinner de vehículos
        vehicleLabels.add("Selecciona vehículo");
        vehicleIds   .add(null);
        vehicleSpinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                vehicleLabels
        );
        vehicleSpinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );
        spinnerVehicles.setAdapter(vehicleSpinnerAdapter);

        // 3) Carga vehículos de Firebase sólo del usuario actual
        loadVehiclesIntoSpinner();

        // 4) Cuando elige un vehículo, observa sólo sus mantenimientos
        dao.loadForVehicle("*")
                .observe(getViewLifecycleOwner(), list -> adapter.setItems(list));

        spinnerVehicles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos == 0) {
                    // Mostrar **todos** los mantenimientos
                    dao.loadAll()
                            .observe(getViewLifecycleOwner(), list -> adapter.setItems(list));
                } else {
                    String vehId = vehicleIds.get(pos);
                    dao.loadForVehicle(vehId)
                            .observe(getViewLifecycleOwner(), list -> adapter.setItems(list));
                }
            }
        });

        // 5) FAB → abrir AddMaintenanceActivity
        fab.setOnClickListener(x -> {
            Intent i = new Intent(getContext(), AddMaintenanceActivity.class);
            startActivityForResult(i, RC_ADD_MAINT);
        });

        return v;
    }

    /** Carga del árbol “vehicles” filtrado por userId */
    private void loadVehiclesIntoSpinner() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase
                .getInstance("https://autosmart-6e3c3-default-rtdb.firebaseio.com")
                .getReference("vehicles");

        ref.orderByChild("userId")
                .equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        // limpia dejando sólo el “Selecciona vehículo”
                        vehicleLabels.clear(); vehicleLabels.add("Selecciona vehículo");
                        vehicleIds   .clear(); vehicleIds   .add(null);

                        for (DataSnapshot ds : snap.getChildren()) {
                            Vehicle v = ds.getValue(Vehicle.class);
                            if (v != null) {
                                vehicleLabels.add(v.getBrand() + " " + v.getModel());
                                vehicleIds   .add(v.getId());
                            }
                        }
                        vehicleSpinnerAdapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError err) {
                        Toast.makeText(getContext(),
                                "Error cargando vehículos: " + err.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    @Override
    public void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if ((req == RC_ADD_MAINT || req == RC_EDIT_MAINT) && res == Activity.RESULT_OK) {
            String msg = req == RC_ADD_MAINT ?
                    "Mantenimiento añadido" : "Mantenimiento actualizado";
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            // LiveData ya refresca automáticamente
        }
    }

}
