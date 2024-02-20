package com.example.cmproject;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import org.w3c.dom.Text;

public class LoginFragment extends Fragment {

    static private final String SHARED_PREFS = "sharedPrefs";
    private Firebase firebase;
    private FirebaseDatabase root = FirebaseDatabase.getInstance("https://cm-project-16f97-default-rtdb.europe-west1.firebasedatabase.app");
    private Database db;
    private MyViewModel viewModel;

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        viewModel = new ViewModelProvider(this).get(MyViewModel.class);
        firebase = new Firebase();
        db = new Database(getContext());

        EditText usernameEditText = view.findViewById(R.id.username);
        EditText passwordEditText = view.findViewById(R.id.password);
        TextView info = view.findViewById(R.id.info_login);
        Button loginButton = view.findViewById(R.id.login_button);
        TextView createAccountButton = view.findViewById(R.id.create_account_button);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                DatabaseReference usersReference = root.getReference("Users");
                usersReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String encryptedPassword = "";
                        try {
                            encryptedPassword = AESEncryption.encrypt(password);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        User userLoggedIn = firebase.login(username, encryptedPassword, dataSnapshot);

                        // Se o utilizador fez login corretamente, é redirecionado para a próxima atividade
                        if(userLoggedIn != null){
                            SharedPreferences sharedPreferences = getActivity().getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            Gson gson = new Gson();
                            String json = gson.toJson(userLoggedIn);
                            editor.putString("userLoggedIn", json);
                            editor.commit();

                            viewModel.setUserLoggedIn(userLoggedIn);

                            Intent intent = new Intent(getContext(), HomeActivity.class);
                            intent.putExtra("userLoggedIn", userLoggedIn);
                            startActivity(intent);
                        }
                        else {
                            info.setText("Username or Password is incorrect");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });

        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_view, RegisterFragment.class, null)
                        .addToBackStack("login")
                        .commit();
            }
        });

        return view;
    }
}