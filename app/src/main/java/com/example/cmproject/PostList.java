package com.example.cmproject;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.collection.LruCache;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.sql.SQLOutput;
import java.util.ArrayList;

public class PostList extends ArrayAdapter {

    private ArrayList<Post> postsArrayList;
    private Activity context;
    private MyViewModel viewModel;
    private boolean isNetworkAvailable;

    public PostList(Activity context, ArrayList<Post> postsArrayList, MyViewModel viewModel, boolean isNetworkAvailable) {
        super(context, R.layout.post, postsArrayList);
        this.context = context;
        this.postsArrayList = postsArrayList;
        this.viewModel = viewModel;
        this.isNetworkAvailable = isNetworkAvailable;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View row=convertView;
        LayoutInflater inflater = context.getLayoutInflater();
        if(convertView==null)
            row = inflater.inflate(R.layout.post, null, true);

        ImageView profileImg = row.findViewById(R.id.post_image);
        TextView nameTextView = row.findViewById(R.id.post_name);
        TextView postTextView = row.findViewById(R.id.post_text);

        // Definir o nome e o conteudo da publicação
        nameTextView.setText(postsArrayList.get(position).getUser().getName());
        postTextView.setText(postsArrayList.get(position).getText());

        // Se o utilizador referente à publicação possuir foto de perfil esta é carregada
        if(postsArrayList.get(position).getUser().isHasProfileImg()) {
            FirebaseStorage storage = FirebaseStorage.getInstance();

            // Se houver internet carrega da Firebase
            if(isNetworkAvailable) {
                StorageReference imageRef = storage.getReference().child("images/" + postsArrayList.get(position).getUser().getUsername());
                imageRef.getBytes(1024 * 1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        profileImg.setImageBitmap(bitmap);
                        viewModel.addBitmapToMemoryCache(postsArrayList.get(position).getUser().getUsername(), bitmap);
                    }
                });
            }
            // Caso contrário carrega a foto da cache
            else {
                Bitmap bitmap = viewModel.getBitmapFromMemCache(postsArrayList.get(position).getUser().getUsername());
                profileImg.setImageBitmap(bitmap);
            }
        }

        return  row;
    }
}
