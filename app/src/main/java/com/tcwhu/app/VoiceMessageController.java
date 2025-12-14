package com.tcwhu.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import java.io.File;
import android.net.Uri;

public class VoiceMessageController implements View.OnTouchListener {

    private static final String TAG = "VoiceMsgController";
    private static final int MAX_DURATION_SECONDS = 60;
    private static final int MAX_DURATION_MILLIS = MAX_DURATION_SECONDS * 1000;
    private static final int MIN_DURATION_MILLIS = 1000;
    private static final int UPDATE_INTERVAL_MILLIS = 100;
    private static final float SLIDE_CANCEL_THRESHOLD_DP = 150f;

    private final Context context;
    private final ChatFileUploader fileUploader;
    private final TextView timerTextView;
    private final TextView slideToCancelText;
    private final ImageButton micButton;
    private final LinearLayout voiceRecordingOverlay;
    private final LinearLayout inputContainer;
    private final float slideCancelThresholdPx;

    private ChatRecorder audioRecorder;
    private long recordingStartTime = 0;
    private boolean isRecording = false;
    private float touchStartY;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRecording) {
                long elapsedTime = System.currentTimeMillis() - recordingStartTime;
                updateTimerUI(elapsedTime);

                if (elapsedTime >= MAX_DURATION_MILLIS) {
                    stopRecording(true);
                } else {
                    handler.postDelayed(this, UPDATE_INTERVAL_MILLIS);
                }
            }
        }
    };

    public VoiceMessageController(Context context, ChatFileUploader fileUploader, ImageButton micButton,
                                  TextView timerTextView, LinearLayout voiceRecordingOverlay, LinearLayout inputContainer,
                                  TextView slideToCancelText) {
        this.context = context;
        this.fileUploader = fileUploader;
        this.timerTextView = timerTextView;
        this.micButton = micButton;
        this.voiceRecordingOverlay = voiceRecordingOverlay;
        this.inputContainer = inputContainer;
        this.slideToCancelText = slideToCancelText;
        this.slideCancelThresholdPx = SLIDE_CANCEL_THRESHOLD_DP * context.getResources().getDisplayMetrics().density;
    }

    private void updateTimerUI(long durationMillis) {
        int totalSeconds = (int) (durationMillis / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        timerTextView.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private void startRecording() {
        if (isRecording) return;

        inputContainer.setVisibility(View.GONE);
        voiceRecordingOverlay.setVisibility(View.VISIBLE);

        String outputFilePath = new File(context.getCacheDir(), "audio_record_" + System.currentTimeMillis() + ".aac").getAbsolutePath();

        audioRecorder = new ChatRecorder(context, outputFilePath, MAX_DURATION_MILLIS);
        try {
            audioRecorder.start();
            isRecording = true;
            recordingStartTime = System.currentTimeMillis();

            micButton.setImageResource(R.drawable.ic_mic);
            timerTextView.setText("00:00");
            slideToCancelText.setText("← Slide up to cancel");
            handler.post(timerRunnable);

        } catch (Exception e) {
            Toast.makeText(context, "Failed to start recording.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Recording start error", e);
            cleanupUI();
            audioRecorder = null;
            isRecording = false;
        }
    }

    private void cleanupUI() {
        micButton.setImageResource(R.drawable.ic_mic);
        voiceRecordingOverlay.setVisibility(View.GONE);
        inputContainer.setVisibility(View.VISIBLE);
    }

    private void stopRecording(boolean shouldSend) {
        if (!isRecording) return;

        handler.removeCallbacks(timerRunnable);
        isRecording = false;
        cleanupUI();

        if (audioRecorder != null) {
            try {
                long recordingDuration = System.currentTimeMillis() - recordingStartTime;
                String filePath = audioRecorder.stop();

                if (filePath != null) {
                    if (recordingDuration < MIN_DURATION_MILLIS) {
                        new File(filePath).delete();
                        Toast.makeText(context, "Recording too short.", Toast.LENGTH_SHORT).show();
                    } else if (shouldSend) {
                        Uri fileUri = Uri.fromFile(new File(filePath));
                        fileUploader.handleFilePickerResult(fileUri, recordingDuration);
                        Toast.makeText(context, "Voice Message Sent.", Toast.LENGTH_SHORT).show();
                    } else {
                        new File(filePath).delete();
                        Toast.makeText(context, "Recording Cancelled.", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Recording stop error", e);
            } finally {
                audioRecorder = null;
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!isRecording) {
                    touchStartY = event.getRawY();
                    startRecording();
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isRecording) {
                    float currentY = event.getRawY();
                    float dy = touchStartY - currentY;

                    if (dy > slideCancelThresholdPx) {
                        stopRecording(false);
                        return true;
                    } else if (dy > 50) {
                        slideToCancelText.setText("Release to Send");
                    } else {
                        slideToCancelText.setText("← Slide up to cancel");
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isRecording) {
                    stopRecording(true);
                }
                return true;
        }
        return false;
    }

    public void cleanup() {
        handler.removeCallbacks(timerRunnable);
        if (audioRecorder != null) {
            try {
                audioRecorder.stop();
            } catch (Exception ignored) {}
        }
    }
}