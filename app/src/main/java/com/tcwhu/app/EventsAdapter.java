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
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    private List<Event> eventList;
    private OnEventClickListener clickListener;

    public EventsAdapter(List<Event> eventList, OnEventClickListener clickListener) {
        this.eventList = eventList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        holder.bind(eventList.get(position), clickListener);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView eventImageView;
        TextView eventTitleTextView, eventDescriptionTextView, eventDateTextView, eventPostedByTextView;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventImageView = itemView.findViewById(R.id.eventImageView);
            eventTitleTextView = itemView.findViewById(R.id.eventTitleTextView);
            eventDescriptionTextView = itemView.findViewById(R.id.eventDescriptionTextView);
            eventDateTextView = itemView.findViewById(R.id.eventDateTextView);
            eventPostedByTextView = itemView.findViewById(R.id.eventPostedByTextView);
        }

        public void bind(final Event event, final OnEventClickListener clickListener) {
            eventTitleTextView.setText(event.getTitle());
            eventDescriptionTextView.setText(event.getDescription());
            eventPostedByTextView.setText("Posted by " + event.getPostedBy());

            SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US);
            eventDateTextView.setText(formatter.format(new Date(event.getDate())));

            if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
                RequestOptions requestOptions = new RequestOptions().transform(new CenterCrop(), new RoundedCorners(16));
                Glide.with(itemView.getContext())
                        .load(event.getImageUrl())
                        .apply(requestOptions)
                        .into(eventImageView);
            } else {
                eventImageView.setImageDrawable(null);
            }

            itemView.setOnClickListener(v -> clickListener.onEventClick(event));
        }
    }
}