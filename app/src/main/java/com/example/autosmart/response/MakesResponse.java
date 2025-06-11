package com.example.autosmart.response;

import java.util.List;

/**
 * Representa la respuesta de la API para las marcas de vehículos.
 */
public class MakesResponse {
    /** Lista de marcas recibidas en la respuesta. */
    public List<Make> Makes;

    /**
     * Marca de vehículo recibida en la respuesta.
     */
    public static class Make {
        /** ID de la marca. */
        public String make_id;
        /** Nombre de la marca. */
        public String make_display;
    }
}
