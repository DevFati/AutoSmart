package com.example.autosmart;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;
import java.util.List;

public class VehiclesFragment extends Fragment {

    private RecyclerView recyclerView;
    private VehicleAdapter adapter;
    private ProgressBar progress;
    private final ArrayList<Vehicle> vehicleList = new ArrayList<>();
    private DatabaseReference vehiclesRef;

    private static final int RC_ADD_VEHICLE = 1001;
    String DB_URL = "https://autosmart-6e3c3-default-rtdb.firebaseio.com";

    public VehiclesFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vehicles, container, false);
        recyclerView = view.findViewById(R.id.recyclerVehicles);
        progress     = view.findViewById(R.id.progressVehicles);
        FloatingActionButton fab = view.findViewById(R.id.fabAddVehicle);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new VehicleAdapter(vehicleList);
        recyclerView.setAdapter(adapter);

        // Larga pulsación para editar/eliminar
        adapter.setOnItemLongClickListener((vehicle, pos) -> {
            showItemMenu(vehicle);
            return true;
        });


        // FAB: abrir formulario para nuevo vehículo
        fab.setOnClickListener(v -> {
            Intent i = new Intent(getContext(), AddVehicleActivity.class);
            startActivityForResult(i, RC_ADD_VEHICLE);
        });



        // Firebase
        vehiclesRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("vehicles");

        loadVehicles();

        return view;
    }

    private void loadVehicles() {
        progress.setVisibility(View.VISIBLE);
        vehiclesRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                vehicleList.clear();
                for (DataSnapshot ds : snap.getChildren()) {
                    Vehicle v = ds.getValue(Vehicle.class);
                    if (v != null) {
                        v.setId(ds.getKey());
                        vehicleList.add(v);
                    }
                }
                adapter.notifyDataSetChanged();
                progress.setVisibility(View.GONE);
            }
            @Override public void onCancelled(@NonNull DatabaseError err) {
                progress.setVisibility(View.GONE);
            }
        });
    }

    private void showItemMenu(Vehicle vehicle) {
        String[] options = {"Editar", "Eliminar"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Acción")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Editar
                        Intent i = new Intent(getContext(), AddVehicleActivity.class);
                        i.putExtra("vehicleId", vehicle.getId());
                        i.putExtra("brand", vehicle.getBrand());
                        i.putExtra("model", vehicle.getModel());
                        i.putExtra("year", vehicle.getYear());
                        i.putExtra("engineType", vehicle.getEngineType());
                        startActivityForResult(i, RC_ADD_VEHICLE);
                    } else {
                        // Eliminar
                        vehiclesRef.child(vehicle.getId()).removeValue();
                    }
                })
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_ADD_VEHICLE && resultCode == getActivity().RESULT_OK) {
            // tras añadir o editar, recargamos la lista
            loadVehicles();
        }
    }

}
