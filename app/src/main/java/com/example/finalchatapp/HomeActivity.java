package com.example.finalchatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import com.example.finalchatapp.fragments.ChatsFragment;
import com.example.finalchatapp.fragments.SettingsFragment;
import com.example.finalchatapp.fragments.UsersFragment;
import com.example.finalchatapp.models.Message;
import com.example.finalchatapp.models.User;
import com.example.finalchatapp.services.NotificationService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;
    private FirebaseAuth mAuth;
    Button testButton;
    FirebaseUser currentUser;
    FirebaseFirestore db;
    ProgressDialog progressDialog;
    User     currentUserObj;
    Map<String, Object> chatData;
    Message message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is signed in
        if (mAuth.getCurrentUser() == null) {
            // Not signed in, return to MainActivity
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Set up toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up bottom navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_chats) {
                selectedFragment = new ChatsFragment();
                toolbar.setTitle("Chats");
            } else if (itemId == R.id.nav_users) {
                selectedFragment = new UsersFragment();
                toolbar.setTitle("Users");
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
                toolbar.setTitle("Settings");
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });

        // Set default fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ChatsFragment())
                .commit();
        toolbar.setTitle("Chats");

        // Add test chat button
//        addTestUserButton();

        // Schedule notifications
        NotificationService.scheduleNotifications(this);
    }




//    private void handleError(Exception e, ProgressDialog progressDialog) {
//        Log.e("InitDB", "Error initializing database", e);
//        progressDialog.dismiss();
//        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//    }
//    private void addTestUserButton() {
//        Button testButton = findViewById(R.id.test_chat_button);
//
//        // Use the same fixed test user ID
//        String testUserId = "testuser123";
//
//        testButton.setOnClickListener(v -> {
//            // Start chat activity with this user
//            Intent intent = new Intent(HomeActivity.this, ChatActivity.class);
//            intent.putExtra("userId", testUserId);
//            startActivity(intent);
//        });
//    }
}