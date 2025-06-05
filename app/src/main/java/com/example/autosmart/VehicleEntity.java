package com.example.autosmart;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.example.autosmart.utils.EncryptionUtils;

@Entity(tableName = "vehicles")
public class VehicleEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String brand;
    private String model;
    private String year;
    private String engineType;
    private String userId;
    private String plate;  // Almacenará la matrícula cifrada
    private boolean isPlateEncrypted = false;

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