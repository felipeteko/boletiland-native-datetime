package com.boletiland.native.datetime;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "NativeDateTime")
public class NativeDateTime extends Plugin {

    private static final String TAG = "NativeDateTime";

    @Override
    public void load() {
        super.load();
        Log.d(TAG, "NativeDateTime plugin loaded");
    }

    @PluginMethod
    public void syncFromRemote(PluginCall call) {
       // Implementación para obtener la hora desde una llamada remota
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
                Context context = getContext();
                long dateTimeMillis = parseDateTime(dateTime);
                Settings.Global.putInt(context.getContentResolver(), Settings.Global.AUTO_TIME, 0);
                Settings.Global.putInt(context.getContentResolver(), Settings.Global.AUTO_TIME_ZONE, 0);
                Settings.Global.putLong(context.getContentResolver(), Settings.Global.TIME, dateTimeMillis);

                // Indicar que la operación fue exitosa
                call.resolve(true);
            } catch (Exception e) {
                Log.e(TAG, "Error syncing from remote time: " + e.getMessage(), e);
                call.reject("Error syncing from remote time: " + e.getMessage());
            }
        }).start();
    }

    @PluginMethod
    public void getDateAndTime(PluginCall call) {
        try {
            long currentTimeMillis = System.currentTimeMillis();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
            String currentDateTime = dateFormat.format(currentTimeMillis);

            JSObject result = new JSObject();
            result.put("datetime", currentDateTime);

            call.resolve(result);
        } catch (Exception e) {
            Log.e(TAG, "Error getting date and time: " + e.getMessage(), e);
            call.reject("Error getting date and time");
        }
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
