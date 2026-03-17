package com.campusshare;

import android.app.Application;
import android.content.Context;

/**
 * CampusShareApp is the Application class.
 * It holds a static reference to the app context so background threads
 * (like the Supabase upload thread) can access the ContentResolver
 * to read image bytes from a URI.
 *
 * Registered in AndroidManifest.xml via android:name=".CampusShareApp"
 */
public class CampusShareApp extends Application {

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }

    public static Context getAppContext() {
        return appContext;
    }
}
