package com.example.autosmart.data.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.autosmart.data.dao.MaintenanceDao;
import com.example.autosmart.data.dao.UserDao;
import com.example.autosmart.data.dao.VehicleDao;

@Database(entities = {UserEntity.class, MaintenanceEntity.class, VehicleEntity.class}, version = 9, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract UserDao userDao();

    public abstract MaintenanceDao maintenanceDao();

    public abstract VehicleDao vehicleDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            android.util.Log.d("AppDatabase", "Inicializando base de datos...");
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "app_database")
                    // Por simplicidad, usamos allowMainThreadQueries() en este ejemplo;
                    // en producción, usa hilos de fondo o corutinas.
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
            android.util.Log.d("AppDatabase", "Base de datos inicializada");
        }
        return instance;
    }
}

