package com.tcwhu.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class PhotoUploadActivity extends AppCompatActivity {

    private ImageView selfiePreview, idPreview;
    private Button buttonUploadSelfie, buttonUploadId, buttonFinish;
    private ProgressBar progressBar;

    private String selfieImageUrl, idImageUrl;
    private boolean isUploadingSelfie;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private String userId;
    private Map<String, Object> userData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_upload);

        userId = getIntent().getStringExtra("userId");
        userData = (Map<String, Object>) getIntent().getSerializableExtra("userData");

        selfiePreview = findViewById(R.id.selfiePreview);
        idPreview = findViewById(R.id.idPreview);
        buttonUploadSelfie = findViewById(R.id.buttonUploadSelfie);
        buttonUploadId = findViewById(R.id.buttonUploadId);
        buttonFinish = findViewById(R.id.buttonFinish);
        progressBar = findViewById(R.id.progressBar);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (isUploadingSelfie) {
                            selfiePreview.setPadding(0, 0, 0, 0);
                            selfiePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            selfiePreview.setImageTintList(null);
                            Glide.with(this).load(selectedImageUri).into(selfiePreview);
                            uploadImageToCloudinary("selfie", selectedImageUri);
                        } else {
                            idPreview.setPadding(0, 0, 0, 0);
                            idPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            idPreview.setImageTintList(null);
                            Glide.with(this).load(selectedImageUri).into(idPreview);
                            uploadImageToCloudinary("id_photo", selectedImageUri);
                        }
                    }
                }
        );

        buttonUploadSelfie.setOnClickListener(v -> {
            isUploadingSelfie = true;
            openImagePicker();
        });

        buttonUploadId.setOnClickListener(v -> {
            isUploadingSelfie = false;
            openImagePicker();
        });

        buttonFinish.setOnClickListener(v -> {
            userData.put("selfiePhotoUrl", selfieImageUrl);
            userData.put("idPhotoUrl", idImageUrl);
            saveUserProfileAndProceed();
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void uploadImageToCloudinary(String type, Uri imageUri) {
        progressBar.setVisibility(View.VISIBLE);
        String publicId = "verification_photos/" + userId + "/" + type;

        MediaManager.get().upload(imageUri)
                .option("public_id", publicId)
                .callback(new UploadCallback() {
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        if ("selfie".equals(type)) {
                            selfieImageUrl = url;
                        } else {
                            idImageUrl = url;
                        }
                        checkIfBothUploaded();
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(PhotoUploadActivity.this, type + " photo uploaded!", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(PhotoUploadActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void checkIfBothUploaded() {
        if (selfieImageUrl != null && idImageUrl != null) {
            buttonFinish.setEnabled(true);
        }
    }

    private void saveUserProfileAndProceed() {
        // --- THIS IS THE CORRECTED LINE ---
        // Use .set() to CREATE the document for the first time.
        FirebaseFirestore.getInstance().collection("users").document(userId).set(userData)
                .addOnSuccessListener(aVoid -> {
                    Intent intent = new Intent(this, AvatarSelectorActivity.class);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("PhotoUploadActivity", "Firestore save failed", e);
                });
    }
}