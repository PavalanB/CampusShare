# CampusShare — Supabase Storage Setup (Free Image Hosting)

Supabase Storage replaces Firebase Storage for hosting resource photos.
It is 100% free up to 1GB with no credit card required.

---

## Step 1 — Create a Supabase account

1. Go to https://supabase.com
2. Click "Start your project" → Sign up with GitHub or Google
3. It's completely free — no credit card needed

---

## Step 2 — Create a new project

1. Click "New Project"
2. Name it: CampusShare
3. Set a database password (save it somewhere safe)
4. Choose a region closest to your location
5. Click "Create new project"
6. Wait about 2 minutes for it to set up

---

## Step 3 — Create a Storage bucket

1. In your Supabase project, click "Storage" in the left sidebar
2. Click "New bucket"
3. Name it exactly: resource-photos
4. Toggle ON "Public bucket" (this lets images load without auth)
5. Click "Create bucket"

---

## Step 4 — Get your project credentials

1. Click "Settings" (gear icon) in the left sidebar
2. Click "API"
3. Copy two values:

   Project URL:
   https://abcdefghijklm.supabase.co
   (looks like a random string followed by .supabase.co)

   anon public key:
   eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   (a very long string starting with eyJ)

---

## Step 5 — Paste credentials into ResourceRepository.java

Open:
  app/src/main/java/com/campusshare/repositories/ResourceRepository.java

Find these 3 lines near the top (around line 42):

```java
private static final String SUPABASE_URL  = "https://YOUR_PROJECT_ID.supabase.co";
private static final String SUPABASE_KEY  = "YOUR_ANON_PUBLIC_KEY";
private static final String BUCKET_NAME   = "resource-photos";
```

Replace with YOUR actual values:

```java
private static final String SUPABASE_URL  = "https://abcdefghijklm.supabase.co";
private static final String SUPABASE_KEY  = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
private static final String BUCKET_NAME   = "resource-photos";
```

Keep BUCKET_NAME exactly as "resource-photos" — this must match what you created in Step 3.

---

## Step 6 — Set Storage Policy (allow uploads)

1. In Supabase → Storage → resource-photos bucket
2. Click "Policies" tab
3. Click "New Policy" → "For full customization"
4. Policy name: Allow all operations
5. Allowed operations: SELECT, INSERT, UPDATE, DELETE
6. Target roles: anon, authenticated
7. USING expression: true
8. WITH CHECK expression: true
9. Click "Review" → "Save policy"

This allows your app to upload and read photos freely.

---

## Step 7 — Sync and run

1. In Android Studio: File → Sync Project with Gradle Files
2. Run the app
3. Try adding a resource with a photo
4. The image should upload and display correctly

---

## How the URL works

When you upload a photo named abc123.jpg to the resource-photos bucket,
the public URL to display it is:

https://YOUR_PROJECT_ID.supabase.co/storage/v1/object/public/resource-photos/abc123.jpg

This URL is stored in Firestore on the Resource document as photoUrl.
Glide loads it directly — no authentication needed because the bucket is public.

---

## Free tier limits

| Resource     | Free Limit      | CampusShare usage estimate |
|--------------|-----------------|---------------------------|
| Storage      | 1 GB            | ~5000 photos at 200KB each |
| Bandwidth    | 2 GB / month    | More than enough for a campus |
| API requests | Unlimited       | No limit                  |

A college project will never come close to these limits.
