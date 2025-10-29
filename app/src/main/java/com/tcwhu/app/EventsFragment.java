package com.tcwhu.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore; // CRITICAL IMPORT
import com.google.firebase.firestore.Query; // CRITICAL IMPORT
import com.google.firebase.firestore.QueryDocumentSnapshot; // CRITICAL IMPORT
import java.util.ArrayList;
import java.util.List;

public class EventsFragment extends Fragment {

    private RecyclerView eventsRecyclerView;
    private EventsAdapter eventsAdapter;
    private List<Event> eventList;
    private TextView emptyView;
    private FirebaseFirestore db; // CRITICAL: Database instance

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance(); // Initialize Firestore
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_events, container, false);

        eventsRecyclerView = view.findViewById(R.id.eventsRecyclerView);
        emptyView = view.findViewById(R.id.emptyView);

        setupRecyclerView();

        return view;
    }

    // --- CRITICAL FIX: Load data in onResume to guarantee synchronization --- ✅
    @Override
    public void onResume() {
        super.onResume();
        loadEventsFromFirestore();
    }

    private void setupRecyclerView() {
        eventList = new ArrayList<>();
        eventsAdapter = new EventsAdapter(eventList);
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventsRecyclerView.setAdapter(eventsAdapter);
    }

    private void loadEventsFromFirestore() {
        // Fetch all events from the 'events' collection, sorted by date
        db.collection("events")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        eventList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Event event = document.toObject(Event.class);
                            if (event.getTitle() != null) {
                                eventList.add(event);
                            }
                        }
                        eventsAdapter.notifyDataSetChanged();
                        checkIfEmpty();
                    } else {
                        Toast.makeText(getContext(), "Error loading events. Check Firestore Index.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkIfEmpty() {
        // Safely check if views exist before manipulating them
        if (eventsRecyclerView == null || emptyView == null || getContext() == null) return;

        if (eventList.isEmpty()) {
            eventsRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            eventsRecyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
}