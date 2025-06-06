package com.example.autosmart.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.autosmart.ui.fragment.DashboardFragment;
import com.example.autosmart.ui.fragment.DiagnosisFragment;
import com.example.autosmart.ui.fragment.MaintenanceFragment;
import com.example.autosmart.ui.fragment.MapsFragment;
import com.example.autosmart.R;
import com.example.autosmart.ui.fragment.SettingsFragment;
import com.example.autosmart.ui.fragment.VehiclesFragment;
import com.example.autosmart.data.db.AppDatabase;
import com.example.autosmart.data.db.UserEntity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawer;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        setupToolbar();
        setupNavigation();
        loadUserData();
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        TextView customTitle = findViewById(R.id.custom_toolbar_title);
        customTitle.setText("AutoSmart");

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, android.R.color.white));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = window.getDecorView().getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void setupNavigation() {
        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        if (getSupportFragmentManager().findFragmentById(R.id.content_frame) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new DashboardFragment())
                    .commit();
            navigationView.setCheckedItem(R.id.nav_dashboard);
        }
    }

    private void loadUserData() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            UserEntity storedUser = db.userDao().getUser();
            
            runOnUiThread(() -> {
                View headerView = navigationView.getHeaderView(0);
                TextView tvUserName = headerView.findViewById(R.id.user_name);
                TextView tvUserEmail = headerView.findViewById(R.id.user_email);
                ImageView userAvatar = headerView.findViewById(R.id.user_avatar);

                if (storedUser != null) {
                    try {
                        String nombre = com.example.autosmart.utils.EncryptionUtils.decrypt(storedUser.getName());
                        String email = com.example.autosmart.utils.EncryptionUtils.decrypt(storedUser.getEmail());
                        tvUserName.setText(nombre);
                        tvUserEmail.setText(email);
                    } catch (Exception e) {
                        tvUserName.setText(storedUser.getName());
                        tvUserEmail.setText(storedUser.getEmail());
                    }
                }

                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                String name = prefs.getString("username", "");
                String photoUri = prefs.getString("profile_photo", null);

                if (name != null && !name.isEmpty()) {
                    tvUserName.setText(name);
                }

                if (photoUri != null && !photoUri.isEmpty()) {
                    Glide.with(this)
                        .load(photoUri)
                        .circleCrop()
                        .into(userAvatar);
                }
            });
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment fragment = null;

        if (id == R.id.nav_dashboard) {
            fragment = new DashboardFragment();
        } else if (id == R.id.nav_vehicles) {
            fragment = new VehiclesFragment();
        } else if (id == R.id.nav_maintenance) {
            fragment = new MaintenanceFragment();
        } else if (id == R.id.nav_diagnostic) {
            fragment = new DiagnosisFragment();
        } else if (id == R.id.nav_maps) {
            fragment = new MapsFragment();
        } else if (id == R.id.nav_settings) {
            fragment = new SettingsFragment();
        } else if (id == R.id.nav_logout) {
            handleLogout();
            return true;
        }

        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .commit();
        }

        drawer.closeDrawers();
        return true;
    }

    private void handleLogout() {
        executor.execute(() -> {
            FirebaseAuth.getInstance().signOut();

            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
            googleSignInClient.signOut();

            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            db.userDao().deleteAll();

            runOnUiThread(() -> {
                Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            });
        });
    }

    public void reloadNavHeader() {
        executor.execute(() -> {
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            String name = prefs.getString("username", "");
            String photoUri = prefs.getString("profile_photo", null);

            runOnUiThread(() -> {
                View headerView = navigationView.getHeaderView(0);
                TextView tvUserName = headerView.findViewById(R.id.user_name);
                ImageView userAvatar = headerView.findViewById(R.id.user_avatar);

                if (name != null && !name.isEmpty()) {
                    tvUserName.setText(name);
                }

                if (photoUri != null && !photoUri.isEmpty()) {
                    Glide.with(this)
                        .load(photoUri)
                        .circleCrop()
                        .into(userAvatar);
                }
            });
        });
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(navigationView)) {
            drawer.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    public void openMenuSection(int menuItemId) {
        navigationView.setCheckedItem(menuItemId);
        onNavigationItemSelected(navigationView.getMenu().findItem(menuItemId));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
