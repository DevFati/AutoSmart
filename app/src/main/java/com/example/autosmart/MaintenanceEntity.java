package com.example.autosmart;

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

        // Constructor
        public MaintenanceEntity(String userId, String vehicleId, String vehiclePlate, String date, String type,
                                 String description, double cost) {
            this.userId      = userId;
            this.vehicleId = vehicleId;
            this.date = date;
            this.type = type;
            this.description = description;
            this.cost = cost;
            this.vehiclePlate = vehiclePlate;

        }
    }


