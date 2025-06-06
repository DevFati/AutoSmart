package com.example.autosmart.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.autosmart.data.db.UserEntity;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(UserEntity user);

    @Query("SELECT * FROM user LIMIT 1")
    UserEntity getUser();

    @Query("DELETE FROM user")
    void deleteAll();

}
