package com.example.cmproject;

import static android.content.Context.MODE_PRIVATE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class RestartService extends BroadcastReceiver {

    static private final String SHARED_PREFS = "sharedPrefs";

    @Override
    public void onReceive(Context context, Intent intent) {

        System.out.println("RESTART SERVICE");

        // Iniciar o serviço das notificações
        Intent serviceIntent = new Intent(context, NotificationService.class);
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String uriString = serviceIntent.toUri(0);
        editor.putString("NotificationService", uriString);
        editor.commit();

        context.startService(serviceIntent);
    }
}
