package com.example.autosmart;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AddMaintenanceActivity extends AppCompatActivity {
    private Spinner spinnerVehicles;
    private TextInputEditText etDate, etType, etDesc, etCost;
    private MaterialButton btnSave;
    public static final String EXTRA_MAINT_ID   = "maintenance_id";

    private MaintenanceDao dao;

    // Listas paralelas para el spinner
    private List<String> vehicleLabels = new ArrayList<>();
    private List<String> vehicleIds    = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;
    private long editingId = -1;  // -1 = modo “nuevo”

    @Override
    protected void onCreate(@Nullable Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_add_maintenance);

        // 1) Bind de vistas
        spinnerVehicles = findViewById(R.id.spinnerVehicle);
        etDate          = findViewById(R.id.etDate);
        etType          = findViewById(R.id.etType);
        etDesc          = findViewById(R.id.etDesc);
        etCost          = findViewById(R.id.etCost);
        btnSave         = findViewById(R.id.btnSave);

        // 2) Prepara spinner con el elemento por defecto
        vehicleLabels.clear();
        vehicleIds.clear();
        vehicleLabels.add("Selecciona vehículo");
        vehicleIds   .add(null);
        spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                vehicleLabels
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicles.setAdapter(spinnerAdapter);

        // 3) Inicializa DAO de Room
        dao = AppDatabase
                .getInstance(this)
                .maintenanceDao();

        // 4) Carga solo tus vehículos desde Firebase
        loadMyVehiclesIntoSpinner();

        // 5) DatePicker en el campo fecha (no permite fechas pasadas)
        etDate.setFocusable(false);
        etDate.setClickable(true);
        etDate.setOnClickListener(v -> {
            Calendar hoy = Calendar.getInstance();
            int yy = hoy.get(Calendar.YEAR);
            int mm = hoy.get(Calendar.MONTH);
            int dd = hoy.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog picker = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        String mes = String.format("%02d", month + 1);
                        String dia = String.format("%02d", dayOfMonth);
                        etDate.setText(year + "-" + mes + "-" + dia);
                    },
                    yy, mm, dd
            );
            picker.getDatePicker().setMinDate(hoy.getTimeInMillis());
            picker.show();
        });

        // — Si vienen extras, cargamos para edición —
        if (getIntent().hasExtra(EXTRA_MAINT_ID)) {
            editingId = getIntent().getLongExtra(EXTRA_MAINT_ID, -1);
            if (editingId >= 0) {
                // recuperar entidad y rellenar UI
                MaintenanceEntity m = dao.findById(editingId);
                if (m != null) {
                    etDate.setText(m.date);
                    etType.setText(m.type);
                    etDesc.setText(m.description);
                    etCost.setText(String.valueOf(m.cost));
                    // preseleccionar spinnerVehicles tras que terminen de cargarse
                    spinnerVehicles.post(() -> {
                        int idx = vehicleIds.indexOf(m.vehicleId);
                        if (idx >= 0) spinnerVehicles.setSelection(idx);
                    });
                }
            }
        }

        // 6) Guardar mantenimiento
        btnSave.setOnClickListener(v -> saveMaintenance());
    }

    /**
     * Recupera de RealtimeDB los vehículos cuyo campo userId coincide con el actual
     * y los mete en el spinner
     */
    private void loadMyVehiclesIntoSpinner() {
        String uid = FirebaseAuth.getInstance()
                .getCurrentUser().getUid();

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
                        vehicleLabels.add("Selecciona vehículo");
                        vehicleIds   .add(null);

                        for (DataSnapshot ds : snap.getChildren()) {
                            Vehicle v = ds.getValue(Vehicle.class);
                            if (v != null) {
                                vehicleLabels.add(v.getBrand() + " " + v.getModel() + " (" + v.getYear() + ")");
                                vehicleIds   .add(v.getId());
                            }
                        }
                        spinnerAdapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError err) {
                        Toast.makeText(
                                AddMaintenanceActivity.this,
                                "Error cargando vehículos: " + err.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    /**
     * Valida campos y guarda el nuevo MaintenanceEntity en la base local
     */
    private void saveMaintenance() {
        int pos = spinnerVehicles.getSelectedItemPosition();
        String vehId = vehicleIds.get(pos);
        if (vehId == null) {
            Toast.makeText(this, "Debes seleccionar un vehículo", Toast.LENGTH_SHORT).show();
            return;
        }
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
        } catch (NumberFormatException e) {
            etCost.setError("Inválido");
            return;
        }

        if (editingId < 0) {
            // Insert nuevo
            MaintenanceEntity nuevo = new MaintenanceEntity(vehId, date, type, desc, cost);
            dao.insert(nuevo);
        } else {
            // Actualiza existente
            MaintenanceEntity exist = dao.findById(editingId);
            exist.vehicleId  = vehId;
            exist.date       = date;
            exist.type       = type;
            exist.description= desc;
            exist.cost       = cost;
            dao.update(exist);
        }

        setResult(Activity.RESULT_OK);
        finish();

    }
}
