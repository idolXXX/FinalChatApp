package com.example.finalchatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.finalchatapp.adapters.MessageAdapter;
import com.example.finalchatapp.models.Message;
import com.example.finalchatapp.models.User;
import com.example.finalchatapp.services.NotificationService;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private CircleImageView profileImage;
    private TextView usernameText;
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private String chatId;
    private String otherUserId;
    private User otherUser;

    private List<Message> messageList;
    private MessageAdapter messageAdapter;

    private Set<String> loadedMessageIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        // Get other user ID from intent
        otherUserId = getIntent().getStringExtra("userId");
        if (otherUserId == null) {
            finish();
            return;
        }

        // Initialize UI elements
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        profileImage = findViewById(R.id.profile_image);
        usernameText = findViewById(R.id.username_text);
        recyclerView = findViewById(R.id.recycler_view);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);

        // Set up RecyclerView
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList, currentUser.getUid());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);


        // Load other user info
        loadUserInfo();

        // Set chat ID (a unique ID for this conversation)
        setChatId();

        // Load messages
        loadMessages();

        // Set up send button
        sendButton.setOnClickListener(v -> sendMessage());

        // Set up back button
        toolbar.setNavigationOnClickListener(v -> finish());

        // Clear the loaded message IDs
        loadedMessageIds = new HashSet<>();

        // Clear any notifications for this chat
        if (otherUserId != null) {
            NotificationService.clearNotification(this, otherUserId);
        }
    }

    private void loadUserInfo() {
        db.collection("users").document(otherUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        otherUser = documentSnapshot.toObject(User.class);
                        if (otherUser != null) {
                            Log.d("ChatActivity", "Successfully loaded user: " + otherUser.getUsername());
                            usernameText.setText(otherUser.getUsername());
                            // You can load profile image using Glide here if user has a profile image
                        } else {
                            Log.e("ChatActivity", "Failed to convert document to User object");
                            Toast.makeText(ChatActivity.this, "Error parsing user data", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("ChatActivity", "User document does not exist for ID: " + otherUserId);
                        // Create a placeholder user if it doesn't exist
                        createPlaceholderUser();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatActivity", "Error loading user info: " + e.getMessage(), e);
                    Toast.makeText(ChatActivity.this, "Error loading user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Create a placeholder user on failure
                    createPlaceholderUser();
                });
    }
    private void createPlaceholderUser() {
        // Create a temporary user object as a fallback
        otherUser = new User(otherUserId, "Unknown User", "");
        usernameText.setText("Unknown User");

        // Optionally save this placeholder to Firestore
        // This can help if the user document is missing
        db.collection("users").document(otherUserId)
                .set(otherUser)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ChatActivity", "Created placeholder user");
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatActivity", "Failed to create placeholder user", e);
                });
    }

    private void setChatId() {
        // Create a unique chat ID based on both users' IDs
        if (currentUser.getUid().compareTo(otherUserId) < 0) {
            chatId = currentUser.getUid() + "_" + otherUserId;
        } else {
            chatId = otherUserId + "_" + currentUser.getUid();
        }

        Log.d("ChatActivity", "Chat ID set to: " + chatId);
    }

    private void loadMessages() {
        db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Toast.makeText(ChatActivity.this, "Error loading messages", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (snapshots != null) {
                            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                if (dc.getType() == DocumentChange.Type.ADDED) {
                                    Message message = dc.getDocument().toObject(Message.class);

                                    // Only add the message if we haven't seen its ID before
                                    if (!loadedMessageIds.contains(message.getMessageId())) {
                                        loadedMessageIds.add(message.getMessageId());
                                        messageList.add(message);
                                    }
                                }
                            }
                            messageAdapter.notifyDataSetChanged();
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    private void sendMessage() {
        String content = messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            return;
        }

        // Disable send button to prevent double-sends
        sendButton.setEnabled(false);

        // Create a new message
        String messageId = UUID.randomUUID().toString();
        Message message = new Message(messageId, currentUser.getUid(), otherUserId, content);

        // Add to loaded IDs set to prevent duplication when Firestore listener fires
        loadedMessageIds.add(messageId);

        // Add message to list right away for immediate feedback
        messageList.add(message);
        messageAdapter.notifyDataSetChanged();
        recyclerView.scrollToPosition(messageList.size() - 1);

        // Clear input
        messageInput.setText("");

        // Save message to Firestore
        db.collection("chats").document(chatId).collection("messages").document(messageId)
                .set(message)
                .addOnSuccessListener(aVoid -> {
                    sendButton.setEnabled(true);
                    // Update chat info in both users' chats collection
                    updateChatInfo(message);
                })
                .addOnFailureListener(e -> {
                    sendButton.setEnabled(true);
                    // If saving fails, remove the message from the list
                    messageList.remove(message);
                    loadedMessageIds.remove(messageId);
                    messageAdapter.notifyDataSetChanged();
                    Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateChatInfo(Message message) {
        // Create the chat info map
        Map<String, Object> chatInfo = new HashMap<>();
        chatInfo.put("lastMessageContent", message.getContent());
        chatInfo.put("lastMessageTimestamp", message.getTimestamp());
        chatInfo.put("lastMessageSenderId", message.getSenderId());

        // Log for debugging
        Log.d("ChatActivity", "Updating chat info - Current User: " + currentUser.getUid()
                + ", Other User: " + otherUserId + ", Message: " + message.getContent());

        // Update chat info for current user
        db.collection("users").document(currentUser.getUid())
                .collection("chats").document(otherUserId)
                .set(chatInfo)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ChatActivity", "Chat info updated for current user");
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatActivity", "Failed to update chat info for current user", e);
                    Toast.makeText(ChatActivity.this, "Failed to update chat info", Toast.LENGTH_SHORT).show();
                });

        // Update chat info for other user
        db.collection("users").document(otherUserId)
                .collection("chats").document(currentUser.getUid())
                .set(chatInfo)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ChatActivity", "Chat info updated for other user");
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatActivity", "Failed to update chat info for other user", e);
                    Toast.makeText(ChatActivity.this, "Failed to update chat info for receiver", Toast.LENGTH_SHORT).show();
                });
    }
}