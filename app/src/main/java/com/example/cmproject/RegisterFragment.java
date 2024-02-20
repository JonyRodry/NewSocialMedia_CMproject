package com.example.cmproject;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class RegisterFragment extends Fragment {

    private Firebase firebase;
    private FirebaseDatabase root = FirebaseDatabase.getInstance("https://cm-project-16f97-default-rtdb.europe-west1.firebasedatabase.app");
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private Database db;
    EditText nameEditText, usernameEditText, passwordEditText, repeatPasswordEditText;
    TextView info;
    ConstraintLayout progressContainer;
    ProgressBar progressBar;
    private String name, username, password, repeatPassword;
    ImageView profileImg;
    Uri imageUri = null;
    private boolean hasProfileImg;

    public RegisterFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        Toolbar toolbar = view.findViewById(R.id.register_toolbar);
        toolbar.inflateMenu(R.menu.register_toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);

        firebase = new Firebase();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        db = new Database(getContext());

        profileImg = view.findViewById(R.id.profile_image);
        nameEditText = view.findViewById(R.id.name_edit_text);
        usernameEditText = view.findViewById(R.id.username_edit_text);
        passwordEditText = view.findViewById(R.id.password_edit_text);
        repeatPasswordEditText = view.findViewById(R.id.repeat_password_edit_text);
        Button registerButton = view.findViewById(R.id.register_button);
        info = view.findViewById(R.id.info_text_view);
        progressContainer = view.findViewById(R.id.progress_container);
        progressBar = view.findViewById(R.id.progress_bar);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_view, LoginFragment.class, null)
                        .commit();
            }
        });

        profileImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseImage();
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createUser();
            }
        });

        return view;
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1 && resultCode==RESULT_OK && data!=null && data.getData()!=null) {
            imageUri = data.getData();
            profileImg.setImageURI(imageUri);
        }
    }

    private void uploadImage(DataSnapshot usersDataSnapshot) {

        progressContainer.setVisibility(View.VISIBLE);

        StorageReference imageReference = storageReference.child("images/" + username);

        imageReference.putFile(imageUri)
            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    progressContainer.setVisibility(View.GONE);
                    insertUserToFirebase(usersDataSnapshot);
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_view, LoginFragment.class, null)
                            .commit();
                    Toast.makeText(getActivity(), "User created successfully!", Toast.LENGTH_LONG).show();
            }});
    }

    private void createUser() {
            info.setText("");
            name = nameEditText.getText().toString();
            username = usernameEditText.getText().toString();
            password = passwordEditText.getText().toString();
            repeatPassword = repeatPasswordEditText.getText().toString();

            // Apresentar erro se o utilizador deixar algum campo vazio
            if(name.equals("") || username.equals("") || password.equals("") || repeatPassword.equals("")) {
                info.setText("Please fill in empty fields");
            }
            else if(username.contains(" ")) {
                info.setText("Username cannot contain spaces");
            }
            else if(password.equals(repeatPassword)) {

                DatabaseReference usersReference = root.getReference("Users");

                // Ir buscar os utilizadores à Firebase
                usersReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot usersDataSnapshot) {

                        // Criar utilizador e fazer upload da foto de perfil caso o utilizador tenha selecionado alguma
                        hasProfileImg = imageUri != null ? true : false;

                        if(hasProfileImg) {

                            int dataSize = 0;
                            try {
                                InputStream fileInputStream = getContext().getContentResolver().openInputStream(imageUri);
                                dataSize = fileInputStream.available();
                                System.out.println("Image Size: " + dataSize);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if (dataSize < 1024 * 1024)
                                uploadImage(usersDataSnapshot);
                            else
                                info.setText("Image size must be under 1MB");
                        }
                        else {
                            insertUserToFirebase(usersDataSnapshot);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            // Apresentar erro se a password e o repeatPassword forem diferentes
            else
                info.setText("Passwords do not match");
    }

    private void insertUserToFirebase(DataSnapshot usersDataSnapshot) {

        String encryptedPassword = "";
        try {
            encryptedPassword = AESEncryption.encrypt(password);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int status = firebase.createUser(name, username, encryptedPassword, hasProfileImg, usersDataSnapshot);

        switch (status) {
            case 0:
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_view, LoginFragment.class, null)
                        .commit();
                Toast.makeText(getActivity(), "User created successfully!", Toast.LENGTH_LONG).show();
                break;

            case 1:
                info.setText("Username already exists");
                break;

            case 2:
                Toast.makeText(getActivity(), "Error creating user in database", Toast.LENGTH_LONG).show();
                break;
        }
    }
}