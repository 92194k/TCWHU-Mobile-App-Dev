package com.tcwhu.app;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messageList;
    private String currentUserId;

    // --- Constructor is now correct ---
    public MessagesAdapter(List<Message> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        // Pass the viewType to the bind method
        holder.bind(messageList.get(position), getItemViewType(position));
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, textTimestamp;
        ImageView messageImage, iconSeenStatus;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            messageImage = itemView.findViewById(R.id.messageImage);
            textTimestamp = itemView.findViewById(R.id.textTimestamp); // Find timestamp view
            iconSeenStatus = itemView.findViewById(R.id.iconSeenStatus); // Find seen status icon
        }

        public void bind(Message message, int viewType) {
            // Bind content (text or image)
            if ("image".equals(message.getType())) {
                messageText.setVisibility(View.GONE);
                messageImage.setVisibility(View.VISIBLE);
                RequestOptions requestOptions = new RequestOptions().transform(new RoundedCorners(32));
                Glide.with(itemView.getContext())
                        .load(message.getContent())
                        .apply(requestOptions)
                        .into(messageImage);
            } else {
                messageText.setVisibility(View.VISIBLE);
                messageImage.setVisibility(View.GONE);
                messageText.setText(message.getContent());
            }

            // Bind Timestamp
            SimpleDateFormat formatter = new SimpleDateFormat("h:mm a", Locale.getDefault());
            textTimestamp.setText(formatter.format(new Date(message.getTimestamp())));

            // Bind Seen Status (ONLY for sent messages)
            if (viewType == VIEW_TYPE_SENT) {
                if (iconSeenStatus == null) return; // Safety check
                if (message.isSeen()) {
                    // Seen: Blue/Purple check
                    iconSeenStatus.setVisibility(View.VISIBLE);
                    DrawableCompat.setTint(
                            iconSeenStatus.getDrawable(),
                            ContextCompat.getColor(itemView.getContext(), R.color.deep_purple) // Your purple
                    );
                } else {
                    // Sent/Delivered: Gray check
                    iconSeenStatus.setVisibility(View.VISIBLE);
                    DrawableCompat.setTint(
                            iconSeenStatus.getDrawable(),
                            ContextCompat.getColor(itemView.getContext(), R.color.inactive_gray)
                    );
                }
            } else {
                // Hide icon if it's a received message
                if (iconSeenStatus != null) {
                    iconSeenStatus.setVisibility(View.GONE);
                }
            }
        }
    }
}