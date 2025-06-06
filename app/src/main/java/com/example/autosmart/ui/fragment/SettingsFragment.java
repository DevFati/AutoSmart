package com.example.autosmart.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.autosmart.R;
import com.example.autosmart.data.db.AppDatabase;
import com.example.autosmart.data.db.UserEntity;
import com.example.autosmart.ui.activity.DashboardActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import android.Manifest;
import android.content.pm.PackageManager;

public class SettingsFragment extends Fragment {
    private static final int PICK_IMAGE = 1001;
    private static final int TAKE_PHOTO = 1002;
    private ImageView imgProfile;
    private TextView tvEditPhoto;
    private EditText etUsername;
    private Spinner spinnerLanguage;
    private RadioGroup rgTheme;
    private Switch switchMaint;
    private Button btnDeleteAccount;
    private Uri profileImageUri;
    private SharedPreferences prefs;
    private String currentPhotoPath;
    private String pendingPhotoUri = null;
    private String pendingUsername = null;
    private String pendingLang = null;
    private int pendingTheme = -1;
    private int pendingPhotoAction = -1; // 0: take photo, 1: pick image

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);
        prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);

        imgProfile = root.findViewById(R.id.imgProfile);
        tvEditPhoto = root.findViewById(R.id.tvEditPhoto);
        etUsername = root.findViewById(R.id.etUsername);
        rgTheme = root.findViewById(R.id.rgTheme);
        switchMaint = root.findViewById(R.id.switchMaint);
        btnDeleteAccount = root.findViewById(R.id.btnDeleteAccount);

        // Cargar nombre y foto guardados
        AppDatabase db = AppDatabase.getInstance(requireContext());
        UserEntity user = db.userDao().getUser();
        if (user != null) {
            String nombre;
            try {
                nombre = com.example.autosmart.utils.EncryptionUtils.decrypt(user.getName());
            } catch (Exception e) {
                nombre = user.getName();
            }
            etUsername.setText(nombre);
        } else {
            etUsername.setText("");
        }
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

        // Tema
        int currentTheme = prefs.getInt("theme", 0); // 0: light, 1: dark
        if (currentTheme == 0) {
            rgTheme.check(R.id.rbLight);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            rgTheme.check(R.id.rbDark);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int theme = 0;
            if (checkedId == R.id.rbLight) {
                theme = 0;
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else if (checkedId == R.id.rbDark) {
                theme = 1;
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            prefs.edit().putInt("theme", theme).apply();
        });

        // Notificaciones de mantenimiento
        switchMaint.setChecked(prefs.getBoolean("notif_maint", false));
        switchMaint.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("notif_maint", isChecked).apply();
            if (isChecked) {
                // Solicitar permiso de notificaciones si no está concedido
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) 
                            != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1003);
                    }
                }
            }
        });

        // Privacidad y acerca de
        root.findViewById(R.id.tvPrivacy).setOnClickListener(v -> {
            // TODO: Abrir política de privacidad o pantalla de permisos
        });
        root.findViewById(R.id.tvAbout).setOnClickListener(v -> {
            // TODO: Mostrar información de la app
        });

        // Eliminar cuenta
        btnDeleteAccount.setOnClickListener(v -> confirmDeleteAccount());

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button btnSave = view.findViewById(R.id.btnSaveSettings);
        btnSave.setOnClickListener(v -> {
            // Guardar nombre
            String name = etUsername.getText().toString();
            prefs.edit().putString("username", name).apply();
            // Guardar foto
            if (pendingPhotoUri != null) {
                prefs.edit().putString("profile_photo", pendingPhotoUri).apply();
                pendingPhotoUri = null;
            }
            // Guardar tema
            if (pendingTheme != -1) {
                if (pendingTheme == 0) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                else if (pendingTheme == 1) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                pendingTheme = -1;
            }
            notifyProfileChanged();
            Snackbar.make(requireView(), "Cambios guardados", Snackbar.LENGTH_LONG).show();
        });
    }

    private void showPhotoOptions() {
        String[] options = {"Tomar foto", "Elegir de galería", "Desde URL"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cambiar foto de perfil")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        pendingPhotoAction = 0;
                        takePhotoWithPermission();
                    } else if (which == 1) {
                        pendingPhotoAction = 1;
                        pickImageWithPermission();
                    } else if (which == 2) enterPhotoUrl();
                })
                .show();
    }

    private void pickImageWithPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_MEDIA_IMAGES}, PICK_IMAGE);
        } else {
            pickImage();
        }
    }

    private void takePhotoWithPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, TAKE_PHOTO);
        } else {
            takePhoto();
        }
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
        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Introduce la URL de la imagen");
        final TextInputLayout inputLayout = new TextInputLayout(requireContext());
        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        inputLayout.setBoxCornerRadii(12,12,12,12);
        inputLayout.setHint("URL de la imagen");
        final TextInputEditText input = new TextInputEditText(requireContext());
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        inputLayout.addView(input);
        builder.setView(inputLayout);
        builder.setNegativeButton("Cancelar", null);
        builder.setPositiveButton("Aceptar", null);
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            final Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btn.setEnabled(false);
            input.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String url = s.toString();
                    boolean valid = url.matches("^https?://.*\\.(jpg|jpeg|png|gif|bmp|webp)$");
                    btn.setEnabled(valid);
                    inputLayout.setError(valid ? null : "Introduce una URL válida de imagen");
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
            btn.setOnClickListener(v -> {
                String url = input.getText().toString();
                pendingPhotoUri = url;
                // Previsualizar
                new Thread(() -> {
                    try {
                        InputStream in = new URL(url).openStream();
                        Bitmap bmp = BitmapFactory.decodeStream(in);
                        requireActivity().runOnUiThread(() -> imgProfile.setImageBitmap(bmp));
                    } catch (Exception ignored) {}
                }).start();
                dialog.dismiss();
            });
        });
        dialog.show();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PICK_IMAGE) {
            boolean granted = false;
            for (int res : grantResults) if (res == PackageManager.PERMISSION_GRANTED) granted = true;
            if (granted) {
                pickImage();
            } else {
                Snackbar.make(requireView(), "Permiso de galería denegado", Snackbar.LENGTH_LONG).show();
            }
        } else if (requestCode == TAKE_PHOTO) {
            boolean granted = false;
            for (int res : grantResults) if (res == PackageManager.PERMISSION_GRANTED) granted = true;
            if (granted) {
                takePhoto();
            } else {
                Snackbar.make(requireView(), "Permiso de cámara denegado", Snackbar.LENGTH_LONG).show();
            }
        }
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