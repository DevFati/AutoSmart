package com.example.autosmart;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MaintenanceDao {
    @Query("SELECT * FROM maintenance WHERE vehicleId = :vehId ORDER BY date DESC")
    LiveData<List<MaintenanceEntity>> loadForVehicle(String vehId);

    @Query("SELECT * FROM maintenance ORDER BY date DESC")
    LiveData<List<MaintenanceEntity>> loadAll();

    @Query("SELECT * FROM maintenance WHERE id = :id")
    MaintenanceEntity getById(long id);

    @Query("SELECT * FROM maintenance WHERE id = :id")
    MaintenanceEntity findById(long id);

    @Insert
    long insert(MaintenanceEntity m);

    @Update
    void update(MaintenanceEntity m);

    @Delete
    void delete(MaintenanceEntity m);
}

