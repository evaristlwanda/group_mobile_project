package com.example.myapplication;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class HomeActivity extends AppCompatActivity {

    private static final String HOME_FRAGMENT_TAG = "home_fragment";
    private static final String PROFILE_FRAGMENT_TAG = "profile_fragment";

    private static final String CHAT_FRAGMENT_TAG = "chat_fragment"; // Add the chat fragment tag

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        // Load default fragment only if it's the first time
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), HOME_FRAGMENT_TAG);
        }
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener =
            item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    loadFragment(new HomeFragment(), HOME_FRAGMENT_TAG);
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    loadFragment(new ProfileFragment(), PROFILE_FRAGMENT_TAG);
                    return true;
                }
                else if (itemId == R.id.nav_chat) { // Add the chat navigation
                    loadFragment(new ChatFragment(), CHAT_FRAGMENT_TAG);
                    return true;
                }
                return false;
            };

    private void loadFragment(Fragment newFragment, String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Check if the fragment is already added
        Fragment existingFragment = fragmentManager.findFragmentByTag(tag);

        if (existingFragment == null) {
            // If not, add the new fragment
            transaction.replace(R.id.fragment_container, newFragment, tag);
        } else {
            // If it exists, just show it
            transaction.replace(R.id.fragment_container, existingFragment, tag);
        }

        transaction.commit();
    }
}