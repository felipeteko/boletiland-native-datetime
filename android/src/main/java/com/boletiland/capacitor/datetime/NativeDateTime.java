package com.boletiland.capacitor.datetime;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

@CapacitorPlugin(
        name = "NativeDateTime",
        permissions = {
                @Permission(
                        alias = "writeSettings",
                        strings = { Manifest.permission.WRITE_SETTINGS }
                ),
                @Permission(
                        alias = "setTime",
                        strings = { Manifest.permission.SET_TIME }
                )
        }
)
public class NativeDateTime extends Plugin {

    private static final String TAG = "NativeDateTime";

    private static final int REQUEST_CODE_WRITE_SETTINGS = 100;

    private PluginCall permissionCallback;

    @Override
    public void load() {
        super.load();
        Log.d(TAG, "NativeDateTime plugin loaded");
    }

    @PermissionCallback
    private void onRequestWriteSettingsPermission(boolean granted) {
        if (granted) {
            // El usuario ha concedido el permiso WRITE_SETTINGS
            Log.e(TAG, "Permission WRITE_SETTINGS granted");
            // Se procede a realizar la operación de cambiar la hora del dispositivo
            if (permissionCallback != null) {
                syncTimeFromRemote(permissionCallback);
                permissionCallback = null; // Limpiar la llamada para evitar llamadas múltiples
            }
        } else {
            // El usuario no concedió el permiso WRITE_SETTINGS
            // Se muestra un mensaje al usuario informándole sobre la importancia del permiso
            Log.e(TAG, "Permission WRITE_SETTINGS denied");
            if (permissionCallback != null) {
                permissionCallback.reject("Permission WRITE_SETTINGS denied");
                permissionCallback = null;
            }
        }
    }

    private void requestWriteSettingsPermission(PluginCall call) {
        permissionCallback = call;
        if (hasRequiredPermissions()) {
            onRequestWriteSettingsPermission(true);
        } else {
            pluginRequestPermissions(new String[]{Manifest.permission.WRITE_SETTINGS, Manifest.permission.SET_TIME}, REQUEST_CODE_WRITE_SETTINGS);
        }
    }

    @Override
    protected void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.handleRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_WRITE_SETTINGS) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            onRequestWriteSettingsPermission(granted);
        }
    }

    @PluginMethod()
    public void syncFromRemote(PluginCall call) {
        requestWriteSettingsPermission(call);
    }

    @PluginMethod()
    public void getCurrent(PluginCall call) {
        try {
            //Obtener Actual
            long currentTimeMillis = System.currentTimeMillis();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
            String currentDateTime = dateFormat.format(currentTimeMillis);

            JSObject result = new JSObject();
            result.put("datetime", currentDateTime);

            call.resolve(result);
        } catch (Exception e) {
            Log.e(TAG, "Error getting current date and time: " + e.getMessage(), e);
            call.reject("Error getting current date and time");
        }
    }

    // Método para sincronizar la hora desde una fuente remota
    private void syncTimeFromRemote(final PluginCall call) {
        new Thread(() -> {
            try {
                // Realizar la llamada a la API de WorldTime
                URL url = new URL("https://worldtimeapi.org/api/timezone/America/Mexico_City");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                // Leer la respuesta JSON
                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parsear la respuesta JSON
                JSObject responseObject = new JSObject(response.toString());
                String dateTime = responseObject.getString("datetime");

                // Establecer la hora del sistema
                long dateTimeMillis = parseDateTime(dateTime);

                // Establecer la hora usando AlarmManager
                AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
                alarmManager.setTime(dateTimeMillis);

                // Indicar que la operación fue exitosa
                call.resolve(new JSObject("true"));
            } catch (Exception e) {
                Log.e(TAG, "Error syncing from remote time: " + e.getMessage(), e);
                call.reject("Error syncing from remote time: " + e.getMessage());
            }
        }).start();
    }

    // Método para convertir la fecha y hora en milisegundos
    private long parseDateTime(String dateTime) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dateFormat.parse(dateTime));
            return calendar.getTimeInMillis();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing date and time: " + e.getMessage(), e);
            return System.currentTimeMillis(); // Devuelve el tiempo actual si hay un error
        }
    }
}