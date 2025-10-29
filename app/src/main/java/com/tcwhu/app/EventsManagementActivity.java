package com.tcwhu.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
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

public class EventsManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EventsManagementAdapter adapter;
    private List<Event> eventList;
    private FirebaseFirestore db;
    private TextView emptyView;

    private long selectedDateMillis = 0;
    private Uri selectedImageUri = null;
    private String uploadedImageUrl = null;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ImageView dialogImagePreview = null;
    private AlertDialog currentDialog = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events_management);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.eventsRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        FloatingActionButton fabAddEvent = findViewById(R.id.fabAddEvent);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setupRecyclerView();

        fabAddEvent.setOnClickListener(v -> showAddEventDialog());

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (dialogImagePreview != null && selectedImageUri != null) {
                            dialogImagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            dialogImagePreview.setPadding(0, 0, 0, 0);
                            Glide.with(this).load(selectedImageUri).into(dialogImagePreview);

                            if (currentDialog != null) {
                                currentDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            }
                        }
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    private void setupRecyclerView() {
        eventList = new ArrayList<>();
        adapter = new EventsManagementAdapter(eventList, this::showDeleteConfirmationDialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
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
                        // --- CRITICAL FIX: Explicitly notify the adapter --- ✅
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

        uploadedImageUrl = null;
        selectedImageUri = null;
        selectedDateMillis = 0;

        TextInputEditText inputTitle = dialogView.findViewById(R.id.inputEventTitle);
        TextInputEditText inputDesc = dialogView.findViewById(R.id.inputEventDescription);
        TextInputEditText inputDate = dialogView.findViewById(R.id.inputEventDate);
        TextInputEditText inputPostedBy = dialogView.findViewById(R.id.inputPostedBy);
        dialogImagePreview = dialogView.findViewById(R.id.imageEventPreview);
        Button buttonUploadEventImage = dialogView.findViewById(R.id.buttonUploadEventImage);

        // 1. DATE PICKER SETUP (Unchanged)
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker().build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            TimeZone timeZoneUTC = TimeZone.getDefault();
            int offsetFromUTC = timeZoneUTC.getOffset(new Date().getTime()) * -1;
            selectedDateMillis = selection + offsetFromUTC;
            inputDate.setText(datePicker.getHeaderText());
        });
        inputDate.setOnClickListener(v -> datePicker.show(getSupportFragmentManager(), "DATE_PICKER"));


        // 2. IMAGE UPLOAD SETUP
        buttonUploadEventImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });


        // 3. DIALOG CREATION
        currentDialog = builder.setPositiveButton("Create", null)
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .create();

        currentDialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = currentDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String title = inputTitle.getText().toString().trim();
                String desc = inputDesc.getText().toString().trim();
                String postedBy = inputPostedBy.getText().toString().trim();

                if (title.isEmpty() || desc.isEmpty() || selectedDateMillis == 0 || postedBy.isEmpty()) {
                    Toast.makeText(this, "Please fill all required fields.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (selectedImageUri != null) {
                    positiveButton.setEnabled(false);
                    uploadEventImage(selectedImageUri, title, desc, postedBy);
                } else {
                    addEventToFirestore(title, desc, postedBy, null);
                    currentDialog.dismiss();
                }
            });
        });

        currentDialog.show();
    }


    private void uploadEventImage(Uri imageUri, String title, String desc, String postedBy) {
        String uniqueId = UUID.randomUUID().toString();
        String publicId = "event_photos/" + uniqueId;

        Toast.makeText(this, "Uploading image...", Toast.LENGTH_LONG).show();

        MediaManager.get().upload(imageUri)
                .option("public_id", publicId)
                .callback(new UploadCallback() {
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        addEventToFirestore(title, desc, postedBy, url);
                        if (currentDialog != null) currentDialog.dismiss();
                        Toast.makeText(EventsManagementActivity.this, "Event created successfully!", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        if (currentDialog != null) currentDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
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
                .addOnSuccessListener(documentReference -> {
                    // The dialog is dismissed and loadEvents() is called in onResume, ensuring the list updates.
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error saving event to database.", Toast.LENGTH_SHORT).show());
    }

    private void showDeleteConfirmationDialog(Event event) { /* ... unchanged ... */ }
    private void deleteEvent(Event event) { /* ... unchanged ... */ }
    private void checkIfEmpty() { /* ... unchanged ... */ }
}