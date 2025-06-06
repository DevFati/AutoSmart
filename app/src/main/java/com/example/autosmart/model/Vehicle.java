package com.example.autosmart.model;

import com.example.autosmart.utils.EncryptionUtils;

public class Vehicle {
    private String id;
    private String brand;
    private String model;
    private String year;
    private String engineType;
    private String userId;   // ← nuevo campo
    private String plate;  // Almacenará la matrícula cifrada
    private boolean isPlateEncrypted = false; // Nuevo campo para controlar si la matrícula está cifrada

    // Constructor vacío requerido por Firebase
    public Vehicle() { }

    // Constructor con parámetros
    public Vehicle(String id, String brand, String model, String year, String engineType, String userId, String plate) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.engineType = engineType;
        this.userId = userId;
        try {
            this.plate = EncryptionUtils.encrypt(plate);
            this.isPlateEncrypted = true;
        } catch (Exception e) {
            this.plate = plate;
            this.isPlateEncrypted = false;
        }
    }

    // Getters y Setters
    public String getId() {
        return id;
    }
    public void setId(String id) {
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

    public String getEngineType() { return engineType; }
    public void setEngineType(String engineType) { this.engineType = engineType; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPlate() { 
        if (!isPlateEncrypted) {
            return plate;
        }
        try {
            return EncryptionUtils.decrypt(plate);
        } catch (Exception e) {
            return plate; // En caso de error, devolver sin descifrar
        }
    }
    
    public void setPlate(String plate) { 
        try {
            this.plate = EncryptionUtils.encrypt(plate);
            this.isPlateEncrypted = true;
        } catch (Exception e) {
            this.plate = plate;
            this.isPlateEncrypted = false;
        }
    }

    // Método para verificar si la matrícula está cifrada
    public boolean isPlateEncrypted() {
        return isPlateEncrypted;
    }
} 