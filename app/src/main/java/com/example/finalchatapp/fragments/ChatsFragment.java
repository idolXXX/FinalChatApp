package com.example.finalchatapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalchatapp.R;
import com.example.finalchatapp.adapters.ChatsAdapter;
import com.example.finalchatapp.models.ChatPreview;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private ChatsAdapter adapter;
    private List<ChatPreview> chatPreviews;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

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

        // Set up RecyclerView
        chatPreviews = new ArrayList<>();
        adapter = new ChatsAdapter(getContext(), chatPreviews);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Load chats
        loadChats();

        return view;
    }

    private void loadChats() {
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid())
                .collection("chats")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    chatPreviews.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                        return;
                    }

                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String userId = document.getId();
                        String lastMessage = document.getString("lastMessageContent");
                        Long timestamp = document.getLong("lastMessageTimestamp");
                        String senderId = document.getString("lastMessageSenderId");

                        if (lastMessage != null && timestamp != null && senderId != null) {
                            ChatPreview chatPreview = new ChatPreview(userId, lastMessage, timestamp, senderId);
                            chatPreviews.add(chatPreview);
                        }
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload chats when fragment becomes visible again
        if (currentUser != null) {
            loadChats();
        }
    }
}