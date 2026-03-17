package com.campusshare.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CloudinaryUploader handles photo uploads to Cloudinary.
 *
 * HOW IT WORKS:
 * Cloudinary accepts "unsigned uploads" — you don't need to sign the request
 * server-side. You just create an "unsigned upload preset" in the Cloudinary
 * dashboard and use it directly from the Android app.
 *
 * No SDK is needed — we use a standard multipart/form-data HTTP POST,
 * which is exactly what every file upload form on the web does.
 *
 * The upload returns a JSON response containing the secure_url of the
 * uploaded image — we store that URL in Firestore on the resource document.
 *
 * SETUP STEPS (one-time):
 * 1. Sign up at cloudinary.com (free)
 * 2. Dashboard → Settings → Upload → Upload presets → Add upload preset
 * 3. Set signing mode: Unsigned
 * 4. Set folder: campusshare/resources
 * 5. Save the preset name into CloudinaryConfig.UPLOAD_PRESET
 * 6. Copy your Cloud Name from Dashboard into CloudinaryConfig.CLOUD_NAME
 */
public class CloudinaryUploader {

    private static final ExecutorService executor  = Executors.newCachedThreadPool();
    private static final Handler         mainThread = new Handler(Looper.getMainLooper());
    private static final String          BOUNDARY   = "CampusShareBoundary" + System.currentTimeMillis();
    private static final String          LINE_END   = "\r\n";
    private static final String          TWO_HYPHENS = "--";

    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String error);
    }

    // ── Main upload method ────────────────────────────────────────────────────

    /**
     * Uploads a photo from the given URI to Cloudinary.
     * Compresses the image to JPEG quality 80 before uploading
     * to reduce bandwidth and storage usage.
     *
     * Returns the secure HTTPS URL of the uploaded image.
     */
    public static void uploadPhoto(Context context, Uri photoUri, UploadCallback callback) {
        executor.execute(() -> {
            try {
                // Step 1 — Read and compress the image
                InputStream is      = context.getContentResolver().openInputStream(photoUri);
                Bitmap bitmap       = BitmapFactory.decodeStream(is);
                Bitmap scaled       = scaleBitmap(bitmap, 1024); // max 1024px wide
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] imageBytes   = baos.toByteArray();

                // Unique filename
                String fileName     = "resource_" + UUID.randomUUID().toString() + ".jpg";

                // Step 2 — Build multipart request
                URL url = new URL(CloudinaryConfig.getUploadUrl());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type",
                    "multipart/form-data; boundary=" + BOUNDARY);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                // Field: upload_preset (required for unsigned uploads)
                writeField(dos, "upload_preset", CloudinaryConfig.UPLOAD_PRESET);

                // Field: folder (organises uploads inside Cloudinary)
                writeField(dos, "folder", CloudinaryConfig.FOLDER);

                // Field: public_id (the filename inside Cloudinary)
                writeField(dos, "public_id", fileName);

                // Field: file (the actual image bytes)
                dos.writeBytes(TWO_HYPHENS + BOUNDARY + LINE_END);
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\""
                    + fileName + "\"" + LINE_END);
                dos.writeBytes("Content-Type: image/jpeg" + LINE_END);
                dos.writeBytes(LINE_END);
                dos.write(imageBytes);
                dos.writeBytes(LINE_END);

                // Close multipart
                dos.writeBytes(TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_END);
                dos.flush();
                dos.close();

                // Step 3 — Read response
                int responseCode = conn.getResponseCode();
                InputStream responseStream = (responseCode == 200)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

                BufferedReader br = new BufferedReader(
                    new InputStreamReader(responseStream, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                if (responseCode == 200) {
                    // Step 4 — Parse the secure_url from JSON response
                    JSONObject json   = new JSONObject(sb.toString());
                    String secureUrl  = json.getString("secure_url");
                    mainThread.post(() -> callback.onSuccess(secureUrl));
                } else {
                    String errorBody = sb.toString();
                    mainThread.post(() -> callback.onFailure(
                        "Upload failed (HTTP " + responseCode + "): " + errorBody));
                }

            } catch (Exception e) {
                mainThread.post(() -> callback.onFailure(
                    "Upload error: " + e.getMessage()));
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Writes a plain text form field to the multipart stream.
     */
    private static void writeField(DataOutputStream dos, String name, String value)
        throws Exception {
        dos.writeBytes(TWO_HYPHENS + BOUNDARY + LINE_END);
        dos.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"" + LINE_END);
        dos.writeBytes(LINE_END);
        dos.writeBytes(value + LINE_END);
    }

    /**
     * Scales a bitmap down so its longest side is at most maxSize pixels.
     * Preserves aspect ratio. If already smaller, returns as-is.
     */
    private static Bitmap scaleBitmap(Bitmap original, int maxSize) {
        int width  = original.getWidth();
        int height = original.getHeight();

        if (width <= maxSize && height <= maxSize) return original;

        float ratio;
        if (width > height) {
            ratio = (float) maxSize / width;
        } else {
            ratio = (float) maxSize / height;
        }

        int newWidth  = Math.round(width  * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }
}
