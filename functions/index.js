const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

/**
 * Triggered whenever a new document is created in /notifications
 * Reads the recipient's FCM token from /users/{uid}/fcmToken
 * and sends the push notification
 */
exports.sendPushNotification = functions.firestore
  .document("notifications/{notifId}")
  .onCreate(async (snap, context) => {
    const data = snap.data();

    const { recipientUID, title, body, type, requestID } = data;

    if (!recipientUID || !title || !body) return null;

    // Get recipient's FCM token from their user document
    const userDoc = await admin.firestore()
      .collection("users")
      .doc(recipientUID)
      .get();

    if (!userDoc.exists) return null;

    const fcmToken = userDoc.data().fcmToken;
    if (!fcmToken) return null;

    // Build the FCM message
    const message = {
      token: fcmToken,
      data: {
        title:     title,
        body:      body,
        type:      type      || "",
        requestID: requestID || "",
      },
      android: {
        priority: "high",
        notification: {
          sound: "default",
          clickAction: "FLUTTER_NOTIFICATION_CLICK",
        },
      },
    };

    try {
      await admin.messaging().send(message);
      // Mark as delivered
      await snap.ref.update({ delivered: true });
      console.log("Notification sent to", recipientUID);
    } catch (error) {
      console.error("Failed to send notification:", error);
    }

    return null;
  });