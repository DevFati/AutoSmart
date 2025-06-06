package com.example.autosmart.ui.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.autosmart.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MapsFragment extends Fragment {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private MapView map;
    private MyLocationNewOverlay myLocationOverlay;
    private IMapController mapController;
    private TextInputEditText searchLocation;
    private View cardStationInfo;
    private TextView tvStationName, tvStationAddress, tvFuels, tvServices, tvSchedule;
    private OkHttpClient client;
    private Marker lastClickedMarker = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getContext(), androidx.preference.PreferenceManager.getDefaultSharedPreferences(getContext()));
        client = new OkHttpClient();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_maps, container, false);

        map = root.findViewById(R.id.map);
        searchLocation = root.findViewById(R.id.searchLocation);
        View include = root.findViewById(R.id.includeStationInfo);
        cardStationInfo = include;
        tvStationName = include.findViewById(R.id.tvStationName);
        tvStationAddress = include.findViewById(R.id.tvStationAddress);
        tvFuels = include.findViewById(R.id.tvFuels);
        tvServices = include.findViewById(R.id.tvServices);
        tvSchedule = include.findViewById(R.id.tvSchedule);

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        mapController = map.getController();
        mapController.setZoom(15.0);

        myLocationOverlay = new MyLocationNewOverlay(map);
        myLocationOverlay.enableMyLocation();
        map.getOverlays().add(myLocationOverlay);

        FloatingActionButton fabMyLocation = root.findViewById(R.id.fabMyLocation);
        fabMyLocation.setOnClickListener(v -> centerOnMyLocation());

        // Configurar la búsqueda
        searchLocation.setOnEditorActionListener((v, actionId, event) -> {
            searchLocation();
            return true;
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Solicitar permisos de ubicación y centrar en la ubicación actual
        if (checkLocationPermission()) {
            centerOnMyLocation();
        } else {
            requestLocationPermission();
        }
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                centerOnMyLocation();
            } else {
                Toast.makeText(requireContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void searchLocation() {
        String location = searchLocation.getText().toString().trim();
        if (location.isEmpty()) {
            Toast.makeText(getContext(), "Introduce una ubicación", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "https://nominatim.openstreetmap.org/search?format=json&q=" + location;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "AutoSmart")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error al buscar ubicación", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) return;

                try {
                    JSONArray results = new JSONArray(response.body().string());
                    if (results.length() > 0) {
                        JSONObject firstResult = results.getJSONObject(0);
                        double lat = firstResult.getDouble("lat");
                        double lon = firstResult.getDouble("lon");

                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            GeoPoint point = new GeoPoint(lat, lon);
                            mapController.setCenter(point);
                            searchNearbyGasStations(point);
                        });
                    } else {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Ubicación no encontrada", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Error procesando respuesta", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void searchNearbyGasStations(GeoPoint center) {
        map.getOverlays().clear();
        map.getOverlays().add(myLocationOverlay);

        String query = String.format(
                "[out:json][timeout:25];" +
                "(" +
                "  node[\"amenity\"=\"fuel\"](around:5000,%f,%f);" +
                "  way[\"amenity\"=\"fuel\"](around:5000,%f,%f);" +
                "  relation[\"amenity\"=\"fuel\"](around:5000,%f,%f);" +
                ");" +
                "out body;" +
                ">;" +
                "out skel qt;",
                center.getLatitude(), center.getLongitude(),
                center.getLatitude(), center.getLongitude(),
                center.getLatitude(), center.getLongitude());

        Request request = new Request.Builder()
                .url("https://overpass-api.de/api/interpreter")
                .post(okhttp3.RequestBody.create(query, okhttp3.MediaType.parse("application/x-www-form-urlencoded")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error al buscar gasolineras", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) return;

                try {
                    JSONObject jsonResponse = new JSONObject(response.body().string());
                    JSONArray elements = jsonResponse.getJSONArray("elements");

                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        for (int i = 0; i < elements.length(); i++) {
                            try {
                                JSONObject element = elements.getJSONObject(i);
                                if (element.has("tags")) {
                                    JSONObject tags = element.getJSONObject("tags");
                                    String name = tags.optString("name", "Gasolinera");
                                    boolean hasElectric = tags.optString("fuel:electric", "no").equals("yes");

                                    double lat = element.optDouble("lat", 0);
                                    double lon = element.optDouble("lon", 0);
                                    if (lat != 0 && lon != 0) {
                                        GeoPoint point = new GeoPoint(lat, lon);
                                        Marker marker = new Marker(map);
                                        marker.setPosition(point);
                                        marker.setTitle(name);
                                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                        marker.setIcon(getResources().getDrawable(
                                                hasElectric ? R.drawable.ic_location_pin_electric : R.drawable.ic_location_pin));

                                        // Construir información detallada con mejor formato
                                        StringBuilder info = new StringBuilder();
                                        info.append(name).append("\n\n");

                                        // Combustibles disponibles
                                        info.append("🚗 Combustibles\n");
                                        boolean hasFuel = false;
                                        if (tags.optString("fuel:octane_95", "no").equals("yes")) {
                                            info.append("  • Gasolina 95\n");
                                            hasFuel = true;
                                        }
                                        if (tags.optString("fuel:octane_98", "no").equals("yes")) {
                                            info.append("  • Gasolina 98\n");
                                            hasFuel = true;
                                        }
                                        if (tags.optString("fuel:diesel", "no").equals("yes")) {
                                            info.append("  • Diésel\n");
                                            hasFuel = true;
                                        }
                                        if (tags.optString("fuel:lpg", "no").equals("yes")) {
                                            info.append("  • GLP\n");
                                            hasFuel = true;
                                        }
                                        if (hasElectric) {
                                            info.append("  • Cargador eléctrico\n");
                                            hasFuel = true;
                                        }
                                        if (!hasFuel) {
                                            info.append("  • No disponible\n");
                                        }

                                        // Servicios adicionales
                                        info.append("\n🛠️ Servicios\n");
                                        StringBuilder servicesCard = new StringBuilder();
                                        boolean hasServicesCard = false;
                                        if (tags.optString("shop", "no").equals("yes")) { servicesCard.append("Tienda, "); hasServicesCard = true; }
                                        if (tags.optString("car_wash", "no").equals("yes")) { servicesCard.append("Lavado de coches, "); hasServicesCard = true; }
                                        if (tags.optString("compressed_air", "no").equals("yes")) { servicesCard.append("Aire comprimido, "); hasServicesCard = true; }
                                        if (tags.optString("toilets", "no").equals("yes")) { servicesCard.append("Baños, "); hasServicesCard = true; }
                                        if (tags.optString("atm", "no").equals("yes")) { servicesCard.append("Cajero automático, "); hasServicesCard = true; }
                                        if (tags.optString("restaurant", "no").equals("yes")) { servicesCard.append("Restaurante, "); hasServicesCard = true; }
                                        String servicesStr = hasServicesCard ? servicesCard.substring(0, servicesCard.length()-2) : "No disponible";

                                        // Horario
                                        String openingHours = tags.optString("opening_hours", "");
                                        info.append("\n⏰ Horario\n");
                                        if (!openingHours.isEmpty()) {
                                            info.append("  ").append(openingHours.replace(";", "\n  "));
                                        } else {
                                            info.append("  • No disponible\n");
                                        }

                                        marker.setOnMarkerClickListener((marker1, mapView) -> {
                                            // Si se hace clic en el mismo marcador, ocultar la tarjeta
                                            if (cardStationInfo.getVisibility() == View.VISIBLE && 
                                                tvStationName.getText().toString().equals(name)) {
                                                cardStationInfo.animate()
                                                        .alpha(0f)
                                                        .setDuration(300)
                                                        .withEndAction(() -> cardStationInfo.setVisibility(View.GONE))
                                                        .start();
                                            } else {
                                                // Dirección
                                                String address = tags.optString("addr:full", tags.optString("addr:street", "No disponible"));
                                                // Combustibles
                                                StringBuilder fuelsCard = new StringBuilder();
                                                boolean hasFuelCard = false;
                                                if (tags.optString("fuel:octane_95", "no").equals("yes")) { fuelsCard.append("Gasolina 95, "); hasFuelCard = true; }
                                                if (tags.optString("fuel:octane_98", "no").equals("yes")) { fuelsCard.append("Gasolina 98, "); hasFuelCard = true; }
                                                if (tags.optString("fuel:diesel", "no").equals("yes")) { fuelsCard.append("Diésel, "); hasFuelCard = true; }
                                                if (tags.optString("fuel:lpg", "no").equals("yes")) { fuelsCard.append("GLP, "); hasFuelCard = true; }
                                                if (hasElectric) { fuelsCard.append("Cargador eléctrico, "); hasFuelCard = true; }
                                                String fuelsStr = hasFuelCard ? fuelsCard.substring(0, fuelsCard.length()-2) : "No disponible";
                                                // Servicios
                                                showPlaceInfo(name, address, fuelsStr, servicesStr, openingHours);
                                            }
                                            return true;
                                        });

                                        map.getOverlays().add(marker);
                                    }
                                }
                            } catch (JSONException e) {
                                continue;
                            }
                        }
                        map.invalidate();
                    });
                } catch (Exception e) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Error procesando gasolineras", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void showPlaceInfo(String name, String address, String fuels, String services, String schedule) {
        tvStationName.setText(name);
        tvStationAddress.setText(address);
        tvFuels.setText(fuels);
        tvServices.setText(services);
        tvSchedule.setText(schedule);
        if (cardStationInfo.getVisibility() != View.VISIBLE) {
            cardStationInfo.setAlpha(0f);
            cardStationInfo.setVisibility(View.VISIBLE);
            cardStationInfo.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
        }
    }

    private void centerOnMyLocation() {
        if (myLocationOverlay.getMyLocation() != null) {
            mapController.animateTo(myLocationOverlay.getMyLocation());
            searchNearbyGasStations(myLocationOverlay.getMyLocation());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
        // Centrar en la ubicación actual cada vez que se vuelve a la pestaña
        if (checkLocationPermission()) {
            centerOnMyLocation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }
}
