package com.example.autosmart;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.room.Room;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

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

        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        UserEntity storedUser = db.userDao().getUser();
        if (storedUser != null) {
            tvUserName.setText(storedUser.getName());
            tvUserEmail.setText(storedUser.getEmail());
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
                    .replace(R.id.content_frame, new DiagnosticFragment())
                    .commit();
        } else if (id == R.id.nav_maps) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new MapsFragment())
                    .commit();
        } else if (id == R.id.nav_settings) {
            // Lanza la Activity o fragmento de ajustes
        } else if (id == R.id.nav_logout) {
            // Implementa el cierre de sesión
            // Por ejemplo: FirebaseAuth.getInstance().signOut();
            // Redirige al LoginActivity y finaliza esta Activity
        }

        // Cierra el menú lateral
        drawer.closeDrawers();
        return true;
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
