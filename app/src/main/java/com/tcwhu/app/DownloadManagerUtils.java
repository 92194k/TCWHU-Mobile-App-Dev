package com.tcwhu.app;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

public class DownloadManagerUtils {

    /**
     * Initiates a file download using the Android system's DownloadManager.
     *
     * @param context The application context.
     * @param url The secure URL of the file to download (from Cloudinary/Firestore).
     * @param fileName The original file name to use for the downloaded file.
     */
    public static void startDownload(Context context, String url, String fileName) {
        try {
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager == null) {
                Toast.makeText(context, "Download service unavailable.", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri uri = Uri.parse(url);
            DownloadManager.Request request = new DownloadManager.Request(uri);

            // 1. Set the destination path: Downloads/TCWHU_Chat/
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "TCWHU_Chat/" + fileName
            );

            // 2. Set file details
            request.setTitle(fileName);
            request.setDescription("Downloading file from chat...");

            // 3. Determine MIME type (helps system know how to open the file)
            String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mimeType != null) {
                request.setMimeType(mimeType);
            }

            // 4. Set visibility and network constraints
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);

            // 5. Enqueue the download
            downloadManager.enqueue(request);

            Toast.makeText(context, "Download started: " + fileName, Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            // Log the error for debugging
            Log.e("DownloadManagerUtils", "Failed to start download: " + e.getMessage());
            Toast.makeText(context, "Download failed: Cannot process file URL.", Toast.LENGTH_LONG).show();
        }
    }
}