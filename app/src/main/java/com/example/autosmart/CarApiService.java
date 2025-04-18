package com.example.autosmart;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface CarApiService {

    // Obtener años disponibles (rango)
    @GET(".")
    Call<YearsResponse> getYears(@Query("cmd") String cmd);

    // Obtener marcas por año
    @GET(".")
    Call<MakesResponse> getMakes(
            @Query("cmd") String cmd,
            @Query("year") String year,
            @Query("sold_in_us") int soldInUs // Opcional: 1 para USA
    );

    // Obtener modelos por marca y año
    @GET(".")
    Call<ModelsResponse> getModels(
            @Query("cmd") String cmd,
            @Query("make") String makeId,
            @Query("year") String year
    );

    // Obtener versiones (trims) con múltiples filtros
    @GET(".")
    Call<TrimsResponse> getTrims(
            @Query("cmd") String cmd,
            @QueryMap Map<String, String> options
    );
}
