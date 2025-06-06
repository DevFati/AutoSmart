package com.example.autosmart.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.autosmart.data.db.VehicleEntity;

import java.util.List;

@Dao
public interface VehicleDao {
    @Query("SELECT * FROM vehicles WHERE userId = :userId")
    LiveData<List<VehicleEntity>> loadAll(String userId);

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    VehicleEntity findById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(VehicleEntity vehicle);

    @Update
    void update(VehicleEntity vehicle);

    @Delete
    void delete(VehicleEntity vehicle);

    @Query("DELETE FROM vehicles WHERE userId = :userId")
    void deleteAllForUser(String userId);

    @Query("SELECT COUNT(*) FROM vehicles WHERE userId = :userId")
    int countVehiclesForUser(String userId);

    @Query("SELECT COUNT(*) FROM vehicles")
    int getVehicleCount();
} 