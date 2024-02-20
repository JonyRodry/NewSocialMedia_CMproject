package com.example.cmproject;

import static android.app.Activity.RESULT_OK;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private static final int REQ_CODE = 124;

    private Firebase firebase = new Firebase();
    private MyViewModel viewModel;
    private ListView listView;
    private Database db;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = new Database(getActivity());
        viewModel = new ViewModelProvider(getActivity()).get(MyViewModel.class);

        FloatingActionButton createPostButton = view.findViewById(R.id.create_post_button);
        listView = view.findViewById(R.id.feed_list_view);

        // Redirecionar o utilizador para a atividade para criar uma nova publicação
        createPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), CreatePostActivity.class);
                intent.putExtra("userLoggedIn", viewModel.getUserLoggedIn());
                startActivityForResult(intent, REQ_CODE);
            }
        });

        // Se houver acesso à internet, carrega os posts da Firebase
        if(Network.isNetworkAvailable(getActivity())) {
            loadFeedPosts();
            ((HomeActivity) getActivity()).loadNotificationService();
        }
        // Caso contrário carrega da base de dados local
        else {
            loadPostsFromLocalStorage();
        }

        return view;
    }

    private void loadFeedPosts() {

        String follower = viewModel.getUserLoggedIn().getUsername();
        FirebaseDatabase root = FirebaseDatabase.getInstance("https://cm-project-16f97-default-rtdb.europe-west1.firebasedatabase.app");
        DatabaseReference followingReference = root.getReference("Following/"+follower);
        DatabaseReference usersReference = root.getReference("Users");

        // Ir buscar os posts dos following users
        followingReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<String> followingUsersList = firebase.getFollowingUsers(snapshot);

                // Ir buscar os posts do feed à Firebase
                DatabaseReference postsReference = root.getReference("Posts");
                postsReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        // Ir buscar os utilizadores à Firebase
                        usersReference.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot usersDataSnapshot) {

                                ArrayList<User> usersList = new ArrayList<>();
                                for(DataSnapshot ds : usersDataSnapshot.getChildren()) {
                                    usersList.add(ds.getValue(User.class));
                                }
                                ArrayList<Post> postsArrayList = firebase.getFeedPosts(followingUsersList, usersList, dataSnapshot);
                                if(getActivity() != null) {
                                    PostList postsList = new PostList(getActivity(), postsArrayList, viewModel, true);
                                    listView.setAdapter(postsList);
                                }
                                db.insertPosts(postsArrayList);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadPostsFromLocalStorage() {
        ArrayList<Post> postsArrayList = db.getPosts();
        System.out.println(postsArrayList.toString());
        PostList postsList = new PostList(getActivity(), postsArrayList, viewModel, false);
        listView.setAdapter(postsList);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == REQ_CODE) {

            // Voltar a carregar os posts
            loadFeedPosts();
        }
    }
}