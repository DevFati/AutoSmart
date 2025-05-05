package com.example.autosmart;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddVehicleActivity extends AppCompatActivity {
    private Spinner spinnerYear, spinnerBrand, spinnerModel, spinnerEngine;
    private Button btnSaveVehicle;
    private CarApiService api;
    private DatabaseReference ref;
    private String vehicleId;
    private TextInputEditText etPlate;
    private TextInputLayout plateContainer;

    // Nuevas variables para almacenar IDs reales
    private List<String> makeIds = new ArrayList<>();
    private List<String> modelNames = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_vehicle);

        // Bind views
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerBrand = findViewById(R.id.spinnerBrand);
        spinnerModel = findViewById(R.id.spinnerModel);
        spinnerEngine = findViewById(R.id.spinnerEngine);
        btnSaveVehicle = findViewById(R.id.btnSaveVehicle);
        etPlate = findViewById(R.id.etPlate);
        plateContainer = findViewById(R.id.plateContainer);

        api = ApiClient.getRetrofit().create(CarApiService.class);
        ref = FirebaseDatabase.getInstance("https://autosmart-6e3c3-default-rtdb.firebaseio.com").getReference("vehicles");

        setupSpinners();
        loadYears();
        setupListeners();

        btnSaveVehicle.setOnClickListener(v -> saveVehicle());
    }

    private void setupSpinners() {
        ArrayAdapter<String> loadingAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                Collections.singletonList("Cargando...")
        );
        loadingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(loadingAdapter);
        spinnerBrand.setAdapter(loadingAdapter);
        spinnerModel.setAdapter(loadingAdapter);
        spinnerEngine.setAdapter(loadingAdapter);
    }

    private void setupListeners() {
        // 1) Al seleccionar año
        spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    // Mostrar el TextInputLayout de Marca
                    View brandInputLayout = (View) spinnerBrand.getParent().getParent();
                    brandInputLayout.setVisibility(View.VISIBLE);
                    loadMakes(parent.getItemAtPosition(position).toString());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No hace falta nada aquí
            }
        });

        // 2) Al seleccionar marca
        spinnerBrand.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && makeIds.size() >= position) {
                    // Mostrar el TextInputLayout de Modelo
                    View modelInputLayout = (View) spinnerModel.getParent().getParent();
                    modelInputLayout.setVisibility(View.VISIBLE);
                    String year = spinnerYear.getSelectedItem().toString();
                    String makeId = makeIds.get(position - 1);
                    loadModels(year, makeId);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // 3) Al seleccionar modelo
        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && modelNames.size() >= position) {
                    // Mostrar el TextInputLayout de Versión
                    View trimInputLayout = (View) spinnerEngine.getParent().getParent();
                    trimInputLayout.setVisibility(View.VISIBLE);
                    String year = spinnerYear.getSelectedItem().toString();
                    String make = spinnerBrand.getSelectedItem().toString();
                    String model = modelNames.get(position - 1);
                    loadTrims(year, make, model);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }


    private void loadYears() {
        showLoading(spinnerYear);
        api.getYears("getYears").enqueue(new Callback<YearsResponse>() {
            @Override
            public void onResponse(Call<YearsResponse> call, Response<YearsResponse> response) {
                if(response.isSuccessful() && response.body() != null) {
                    YearsResponse years = response.body();
                    List<String> yearList = new ArrayList<>();
                    yearList.add("Selecciona año");
                    for(int i = Integer.parseInt(years.Years.max_year); i >= Integer.parseInt(years.Years.min_year); i--) {
                        yearList.add(String.valueOf(i));
                    }
                    updateSpinner(spinnerYear, yearList);
                }
            }
            @Override
            public void onFailure(Call<YearsResponse> call, Throwable t) {
                showError("Error cargando años");
            }
        });
    }

    private void loadMakes(String year) {
        showLoading(spinnerBrand);
        api.getMakes("getMakes", year, 1).enqueue(new Callback<MakesResponse>() {
            @Override
            public void onResponse(Call<MakesResponse> call, Response<MakesResponse> response) {
                if(response.isSuccessful() && response.body() != null) {
                    makeIds.clear();
                    List<String> makes = new ArrayList<>();
                    makes.add("Selecciona marca");

                    for(MakesResponse.Make make : response.body().Makes) {
                        makeIds.add(make.make_id);
                        makes.add(make.make_display);
                    }
                    updateSpinner(spinnerBrand, makes);
                }
            }
            @Override
            public void onFailure(Call<MakesResponse> call, Throwable t) {
                showError("Error cargando marcas");
            }
        });
    }

    private void loadModels(String year, String makeId) {
        showLoading(spinnerModel);
        api.getModels("getModels", makeId, year).enqueue(new Callback<ModelsResponse>() {
            @Override
            public void onResponse(Call<ModelsResponse> call, Response<ModelsResponse> response) {
                if(response.isSuccessful() && response.body() != null) {
                    modelNames.clear();
                    List<String> models = new ArrayList<>();
                    models.add("Selecciona modelo");

                    for(ModelsResponse.Model model : response.body().Models) {
                        modelNames.add(model.model_name);
                        models.add(model.model_name);
                    }
                    updateSpinner(spinnerModel, models);
                }
            }
            @Override
            public void onFailure(Call<ModelsResponse> call, Throwable t) {
                showError("Error cargando modelos");
            }
        });
    }

    private void loadTrims(String year, String make, String model) {
        showLoading(spinnerEngine);
        Map<String, String> options = new HashMap<>();
        options.put("year", year);
        options.put("make", make);
        options.put("model", model);

        api.getTrims("getTrims", options).enqueue(new Callback<TrimsResponse>() {
            @Override
            public void onResponse(Call<TrimsResponse> call, Response<TrimsResponse> response) {
                if(response.isSuccessful() && response.body() != null) {
                    List<String> trims = new ArrayList<>();
                    trims.add("Selecciona versión");

                    for(TrimsResponse.Trim trim : response.body().Trims) {
                        trims.add(trim.model_trim);
                    }
                    updateSpinner(spinnerEngine, trims);
                }
            }
            @Override
            public void onFailure(Call<TrimsResponse> call, Throwable t) {
                showError("Error cargando versiones");
            }
        });
    }

    private void updateSpinner(Spinner spinner, List<String> data) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                data
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showLoading(Spinner spinner) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                Collections.singletonList("Cargando...")
        );
        spinner.setAdapter(adapter);
    }

    private void saveVehicle() {
        // Validación mejorada
        if (spinnerYear.getSelectedItemPosition() == 0 ||
                spinnerBrand.getSelectedItemPosition() == 0 ||
                spinnerModel.getSelectedItemPosition() == 0 ||
                spinnerEngine.getSelectedItemPosition() == 0) {

            Toast.makeText(this, "Selecciona todas las opciones", Toast.LENGTH_SHORT).show();
            return;
        }

        String year = spinnerYear.getSelectedItem().toString();
        String make = spinnerBrand.getSelectedItem().toString();
        String model = spinnerModel.getSelectedItem().toString();
        String trim = spinnerEngine.getSelectedItem().toString();
        String plate = etPlate.getText().toString().trim().toUpperCase();

        if (plate.isEmpty()) {
            plateContainer.setError("Ingresa la matrícula");
            etPlate.requestFocus();
            return;
        }
        if (!plate.matches("^[0-9]{4}[A-Z]{3}$")) {
            plateContainer.setError("Formato de matrícula inválido (ej: 1234ABC)");
            etPlate.requestFocus();
            return;
        } else {
            plateContainer.setError(null);
        }
        if (vehicleId == null) {
            vehicleId = ref.push().getKey();
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // --- Comprobar si la matrícula ya existe para este usuario ---
        ref.orderByChild("userId").equalTo(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean exists = false;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Vehicle v = child.getValue(Vehicle.class);
                    if (v != null && v.getPlate() != null && v.getPlate().equalsIgnoreCase(plate)) {
                        exists = true;
                        break;
                    }
                }
                if (exists) {
                    plateContainer.setError("Esta matrícula ya está registrada");
                    etPlate.requestFocus();
                } else {
                    plateContainer.setError(null);
                    Vehicle vehicle = new Vehicle(vehicleId, make, model, year, trim, uid, plate);
                    ref.child(vehicleId).setValue(vehicle)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(AddVehicleActivity.this, "Vehículo guardado", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(AddVehicleActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AddVehicleActivity.this, "Error comprobando matrícula", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Nuevo método para decodificar VIN
    private void decodeVin(String vin) {
        Map<String, String> options = new HashMap<>();
        options.put("cmd", "getTrims");
        options.put("vin", vin);

        api.getTrims("getTrims", options).enqueue(new Callback<TrimsResponse>() {
            @Override
            public void onResponse(Call<TrimsResponse> call, Response<TrimsResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().Trims.isEmpty()) {
                    TrimsResponse.Trim trim = response.body().Trims.get(0);

                    // Actualizar spinners con los datos del VIN
                    spinnerYear.setSelection(getIndex(spinnerYear, trim.model_year));
                    spinnerBrand.setSelection(getIndex(spinnerBrand, trim.make_display));
                    spinnerModel.setSelection(getIndex(spinnerModel, trim.model_name));
                    spinnerEngine.setSelection(getIndex(spinnerEngine, trim.model_trim));
                }
            }

            @Override
            public void onFailure(Call<TrimsResponse> call, Throwable t) {
                Log.e("API", "Error VIN: " + t.getMessage());
                Toast.makeText(AddVehicleActivity.this, "Error decodificando VIN", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getIndex(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                return i;
            }
        }
        return 0;
    }
}