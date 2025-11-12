package com.tcwhu.app;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT = "EXTRA_EVENT";

    private ImageView detailEventImage;
    private TextView detailEventTitle, detailEventDate, detailEventPostedBy, detailEventDescription;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(""); // We want the title to collapse, not be static
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        detailEventImage = findViewById(R.id.detailEventImage);
        detailEventTitle = findViewById(R.id.detailEventTitle);
        detailEventDate = findViewById(R.id.detailEventDate);
        detailEventPostedBy = findViewById(R.id.detailEventPostedBy);
        detailEventDescription = findViewById(R.id.detailEventDescription);

        // Get the Event object from the Intent
        Event event = (Event) getIntent().getSerializableExtra(EXTRA_EVENT);

        if (event == null) {
            Toast.makeText(this, "Error: Could not load event details.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        populateEventDetails(event);
    }

    private void populateEventDetails(Event event) {
        detailEventTitle.setText(event.getTitle());
        detailEventDescription.setText(event.getDescription());
        detailEventPostedBy.setText("Posted by " + event.getPostedBy());

        // Format Date
        SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US);
        String formattedDate = formatter.format(new Date(event.getDate()));
        detailEventDate.setText(formattedDate);

        // Load Image
        if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(event.getImageUrl())
                    .centerCrop()
                    .into(detailEventImage);
        } else {
            // Set a default image if none provided
            detailEventImage.setImageResource(R.drawable.ic_events);
        }
    }
}