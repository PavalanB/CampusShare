package com.campusshare.repositories;

import android.net.Uri;

import com.campusshare.models.Resource;
import com.campusshare.models.BorrowRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Date;
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
    private static final String REQUESTS_COLLECTION = "borrow_requests";

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

    public interface BorrowRequestListCallback {
        void onSuccess(List<BorrowRequest> requests);
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

    // --- Borrow Request Methods ---

    public void addBorrowRequest(BorrowRequest request, SimpleCallback callback) {
        db.collection(REQUESTS_COLLECTION)
            .add(request)
            .addOnSuccessListener(docRef -> {
                request.setRequestID(docRef.getId());
                docRef.update("requestID", docRef.getId())
                    .addOnSuccessListener(unused -> callback.onSuccess())
                    .addOnFailureListener(e -> callback.onSuccess());
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void checkAvailability(String resourceID, Date start, Date end, int requestedQty, int totalAvailable, BorrowRequestListCallback callback) {
        db.collection(REQUESTS_COLLECTION)
            .whereEqualTo("resourceID", resourceID)
            .whereIn("status", List.of("APPROVED", "ONGOING", "PENDING"))
            .get()
            .addOnSuccessListener(snapshots -> {
                List<BorrowRequest> activeRequests = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    BorrowRequest br = doc.toObject(BorrowRequest.class);
                    // Check for overlap: (start1 <= end2) and (end1 >= start2)
                    if (start.before(br.getEndDate()) && end.after(br.getStartDate())) {
                        activeRequests.add(br);
                    }
                }
                callback.onSuccess(activeRequests);
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void fetchMyBorrowRequests(String userID, BorrowRequestListCallback callback) {
        db.collection(REQUESTS_COLLECTION)
            .whereEqualTo("borrowerID", userID)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<BorrowRequest> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    list.add(doc.toObject(BorrowRequest.class));
                }
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void fetchReceivedBorrowRequests(String userID, BorrowRequestListCallback callback) {
        db.collection(REQUESTS_COLLECTION)
            .whereEqualTo("ownerID", userID)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<BorrowRequest> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    list.add(doc.toObject(BorrowRequest.class));
                }
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void updateBorrowRequestStatus(String requestID, String newStatus, SimpleCallback callback) {
        db.collection(REQUESTS_COLLECTION)
            .document(requestID)
            .update("status", newStatus)
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void extendBorrowRequest(String requestID, Date newEndDate, SimpleCallback callback) {
        db.collection(REQUESTS_COLLECTION)
            .document(requestID)
            .update("endDate", newEndDate)
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
