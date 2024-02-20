package com.example.cmproject;


import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


// Esta classe contém métodos auxiliares de acesso à Firebase.

public class Firebase {

    private FirebaseDatabase root = FirebaseDatabase.getInstance("https://cm-project-16f97-default-rtdb.europe-west1.firebasedatabase.app");

    public int createUser(String name, String username, String password, boolean hasProfileImg, DataSnapshot dataSnapshot) {

        if(checkIfUsernameExists(username, dataSnapshot))
            return 1;

        else {
            DatabaseReference usersReference = root.getReference("Users");
            DatabaseReference lastUserIDReference = root.getReference("LastUserID");

            User newUser = new User(name, username, password, hasProfileImg);

            lastUserIDReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    int lastUserID = Math.toIntExact((Long) snapshot.getValue());
                    int newUserID = lastUserID + 1;
                    usersReference.child(Integer.toString(newUserID)).setValue(newUser);
                    snapshot.getRef().setValue(newUserID);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

            return 0;
        }
    }

    public boolean checkIfUsernameExists(String username, DataSnapshot dataSnapshot) {

        for(DataSnapshot ds : dataSnapshot.getChildren()) {
            User user = ds.getValue(User.class);
            if(user.getUsername().equals(username))
                return true;
        }

        return false;
    }

    public User login(String username, String password, DataSnapshot dataSnapshot) {

        for(DataSnapshot ds : dataSnapshot.getChildren()) {
            User user = ds.getValue(User.class);

            if(user.getUsername().equals(username) && user.getPassword().equals(password)) {
                return user;
            }
        }

        return null;
    }

    public User getUser(String username, DataSnapshot dataSnapshot) {

        for(DataSnapshot ds : dataSnapshot.getChildren()) {
            User user = ds.getValue(User.class);

            if(user.getUsername().equals(username))
                return user;
        }
        return null;
    }

    public ArrayList<User> getAllUsers(DataSnapshot dataSnapshot) {

        ArrayList<User> usersList = new ArrayList();
        for(DataSnapshot ds : dataSnapshot.getChildren()) {
            User user = ds.getValue(User.class);

            usersList.add(user);
        }

        return usersList;
    }

    public void createPost(String username, String text) {
        DatabaseReference usersReference = root.getReference("Posts");
        DatabaseReference lastUserIDReference = root.getReference("LastPostID");

        PostAux post = new PostAux(username, text);

        lastUserIDReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int lastPostID = Math.toIntExact((Long) snapshot.getValue());
                int newPostID = lastPostID + 1;
                usersReference.child(Integer.toString(newPostID)).setValue(post);
                snapshot.getRef().setValue(newPostID);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public ArrayList<Post> getProfilePosts(String username, DataSnapshot postsDataSnapshot, ArrayList<User> userArrayList) {

        ArrayList<Post> postsList = new ArrayList();

        for(DataSnapshot ds : postsDataSnapshot.getChildren()) {
            PostAux postAux = ds.getValue(PostAux.class);

            for(User user : userArrayList) {
                if(postAux.getUsername().equals(username) && postAux.getUsername().equals(user.getUsername())) {
                    postsList.add(new Post(user, postAux.getText()));
                }
            }
        }

        return postsList;
    }

    public void setFollowing(String follower, String following) {
        FirebaseDatabase root = FirebaseDatabase.getInstance("https://cm-project-16f97-default-rtdb.europe-west1.firebasedatabase.app");
        DatabaseReference followingReference = root.getReference("Following/"+follower);
        followingReference.child(following).setValue(following);
    }

    public void removeFollowing(String follower, String following) {
        FirebaseDatabase root = FirebaseDatabase.getInstance("https://cm-project-16f97-default-rtdb.europe-west1.firebasedatabase.app");
        DatabaseReference followingReference = root.getReference("Following/"+follower);
        followingReference.child(following).removeValue();
    }

    public boolean checkIfFollows(String following, DataSnapshot dataSnapshot) {

        for(DataSnapshot ds : dataSnapshot.getChildren()) {
            if(ds.getValue().toString().equals(following))
                return true;
        }

        return false;
    }

    public ArrayList<String> getFollowingUsers(DataSnapshot dataSnapshot) {

        ArrayList<String> followingUsersList = new ArrayList<>();

        for(DataSnapshot ds : dataSnapshot.getChildren()) {
            followingUsersList.add(ds.getValue().toString());
        }

        return followingUsersList;
    }

    public ArrayList<Post> getFeedPosts(ArrayList<String> following, ArrayList<User> usersList, DataSnapshot dataSnapshot) {

        ArrayList<Post> feedPostsList = new ArrayList<>();

        for(DataSnapshot ds : dataSnapshot.getChildren()) {
            PostAux postAux = ds.getValue(PostAux.class);

            if(following.contains(postAux.getUsername())) {

                for(User user : usersList) {

                    if(postAux.getUsername().equals(user.getUsername())) {
                        feedPostsList.add(new Post(user, postAux.getText()));
                    }
                }
            }
        }



        return feedPostsList;
    }

    public int updateUser(String newName, String username, String newPassword, boolean hasProfileImg, DataSnapshot dataSnapshot) {

        DatabaseReference usersReference = root.getReference("Users");
        User updatedUser = new User(newName, username, newPassword, hasProfileImg);

        for(DataSnapshot ds : dataSnapshot.getChildren()) {

            User user = ds.getValue(User.class);
            if(user.getUsername().equals(username)) {
                String userID = ds.getKey();
                usersReference.child(userID).setValue(updatedUser);
            }
        }

        return 0;

    }
}

