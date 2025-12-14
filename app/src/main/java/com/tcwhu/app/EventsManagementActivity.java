package com.tcwhu.app;

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

    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), this::handleCropImageResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events_management);

        db = FirebaseFirestore.getInstance();
        initViews();
        setupRecyclerView();

        fabAddEvent.setOnClickListener(v -> showAddEventDialog());
    }

    private void initViews() {
        recyclerView = findViewById(R.id.eventsRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        fabAddEvent = findViewById(R.id.fabAddEvent);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
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
                if (dy > 0 && fabAddEvent.isShown()) fabAddEvent.hide();
                else if (dy < 0 && !fabAddEvent.isShown()) fabAddEvent.show();
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

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker().build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            TimeZone timeZoneUTC = TimeZone.getDefault();
            int offsetFromUTC = timeZoneUTC.getOffset(new Date().getTime()) * -1;
            selectedDateMillis = selection + offsetFromUTC;
            inputDate.setText(datePicker.getHeaderText());
            inputDate.setError(null);
        });
        inputDate.setOnClickListener(v -> datePicker.show(getSupportFragmentManager(), "DATE_PICKER"));

        buttonUploadEventImage.setOnClickListener(v -> {
            CropImageOptions cropOptions = new CropImageOptions();
            cropOptions.guidelines = CropImageView.Guidelines.ON;
            cropOptions.aspectRatioX = 16;
            cropOptions.aspectRatioY = 9;
            cropOptions.fixAspectRatio = true;
            cropImageLauncher.launch(new CropImageContractOptions(null, cropOptions));
        });

        currentDialog = builder.setPositiveButton("Create", null)
                .setNegativeButton("Cancel", (d, i) -> d.dismiss())
                .create();

        currentDialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = currentDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String title = inputTitle.getText().toString().trim();
                String desc = inputDesc.getText().toString().trim();
                String postedBy = inputPostedBy.getText().toString().trim();

                if (title.isEmpty() || desc.isEmpty() || selectedDateMillis == 0 || postedBy.isEmpty()) {
                    Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
                    return;
                }

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
            if (selectedImageUri != null && getFileSize(selectedImageUri) <= MAX_FILE_SIZE_BYTES) {
                if (dialogImagePreview != null) {
                    dialogImagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    Glide.with(this).load(selectedImageUri).into(dialogImagePreview);
                    if (currentDialog != null) currentDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            } else {
                Toast.makeText(this, "Image too large or invalid.", Toast.LENGTH_SHORT).show();
            }
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
        String publicId = "event_photos/" + UUID.randomUUID().toString();
        MediaManager.get().upload(imageUri)
                .option("public_id", publicId)
                .callback(new UploadCallback() {
                    @Override public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        addEventToFirestore(title, desc, postedBy, url);
                        if (currentDialog != null) currentDialog.dismiss();
                        Toast.makeText(EventsManagementActivity.this, "Event created.", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        if (currentDialog != null) currentDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        Toast.makeText(EventsManagementActivity.this, "Upload failed.", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onStart(String r) {}
                    @Override public void onProgress(String r, long b, long t) {}
                    @Override public void onReschedule(String r, ErrorInfo e) {}
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

    private void checkIfEmpty() {
        boolean isEmpty = eventList.isEmpty();
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onEventClick(Event event) {
        Intent intent = new Intent(this, EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT, event);
        startActivity(intent);
    }

    @Override
    public void onDelete(Event event) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    db.collection("events").document(event.getId()).delete()
                            .addOnSuccessListener(a -> {
                                Toast.makeText(this, "Event deleted.", Toast.LENGTH_SHORT).show();
                                loadEvents();
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }
}