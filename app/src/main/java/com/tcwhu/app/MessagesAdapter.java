package com.tcwhu.app;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler; // <--- ADDED
import android.os.Looper; // <--- ADDED
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar; // <--- ADDED
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
    private final Handler playbackHandler = new Handler(Looper.getMainLooper()); // <--- ADDED HANDLER

    // Runnable for updating the progress bar and time text
    private final Runnable updatePlaybackProgress = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying() && currentAudioHolder != null) {
                int currentPosition = mediaPlayer.getCurrentPosition();
                int duration = mediaPlayer.getDuration();

                // Set max duration for SeekBar (in milliseconds)
                currentAudioHolder.audioSeekBar.setMax(duration);
                currentAudioHolder.audioSeekBar.setProgress(currentPosition);

                // Update time display: 0:0X / 0:YY
                currentAudioHolder.textAudioDuration.setText(
                        formatDuration(currentPosition) + " / " + formatDuration(duration)
                );

                playbackHandler.postDelayed(this, 100); // Update every 100ms
            }
        }
    };
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
        playbackHandler.removeCallbacks(updatePlaybackProgress); // <--- CLEANUP HANDLER
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

            holder.iconAudioPlay.setImageResource(R.drawable.ic_pause);
            currentAudioHolder = holder;
            currentlyPlayingUrl = url;

            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();

                // Use the duration from the MediaPlayer after preparation
                int duration = mp.getDuration();
                currentAudioHolder.audioSeekBar.setMax(duration);
                currentAudioHolder.audioSeekBar.setProgress(0);

                // Display the correct full duration immediately (0:00 / 0:XX)
                currentAudioHolder.textAudioDuration.setText(
                        formatDuration(0) + " / " + formatDuration(duration)
                );

                playbackHandler.post(updatePlaybackProgress); // Start progress updates
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                // Reset UI on completion
                currentAudioHolder.audioSeekBar.setProgress(0);
                // Display only the total duration (0:XX)
                currentAudioHolder.textAudioDuration.setText(formatDuration(currentAudioHolder.message.getMediaDuration()));
                stopPlayback();
            });

        } catch (IOException e) {
            Toast.makeText(holder.itemView.getContext(), "Error playing audio.", Toast.LENGTH_SHORT).show();
            stopPlayback();
        }
    }

    public void stopPlayback() {
        playbackHandler.removeCallbacks(updatePlaybackProgress); // Stop progress updates

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (currentAudioHolder != null) {
            currentAudioHolder.iconAudioPlay.setImageResource(R.drawable.ic_play_arrow);
            // On stop, reset progress bar and display end time
            currentAudioHolder.audioSeekBar.setProgress(0);
            currentAudioHolder.textAudioDuration.setText(formatDuration(currentAudioHolder.message.getMediaDuration()));
            currentAudioHolder = null;
        }
        currentlyPlayingUrl = null;
    }

    /**
     * Formats duration in milliseconds to "m:ss" or "mm:ss"
     */
    private String formatDuration(long durationMillis) {
        int totalSeconds = (int) (durationMillis / 1000);
        int seconds = totalSeconds % 60;
        int minutes = totalSeconds / 60;

        if (minutes > 0) {
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "0:%02d", seconds);
        }
    }

    // --- MessageViewHolder Class ---
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        // --- TEXT/IMAGE UI ---
        TextView messageText, textTimestamp, textFileName, textDeletedStatus;
        ImageView messageImage, iconSeenStatus, iconFileDownload;
        LinearLayout fileContainer;

        // --- AUDIO UI ---
        TextView textAudioDuration; // <--- DURATION TEXT VIEW
        ImageView iconAudioPlay;
        LinearLayout audioContainer;
        SeekBar audioSeekBar; // <--- SEEKBAR

        Message message;

        private final MessagesAdapter adapter;
        private final MessageOptionHandler messageOptionHandler;

        public MessageViewHolder(@NonNull View itemView, MessagesAdapter adapter, MessageOptionHandler messageOptionHandler) {
            super(itemView);
            this.adapter = adapter;
            this.messageOptionHandler = messageOptionHandler;

            // --- Find Common Views ---
            messageText = itemView.findViewById(R.id.messageText);
            messageImage = itemView.findViewById(R.id.messageImage);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            iconSeenStatus = itemView.findViewById(R.id.iconSeenStatus);
            fileContainer = itemView.findViewById(R.id.fileContainer);
            textFileName = itemView.findViewById(R.id.textFileName);
            iconFileDownload = itemView.findViewById(R.id.iconFileDownload);
            textDeletedStatus = itemView.findViewById(R.id.textDeletedStatus);

            // --- Find Audio Views ---
            audioContainer = itemView.findViewById(R.id.audioContainer);
            iconAudioPlay = itemView.findViewById(R.id.iconAudioPlay);
            audioSeekBar = itemView.findViewById(R.id.audioSeekBar);
            textAudioDuration = itemView.findViewById(R.id.textAudioDuration);

            // --- SEEKBAR LISTENER ---
            if (audioSeekBar != null) {
                audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (message == null) return; // Check added for safety during binding

                        long totalDuration = message.getMediaDuration();

                        if (fromUser && adapter.mediaPlayer != null && adapter.currentAudioHolder == MessageViewHolder.this) {
                            // User is dragging and this is the currently playing message
                            adapter.mediaPlayer.seekTo(progress);
                            textAudioDuration.setText(adapter.formatDuration(progress) + " / " + adapter.formatDuration(totalDuration));
                        } else if (fromUser) {
                            // User is dragging a seekbar on a non-playing message
                            textAudioDuration.setText(adapter.formatDuration(progress) + " / " + adapter.formatDuration(totalDuration));
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        if (adapter.mediaPlayer != null && adapter.currentAudioHolder == MessageViewHolder.this) {
                            adapter.playbackHandler.removeCallbacks(adapter.updatePlaybackProgress);
                        }
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        if (adapter.mediaPlayer != null && adapter.currentAudioHolder == MessageViewHolder.this) {
                            adapter.playbackHandler.post(adapter.updatePlaybackProgress);
                        }
                    }
                });
            }
            // ------------------------
        }

        public void bind(Message message, int viewType) {
            this.message = message; // Store for duration access

            // Reset all containers to GONE
            messageText.setVisibility(View.GONE);
            messageImage.setVisibility(View.GONE);
            if (fileContainer != null) fileContainer.setVisibility(View.GONE);
            if (audioContainer != null) audioContainer.setVisibility(View.GONE);
            if (textDeletedStatus != null) textDeletedStatus.setVisibility(View.GONE);
            textTimestamp.setVisibility(View.VISIBLE);
            if (iconSeenStatus != null) iconSeenStatus.setVisibility(View.GONE);

            // Set up long press listener for options
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
                    itemView.setOnLongClickListener(null);
                    return;
                }
            }

            // --- MEDIA/FILE TYPE HANDLING ---
            String type = message.getType() != null ? message.getType().toLowerCase() : "";
            final String content = message.getContent();
            final String fileName = message.getFileName() != null ? message.getFileName() : type + "_" + message.getTimestamp();

            boolean isMediaHandled = false;

            // --- IMAGE ---
            if ("image".equals(type)) {
                messageImage.setVisibility(View.VISIBLE);
                isMediaHandled = true;

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

                // --- AUDIO / FILE / VIDEO ---
            } else if ("file".equals(type) || "video".equals(type) || "audio".equals(type)) {

                if (content != null && content.startsWith("http")) {
                    isMediaHandled = true;

                    // Display as a file container for download (Files, Audio, and Video)
                    if (fileContainer != null && textFileName != null && iconFileDownload != null) {

                        // For Audio, we prioritize the dedicated audio player view,
                        // so we only show the file container if it's not audio
                        if (!"audio".equals(type)) {
                            fileContainer.setVisibility(View.VISIBLE);
                        }

                        // Set text based on actual type
                        String displayFileName;
                        if ("file".equals(type)) {
                            displayFileName = fileName;
                        } else if ("video".equals(type)) {
                            displayFileName = "[Video] " + fileName;
                        } else if ("audio".equals(type)) {
                            displayFileName = "[Audio Message]"; // Often simplified for voice notes
                        } else {
                            displayFileName = fileName;
                        }

                        textFileName.setText(displayFileName);

                        // UNIVERSAL DOWNLOAD CLICK HANDLER (for file/video)
                        View downloadTarget = ("file".equals(type) || "video".equals(type)) ? fileContainer : iconFileDownload;
                        downloadTarget.setOnClickListener(v -> {
                            if (content != null && fileName != null) {
                                // DownloadManagerUtils.startDownload(v.getContext(), content, fileName);
                                Toast.makeText(v.getContext(), "Download started for: " + displayFileName, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(v.getContext(), "File URL or name missing.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    // Specific Audio Playback Control
                    if ("audio".equals(type) && audioContainer != null && iconAudioPlay != null) {
                        audioContainer.setVisibility(View.VISIBLE);

                        long durationMillis = message.getMediaDuration();

                        // Set duration display and max progress
                        if (durationMillis > 0) {
                            audioSeekBar.setMax((int) durationMillis);
                            textAudioDuration.setText(adapter.formatDuration(durationMillis));
                        } else {
                            textAudioDuration.setText("--:--");
                        }
                        audioSeekBar.setProgress(0);

                        // Handle audio playback icons
                        if (adapter.currentlyPlayingUrl != null && adapter.currentlyPlayingUrl.equals(content)) {
                            iconAudioPlay.setImageResource(R.drawable.ic_pause);
                            adapter.currentAudioHolder = this;
                            adapter.playbackHandler.post(adapter.updatePlaybackProgress);
                        } else {
                            iconAudioPlay.setImageResource(R.drawable.ic_play_arrow);
                        }

                        // Play/Pause Click Handler
                        iconAudioPlay.setOnClickListener(v -> {
                            if (adapter.currentlyPlayingUrl != null && adapter.currentlyPlayingUrl.equals(content)) {
                                adapter.stopPlayback();
                            } else {
                                adapter.startPlayback(this, content);
                            }
                        });

                        // Long press to download audio
                        audioContainer.setOnLongClickListener(v -> {
                            Toast.makeText(v.getContext(), "Download started for Audio.", Toast.LENGTH_SHORT).show();
                            return true;
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

            // Bind Seen Status
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