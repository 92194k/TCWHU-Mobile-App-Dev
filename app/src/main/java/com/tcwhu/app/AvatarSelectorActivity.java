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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AvatarSelectorActivity extends AppCompatActivity implements AvatarAdapter.OnAvatarListener {

    private Map<String, List<String>> avatarMap;
    private AvatarAdapter avatarAdapter;
    private String selectedAvatar = null;

    // NOTE: userId is still retrieved but is not used to write directly in this Activity.
    private String userId;

    private CardView previewCard;
    private TextView previewAvatarText;
    private Button buttonConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar_selector);

        userId = getIntent().getStringExtra("userId");

        // Safety check (optional but good practice)
        if (userId == null || userId.isEmpty()) {
            // Can proceed without userId if its only purpose is to return data,
            // but logging the error is best for debugging the flow.
            // Log.e("AvatarSelector", "Warning: userId is null/empty.");
        }

        // Find views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        previewCard = findViewById(R.id.previewCard);
        previewAvatarText = findViewById(R.id.previewAvatarText);
        ChipGroup categoryChipGroup = findViewById(R.id.categoryChipGroup);
        RecyclerView avatarRecyclerView = findViewById(R.id.avatarRecyclerView);
        buttonConfirm = findViewById(R.id.buttonConfirm);

        // Setup UI and data
        setupToolbar(toolbar);
        setupAvatarData();
        setupCategoryChips(categoryChipGroup);
        setupRecyclerView(avatarRecyclerView);
        updateAvatarGrid("Smileys & Emotion", categoryChipGroup);

        buttonConfirm.setOnClickListener(v -> {
            if (selectedAvatar != null) {
                // --- CRITICAL UNIVERSAL FIX --- ✅
                // Always return the selected avatar via RESULT_OK.
                // The calling Activity (SignUp or ProfileFragment) is responsible for DB updates.
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selectedAvatar", selectedAvatar);
                resultIntent.putExtra("userId", userId); // Pass back the userId just in case
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(AvatarSelectorActivity.this, "Please select an avatar first.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // NOTE: The previous method updateUserAvatarAndProceed() is REMOVED from this Activity.
    // It is now the responsibility of StudentProfileFragment.java or StudentSignUpActivity.java to handle the update.

    @Override
    public void onAvatarClick(String avatar) {
        selectedAvatar = avatar;

        int position = -1;
        if (avatarAdapter != null) {
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
        buttonConfirm.setText("Confirm Selection");
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
        if (avatarAdapter != null) {
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