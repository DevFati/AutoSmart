package com.example.autosmart.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Cliente para acceder a la API externa de vehículos usando Retrofit.
 */
public class ApiClient {
        private static final String BASE_URL = "https://www.carqueryapi.com/api/0.3/";
        private static Retrofit retrofit;

        /**
         * Obtiene la instancia singleton de Retrofit configurada para la API.
         * @return Instancia de Retrofit.
         */
        public static Retrofit getRetrofit() {
            if (retrofit == null) {
                retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
            }
            return retrofit;
        }
    }