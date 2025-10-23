package com.tcwhu.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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

    private MaterialToolbar toolbar;
    private CardView previewCard;
    private TextView previewAvatarText;
    private ChipGroup categoryChipGroup;
    private RecyclerView avatarRecyclerView;
    private Button buttonConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar_selector);

        toolbar = findViewById(R.id.toolbar);
        previewCard = findViewById(R.id.previewCard);
        previewAvatarText = findViewById(R.id.previewAvatarText);
        categoryChipGroup = findViewById(R.id.categoryChipGroup);
        avatarRecyclerView = findViewById(R.id.avatarRecyclerView);
        buttonConfirm = findViewById(R.id.buttonConfirm);

        setupToolbar();
        setupAvatarData();
        setupCategoryChips();
        setupRecyclerView();

        updateAvatarGrid("Smileys & Emotion");

        buttonConfirm.setOnClickListener(v -> {
            if (selectedAvatar != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selectedAvatar", selectedAvatar);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        avatarAdapter = new AvatarAdapter(new ArrayList<>(), this);
        avatarRecyclerView.setLayoutManager(new GridLayoutManager(this, 5));
        avatarRecyclerView.setAdapter(avatarAdapter);
    }

    private void setupCategoryChips() {
        for (String category : avatarMap.keySet()) {
            Chip chip = new Chip(this);
            chip.setText(category);
            chip.setCheckable(true);
            chip.setOnClickListener(v -> updateAvatarGrid(category));
            categoryChipGroup.addView(chip);
        }
        if (categoryChipGroup.getChildCount() > 0) {
            ((Chip) categoryChipGroup.getChildAt(0)).setChecked(true);
        }
    }

    // --- THIS METHOD IS NOW CORRECTED --- ✅
    private void updateAvatarGrid(String category) {
        List<String> avatars = avatarMap.get(category);
        // Call the new public method in the adapter to update the grid
        avatarAdapter.updateAvatars(avatars);
        // Reset the selection highlight when changing categories
        avatarAdapter.setSelectedPosition(RecyclerView.NO_POSITION);
    }

    @Override
    public void onAvatarClick(String avatar) {
        selectedAvatar = avatar;
        int position = -1;
        // Since avatarList is private, we must get the position from the new list
        for (int i=0; i < avatarAdapter.getItemCount(); i++) {
            if (avatarMap.get("Smileys & Emotion").get(i).equals(avatar) ||
                    avatarMap.get("Animals & Nature").get(i).equals(avatar) ||
                    avatarMap.get("Food & Drink").get(i).equals(avatar) ||
                    avatarMap.get("Objects & Symbols").get(i).equals(avatar)) {
                position = i;
                break;
            }
        }

        avatarAdapter.setSelectedPosition(position);

        previewCard.setVisibility(View.VISIBLE);
        previewAvatarText.setText(avatar);
        buttonConfirm.setEnabled(true);
        buttonConfirm.setText("Confirm Selection");
    }

    private void setupAvatarData() {
        avatarMap = new LinkedHashMap<>();
        avatarMap.put("Smileys & Emotion", List.of("😀", "😃", "😄", "😁", "😅", "😂", "🤣", "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚", "🤪", "😜", "😝", "😛", "🤑", "🤗", "🤭", "🤫", "🤔", "🤐", "🤨"));
        avatarMap.put("Animals & Nature", List.of("🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔", "🐧", "🐦", "🦆", "🦉", "🐙", "🦄", "🐝", "🦋", "🐌", "🐛", "🦗", "🕷️", "🦀", "🐠"));
        avatarMap.put("Food & Drink", List.of("🍎", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🥑", "🥔", "🥕", "🌽", "🍕", "🍔", "🍰", "🎂", "🍦", "🧁", "🍩", "🍪", "☕", "🍵", "🥤", "🧃"));
        avatarMap.put("Objects & Symbols", List.of("🌟", "⭐", "✨", "💫", "🔥", "💥", "⚡", "🌈", "☀️", "🌙", "⭐", "🎈", "🎉", "🎊", "🎁", "🏆", "🥇", "🥈", "🥉", "🎖️", "👑", "💎", "🔑", "🧸", "🎀", "🎮", "🤖", "👾", "👽", "🛸"));
    }
}