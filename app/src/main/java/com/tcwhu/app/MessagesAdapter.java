package com.tcwhu.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // IMPORT ADDED
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide; // IMPORT ADDED
import com.bumptech.glide.load.resource.bitmap.RoundedCorners; // IMPORT ADDED
import com.bumptech.glide.request.RequestOptions; // IMPORT ADDED
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messageList;
    private String currentUserId;

    // --- CRITICAL FIX: Updated constructor to accept currentUserId ---
    public MessagesAdapter(List<Message> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        // Safety check for null senderId
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
        holder.bind(messageList.get(position));
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        ImageView messageImage; // The ImageView for photos

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            messageImage = itemView.findViewById(R.id.messageImage); // Initialize the ImageView
        }

        public void bind(Message message) {
            // Check the message type
            if ("image".equals(message.getType())) {
                // It's an image. Hide text, show image.
                messageText.setVisibility(View.GONE);
                messageImage.setVisibility(View.VISIBLE);

                // Define Glide options for rounded corners
                RequestOptions requestOptions = new RequestOptions()
                        .transform(new RoundedCorners(32));

                Glide.with(itemView.getContext())
                        .load(message.getContent()) // The content is the URL
                        .apply(requestOptions)
                        .into(messageImage);
            } else {
                // It's a text message. Show text, hide image.
                messageText.setVisibility(View.VISIBLE);
                messageImage.setVisibility(View.GONE);
                messageText.setText(message.getContent());
            }
        }
    }
}