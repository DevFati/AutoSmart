package com.example.autosmart.ui.activity;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.example.autosmart.R;

/**
 * Actividad principal de la aplicación AutoSmart.
 * Gestiona la navegación y la inicialización principal.
 */
public class MainActivity extends AppCompatActivity {

    // Tiempo de espera en milisegundos (ej. 3 segundos)
    private static final int SPLASH_TIME_OUT = 3000;

    /**
     * Método llamado al crear la actividad.
     * @param savedInstanceState Estado guardado.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Establece el layout de la pantalla de bienvenida
        setContentView(R.layout.activity_main);

        // Utiliza un Handler para ejecutar el código que redirige después del tiempo indicado
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Inicia la LoginActivity
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                // Finaliza MainActivity para que el usuario no pueda volver pulsando "atrás"
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}
