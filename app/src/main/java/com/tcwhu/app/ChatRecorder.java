package com.tcwhu.app;

import android.content.Context;
import android.media.MediaRecorder;
import java.io.File;
import java.io.IOException;

public class ChatRecorder {

    private MediaRecorder recorder;
    private String outputFilePath;

    /**
     * @param context Application context.
     * @param outputFilePath The path where the audio file will be saved.
     * @param maxDurationMillis The maximum time allowed for the recording in milliseconds.
     */
    public ChatRecorder(Context context, String outputFilePath, int maxDurationMillis) {
        this.outputFilePath = outputFilePath;
        setupRecorder(maxDurationMillis);
    }

    private void setupRecorder(int maxDurationMillis) {
        recorder = new MediaRecorder();

        // --- Finer Sound Quality Configuration (Maximized AAC Settings) ---
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);

        // Output Format: AAC_ADTS
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        // High quality settings for finer sound:
        recorder.setAudioSamplingRate(44100);
        recorder.setAudioEncodingBitRate(128000);
        // ------------------------------------------------------------------

        // Set the maximum duration
        recorder.setMaxDuration(maxDurationMillis);

        recorder.setOutputFile(outputFilePath);
    }

    public void start() throws IOException {
        // Ensure the file path is ready
        File file = new File(outputFilePath);
        if (file.exists()) file.delete();
        file.createNewFile();

        recorder.prepare();
        recorder.start();
    }

    /**
     * Stops the recorder and returns the path to the saved file.
     * @return The path to the recorded audio file.
     */
    public String stop() throws IllegalStateException {
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
            return outputFilePath;
        }
        return null;
    }
}