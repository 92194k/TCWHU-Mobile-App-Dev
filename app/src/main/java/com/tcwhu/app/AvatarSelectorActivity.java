package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AvatarSelectorActivity extends AppCompatActivity implements AvatarAdapter.OnAvatarListener {

    private Map<String, List<String>> avatarMap;
    private AvatarAdapter avatarAdapter;
    private String selectedAvatar = null;
    private String userId;
    private FirebaseFirestore db;

    private CardView previewCard;
    private TextView previewAvatarText;
    private Button buttonConfirm;

    // NEW: to distinguish who opened this activity
    private boolean isFromProfile = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar_selector);

        db = FirebaseFirestore.getInstance();
        userId = getIntent().getStringExtra("userId");

        // NEW: detect if opened from profile
        isFromProfile = getIntent().getBooleanExtra("fromProfile", false);

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Error: User ID is missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        previewCard = findViewById(R.id.previewCard);
        previewAvatarText = findViewById(R.id.previewAvatarText);
        ChipGroup categoryChipGroup = findViewById(R.id.categoryChipGroup);
        RecyclerView avatarRecyclerView = findViewById(R.id.avatarRecyclerView);
        buttonConfirm = findViewById(R.id.buttonConfirm);

        setupToolbar(toolbar);
        setupAvatarData();
        setupCategoryChips(categoryChipGroup);
        setupRecyclerView(avatarRecyclerView);
        updateAvatarGrid("Smileys & Emotion", categoryChipGroup);

        buttonConfirm.setOnClickListener(v -> {
            if (selectedAvatar != null) {
                if (isFromProfile) {
                    // 👇 NEW: return the avatar result instead of navigating
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("selectedAvatar", selectedAvatar);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    // 👇 Sign-up flow: save avatar to Firestore then go to pending verification
                    saveAvatarToFirestoreAndProceed(selectedAvatar);
                }
            } else {
                Toast.makeText(this, "Please select an avatar first.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveAvatarToFirestoreAndProceed(String avatar) {
        Map<String, Object> avatarUpdate = new HashMap<>();
        avatarUpdate.put("avatar", avatar);

        db.collection("users").document(userId).update(avatarUpdate)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AvatarSelectorActivity.this, "Registration complete.", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(AvatarSelectorActivity.this, PendingVerificationActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save avatar and complete signup.", Toast.LENGTH_LONG).show()
                );
    }

    @Override
    public void onAvatarClick(String avatar) {
        selectedAvatar = avatar;

        int position = -1;
        if (avatarAdapter != null && avatarAdapter.getAvatarList() != null) {
            List<String> currentList = avatarAdapter.getAvatarList();
            for (int i = 0; i < currentList.size(); i++) {
                if (currentList.get(i).equals(avatar)) {
                    position = i;
                    break;
                }
            }
        }

        if (position != -1) {
            avatarAdapter.setSelectedPosition(position);
        }

        previewCard.setVisibility(View.VISIBLE);
        previewAvatarText.setText(avatar);
        buttonConfirm.setEnabled(true);
        buttonConfirm.setText(isFromProfile ? "Save Avatar" : "Confirm Selection");
    }

    private void setupToolbar(MaterialToolbar toolbar) {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        avatarAdapter = new AvatarAdapter(new ArrayList<>(), this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 5));
        recyclerView.setAdapter(avatarAdapter);
    }

    private void setupCategoryChips(ChipGroup chipGroup) {
        for (String category : avatarMap.keySet()) {
            Chip chip = new Chip(this);
            chip.setText(category);
            chip.setCheckable(true);
            chip.setOnClickListener(v -> updateAvatarGrid(category, chipGroup));
            chipGroup.addView(chip);
        }
    }

    private void updateAvatarGrid(String category, ChipGroup chipGroup) {
        List<String> avatars = avatarMap.get(category);
        if (avatarAdapter != null && avatars != null) {
            avatarAdapter.updateAvatars(avatars);
            avatarAdapter.setSelectedPosition(RecyclerView.NO_POSITION);
        }
    }

    private void setupAvatarData() {
        avatarMap = new LinkedHashMap<>();
        avatarMap.put("Smileys & Emotion", List.of("😀", "😃", "😄", "😁", "😅", "😂", "🤣", "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚", "🤪", "😜", "😝", "😛", "🤑", "🤗", "🤭", "🤫", "🤔", "🤐", "🤨"));
        avatarMap.put("Animals & Nature", List.of("🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔", "🐧", "🐦", "🦆", "🦉", "🐙", "🦄", "🐝", "🦋", "🐌", "🐛", "🦗", "🕷️", "🦀", "🐠"));
        avatarMap.put("Food & Drink", List.of("🍎", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🥑", "🥔", "🥕", "🌽", "🍕", "🍔", "🍰", "🎂", "🍦", "🧁", "🍩", "🍪", "☕", "🍵", "🥤", "🧃"));
        avatarMap.put("Objects & Symbols", List.of("🌟", "⭐", "✨", "💫", "🔥", "💥", "⚡", "🌈", "☀️", "🌙", "⭐", "🎈", "🎉", "🎊", "🎁", "🏆", "🥇", "🥈", "🥉", "🎖️", "👑", "💎", "🔑", "🧸", "🎀", "🎮", "🤖", "👾", "👽", "🛸"));
    }
}
