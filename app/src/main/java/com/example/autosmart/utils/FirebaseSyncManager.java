package com.example.autosmart.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.autosmart.data.db.AppDatabase;
import com.example.autosmart.data.db.MaintenanceEntity;
import com.example.autosmart.data.db.UserEntity;
import com.example.autosmart.data.db.VehicleEntity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utilidad para sincronizar datos entre Firebase y la base de datos local.
 */
public class FirebaseSyncManager {
    private static final String TAG = "FirebaseSyncManager";
    private final Context context;
    private final AppDatabase db;
    private final ExecutorService executor;
    private final DatabaseReference rootRef;

    public FirebaseSyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.db = AppDatabase.getInstance(this.context);
        this.executor = Executors.newSingleThreadExecutor();
        this.rootRef = FirebaseDatabase.getInstance("https://autosmart-6e3c3-default-rtdb.firebaseio.com")
                .getReference();
    }

    /**
     * Sincroniza todos los datos del usuario desde Firebase a la base de datos local.
     * @param onComplete Callback que se ejecuta cuando la sincronización termina.
     */
    public void syncUserData(Runnable onComplete) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid == null) {
            Log.e(TAG, "No hay usuario autenticado");
            return;
        }

        executor.execute(() -> {
            try {
                // 1. Sincronizar usuario
                syncUser(uid, () -> {
                    // 2. Sincronizar vehículos
                    syncVehicles(uid, () -> {
                        // 3. Sincronizar mantenimientos
                        syncMaintenances(uid, onComplete);
                    });
                });
            } catch (Exception e) {
                Log.e(TAG, "Error en sincronización: " + e.getMessage());
                if (onComplete != null) onComplete.run();
            }
        });
    }

    /**
     * Guarda o actualiza la información del usuario en Firebase.
     * @param userEntity Entidad del usuario a guardar
     * @param onComplete Callback que se ejecuta cuando la operación termina
     */
    public void saveUserToFirebase(UserEntity userEntity, Runnable onComplete) {
        String uid = userEntity.getFirebaseUid();
        if (uid == null) {
            Log.e(TAG, "No hay UID de usuario");
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", userEntity.getName());
        userData.put("email", userEntity.getEmail());
        userData.put("isNameEncrypted", userEntity.isNameEncrypted());
        userData.put("isEmailEncrypted", userEntity.isEmailEncrypted());
        userData.put("lastSync", System.currentTimeMillis());

        // Obtener la URL de la foto de perfil de SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String photoUrl = prefs.getString("profile_photo", null);
        if (photoUrl != null) {
            userData.put("photoUrl", photoUrl);
        }

        rootRef.child("users").child(uid)
                .setValue(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Usuario guardado en Firebase");
                    if (onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error guardando usuario en Firebase: " + e.getMessage());
                    if (onComplete != null) onComplete.run();
                });
    }

    private void syncUser(String uid, Runnable onComplete) {
        rootRef.child("users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        executor.execute(() -> {
                            try {
                                if (snapshot.exists()) {
                                    UserEntity user = snapshot.getValue(UserEntity.class);
                                    if (user != null) {
                                        db.userDao().insertUser(user);
                                        
                                        // Guardar la URL de la foto en SharedPreferences si existe
                                        String photoUrl = snapshot.child("photoUrl").getValue(String.class);
                                        if (photoUrl != null) {
                                            SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                                            prefs.edit().putString("profile_photo", photoUrl).apply();
                                        }
                                        
                                        Log.d(TAG, "Usuario sincronizado");
                                    }
                                }
                                if (onComplete != null) onComplete.run();
                            } catch (Exception e) {
                                Log.e(TAG, "Error sincronizando usuario: " + e.getMessage());
                                if (onComplete != null) onComplete.run();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error en Firebase: " + error.getMessage());
                        if (onComplete != null) onComplete.run();
                    }
                });
    }

    private void syncVehicles(String uid, Runnable onComplete) {
        rootRef.child("vehicles")
                .orderByChild("userId")
                .equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        executor.execute(() -> {
                            try {
                                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                                    // Limpiar vehículos existentes
                                    db.vehicleDao().deleteAllForUser(uid);
                                    // Insertar nuevos vehículos
                                    for (DataSnapshot ds : snapshot.getChildren()) {
                                        VehicleEntity vehicle = ds.getValue(VehicleEntity.class);
                                        if (vehicle != null) {
                                            db.vehicleDao().insert(vehicle);
                                        }
                                    }
                                    Log.d(TAG, "Vehículos sincronizados");
                                } else {
                                    Log.d(TAG, "No hay vehículos en Firebase. No se borra nada local.");
                                }
                                if (onComplete != null) onComplete.run();
                            } catch (Exception e) {
                                Log.e(TAG, "Error sincronizando vehículos: " + e.getMessage());
                                if (onComplete != null) onComplete.run();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error en Firebase: " + error.getMessage());
                        if (onComplete != null) onComplete.run();
                    }
                });
    }

    private void syncMaintenances(String uid, Runnable onComplete) {
        rootRef.child("vehicles")
                .orderByChild("userId")
                .equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        executor.execute(() -> {
                            try {
                                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                                    // Limpiar mantenimientos existentes
                                    for (DataSnapshot vehicleSnapshot : snapshot.getChildren()) {
                                        String vehicleId = vehicleSnapshot.getKey();
                                        if (vehicleId != null) {
                                            db.maintenanceDao().deleteForVehicle(vehicleId);
                                        }
                                    }
                                    // Insertar nuevos mantenimientos
                                    for (DataSnapshot vehicleSnapshot : snapshot.getChildren()) {
                                        DataSnapshot maintenancesSnapshot = vehicleSnapshot.child("maintenances");
                                        for (DataSnapshot maintSnapshot : maintenancesSnapshot.getChildren()) {
                                            MaintenanceEntity maintenance = maintSnapshot.getValue(MaintenanceEntity.class);
                                            if (maintenance != null) {
                                                db.maintenanceDao().insert(maintenance);
                                            }
                                        }
                                    }
                                    Log.d(TAG, "Mantenimientos sincronizados");
                                } else {
                                    Log.d(TAG, "No hay mantenimientos en Firebase. No se borra nada local.");
                                }
                                if (onComplete != null) onComplete.run();
                            } catch (Exception e) {
                                Log.e(TAG, "Error sincronizando mantenimientos: " + e.getMessage());
                                if (onComplete != null) onComplete.run();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error en Firebase: " + error.getMessage());
                        if (onComplete != null) onComplete.run();
                    }
                });
    }

    /**
     * Cierra el executor cuando ya no se necesite.
     */
    public void shutdown() {
        executor.shutdown();
    }
} 