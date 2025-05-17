package com.example.finalchatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.finalchatapp.fragments.LoginFragment;
import com.example.finalchatapp.services.NotificationService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class MainActivity extends AppCompatActivity {


    private FirebaseAuth mAuth;
    private static final int NOTIFICATION_PERMISSION_CODE = 100;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        requestNotificationPermission();

        if (currentUser != null) {
            // User is already signed in, go to Home Activity
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);

            // Schedule notifications
            NotificationService.scheduleNotifications(this);

            finish();
        } else {
            // User is not signed in, show Login Fragment
            loadLoginFragment();
        }
        if (getIntent().getBooleanExtra("openChat", false)) {
            String chatUserId = getIntent().getStringExtra("chatUserId");
            if (chatUserId != null && mAuth.getCurrentUser() != null) {
                // Clear the notification
                NotificationService.clearNotification(this, chatUserId);

                // Open the chat activity
                Intent chatIntent = new Intent(this, ChatActivity.class);
                chatIntent.putExtra("userId", chatUserId);
                startActivity(chatIntent);
                finish();
                return;
            }
            }

    }
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {

                Log.d("MainActivity", "⚠️ Requesting notification permission");
                ActivityCompat.requestPermissions(
                        this,
                        new String[] { android.Manifest.permission.POST_NOTIFICATIONS },
                        NOTIFICATION_PERMISSION_CODE);
            } else {
                Log.d("MainActivity", "⚠️ Notification permission already granted");
            }
        } else {
            Log.d("MainActivity", "⚠️ Notification permission not needed for this Android version");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "⚠️ Notification permission granted");
                // You can call methods that depend on this permission here
            } else {
                Log.d("MainActivity", "⚠️ Notification permission denied");
                Toast.makeText(this, "Notification permission is needed for chat notifications", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if user is signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, go to Home Activity
            startActivity(new Intent(this, HomeActivity.class));

            // Schedule notifications
            NotificationService.scheduleNotifications(this);

            finish();
        } else {
            // User is not signed in, show Login Fragment
            loadLoginFragment();
        }
    }

    private void loadLoginFragment() {
        Fragment loginFragment = new LoginFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.auth_container, loginFragment);
        transaction.commit();
    }
}