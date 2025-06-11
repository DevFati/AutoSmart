package com.example.autosmart.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.example.autosmart.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class SettingsActivity extends AppCompatActivity {
    private static final String PRIVACY_POLICY_URL = "https://www.privacypolicies.com/live/d89f7d6b-b6f0-4740-86bf-25972ed4e010";
    private ImageView ivProfile;
    private TextInputEditText etUsername;
    private SharedPreferences prefs;
    private static final int PICK_IMAGE = 1;
    private boolean isDarkModeEnabled;
    private MaterialButton btnSave;
    private MaterialButton btnPrivacy;
    private MaterialButton btnAbout;
    private MaterialButton btnLogout;
    private int selectedTheme = -1; // 0: claro, 1: oscuro
    private RadioGroup radioGroupTheme;
    private RadioButton radioLight;
    private RadioButton radioDark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Configurar toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Configuración");

        // Inicializar vistas
        ivProfile = findViewById(R.id.ivProfile);
        etUsername = findViewById(R.id.etUsername);
        prefs = getSharedPreferences("AutoSmartPrefs", MODE_PRIVATE);
        btnSave = findViewById(R.id.btnSave);
        btnPrivacy = findViewById(R.id.btnPrivacy);
        btnAbout = findViewById(R.id.btnAbout);
        btnLogout = findViewById(R.id.btnLogout);
        radioGroupTheme = findViewById(R.id.radioGroupTheme);
        radioLight = findViewById(R.id.radioLight);
        radioDark = findViewById(R.id.radioDark);

        // Cargar datos guardados
        String username = prefs.getString("username", "");
        String photoPath = prefs.getString("photo_path", "");
        isDarkModeEnabled = prefs.getBoolean("dark_mode", false);
        selectedTheme = isDarkModeEnabled ? 1 : 0;
        etUsername.setText(username);
        if (isDarkModeEnabled) radioDark.setChecked(true); else radioLight.setChecked(true);
        radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioLight) selectedTheme = 0;
            else if (checkedId == R.id.radioDark) selectedTheme = 1;
        });

        // Configurar listeners
        setupListeners();
    }

    private void setupListeners() {
        // Cambiar foto de perfil
        ivProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE);
        });

        // Guardar cambios
        btnSave.setOnClickListener(v -> saveSettings());

        // Privacidad y permisos
        btnPrivacy.setOnClickListener(v -> openPrivacyPolicy());

        // Acerca de
        btnAbout.setOnClickListener(v -> showAboutDialog());

        // Cerrar sesión
        btnLogout.setOnClickListener(v -> logout());
    }

    private void saveSettings() {
        String newUsername = etUsername.getText().toString().trim();
        // Guardar nombre de usuario
        if (!newUsername.isEmpty()) {
            prefs.edit().putString("username", newUsername).apply();
        }
        // Guardar y aplicar tema solo si cambió
        boolean newDarkMode = (selectedTheme == 1);
        if (newDarkMode != isDarkModeEnabled) {
            prefs.edit().putBoolean("dark_mode", newDarkMode).apply();
            AppCompatDelegate.setDefaultNightMode(
                newDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            recreate();
        }
        Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show();
    }

    private void openPrivacyPolicy() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL));
        startActivity(intent);
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Acerca de AutoSmart")
            .setMessage("AutoSmart v1.0\n\n" +
                       "Una aplicación para gestionar el mantenimiento de tus vehículos.\n\n" +
                       "Desarrollado con amor para hacer tu vida más fácil.")
            .setPositiveButton("OK", null)
            .show();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            ivProfile.setImageURI(selectedImage);
            
            // Guardar la ruta de la imagen
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("photo_path", selectedImage.toString());
            editor.apply();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 