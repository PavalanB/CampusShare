package com.campusshare.repositories;

import com.campusshare.models.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * AuthRepository handles all Firebase Authentication and Firestore
 * user profile operations.
 */
public class AuthRepository {

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore db;

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

    public void login(String email, String password, UserProfileCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                if (authResult.getUser() != null) {
                    fetchUserProfile(authResult.getUser().getUid(), callback);
                } else {
                    callback.onFailure("Authentication failed: User is null");
                }
            })
            .addOnFailureListener(e -> callback.onFailure(getFriendlyError(e.getMessage())));
    }

    public void register(String email, String password, String name, String phone,
                         String department, String year, String collegeID,
                         UserProfileCallback callback) {

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                if (authResult.getUser() != null) {
                    String uid = authResult.getUser().getUid();
                    User newUser = new User(uid, name, email, phone, department, year, collegeID);

                    db.collection("users")
                        .document(uid)
                        .set(newUser)
                        .addOnSuccessListener(unused -> callback.onSuccess(newUser))
                        .addOnFailureListener(e -> callback.onFailure("Account created but profile save failed: " + e.getMessage()));
                } else {
                    callback.onFailure("Registration failed: User is null");
                }
            })
            .addOnFailureListener(e -> callback.onFailure(getFriendlyError(e.getMessage())));
    }

    public void updateUserName(String uid, String newName, UserProfileCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);

        db.collection("users")
            .document(uid)
            .update(updates)
            .addOnSuccessListener(unused -> fetchUserProfile(uid, callback))
            .addOnFailureListener(e -> callback.onFailure("Failed to update name: " + e.getMessage()));
    }

    public void sendPasswordReset(String email, AuthCallback callback) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener(unused -> callback.onSuccess(null))
            .addOnFailureListener(e -> callback.onFailure(getFriendlyError(e.getMessage())));
    }

    public void fetchUserProfile(String uid, UserProfileCallback callback) {
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    User user = snapshot.toObject(User.class);
                    if (user != null) {
                        callback.onSuccess(user);
                    } else {
                        callback.onFailure("User profile is empty.");
                    }
                } else {
                    callback.onFailure("User profile not found in database.");
                }
            })
            .addOnFailureListener(e -> callback.onFailure("Firestore Error: " + e.getMessage()));
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public void logout() {
        firebaseAuth.signOut();
    }

    private String getFriendlyError(String firebaseError) {
        if (firebaseError == null) return "Something went wrong. Please try again.";
        String lowerError = firebaseError.toLowerCase();

        if (lowerError.contains("email address is already in use") || lowerError.contains("already-in-use"))
            return "This email is already registered. Try logging in.";
        if (lowerError.contains("no user record") || lowerError.contains("user-not-found"))
            return "No account found with this email.";
        if (lowerError.contains("password is invalid") || lowerError.contains("wrong-password") || lowerError.contains("invalid_login_credentials"))
            return "Incorrect password or email. Please try again.";
        if (lowerError.contains("badly formatted") || lowerError.contains("invalid-email"))
            return "Please enter a valid email address.";
        if (lowerError.contains("network error") || lowerError.contains("network-request-failed"))
            return "Network error. Check your internet connection.";
        if (lowerError.contains("too many requests") || lowerError.contains("too-many-requests"))
            return "Too many attempts. Please try again later.";

        return "Authentication error: " + firebaseError;
    }
}
