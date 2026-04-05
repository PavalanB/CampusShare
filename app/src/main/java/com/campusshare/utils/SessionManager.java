package com.campusshare.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.campusshare.models.User;

/**
 * SessionManager stores the logged-in user's basic info in SharedPreferences
 * so the app doesn't need to fetch Firestore on every screen load.
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
    private static final String KEY_PROFILE_PHOTO = "profilePhoto";
    private static final String KEY_CREDIT_SCORE = "creditScore";
    private static final String KEY_AVG_RATING = "avgRating";
    private static final String KEY_TOTAL_RATINGS = "totalRatings";
    private static final String KEY_TOTAL_BORROWS = "totalBorrows";
    private static final String KEY_TOTAL_LENDS = "totalLends";
    private static final String KEY_FCM_TOKEN = "fcmToken";

    public static void saveUser(Context context, User user) {
        if (user == null) return;
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
        editor.putString(KEY_PROFILE_PHOTO, user.getProfilePhoto());
        editor.putFloat(KEY_CREDIT_SCORE, (float) user.getCreditScore());
        editor.putFloat(KEY_AVG_RATING, (float) user.getAvgRating());
        editor.putInt(KEY_TOTAL_RATINGS, user.getTotalRatings());
        editor.putInt(KEY_TOTAL_BORROWS, user.getTotalBorrows());
        editor.putInt(KEY_TOTAL_LENDS, user.getTotalLends());
        editor.putString(KEY_FCM_TOKEN, user.getFcmToken());
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
        user.setProfilePhoto(prefs.getString(KEY_PROFILE_PHOTO, ""));
        user.setCreditScore(prefs.getFloat(KEY_CREDIT_SCORE, 0f));
        user.setAvgRating(prefs.getFloat(KEY_AVG_RATING, 0f));
        user.setTotalRatings(prefs.getInt(KEY_TOTAL_RATINGS, 0));
        user.setTotalBorrows(prefs.getInt(KEY_TOTAL_BORROWS, 0));
        user.setTotalLends(prefs.getInt(KEY_TOTAL_LENDS, 0));
        user.setFcmToken(prefs.getString(KEY_FCM_TOKEN, ""));
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
}
