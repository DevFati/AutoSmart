package com.example.autosmart.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.example.autosmart.utils.EncryptionUtils;

/**
 * Entidad que representa un usuario en la base de datos local (Room).
 * Almacena información cifrada del usuario.
 */
@Entity(tableName = "users")
public class UserEntity {

    /** Identificador único de Firebase para el usuario. */
    @PrimaryKey
    @NonNull
    private String firebaseUid;

    /** Nombre cifrado del usuario. */
    private String name;    // Almacenará el nombre cifrado
    /** Email cifrado del usuario. */
    private String email;   // Almacenará el email cifrado
    /** Indica si el nombre está cifrado. */
    private boolean isNameEncrypted = false;
    /** Indica si el email está cifrado. */
    private boolean isEmailEncrypted = false;

    private String photoUrl;
    private long lastSync;

    /**
     * Constructor vacío requerido por Room.
     */
    public UserEntity() { }

    /**
     * Constructor con parámetros.
     * @param firebaseUid UID de Firebase.
     * @param name Nombre del usuario.
     * @param email Email del usuario.
     */
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

    /**
     * Obtiene el UID de Firebase.
     * @return UID de Firebase.
     */
    @NonNull
    public String getFirebaseUid() {
        return firebaseUid;
    }

    /**
     * Establece el UID de Firebase.
     * @param firebaseUid Nuevo UID.
     */
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

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public long getLastSync() { return lastSync; }
    public void setLastSync(long lastSync) { this.lastSync = lastSync; }
}
