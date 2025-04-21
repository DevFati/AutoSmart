package com.example.autosmart;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
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
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AddMaintenanceActivity extends AppCompatActivity {
    public static final String EXTRA_MAINT_ID   = "maintenance_id";
    public static final String EXTRA_VEHICLE_ID = "vehicle_id";

    private Spinner spinnerVehicles;
    private TextInputEditText etDate, etType, etDesc, etCost;
    private MaterialButton btnSave;

    private MaintenanceDao dao;

    // Para distinguir insertar vs editar
    private long editingId = -1;
    private String originalUserId;

    // Listas para el spinner de vehículos
    private List<String> vehicleLabels = new ArrayList<>();
    private List<String> vehicleIds    = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

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

        // 2) Prepara spinner con valor inicial
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

        // 3) Inicializa DAO
        dao = AppDatabase
                .getInstance(this)
                .maintenanceDao();

        // 4) Carga vehículos del usuario
        loadMyVehiclesIntoSpinner();

        // 5) DatePicker en campo fecha (no fechas pasadas)
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

        // 6) Comprueba si viene EXTRA_MAINT_ID para editar
        if (getIntent().hasExtra(EXTRA_MAINT_ID)) {
            editingId = getIntent().getLongExtra(EXTRA_MAINT_ID, -1);
            if (editingId >= 0) {
                // Carga datos existentes
                MaintenanceEntity exist = dao.findById(editingId);
                if (exist != null) {
                    originalUserId = exist.userId;
                    etDate.setText(exist.date);
                    etType.setText(exist.type);
                    etDesc.setText(exist.description);
                    etCost.setText(String.valueOf(exist.cost));
                    // Posponer selección del spinner hasta que se carguen vehículos:
                    spinnerVehicles.post(() -> {
                        int idx = vehicleIds.indexOf(exist.vehicleId);
                        if (idx >= 0) spinnerVehicles.setSelection(idx);
                    });
                }
            }
        }

        // 7) Guardar / actualizar
        btnSave.setOnClickListener(v -> saveMaintenance());
    }

    /** Recupera de Firebase los vehículos de este usuario */
    private void loadMyVehiclesIntoSpinner() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
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
                                vehicleLabels.add(
                                        v.getBrand() + " " + v.getModel() + " (" + v.getYear() + ")"
                                );
                                vehicleIds.add(v.getId());
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

    /** Inserta o actualiza el mantenimiento */
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
            // NUEVO
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            MaintenanceEntity m = new MaintenanceEntity(
                    uid,      // userId
                    vehId,
                    date,
                    type,
                    desc,
                    cost
            );
            dao.insert(m);
        } else {
            // ACTUALIZAR
            MaintenanceEntity exist = dao.findById(editingId);
            if (exist != null) {
                exist.vehicleId   = vehId;
                exist.date        = date;
                exist.type        = type;
                exist.description = desc;
                exist.cost        = cost;
                // userId queda en exist.userId (o conserva originalUserId)
                dao.update(exist);
            }
        }

        setResult(Activity.RESULT_OK);
        finish();
    }
}
