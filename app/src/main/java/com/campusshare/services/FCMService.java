package com.campusshare.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.campusshare.R;
import com.campusshare.activities.InboxActivity;
import com.campusshare.utils.NotificationHelper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * FCMService receives push notifications sent from Firestore Cloud Functions
 * (or manually from the Firebase Console for testing).
 *
 * Notification types used in CampusShare:
 *   type = "REQUEST_RECEIVED"  → sent to owner when borrower sends request
 *   type = "REQUEST_ACCEPTED"  → sent to borrower when owner accepts
 *   type = "REQUEST_REJECTED"  → sent to borrower when owner rejects
 *   type = "ITEM_DUE_SOON"     → sent to borrower 1 day before due date
 *
 * Each notification taps through to InboxActivity.
 */
public class FCMService extends FirebaseMessagingService {

    private static final String CHANNEL_ID   = "campusshare_channel";
    private static final String CHANNEL_NAME = "CampusShare Notifications";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Extract data payload (we use data messages, not notification messages,
        // so the app handles display in all states — foreground and background)
        if (remoteMessage.getData().isEmpty()) return;

        String title   = remoteMessage.getData().get("title");
        String body    = remoteMessage.getData().get("body");
        String type    = remoteMessage.getData().get("type");

        if (title == null) title = "CampusShare";
        if (body  == null) body  = "You have a new notification";

        showNotification(title, body);
    }

    /**
     * Called when FCM generates a new registration token.
     * Save this token to the user's Firestore document so the server
     * knows which device to send notifications to.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        NotificationHelper.saveTokenToFirestore(token);
    }

    // ── Show notification ─────────────────────────────────────────────────────

    private void showNotification(String title, String body) {
        createNotificationChannel();

        // Tap notification → open InboxActivity
        Intent intent = new Intent(this, InboxActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent);

        NotificationManager manager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Borrow request updates");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}
