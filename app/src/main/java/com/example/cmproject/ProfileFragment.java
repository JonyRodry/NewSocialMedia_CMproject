package com.example.cmproject;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class ProfileFragment extends Fragment {

    private static final int REQ_CODE = 123;

    private FirebaseDatabase root = FirebaseDatabase.getInstance("https://cm-project-16f97-default-rtdb.europe-west1.firebasedatabase.app");
    private Firebase firebase = new Firebase();
    private FirebaseStorage storage;
    private StorageReference imageRef;
    private MyViewModel viewModel;
    private ImageView profileImg;
    private ListView listView;
    private Button followButton, unfollowButton, logoutButton;
    private MQTTHelper mqttHelper;
    private Toolbar toolbar;
    private Database db;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        db = new Database(getActivity());
        viewModel = new ViewModelProvider(getActivity()).get(MyViewModel.class);

        profileImg = view.findViewById(R.id.profile_image);
        listView = view.findViewById(R.id.profile_list_view);
        followButton = view.findViewById(R.id.follow_button);
        unfollowButton = view.findViewById(R.id.unfollow_button);
        logoutButton = view.findViewById(R.id.logout_button);

        storage = FirebaseStorage.getInstance();
        mqttHelper = new MQTTHelper(getContext(), viewModel.getUserLoggedIn().getUsername());
        mqttHelper.connect();

        toolbar = view.findViewById(R.id.profile_toolbar);

        // Se for o perfil do próprio utilizador remover o botão de Follow e mostrar o botão de Logout
        if(viewModel.getUserForProfile().getUsername().equals(viewModel.getUserLoggedIn().getUsername())) {
            toolbar.inflateMenu(R.menu.profile_toolbar);
            followButton.setVisibility(View.INVISIBLE);
            logoutButton.setVisibility(View.VISIBLE);
        }

        // Caso contrário carrega o perfil normalmente
        else {
            String follower = viewModel.getUserLoggedIn().getUsername();
            String following = viewModel.getUserForProfile().getUsername();
            DatabaseReference followingReference = root.getReference("Following/"+follower);

            if(Network.isNetworkAvailable(getActivity())) {
                followingReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean isFollower = firebase.checkIfFollows(following, snapshot);

                        // Se seguir o utilizador coloca o botão de Unfollow, caso contrário carrega o de Follow
                        if (isFollower) {
                            followButton.setVisibility(View.INVISIBLE);
                            unfollowButton.setVisibility(View.VISIBLE);
                        } else {
                            unfollowButton.setVisibility(View.INVISIBLE);
                            followButton.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        }
        if(viewModel.getPage().equals("Search"))
            toolbar.setNavigationIcon(R.drawable.ic_back);

        // Definir o nome do utilizador na página de perfil
        toolbar.setTitle(viewModel.getUserForProfile().getName());

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().onBackPressed();
            }
        });

        // Quando o utilizador clicar no botão para editar perfil
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(getContext(), EditProfileActivity.class);
                intent.putExtra("userLoggedIn", viewModel.getUserLoggedIn());
                startActivityForResult(intent, REQ_CODE);
                return false;
            }
        });

        // Se o utilizador tiver foto de perfil, esta é carregada
        if(Network.isNetworkAvailable(getActivity()) && viewModel.getUserForProfile().isHasProfileImg()) {
            imageRef = storage.getReference().child("images/" + viewModel.getUserForProfile().getUsername());
            imageRef.getBytes(1024 * 1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    profileImg.setImageBitmap(bitmap);
                }
            });
        }
        // Se não houver internet é carregada da cache
        else if(!Network.isNetworkAvailable(getActivity()) && viewModel.getUserForProfile().isHasProfileImg()) {
            Bitmap bitmap = viewModel.getBitmapFromMemCache(viewModel.getUserForProfile().getUsername());
            if(bitmap != null)
                profileImg.setImageBitmap(bitmap);
        }

        // Listener para quando o utilizador clicar no botão de Follow
        followButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                followButton.setVisibility(View.INVISIBLE);
                unfollowButton.setVisibility(View.VISIBLE);

                String follower = viewModel.getUserLoggedIn().getUsername();
                String following = viewModel.getUserForProfile().getUsername();
                firebase.setFollowing(follower, following);

                mqttHelper.subscribeToTopic("cm/notifications/"+following);
            }
        });

        // Listener para quando o utilizador clicar no botão de Unfollow
        unfollowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unfollowButton.setVisibility(View.INVISIBLE);
                followButton.setVisibility(View.VISIBLE);

                String follower = viewModel.getUserLoggedIn().getUsername();
                String following = viewModel.getUserForProfile().getUsername();
                firebase.removeFollowing(follower, following);

                mqttHelper.unsubscribeFromTopic("cm/notifications/"+following);
            }
        });

        // Carrega os posts da internet
        if(Network.isNetworkAvailable(getActivity()))
            loadPosts();

        // Carrega os posts da SQLite
        else {
            if(viewModel.getUserForProfile().getUsername().equals(viewModel.getUserLoggedIn().getUsername()))
                loadPostsFromLocalStorage();
        }

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((HomeActivity)getActivity()).logout();
            }
        });

        return view;
    }

    private void loadPosts() {

        // Ir buscar todos os posts do perfil à Firebase
        DatabaseReference postsReference = root.getReference("Posts");
        DatabaseReference usersReference = root.getReference("Users");

        postsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot postsDataSnapshot) {

                usersReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot usersDataSnapshot) {
                        ArrayList<User> usersList = new ArrayList<>();
                        for(DataSnapshot ds : usersDataSnapshot.getChildren()) {
                            usersList.add(ds.getValue(User.class));
                        }

                        // Carrega os posts referentes ao perfil
                        ArrayList<Post> postsArrayList = firebase.getProfilePosts(viewModel.getUserForProfile().getUsername(), postsDataSnapshot, usersList);
                        if(getActivity() != null) {
                            PostList postsList = new PostList(getActivity(), postsArrayList, viewModel, true);
                            listView.setAdapter(postsList);
                        }
                        db.insertProfilePosts(postsArrayList);
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

    private void loadPostsFromLocalStorage() {
        ArrayList<Post> postsArrayList = db.getProfilePosts();
        PostList postsList = new PostList(getActivity(), postsArrayList, viewModel, false);
        listView.setAdapter(postsList);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Se o utilizador tiver alterado os seus dados no EditProfileActivity estes são atualizados
        if(resultCode == RESULT_OK && requestCode == REQ_CODE) {
            User userLoggedIn = (User) data.getSerializableExtra("userLoggedInUpdated");
            viewModel.setUserLoggedIn(userLoggedIn);
            viewModel.setUserForProfile(userLoggedIn);

            toolbar.setTitle(userLoggedIn.getName());

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

            // Voltar a carregar os posts
            loadPosts();
        }
    }
}