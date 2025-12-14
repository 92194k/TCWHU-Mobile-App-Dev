package com.tcwhu.app;

import android.content.Intent;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class EventsFragment extends Fragment implements EventsAdapter.OnEventClickListener {

    private RecyclerView eventsRecyclerView;
    private EventsAdapter eventsAdapter;
    private List<Event> eventList;
    private TextView emptyView;
    private FirebaseFirestore db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
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

    @Override
    public void onResume() {
        super.onResume();
        loadEventsFromFirestore();
    }

    private void setupRecyclerView() {
        eventList = new ArrayList<>();
        eventsAdapter = new EventsAdapter(eventList, this);
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventsRecyclerView.setAdapter(eventsAdapter);
    }

    private void loadEventsFromFirestore() {
        db.collection("events")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        eventList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Event event = document.toObject(Event.class);
                            if (event.getTitle() != null) {
                                event.setId(document.getId());
                                eventList.add(event);
                            }
                        }
                        eventsAdapter.notifyDataSetChanged();
                        checkIfEmpty();
                    } else {
                        Toast.makeText(getContext(), "Error loading events.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkIfEmpty() {
        if (eventsRecyclerView == null || emptyView == null || getContext() == null) return;

        boolean isEmpty = eventList.isEmpty();
        eventsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onEventClick(Event event) {
        Intent intent = new Intent(getActivity(), EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT, event);
        startActivity(intent);
    }
}