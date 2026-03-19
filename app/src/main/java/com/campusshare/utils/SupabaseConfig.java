package com.campusshare.utils;

/**
 * SupabaseConfig holds your Supabase project credentials.
 *
 * HOW TO FILL THIS IN:
 *  1. Go to supabase.com → your CampusShare project
 *  2. Click Settings → API
 *  3. Copy "Project URL" → paste as SUPABASE_URL below
 *  4. Copy "anon public" key → paste as SUPABASE_ANON_KEY below
 *
 * The bucket name must match exactly what you created in Supabase Storage.
 * If you named it "resource-photos" during setup, leave BUCKET_NAME as is.
 */
public class SupabaseConfig {

    // Paste your Project URL here (looks like: https://xxxxxxxxxxxx.supabase.co)
    public static final String SUPABASE_URL = "https://YOUR_PROJECT_ID.supabase.co";

    // Paste your anon public key here (long string starting with "eyJ...")
    public static final String SUPABASE_ANON_KEY = "YOUR_ANON_KEY_HERE";

    // Must match the bucket name you created in Supabase Storage dashboard
    public static final String BUCKET_NAME = "resource-photos";

    // Full upload endpoint — do not change this
    public static final String STORAGE_URL =
        SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/";

    // Full public URL base for reading images — do not change this
    public static final String PUBLIC_URL =
        SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/";
}
