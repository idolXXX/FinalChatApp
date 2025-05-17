package com.example.finalchatapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.example.finalchatapp.fragments.ChatsFragment;
import com.example.finalchatapp.fragments.SettingsFragment;
import com.example.finalchatapp.fragments.UsersFragment;
import com.example.finalchatapp.services.MessageListener;
import com.example.finalchatapp.services.NotificationService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 100;

    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;
    private FirebaseAuth mAuth;
    private Button forceButton;
    private Button testNotificationButton;

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

        // Create notification channel
        NotificationService.createNotificationChannel(this);

        // Request notification permissions
        requestNotificationPermission();

        // Set up the buttons
        setupButtons();

        // Schedule periodic notification checks
        NotificationService.scheduleNotifications(this);

        // Start real-time message listener
        MessageListener.startMessageListeners(this);

        Log.d(TAG, "HomeActivity created, notification services initialized");
    }

    private void setupButtons() {
        // Force Button - Directly shows a chat notification
        forceButton = findViewById(R.id.force_button);
        forceButton.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) return;

            String otherUserId = "3VHEXkMqKMgafJo8QAZs994nZ4v2"; // Replace with actual user ID
            String senderName = "Test User";
            String messageContent = "This is a direct test message from the Force button!";

            // Show notification directly
            NotificationService.showDirectNotification(
                    this,
                    senderName,
                    messageContent,
                    otherUserId
            );

            Log.d(TAG, "Force notification button pressed");
            Toast.makeText(this, "Force notification sent", Toast.LENGTH_SHORT).show();
        });

        // Test Standard Notification Button
        testNotificationButton = findViewById(R.id.test_notification_button);
        testNotificationButton.setOnClickListener(v -> {
            // Show a simple test notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationService.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_chat)
                    .setContentTitle("Test Notification")
                    .setContentText("This is a standard test notification")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setDefaults(NotificationCompat.DEFAULT_ALL);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            try {
                notificationManager.notify(9999, builder.build());
                Toast.makeText(this, "Standard notification sent", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Test notification sent");
            } catch (SecurityException e) {
                Log.e(TAG, "Permission denied for showing notification", e);
                Toast.makeText(this, "Permission denied: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                // Try requesting permission again
                requestNotificationPermission();
            }
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean hasPermission = ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

            Log.d(TAG, "Notification permission status: " + (hasPermission ? "GRANTED" : "DENIED"));

            if (!hasPermission) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted");
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "Notification permission denied");
                Toast.makeText(this, "Notifications won't work without permission", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up MessageListener resources when the activity is destroyed
        // This is important to prevent memory leaks
        MessageListener.stopAllListeners();
    }
}