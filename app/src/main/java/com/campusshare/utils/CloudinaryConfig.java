package com.campusshare.utils;

/**
 * CloudinaryConfig holds your Cloudinary credentials.
 *
 * HOW TO GET THESE VALUES:
 * 1. Go to https://cloudinary.com → Sign Up (free, no credit card)
 * 2. After signup → Dashboard page shows all three values below
 * 3. Replace the placeholder strings with your actual values
 *
 * FREE TIER:
 *   25 GB storage
 *   25 GB bandwidth/month
 *   Automatic image compression and resizing
 *   No credit card required — ever
 */
public class CloudinaryConfig {

    // Found on your Cloudinary Dashboard → Product Environment Credentials
    public static final String CLOUD_NAME  = "dndbhzp9q";   // e.g. "dxyz123abc"
    public static final String API_KEY     = "266153253722785";       // e.g. "123456789012345"
    public static final String API_SECRET  = "qFCto99arGkb3tk6WA27MfvIlvk";    // e.g. "abcDEFghiJKLmnoPQR"

    // Upload preset — create this in Cloudinary Dashboard:
    // Settings → Upload → Upload presets → Add upload preset
    // Set signing mode to "Unsigned" and save the preset name here
    public static final String UPLOAD_PRESET = "campusshare_preset";

    // Cloudinary upload endpoint (uses CLOUD_NAME)
    public static String getUploadUrl() {
        return "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";
    }

    // Folder inside Cloudinary where resource photos are stored
    public static final String FOLDER = "campusshare/resources";
}
