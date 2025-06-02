package com.example.finalchatapp.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalchatapp.R;
import com.example.finalchatapp.adapters.ChatsAdapter;
import com.example.finalchatapp.models.ChatPreview;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private ProgressBar progressBar;
    private ChatsAdapter adapter;
    private List<ChatPreview> chatPreviews;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private static final String TAG = "ChatsFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize UI elements
        recyclerView = view.findViewById(R.id.chats_recycler_view);
        emptyView = view.findViewById(R.id.empty_view);
        progressBar = view.findViewById(R.id.progress_bar); // Add this to your layout

        if (currentUser != null) {
            Log.d(TAG, "User is authenticated: " + currentUser.getUid());
        } else {
            Log.d(TAG, "User is NOT authenticated");
        }

        // Set up RecyclerView
        chatPreviews = new ArrayList<>();
        adapter = new ChatsAdapter(getContext(), chatPreviews);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Initial UI state
        showLoading();

        // Load chats
        loadChats();

        return view;
    }

    private void loadChats() {
        if (currentUser == null) {
            showEmpty("You are not logged in");
            return;
        }

        Log.d(TAG, "Loading chats for user: " + currentUser.getUid());

        db.collection("users").document(currentUser.getUid())
                .collection("chats")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    chatPreviews.clear();

                    Log.d(TAG, "Chats query returned " + queryDocumentSnapshots.size() + " documents");

                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmpty("No conversations yet");
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            String userId = document.getId();
                            String lastMessage = document.getString("lastMessageContent");
                            Long timestamp = document.getLong("lastMessageTimestamp");
                            String senderId = document.getString("lastMessageSenderId");

                            Log.d(TAG, "Found chat with user: " + userId + ", last message: " + lastMessage);

                            if (lastMessage != null && timestamp != null && senderId != null) {
                                ChatPreview chatPreview = new ChatPreview(userId, lastMessage, timestamp, senderId);
                                chatPreviews.add(chatPreview);
                            } else {
                                Log.w(TAG, "Missing data for chat with user: " + userId);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing chat document: " + document.getId(), e);
                        }
                    }

                    Log.d(TAG, "Added " + chatPreviews.size() + " chats to the list");

                    if (chatPreviews.isEmpty()) {
                        showEmpty("No valid conversations found");
                    } else {
                        showChats();
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading chats", e);
                    showError("Error loading chats: " + e.getMessage());
                });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
    }

    private void showChats() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    private void showEmpty(String message) {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        emptyView.setText(message);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        showEmpty("Error: " + message);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (isAdded()) {
            showLoading();
            loadChats();
        }
    }
}