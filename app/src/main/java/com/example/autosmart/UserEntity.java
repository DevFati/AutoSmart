package com.example.autosmart;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user")
public class UserEntity {

    @PrimaryKey
    @NonNull
    private String firebaseUid; // Usamos el UID de Firebase como ID

    private String name;
    private String email;

    // Constructor vacío requerido por Room
    public UserEntity() { }

    // Constructor con parámetros: utiliza el UID obtenido de Firebase
    public UserEntity(@NonNull String firebaseUid, String name, String email) {
        this.firebaseUid = firebaseUid;
        this.name = name;
        this.email = email;
    }

    // Getters y Setters
    @NonNull
    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(@NonNull String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
