package com.tcwhu.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventsManagementAdapter extends RecyclerView.Adapter<EventsManagementAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDeleteClick(Event event);
    }

    private List<Event> eventList;
    private OnDeleteListener listener;

    public EventsManagementAdapter(List<Event> eventList, OnDeleteListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_admin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.bind(event, listener);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView eventTitleTextView, eventDateTextView, eventDescriptionTextView;
        ImageButton buttonDeleteEvent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            eventTitleTextView = itemView.findViewById(R.id.eventTitleTextView);
            eventDateTextView = itemView.findViewById(R.id.eventDateTextView);
            eventDescriptionTextView = itemView.findViewById(R.id.eventDescriptionTextView);
            buttonDeleteEvent = itemView.findViewById(R.id.buttonDeleteEvent);
        }

        public void bind(final Event event, final OnDeleteListener listener) {
            eventTitleTextView.setText(event.getTitle());
            eventDescriptionTextView.setText(event.getDescription());

            SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            eventDateTextView.setText(formatter.format(new Date(event.getDate())));

            buttonDeleteEvent.setOnClickListener(v -> listener.onDeleteClick(event));
        }
    }
}