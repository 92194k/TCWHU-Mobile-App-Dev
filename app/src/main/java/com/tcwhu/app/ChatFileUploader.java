package com.tcwhu.app;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatFileUploader {

    private static final String TAG = "ChatFileUploader";
    private static final long MAX_FILE_SIZE_MB = 25;
    private static final long MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;

    private final Context context;
    private final String chatId;
    private final ChatWindowCallbacks callbacks;
    private final FileUploadCompletionListener listener;

    public interface FileUploadCompletionListener {
        void onFileUploadCompleted(String type, String content, String fileName, long durationMillis);
    }

    public ChatFileUploader(Context context, String chatId, ChatWindowCallbacks callbacks, FileUploadCompletionListener listener) {
        this.context = context;
        this.chatId = chatId;
        this.callbacks = callbacks;
        this.listener = listener;
        initCloudinary();
    }

    private void initCloudinary() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dggeonpfw");
            config.put("api_key", "147481881754886");
            config.put("api_secret", "583Dz7vp2y6TRaDBuCj8HbHoQX4");
            MediaManager.init(context, config);
        } catch (IllegalStateException e) {
            Log.i(TAG, "Cloudinary already initialized.");
        }
    }

    public void openFilePicker(ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        launcher.launch(intent);
    }

    public void handleFilePickerResult(Uri selectedFileUri) {
        handleFilePickerResult(selectedFileUri, 0);
    }

    public void handleFilePickerResult(Uri selectedFileUri, long durationMillis) {
        if (selectedFileUri == null) return;

        long fileSize = getFileSize(selectedFileUri);
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            callbacks.showToast("File too large. Max is " + MAX_FILE_SIZE_MB + "MB", Toast.LENGTH_LONG);
            return;
        }

        String fileName = getFileName(selectedFileUri);
        String mimeType = context.getContentResolver().getType(selectedFileUri);

        if (mimeType == null || mimeType.isEmpty() || mimeType.equals("application/octet-stream")) {
            if (fileName.toLowerCase().endsWith(".aac")) mimeType = "audio/aac";
            else if (fileName.toLowerCase().endsWith(".mp3")) mimeType = "audio/mpeg";
            else if (fileName.toLowerCase().endsWith(".pdf")) mimeType = "application/pdf";
            else mimeType = "application/octet-stream";
        }

        uploadToCloudinary(selectedFileUri, mimeType, fileName, durationMillis);
    }

    private String getFileName(Uri uri) {
        Cursor cursor = null;
        String name = "Unknown_File";
        try {
            if (uri.getScheme() != null && uri.getScheme().equals("content")) {
                cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) name = cursor.getString(nameIndex);
                }
            } else if (uri.getScheme() != null && uri.getScheme().equals("file")) {
                name = new File(uri.getPath()).getName();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file name", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return name.replaceAll("\\s+", "_");
    }

    private long getFileSize(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex != -1) return cursor.getLong(sizeIndex);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file size", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return -1;
    }

    private void uploadToCloudinary(Uri uri, String mimeType, String fileName, long durationMillis) {
        callbacks.onProgressVisibilityChanged(View.VISIBLE);

        String resourceType;
        String firestoreType;
        String typePrefix = mimeType.contains("/") ? mimeType.split("/")[0] : mimeType;

        if (typePrefix.equals("image")) {
            resourceType = "image";
            firestoreType = "image";
        } else if (typePrefix.equals("video")) {
            resourceType = "video";
            firestoreType = "video";
        } else if (typePrefix.equals("audio")) {
            resourceType = "video";
            firestoreType = "audio";
        } else {
            resourceType = "raw";
            firestoreType = "file";
        }

        String folder = "chat_files/" + chatId;
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) extension = fileName.substring(i);

        String publicId = folder + "/" + UUID.randomUUID().toString() + extension;

        MediaManager.get().upload(uri)
                .option("public_id", publicId)
                .option("resource_type", resourceType)
                .callback(new UploadCallback() {
                    @Override
                    public void onSuccess(String reqId, Map result) {
                        callbacks.onProgressVisibilityChanged(View.GONE);
                        String url = (String) result.get("secure_url");
                        listener.onFileUploadCompleted(firestoreType, url, fileName, durationMillis);
                    }

                    @Override
                    public void onError(String reqId, ErrorInfo error) {
                        callbacks.onProgressVisibilityChanged(View.GONE);
                        callbacks.showToast("Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT);
                    }

                    @Override public void onStart(String r) {}
                    @Override public void onProgress(String r, long b, long t) {}
                    @Override public void onReschedule(String r, ErrorInfo e) {}
                }).dispatch();
    }

    public void deleteCloudinaryFile(String fileUrl, final Runnable onSuccess) {
        if (fileUrl == null || !fileUrl.contains("/upload/v")) {
            if (onSuccess != null) onSuccess.run();
            return;
        }
        // Client-side deletion skipped due to signature requirements
        if (onSuccess != null) onSuccess.run();
    }
}