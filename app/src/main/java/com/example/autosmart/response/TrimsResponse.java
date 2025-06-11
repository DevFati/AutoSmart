package com.example.autosmart.response;

import java.util.List;

/**
 * Representa la respuesta de la API para los acabados (trims) de vehículos.
 */
public class TrimsResponse {
    /** Lista de acabados recibidos en la respuesta. */
    public List<Trim> Trims;

    /**
     * Acabado (trim) de vehículo recibido en la respuesta.
     */
    public static class Trim {
        /** Año del modelo. */
        public String model_year;
        /** Nombre de la marca. */
        public String make_display;
        /** Nombre del modelo. */
        public String model_name;
        /** Nombre del acabado. */
        public String model_trim;
    }
}