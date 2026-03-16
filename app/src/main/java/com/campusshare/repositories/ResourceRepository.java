package com.campusshare.repositories;

import android.net.Uri;

import com.campusshare.models.Resource;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ResourceRepository handles all Firestore reads/writes for resources
 * and Firebase Storage uploads for resource photos.
<<<<<<< HEAD
 *
 * All Activities and Fragments call this class — never Firebase directly.
=======
>>>>>>> 7f31e5da9ccded4a3555fe38e2ea6a769e9225c3
 */
public class ResourceRepository {

    private final FirebaseFirestore db;
    private final StorageReference storageRef;

    private static final String COLLECTION = "resources";

<<<<<<< HEAD
    // ─── Callback interfaces ──────────────────────────────────────────────────

=======
>>>>>>> 7f31e5da9ccded4a3555fe38e2ea6a769e9225c3
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
    }

<<<<<<< HEAD
    // ─── Add Resource ─────────────────────────────────────────────────────────

    /**
     * Adds a new resource to Firestore. If a photo URI is provided,
     * uploads the photo first, then saves the resource with the download URL.
     */
    public void addResource(Resource resource, Uri photoUri, ResourceCallback callback) {
        if (photoUri != null) {
            // Upload photo first, then save resource
=======
    public void addResource(Resource resource, Uri photoUri, ResourceCallback callback) {
        if (photoUri != null) {
>>>>>>> 7f31e5da9ccded4a3555fe38e2ea6a769e9225c3
            uploadPhoto(photoUri, new PhotoUploadCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    resource.setPhotoUrl(downloadUrl);
                    saveResourceToFirestore(resource, callback);
                }
                @Override
                public void onFailure(String error) {
<<<<<<< HEAD
                    // Save resource without photo rather than failing entirely
=======
>>>>>>> 7f31e5da9ccded4a3555fe38e2ea6a769e9225c3
                    saveResourceToFirestore(resource, callback);
                }
            });
        } else {
            saveResourceToFirestore(resource, callback);
        }
    }

    private void saveResourceToFirestore(Resource resource, ResourceCallback callback) {
<<<<<<< HEAD
        // Let Firestore auto-generate the document ID
        db.collection(COLLECTION)
            .add(resource)
            .addOnSuccessListener(docRef -> {
                // Write the generated ID back into the document
                resource.setResourceID(docRef.getId());
                docRef.update("resourceID", docRef.getId())
                    .addOnSuccessListener(unused -> callback.onSuccess(resource))
                    .addOnFailureListener(e -> callback.onSuccess(resource)); // non-critical failure
=======
        db.collection(COLLECTION)
            .add(resource)
            .addOnSuccessListener(docRef -> {
                resource.setResourceID(docRef.getId());
                docRef.update("resourceID", docRef.getId())
                    .addOnSuccessListener(unused -> callback.onSuccess(resource))
                    .addOnFailureListener(e -> callback.onSuccess(resource));
>>>>>>> 7f31e5da9ccded4a3555fe38e2ea6a769e9225c3
            })
            .addOnFailureListener(e -> callback.onFailure("Failed to add resource: " + e.getMessage()));
    }

<<<<<<< HEAD
    // ─── Upload Photo ─────────────────────────────────────────────────────────

    /**
     * Uploads a photo to Firebase Storage under /resource_photos/{uuid}.jpg
     * and returns the public download URL.
     */
    public void uploadPhoto(Uri photoUri, PhotoUploadCallback callback) {
        // Unique filename prevents collisions
=======
    public void uploadPhoto(Uri photoUri, PhotoUploadCallback callback) {
>>>>>>> 7f31e5da9ccded4a3555fe38e2ea6a769e9225c3
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

<<<<<<< HEAD
    // ─── Fetch All Available Resources ───────────────────────────────────────

    /**
     * Fetches all available resources, excluding the current user's own listings.
     * Ordered by newest first.
     */
=======
>>>>>>> 7f31e5da9ccded4a3555fe38e2ea6a769e9225c3
    public void fetchAvailableResources(String currentUserID, ResourceListCallback callback) {
        db.collection(COLLECTION)
            .whereEqualTo("available", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<Resource> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Resource r = doc.toObject(Resource.class);
<<<<<<< HEAD
                    // Don't show the user their own resources in the browse feed
                    if (!r.getOwnerID().equals(currentUserID)) {
=======
                    if (r.getOwnerID() != null && !r.getOwnerID().equals(currentUserID)) {
>>>>>>> 7f31e5da9ccded4a3555fe38e2ea6a769e9225c3
                        list.add(r);
                    }
                }
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

<<<<<<< HEAD
    // ─── Fetch My Listings ────────────────────────────────────────────────────

    /**
     * Fetches all resources listed by the current user (their "My Listings" screen).
     */
=======
>>>>>>> 7f31e5da9ccded4a3555fe38e2ea6a769e9225c3
    public void fetchMyResources(String userID, ResourceListCallback callback) {
        db.collection(COLLECTION)
            .whereEqualTo("ownerID", userID)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<Resource> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    list.add(doc.toObject(Resource.class));
                }
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

<<<<<<< HEAD
    // ─── Fetch by Category ────────────────────────────────────────────────────

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
                    if (!r.getOwnerID().equals(currentUserID)) {
                        list.add(r);
                    }
                }
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ─── Update Resource ──────────────────────────────────────────────────────

=======
>>>>>>> 7f31e5da9ccded4a3555fe38e2ea6a769e9225c3
    public void updateResource(Resource resource, Uri newPhotoUri, ResourceCallback callback) {
        if (newPhotoUri != null) {
            uploadPhoto(newPhotoUri, new PhotoUploadCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    resource.setPhotoUrl(downloadUrl);
                    pushUpdate(resource, callback);
                }
                @Override
                public void onFailure(String error) {
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
            .addOnFailureListener(e -> callback.onFailure("Update failed: " + e.getMessage()));
    }

<<<<<<< HEAD
    // ─── Toggle Availability ──────────────────────────────────────────────────

    public void setAvailability(String resourceID, boolean available, SimpleCallback callback) {
        db.collection(COLLECTION)
            .document(resourceID)
            .update("available", available)
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ─── Delete Resource ──────────────────────────────────────────────────────

=======
>>>>>>> 7f31e5da9ccded4a3555fe38e2ea6a769e9225c3
    public void deleteResource(String resourceID, SimpleCallback callback) {
        db.collection(COLLECTION)
            .document(resourceID)
            .delete()
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onFailure("Delete failed: " + e.getMessage()));
    }
<<<<<<< HEAD

    // ─── Fetch Single Resource ────────────────────────────────────────────────

    public void fetchResource(String resourceID, ResourceCallback callback) {
        db.collection(COLLECTION)
            .document(resourceID)
            .get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    callback.onSuccess(snapshot.toObject(Resource.class));
                } else {
                    callback.onFailure("Resource not found.");
                }
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
=======
>>>>>>> 7f31e5da9ccded4a3555fe38e2ea6a769e9225c3
}
