package com.example.finalchatapp.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalchatapp.R;
import com.example.finalchatapp.adapters.UsersAdapter;
import com.example.finalchatapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UsersFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private TextView errorView;
    private ProgressBar progressBar;
    private UsersAdapter adapter;
    private List<User> userList;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private static final String TAG = "UsersFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_users, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize UI elements
        recyclerView = view.findViewById(R.id.users_recycler_view);
        emptyView = view.findViewById(R.id.empty_view);
        errorView = view.findViewById(R.id.error_view);
        progressBar = view.findViewById(R.id.progress_bar);

        
        userList = new ArrayList<>();
        adapter = new UsersAdapter(getContext(), userList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Initial UI state
        showLoading();

        loadUsers();

        return view;
    }

    private void loadUsers() {
        if (currentUser == null) {
            showError("You are not logged in");
            return;
        }

        Log.d(TAG, "Loading users from Firestore");

        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No users found in Firestore");
                        showEmpty();
                        return;
                    }

                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " users in Firestore");

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            User user = document.toObject(User.class);
                            Log.d(TAG, "Parsed user: " + user.getUsername() + " with ID: " + user.getUserId());

                            // Skip current user
                            if (!user.getUserId().equals(currentUser.getUid())) {
                                userList.add(user);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing user document: " + document.getId(), e);
                        }
                    }

                    Log.d(TAG, "Total users added to list: " + userList.size());

                    if (userList.isEmpty()) {
                        showEmpty();
                    } else {
                        showUsers();
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading users", e);
                    showError("Error loading users: " + e.getMessage());
                });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
    }

    private void showUsers() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
    }

    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
        errorView.setText(message);
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded()) {
            showLoading();
            loadUsers();
        }
    }
}