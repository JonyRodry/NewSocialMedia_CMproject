package com.example.cmproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import android.view.MenuItem;
import android.view.View;

import android.widget.EditText;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class CreatePostActivity extends AppCompatActivity {

    private Activity activity;
    private Firebase firebase = new Firebase();
    private MQTTHelper mqttHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        activity = this;

        // Receber o broadcast de logout
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.package.ACTION_LOGOUT");
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        }, intentFilter);

        // Obter o objeto referente ao utilizador que tem o login efetuado
        Intent intent = getIntent();
        User userLoggedIn = (User) intent.getSerializableExtra("userLoggedIn");

        Context context = this;
        EditText postEditText = findViewById(R.id.post_edit_text);

        // Conectar ao broker
        mqttHelper = new MQTTHelper(context, userLoggedIn.getUsername());
        mqttHelper.connect();

        Toolbar toolbar = findViewById(R.id.create_post_toolbar);
        toolbar.inflateMenu(R.menu.create_post_toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);

        // Colocar o botao de publicar post a cinzento
        MenuItem publishButton = toolbar.getMenu().getItem(0);
        publishButton.getIcon().setColorFilter(Color.GRAY,  PorterDuff.Mode.SRC_IN);

        // Listener para verificar se o texto da publicação não é vazio
        postEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                // Bloquear o botão enquanto o texto da publicação é vazio
                if(charSequence.toString().equals("")) {
                    publishButton.setEnabled(false);
                    publishButton.getIcon().setColorFilter(Color.GRAY,  PorterDuff.Mode.SRC_IN);
                }
                else {
                    publishButton.setEnabled(true);
                    publishButton.getIcon().setColorFilter(Color.WHITE,  PorterDuff.Mode.SRC_IN);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // Listener para o botão de voltar atrás
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        // Quando o utilizador clicar no botão para publicar post
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                String postString = postEditText.getText().toString();
                String jsonMessage = "{Username: " + userLoggedIn.getUsername() + ", Post: " + postString + "}";

                mqttHelper.publish("cm/notifications/" + userLoggedIn.getUsername(), jsonMessage);

                firebase.createPost(userLoggedIn.getUsername(), postString);
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                if(!Network.isNetworkAvailable(activity))
                    Toast.makeText(context, "Post will be published when reconnect", Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(context, "Post published successfully!", Toast.LENGTH_LONG).show();
                finish();

                return false;
            }
        });
    }
}