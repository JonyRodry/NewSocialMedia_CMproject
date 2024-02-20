package com.example.cmproject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
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

import java.io.InputStream;

public class EditProfileActivity extends AppCompatActivity {

    private Context context;
    private Activity activity;
    EditText nameEditText, passwordEditText, repeatNewPasswordEditText;
    ImageView profileImg;
    TextView info;
    String oldName, username, oldPassword, newName, newPassword, repeatNewPassword;
    private FirebaseDatabase root = FirebaseDatabase.getInstance("https://cm-project-16f97-default-rtdb.europe-west1.firebasedatabase.app");
    private Firebase firebase = new Firebase();
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private StorageReference imageRef;
    ConstraintLayout progressContainer;
    ProgressBar progressBar;
    Uri imageUri = null;
    private User userLoggedIn;
    private boolean profileImgChanged=false, nameChanged=false, passwordChanged=false, check=true, hasProfileImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        context = this;
        activity = this;
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Obter o utilizador que está loggado
        Intent intent = getIntent();
        userLoggedIn = (User) intent.getSerializableExtra("userLoggedIn");

        oldName = userLoggedIn.getName();
        username = userLoggedIn.getUsername();
        oldPassword = userLoggedIn.getPassword();

        // Receber o broadcast de logout
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.package.ACTION_LOGOUT");
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        }, intentFilter);

        Toolbar toolbar = findViewById(R.id.edit_profile_toolbar);
        toolbar.inflateMenu(R.menu.edit_profile_toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        progressContainer = findViewById(R.id.progress_container);
        progressBar = findViewById(R.id.progress_bar);

        nameEditText = findViewById(R.id.edit_name);
        passwordEditText = findViewById(R.id.edit_password);
        repeatNewPasswordEditText = findViewById(R.id.edit_repeat_password);
        profileImg = findViewById(R.id.edit_profile_image);
        info = findViewById(R.id.edit_profile_info);

        nameEditText.setText(oldName);
        passwordEditText.setText(oldPassword);

        // Se o utilizador tiver foto de perfil, esta é carregada
        if(userLoggedIn.isHasProfileImg()) {
            imageRef = storage.getReference().child("images/" + userLoggedIn.getUsername());
            imageRef.getBytes(1024 * 1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    profileImg.setImageBitmap(bitmap);
                }
            });
        }

        // Listener para voltar atrás
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        // Listener para o botão de guardar alterações
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(Network.isNetworkAvailable(activity))
                    saveProfileChanges();
                else
                    Toast.makeText(context, "No Internet Connection", Toast.LENGTH_LONG).show();
                return false;
            }
        });

        // Listener para alterar a foto de perfil
        profileImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseImage();
            }
        });
    }

    private void saveProfileChanges() {
        check = true;
        newName = nameEditText.getText().toString();
        newPassword = passwordEditText.getText().toString();
        repeatNewPassword = repeatNewPasswordEditText.getText().toString();

        if(newName.equals("") || newPassword.equals("")) {
            info.setText("Fields cannot be empty");
        }
        else {
            if(!oldName.equals(newName)) {
                nameChanged = true;
            }
            if(!oldPassword.equals(newPassword)) {
                if(repeatNewPassword.equals("")) {
                    info.setText("Repeat New Password field cannot be empty");
                    check = false;
                }
                else if(!newPassword.equals(repeatNewPassword)) {
                    info.setText("Passwords do not match");
                    check = false;
                }
                else
                    passwordChanged = true;
            }

            // Se os dados forem submetidos corretamente, então atualizamos o utilizador na Firebase
            if(check && (profileImgChanged || nameChanged || passwordChanged)) {

                DatabaseReference usersReference = root.getReference("Users");

                // Ir buscar os utilizadores à Firebase
                usersReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot usersDataSnapshot) {

                        // Fazer upload da foto de perfil caso o utilizador tenha selecionado alguma
                        hasProfileImg = userLoggedIn.isHasProfileImg();

                        if(profileImgChanged) {

                            int dataSize = 0;
                            try {
                                InputStream fileInputStream = getContentResolver().openInputStream(imageUri);
                                dataSize = fileInputStream.available();
                                System.out.println("Image Size: " + dataSize);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if (dataSize < 1024 * 1024) {
                                hasProfileImg = true;
                                uploadImage(usersDataSnapshot);
                            }
                            else
                                info.setText("Image size must be under 1MB");
                        }
                        else {
                            updateUserOnFirebase(usersDataSnapshot);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        }
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
            profileImgChanged = true;
        }
    }

    private void uploadImage(DataSnapshot usersDataSnapshot) {

        progressContainer.setVisibility(View.VISIBLE);

        StorageReference imageReference = storageReference.child("images/" + userLoggedIn.getUsername());

        imageReference.putFile(imageUri)
            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    progressContainer.setVisibility(View.GONE);
                    updateUserOnFirebase(usersDataSnapshot);
                    Toast.makeText(context, "User updated successfully!", Toast.LENGTH_LONG).show();
                }
            });
    }

    private void updateUserOnFirebase(DataSnapshot usersDataSnapshot) {
        String encryptedPassword = "";
        if(passwordChanged) {
            try {
                encryptedPassword = AESEncryption.encrypt(newPassword);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            encryptedPassword = oldPassword;
        }

        int status = firebase.updateUser(newName, username, encryptedPassword, hasProfileImg, usersDataSnapshot);

        switch (status) {
            case 0:
                User userLoggedInUpdated = new User(newName, username, encryptedPassword, hasProfileImg);
                Intent intent = new Intent();
                intent.putExtra("userLoggedInUpdated", userLoggedInUpdated);
                setResult(RESULT_OK, intent);
                finish();
                Toast.makeText(context, "User updated successfully!", Toast.LENGTH_LONG).show();
                break;

            case 1:
                info.setText("Username already exists");
                break;

            case 2:
                Toast.makeText(context, "Error creating user in database", Toast.LENGTH_LONG).show();
                break;
        }
    }
}