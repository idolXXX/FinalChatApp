package com.example.finalchatapp.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalchatapp.MainActivity;
import com.example.finalchatapp.R;
import com.example.finalchatapp.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;

    private CircleImageView profileImage;
    private TextView changeProfileText;
    private EditText usernameInput, statusInput;
    private Button saveButton, logoutButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;
    private StorageReference storageRef;

    private Uri imageUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = mAuth.getCurrentUser();
        storageRef = storage.getReference();

        // Initialize UI elements
        profileImage = view.findViewById(R.id.profile_image);
        changeProfileText = view.findViewById(R.id.change_profile_text);
        usernameInput = view.findViewById(R.id.username_input);
        statusInput = view.findViewById(R.id.status_input);
        saveButton = view.findViewById(R.id.save_button);
        logoutButton = view.findViewById(R.id.logout_button);

        // Load user info
        loadUserInfo();

        // Set click listeners
        changeProfileText.setOnClickListener(v -> openImagePicker());
        saveButton.setOnClickListener(v -> saveUserInfo());
        logoutButton.setOnClickListener(v -> logout());

        return view;
    }

    private void loadUserInfo() {
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        usernameInput.setText(user.getUsername());
                        statusInput.setText(user.getStatus());

                        // Load profile image with Glide if available
                        // For now, we'll use the default profile image
                    }
                });
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK
                && data != null && data.getData() != null) {
            imageUri = data.getData();

            // Display the selected image
            profileImage.setImageURI(imageUri);
        }
    }

    private void saveUserInfo() {
        if (currentUser == null) return;

        String username = usernameInput.getText().toString().trim();
        String status = statusInput.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(username)) {
            usernameInput.setError("Username is required");
            return;
        }

        // Disable save button
        saveButton.setEnabled(false);

        // Update user info
        if (imageUri != null) {
            // Upload image first
            StorageReference fileRef = storageRef.child("profile_images/" + currentUser.getUid());
            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Get download URL
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            // Update user info with image URL
                            updateUserInfo(username, status, uri.toString());
                        });
                    })
                    .addOnFailureListener(e -> {
                        saveButton.setEnabled(true);
                        Toast.makeText(getContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Update user info without changing image
            updateUserInfo(username, status, null);
        }
    }

    private void updateUserInfo(String username, String status, String imageUrl) {
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        user.setUsername(username);
                        user.setStatus(status);

                        if (imageUrl != null) {
                            user.setProfileImageUrl(imageUrl);
                        }

                        // Save updated user
                        db.collection("users").document(currentUser.getUid())
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    saveButton.setEnabled(true);
                                    Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    saveButton.setEnabled(true);
                                    Toast.makeText(getContext(), "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                });
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}