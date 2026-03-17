# CampusShare — Firebase Setup Instructions

Follow these steps BEFORE running the app. Without google-services.json the app will crash.

---

## Step 1 — Create Firebase Project

1. Go to https://console.firebase.google.com
2. Click "Add project"
3. Name it: CampusShare
4. Disable Google Analytics (not needed for now)
5. Click "Create project"

---

## Step 2 — Add Android App to Firebase

1. In your Firebase project, click the Android icon
2. Enter package name exactly: `com.campusshare`
3. App nickname: CampusShare
4. Click "Register app"
5. Download `google-services.json`
6. Place it in: `CampusShare/app/google-services.json`
   (same folder as your app-level build.gradle)

---

## Step 3 — Enable Firebase Authentication

1. In Firebase Console → Authentication → Get Started
2. Click "Sign-in method" tab
3. Enable "Email/Password"
4. Click Save

---

## Step 4 — Create Firestore Database

1. In Firebase Console → Firestore Database → Create database
2. Choose "Start in test mode" (safe for development)
3. Select your region (choose closest to your location)
4. Click "Enable"

---

## Step 5 — Set Firestore Security Rules

Go to Firestore → Rules and paste these rules:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Users can read any profile, but only write their own
    match /users/{userID} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userID;

      // Credit ledger — only the owner can read/write their own ledger
      match /ledger/{partnerID} {
        allow read, write: if request.auth != null && request.auth.uid == userID;
      }
    }

    // Resources — any logged-in student can read, only owner can write
    match /resources/{resourceID} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null
        && request.auth.uid == resource.data.ownerID;
    }

    // Requests — borrower and owner can both read, only borrower can create
    match /requests/{requestID} {
      allow read: if request.auth != null
        && (request.auth.uid == resource.data.borrowerID
            || request.auth.uid == resource.data.ownerID);
      allow create: if request.auth != null;
      allow update: if request.auth != null
        && (request.auth.uid == resource.data.borrowerID
            || request.auth.uid == resource.data.ownerID);
    }
  }
}
```

Click "Publish"

---

## Step 6 — Add Google Services to project-level build.gradle

Open `CampusShare/build.gradle` (project level, not app level) and add:

```gradle
buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.4.1'
    }
}
```

---

## Step 7 — Sync and Run

1. In Android Studio: File → Sync Project with Gradle Files
2. Run on emulator or physical device
3. You should see the Login screen

---

## Firestore Data Structure (auto-created when first user registers)

```
/users
  /{userID}
      name: "Arjun Kumar"
      email: "arjun@college.edu"
      collegeID: "CS2021001"
      department: "Computer Science"
      year: "2nd Year"
      phone: "9876543210"
      creditScore: 0.0
      avgRating: 0.0
      totalBorrows: 0
      totalLends: 0
      profilePhoto: ""
    /ledger
      /{partnerUserID}
          balance: -1    (negative = you owe them, positive = they owe you)
          lastUpdated: timestamp
```

---

## Common Errors

| Error | Fix |
|-------|-----|
| `google-services.json not found` | Download from Firebase Console and place in /app folder |
| `Default FirebaseApp is not initialized` | Check google-services plugin in both build.gradle files |
| `PERMISSION_DENIED` on Firestore | Check Security Rules — make sure test mode is on |
| `Network error` | Add INTERNET permission to AndroidManifest.xml |
