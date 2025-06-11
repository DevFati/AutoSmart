package com.example.autosmart.response;

/**
 * Representa la respuesta de la API para los años disponibles de vehículos.
 */
public class YearsResponse {
    /** Rango de años recibido en la respuesta. */
    public YearRange Years;

    /**
     * Rango de años disponible.
     */
    public static class YearRange {
        /** Año mínimo disponible. */
        public String min_year;
        /** Año máximo disponible. */
        public String max_year;
    }
} 