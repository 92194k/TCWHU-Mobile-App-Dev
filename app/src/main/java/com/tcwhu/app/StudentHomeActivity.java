package com.tcwhu.app;

import android.content.Intent; // <-- ADDED
import android.os.Bundle;
import android.util.Log; // <-- ADDED
import android.widget.Toast; // <-- ADDED

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth; // <-- ADDED
import com.google.firebase.auth.FirebaseUser; // <-- ADDED
import com.google.firebase.firestore.FirebaseFirestore; // <-- ADDED
import com.google.firebase.firestore.ListenerRegistration; // <-- ADDED
import com.google.firebase.messaging.FirebaseMessaging; // <-- ADDED

public class StudentHomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    // --- ADDED: Firebase variables ---
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration userStatusListener;
    private String currentUserId;
    // ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        // --- ADDED: Initialize Firebase ---
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Safety check: If no user is logged in, kick them to Landing
        if (currentUser == null) {
            forceLogoutAndRedirect("Session expired. Please log in again.");
            return;
        }
        currentUserId = currentUser.getUid();
        // --- END OF ADDED CODE ---


        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Load the default fragment
        replaceFragment(new FindSomeoneFragment());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_find) {
                replaceFragment(new FindSomeoneFragment());
            } else if (itemId == R.id.navigation_chat) {
                replaceFragment(new ChatFragment());
            } else if (itemId == R.id.navigation_events) {
                replaceFragment(new EventsFragment());
            } else if (itemId == R.id.navigation_profile) {
                replaceFragment(new StudentProfileFragment());
            }
            return true;
        });

        // --- ADDED: Start the real-time listener ---
        startUserStatusListener(currentUserId);
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    // --- ADDED: This method watches the user's doc for changes ---
    private void startUserStatusListener(String userId) {
        // Ensure we don't attach multiple listeners
        if (userStatusListener != null) {
            userStatusListener.remove();
        }

        userStatusListener = db.collection("users").document(userId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w("StatusCheck", "Listen failed.", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Log.d("StatusCheck", "User document updated.");
                        // Check the ban/suspend fields from your Firestore document
                        Boolean isBanned = snapshot.getBoolean("isBanned");
                        Boolean isSuspended = snapshot.getBoolean("isSuspended");

                        if (Boolean.TRUE.equals(isBanned)) {
                            Log.w("StatusCheck", "User has been BANNED. Forcing logout.");
                            forceLogoutAndRedirect("Your account has been permanently banned.");
                        } else if (Boolean.TRUE.equals(isSuspended)) {
                            Log.w("StatusCheck", "User has been SUSPENDED. Forcing logout.");
                            forceLogoutAndRedirect("Your account has been temporarily suspended.");
                        }

                    } else {
                        // This handles if an admin DELETES the user's document
                        Log.w("StatusCheck", "User document no longer exists. Forcing logout.");
                        forceLogoutAndRedirect("Your account has been deleted.");
                    }
                });
    }

    // --- ADDED: This method handles the "kick" ---
    private void forceLogoutAndRedirect(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        // Unsubscribe from topics so they don't get notifications after being kicked
        FirebaseMessaging.getInstance().unsubscribeFromTopic("all_users");

        // Sign out from Firebase Auth
        mAuth.signOut();

        // Go to suspended/banned screen (or Landing screen)
        Intent intent = new Intent(StudentHomeActivity.this, AccountSuspendedActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Close this home activity
    }

    // --- ADDED: Clean up the listener when the activity is destroyed ---
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userStatusListener != null) {
            userStatusListener.remove(); // Detach the listener
        }
    }
}