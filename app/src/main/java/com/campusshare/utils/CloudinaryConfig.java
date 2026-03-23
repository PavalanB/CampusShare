package com.campusshare.utils;

import com.campusshare.BuildConfig;

/**
 * CloudinaryConfig holds your Cloudinary credentials.
 * Values are now loaded from BuildConfig (which gets them from local.properties)
 * to keep them secure and out of version control.
 */
public class CloudinaryConfig {

    public static final String CLOUD_NAME  = BuildConfig.CLOUDINARY_CLOUD_NAME;
    public static final String API_KEY     = BuildConfig.CLOUDINARY_API_KEY;
    public static final String API_SECRET  = BuildConfig.CLOUDINARY_API_SECRET;
    public static final String UPLOAD_PRESET = BuildConfig.CLOUDINARY_UPLOAD_PRESET;

    // Cloudinary upload endpoint (uses CLOUD_NAME)
    public static String getUploadUrl() {
        return "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";
    }

    // Folder inside Cloudinary where resource photos are stored
    public static final String FOLDER = "campusshare/resources";
}
