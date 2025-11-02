package com.tcwhu.app;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;

public class PrivacyPolicyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // CRITICAL FIX: Use the correct layout file name
        setContentView(R.layout.activity_privacy_policy_layout);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Find both TextViews
        TextView textTitle = findViewById(R.id.textPrivacyTitle);
        TextView textBody = findViewById(R.id.textPrivacyPolicy);

        // Set the title (it's plain text, no HTML needed)
        textTitle.setText(R.string.privacy_policy_title);

        // Get the HTML-formatted string
        String htmlBody = getString(R.string.privacy_policy_body);

        // Use the modern method for rendering HTML
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            textBody.setText(Html.fromHtml(htmlBody, Html.FROM_HTML_MODE_COMPACT));
        } else {
            // Use the deprecated method for older Android versions
            textBody.setText(Html.fromHtml(htmlBody));
        }
    }
}