package com.example.autosmart;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.Manifest;
import android.content.pm.PackageManager;

/**
 * Activity para agregar o editar un mantenimiento.
 */
public class AddMaintenanceActivity extends AppCompatActivity {
    public static final String EXTRA_MAINT_ID   = "maintenance_id";

    private MaterialAutoCompleteTextView spinnerVehicles;
    private TextInputEditText etDate, etTime, etType, etDesc, etCost, etPlate;
    private MaterialButton btnSave;

    private MaintenanceDao dao;
    private long editingId = -1;

    private final List<String> vehicleLabels = new ArrayList<>();
    private final List<String> vehicleIds    = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    private String pendingVehicleId = null;
    private String pendingPlate = null;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(@Nullable Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_add_maintenance);

        // 1) Encuentra todas las vistas (IDs deben coincidir con tu XML)
        spinnerVehicles = findViewById(R.id.spinnerVehicle);
        etDate          = findViewById(R.id.etDate);
        etTime          = findViewById(R.id.etTime);
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
                android.R.layout.simple_dropdown_item_1line,
                vehicleLabels
        );
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
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, day);
                        
                        // Si la fecha seleccionada es hoy, verificar la hora
                        if (selectedDate.get(Calendar.YEAR) == hoy.get(Calendar.YEAR) &&
                            selectedDate.get(Calendar.MONTH) == hoy.get(Calendar.MONTH) &&
                            selectedDate.get(Calendar.DAY_OF_MONTH) == hoy.get(Calendar.DAY_OF_MONTH)) {
                            // Si es hoy, la hora mínima debe ser la actual
                            String currentTime = etTime.getText().toString();
                            if (!currentTime.isEmpty()) {
                                String[] timeParts = currentTime.split(":");
                                int currentHour = Integer.parseInt(timeParts[0]);
                                int currentMinute = Integer.parseInt(timeParts[1]);
                                if (currentHour < hoy.get(Calendar.HOUR_OF_DAY) ||
                                    (currentHour == hoy.get(Calendar.HOUR_OF_DAY) && 
                                     currentMinute < hoy.get(Calendar.MINUTE))) {
                                    etTime.setText(String.format("%02d:%02d", 
                                        hoy.get(Calendar.HOUR_OF_DAY), 
                                        hoy.get(Calendar.MINUTE)));
                                }
                            }
                        }
                        
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

        // 5) TimePicker
        etTime.setFocusable(false);
        etTime.setClickable(true);
        etTime.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            int hour = now.get(Calendar.HOUR_OF_DAY);
            int minute = now.get(Calendar.MINUTE);
            
            // Verificar si la fecha seleccionada es hoy
            String selectedDate = etDate.getText().toString();
            if (!selectedDate.isEmpty()) {
                String[] dateParts = selectedDate.split("-");
                Calendar selectedCal = Calendar.getInstance();
                selectedCal.set(
                    Integer.parseInt(dateParts[0]),
                    Integer.parseInt(dateParts[1]) - 1,
                    Integer.parseInt(dateParts[2])
                );
                
                Calendar today = Calendar.getInstance();
                if (selectedCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    selectedCal.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    selectedCal.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)) {
                    // Si es hoy, usar la hora actual como mínima
                    hour = today.get(Calendar.HOUR_OF_DAY);
                    minute = today.get(Calendar.MINUTE);
                }
            }
            
            new android.app.TimePickerDialog(this, (view, h, m) -> {
                etTime.setText(String.format("%02d:%02d", h, m));
            }, hour, minute, true).show();
        });

        // 6) Si viene EXTRA_MAINT_ID, cargo para editar
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
                    // Si la hora está en la fecha ("2025-05-01 14:30"), sepárala
                    if (exist.date != null && exist.date.contains(" ")) {
                        String[] parts = exist.date.split(" ");
                        etDate.setText(parts[0]);
                        etTime.setText(parts[1]);
                    }
                }
            }
        }

        spinnerAdapter.notifyDataSetChanged();

        // Selección del vehículo y matrícula después de cargar los vehículos
        if (pendingVehicleId != null) {
            int idx = vehicleIds.indexOf(pendingVehicleId);
            if (idx >= 0) {
                spinnerVehicles.setText(vehicleLabels.get(idx), false);
                // El listener del spinner rellenará la matrícula automáticamente
            } else {
                etPlate.setText(pendingPlate != null ? pendingPlate : "");
            }
            pendingVehicleId = null;
            pendingPlate = null;
        }

        // 7) Botón Guardar
        btnSave.setOnClickListener(v -> saveMaintenance());

        // Agregar listener para actualizar la matrícula
        spinnerVehicles.setOnItemClickListener((parent, view, position, id) -> {
            if (position > 0) { // Si se selecciona un vehículo (no el placeholder)
                String vehId = vehicleIds.get(position);
                // Obtener la matrícula del vehículo seleccionado
                DatabaseReference ref = FirebaseDatabase
                        .getInstance("https://autosmart-6e3c3-default-rtdb.firebaseio.com")
                        .getReference("vehicles");
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
        });
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
                                spinnerVehicles.setText(vehicleLabels.get(idx), false);
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
    }

    /** Valida y guarda o actualiza el registro de mantenimiento */
    private void saveMaintenance() {
        // Obtener el vehículo seleccionado
        String selectedText = spinnerVehicles.getText().toString();
        int pos = vehicleLabels.indexOf(selectedText);
        if (pos <= 0) { // Si no hay selección o es el placeholder
            showErrorSnackbar("Debes seleccionar un vehículo");
            return;
        }
        String vehId = vehicleIds.get(pos);

        // Resto de campos
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        
        // Validar fecha y hora
        if (!isValidDateTime(date, time)) {
            return;
        }
        
        String dateTime = date + (time.isEmpty() ? "" : (" " + time));
        String type = etType.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();
        if (date.isEmpty() || type.isEmpty()) {
            showErrorSnackbar("Rellena fecha y tipo");
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
                    userId, vehId, plate, dateTime, type, desc, cost
            );
            long id = dao.insert(m);
            android.util.Log.d("AddMaintenance", "Nuevo mantenimiento guardado con ID: " + id);
            programarNotificacionMantenimiento(date, time, type, vehId, plate);
        } else {
            // Actualizar
            MaintenanceEntity exist = dao.findById(editingId);
            exist.vehicleId  = vehId;
            exist.vehiclePlate = plate;
            exist.date       = dateTime;
            exist.type       = type;
            exist.description= desc;
            exist.cost       = cost;
            dao.update(exist);
            android.util.Log.d("AddMaintenance", "Mantenimiento actualizado ID: " + editingId);
            programarNotificacionMantenimiento(date, time, type, vehId, plate);
        }

        setResult(Activity.RESULT_OK);
        finish();
    }

    private boolean isValidDateTime(String date, String time) {
        if (date.isEmpty()) {
            showErrorSnackbar("Debes seleccionar una fecha");
            return false;
        }

        Calendar now = Calendar.getInstance();
        Calendar selectedDate = Calendar.getInstance();
        
        try {
            String[] dateParts = date.split("-");
            selectedDate.set(
                Integer.parseInt(dateParts[0]),
                Integer.parseInt(dateParts[1]) - 1,
                Integer.parseInt(dateParts[2])
            );

            // Si la fecha es hoy, verificar la hora
            if (selectedDate.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                selectedDate.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                selectedDate.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)) {
                
                if (time.isEmpty()) {
                    showErrorSnackbar("Para hoy debes especificar una hora");
                    return false;
                }

                String[] timeParts = time.split(":");
                int hour = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);

                if (hour < now.get(Calendar.HOUR_OF_DAY) ||
                    (hour == now.get(Calendar.HOUR_OF_DAY) && 
                     minute <= now.get(Calendar.MINUTE))) {
                    showErrorSnackbar("La hora debe ser posterior a la hora actual");
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            showErrorSnackbar("Formato de fecha u hora inválido");
            return false;
        }
    }

    private void showErrorSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        );
        
        // Personalizar el estilo del Snackbar
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(getResources().getColor(R.color.red_500));
        
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(getResources().getColor(android.R.color.white));
        textView.setTextSize(16);
        
        // Añadir icono de error
        Drawable errorIcon = getResources().getDrawable(android.R.drawable.ic_dialog_alert);
        errorIcon.setTint(getResources().getColor(android.R.color.white));
        textView.setCompoundDrawablesWithIntrinsicBounds(errorIcon, null, null, null);
        textView.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.snackbar_icon_padding));
        
        snackbar.show();
    }

    // Método stub para programar notificación
    private void programarNotificacionMantenimiento(String fecha, String hora, String tipo, String vehId, String plate) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        if (!prefs.getBoolean("notif_maint", false)) return;

        try {
            // Verificar permisos de notificación
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, 
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1003);
                    return;
                }
            }

            String dateTimeStr = fecha + (hora.isEmpty() ? " 09:00" : (" " + hora));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date date = sdf.parse(dateTimeStr);
            if (date == null) return;

            long triggerAt = date.getTime();
            
            // Crear un ID único para la notificación
            int notificationId = (int) (triggerAt / 1000);
            
            // Crear el intent con flags actualizados
            Intent intent = new Intent(this, MaintenanceReminderReceiver.class);
            intent.putExtra("type", tipo);
            intent.putExtra("plate", plate);
            intent.putExtra("vehId", vehId);
            intent.putExtra("notificationId", notificationId);
            
            PendingIntent pi = PendingIntent.getBroadcast(
                this, 
                notificationId, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                // Usar setAlarmClock para mayor precisión
                AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(triggerAt, pi);
                am.setAlarmClock(alarmClockInfo, pi);
                
                // Log para debugging
                android.util.Log.d("Maintenance", "Notificación programada para: " + dateTimeStr + 
                    " (ID: " + notificationId + ")");
            }
        } catch (Exception e) {
            android.util.Log.e("Maintenance", "Error programando notificación: " + e.getMessage());
        }
    }
}
