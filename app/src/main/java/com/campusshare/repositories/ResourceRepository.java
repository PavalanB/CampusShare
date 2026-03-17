package com.campusshare.repositories;

import android.content.Context;
import android.net.Uri;

import com.campusshare.models.Resource;
import com.campusshare.utils.CloudinaryUploader;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * ResourceRepository handles all Firestore reads/writes for resources
 * and Firebase Storage uploads for resource photos.
 * ResourceRepository — Cloudinary version
 *
 * All Activities and Fragments call this class — never Firebase directly.
 * Database:      Firestore
 * Photo upload:  Cloudinary
 *
 * Sorting is handled in-memory to avoid requiring manual Firestore Indexes.
 */
public class ResourceRepository {

    private final FirebaseFirestore db;
    private final StorageReference storageRef;

    private final Context context;
    private static final String COLLECTION = "resources";

    // ─── Callback interfaces ──────────────────────────────────────────────────

    public interface ResourceCallback {
        void onSuccess(Resource resource);
        void onFailure(String error);
    }

    public interface ResourceListCallback {
        void onSuccess(List<Resource> resources);
        void onFailure(String error);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface PhotoUploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(String error);
    }

    public ResourceRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.storageRef = FirebaseStorage.getInstance().getReference();
    public ResourceRepository(Context context) {
        this.db      = FirebaseFirestore.getInstance();
        this.context = context;
    }

    // ─── Add Resource ─────────────────────────────────────────────────────────
    // ── Add Resource ──────────────────────────────────────────────────────────

