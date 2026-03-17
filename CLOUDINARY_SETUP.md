# CampusShare — Cloudinary Setup Guide
# Photos stored on Cloudinary | Database on Firestore | Auth via Firebase

---

## Why Cloudinary?
- 25 GB free storage — no credit card ever needed
- Automatic image compression and resizing
- Global CDN — images load fast anywhere
- No SDK needed — plain HTTP upload from Android

---

## STEP 1 — Create Cloudinary Account

1. Go to https://cloudinary.com
2. Click "Sign Up For Free"
3. Fill in your details — no credit card required
4. Verify your email and log in

---

## STEP 2 — Get Your Credentials

After logging in, you land on the Dashboard.
Look for the "Product Environment Credentials" section.

Copy these three values:
  - Cloud name    →  e.g. dxyz123abc
  - API Key       →  e.g. 123456789012345
  - API Secret    →  e.g. abcDEFghiJKLmnoPQR

---

## STEP 3 — Create an Upload Preset (IMPORTANT)

This allows the Android app to upload without a server signing the request.

1. In Cloudinary Dashboard → top right gear icon → Settings
2. Click "Upload" in the left menu
3. Scroll down to "Upload presets"
4. Click "Add upload preset"
5. Set these values:
     Preset name:    campusshare_preset
     Signing mode:   Unsigned   ← MUST be Unsigned
     Folder:         campusshare/resources
6. Click "Save"

---

## STEP 4 — Add Your Credentials to the App

Open this file in Android Studio:
  app/src/main/java/com/campusshare/utils/CloudinaryConfig.java

Replace the placeholder values:

  public static final String CLOUD_NAME     = "your_actual_cloud_name";
  public static final String API_KEY        = "your_actual_api_key";
  public static final String API_SECRET     = "your_actual_api_secret";
  public static final String UPLOAD_PRESET  = "campusshare_preset";

Note: API_KEY and API_SECRET are in CloudinaryConfig.java for reference
but are NOT sent from the Android app (unsigned upload only uses
CLOUD_NAME and UPLOAD_PRESET). Keep the secret safe — never put it
in a public GitHub repo.

---

## STEP 5 — Test the Upload

Run the app → go to Add Resource → pick a photo → tap Save.

If the upload works, you will see the photo appear in:
  Cloudinary Dashboard → Media Library → campusshare/resources folder

---

## How It Works in Code

1. Student picks a photo from gallery (returns a Uri)
2. CloudinaryUploader.uploadPhoto() reads the Uri, compresses to JPEG 80%
3. Sends a multipart/form-data POST to:
   https://api.cloudinary.com/v1_1/{CLOUD_NAME}/image/upload
4. Cloudinary returns a JSON with secure_url
5. That URL is saved to Firestore on the resource document
6. Glide loads the URL in ImageViews — works exactly like any HTTPS image

---

## Free Tier Limits

  Storage:          25 GB
  Bandwidth:        25 GB / month
  Transformations:  25,000 / month
  No credit card required — ever

A college campus app with hundreds of students will use
maybe 1–2 GB total. You will never hit the free limits.

---

## Stack Summary

  Login / Register     →  Firebase Auth    (free, no card)
  Database             →  Firestore        (free, no card)
  Push notifications   →  FCM              (free, no card)
  Photo storage        →  Cloudinary       (free, no card)

Total cost for your college project: zero rupees.
