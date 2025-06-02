package com.example.finalchatapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final int STORAGE_PERMISSION_REQUEST = 101;

    private Toolbar toolbar;
    private CircleImageView profileImage;
    private TextView usernameText;
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton attachButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private FirebaseUser currentUser;

    private String chatId;
    private String otherUserId;
    private User otherUser;

    private List<Message> messageList;
    private MessageAdapter messageAdapter;

    private Set<String> loadedMessageIds = new HashSet<>();

    // Image handling variables
    private Uri imageUri = null;
    private String currentPhotoPath = "";

    // Activity result launchers for image picking
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
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
        attachButton = findViewById(R.id.attach_button);

        // Set up RecyclerView
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList, currentUser.getUid());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);

        // Initialize activity result launchers
        initializeActivityResultLaunchers();

        // Load other user info
        loadUserInfo();

        // Set chat ID (a unique ID for this conversation)
        setChatId();

        messageAdapter.setChatId(chatId);

        // Load messages
        loadMessages();

        // Set up send button
        sendButton.setOnClickListener(v -> sendMessage());

        // Set up attach button
        attachButton.setOnClickListener(v -> showImageOptions());

        // Set up back button
        toolbar.setNavigationOnClickListener(v -> finish());

        // Clear the loaded message IDs
        loadedMessageIds = new HashSet<>();

        // Clear any notifications for this chat
        if (otherUserId != null) {
            NotificationService.clearNotification(this, otherUserId);
        }
    }

    private void initializeActivityResultLaunchers() {
        // Gallery launcher
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        uploadImage();
                    }
                });

        // Camera launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && imageUri != null) {
                        uploadImage();
                    }
                });
    }

    private void loadUserInfo() {
        db.collection("users").document(otherUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        otherUser = documentSnapshot.toObject(User.class);
                        if (otherUser != null) {
                            Log.d(TAG, "Successfully loaded user: " + otherUser.getUsername());
                            usernameText.setText(otherUser.getUsername());

                            // Load profile image using Glide
                            if (otherUser.getProfileImageUrl() != null && !otherUser.getProfileImageUrl().isEmpty()) {
                                Glide.with(this)
                                        .load(otherUser.getProfileImageUrl())
                                        .placeholder(R.drawable.default_profile)
                                        .error(R.drawable.default_profile)
                                        .into(profileImage);
                            } else {
                                // Use default profile image
                                profileImage.setImageResource(R.drawable.default_profile);
                            }
                        } else {
                            Log.e(TAG, "Failed to convert document to User object");
                            Toast.makeText(ChatActivity.this, "Error parsing user data", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "User document does not exist for ID: " + otherUserId);
                        // Create a placeholder user if it doesn't exist
                        createPlaceholderUser();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user info: " + e.getMessage(), e);
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
                    Log.d(TAG, "Created placeholder user");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create placeholder user", e);
                });
    }

    private void setChatId() {
        // Create a unique chat ID based on both users' IDs
        if (currentUser.getUid().compareTo(otherUserId) < 0) {
            chatId = currentUser.getUid() + "_" + otherUserId;
        } else {
            chatId = otherUserId + "_" + currentUser.getUid();
        }

        Log.d(TAG, "Chat ID set to: " + chatId);
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
                            scrollToBottom();
                        }
                    }
                });
    }

    // Helper method to scroll to the bottom of the chat
    private void scrollToBottom() {
        if (messageList.size() > 0) {
            recyclerView.scrollToPosition(messageList.size() - 1);
        }
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
        scrollToBottom();

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

    // Show image selection options dialog
    private void showImageOptions() {
        String[] options = {"Take Photo", "Choose from Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Send Image");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Take photo
                checkCameraPermission();
            } else {
                // Choose from gallery
                checkStoragePermission();
            }
        });
        builder.show();
    }

    // Check camera permission
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            openCamera();
        }
    }

    // Check storage permission
    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+, we need READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, STORAGE_PERMISSION_REQUEST);
            } else {
                openGallery();
            }
        } else {
            // For older versions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST);
            } else {
                openGallery();
            }
        }
    }

    // Open camera intent
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Create file for image
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            return;
        }

        // Continue only if file was created
        if (photoFile != null) {
            // Get URI from file using FileProvider for Android 7+
            imageUri = FileProvider.getUriForFile(this,
                    "com.example.finalchatapp.fileprovider", photoFile);

            // Launch camera
            cameraLauncher.launch(imageUri);
        }
    }

    // Create a temporary image file
    private File createImageFile() throws IOException {
        // Create unique filename
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save file path for use with camera intent
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // Open gallery intent
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    // Handle permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission required to take photos", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Storage permission required to select images", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Upload selected image to Firebase Storage
    private void uploadImage() {
        if (imageUri == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();

        // Create unique filename
        String filename = UUID.randomUUID().toString() + "." + getFileExtension(imageUri);

        // Create storage reference
        StorageReference fileRef = storageReference.child("chat_images/" + filename);

        // Upload file
        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Create and send image message
                        sendImageMessage(uri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ChatActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnProgressListener(taskSnapshot -> {
                    // Calculate progress percentage
                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    Log.d(TAG, "Upload progress: " + progress + "%");
                });
    }

    // Get file extension from URI
    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    // Send image message
    private void sendImageMessage(String imageUrl) {
        // Create a new message
        String messageId = UUID.randomUUID().toString();
        Message message = new Message(messageId, currentUser.getUid(), otherUserId, imageUrl, Message.TYPE_IMAGE);

        // Add to loaded IDs set to prevent duplication
        loadedMessageIds.add(messageId);

        // Add message to list for immediate feedback
        messageList.add(message);
        messageAdapter.notifyDataSetChanged();
        scrollToBottom();

        // Save to Firestore
        db.collection("chats").document(chatId).collection("messages").document(messageId)
                .set(message)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Image message sent successfully");
                    // Update chat info
                    updateChatInfo(message);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send image message", e);
                    Toast.makeText(ChatActivity.this, "Failed to send image", Toast.LENGTH_SHORT).show();

                    // Remove from list if failed
                    messageList.remove(message);
                    loadedMessageIds.remove(messageId);
                    messageAdapter.notifyDataSetChanged();
                });
    }

    // Update chat info - enhanced to handle image messages
    private void updateChatInfo(Message message) {
        // Create the chat info map
        Map<String, Object> chatInfo = new HashMap<>();

        // For text messages, use content. For image messages, use a placeholder text
        String lastMessageContent = message.getType() == Message.TYPE_IMAGE ?
                "📷 Image" : message.getContent();

        chatInfo.put("lastMessageContent", lastMessageContent);
        chatInfo.put("lastMessageTimestamp", message.getTimestamp());
        chatInfo.put("lastMessageSenderId", message.getSenderId());

        // Log for debugging
        Log.d(TAG, "Updating chat info - Current User: " + currentUser.getUid()
                + ", Other User: " + otherUserId + ", Message: " + lastMessageContent);

        // Update chat info for current user
        db.collection("users").document(currentUser.getUid())
                .collection("chats").document(otherUserId)
                .set(chatInfo)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Chat info updated for current user");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update chat info for current user", e);
                    Toast.makeText(ChatActivity.this, "Failed to update chat info", Toast.LENGTH_SHORT).show();
                });

        // Update chat info for other user
        db.collection("users").document(otherUserId)
                .collection("chats").document(currentUser.getUid())
                .set(chatInfo)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Chat info updated for other user");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update chat info for other user", e);
                    Toast.makeText(ChatActivity.this, "Failed to update chat info for receiver", Toast.LENGTH_SHORT).show();
                });
    }

}