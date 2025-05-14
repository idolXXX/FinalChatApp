package com.example.finalchatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.example.finalchatapp.fragments.ChatsFragment;
import com.example.finalchatapp.fragments.SettingsFragment;
import com.example.finalchatapp.fragments.UsersFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;
    private FirebaseAuth mAuth;

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
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
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
            }
        });

        // Set default fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ChatsFragment())
                .commit();
        toolbar.setTitle("Chats");
    }
}