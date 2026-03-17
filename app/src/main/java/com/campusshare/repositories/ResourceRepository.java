package com.campusshare.repositories;

import android.content.Context;
import android.net.Uri;

import com.campusshare.models.Resource;
import com.campusshare.utils.CloudinaryUploader;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ResourceRepository — Cloudinary version
 *
 * Database:      Firestore
 * Photo upload:  Cloudinary
 *
 * Sorting is handled in-memory to avoid requiring manual Firestore Indexes.
 */
public class ResourceRepository {

    private final FirebaseFirestore db;
    private final Context context;
    private static final String COLLECTION = "resources";

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

    public ResourceRepository(Context context) {
        this.db      = FirebaseFirestore.getInstance();
        this.context = context;
    }

    // ── Add Resource ──────────────────────────────────────────────────────────

    public void addResource(Resource resource, Uri photoUri, ResourceCallback callback) {
        if (photoUri != null) {
            uploadPhoto(context, photoUri, new PhotoUploadCallback() {
                @Override public void onSuccess(String url) {
                    resource.setPhotoUrl(url);
                    saveToFirestore(resource, callback);
                }
                @Override public void onFailure(String error) {
                    resource.setPhotoUrl("");
                    saveToFirestore(resource, callback);
                }
            });
        } else {
            saveToFirestore(resource, callback);
        }
    }

    private void saveToFirestore(Resource resource, ResourceCallback callback) {
        db.collection(COLLECTION)
            .add(resource)
            .addOnSuccessListener(docRef -> {
                resource.setResourceID(docRef.getId());
                docRef.update("resourceID", docRef.getId())
                    .addOnSuccessListener(v -> callback.onSuccess(resource))
                    .addOnFailureListener(e -> callback.onSuccess(resource));
            })
            .addOnFailureListener(e ->
                callback.onFailure("Failed to save resource: " + e.getMessage()));
    }

    // ── Upload Photo to Cloudinary ─────────────────────────────────────────────

    public static void uploadPhoto(Context context, Uri photoUri, PhotoUploadCallback callback) {
        CloudinaryUploader.uploadPhoto(context, photoUri, new CloudinaryUploader.UploadCallback() {
            @Override public void onSuccess(String url) { callback.onSuccess(url); }
            @Override public void onFailure(String error) { callback.onFailure(error); }
        });
    }

    // ── Fetch Available Resources ─────────────────────────────────────────────

    public void fetchAvailableResources(String currentUserID, ResourceListCallback callback) {
        db.collection(COLLECTION)
            .whereEqualTo("available", true)
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
                    }
                }
                // Sort by date descending (newest first)
                Collections.sort(list, (r1, r2) -> {
                    if (r1.getCreatedAt() == null || r2.getCreatedAt() == null) return 0;
                    return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                });
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure("Firestore Error: " + e.getMessage()));
    }

    // ── Fetch My Resources ────────────────────────────────────────────────────

    public void fetchMyResources(String userID, ResourceListCallback callback) {
        db.collection(COLLECTION)
            .whereEqualTo("ownerID", userID)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<Resource> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Resource r = doc.toObject(Resource.class);
                    if (r != null) list.add(r);
                }
                // Sort by date descending
                Collections.sort(list, (r1, r2) -> {
                    if (r1.getCreatedAt() == null || r2.getCreatedAt() == null) return 0;
                    return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                });
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure("Firestore Error: " + e.getMessage()));
    }

    // ── Fetch by Category ─────────────────────────────────────────────────────

    public void fetchByCategory(String category, String currentUserID,
                                ResourceListCallback callback) {
        db.collection(COLLECTION)
            .whereEqualTo("available", true)
            .whereEqualTo("category", category)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<Resource> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Resource r = doc.toObject(Resource.class);
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
            .addOnFailureListener(e -> callback.onFailure("Firestore Error: " + e.getMessage()));
    }

    // ── Update Resource ───────────────────────────────────────────────────────

    public void updateResource(Resource resource, Uri newPhotoUri, ResourceCallback callback) {
        if (newPhotoUri != null) {
            uploadPhoto(context, newPhotoUri, new PhotoUploadCallback() {
                @Override public void onSuccess(String url) {
                    resource.setPhotoUrl(url);
                    pushUpdate(resource, callback);
                }
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
            .addOnSuccessListener(v -> callback.onSuccess(resource))
            .addOnFailureListener(e -> callback.onFailure("Update failed: " + e.getMessage()));
    }

    // ── Set Availability ──────────────────────────────────────────────────────

    public void setAvailability(String resourceID, boolean available, SimpleCallback callback) {
        db.collection(COLLECTION)
            .document(resourceID)
            .update("available", available)
            .addOnSuccessListener(v -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ── Delete Resource ───────────────────────────────────────────────────────

    public void deleteResource(String resourceID, SimpleCallback callback) {
        db.collection(COLLECTION)
            .document(resourceID)
            .delete()
            .addOnSuccessListener(v -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onFailure("Delete failed: " + e.getMessage()));
    }

    // ── Fetch Single Resource ─────────────────────────────────────────────────

    public void fetchResource(String resourceID, ResourceCallback callback) {
        db.collection(COLLECTION)
            .document(resourceID)
            .get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot.exists())
                    callback.onSuccess(snapshot.toObject(Resource.class));
                else
                    callback.onFailure("Resource not found.");
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
