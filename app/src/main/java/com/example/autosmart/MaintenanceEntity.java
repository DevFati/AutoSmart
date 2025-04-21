package com.example.autosmart;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

    @Entity(tableName = "maintenance")
    public class MaintenanceEntity {
        @PrimaryKey(autoGenerate = true)
        public long id;

        public String vehicleId;     // FK al vehículo
        public String date;          // p.ej. "2025-04-18"
        public String type;          // "Revisión", "Cambio aceite", etc.
        public String description;   // detalles
        public double cost;          // coste

        // Constructor
        public MaintenanceEntity(String vehicleId, String date, String type,
                                 String description, double cost) {
            this.vehicleId = vehicleId;
            this.date = date;
            this.type = type;
            this.description = description;
            this.cost = cost;
        }
    }


