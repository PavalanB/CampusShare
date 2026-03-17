package com.campusshare.repositories;

import com.campusshare.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * AuthRepository handles all Firebase Authentication and Firestore
 * user profile operations. Activities never call Firebase directly —
 * they always go through this class.
 */
public class AuthRepository {

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore db;

    // Callback interfaces so Activities can respond to async results
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(String errorMessage);
    }

    public interface UserProfileCallback {
        void onSuccess(User user);
        void onFailure(String errorMessage);
    }

    public AuthRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    /**
     * Logs in with email and password using Firebase Auth.
     * On success, fetches the user's Firestore profile and returns it.
     */
    public void login(String email, String password, UserProfileCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                // Auth succeeded — now fetch their Firestore profile
                String uid = authResult.getUser().getUid();
                fetchUserProfile(uid, callback);
            })
            .addOnFailureListener(e -> callback.onFailure(getFriendlyError(e.getMessage())));
    }

    // ─── Register ─────────────────────────────────────────────────────────────

    /**
     * Creates a new Firebase Auth account, then saves the user profile
     * document in Firestore under /users/{uid}.
     */
    public void register(String email, String password, String name, String phone,
                         String department, String year, String collegeID,
                         UserProfileCallback callback) {

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                String uid = authResult.getUser().getUid();

                // Build the user model
                User newUser = new User(uid, name, email, phone, department, year, collegeID);

                // Save to Firestore /users/{uid}
                db.collection("users")
                    .document(uid)
                    .set(newUser)
                    .addOnSuccessListener(unused -> callback.onSuccess(newUser))
                    .addOnFailureListener(e -> callback.onFailure("Account created but profile save failed: " + e.getMessage()));
            })
            .addOnFailureListener(e -> callback.onFailure(getFriendlyError(e.getMessage())));
    }

    // ─── Forgot Password ──────────────────────────────────────────────────────

    /**
     * Sends a password reset email via Firebase Auth.
     */
    public void sendPasswordReset(String email, AuthCallback callback) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener(unused -> callback.onSuccess(null))
            .addOnFailureListener(e -> callback.onFailure(getFriendlyError(e.getMessage())));
    }

    // ─── Fetch Profile ────────────────────────────────────────────────────────

    /**
     * Reads the user document from Firestore and returns a User object.
     */
    public void fetchUserProfile(String uid, UserProfileCallback callback) {
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    User user = snapshot.toObject(User.class);
                    callback.onSuccess(user);
                } else {
                    callback.onFailure("User profile not found.");
                }
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ─── Session Helpers ──────────────────────────────────────────────────────

    /** Returns the currently logged-in Firebase user, or null if not logged in. */
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /** Signs out the current user from Firebase Auth. */
    public void logout() {
        firebaseAuth.signOut();
    }

    // ─── Error Translator ─────────────────────────────────────────────────────

    /**
     * Converts Firebase error messages into plain English for users.
     */
    private String getFriendlyError(String firebaseError) {
        if (firebaseError == null) return "Something went wrong. Please try again.";
        String lower = firebaseError.toLowerCase();
        
        if (lower.contains("email address is already in use"))
            return "This email is already registered. Try logging in.";
        if (lower.contains("no user record"))
            return "No account found with this email.";
        if (lower.contains("password is invalid") || lower.contains("invalid_login_credentials"))
            return "Incorrect password. Please try again.";
        if (lower.contains("badly formatted"))
            return "Please enter a valid email address.";
        if (lower.contains("network error") || lower.contains("network_error"))
            return "Network error. Check your internet connection.";
            
        // If it's none of the above, return the actual error so we can debug it
        return "Error: " + firebaseError;
    }
}
