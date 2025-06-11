package com.example.autosmart.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.autosmart.data.db.UserEntity;

/**
 * DAO para acceder a los datos de usuarios en la base de datos local.
 */
@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(UserEntity user);

    @Query("SELECT * FROM users LIMIT 1")
    UserEntity getUser();

    @Query("DELETE FROM users")
    void deleteAll();

}
