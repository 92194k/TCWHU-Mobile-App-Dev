package com.tcwhu.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;
import java.util.Random;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHAT_CHANNEL_BASE = "channel_base_chat";
    private static final String EVENT_CHANNEL_BASE = "channel_base_event";
    private static final String CHAT_SOUND_KEY = "default_chat_tone";
    private static final String EVENT_SOUND_KEY = "default_event_tone";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        if (data.isEmpty() && remoteMessage.getNotification() == null) return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Filter: Don't notify sender
        if (currentUser != null && data.containsKey("senderId") && data.get("senderId").equals(currentUser.getUid())) return;

        String type = data.get("type");
        String title = data.get("title");
        String body = data.get("body");
        String otherUserId = data.get("otherUserId");

        Intent intent;
        String channelBaseId;
        String channelName;
        String soundFile;

        if ("chat".equals(type) && otherUserId != null) {
            intent = new Intent(this, ChatWindowActivity.class);
            intent.putExtra(ChatWindowActivity.EXTRA_OTHER_USER_ID, otherUserId);
            channelBaseId = CHAT_CHANNEL_BASE;
            channelName = "Chat Messages";
            soundFile = CHAT_SOUND_KEY;
        } else if ("event".equals(type)) {
            intent = new Intent(this, StudentHomeActivity.class);
            channelBaseId = EVENT_CHANNEL_BASE;
            channelName = "Event Announcements";
            soundFile = EVENT_SOUND_KEY;
        } else {
            intent = new Intent(this, StudentHomeActivity.class);
            channelBaseId = "channel_base_general";
            channelName = "General Notifications";
            soundFile = null;
        }

        String finalChannelId = channelBaseId + "_" + (soundFile != null ? soundFile : "default");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        sendNotification(title, body, finalChannelId, channelName, intent, soundFile);
    }

    private void sendNotification(String title, String body, String channelId, String channelName, Intent intent, String soundFile) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        if (soundFile != null) {
            int resId = getResources().getIdentifier(soundFile.toLowerCase(), "raw", getPackageName());
            if (resId != 0) {
                soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/" + resId);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = manager.getNotificationChannel(channelId);
            if (channel == null) {
                channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();
                channel.setSound(soundUri, audioAttributes);
                channel.enableLights(true);
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{0, 250, 250, 250});
                manager.createNotificationChannel(channel);
            }
        }

        int notificationId = (int) (System.currentTimeMillis() % 100000000L) + new Random().nextInt(100000);

        PendingIntent finalPendingIntent = PendingIntent.getActivity(
                this, notificationId, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        int iconResId = getResources().getIdentifier("ic_chat", "drawable", getPackageName());
        if (iconResId == 0) iconResId = R.mipmap.ic_launcher;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(iconResId)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(finalPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setSound(soundUri);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        manager.notify(notificationId, builder.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) saveTokenToFirestore(user.getUid(), token);
    }

    private void saveTokenToFirestore(String uid, String token) {
        FirebaseFirestore.getInstance().collection("users").document(uid).update("notificationToken", token);
    }
}