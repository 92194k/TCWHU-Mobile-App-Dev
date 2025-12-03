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

    public interface FileUploadCompletionListener {
        void onFileUploadCompleted(String type, String content, String fileName);
    }

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

    // Handles the result from the file picker
    public void handleFilePickerResult(Uri selectedFileUri) {
        // 1. Validate Size
        long fileSize = getFileSize(selectedFileUri);
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            callbacks.showToast("File too large. Max is " + MAX_FILE_SIZE_MB + "MB", Toast.LENGTH_LONG);
            return;
        }

        // 2. Get Metadata
        String mimeType = context.getContentResolver().getType(selectedFileUri);
        String fileName = getFileName(selectedFileUri);

        if (mimeType == null) mimeType = "application/octet-stream";

        // 3. Dispatch Unified Upload
        uploadToCloudinary(selectedFileUri, mimeType, fileName);
    }

    private String getFileName(Uri uri) {
        Cursor cursor = null;
        String name = "Unknown File";
        try {
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    name = cursor.getString(nameIndex);
                }
            }
        } catch (Exception e) {
            Log.e("ChatFileUploader", "Error getting file name", e);
        } finally {
            if (cursor != null) cursor.close();
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

    // UNIFIED UPLOAD METHOD (handles image, video, audio, file)
    private void uploadToCloudinary(Uri uri, String mimeType, String fileName) {
        callbacks.onProgressVisibilityChanged(View.VISIBLE);

        String resourceType;
        String firestoreType;
        String typePrefix = mimeType.split("/")[0];

        if (typePrefix.equals("image")) {
            resourceType = "image";
            firestoreType = "image";
        } else if (typePrefix.equals("video")) {
            resourceType = "video";
            firestoreType = "video";
        } else if (typePrefix.equals("audio")) {
            resourceType = "video"; // Cloudinary often prefers video for audio to handle complex formats
            firestoreType = "audio";
        } else {
            resourceType = "raw"; // Use 'raw' for documents (PDF, DOCX, custom files)
            firestoreType = "file"; // Save as 'file' type in Firestore
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
                        // Notify the Activity with the correct type and file name
                        listener.onFileUploadCompleted(firestoreType, url, fileName);
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