package com.example.autosmart;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.room.Room;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawer;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);  // Usa el layout creado anteriormente

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Configura el toggle del Drawer con la Toolbar
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();


        View headerView = navigationView.getHeaderView(0);
        TextView tvUserName = headerView.findViewById(R.id.user_name);
        TextView tvUserEmail = headerView.findViewById(R.id.user_email);
        ImageView userAvatar = headerView.findViewById(R.id.user_avatar);

        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        UserEntity storedUser = db.userDao().getUser();
        if (storedUser != null) {
            tvUserName.setText(storedUser.getName());
            tvUserEmail.setText(storedUser.getEmail());
        }

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String name = prefs.getString("username", "");
        String photoUri = prefs.getString("profile_photo", null);
        if (name != null && !name.isEmpty()) {
            tvUserName.setText(name);
        }
        if (photoUri != null && !photoUri.isEmpty()) {
            if (photoUri.startsWith("http")) {
                new Thread(() -> {
                    try {
                        java.io.InputStream in = new java.net.URL(photoUri).openStream();
                        final android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(in);
                        runOnUiThread(() -> userAvatar.setImageBitmap(bmp));
                    } catch (Exception ignored) {}
                }).start();
            } else {
                userAvatar.setImageURI(android.net.Uri.parse(photoUri));
            }
        }

        // Carga el fragmento Dashboard por defecto
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new DashboardFragment())
                    .commit();
            navigationView.setCheckedItem(R.id.nav_dashboard);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new DashboardFragment())
                    .commit();
        } else if (id == R.id.nav_vehicles) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new VehiclesFragment())
                    .commit();
        } else if (id == R.id.nav_maintenance) {
            // Cargar el fragmento de mantenimiento (por implementar)
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new MaintenanceFragment())
                    .commit();
        } else if (id == R.id.nav_diagnostic) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new DiagnosisFragment())
                    .commit();
        } else if (id == R.id.nav_maps) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new MapsFragment())
                    .commit();
        } else if (id == R.id.nav_settings) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, new SettingsFragment())
                .commit();
        } else if (id == R.id.nav_logout) {
            // Cierra la sesión en Firebase
            FirebaseAuth.getInstance().signOut();

            // Cierra la sesión en Google
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
            googleSignInClient.signOut();

            // Borra el usuario almacenado en la base de datos local
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            db.userDao().deleteAll();

            // Redirige al LoginActivity y finaliza DashboardActivity
            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }

        // Cierra el menú lateral
        drawer.closeDrawers();
        return true;
    }

    public void reloadNavHeader() {
        View headerView = navigationView.getHeaderView(0);
        TextView tvUserName = headerView.findViewById(R.id.user_name);
        TextView tvUserEmail = headerView.findViewById(R.id.user_email);
        ImageView userAvatar = headerView.findViewById(R.id.user_avatar);
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String name = prefs.getString("username", "");
        String photoUri = prefs.getString("profile_photo", null);
        if (name != null && !name.isEmpty()) {
            tvUserName.setText(name);
        }
        if (photoUri != null && !photoUri.isEmpty()) {
            if (photoUri.startsWith("http")) {
                new Thread(() -> {
                    try {
                        java.io.InputStream in = new java.net.URL(photoUri).openStream();
                        final android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(in);
                        runOnUiThread(() -> userAvatar.setImageBitmap(bmp));
                    } catch (Exception ignored) {}
                }).start();
            } else {
                userAvatar.setImageURI(android.net.Uri.parse(photoUri));
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Cierra el Drawer si está abierto
        if (drawer.isDrawerOpen(navigationView)) {
            drawer.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }
}
