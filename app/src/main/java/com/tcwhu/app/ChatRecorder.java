package com.tcwhu.app;

import android.content.Context;
import android.media.MediaRecorder;
import java.io.File;
import java.io.IOException;

public class ChatRecorder {

    private MediaRecorder recorder;
    private String outputFilePath;

    public ChatRecorder(Context context, String outputFilePath, int maxDurationMillis) {
        this.outputFilePath = outputFilePath;
        setupRecorder(maxDurationMillis);
    }

    private void setupRecorder(int maxDurationMillis) {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioSamplingRate(44100);
        recorder.setAudioEncodingBitRate(128000);
        recorder.setMaxDuration(maxDurationMillis);
        recorder.setOutputFile(outputFilePath);
    }

    public void start() throws IOException {
        File file = new File(outputFilePath);
        if (file.exists()) file.delete();
        file.createNewFile();

        recorder.prepare();
        recorder.start();
    }

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