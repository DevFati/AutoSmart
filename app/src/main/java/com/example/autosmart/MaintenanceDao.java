package com.example.autosmart;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;
import java.util.Map;

@Dao
public interface MaintenanceDao {
    @Query("SELECT * FROM maintenance WHERE userId = :userId ORDER BY date DESC")
    LiveData<List<MaintenanceEntity>> loadAll(String userId);

    @Query("SELECT * FROM maintenance WHERE userId = :userId AND vehicleId = :vehId ORDER BY date DESC")
    LiveData<List<MaintenanceEntity>> loadForVehicle(String userId, String vehId);

    @Insert long insert(MaintenanceEntity m);
    @Update void update(MaintenanceEntity m);
    @Delete void delete(MaintenanceEntity m);

    @Query("SELECT * FROM maintenance WHERE id = :id LIMIT 1")
    MaintenanceEntity findById(long id);

    @Query("DELETE FROM maintenance WHERE vehicleId = :vehId")
    void deleteForVehicle(String vehId);

    // Método de depuración
    @Query("SELECT COUNT(*) FROM maintenance WHERE userId = :userId")
    int countMaintenanceForUser(String userId);
}





