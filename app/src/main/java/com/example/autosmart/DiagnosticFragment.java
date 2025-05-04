package com.example.autosmart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DiagnosticFragment extends Fragment {
    private Spinner spinnerVehicles;
    private Button btnFetch;
    private RecyclerView rvSuggestions;
    private SuggestionsAdapter suggestionsAdapter;
    private ApiNinjasService api;

    // Para poblar el Spinner
    private List<Vehicle> vehiclesList = new ArrayList<>();
    private List<String>  vehicleLabels = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inf.inflate(R.layout.fragment_diagnostic, container, false);

        spinnerVehicles = v.findViewById(R.id.spinnerVehicleDiag);
        btnFetch        = v.findViewById(R.id.btnFetchDiag);
        rvSuggestions   = v.findViewById(R.id.rvSuggestions);

        // 1) RecyclerView para sugerencias
        rvSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        suggestionsAdapter = new SuggestionsAdapter();
        rvSuggestions.setAdapter(suggestionsAdapter);

        // 2) Spinner vacío con placeholder
        vehicleLabels.add("Selecciona vehículo");
        vehiclesList.add(null);
        spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                vehicleLabels
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicles.setAdapter(spinnerAdapter);

        // 3) Carga tus vehículos desde Firebase
        loadMyVehiclesIntoSpinner();

        // 4) Configura Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.api-ninjas.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(ApiNinjasService.class);

        // 5) Al pulsar “Fetch”
        btnFetch.setOnClickListener(b -> {
            int pos = spinnerVehicles.getSelectedItemPosition();
            Vehicle veh = vehiclesList.get(pos);
            if (veh == null) {
                Toast.makeText(getContext(),
                        "Selecciona un vehículo",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            fetchMaintenanceSuggestions(veh);
        });

        return v;
    }

    /** Trae únicamente los vehículos de este usuario y actualiza el Spinner */
    private void loadMyVehiclesIntoSpinner() {
        String uid = FirebaseAuth.getInstance()
                .getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase
                .getInstance("https://autosmart-6e3c3-default-rtdb.firebaseio.com")
                .getReference("vehicles");

        ref.orderByChild("userId")
                .equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        vehicleLabels.clear();
                        vehiclesList.clear();
                        // placeholder
                        vehicleLabels.add("Selecciona vehículo");
                        vehiclesList.add(null);

                        for (DataSnapshot ds : snap.getChildren()) {
                            Vehicle v = ds.getValue(Vehicle.class);
                            if (v != null) {
                                vehiclesList.add(v);
                                vehicleLabels.add(
                                        v.getBrand() + " " + v.getModel() + " (" + v.getYear() + ")"
                                );
                            }
                        }
                        spinnerAdapter.notifyDataSetChanged();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError err) {
                        Toast.makeText(getContext(),
                                "Error cargando vehículos: " + err.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    /** Llama a la API de mantenimiento y actualiza el RecyclerView */
    private void fetchMaintenanceSuggestions(Vehicle v) {
        api.getMaintenance(
                getString(R.string.api_ninjas_key),
                v.getBrand(),
                v.getModel(),
                Integer.parseInt(v.getYear())
        ).enqueue(new Callback<List<MaintenanceSuggestion>>() {
            @Override public void onResponse(
                    Call<List<MaintenanceSuggestion>> call,
                    Response<List<MaintenanceSuggestion>> resp
            ) {
                if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                    suggestionsAdapter.submitList(resp.body());
                } else {
                    Toast.makeText(getContext(),
                            "No hay sugerencias disponibles",
                            Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(
                    Call<List<MaintenanceSuggestion>> call, Throwable t
            ) {
                Toast.makeText(getContext(),
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
