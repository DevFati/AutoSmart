package com.example.autosmart.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autosmart.R;
import com.example.autosmart.ui.adapter.VehicleAdapter;
import com.example.autosmart.data.dao.MaintenanceDao;
import com.example.autosmart.data.dao.VehicleDao;
import com.example.autosmart.data.db.AppDatabase;
import com.example.autosmart.data.db.VehicleEntity;
import com.example.autosmart.model.Vehicle;
import com.example.autosmart.ui.activity.AddVehicleActivity;
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
    private VehicleDao vehicleDao;
    private TextView tvNoVehicles;

    // Referencia raíz y consulta filtrada
    private DatabaseReference rootRef;
    private Query    vehiclesQuery;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(com.example.autosmart.R.layout.fragment_vehicles, container, false);

        recyclerView = root.findViewById(com.example.autosmart.R.id.recyclerVehicles);
        progressBar  = root.findViewById(com.example.autosmart.R.id.progressVehicles);
        fab          = root.findViewById(com.example.autosmart.R.id.fabAddVehicle);
        tvNoVehicles = root.findViewById(com.example.autosmart.R.id.tvNoVehicles);
        maintenanceDao = AppDatabase.getInstance(requireContext()).maintenanceDao();
        vehicleDao = AppDatabase.getInstance(requireContext()).vehicleDao();

        // 1) Configura RecyclerView + Adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new VehicleAdapter(vehicleList);
        recyclerView.setAdapter(adapter);

        // 2.5) Configura short click para abrir detalle
        adapter.setOnItemClickListener((vehicle, pos) -> {
            Bundle args = new Bundle();
            args.putString("vehicleId", vehicle.getId());
            VehicleDetailFragment fragment = new VehicleDetailFragment();
            fragment.setArguments(args);
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(com.example.autosmart.R.id.vehicles_coordinator, fragment)
                .addToBackStack(null)
                .commit();
            fab.setVisibility(View.GONE);
        });

        // Nuevo: Listener para eliminar
        adapter.setOnDeleteClickListener((vehicle, pos) -> {
            showDeleteMenu(vehicle, recyclerView.findViewHolderForAdapterPosition(pos).itemView);
        });
        // Nuevo: Listener para editar
        adapter.setOnEditClickListener((vehicle, pos) -> {
            Intent intent = new Intent(getActivity(), AddVehicleActivity.class);
            intent.putExtra("vehicleId", vehicle.getId());
            startActivity(intent);
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
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        vehiclesQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                vehicleList.clear();
                for (DataSnapshot child : snap.getChildren()) {
                    Vehicle veh = child.getValue(Vehicle.class);
                    if (veh != null) {
                        vehicleList.add(veh);
                        // Guardar en base de datos local
                        vehicleDao.insert(VehicleEntity.fromVehicle(veh));
                    }
                }
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                // Mostrar/ocultar mensaje de lista vacía
                if (vehicleList.isEmpty()) {
                    tvNoVehicles.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    tvNoVehicles.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
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
            .setIcon(com.example.autosmart.R.drawable.ic_delete)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar", (dialog, which) -> {
                // 1) Borra de Firebase
                rootRef.child(vehicle.getId()).removeValue()
                        .addOnSuccessListener(a -> {
                            // 2) Borra localmente todos sus mantenimientos
                            maintenanceDao.deleteForVehicle(vehicle.getId());
                            // 3) Borra el vehículo de la base de datos local
                            vehicleDao.delete(VehicleEntity.fromVehicle(vehicle));
                            // Mostrar Snackbar bonito y consistente
                            View rootView = getView();
                            if (rootView != null) {
                                com.google.android.material.snackbar.Snackbar snackbar = com.google.android.material.snackbar.Snackbar.make(
                                    rootView,
                                    "Vehículo y sus mantenimientos eliminados",
                                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                                );
                                snackbar.setBackgroundTint(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.red_500));
                                snackbar.setTextColor(android.graphics.Color.WHITE);
                                snackbar.setAction("OK", v2 -> {});
                                snackbar.show();
                            }
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
