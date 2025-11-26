package com.tcwhu.app;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

public class DownloadManagerUtils {

    public static void startDownload(Context context, String url, String fileName) {
        try {
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

            Uri uri = Uri.parse(url);
            DownloadManager.Request request = new DownloadManager.Request(uri);

            // Set the destination directory to the user's Downloads folder
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            // Allow the file to be scanned by MediaScanner (shows up in Gallery/Files)
            request.allowScanningByMediaScanner();

            // Display notification while downloading
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            // Set the download title
            request.setTitle("Downloading: " + fileName);

            // Enqueue the download request
            downloadManager.enqueue(request);

            Toast.makeText(context, "Download started: " + fileName, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}