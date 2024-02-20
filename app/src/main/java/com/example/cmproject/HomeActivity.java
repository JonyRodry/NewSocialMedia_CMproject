package com.example.cmproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.LruCache;
import androidx.lifecycle.ViewModelProvider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.net.URISyntaxException;

public class HomeActivity extends AppCompatActivity {

    static private final String SHARED_PREFS = "sharedPrefs";

    private MyViewModel viewModel;
    private Intent serviceIntent = null;
    private boolean notificationServiceIsRunning = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Receber o broadcast de logout
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.package.ACTION_LOGOUT");
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        }, intentFilter);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        viewModel = new ViewModelProvider(this).get(MyViewModel.class);

        // Inicializar cache para armazenar as fotos de perfil
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        LruCache<String, Bitmap> memoryCache = new LruCache<>(cacheSize);
        viewModel.setMemoryCache(memoryCache);

        // Receber o user que veio da activity anterior
        Intent intent = getIntent();
        User userLoggedIn = (User) intent.getSerializableExtra("userLoggedIn");
        viewModel.setUserLoggedIn(userLoggedIn);

        // Caso o utilizador tenha acesso à internet, carrega o serviço de notificações
        if(Network.isNetworkAvailable(this)) {
            loadNotificationService();
        }

        // Navbar listener
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.home:
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.home_fragment_view, HomeFragment.class, null)
                                .commit();
                        break;

                    case R.id.search:
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.home_fragment_view, SearchFragment.class, null)
                                .commit();

                        break;

                    case R.id.profile:
                        User user = viewModel.getUserLoggedIn();
                        viewModel.setUserForProfile(user);
                        viewModel.setPage("OwnProfile");
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.home_fragment_view, ProfileFragment.class, null)
                                .commit();
                        break;
                }

                return true;
            }
        });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.home_fragment_view, HomeFragment.class, null)
                .commit();
    }

    public void loadNotificationService() {

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        String notificationServiceString = sharedPreferences.getString("NotificationService", null);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (notificationServiceString == null) {

            // Iniciar o serviço das notificações
            serviceIntent = new Intent(this, NotificationService.class);
            startService(serviceIntent);
            String uriString = serviceIntent.toUri(0);
            editor.putString("NotificationService", uriString);
            editor.commit();
            notificationServiceIsRunning = true;
        }
        else {

            try {
                serviceIntent = Intent.parseUri(notificationServiceString, 0);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            notificationServiceIsRunning = true;
        }
    }

    public void logout() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userLoggedIn", null);
        editor.putString("NotificationService", null);
        editor.commit();

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("com.package.ACTION_LOGOUT");
        sendBroadcast(broadcastIntent);

        if(serviceIntent != null)
            stopService(serviceIntent);

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(serviceIntent != null)
            stopService(serviceIntent);
    }
}