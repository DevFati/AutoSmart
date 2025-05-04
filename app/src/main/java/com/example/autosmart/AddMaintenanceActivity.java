package com.example.autosmart;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Activity para agregar o editar un mantenimiento.
 */
public class AddMaintenanceActivity extends AppCompatActivity {
    public static final String EXTRA_MAINT_ID   = "maintenance_id";

    private Spinner spinnerVehicles;
    private TextInputEditText etDate, etType, etDesc, etCost, etPlate;
    private MaterialButton btnSave;

    private MaintenanceDao dao;
    private long editingId = -1;

    private final List<String> vehicleLabels = new ArrayList<>();
    private final List<String> vehicleIds    = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    private String pendingVehicleId = null;
    private String pendingPlate = null;

    @Override
    protected void onCreate(@Nullable Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_add_maintenance);

        // 1) Encuentra todas las vistas (IDs deben coincidir con tu XML)
        spinnerVehicles = findViewById(R.id.spinnerVehicle);
        etDate          = findViewById(R.id.etDate);
        etType          = findViewById(R.id.etType);
        etDesc          = findViewById(R.id.etDesc);
        etCost          = findViewById(R.id.etCost);
        etPlate         = findViewById(R.id.etPlate);
        btnSave         = findViewById(R.id.btnSave);

        // 2) Inicia el DAO de Room
        dao = AppDatabase.getInstance(this).maintenanceDao();

        // 3) Prepara el spinner de vehículos
        vehicleLabels.add("Selecciona vehículo");
        vehicleIds  .add(null);
        spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                vehicleLabels
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicles.setAdapter(spinnerAdapter);

        loadMyVehiclesIntoSpinner();

        // 4) DatePicker (no permite fechas pasadas)
        etDate.setFocusable(false);
        etDate.setClickable(true);
        etDate.setOnClickListener(v -> {
            Calendar hoy = Calendar.getInstance();
            DatePickerDialog picker = new DatePickerDialog(
                    this,
                    (view, year, month, day) -> {
                        etDate.setText(String.format("%04d-%02d-%02d",
                                year, month+1, day));
                    },
                    hoy.get(Calendar.YEAR),
                    hoy.get(Calendar.MONTH),
                    hoy.get(Calendar.DAY_OF_MONTH)
            );
            picker.getDatePicker().setMinDate(hoy.getTimeInMillis());
            picker.show();
        });

        // 5) Si viene EXTRA_MAINT_ID, cargo para editar
        if (getIntent().hasExtra(EXTRA_MAINT_ID)) {
            editingId = getIntent().getLongExtra(EXTRA_MAINT_ID, -1);
            if (editingId >= 0) {
                MaintenanceEntity exist = dao.findById(editingId);
                if (exist != null) {
                    etDate.setText(exist.date);
                    etType.setText(exist.type);
                    etDesc.setText(exist.description);
                    etCost.setText(String.valueOf(exist.cost));
                    pendingVehicleId = exist.vehicleId;
                    pendingPlate = exist.vehiclePlate;
                }
            }
        }

        spinnerAdapter.notifyDataSetChanged();

        // Selección del vehículo y matrícula después de cargar los vehículos
        if (pendingVehicleId != null) {
            int idx = vehicleIds.indexOf(pendingVehicleId);
            if (idx >= 0) {
                spinnerVehicles.setSelection(idx);
                // El listener del spinner rellenará la matrícula automáticamente
            } else {
                etPlate.setText(pendingPlate != null ? pendingPlate : "");
            }
            pendingVehicleId = null;
            pendingPlate = null;
        }