    /**
     * Adds a new resource to Firestore. If a photo URI is provided,
     * uploads the photo first, then saves the resource with the download URL.
     */
    public void addResource(Resource resource, Uri photoUri, ResourceCallback callback) {
        if (photoUri != null) {
            // Upload photo first, then save resource
            uploadPhoto(photoUri, new PhotoUploadCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    resource.setPhotoUrl(downloadUrl);
                    saveResourceToFirestore(resource, callback);
            uploadPhoto(context, photoUri, new PhotoUploadCallback() {
                @Override public void onSuccess(String url) {
                    resource.setPhotoUrl(url);
                    saveToFirestore(resource, callback);
                }
                @Override public void onFailure(String error) {
                    resource.setPhotoUrl("");
                    saveToFirestore(resource, callback);
                @Override
                public void onFailure(String error) {
                    // Save resource without photo rather than failing entirely
                    saveResourceToFirestore(resource, callback);
                }
            });
        } else {
            saveToFirestore(resource, callback);
            saveResourceToFirestore(resource, callback);
        }
    }

    private void saveToFirestore(Resource resource, ResourceCallback callback) {
    private void saveResourceToFirestore(Resource resource, ResourceCallback callback) {
        // Let Firestore auto-generate the document ID
        db.collection(COLLECTION)
            .add(resource)
            .addOnSuccessListener(docRef -> {
                // Write the generated ID back into the document
                resource.setResourceID(docRef.getId());
                docRef.update("resourceID", docRef.getId())
                    .addOnSuccessListener(v -> callback.onSuccess(resource))
                    .addOnFailureListener(e -> callback.onSuccess(resource));
                    .addOnSuccessListener(unused -> callback.onSuccess(resource))
                    .addOnFailureListener(e -> callback.onSuccess(resource)); // non-critical failure
            })
            .addOnFailureListener(e ->
                callback.onFailure("Failed to save resource: " + e.getMessage()));
            .addOnFailureListener(e -> callback.onFailure("Failed to add resource: " + e.getMessage()));
    }

    // ── Upload Photo to Cloudinary ─────────────────────────────────────────────
    // ─── Upload Photo ─────────────────────────────────────────────────────────

    public static void uploadPhoto(Context context, Uri photoUri, PhotoUploadCallback callback) {
        CloudinaryUploader.uploadPhoto(context, photoUri, new CloudinaryUploader.UploadCallback() {
            @Override public void onSuccess(String url) { callback.onSuccess(url); }
            @Override public void onFailure(String error) { callback.onFailure(error); }
        });
    /**
     * Uploads a photo to Firebase Storage under /resource_photos/{uuid}.jpg
     * and returns the public download URL.
     */
    public void uploadPhoto(Uri photoUri, PhotoUploadCallback callback) {
        // Unique filename prevents collisions
        String filename = "resource_photos/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference photoRef = storageRef.child(filename);

        photoRef.putFile(photoUri)
            .addOnSuccessListener(taskSnapshot ->
                photoRef.getDownloadUrl()
                    .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                    .addOnFailureListener(e -> callback.onFailure("Could not get photo URL"))
            )
            .addOnFailureListener(e -> callback.onFailure("Photo upload failed: " + e.getMessage()));
    }

    // ── Fetch Available Resources ─────────────────────────────────────────────
    // ─── Fetch All Available Resources ───────────────────────────────────────

    /**
     * Fetches all available resources, excluding the current user's own listings.
     * Ordered by newest first.
     */
    public void fetchAvailableResources(String currentUserID, ResourceListCallback callback) {
        db.collection(COLLECTION)
            .whereEqualTo("available", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<Resource> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Resource r = doc.toObject(Resource.class);
                    if (r != null && r.getOwnerID() != null) {
                        // Don't show own resources in the browse feed
                        if (!r.getOwnerID().equals(currentUserID)) {
                            list.add(r);
                        }
                    // Don't show the user their own resources in the browse feed
                    if (r.getOwnerID() != null && !r.getOwnerID().equals(currentUserID)) {
                        list.add(r);
                    }
                }
                // Sort by date descending (newest first)
                Collections.sort(list, (r1, r2) -> {
                    if (r1.getCreatedAt() == null || r2.getCreatedAt() == null) return 0;
                    return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                });
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            .addOnFailureListener(e -> callback.onFailure("Firestore Error: " + e.getMessage()));
    }

    // ── Fetch My Resources ────────────────────────────────────────────────────
    // ─── Fetch My Listings ────────────────────────────────────────────────────

    /**
     * Fetches all resources listed by the current user (their "My Listings" screen).
     */
    public void fetchMyResources(String userID, ResourceListCallback callback) {
        db.collection(COLLECTION)
            .whereEqualTo("ownerID", userID)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<Resource> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Resource r = doc.toObject(Resource.class);
                    if (r != null) list.add(r);
                    list.add(doc.toObject(Resource.class));
                }
                // Sort by date descending
                Collections.sort(list, (r1, r2) -> {
                    if (r1.getCreatedAt() == null || r2.getCreatedAt() == null) return 0;
                    return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                });
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure("Firestore Error: " + e.getMessage()));
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ── Fetch by Category ─────────────────────────────────────────────────────
    // ─── Fetch by Category ────────────────────────────────────────────────────

    public void fetchByCategory(String category, String currentUserID,
                                ResourceListCallback callback) {
    public void fetchByCategory(String category, String currentUserID, ResourceListCallback callback) {
        db.collection(COLLECTION)
            .whereEqualTo("available", true)
            .whereEqualTo("category", category)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<Resource> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Resource r = doc.toObject(Resource.class);
                    if (r.getOwnerID() != null && !r.getOwnerID().equals(currentUserID)) {
                        list.add(r);
                    if (r != null && r.getOwnerID() != null) {
                        if (!r.getOwnerID().equals(currentUserID)) {
                            list.add(r);
                        }
                    }
                }
                // Sort by date descending
                Collections.sort(list, (r1, r2) -> {
                    if (r1.getCreatedAt() == null || r2.getCreatedAt() == null) return 0;
                    return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                });
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            .addOnFailureListener(e -> callback.onFailure("Firestore Error: " + e.getMessage()));
    }

    // ─── Update Resource ──────────────────────────────────────────────────────
    // ── Update Resource ───────────────────────────────────────────────────────

    public void updateResource(Resource resource, Uri newPhotoUri, ResourceCallback callback) {
        if (newPhotoUri != null) {
            uploadPhoto(newPhotoUri, new PhotoUploadCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    resource.setPhotoUrl(downloadUrl);
            uploadPhoto(context, newPhotoUri, new PhotoUploadCallback() {
                @Override public void onSuccess(String url) {
                    resource.setPhotoUrl(url);
                    pushUpdate(resource, callback);
                }
                @Override
                public void onFailure(String error) {
                @Override public void onFailure(String error) {
                    pushUpdate(resource, callback);
                }
            });
        } else {
            pushUpdate(resource, callback);
        }
    }

    private void pushUpdate(Resource resource, ResourceCallback callback) {
        db.collection(COLLECTION)
            .document(resource.getResourceID())
            .set(resource)
            .addOnSuccessListener(unused -> callback.onSuccess(resource))
            .addOnSuccessListener(v -> callback.onSuccess(resource))
            .addOnFailureListener(e -> callback.onFailure("Update failed: " + e.getMessage()));
    }

    // ─── Toggle Availability ──────────────────────────────────────────────────
    // ── Set Availability ──────────────────────────────────────────────────────

    public void setAvailability(String resourceID, boolean available, SimpleCallback callback) {
        db.collection(COLLECTION)
            .document(resourceID)
            .update("available", available)
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnSuccessListener(v -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ─── Delete Resource ──────────────────────────────────────────────────────
    // ── Delete Resource ───────────────────────────────────────────────────────

    public void deleteResource(String resourceID, SimpleCallback callback) {
        db.collection(COLLECTION)
            .document(resourceID)
            .delete()
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnSuccessListener(v -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onFailure("Delete failed: " + e.getMessage()));
    }

    // ─── Fetch Single Resource ────────────────────────────────────────────────
    // ── Fetch Single Resource ─────────────────────────────────────────────────

    public void fetchResource(String resourceID, ResourceCallback callback) {
        db.collection(COLLECTION)
            .document(resourceID)
            .get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                if (snapshot.exists())
                    callback.onSuccess(snapshot.toObject(Resource.class));
                } else {
                else
                    callback.onFailure("Resource not found.");
                }
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
