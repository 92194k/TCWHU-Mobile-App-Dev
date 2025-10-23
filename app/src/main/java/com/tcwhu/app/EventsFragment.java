package com.tcwhu.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class EventsFragment extends Fragment {

    private RecyclerView eventsRecyclerView;
    private EventsAdapter eventsAdapter;
    private List<Event> eventList;
    private TextView emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_events, container, false);

        eventsRecyclerView = view.findViewById(R.id.eventsRecyclerView);
        emptyView = view.findViewById(R.id.emptyView);

        setupRecyclerView();
        loadEvents(); // We'll load sample data for now

        return view;
    }

    private void setupRecyclerView() {
        eventList = new ArrayList<>();
        eventsAdapter = new EventsAdapter(eventList);
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventsRecyclerView.setAdapter(eventsAdapter);
    }

    private void loadEvents() {
        // TODO: In the future, we will load this data from Firestore.
        // For now, let's add some sample data to see how it looks.

        eventList.add(new Event(
                "University Week 2025",
                "Join us for a week of fun, games, and competitions celebrating our university's anniversary.",
                1729785600000L, // A sample date in milliseconds
                "https://images.pexels.com/photos/2774556/pexels-photo-2774556.jpeg",
                "Admin"
        ));
        eventList.add(new Event(
                "Tech Symposium",
                "A gathering of tech enthusiasts and professionals to discuss the latest trends in technology and innovation.",
                1730457600000L, // A sample date in milliseconds
                "https://images.pexels.com/photos/3184328/pexels-photo-3184328.jpeg",
                "Admin"
        ));

        checkIfEmpty();
        eventsAdapter.notifyDataSetChanged();
    }

    private void checkIfEmpty() {
        if (eventList.isEmpty()) {
            eventsRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            eventsRecyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
}