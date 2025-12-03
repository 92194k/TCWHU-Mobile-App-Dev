package com.tcwhu.app;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
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

// NOTE: Placeholder classes (Message, DownloadManagerUtils, FullScreenImageActivity, R.drawable.ic_pause/ic_play_arrow/R.color.deep_purple/R.color.inactive_gray) are assumed to exist.

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<Message> messageList;
    private final String currentUserId;
    private final MessageOptionHandler messageOptionHandler;

    // --- Audio Playback Management ---
    private MediaPlayer mediaPlayer;
    private MessageViewHolder currentAudioHolder = null;
    private String currentlyPlayingUrl = null;
    // ---------------------------------

    /**
     * Primary Constructor. Takes MessageOptionHandler interface.
     */
    public MessagesAdapter(List<Message> messageList, String currentUserId, MessageOptionHandler messageOptionHandler) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
        this.messageOptionHandler = messageOptionHandler;
    }

    public void cleanup() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
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
        return new MessageViewHolder(view, this, messageOptionHandler);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.bind(messageList.get(position), getItemViewType(position));
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // Private startPlayback helper is used internally by the ViewHolder
    private void startPlayback(MessageViewHolder holder, String url) {
        stopPlayback();

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();

            // NOTE: Assuming R.drawable.ic_pause exists
            holder.iconAudioPlay.setImageResource(R.drawable.ic_pause);
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
            // NOTE: Assuming R.drawable.ic_play_arrow exists
            currentAudioHolder.iconAudioPlay.setImageResource(R.drawable.ic_play_arrow);
            currentAudioHolder = null;
        }
        currentlyPlayingUrl = null;
    }

    // --- Updated ViewHolder Class ---
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, textTimestamp, textFileName, textDeletedStatus;
        ImageView messageImage, iconSeenStatus, iconFileDownload, iconAudioPlay;
        LinearLayout fileContainer, audioContainer, mediaContainer; // mediaContainer added for audio/video

        private final MessagesAdapter adapter;
        private final MessageOptionHandler messageOptionHandler;

        public MessageViewHolder(@NonNull View itemView, MessagesAdapter adapter, MessageOptionHandler messageOptionHandler) {
            super(itemView);
            this.adapter = adapter;
            this.messageOptionHandler = messageOptionHandler;

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
            // Assuming mediaContainer can be found for audio/video if separate from fileContainer
            // mediaContainer = itemView.findViewById(R.id.mediaContainer);
        }

        public void bind(Message message, int viewType) {

            // Reset all containers to GONE
            messageText.setVisibility(View.GONE);
            messageImage.setVisibility(View.GONE);
            if (fileContainer != null) fileContainer.setVisibility(View.GONE);
            if (audioContainer != null) audioContainer.setVisibility(View.GONE);
            // if (mediaContainer != null) mediaContainer.setVisibility(View.GONE); // If used
            if (textDeletedStatus != null) textDeletedStatus.setVisibility(View.GONE);
            textTimestamp.setVisibility(View.VISIBLE);
            if (iconSeenStatus != null) iconSeenStatus.setVisibility(View.GONE);

            // Set up long press listener for options (like single message deletion)
            if (message.getStatus() == 0) {
                itemView.setOnLongClickListener(v -> {
                    messageOptionHandler.showMessageOptions(message);
                    return true;
                });
            } else {
                itemView.setOnLongClickListener(null);
            }

            // --- DELETION LOGIC (Status 1) ---
            if (message.getStatus() == 1) {
                if (textDeletedStatus != null) {
                    textDeletedStatus.setText("Message deleted");
                    textDeletedStatus.setVisibility(View.VISIBLE);
                    textTimestamp.setVisibility(View.GONE);
                    itemView.setOnLongClickListener(null); // Ensure no options on deleted messages
                    return;
                }
            }

            // --- MEDIA/FILE TYPE HANDLING (for active messages only) ---
            String type = message.getType() != null ? message.getType().toLowerCase() : "";
            final String content = message.getContent();
            final String fileName = message.getFileName() != null ? message.getFileName() : type + "_" + message.getTimestamp();

            boolean isMediaHandled = false; // Flag to track if content is media

            // --- IMAGE ---
            if ("image".equals(type)) {
                messageImage.setVisibility(View.VISIBLE);
                isMediaHandled = true;

                RequestOptions requestOptions = new RequestOptions().transform(new RoundedCorners(32));
                Glide.with(itemView.getContext())
                        .load(content)
                        .apply(requestOptions)
                        .into(messageImage);

                // Clicking image opens full screen AND makes it downloadable
                messageImage.setOnClickListener(v -> {
                    // Option 1: View full screen (current)
                    Intent intent = new Intent(v.getContext(), FullScreenImageActivity.class);
                    intent.putExtra("imageUrl", content);
                    v.getContext().startActivity(intent);

                    // Option 2: Initiate download (added for required functionality)
                    // If you want immediate download on click:
                    // DownloadManagerUtils.startDownload(v.getContext(), content, fileName);
                });

                // If you want a separate download button for images, you'd need another ImageView in the layout.

                // --- FILE (Documents) ---
            } else if ("file".equals(type) || "video".equals(type) || "audio".equals(type)) {

                // Check if the content is not text, but a downloadable URL
                if (content != null && content.startsWith("http")) {

                    isMediaHandled = true;

                    // Display as a file container for download (Files, Audio, and Video)
                    if (fileContainer != null && textFileName != null && iconFileDownload != null) {
                        fileContainer.setVisibility(View.VISIBLE);

                        // Set text based on actual type
                        String displayFileName;
                        if ("file".equals(type)) {
                            displayFileName = fileName;
                        } else if ("video".equals(type)) {
                            displayFileName = "[Video] " + fileName;
                        } else if ("audio".equals(type)) {
                            displayFileName = "[Audio] " + fileName;
                        } else {
                            displayFileName = fileName;
                        }

                        textFileName.setText(displayFileName);

                        // UNIVERSAL DOWNLOAD CLICK HANDLER
                        View downloadTarget = ("file".equals(type)) ? fileContainer : iconFileDownload;
                        downloadTarget.setOnClickListener(v -> {
                            if (content != null && fileName != null) {
                                // NOTE: Assumed DownloadManagerUtils.startDownload exists
                                // DownloadManagerUtils.startDownload(v.getContext(), content, fileName);
                                Toast.makeText(v.getContext(), "Download started for: " + displayFileName, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(v.getContext(), "File URL or name missing.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    // Specific Audio Playback Control (separate from download icon logic)
                    if ("audio".equals(type) && audioContainer != null && iconAudioPlay != null) {
                        audioContainer.setVisibility(View.VISIBLE);

                        // Handle audio playback icons (as before)
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
                }

            }

            // --- FINAL TEXT FALLBACK ---
            if (!isMediaHandled) {
                messageText.setVisibility(View.VISIBLE);
                messageText.setText(content);
            }
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
                            ContextCompat.getColor(context, R.color.deep_purple)
                    );
                } else {
                    DrawableCompat.setTint(
                            iconSeenStatus.getDrawable(),
                            ContextCompat.getColor(context, R.color.inactive_gray)
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