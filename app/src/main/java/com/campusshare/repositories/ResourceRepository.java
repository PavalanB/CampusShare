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
 */
public class ResourceRepository {

    private final FirebaseFirestore db;
    private final StorageReference storageRef;

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

    public ResourceRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.storageRef = FirebaseStorage.getInstance().getReference();
    }

    public void addResource(Resource resource, Uri photoUri, ResourceCallback callback) {
        if (photoUri != null) {
            uploadPhoto(photoUri, new PhotoUploadCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    resource.setPhotoUrl(downloadUrl);
                    saveResourceToFirestore(resource, callback);
                }
                @Override
                public void onFailure(String error) {
                    saveResourceToFirestore(resource, callback);
                }
            });
        } else {
            saveResourceToFirestore(resource, callback);
        }
    }

    private void saveResourceToFirestore(Resource resource, ResourceCallback callback) {
        db.collection(COLLECTION)
            .add(resource)
            .addOnSuccessListener(docRef -> {
                resource.setResourceID(docRef.getId());
                docRef.update("resourceID", docRef.getId())
                    .addOnSuccessListener(unused -> callback.onSuccess(resource))
                    .addOnFailureListener(e -> callback.onSuccess(resource));
            })
            .addOnFailureListener(e -> callback.onFailure("Failed to add resource: " + e.getMessage()));
    }

    public void uploadPhoto(Uri photoUri, PhotoUploadCallback callback) {
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

    public void fetchAvailableResources(String currentUserID, ResourceListCallback callback) {
        db.collection(COLLECTION)
            .whereEqualTo("available", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<Resource> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Resource r = doc.toObject(Resource.class);
                    if (r.getOwnerID() != null && !r.getOwnerID().equals(currentUserID)) {
                        list.add(r);
                    }
                }
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

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

    public void deleteResource(String resourceID, SimpleCallback callback) {
        db.collection(COLLECTION)
            .document(resourceID)
            .delete()
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onFailure("Delete failed: " + e.getMessage()));
    }
}
