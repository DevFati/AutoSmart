package com.example.autosmart.ui.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autosmart.ui.adapter.DiagnosisHistoryAdapter;
import com.example.autosmart.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Fragmento que permite buscar y mostrar información de códigos OBD, así como el historial de diagnósticos.
 */
public class DiagnosisFragment extends Fragment {

    private TextInputEditText etObdCode;
    private MaterialButton btnSearch;
    private ProgressBar progressBar;
    private CardView cardResult;
    private RecyclerView rvHistory;

    // Result views
    private android.widget.TextView tvObdCode, tvObdDefinition, tvObdCauses, tvObdCausesTitle;

    private final List<DiagnosisResult> history = new ArrayList<>();
    private DiagnosisHistoryAdapter historyAdapter;

    /**
     * Se llama para crear y devolver la jerarquía de vistas asociada al fragmento.
     * @param inflater El LayoutInflater.
     * @param container El contenedor padre.
     * @param savedInstanceState Estado guardado.
     * @return Vista raíz del fragmento.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_diagnosis, container, false);

        etObdCode = root.findViewById(R.id.etObdCode);
        btnSearch = root.findViewById(R.id.btnSearchObd);
        progressBar = root.findViewById(R.id.progressDiagnosis);
        cardResult = root.findViewById(R.id.cardDiagnosisResult);
        rvHistory = root.findViewById(R.id.rvDiagnosisHistory);

        tvObdCode = root.findViewById(R.id.tvObdCode);
        tvObdDefinition = root.findViewById(R.id.tvObdDefinition);
        tvObdCauses = root.findViewById(R.id.tvObdCauses);
        tvObdCausesTitle = root.findViewById(R.id.tvObdCausesTitle);

        // Historial
        historyAdapter = new DiagnosisHistoryAdapter(history);
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvHistory.setAdapter(historyAdapter);

        btnSearch.setOnClickListener(v -> searchObdCode());

        return root;
    }

    /**
     * Realiza la búsqueda del código OBD introducido y actualiza el historial.
     */
    private void searchObdCode() {
        String code = etObdCode.getText().toString().trim().toUpperCase();
        if (TextUtils.isEmpty(code)) {
            etObdCode.setError("Introduce un código");
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        cardResult.setVisibility(View.GONE);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://car-code.p.rapidapi.com/obd2/" + code)
                .get()
                .addHeader("x-rapidapi-host", "car-code.p.rapidapi.com")
                .addHeader("x-rapidapi-key", "b2f9469579msha213c07b8130c1ap1e4793jsn5b9c97c7b358")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                if (!response.isSuccessful()) {
                    getActivity().runOnUiThread(() -> {
                        View rootView = getView();
                        if (rootView != null) {
                            Snackbar snackbar = Snackbar.make(rootView, "\u26A0\uFE0F  Código no encontrado", Snackbar.LENGTH_LONG);
                            snackbar.setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.blue_500));
                            snackbar.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                            snackbar.setAction("OK", v -> {});
                            snackbar.show();
                        }
                    });
                    return;
                }
                try {
                    String body = response.body().string();
                    JSONObject obj = new JSONObject(body);
                    String code = obj.optString("code", "");
                    String definition = obj.optString("definition", "Sin definición");
                    JSONArray causesArr = obj.optJSONArray("cause");
                    List<String> causes = new ArrayList<>();
                    if (causesArr != null) {
                        for (int i = 0; i < causesArr.length(); i++) {
                            causes.add(causesArr.getString(i));
                        }
                    }
                    // Traduce la definición y las causas
                    translateText(definition, translatedDef -> {
                        List<String> translatedCauses = new ArrayList<>(java.util.Collections.nCopies(causes.size(), ""));
                        if (causes.isEmpty()) {
                            DiagnosisResult result = new DiagnosisResult(code, translatedDef, translatedCauses);
                            history.add(0, result);
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> showResult(result));
                        } else {
                            AtomicInteger count = new AtomicInteger(0);
                            for (int i = 0; i < causes.size(); i++) {
                                final int idx = i;
                                translateText(causes.get(i), translatedCause -> {
                                    translatedCauses.set(idx, translatedCause);
                                    if (count.incrementAndGet() == causes.size()) {
                                        DiagnosisResult result = new DiagnosisResult(code, translatedDef, translatedCauses);
                                        history.add(0, result);
                                        if (getActivity() == null) return;
                                        getActivity().runOnUiThread(() -> showResult(result));
                                    }
                                });
                            }
                        }
                    });
                } catch (Exception e) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Error procesando respuesta", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    /**
     * Traduce un texto usando la API de Google Translate.
     * @param text Texto a traducir.
     * @param callback Callback con el texto traducido.
     */
    private void translateText(String text, OnTranslationReady callback) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=es&dt=t&q=" + Uri.encode(text);
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onReady(text); // Si falla, muestra el original
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    String body = response.body().string();
                    JSONArray arr = new JSONArray(body);
                    String translated = arr.getJSONArray(0).getJSONArray(0).getString(0);
                    callback.onReady(translated);
                } catch (Exception e) {
                    callback.onReady(text);
                }
            }
        });
    }

    interface OnTranslationReady {
        void onReady(String translatedText);
    }

    /**
     * Muestra el resultado de un diagnóstico en la interfaz.
     * @param result Resultado del diagnóstico.
     */
    private void showResult(DiagnosisResult result) {
        cardResult.setVisibility(View.VISIBLE);
        tvObdCode.setText("Código: " + result.getCode());
        tvObdDefinition.setText(result.getDefinition());
        if (result.getCauses() != null && !result.getCauses().isEmpty()) {
            tvObdCausesTitle.setVisibility(View.VISIBLE);
            tvObdCauses.setText("• " + TextUtils.join("\n• ", result.getCauses()));
        } else {
            tvObdCausesTitle.setVisibility(View.GONE);
            tvObdCauses.setText("");
        }
        historyAdapter.notifyDataSetChanged();
    }

    /**
     * Modelo simple para el resultado de un diagnóstico OBD.
     */
    public static class DiagnosisResult {
        private String code, definition;
        private List<String> causes;
        /**
         * Constructor de DiagnosisResult.
         * @param code Código OBD.
         * @param definition Definición del código.
         * @param causes Lista de causas posibles.
         */
        public DiagnosisResult(String code, String definition, List<String> causes) {
            this.code = code;
            this.definition = definition;
            this.causes = causes;
        }
        /**
         * Obtiene el código OBD.
         * @return Código OBD.
         */
        public String getCode() { return code; }
        /**
         * Obtiene la definición del código.
         * @return Definición.
         */
        public String getDefinition() { return definition; }
        /**
         * Obtiene la lista de causas posibles.
         * @return Lista de causas.
         */
        public List<String> getCauses() { return causes; }
    }
} 