package com.example.autosmart.model;

import androidx.room.Ignore;
import java.util.List;

/**
 * Representa un vehículo en la aplicación AutoSmart.
 * Versión simplificada para Firebase.
 */
public class Vehicle {
    private String id;
    private String brand;
    private String model;
    private String year;
    private String engineType;
    private String userId;
    private String plate;  // Matrícula sin cifrar para Firebase

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
        this.plate = plate;
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
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }
} 