package com.example.autosmart.network;

import com.example.autosmart.response.MakesResponse;
import com.example.autosmart.response.ModelsResponse;
import com.example.autosmart.response.TrimsResponse;
import com.example.autosmart.response.YearsResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * Interfaz de Retrofit para acceder a los endpoints de la API de vehículos.
 */
public interface CarApiService {

    /**
     * Obtiene los años disponibles.
     * @param cmd Comando de la API.
     * @return Llamada Retrofit con YearsResponse.
     */
    @GET(".")
    Call<YearsResponse> getYears(@Query("cmd") String cmd);

    /**
     * Obtiene las marcas por año.
     * @param cmd Comando de la API.
     * @param year Año.
     * @param soldInUs Indica si es vendido en USA.
     * @return Llamada Retrofit con MakesResponse.
     */
    @GET(".")
    Call<MakesResponse> getMakes(
            @Query("cmd") String cmd,
            @Query("year") String year,
            @Query("sold_in_us") int soldInUs // Opcional: 1 para USA
    );

    /**
     * Obtiene los modelos por marca y año.
     * @param cmd Comando de la API.
     * @param makeId ID de la marca.
     * @param year Año.
     * @return Llamada Retrofit con ModelsResponse.
     */
    @GET(".")
    Call<ModelsResponse> getModels(
            @Query("cmd") String cmd,
            @Query("make") String makeId,
            @Query("year") String year
    );

    /**
     * Obtiene los acabados (trims) con múltiples filtros.
     * @param cmd Comando de la API.
     * @param options Opciones de filtro.
     * @return Llamada Retrofit con TrimsResponse.
     */
    @GET(".")
    Call<TrimsResponse> getTrims(
            @Query("cmd") String cmd,
            @QueryMap Map<String, String> options
    );
} 