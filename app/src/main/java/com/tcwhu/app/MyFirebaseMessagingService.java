package com.tcwhu.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "chat_messages_channel";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String senderId = remoteMessage.getData().get("senderId");
        String otherUserId = remoteMessage.getData().get("otherUserId");
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");

        if (senderId != null && senderId.equals(currentUser.getUid())) return;

        Intent intent = new Intent(this, ChatWindowActivity.class);
        intent.putExtra(ChatWindowActivity.EXTRA_OTHER_USER_ID, otherUserId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID,
                    "Chat Messages",
                    NotificationManager.IMPORTANCE_HIGH
            ));
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify(1, builder.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) saveTokenToFirestore(user.getUid(), token);
    }

    private void saveTokenToFirestore(String uid, String token) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("notificationToken", token)
                .addOnSuccessListener(x -> Log.d("FCM", "Token updated"))
                .addOnFailureListener(e -> Log.e("FCM", "Token failed: " + e));
    }
}
