# CampusShare — Cloud Functions Setup (for Push Notifications)

Push notifications in CampusShare work like this:
1. The Android app writes a document to /notifications/{id} in Firestore
2. A Cloud Function detects the new document and sends the FCM push to the recipient's device

This is the standard serverless pattern — no backend server needed.

---

## Step 1 — Install Firebase CLI

```bash
npm install -g firebase-tools
firebase login
```

---

## Step 2 — Initialize Functions in your project

```bash
cd CampusShare   # your project root
firebase init functions
# Choose: JavaScript, No ESLint, Yes install dependencies
```

---

## Step 3 — Replace functions/index.js with this code

```javascript
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
```

---

## Step 4 — Deploy

```bash
firebase deploy --only functions
```

---

## Step 5 — Test from Firebase Console

1. Go to Firestore → Add document to /notifications
2. Set fields: recipientUID, title, body, type
3. The Cloud Function triggers and sends the push to that user's device

---

## Firestore Security Rules for /notifications

Add this to your Firestore rules:

```
match /notifications/{notifID} {
  // Only the app (authenticated users) can create notifications
  // Cloud Functions use admin SDK so they bypass rules
  allow create: if request.auth != null;
  allow read, update: if false; // Cloud Function only
}
```

---

## Testing without Cloud Functions

During development you can test notifications directly from the Firebase Console:
1. Firebase Console → Cloud Messaging → Send test message
2. Enter your device's FCM token (printed in Android Studio Logcat as "FCM Token: xxxxx")
3. Send the test message

To see your token in Logcat, add this to MainActivity temporarily:
```java
FirebaseMessaging.getInstance().getToken()
  .addOnSuccessListener(token -> Log.d("FCM", "Token: " + token));
```
