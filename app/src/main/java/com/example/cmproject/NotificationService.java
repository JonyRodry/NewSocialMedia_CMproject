package com.example.cmproject;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class NotificationService extends Service {

    static private final String SHARED_PREFS = "sharedPrefs";
    private MQTTHelper mqttHelper = null;
    private Context context;
    private Firebase firebase = new Firebase();
    private User userLoggedIn = null;

    public NotificationService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int id) {

        context = this;

        System.out.println("--- START SERVICE ---");

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        Gson gson = new Gson();
        String userLoggedInJson = sharedPreferences.getString("userLoggedIn", null);

        if(userLoggedInJson != null) {
            userLoggedIn = gson.fromJson(userLoggedInJson, User.class);
            mqttHelper = new MQTTHelper(getApplicationContext(), userLoggedIn.getUsername());

            mqttHelper.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    subscribeToFollowingUsers();
                }

                @Override
                public void connectionLost(Throwable throwable) {

                }

                @Override
                public void messageArrived(String topic, MqttMessage mqttMessage) {
                    messageArrivedNotification(mqttMessage.toString());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                }
            });

            mqttHelper.connect();
        }

        return START_STICKY;
    }

    private void subscribeToFollowingUsers() {
        FirebaseDatabase root = FirebaseDatabase.getInstance("https://cm-project-16f97-default-rtdb.europe-west1.firebasedatabase.app");
        DatabaseReference followingReference = root.getReference("Following/"+userLoggedIn.getUsername());

        // Ir buscar os following users à Firebase
        followingReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<String> followingUsersList = firebase.getFollowingUsers(dataSnapshot);

                for(String followingUser : followingUsersList) {
                    mqttHelper.subscribeToTopic("cm/notifications/" + followingUser);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void messageArrivedNotification(String message) {

        CharSequence name = "CM";
        String CHANNEL_ID = "CM";

        // Criar o objeto JSON a partir da mensagem recebida
        String username = "";
        String postText = "";
        JSONObject postObject = null;
        try {
            postObject = new JSONObject(message);
        } catch (JSONException e) {}

        boolean valid = false;

        // Obter o corpo do post
        if(postObject != null) {
            try {
                username = postObject.getString("Username");
                postText = postObject.getString("Post");
                valid = true;
            } catch (JSONException e) {}
        }

        // Se a mensagem recebida vier no formato correto, cria a notificação
        if(valid) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, name, importance);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("New Post From " + username)
                    .setContentText(postText)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
            notificationManager.notify(0, notificationBuilder.build());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        System.out.println("ON DESTROY SERVICE!!!");

        stopSelf();
        mqttHelper.stop();

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        String userLoggedInJson = sharedPreferences.getString("userLoggedIn", null);

        // Se o utilizador estiver loggado, cria o serviço para ficar a correr em segundo plano
        if(userLoggedInJson != null) {
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("restartservice");
            broadcastIntent.setClass(getApplicationContext(), RestartService.class);
            this.sendBroadcast(broadcastIntent);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}