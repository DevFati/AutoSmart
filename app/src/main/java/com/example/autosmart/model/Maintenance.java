package com.example.autosmart.model;

/**
 * Representa un mantenimiento realizado a un vehículo.
 * Esta clase es usada para el mapeo con Firebase.
 */
public class Maintenance {
    private String id;
    private String userId;
    private String vehicleId;
    private long date;
    private String type;
    private String description;
    private double cost;
    private String vehiclePlate;
    private int kilometraje;
    private boolean isDeleted;

    // Constructor vacío requerido por Firebase
    public Maintenance() {
        this.isDeleted = false;
    }

    // Constructor con parámetros
    public Maintenance(String id, String userId, String vehicleId, String vehiclePlate, 
                      long date, String type, String description, double cost, int kilometraje) {
        this.id = id;
        this.userId = userId;
        this.vehicleId = vehicleId;
        this.vehiclePlate = vehiclePlate;
        this.date = date;
        this.type = type;
        this.description = description;
        this.cost = cost;
        this.kilometraje = kilometraje;
        this.isDeleted = false;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public void setVehiclePlate(String vehiclePlate) {
        this.vehiclePlate = vehiclePlate;
    }

    public int getKilometraje() {
        return kilometraje;
    }

    public void setKilometraje(int kilometraje) {
        this.kilometraje = kilometraje;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }
} 