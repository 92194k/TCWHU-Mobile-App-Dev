package com.tcwhu.app;

import android.content.ContentResolver;
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

// NOTE: Placeholder classes (ChatWindowCallbacks) are assumed to exist.

public class ChatFileUploader {

    private static final long MAX_FILE_SIZE_MB = 25;
    private static final long MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;

    private final Context context;
    private final String chatId;
    private final ChatWindowCallbacks callbacks;
    private final FileUploadCompletionListener listener;

    // --- REVISED INTERFACE ---
    public interface FileUploadCompletionListener {
        // ADDED durationMillis parameter
        void onFileUploadCompleted(String type, String content, String fileName, long durationMillis);
    }
    // -------------------------

    public ChatFileUploader(Context context, String chatId, ChatWindowCallbacks callbacks, FileUploadCompletionListener listener) {
        this.context = context;
        this.chatId = chatId;
        this.callbacks = callbacks;
        this.listener = listener;
        initCloudinary();
    }

    private void initCloudinary() {
        // Initialize Cloudinary (Use your actual config if different)
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dggeonpfw");
            config.put("api_key", "147481881754886");
            config.put("api_secret", "583Dz7vp2y6TRaDBuCj8HbHoQX4");
            MediaManager.init(context, config);
        } catch (IllegalStateException e) {
            Log.i("ChatFileUploader", "Cloudinary already initialized.");
        }
    }

    // Opens generic file picker
    public void openFilePicker(ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Allows all file types (image/*, application/pdf, etc.)
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        launcher.launch(intent);
    }

    // --- REVISED FOR FILE PICKER RESULT (No Duration, assumes 0) ---
    public void handleFilePickerResult(Uri selectedFileUri) {
        handleFilePickerResult(selectedFileUri, 0);
    }

    // --- NEW/MODIFIED METHOD TO ACCEPT DURATION ---
    /**
     * Handles the result from the file picker, or a local file (like recorded audio).
     * @param selectedFileUri The Uri of the file.
     * @param durationMillis The duration in milliseconds (0 for non-media files).
     */
    public void handleFilePickerResult(Uri selectedFileUri, long durationMillis) {
        // 1. Validate Size
        long fileSize = getFileSize(selectedFileUri);
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            callbacks.showToast("File too large. Max is " + MAX_FILE_SIZE_MB + "MB", Toast.LENGTH_LONG);
            return;
        }

        // 2. Get Metadata
        String fileName = getFileName(selectedFileUri); // Get filename first
        String mimeType = context.getContentResolver().getType(selectedFileUri);

        // --- FIX 1: Robust MIME Type Assignment, especially for recorded audio (.aac) ---
        if (mimeType == null || mimeType.isEmpty() || mimeType.equals("application/octet-stream")) {
            // Check the filename extension for known local formats (like AAC from ChatRecorder)
            if (fileName.toLowerCase().endsWith(".aac")) {
                mimeType = "audio/aac";
            } else {
                mimeType = "application/octet-stream";
            }
        }

        // 3. Dispatch Unified Upload
        // Pass durationMillis to the final upload method
        uploadToCloudinary(selectedFileUri, mimeType, fileName, durationMillis);
    }
    // ---------------------------------------------


    /**
     * Retrieves the file name, handling both content:// URIs and file:// URIs (like recorded audio).
     */
    private String getFileName(Uri uri) {
        Cursor cursor = null;
        String name = "Unknown File";

        if (uri.getScheme() == null) return name;

        if (uri.getScheme().equals("content")) {
            try {
                cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e("ChatFileUploader", "Error getting content file name", e);
            } finally {
                if (cursor != null) cursor.close();
            }
        } else if (uri.getScheme().equals("file")) {
            // For local files (like recorder output), extract name directly from path
            name = new File(uri.getPath()).getName();
        }
        return name;
    }

    private long getFileSize(Uri uri) {
        Cursor cursor = null;
        try {
            ContentResolver contentResolver = context.getContentResolver();
            cursor = contentResolver.query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex != -1) {
                    return cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception e) {
            Log.e("ChatFileUploader", "Error getting file size", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return -1;
    }

    // --- REVISED UNIFIED UPLOAD METHOD ---
    // ADDED durationMillis parameter
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
            firestoreType = "audio"; // Correct type for Firestore
        } else {
            resourceType = "raw";
            firestoreType = "file";
        }

        String folder = "chat_files/" + chatId;
        String publicId = folder + "/" + UUID.randomUUID().toString();

        MediaManager.get().upload(uri)
                .option("public_id", publicId)
                .option("resource_type", resourceType)
                .callback(new UploadCallback() {
                    @Override
                    public void onSuccess(String reqId, Map result) {
                        callbacks.onProgressVisibilityChanged(View.GONE);
                        String url = (String) result.get("secure_url");
                        // Notify the Activity with the correct type, file name, AND DURATION
                        listener.onFileUploadCompleted(firestoreType, url, fileName, durationMillis);
                    }

                    @Override
                    public void onError(String reqId, ErrorInfo error) {
                        callbacks.onProgressVisibilityChanged(View.GONE);
                        callbacks.showToast("Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT);
                    }

                    @Override
                    public void onStart(String r) {
                    }

                    @Override
                    public void onProgress(String r, long b, long t) {
                    }

                    @Override
                    public void onReschedule(String r, ErrorInfo e) {
                    }
                }).dispatch();
    }
}