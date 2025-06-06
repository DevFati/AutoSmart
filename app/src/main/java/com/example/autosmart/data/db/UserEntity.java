package com.example.autosmart.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.example.autosmart.utils.EncryptionUtils;

@Entity(tableName = "user")
public class UserEntity {

    @PrimaryKey
    @NonNull
    private String firebaseUid;

    private String name;    // Almacenará el nombre cifrado
    private String email;   // Almacenará el email cifrado
    private boolean isNameEncrypted = false;
    private boolean isEmailEncrypted = false;

    // Constructor vacío requerido por Room
    public UserEntity() { }

    // Constructor con parámetros
    public UserEntity(@NonNull String firebaseUid, String name, String email) {
        this.firebaseUid = firebaseUid;
        try {
            this.name = EncryptionUtils.encrypt(name);
            this.isNameEncrypted = true;
        } catch (Exception e) {
            this.name = name;
            this.isNameEncrypted = false;
        }
        
        try {
            this.email = EncryptionUtils.encrypt(email);
            this.isEmailEncrypted = true;
        } catch (Exception e) {
            this.email = email;
            this.isEmailEncrypted = false;
        }
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
        if (!isNameEncrypted) {
            return name;
        }
        try {
            return EncryptionUtils.decrypt(name);
        } catch (Exception e) {
            return name;
        }
    }

    public void setName(String name) {
        try {
            this.name = EncryptionUtils.encrypt(name);
            this.isNameEncrypted = true;
        } catch (Exception e) {
            this.name = name;
            this.isNameEncrypted = false;
        }
    }

    public String getEmail() {
        if (!isEmailEncrypted) {
            return email;
        }
        try {
            return EncryptionUtils.decrypt(email);
        } catch (Exception e) {
            return email;
        }
    }

    public void setEmail(String email) {
        try {
            this.email = EncryptionUtils.encrypt(email);
            this.isEmailEncrypted = true;
        } catch (Exception e) {
            this.email = email;
            this.isEmailEncrypted = false;
        }
    }

    // Métodos para verificar si los campos están cifrados
    public boolean isNameEncrypted() {
        return isNameEncrypted;
    }

    public void setNameEncrypted(boolean nameEncrypted) {
        isNameEncrypted = nameEncrypted;
    }

    public boolean isEmailEncrypted() {
        return isEmailEncrypted;
    }

    public void setEmailEncrypted(boolean emailEncrypted) {
        isEmailEncrypted = emailEncrypted;
    }
}
