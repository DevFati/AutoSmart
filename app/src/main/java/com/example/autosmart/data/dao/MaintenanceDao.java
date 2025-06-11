package com.example.autosmart.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.autosmart.data.db.MaintenanceEntity;

import java.util.List;

/**
 * DAO para acceder a los datos de mantenimientos en la base de datos local.
 */
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

    @Query("SELECT COUNT(*) FROM maintenance WHERE isDeleted = 0")
    int getMaintenanceCount();

    @Query("SELECT * FROM maintenance WHERE date >= :todayTimestamp AND isDeleted = 0 ORDER BY date ASC LIMIT 1")
    MaintenanceEntity getNextMaintenance(long todayTimestamp);

    @Query("SELECT * FROM maintenance WHERE userId = :userId ORDER BY date DESC")
    List<MaintenanceEntity> getAllForUser(String userId);
}





