package com.example.autosmart.response;

import java.util.List;

/**
 * Representa la respuesta de la API para los modelos de vehículos.
 */
public class ModelsResponse {
    /** Lista de modelos recibidos en la respuesta. */
    public List<Model> Models;

    /**
     * Modelo de vehículo recibido en la respuesta.
     */
    public static class Model {
        /** Nombre del modelo. */
        public String model_name;
        /** ID de la marca del modelo. */
        public String model_make_id;
    }
}