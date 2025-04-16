package com.example.autosmart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class DashboardFragment extends Fragment {
    private TextView tvGreeting;

    public DashboardFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Infla el layout para este fragment
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);



        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvGreeting = view.findViewById(R.id.tvGreeting);

        // Recuperar el usuario de la base de datos local
        AppDatabase db = AppDatabase.getInstance(requireContext());
        UserEntity user = db.userDao().getUser();

        if (user != null) {
            // Mostrar saludo personalizado
            tvGreeting.setText("Hola, " + user.getName());
        } else {
            tvGreeting.setText("Hola, conductor");
        }


    }
}
