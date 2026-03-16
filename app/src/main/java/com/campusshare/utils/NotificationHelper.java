package com.campusshare.utils;

import android.content.Context;
import android.util.Log;

import com.campusshare.models.BorrowRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

/**
 * NotificationHelper handles two things:
 *
 * 1. FCM Token Management:
 *    Each device gets a unique FCM token. We save this to Firestore under
 *    /users/{uid}/fcmToken so Cloud Functions can look up who to notify.
 *
 * 2. Notification Triggers:
 *    We write a document to /notifications/{id} in Firestore.
 *    A Cloud Function (see CLOUD_FUNCTIONS_SETUP.md) watches this collection
 *    and sends the actual FCM push notification.
 *
 *    This approach works without a backend server — Firestore + Cloud Functions
 *    handles all the delivery.
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    // ── Save FCM Token ────────────────────────────────────────────────────────

    /**
     * Call this on app startup (in MainActivity.onCreate) and
     * from FCMService.onNewToken when the token refreshes.
     */
    public static void saveTokenToFirestore(String token) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || token == null) return;

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("fcmToken", token)
            .addOnFailureListener(e -> Log.w(TAG, "Failed to save FCM token", e));
    }

    /**
     * Fetches the current device token and saves it to Firestore.
     * Call once after login in MainActivity.
     */
    public static void refreshAndSaveToken() {
        FirebaseMessaging.getInstance().getToken()
            .addOnSuccessListener(NotificationHelper::saveTokenToFirestore)
            .addOnFailureListener(e -> Log.w(TAG, "Failed to get FCM token", e));
    }

    // ── Trigger Notifications via Firestore ───────────────────────────────────

    /**
     * Notifies the resource owner that a new borrow request was received.
     * Writes a document to /notifications — Cloud Function delivers the push.
     */
    public static void notifyRequestReceived(BorrowRequest request) {
        sendNotification(
            request.getOwnerID(),
            "New Borrow Request",
            request.getBorrowerName() + " wants to borrow your " + request.getResourceName(),
            "REQUEST_RECEIVED",
            request.getRequestID()
        );
    }

    /**
     * Notifies the borrower that their request was accepted.
     */
    public static void notifyRequestAccepted(BorrowRequest request) {
        sendNotification(
            request.getBorrowerID(),
            "Request Accepted!",
            request.getOwnerName() + " accepted your request for " + request.getResourceName(),
            "REQUEST_ACCEPTED",
            request.getRequestID()
        );
    }

    /**
     * Notifies the borrower that their request was rejected.
     */
    public static void notifyRequestRejected(BorrowRequest request) {
        sendNotification(
            request.getBorrowerID(),
            "Request Rejected",
            request.getOwnerName() + " could not fulfil your request for " + request.getResourceName(),
            "REQUEST_REJECTED",
            request.getRequestID()
        );
    }

    /**
     * Notifies the borrower that the item has been marked returned
     * and prompts them to rate the experience.
     */
    public static void notifyItemReturned(BorrowRequest request) {
        sendNotification(
            request.getBorrowerID(),
            "Item Returned",
            "Your borrow of " + request.getResourceName() + " is complete. Please rate " + request.getOwnerName(),
            "ITEM_RETURNED",
            request.getRequestID()
        );
    }

    // ── Internal: Write to /notifications collection ──────────────────────────

    private static void sendNotification(String recipientUID, String title,
                                         String body, String type, String requestID) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("recipientUID", recipientUID);
        notification.put("title",        title);
        notification.put("body",         body);
        notification.put("type",         type);
        notification.put("requestID",    requestID);
        notification.put("timestamp",    com.google.firebase.Timestamp.now());
        notification.put("delivered",    false);

        FirebaseFirestore.getInstance()
            .collection("notifications")
            .add(notification)
            .addOnFailureListener(e -> Log.w(TAG, "Failed to queue notification", e));
    }
}
