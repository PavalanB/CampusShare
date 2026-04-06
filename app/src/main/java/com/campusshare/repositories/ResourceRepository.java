package com.campusshare.repositories;

import android.content.Context;
import android.net.Uri;

import com.campusshare.models.Resource;
import com.campusshare.utils.CloudinaryUploader;
import com.campusshare.models.BorrowRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ResourceRepository handles all Firestore reads/writes for resources
 * and Cloudinary uploads for resource photos.
 */
public class ResourceRepository {

    private final FirebaseFirestore db;
    private final Context context;
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

    public ResourceRepository(Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.context = context;
    }

    public void addResource(Resource resource, Uri photoUri, ResourceCallback callback) {
        if (photoUri != null) {
            uploadPhoto(context, photoUri, new PhotoUploadCallback() {
                @Override
                public void onSuccess(String url) {
                    resource.setPhotoUrl(url);
                    saveToFirestore(resource, callback);
                }
                @Override
                public void onFailure(String error) {
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
                    .addOnSuccessListener(unused -> callback.onSuccess(resource))
                    .addOnFailureListener(e -> callback.onSuccess(resource));
            })
            .addOnFailureListener(e -> callback.onFailure("Failed to add resource: " + e.getMessage()));
    }

    public static void uploadPhoto(Context context, Uri photoUri, PhotoUploadCallback callback) {
        CloudinaryUploader.uploadPhoto(context, photoUri, new CloudinaryUploader.UploadCallback() {
            @Override public void onSuccess(String url) { callback.onSuccess(url); }
            @Override public void onFailure(String error) { callback.onFailure(error); }
        });
    }

    public void fetchAvailableResources(String currentUserID, ResourceListCallback callback) {
        db.collection(COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<Resource> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Resource r = doc.toObject(Resource.class);
                    if (r != null && r.getOwnerID() != null) {
                        // Don't show my own resources in browse
                        if (!r.getOwnerID().equals(currentUserID)) {
                            list.add(r);
                        }
                    }
                }
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure("Firestore Error: " + e.getMessage()));
    }

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
                }
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure("Firestore Error: " + e.getMessage()));
    }

    public void fetchByCategory(String category, String currentUserID, ResourceListCallback callback) {
        db.collection(COLLECTION)
            .whereEqualTo("category", category)
            .orderBy("createdAt", Query.Direction.DESCENDING)
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
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure("Firestore Error: " + e.getMessage()));
    }

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
            .addOnSuccessListener(unused -> callback.onSuccess(resource))
            .addOnFailureListener(e -> callback.onFailure("Update failed: " + e.getMessage()));
    }

    public void setAvailability(String resourceID, boolean available, SimpleCallback callback) {
        db.collection(COLLECTION)
            .document(resourceID)
            .update("available", available)
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void deleteResource(String resourceID, SimpleCallback callback) {
        db.collection(COLLECTION)
            .document(resourceID)
            .delete()
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onFailure("Delete failed: " + e.getMessage()));
    }

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

    /**
     * Checks if a resource is available for a given date range and quantity.
     * Returns an error message if there's a conflict.
     */
    public void checkAvailability(String resourceID, Date start, Date end, int requestedQty, int totalAvailable, SimpleCallback callback) {
        db.collection(REQUESTS_COLLECTION)
            .whereEqualTo("resourceID", resourceID)
            .whereIn("status", List.of(BorrowRequest.STATUS_ACCEPTED, BorrowRequest.STATUS_ONGOING, BorrowRequest.STATUS_EXTENSION_PENDING))
            .get()
            .addOnSuccessListener(snapshots -> {
                int occupied = 0;
                Date firstConflictStart = null;
                Date lastConflictEnd = null;

                for (QueryDocumentSnapshot doc : snapshots) {
                    BorrowRequest br = doc.toObject(BorrowRequest.class);
                    // Use effective dates for robust check
                    Date brStart = br.getEffectiveStartDate();
                    Date brEnd = br.getEffectiveEndDate();

                    // Overlap check: (start < brEnd) and (end > brStart)
                    if (start.before(brEnd) && end.after(brStart)) {
                        occupied += Math.max(1, br.getQuantity());
                        if (firstConflictStart == null || brStart.before(firstConflictStart)) firstConflictStart = brStart;
                        if (lastConflictEnd == null || brEnd.after(lastConflictEnd)) lastConflictEnd = brEnd;
                    }
                }

                if (occupied + requestedQty > totalAvailable) {
                    SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    String msg = "Resource is already borrowed by another user";
                    if (firstConflictStart != null && lastConflictEnd != null) {
                        msg += " from " + fmt.format(firstConflictStart) + " to " + fmt.format(lastConflictEnd) + ".";
                        msg += " It will be available after " + fmt.format(lastConflictEnd) + ".";
                    } else {
                        msg += ".";
                    }
                    callback.onFailure(msg);
                } else {
                    callback.onSuccess();
                }
            })
            .addOnFailureListener(e -> callback.onFailure("Availability check failed: " + e.getMessage()));
    }

    public void fetchMyBorrowRequests(String userID, BorrowRequestListCallback callback) {
        db.collection(REQUESTS_COLLECTION)
            .whereEqualTo("borrowerID", userID)
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
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        
        if (BorrowRequest.STATUS_ACCEPTED.equals(newStatus)) {
            updates.put("acceptedDate", new Date());
        } else if (BorrowRequest.STATUS_RETURNED.equals(newStatus) || "COMPLETED".equals(newStatus) || "OVERDUE_RETURNED".equals(newStatus)) {
            updates.put("returnedDate", new Date());
        }

        db.collection(REQUESTS_COLLECTION)
            .document(requestID)
            .update(updates)
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
