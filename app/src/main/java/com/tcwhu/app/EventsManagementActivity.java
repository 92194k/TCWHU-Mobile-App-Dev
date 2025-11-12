package com.tcwhu.app;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImage;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

public class EventsManagementActivity extends AppCompatActivity implements EventsManagementAdapter.OnEventActionListener {

    private RecyclerView recyclerView;
    private EventsManagementAdapter adapter;
    private List<Event> eventList;
    private FirebaseFirestore db;
    private TextView emptyView;
    private FloatingActionButton fabAddEvent;

    private static final long MAX_FILE_SIZE_MB = 25;
    private static final long MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;

    private long selectedDateMillis = 0;
    private Uri selectedImageUri = null;
    private ImageView dialogImagePreview = null;
    private AlertDialog currentDialog = null;

    // --- Correct CropImage launcher for CanHub 4.x ---
    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), this::handleCropImageResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events_management);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.eventsRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        fabAddEvent = findViewById(R.id.fabAddEvent);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setupRecyclerView();

        fabAddEvent.setOnClickListener(v -> showAddEventDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    private void setupRecyclerView() {
        eventList = new ArrayList<>();
        adapter = new EventsManagementAdapter(eventList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && fabAddEvent.isShown()) {
                    fabAddEvent.hide();
                } else if (dy < 0 && !fabAddEvent.isShown()) {
                    fabAddEvent.show();
                }
            }
        });
    }

    private void loadEvents() {
        db.collection("events")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        eventList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Event event = document.toObject(Event.class);
                            event.setId(document.getId());
                            eventList.add(event);
                        }
                        adapter.notifyDataSetChanged();
                        checkIfEmpty();
                    } else {
                        Toast.makeText(this, "Error loading events.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAddEventDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_event, null);
        builder.setView(dialogView);

        selectedImageUri = null;
        selectedDateMillis = 0;

        TextInputEditText inputTitle = dialogView.findViewById(R.id.inputEventTitle);
        TextInputEditText inputDesc = dialogView.findViewById(R.id.inputEventDescription);
        TextInputEditText inputDate = dialogView.findViewById(R.id.inputEventDate);
        TextInputEditText inputPostedBy = dialogView.findViewById(R.id.inputPostedBy);
        dialogImagePreview = dialogView.findViewById(R.id.imageEventPreview);
        MaterialButton buttonUploadEventImage = dialogView.findViewById(R.id.buttonUploadEventImage);

        // DATE PICKER
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker().build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            TimeZone timeZoneUTC = TimeZone.getDefault();
            int offsetFromUTC = timeZoneUTC.getOffset(new Date().getTime()) * -1;
            selectedDateMillis = selection + offsetFromUTC;
            inputDate.setText(datePicker.getHeaderText());
            inputDate.setError(null);
        });
        inputDate.setOnClickListener(v -> datePicker.show(getSupportFragmentManager(), "DATE_PICKER"));

        // IMAGE CROPPER
        buttonUploadEventImage.setOnClickListener(v -> {
            CropImageOptions cropOptions = new CropImageOptions();
            cropOptions.guidelines = CropImageView.Guidelines.ON;
            cropOptions.aspectRatioX = 16;
            cropOptions.aspectRatioY = 9;
            cropOptions.fixAspectRatio = true;
            cropOptions.showCropOverlay = true;
            cropOptions.allowRotation = true;
            cropOptions.activityTitle = "Crop Event Photo";
            cropOptions.activityMenuIconColor = getResources().getColor(R.color.white);

            CropImageContractOptions contractOptions = new CropImageContractOptions(null, cropOptions);
            cropImageLauncher.launch(contractOptions);
        });

        currentDialog = builder.setPositiveButton("Create", null)
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .create();

        currentDialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = currentDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String title = inputTitle.getText().toString().trim();
                String desc = inputDesc.getText().toString().trim();
                String postedBy = inputPostedBy.getText().toString().trim();

                boolean isValid = true;
                if (title.isEmpty()) { inputTitle.setError("Title is required"); isValid=false; } else inputTitle.setError(null);
                if (desc.isEmpty()) { inputDesc.setError("Description is required"); isValid=false; } else inputDesc.setError(null);
                if (selectedDateMillis == 0) { inputDate.setError("Date is required"); isValid=false; } else inputDate.setError(null);
                if (postedBy.isEmpty()) { inputPostedBy.setError("Posted By is required"); isValid=false; } else inputPostedBy.setError(null);

                if (!isValid) return;

                if (selectedImageUri != null) {
                    positiveButton.setEnabled(false);
                    positiveButton.setText("Uploading...");
                    uploadEventImage(selectedImageUri, title, desc, postedBy);
                } else {
                    addEventToFirestore(title, desc, postedBy, null);
                    currentDialog.dismiss();
                }
            });
        });

        currentDialog.show();
    }

    private void handleCropImageResult(CropImageView.CropResult result) {
        if (result.isSuccessful()) {
            selectedImageUri = result.getUriContent();
            if (selectedImageUri == null) {
                Toast.makeText(this, "Failed to get cropped image", Toast.LENGTH_SHORT).show();
                return;
            }
            if (getFileSize(selectedImageUri) > MAX_FILE_SIZE_BYTES) {
                Toast.makeText(this, "Image is too large. Max " + MAX_FILE_SIZE_MB + "MB.", Toast.LENGTH_LONG).show();
                selectedImageUri = null;
                return;
            }
            if (dialogImagePreview != null) {
                dialogImagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                dialogImagePreview.setPadding(0,0,0,0);
                Glide.with(this).load(selectedImageUri).into(dialogImagePreview);
                if (currentDialog != null)
                    currentDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            }
        } else if (result.getError() != null) {
            Toast.makeText(this, "Cropping failed: " + result.getError().getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private long getFileSize(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (!cursor.isNull(sizeIndex)) return cursor.getLong(sizeIndex);
            }
        } catch (Exception e) { Log.e("EventsManagement", "Error getting file size", e); }
        return -1;
    }

    private void uploadEventImage(Uri imageUri, String title, String desc, String postedBy) {
        String uniqueId = UUID.randomUUID().toString();
        String publicId = "event_photos/" + uniqueId;

        Toast.makeText(this, "Uploading image...", Toast.LENGTH_LONG).show();

        MediaManager.get().upload(imageUri)
                .option("public_id", publicId)
                .callback(new UploadCallback() {
                    @Override public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        addEventToFirestore(title, desc, postedBy, url);
                        if (currentDialog != null) currentDialog.dismiss();
                        Toast.makeText(EventsManagementActivity.this, "Event created successfully!", Toast.LENGTH_LONG).show();
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        if (currentDialog != null)
                            currentDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        Toast.makeText(EventsManagementActivity.this, "Image upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
                    }
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void addEventToFirestore(String title, String desc, String postedBy, String imageUrl) {
        Map<String, Object> newEvent = new HashMap<>();
        newEvent.put("title", title);
        newEvent.put("description", desc);
        newEvent.put("date", selectedDateMillis);
        newEvent.put("postedBy", postedBy);
        newEvent.put("imageUrl", imageUrl);

        db.collection("events").add(newEvent)
                .addOnSuccessListener(doc -> loadEvents())
                .addOnFailureListener(e -> Toast.makeText(this, "Error saving event.", Toast.LENGTH_SHORT).show());
    }

    private void showDeleteConfirmationDialog(Event event) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Yes", (dialog, which) -> deleteEvent(event))
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteEvent(Event event) {
        if (event.getId() == null || event.getId().isEmpty()) {
            Toast.makeText(this, "Cannot delete event (missing ID).", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("events").document(event.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event deleted successfully", Toast.LENGTH_SHORT).show();
                    loadEvents();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void checkIfEmpty() {
        if (eventList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onEventClick(Event event) {
        Intent intent = new Intent(this, EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT, event);
        startActivity(intent);
    }

    @Override
    public void onDelete(Event event) {
        showDeleteConfirmationDialog(event);
    }
}
