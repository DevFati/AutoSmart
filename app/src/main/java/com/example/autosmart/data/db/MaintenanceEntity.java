package com.example.autosmart.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

    @Entity(tableName = "maintenance")
    public class MaintenanceEntity {
        @PrimaryKey(autoGenerate = true)
        public long id;

        public String userId;      // <<< Nuevo
        public String vehicleId;     // FK al vehículo
        public String date;          // p.ej. "2025-04-18"
        public String type;          // "Revisión", "Cambio aceite", etc.
        public String description;   // detalles
        public double cost;          // coste
        public String vehiclePlate;  // Nueva propiedad
        public int kilometraje;     // Nuevo campo
        public boolean isDeleted = false; // Nuevo campo para historial permanente

        // Constructor
        public MaintenanceEntity(String userId, String vehicleId, String vehiclePlate, String date, String type,
                                 String description, double cost, int kilometraje) {
            this.userId      = userId;
            this.vehicleId = vehicleId;
            this.date = date;
            this.type = type;
            this.description = description;
            this.cost = cost;
            this.vehiclePlate = vehiclePlate;
            this.kilometraje = kilometraje;
            this.isDeleted = false;
        }
    }


