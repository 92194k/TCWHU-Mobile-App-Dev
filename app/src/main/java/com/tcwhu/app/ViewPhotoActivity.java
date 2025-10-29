package com.tcwhu.app;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;

public class ViewPhotoActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URL = "image_url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_photo);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        ImageView fullScreenImageView = findViewById(R.id.fullScreenImageView);

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .into(fullScreenImageView);
        }
    }
}