package com.tcwhu.app;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import java.io.File;

public class DownloadManagerUtils {

    public static void startDownload(Context context, String url, String fileName, String mimeType) {
        try {
            if (url == null || url.isEmpty()) {
                Toast.makeText(context, "Error: No URL provided", Toast.LENGTH_SHORT).show();
                return;
            }

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager == null) return;

            String cleanFileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

            if (!cleanFileName.contains(".")) {
                if (mimeType != null) {
                    String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                    cleanFileName += (ext != null) ? "." + ext : ".bin";
                }
            }

            Uri uri = Uri.parse(url);
            DownloadManager.Request request = new DownloadManager.Request(uri);

            request.setTitle(cleanFileName);
            request.setDescription("Downloading file...");
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setAllowedOverRoaming(true);
            request.setAllowedOverMetered(true);

            if (mimeType != null && !mimeType.equals("*/*")) request.setMimeType(mimeType);

            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "TCWHU_Chat" + File.separator + cleanFileName);

            downloadManager.enqueue(request);
            Toast.makeText(context, "Download started: " + cleanFileName, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("DownloadManagerUtils", "Download Error: " + e.getMessage());
            Toast.makeText(context, "Download failed.", Toast.LENGTH_SHORT).show();
        }
    }
}