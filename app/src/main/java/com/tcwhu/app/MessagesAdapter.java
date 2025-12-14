package com.tcwhu.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
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

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<Message> messageList;
    private final String currentUserId;
    private final MessageOptionHandler messageOptionHandler;
    private final DownloadRequestListener downloadListener;

    // Audio Playback
    private MediaPlayer mediaPlayer;
    private MessageViewHolder currentAudioHolder = null;
    private String currentlyPlayingUrl = null;
    private final Handler playbackHandler = new Handler(Looper.getMainLooper());

    private final Runnable updatePlaybackProgress = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying() && currentAudioHolder != null) {
                int currentPosition = mediaPlayer.getCurrentPosition();
                int duration = mediaPlayer.getDuration();
                currentAudioHolder.audioSeekBar.setMax(duration);
                currentAudioHolder.audioSeekBar.setProgress(currentPosition);

                if (duration > 0) {
                    currentAudioHolder.textAudioDuration.setText(
                            formatDuration(currentPosition) + " / " + formatDuration(duration)
                    );
                }
                playbackHandler.postDelayed(this, 100);
            }
        }
    };

    public MessagesAdapter(List<Message> messageList, String currentUserId,
                           MessageOptionHandler messageOptionHandler, DownloadRequestListener downloadListener) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
        this.messageOptionHandler = messageOptionHandler;
        this.downloadListener = downloadListener;
    }

    public void cleanup() {
        playbackHandler.removeCallbacks(updatePlaybackProgress);
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
                int duration = mp.getDuration();
                currentAudioHolder.audioSeekBar.setMax(duration);
                currentAudioHolder.audioSeekBar.setProgress(0);
                currentAudioHolder.textAudioDuration.setText(formatDuration(0) + " / " + formatDuration(duration));
                playbackHandler.post(updatePlaybackProgress);
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                currentAudioHolder.audioSeekBar.setProgress(0);
                currentAudioHolder.textAudioDuration.setText(formatDuration(currentAudioHolder.message.getMediaDuration()));
                stopPlayback();
            });
        } catch (IOException e) {
            Toast.makeText(holder.itemView.getContext(), "Error playing audio.", Toast.LENGTH_SHORT).show();
            stopPlayback();
        }
    }

    public void stopPlayback() {
        playbackHandler.removeCallbacks(updatePlaybackProgress);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (currentAudioHolder != null) {
            currentAudioHolder.iconAudioPlay.setImageResource(R.drawable.ic_play_arrow);
            currentAudioHolder.audioSeekBar.setProgress(0);
            currentAudioHolder.textAudioDuration.setText(formatDuration(currentAudioHolder.message.getMediaDuration()));
            currentAudioHolder = null;
        }
        currentlyPlayingUrl = null;
    }

    private String formatDuration(long durationMillis) {
        int totalSeconds = (int) (durationMillis / 1000);
        int seconds = totalSeconds % 60;
        int minutes = totalSeconds / 60;
        if (minutes > 0) return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
        else return String.format(Locale.getDefault(), "0:%02d", seconds);
    }

    private String getMimeTypeFromFileName(String fileName) {
        if (fileName == null) return "*/*";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot != -1) {
            String extension = fileName.substring(lastDot + 1).toLowerCase();
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) return mime;
        }
        return "*/*";
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, textTimestamp, textFileName, textDeletedStatus, textAudioDuration;
        ImageView messageImage, iconSeenStatus, iconFileDownload, iconFile;
        LinearLayout fileContainer;
        ImageView iconAudioPlay;
        LinearLayout audioContainer;
        SeekBar audioSeekBar;
        Message message;
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
            textDeletedStatus = itemView.findViewById(R.id.textDeletedStatus);

            audioContainer = itemView.findViewById(R.id.audioContainer);
            iconAudioPlay = itemView.findViewById(R.id.iconAudioPlay);
            audioSeekBar = itemView.findViewById(R.id.audioSeekBar);
            textAudioDuration = itemView.findViewById(R.id.textAudioDuration);
            iconFile = itemView.findViewById(R.id.iconFile);

            if (audioSeekBar != null) {
                audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (message == null) return;
                        long totalDuration = message.getMediaDuration();
                        if (fromUser && adapter.mediaPlayer != null && adapter.currentAudioHolder == MessageViewHolder.this) {
                            adapter.mediaPlayer.seekTo(progress);
                            textAudioDuration.setText(adapter.formatDuration(progress) + " / " + adapter.formatDuration(totalDuration));
                        } else if (fromUser) {
                            textAudioDuration.setText(adapter.formatDuration(progress) + " / " + adapter.formatDuration(totalDuration));
                        }
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {
                        if (adapter.mediaPlayer != null && adapter.currentAudioHolder == MessageViewHolder.this)
                            adapter.playbackHandler.removeCallbacks(adapter.updatePlaybackProgress);
                    }
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {
                        if (adapter.mediaPlayer != null && adapter.currentAudioHolder == MessageViewHolder.this)
                            adapter.playbackHandler.post(adapter.updatePlaybackProgress);
                    }
                });
            }
        }

        public void bind(Message message, int viewType) {
            this.message = message;

            messageText.setVisibility(View.GONE);
            messageImage.setVisibility(View.GONE);
            if (fileContainer != null) fileContainer.setVisibility(View.GONE);
            if (audioContainer != null) audioContainer.setVisibility(View.GONE);
            if (textDeletedStatus != null) textDeletedStatus.setVisibility(View.GONE);

            textTimestamp.setVisibility(View.VISIBLE);
            if (iconSeenStatus != null) iconSeenStatus.setVisibility(View.GONE);

            // Handle Deleted Messages
            if (message.getStatus() == 1) {
                if (textDeletedStatus != null) {
                    textDeletedStatus.setText(message.getContent());
                    textDeletedStatus.setVisibility(View.VISIBLE);
                    textTimestamp.setVisibility(View.GONE);
                    itemView.setOnLongClickListener(null);
                    return;
                }
            }

            itemView.setOnLongClickListener(v -> {
                messageOptionHandler.showMessageOptions(message);
                return true;
            });

            String type = message.getType() != null ? message.getType().toLowerCase() : "";
            final String content = message.getContent();
            String tempFileName = message.getFileName() != null ? message.getFileName() : content;

            if (tempFileName.endsWith(".docs")) {
                tempFileName = tempFileName.replace(".docs", ".docx");
            }
            final String fileName = tempFileName;

            boolean isMediaHandled = false;
            final String mimeType = adapter.getMimeTypeFromFileName(fileName);

            if ("image".equals(type) && messageImage != null) {
                messageImage.setVisibility(View.VISIBLE);
                isMediaHandled = true;
                Glide.with(itemView.getContext()).load(content)
                        .apply(new RequestOptions().transform(new RoundedCorners(32))).into(messageImage);
                messageImage.setOnClickListener(v -> {
                    Intent intent = new Intent(v.getContext(), FullScreenImageActivity.class);
                    intent.putExtra("imageUrl", content);
                    v.getContext().startActivity(intent);
                });
            } else if ("file".equals(type) || "video".equals(type) || "audio".equals(type)) {
                isMediaHandled = true;

                if ("audio".equals(type) && audioContainer != null) {
                    audioContainer.setVisibility(View.VISIBLE);
                    long durationMillis = message.getMediaDuration();
                    if (durationMillis > 0) {
                        audioSeekBar.setMax((int) durationMillis);
                        textAudioDuration.setText(adapter.formatDuration(durationMillis));
                    } else textAudioDuration.setText("--:--");
                    audioSeekBar.setProgress(0);

                    if (adapter.currentlyPlayingUrl != null && adapter.currentlyPlayingUrl.equals(content)) {
                        iconAudioPlay.setImageResource(R.drawable.ic_pause);
                        adapter.currentAudioHolder = this;
                        adapter.playbackHandler.post(adapter.updatePlaybackProgress);
                    } else iconAudioPlay.setImageResource(R.drawable.ic_play_arrow);

                    iconAudioPlay.setOnClickListener(v -> {
                        if (adapter.currentlyPlayingUrl != null && adapter.currentlyPlayingUrl.equals(content)) adapter.stopPlayback();
                        else adapter.startPlayback(this, content);
                    });

                } else if (fileContainer != null) {
                    fileContainer.setVisibility(View.VISIBLE);
                    textFileName.setText(fileName);

                    if (iconFile != null) {
                        String lowerName = fileName.toLowerCase();
                        if (lowerName.endsWith(".pdf") || lowerName.endsWith(".doc") || lowerName.endsWith(".docx") || lowerName.endsWith(".xls"))
                            iconFile.setImageResource(R.drawable.ic_file_document);
                        else iconFile.setImageResource(R.drawable.ic_file_document);
                    }

                    if (iconFileDownload != null) {
                        iconFileDownload.setVisibility(View.VISIBLE);
                        iconFileDownload.setOnClickListener(v -> adapter.downloadListener.onFileDownloadRequested(content, fileName, mimeType));
                    }
                }
            }

            if (!isMediaHandled) {
                messageText.setVisibility(View.VISIBLE);
                messageText.setText(content);
            }

            SimpleDateFormat formatter = new SimpleDateFormat("h:mm a", Locale.getDefault());
            textTimestamp.setText(formatter.format(new Date(message.getTimestamp())));

            if (viewType == VIEW_TYPE_SENT && iconSeenStatus != null) {
                iconSeenStatus.setVisibility(View.VISIBLE);
                Context context = itemView.getContext();
                Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_check_double);

                if (drawable != null) {
                    drawable = DrawableCompat.wrap(drawable.mutate());
                    if (message.isSeen()) {
                        DrawableCompat.setTint(drawable, ContextCompat.getColor(context, R.color.deep_purple));
                    } else {
                        DrawableCompat.setTint(drawable, ContextCompat.getColor(context, R.color.inactive_gray));
                    }
                    iconSeenStatus.setImageDrawable(drawable);
                }
            } else {
                if (iconSeenStatus != null) iconSeenStatus.setVisibility(View.GONE);
            }
        }
    }
}