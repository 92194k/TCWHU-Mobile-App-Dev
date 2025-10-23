package com.tcwhu.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class EventsManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EventsManagementAdapter adapter;
    private List<Event> eventList;
    private FirebaseFirestore db;
    private TextView emptyView;
    private long selectedDateMillis = 0; // To store selected date

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
        getSupportActionBar().setDisplayShowHomeEnabled(true);
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
        adapter = new EventsManagementAdapter(eventList, event -> showDeleteConfirmationDialog(event));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadEvents() {
        db.collection("events").orderBy("date", Query.Direction.DESCENDING).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        eventList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Event event = document.toObject(Event.class);
                            // Store Firestore document ID inside the Event object
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
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_event, null);
        builder.setView(dialogView);

        TextInputEditText inputTitle = dialogView.findViewById(R.id.inputEventTitle);
        TextInputEditText inputDesc = dialogView.findViewById(R.id.inputEventDescription);
        TextInputEditText inputDate = dialogView.findViewById(R.id.inputEventDate);
        TextInputEditText inputPostedBy = dialogView.findViewById(R.id.inputPostedBy);
        TextInputEditText inputImageUrl = dialogView.findViewById(R.id.inputImageUrl);
        selectedDateMillis = 0; // Reset date

        // --- Date Picker Setup ---
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select event date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // Adjust for TimeZone offset
            TimeZone timeZoneUTC = TimeZone.getDefault();
            int offsetFromUTC = timeZoneUTC.getOffset(new Date().getTime()) * -1;
            selectedDateMillis = selection + offsetFromUTC;

            // Format and display the selected date
            inputDate.setText(datePicker.getHeaderText());
        });

        inputDate.setOnClickListener(v -> datePicker.show(getSupportFragmentManager(), "DATE_PICKER"));
        // --- End Date Picker ---

        builder.setPositiveButton("Create", (dialog, which) -> {
            String title = inputTitle.getText().toString().trim();
            String desc = inputDesc.getText().toString().trim();
            String postedBy = inputPostedBy.getText().toString().trim();
            String imageUrl = inputImageUrl.getText().toString().trim();

            if (!title.isEmpty() && !desc.isEmpty() && selectedDateMillis > 0 && !postedBy.isEmpty()) {
                Event newEvent = new Event(title, desc, selectedDateMillis, imageUrl, postedBy);
                addEventToFirestore(newEvent);
            } else {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void addEventToFirestore(Event event) {
        db.collection("events").add(event)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Event added successfully", Toast.LENGTH_SHORT).show();
                    loadEvents(); // Refresh list
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error adding event", Toast.LENGTH_SHORT).show());
    }

    private void showDeleteConfirmationDialog(Event event) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete '" + event.getTitle() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent(event))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteEvent(Event event) {
        db.collection("events").document(event.getId()).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                    loadEvents(); // Refresh list
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error deleting event", Toast.LENGTH_SHORT).show());
    }

    private void checkIfEmpty() {
        emptyView.setVisibility(eventList.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(eventList.isEmpty() ? View.GONE : View.VISIBLE);
    }
}