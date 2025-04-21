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

        // 1) Configura RecyclerView + Adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new VehicleAdapter(vehicleList);
        recyclerView.setAdapter(adapter);

        // 2) Configura long‑click para eliminar
        adapter.setOnItemLongClickListener((vehicle, pos) -> {
            showDeleteMenu(vehicle, recyclerView.findViewHolderForAdapterPosition(pos).itemView);
            return true;
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

    /** Muestra solo la opción “Eliminar” en un PopupMenu */
    private void showDeleteMenu(Vehicle vehicle, View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add("Eliminar");
        menu.setOnMenuItemClickListener(item -> {
            rootRef.child(vehicle.getId()).removeValue()
                    .addOnSuccessListener(a ->
                            Toast.makeText(getContext(),
                                    "Vehículo eliminado",
                                    Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(),
                                    "Error al eliminar: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
            return true;
        });
        menu.show();
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
