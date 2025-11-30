package com.tcwhu.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// NOTE: Placeholder classes (Message, DownloadManagerUtils) are assumed to exist.

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messageList;
    private String currentUserId;
    private ChatWindowActivity chatActivity; // Instance of the activity for callbacks

    // --- Audio Playback Management ---
    private MediaPlayer mediaPlayer;
    private MessageViewHolder currentAudioHolder = null;
    private String currentlyPlayingUrl = null;
    // ---------------------------------

    /**
     * Corrected Primary Constructor.
     */
    public MessagesAdapter(List<Message> messageList, String currentUserId, ChatWindowActivity chatActivity) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
        this.chatActivity = chatActivity;
    }

    /**
     * Legacy/Secondary Constructor - Should be updated to include ChatWindowActivity.
     * The logic below may cause a NullPointerException if message long-press is used.
     */
    public MessagesAdapter(List<Message> messageList, String currentUserId) {
        this(messageList, currentUserId, null); // Pass null for activity, risking NPE on long-press
    }

    // --- New method to release resources on close ---
    public void cleanup() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    // ------------------------------------------------

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
        // Pass adapter instance for internal audio control methods
        holder.bind(messageList.get(position), getItemViewType(position), chatActivity, this);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // --- Audio Control Methods ---
    private void startPlayback(MessageViewHolder holder, String url) {
        // Stop any currently playing audio
        stopPlayback();

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();

            holder.iconAudioPlay.setImageResource(R.drawable.ic_pause); // Assuming you have an ic_pause
            currentAudioHolder = holder;
            currentlyPlayingUrl = url;

            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                stopPlayback();
            });

        } catch (IOException e) {
            Toast.makeText(holder.itemView.getContext(), "Error playing audio.", Toast.LENGTH_SHORT).show();
            stopPlayback();
        }
    }

    public void stopPlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (currentAudioHolder != null) {
            currentAudioHolder.iconAudioPlay.setImageResource(R.drawable.ic_play_arrow); // Assuming ic_play_arrow
            currentAudioHolder = null;
        }
        currentlyPlayingUrl = null;
    }
    // -----------------------------

    // --- MODIFICATION 3: Updated ViewHolder Class ---
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, textTimestamp, textFileName, textDeletedStatus;
        ImageView messageImage, iconSeenStatus, iconFileDownload, iconAudioPlay;
        LinearLayout fileContainer, audioContainer;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            messageImage = itemView.findViewById(R.id.messageImage);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            iconSeenStatus = itemView.findViewById(R.id.iconSeenStatus);

            fileContainer = itemView.findViewById(R.id.fileContainer);
            textFileName = itemView.findViewById(R.id.textFileName);
            iconFileDownload = itemView.findViewById(R.id.iconFileDownload);
            audioContainer = itemView.findViewById(R.id.audioContainer);
            iconAudioPlay = itemView.findViewById(R.id.iconAudioPlay);
            textDeletedStatus = itemView.findViewById(R.id.textDeletedStatus);
        }

        public void bind(Message message, int viewType, ChatWindowActivity chatActivity, MessagesAdapter adapter) {

            // Reset all containers to GONE
            messageText.setVisibility(View.GONE);
            messageImage.setVisibility(View.GONE);
            if (fileContainer != null) fileContainer.setVisibility(View.GONE);
            if (audioContainer != null) audioContainer.setVisibility(View.GONE);
            if (textDeletedStatus != null) textDeletedStatus.setVisibility(View.GONE);
            textTimestamp.setVisibility(View.VISIBLE); // Assume visible initially
            if (iconSeenStatus != null) iconSeenStatus.setVisibility(View.GONE); // Reset seen status visibility

            // --- DELETION LOGIC (Status 1) ---
            if (message.getStatus() == 1) {
                if (textDeletedStatus != null) {
                    textDeletedStatus.setText("Message deleted");
                    textDeletedStatus.setVisibility(View.VISIBLE);
                    textTimestamp.setVisibility(View.GONE);
                    itemView.setOnLongClickListener(null); // Disable long press on deleted message
                    return;
                }
            }

            // --- MEDIA/FILE TYPE HANDLING (for active messages only) ---
            String type = message.getType();
            String content = message.getContent();

            if ("image".equals(type)) {
                messageImage.setVisibility(View.VISIBLE);

                RequestOptions requestOptions = new RequestOptions().transform(new RoundedCorners(32));
                Glide.with(itemView.getContext())
                        .load(content)
                        .apply(requestOptions)
                        .into(messageImage);

                messageImage.setOnClickListener(v -> {
                    Intent intent = new Intent(v.getContext(), FullScreenImageActivity.class);
                    intent.putExtra("imageUrl", content);
                    v.getContext().startActivity(intent);
                });

            } else if ("file".equals(type)) {
                if (fileContainer != null && textFileName != null && iconFileDownload != null) {
                    fileContainer.setVisibility(View.VISIBLE);
                    // Assuming getFileName() is a method on the Message class
                    textFileName.setText(message.getFileName());

                    iconFileDownload.setOnClickListener(v -> {
                        // Assuming DownloadManagerUtils is available
                        // DownloadManagerUtils.startDownload(v.getContext(), content, message.getFileName());
                        Toast.makeText(v.getContext(), "Download function called (placeholder).", Toast.LENGTH_SHORT).show();
                    });
                }

            } else if ("audio".equals(type)) {
                if (audioContainer != null && iconAudioPlay != null) {
                    audioContainer.setVisibility(View.VISIBLE);

                    if (adapter.currentlyPlayingUrl != null && adapter.currentlyPlayingUrl.equals(content)) {
                        iconAudioPlay.setImageResource(R.drawable.ic_pause);
                        adapter.currentAudioHolder = this;
                    } else {
                        iconAudioPlay.setImageResource(R.drawable.ic_play_arrow);
                    }

                    iconAudioPlay.setOnClickListener(v -> {
                        if (adapter.currentlyPlayingUrl != null && adapter.currentlyPlayingUrl.equals(content)) {
                            adapter.stopPlayback();
                        } else {
                            adapter.startPlayback(this, content);
                        }
                    });
                }

            } else {
                // Default: Text message
                messageText.setVisibility(View.VISIBLE);
                messageText.setText(content);
            }

            // --- MESSAGE OPTIONS TRIGGER (Long Press) ---
            itemView.setOnLongClickListener(v -> {
                if (chatActivity != null) {
                    // FIX: Changed showDeleteOptions to showMessageOptions
                    chatActivity.showMessageOptions(message);
                    return true;
                }
                Toast.makeText(v.getContext(), "Error: Activity context missing.", Toast.LENGTH_SHORT).show();
                return false;
            });
            // ---------------------------------------------

            // Bind Timestamp
            SimpleDateFormat formatter = new SimpleDateFormat("h:mm a", Locale.getDefault());
            textTimestamp.setText(formatter.format(new Date(message.getTimestamp())));

            // Bind Seen Status (ONLY for sent messages)
            if (viewType == VIEW_TYPE_SENT) {
                if (iconSeenStatus == null) return;
                iconSeenStatus.setVisibility(View.VISIBLE);
                Context context = itemView.getContext();

                if (message.isSeen()) {
                    DrawableCompat.setTint(
                            iconSeenStatus.getDrawable(),
                            ContextCompat.getColor(context, R.color.deep_purple) // Replace R.color.deep_purple
                    );
                } else {
                    DrawableCompat.setTint(
                            iconSeenStatus.getDrawable(),
                            ContextCompat.getColor(context, R.color.inactive_gray) // Replace R.color.inactive_gray
                    );
                }
            } else {
                if (iconSeenStatus != null) {
                    iconSeenStatus.setVisibility(View.GONE);
                }
            }
        }
    }
}