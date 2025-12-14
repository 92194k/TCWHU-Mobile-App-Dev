package com.tcwhu.app;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ActivityLogDetailActivity extends AppCompatActivity {

    public static final String EXTRA_LOG = "extra_log";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        TextView textAction = findViewById(R.id.textDetailAction);
        TextView textAdminId = findViewById(R.id.textDetailAdminId);
        TextView textTimestamp = findViewById(R.id.textDetailTimestamp);
        TextView textTargetId = findViewById(R.id.textDetailTargetId);
        TextView textTargetIdLabel = findViewById(R.id.textDetailTargetIdLabel);

        ActivityLog log = (ActivityLog) getIntent().getSerializableExtra(EXTRA_LOG);

        if (log != null) {
            textAction.setText(log.getAction());
            textAdminId.setText(log.getAdminId());

            SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy, h:mm:ss a", Locale.US);
            textTimestamp.setText(formatter.format(new Date(log.getTimestamp())));

            if (log.getTargetId() != null && !log.getTargetId().isEmpty()) {
                textTargetId.setText(log.getTargetId());
                textTargetId.setVisibility(View.VISIBLE);
                textTargetIdLabel.setVisibility(View.VISIBLE);
            } else {
                textTargetId.setVisibility(View.GONE);
                textTargetIdLabel.setVisibility(View.GONE);
            }
        } else {
            Toast.makeText(this, "Error: Could not load log details.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}