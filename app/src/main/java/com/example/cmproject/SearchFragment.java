package com.example.cmproject;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class SearchFragment extends Fragment {

    private ArrayList<User> filteredUsers;
    private Firebase firebase = new Firebase();
    private ArrayList<User> usersList;

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // Carregar utilizadores da Firebase
        loadUsers();

        EditText searchEditText = view.findViewById(R.id.search_edit_text);
        ListView listView = view.findViewById(R.id.profile_list_view);
        MyViewModel viewModel = new ViewModelProvider(getActivity()).get(MyViewModel.class);

        // Listener para quando o texto na barra de pesquisa é alterado
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence searchText, int i, int i1, int i2) {
                filteredUsers = new ArrayList<>();

                // Filtrar os utilizadores
                if(usersList != null && !searchText.toString().equals("")) {
                    for (User user : usersList) {
                        if (user.getUsername().contains(searchText)) {
                            filteredUsers.add(user);
                        }
                    }
                }

                SearchList searchList = new SearchList(getActivity(), filteredUsers);
                listView.setAdapter(searchList);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // Abrir o perfil do utilizador quando o utilizador clica em cima de algum
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                viewModel.setUserForProfile(filteredUsers.get(position));
                viewModel.setPage("Search");
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.home_fragment_view, ProfileFragment.class, null)
                        .addToBackStack(null)
                        .commit();
            }
        });

        return view;
    }

    private void loadUsers() {

        // Ir buscar todos os utilizadores à Firebase
        FirebaseDatabase root = FirebaseDatabase.getInstance("https://cm-project-16f97-default-rtdb.europe-west1.firebasedatabase.app");
        DatabaseReference userReference = root.getReference("Users");
        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                usersList = firebase.getAllUsers(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}