package com.example.autosmart;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;

public class SettingsFragment extends Fragment {
    private static final int PICK_IMAGE = 1001;
    private static final int TAKE_PHOTO = 1002;
    private ImageView imgProfile;
    private TextView tvEditPhoto;
    private EditText etUsername;
    private Spinner spinnerLanguage;
    private RadioGroup rgTheme;
    private Switch switchMaint, switchFuel;
    private Button btnLogout, btnDeleteAccount;
    private Uri profileImageUri;
    private SharedPreferences prefs;
    private String currentPhotoPath;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);
        prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);

        imgProfile = root.findViewById(R.id.imgProfile);
        tvEditPhoto = root.findViewById(R.id.tvEditPhoto);
        etUsername = root.findViewById(R.id.etUsername);
        spinnerLanguage = root.findViewById(R.id.spinnerLanguage);
        rgTheme = root.findViewById(R.id.rgTheme);
        switchMaint = root.findViewById(R.id.switchMaint);
        switchFuel = root.findViewById(R.id.switchFuel);
        btnLogout = root.findViewById(R.id.btnLogout);
        btnDeleteAccount = root.findViewById(R.id.btnDeleteAccount);

        // Cargar nombre y foto guardados
        etUsername.setText(prefs.getString("username", ""));
        String photoUri = prefs.getString("profile_photo", null);
        if (photoUri != null && !photoUri.isEmpty()) {
            if (photoUri.startsWith("http")) {
                new Thread(() -> {
                    try {
                        InputStream in = new URL(photoUri).openStream();
                        Bitmap bmp = BitmapFactory.decodeStream(in);
                        requireActivity().runOnUiThread(() -> imgProfile.setImageBitmap(bmp));
                    } catch (Exception ignored) {}
                }).start();
            } else {
                imgProfile.setImageURI(Uri.parse(photoUri));
            }
        }

        // Foto de perfil
        imgProfile.setOnClickListener(v -> showPhotoOptions());
        tvEditPhoto.setOnClickListener(v -> showPhotoOptions());

        // Nombre editable
        etUsername.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String name = etUsername.getText().toString();
                prefs.edit().putString("username", name).apply();
                notifyProfileChanged();
            }
        });

        // Idioma
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new String[]{"Español", "English"});
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(langAdapter);
        String lang = prefs.getString("lang", "es");
        spinnerLanguage.setSelection(lang.equals("en") ? 1 : 0);
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newLang = (position == 1) ? "en" : "es";
                if (!newLang.equals(prefs.getString("lang", "es"))) {
                    prefs.edit().putString("lang", newLang).apply();
                    setLocale(newLang);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Tema
        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbLight) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else if (checkedId == R.id.rbDark) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
        });

        // Notificaciones (puedes guardar en SharedPreferences)
        switchMaint.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("notif_maint", isChecked).apply();
        });
        switchFuel.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("notif_fuel", isChecked).apply();
        });

        // Privacidad y acerca de
        root.findViewById(R.id.tvPrivacy).setOnClickListener(v -> {
            // TODO: Abrir política de privacidad o pantalla de permisos
        });
        root.findViewById(R.id.tvAbout).setOnClickListener(v -> {
            // TODO: Mostrar información de la app
        });

        // Cerrar sesión
        btnLogout.setOnClickListener(v -> {
            // TODO: Lógica de logout
            Snackbar.make(root, "Sesión cerrada", Snackbar.LENGTH_SHORT).show();
        });

        // Eliminar cuenta
        btnDeleteAccount.setOnClickListener(v -> confirmDeleteAccount());

        return root;
    }

    private void showPhotoOptions() {
        String[] options = {"Tomar foto", "Elegir de galería", "Desde URL"};
        new AlertDialog.Builder(getContext())
                .setTitle("Cambiar foto de perfil")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) takePhoto();
                    else if (which == 1) pickImage();
                    else if (which == 2) enterPhotoUrl();
                })
                .show();
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = File.createTempFile("profile_photo", ".jpg", requireActivity().getExternalFilesDir(null));
                currentPhotoPath = photoFile.getAbsolutePath();
            } catch (IOException ex) { return; }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, TAKE_PHOTO);
            }
        }
    }

    private void enterPhotoUrl() {
        EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        new AlertDialog.Builder(getContext())
                .setTitle("Introduce la URL de la imagen")
                .setView(input)
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    String url = input.getText().toString();
                    if (!url.isEmpty()) {
                        prefs.edit().putString("profile_photo", url).apply();
                        new Thread(() -> {
                            try {
                                InputStream in = new URL(url).openStream();
                                Bitmap bmp = BitmapFactory.decodeStream(in);
                                requireActivity().runOnUiThread(() -> imgProfile.setImageBitmap(bmp));
                            } catch (Exception ignored) {}
                        }).start();
                        notifyProfileChanged();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            profileImageUri = data.getData();
            imgProfile.setImageURI(profileImageUri);
            prefs.edit().putString("profile_photo", profileImageUri.toString()).apply();
            notifyProfileChanged();
        } else if (requestCode == TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            File file = new File(currentPhotoPath);
            if (file.exists()) {
                imgProfile.setImageURI(Uri.fromFile(file));
                prefs.edit().putString("profile_photo", Uri.fromFile(file).toString()).apply();
                notifyProfileChanged();
            }
        }
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        requireActivity().recreate();
    }

    private void notifyProfileChanged() {
        // Notifica a la actividad para que recargue el header del menú lateral
        if (getActivity() instanceof DashboardActivity) {
            ((DashboardActivity) getActivity()).reloadNavHeader();
        }
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar cuenta")
                .setMessage("¿Estás seguro de que quieres eliminar tu cuenta? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    // TODO: Eliminar cuenta del backend
                    Snackbar.make(getView(), "Cuenta eliminada", Snackbar.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
} 