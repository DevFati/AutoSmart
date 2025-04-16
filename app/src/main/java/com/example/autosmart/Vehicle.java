package com.example.autosmart;

public class Vehicle {
    private String id;
    private String brand;
    private String model;
    private String year;

    // Constructor vacío requerido por Firebase
    public Vehicle() { }

    // Constructor con parámetros
    public Vehicle(String id, String brand, String model, String year) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.year = year;
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
}

