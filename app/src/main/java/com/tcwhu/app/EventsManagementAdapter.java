package com.tcwhu.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.RequestOptions;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class EventsManagementAdapter extends RecyclerView.Adapter<EventsManagementAdapter.ViewHolder> {

    public interface OnEventActionListener {
        void onEventClick(Event event);
        void onDelete(Event event);
    }

    private final List<Event> events;
    private final OnEventActionListener listener;

    public EventsManagementAdapter(List<Event> events, OnEventActionListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_admin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event, listener);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, postedBy, eventDate, targetId;
        ImageView deleteButton, eventImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textEventTitle);
            description = itemView.findViewById(R.id.textEventDescription);
            postedBy = itemView.findViewById(R.id.textPostedBy);
            eventDate = itemView.findViewById(R.id.textEventDate);
            targetId = itemView.findViewById(R.id.textTargetId);
            deleteButton = itemView.findViewById(R.id.buttonDeleteEvent);
            eventImage = itemView.findViewById(R.id.imageEvent);
        }

        public void bind(final Event event, final OnEventActionListener listener) {
            title.setText(event.getTitle());
            description.setText(event.getDescription());
            postedBy.setText("Posted by: " + event.getPostedBy());

            if (event.getDate() != 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
                eventDate.setText("— " + sdf.format(new Date(event.getDate())));
            } else {
                eventDate.setText("");
            }

            if (event.getTargetId() != null && !event.getTargetId().isEmpty()) {
                targetId.setVisibility(View.VISIBLE);
                targetId.setText("Target: " + event.getTargetId());
            } else {
                targetId.setVisibility(View.GONE);
            }

            if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
                eventImage.setVisibility(View.VISIBLE);
                Glide.with(eventImage.getContext())
                        .load(event.getImageUrl())
                        .centerCrop()
                        .placeholder(R.drawable.ic_events)
                        .error(R.drawable.ic_alert_error)
                        .into(eventImage);
            } else {
                eventImage.setVisibility(View.GONE);
            }

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDelete(event);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventClick(event);
                }
            });
        }
    }
}