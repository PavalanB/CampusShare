package com.campusshare.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.campusshare.models.User;

/**
 * SessionManager stores the logged-in user's basic info and theme preferences.
 */
public class SessionManager {

    private static final String PREF_NAME = "CampusShareSession";
    private static final String KEY_USER_ID = "userID";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_DEPARTMENT = "department";
    private static final String KEY_YEAR = "year";
    private static final String KEY_COLLEGE_ID = "collegeID";
    private static final String KEY_CREDIT_SCORE = "creditScore";
    private static final String KEY_AVG_RATING = "avgRating";
    private static final String KEY_THEME_MODE = "themeMode";

    public static void saveUser(Context context, User user) {
        SharedPreferences.Editor editor = context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit();

        editor.putString(KEY_USER_ID, user.getUserID());
        editor.putString(KEY_NAME, user.getName());
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putString(KEY_PHONE, user.getPhone());
        editor.putString(KEY_DEPARTMENT, user.getDepartment());
        editor.putString(KEY_YEAR, user.getYear());
        editor.putString(KEY_COLLEGE_ID, user.getCollegeID());
        editor.putFloat(KEY_CREDIT_SCORE, (float) user.getCreditScore());
        editor.putFloat(KEY_AVG_RATING, (float) user.getAvgRating());
        editor.apply();
    }

    public static User getUser(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        String userID = prefs.getString(KEY_USER_ID, null);
        if (userID == null) return null; // Not logged in

        User user = new User(
            userID,
            prefs.getString(KEY_NAME, ""),
            prefs.getString(KEY_EMAIL, ""),
            prefs.getString(KEY_PHONE, ""),
            prefs.getString(KEY_DEPARTMENT, ""),
            prefs.getString(KEY_YEAR, ""),
            prefs.getString(KEY_COLLEGE_ID, "")
        );
        user.setCreditScore(prefs.getFloat(KEY_CREDIT_SCORE, 0f));
        user.setAvgRating(prefs.getFloat(KEY_AVG_RATING, 0f));
        return user;
    }

    public static String getUserID(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_ID, null);
    }

    public static boolean isLoggedIn(Context context) {
        return getUserID(context) != null;
    }

    public static void clearSession(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply();
    }

    public static void setThemeMode(Context context, int mode) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_THEME_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static int getThemeMode(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static void applyTheme(Context context) {
        AppCompatDelegate.setDefaultNightMode(getThemeMode(context));
    }
}