        // 6) Botón Guardar
        btnSave.setOnClickListener(v -> saveMaintenance());
    }

    /** Carga desde Firebase solo los vehículos de este usuario para el spinner */
    private void loadMyVehiclesIntoSpinner() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase
                .getInstance("https://autosmart-6e3c3-default-rtdb.firebaseio.com")
                .getReference("vehicles");

        ref.orderByChild("userId").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        vehicleLabels.clear();
                        vehicleIds  .clear();
                        vehicleLabels.add("Selecciona vehículo");
                        vehicleIds  .add(null);

                        for (DataSnapshot ds : snap.getChildren()) {
                            Vehicle v = ds.getValue(Vehicle.class);
                            if (v != null) {
                                vehicleLabels.add(
                                        v.getBrand() + " " + v.getModel() + " (" + v.getYear() + ")"
                                );
                                vehicleIds.add(v.getId());
                            }
                        }
                        spinnerAdapter.notifyDataSetChanged();

                        // Selección del vehículo y matrícula después de cargar los vehículos
                        if (pendingVehicleId != null) {
                            int idx = vehicleIds.indexOf(pendingVehicleId);
                            if (idx >= 0) {
                                spinnerVehicles.setSelection(idx);
                                // El listener del spinner rellenará la matrícula automáticamente
                            } else {
                                etPlate.setText(pendingPlate != null ? pendingPlate : "");
                            }
                            pendingVehicleId = null;
                            pendingPlate = null;
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError err) {
                        Toast.makeText(AddMaintenanceActivity.this,
                                "Error cargando vehículos: " + err.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        // Agregar listener al spinner para actualizar la matrícula
        spinnerVehicles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Si se selecciona un vehículo (no el placeholder)
                    String vehId = vehicleIds.get(position);
                    // Obtener la matrícula del vehículo seleccionado
                    ref.child(vehId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Vehicle v = snapshot.getValue(Vehicle.class);
                            if (v != null) {
                                etPlate.setText(v.getPlate());
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(AddMaintenanceActivity.this,
                                    "Error obteniendo matrícula: " + error.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    etPlate.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                etPlate.setText("");
            }
        });
    }

    /** Valida y guarda o actualiza el registro de mantenimiento */
    private void saveMaintenance() {
        // Spinner
        int pos = spinnerVehicles.getSelectedItemPosition();
        String vehId = vehicleIds.get(pos);
        if (vehId == null) {
            Toast.makeText(this, "Debes seleccionar un vehículo", Toast.LENGTH_SHORT).show();
            return;
        }

        // Resto de campos
        String date = etDate.getText().toString().trim();
        String type = etType.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();
        if (date.isEmpty() || type.isEmpty()) {
            Toast.makeText(this, "Rellena fecha y tipo", Toast.LENGTH_SHORT).show();
            return;
        }

        double cost;
        try {
            cost = Double.parseDouble(etCost.getText().toString().trim());
        } catch (NumberFormatException ex) {
            etCost.setError("Inválido");
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String plate = etPlate.getText().toString().trim();
        android.util.Log.d("AddMaintenance", "Guardando mantenimiento: userId=" + userId + ", vehId=" + vehId + ", plate=" + plate);

        if (editingId < 0) {
            // Nuevo
            MaintenanceEntity m = new MaintenanceEntity(
                    userId, vehId, plate, date, type, desc, cost
            );
            long id = dao.insert(m);
            android.util.Log.d("AddMaintenance", "Nuevo mantenimiento guardado con ID: " + id);
            Toast.makeText(this, "✅ Mantenimiento guardado (ID: " + id + ")", Toast.LENGTH_LONG).show();
        } else {
            // Actualizar
            MaintenanceEntity exist = dao.findById(editingId);
            exist.vehicleId  = vehId;
            exist.vehiclePlate = plate;
            exist.date       = date;
            exist.type       = type;
            exist.description= desc;
            exist.cost       = cost;
            dao.update(exist);
            android.util.Log.d("AddMaintenance", "Mantenimiento actualizado ID: " + editingId);
            Toast.makeText(this, "✅ Mantenimiento actualizado", Toast.LENGTH_SHORT).show();
        }

        setResult(Activity.RESULT_OK);
        finish();
    }
}
