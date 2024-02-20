package com.example.cmproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

public class MainActivity extends AppCompatActivity {

    static private final String SHARED_PREFS = "sharedPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context context = this;

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        Gson gson = new Gson();
        String userLoggedInJson = sharedPreferences.getString("userLoggedIn", null);

        // Se existir um user com login, passamos esse user para a próxima atividade
        if(userLoggedInJson != null) {
            User userLoggedIn = gson.fromJson(userLoggedInJson, User.class);
            Intent intent = new Intent(context, HomeActivity.class);
            intent.putExtra("userLoggedIn", userLoggedIn);
            startActivity(intent);
            finish();
        }

        // Senão é redirecionado para a página de login
        else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_view, LoginFragment.class, null)
                    .commit();
        }
    }
}