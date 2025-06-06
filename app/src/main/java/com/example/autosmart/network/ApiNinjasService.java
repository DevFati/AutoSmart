package com.example.autosmart.network;

import com.example.autosmart.model.MaintenanceSuggestion;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface ApiNinjasService {
    @GET("v1/maintenance")
    Call<List<MaintenanceSuggestion>> getMaintenance(
            @Header("X-Api-Key") String apiKey,
            @Query("make") String make,
            @Query("model") String model,
            @Query("year") int year
    );
}