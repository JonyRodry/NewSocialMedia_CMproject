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

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class SearchList extends ArrayAdapter {

    private ArrayList<User> users;
    private Activity context;

    public SearchList(Activity context, ArrayList<User> users) {
        super(context, R.layout.search_item, users);
        this.context = context;
        this.users = users;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View row = convertView;
        LayoutInflater inflater = context.getLayoutInflater();
        if (convertView == null)
            row = inflater.inflate(R.layout.search_item, null, true);

        ImageView profileImg = row.findViewById(R.id.search_profile_img);
        TextView nameTextView = row.findViewById(R.id.search_name);
        TextView usernameTextView = row.findViewById(R.id.search_username);


        nameTextView.setText(users.get(position).getName());
        usernameTextView.setText("@" + users.get(position).getUsername());

        if (users.get(position).isHasProfileImg()) {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference imageRef = storage.getReference().child("images/" + users.get(position).getUsername());
            imageRef.getBytes(1024 * 1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    profileImg.setImageBitmap(bitmap);
                }
            });
        }

        return row;
    }
}
