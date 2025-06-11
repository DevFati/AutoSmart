package com.example.autosmart.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

import com.example.autosmart.model.Vehicle;
import com.example.autosmart.utils.EncryptionUtils;

/**
 * Entidad que representa un vehículo en la base de datos local (Room).
 * Almacena información cifrada de la matrícula.
 */
@Entity(tableName = "vehicles")
public class VehicleEntity {
    /** Identificador único del vehículo. */
    @PrimaryKey
    @NonNull
    private String id;
    /** Marca del vehículo. */
    private String brand;
    /** Modelo del vehículo. */
    private String model;
    /** Año del vehículo. */
    private String year;
    /** Tipo de motor del vehículo. */
    private String engineType;
    /** ID del usuario propietario. */
    private String userId;
    /** Matrícula cifrada del vehículo. */
    private String plate;  // Almacenará la matrícula cifrada
    /** Indica si la matrícula está cifrada. */
    private boolean isPlateEncrypted = false;
    @Ignore
    private Object maintenances;

    // Constructor vacío requerido por Room
    public VehicleEntity() { }

    // Constructor con parámetros
    public VehicleEntity(String id, String brand, String model, String year, String engineType, String userId, String plate) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.engineType = engineType;
        this.userId = userId;
        if (isBase64(plate)) {
            this.plate = plate;
            this.isPlateEncrypted = true;
        } else {
            try {
                this.plate = EncryptionUtils.encrypt(plate);
                this.isPlateEncrypted = true;
            } catch (Exception e) {
                this.plate = plate;
                this.isPlateEncrypted = false;
            }
        }
    }

    // Getters y Setters
    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getEngineType() {
        return engineType;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPlate() {
        if (!isPlateEncrypted) {
            return plate;
        }
        try {
            return EncryptionUtils.decrypt(plate);
        } catch (Exception e) {
            return plate;
        }
    }

    public void setPlate(String plate) {
        if (isBase64(plate)) {
            this.plate = plate;
            this.isPlateEncrypted = true;
        } else {
            try {
                this.plate = EncryptionUtils.encrypt(plate);
                this.isPlateEncrypted = true;
            } catch (Exception e) {
                this.plate = plate;
                this.isPlateEncrypted = false;
            }
        }
    }

    public boolean isPlateEncrypted() {
        return isPlateEncrypted;
    }

    public void setPlateEncrypted(boolean plateEncrypted) {
        isPlateEncrypted = plateEncrypted;
    }

    @Ignore
    public Object getMaintenances() { return maintenances; }
    @Ignore
    public void setMaintenances(Object maintenances) { this.maintenances = maintenances; }

    // Método para convertir de Vehicle a VehicleEntity
    public static VehicleEntity fromVehicle(Vehicle vehicle) {
        return new VehicleEntity(
            vehicle.getId(),
            vehicle.getBrand(),
            vehicle.getModel(),
            vehicle.getYear(),
            vehicle.getEngineType(),
            vehicle.getUserId(),
            vehicle.getPlate()
        );
    }

    // Método para convertir a Vehicle
    public Vehicle toVehicle() {
        return new Vehicle(
            this.id,
            this.brand,
            this.model,
            this.year,
            this.engineType,
            this.userId,
            this.getPlate()
        );
    }

    private boolean isBase64(String value) {
        // Un valor cifrado en Base64 suele ser más largo que una matrícula normal y solo contiene ciertos caracteres
        return value != null && value.matches("^[A-Za-z0-9+/=]{16,}$");
    }
} 