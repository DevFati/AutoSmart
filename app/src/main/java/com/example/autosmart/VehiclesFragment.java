package com.example.autosmart;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class VehiclesFragment extends Fragment {
    private static final int RC_ADD_VEHICLE = 5001;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FloatingActionButton fab;
    private VehicleAdapter adapter;
    private List<Vehicle> vehicleList = new ArrayList<>();
    private MaintenanceDao maintenanceDao;

    // Referencia raíz y consulta filtrada
    private DatabaseReference rootRef;
    private Query    vehiclesQuery;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_vehicles, container, false);

        recyclerView = root.findViewById(R.id.recyclerVehicles);
        progressBar  = root.findViewById(R.id.progressVehicles);
        fab          = root.findViewById(R.id.fabAddVehicle);
        maintenanceDao = AppDatabase.getInstance(requireContext()).maintenanceDao();

        // 1) Configura RecyclerView + Adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new VehicleAdapter(vehicleList);
        recyclerView.setAdapter(adapter);

        // 2) Configura long‑click para eliminar
        adapter.setOnItemLongClickListener((vehicle, pos) -> {
            showDeleteMenu(vehicle, recyclerView.findViewHolderForAdapterPosition(pos).itemView);
            return true;
        });

        // 2.5) Configura short click para abrir detalle
        adapter.setOnItemClickListener((vehicle, pos) -> {
            Bundle args = new Bundle();
            args.putString("vehicleId", vehicle.getId());
            VehicleDetailFragment fragment = new VehicleDetailFragment();
            fragment.setArguments(args);
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.vehicles_coordinator, fragment)
                .addToBackStack(null)
                .commit();
            fab.setVisibility(View.GONE);
        });

        // 3) Prepara Firebase:
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        rootRef = FirebaseDatabase
                .getInstance("https://autosmart-6e3c3-default-rtdb.firebaseio.com/")
                .getReference("vehicles");
        vehiclesQuery = rootRef.orderByChild("userId").equalTo(uid);

        // 4) Lee solo tus vehículos
        loadVehiclesFromFirebase();

        // 5) FAB → AddVehicleActivity
        fab.setOnClickListener(v -> startActivityForResult(
                new Intent(getActivity(), AddVehicleActivity.class),
                RC_ADD_VEHICLE
        ));

        return root;
    }

    private void loadVehiclesFromFirebase() {
        progressBar.setVisibility(View.VISIBLE);

        vehiclesQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                vehicleList.clear();
                for (DataSnapshot child : snap.getChildren()) {
                    Vehicle veh = child.getValue(Vehicle.class);
                    if (veh != null) {
                        vehicleList.add(veh);
                    }
                }
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError err) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(),
                        "Error cargando vehículos: " + err.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Muestra solo la opción "Eliminar" en un PopupMenu */
    private void showDeleteMenu(Vehicle vehicle, View anchor) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("¿Eliminar vehículo?")
            .setMessage("Esta acción no se puede deshacer. ¿Seguro que quieres eliminar este vehículo?")
            .setIcon(R.drawable.ic_delete)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar", (dialog, which) -> {
                // 1) Borra de Firebase
                rootRef.child(vehicle.getId()).removeValue()
                        .addOnSuccessListener(a -> {
                            // 2) Borra localmente todos sus mantenimientos
                            maintenanceDao.deleteForVehicle(vehicle.getId());
                            Toast.makeText(getContext(),
                                    "Vehículo y sus mantenimientos eliminados",
                                    Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(),
                                        "Error al eliminar: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show()
                        );
            })
            .show();
    }

    @Override
    public void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (req == RC_ADD_VEHICLE && res == Activity.RESULT_OK) {
            Toast.makeText(getContext(),
                    "Vehículo añadido correctamente",
                    Toast.LENGTH_SHORT).show();
            // La consulta filtrada volverá a dispararse y recargará la lista
        }
    }
}
