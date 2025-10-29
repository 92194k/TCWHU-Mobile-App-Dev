package com.tcwhu.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop; // CRITICAL IMPORT
import com.bumptech.glide.load.resource.bitmap.RoundedCorners; // CRITICAL IMPORT
import com.bumptech.glide.request.RequestOptions; // CRITICAL IMPORT
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private List<Event> eventList;

    public EventsAdapter(List<Event> eventList) {
        this.eventList = eventList;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.bind(event);
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

        public void bind(Event event) {
            eventTitleTextView.setText(event.getTitle());
            eventDescriptionTextView.setText(event.getDescription());
            eventPostedByTextView.setText("Posted by " + event.getPostedBy());

            // Format the date
            SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US);
            String formattedDate = formatter.format(new Date(event.getDate()));
            eventDateTextView.setText(formattedDate);

            // --- CRITICAL FIX: Load image with CenterCrop and Rounding --- ✅
            if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {

                // Define Glide options for scaling and corner radius (16dp to match the card view)
                RequestOptions requestOptions = new RequestOptions().transform(
                        new CenterCrop(),
                        new RoundedCorners(16)
                );

                Glide.with(itemView.getContext())
                        .load(event.getImageUrl())
                        .apply(requestOptions)
                        .into(eventImageView);
            }
        }
    }
}