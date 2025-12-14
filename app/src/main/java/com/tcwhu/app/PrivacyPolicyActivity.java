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
        setContentView(R.layout.activity_privacy_policy_layout);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        TextView textTitle = findViewById(R.id.textPrivacyTitle);
        TextView textBody = findViewById(R.id.textPrivacyPolicy);

        textTitle.setText(R.string.privacy_policy_title);
        String htmlBody = getString(R.string.privacy_policy_body);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            textBody.setText(Html.fromHtml(htmlBody, Html.FROM_HTML_MODE_COMPACT));
        } else {
            textBody.setText(Html.fromHtml(htmlBody));
        }
    }
}