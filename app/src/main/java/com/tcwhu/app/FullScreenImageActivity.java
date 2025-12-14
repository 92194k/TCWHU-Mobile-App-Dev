package com.tcwhu.app;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class FullScreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        ImageView imageView = findViewById(R.id.fullScreenImageView);
        String imageUrl = getIntent().getStringExtra("imageUrl");

        if (imageUrl != null) {
            Glide.with(this).load(imageUrl).into(imageView);
        }

        imageView.setOnClickListener(v -> finish());
    }
}