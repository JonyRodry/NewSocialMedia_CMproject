package com.example.cmproject;

import android.content.Context;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.Serializable;

public class MQTTHelper implements Serializable {
    public MqttAndroidClient client = null;

    final String server = "tcp://broker.hivemq.com:1883";
    private String clientId;
    IMqttToken token = null;


    public MQTTHelper(Context context, String clientId) {
        this.clientId = clientId;

        client = new MqttAndroidClient(context, server, clientId);
    }

    public void setCallback(MqttCallbackExtended callback) {
        client.setCallback(callback);
    }

    public void connect() {

        try {
            token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // Conexão com sucesso
                    System.out.println("Connection Succeeded!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Erro na conexão
                    System.out.println("Connection Error!");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void stop() {

        try {
            if(client != null) {
                client.disconnect();
            }
        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }


    public void subscribeToTopic(String topic) {

        try {
            client.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    System.out.println("Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    System.out.println("Subscribed fail!");
                }
            });

        } catch (MqttException ex) {
            System.err.println("Exception subscribing");
            ex.printStackTrace();
        }
    }

    public void unsubscribeFromTopic(String topic) {

        try {
            client.unsubscribe(topic, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    System.out.println("Unsubscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    System.out.println("Error unsubscribing");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String topic, String messageString) {
        System.out.println(messageString);
        MqttMessage message = new MqttMessage(messageString.getBytes());
        try {
            System.out.println(topic);
            client.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
