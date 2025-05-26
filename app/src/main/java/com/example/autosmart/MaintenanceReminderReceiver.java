package com.example.autosmart;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import android.util.Log;

public class MaintenanceReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "maint_reminder";
    private static final String CHANNEL_NAME = "Recordatorios de mantenimiento";
    private static final String CHANNEL_DESC = "Notificaciones de mantenimientos programados";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("MaintenanceReceiver", "Recibida notificación de mantenimiento");
        
        // Verificar si las notificaciones están activadas
        SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("notif_maint", false)) {
            Log.d("MaintenanceReceiver", "Notificaciones desactivadas en preferencias");
            return;
        }

        String type = intent.getStringExtra("type");
        String plate = intent.getStringExtra("plate");
        String vehId = intent.getStringExtra("vehId");
        int notificationId = intent.getIntExtra("notificationId", (int) System.currentTimeMillis());

        String title = "¡Tienes un mantenimiento hoy!";
        String text = "Tipo: " + type + "\nVehículo: " + plate;

        // Crear canal de notificación si es necesario
        createNotificationChannel(context);

        // Construir la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_service)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(new long[]{0, 500, 200, 500})
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        // Mostrar la notificación
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) 
                        == PackageManager.PERMISSION_GRANTED) {
                    nm.notify(notificationId, builder.build());
                    Log.d("MaintenanceReceiver", "Notificación mostrada correctamente");
                } else {
                    Log.e("MaintenanceReceiver", "No hay permiso para mostrar notificaciones");
                }
            } else {
                nm.notify(notificationId, builder.build());
                Log.d("MaintenanceReceiver", "Notificación mostrada correctamente");
            }
        } catch (Exception e) {
            Log.e("MaintenanceReceiver", "Error mostrando notificación: " + e.getMessage());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            channel.setShowBadge(true);
            
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
                Log.d("MaintenanceReceiver", "Canal de notificación creado");
            }
        }
    }
} 