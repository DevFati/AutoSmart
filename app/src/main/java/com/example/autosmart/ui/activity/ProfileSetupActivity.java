package com.example.autosmart.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.autosmart.R;
import com.example.autosmart.data.db.AppDatabase;
import com.example.autosmart.data.db.UserEntity;

import java.io.IOException;

public class ProfileSetupActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private EditText etUsername;
    private ImageView imgProfile;
    private Button btnChoosePhoto, btnContinue;
    private Uri selectedImageUri = null;
    private String defaultPhotoUri = "android.resource://com.example.autosmart/drawable/ic_default_avatar";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        etUsername = findViewById(R.id.etUsername);
        imgProfile = findViewById(R.id.imgProfile);
        btnChoosePhoto = findViewById(R.id.btnChoosePhoto);
        btnContinue = findViewById(R.id.btnContinue);

        // Imagen por defecto
        imgProfile.setImageResource(R.drawable.ic_profile);

        // Si ya existe usuario en Room, mostrar el nombre desencriptado
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        UserEntity user = db.userDao().getUser();
        if (user != null) {
            String nombre;
            try {
                nombre = com.example.autosmart.utils.EncryptionUtils.decrypt(user.getName());
            } catch (Exception e) {
                nombre = user.getName();
            }
            etUsername.setText(nombre);
        }

        btnChoosePhoto.setOnClickListener(v -> openImagePicker());

        btnContinue.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            if (TextUtils.isEmpty(username)) {
                etUsername.setError("Elige un nombre de usuario");
                return;
            }
            // Guardar en SharedPreferences
            SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("username", username);
            if (selectedImageUri != null) {
                editor.putString("profile_photo", selectedImageUri.toString());
            } else {
                editor.putString("profile_photo", defaultPhotoUri);
            }
            editor.apply();

            // Guardar en base de datos local
            String email = getIntent().getStringExtra("email");
            String uid = getIntent().getStringExtra("uid");
            UserEntity userEntity = new UserEntity(uid, username, email);
            db.userDao().insertUser(userEntity);

            // Actualizar el perfil de Firebase Auth
            com.google.firebase.auth.FirebaseUser firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                com.google.firebase.auth.UserProfileChangeRequest profileUpdates = new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(username)
                    .setPhotoUri(selectedImageUri != null ? selectedImageUri : Uri.parse(defaultPhotoUri))
                    .build();
                firebaseUser.updateProfile(profileUpdates);
            }

            // Ir al Dashboard
            View rootView = findViewById(android.R.id.content);
            com.google.android.material.snackbar.Snackbar snackbar = com.google.android.material.snackbar.Snackbar.make(rootView, "", com.google.android.material.snackbar.Snackbar.LENGTH_LONG);
            @SuppressLint("RestrictedApi") com.google.android.material.snackbar.Snackbar.SnackbarLayout layout = (com.google.android.material.snackbar.Snackbar.SnackbarLayout) snackbar.getView();
            android.view.LayoutInflater inflater = android.view.LayoutInflater.from(this);
            View customView = inflater.inflate(R.layout.snackbar_profile_saved, null);
            android.widget.TextView text = customView.findViewById(R.id.snackbar_text);
            text.setText("¡Perfil guardado!\nPodrás cambiar tu foto de perfil más adelante desde los ajustes.");
            android.widget.ImageView icon = customView.findViewById(R.id.snackbar_icon);
            icon.setImageResource(R.drawable.ic_profile);
            layout.removeAllViews();
            layout.addView(customView);
            snackbar.show();
            new android.os.Handler().postDelayed(() -> {
                Intent intent = new Intent(ProfileSetupActivity.this, DashboardActivity.class);
                startActivity(intent);
                finish();
            }, 5000); // 5 segundos de espera
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                imgProfile.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
} 