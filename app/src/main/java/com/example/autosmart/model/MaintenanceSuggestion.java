package com.example.autosmart.model;

/**
 * Representa una sugerencia de mantenimiento para un vehículo.
 */
public class MaintenanceSuggestion {
    /** Tarea de mantenimiento sugerida (ej: "Cambio de aceite"). */
    public String task;       // “Oil change”, “Tune up”, etc.
    /** Frecuencia recomendada para la tarea (ej: "10,000 km"). */
    public String frequency;  // e.g. “10,000 miles”
} 