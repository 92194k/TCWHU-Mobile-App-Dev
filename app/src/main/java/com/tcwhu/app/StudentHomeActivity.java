package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.HashMap;
import java.util.Map;

public class StudentHomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration userStatusListener;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            forceLogoutAndRedirect("Session expired. Please log in again.");
            return;
        }
        currentUserId = currentUser.getUid();

        fetchAndSaveFCMToken(currentUserId);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        replaceFragment(new ChatFragment());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_find) selectedFragment = new FindSomeoneFragment();
            else if (itemId == R.id.navigation_chat) selectedFragment = new ChatFragment();
            else if (itemId == R.id.navigation_events) selectedFragment = new EventsFragment();
            else if (itemId == R.id.navigation_profile) selectedFragment = new StudentProfileFragment();

            if (selectedFragment != null) {
                replaceFragment(selectedFragment);
                return true;
            }
            return false;
        });

        startUserStatusListener(currentUserId);
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    private void fetchAndSaveFCMToken(String userId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) return;

                    String token = task.getResult();
                    if (token != null) {
                        Map<String, Object> tokenUpdate = new HashMap<>();
                        tokenUpdate.put("notificationToken", token);
                        db.collection("users").document(userId).update(tokenUpdate);
                    }
                });
    }

    private void startUserStatusListener(String userId) {
        if (userStatusListener != null) userStatusListener.remove();

        userStatusListener = db.collection("users").document(userId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) return;

                    if (snapshot != null && snapshot.exists()) {
                        Student student = snapshot.toObject(Student.class);
                        if (student == null) return;

                        if (student.isBanned()) {
                            forceLogoutAndRedirect("Your account has been permanently banned.");
                        } else if (student.isSuspended()) {
                            if (System.currentTimeMillis() < student.getSuspendEndDate()) {
                                forceLogoutAndRedirect("Your account has been temporarily suspended.");
                            }
                        }
                    } else {
                        forceLogoutAndRedirect("Your account has been deleted.");
                    }
                });
    }

    private void forceLogoutAndRedirect(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        FirebaseMessaging.getInstance().unsubscribeFromTopic("all_users");
        mAuth.signOut();

        Intent intent = new Intent(this, AccountSuspendedActivity.class);
        intent.putExtra("STATUS_MESSAGE", message);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userStatusListener != null) userStatusListener.remove();
    }
}