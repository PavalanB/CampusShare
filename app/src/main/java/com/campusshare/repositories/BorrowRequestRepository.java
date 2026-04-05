package com.campusshare.repositories;

import android.content.Context;

import com.campusshare.models.BorrowRequest;
import com.campusshare.models.Resource;
import com.campusshare.utils.CreditManager;
import com.campusshare.utils.NotificationHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * BorrowRequestRepository — all Firestore operations for borrow requests.
 */
public class BorrowRequestRepository {

    private final FirebaseFirestore db;
    private final ResourceRepository resourceRepository;
    private final CreditManager creditManager;
    private static final String COLLECTION = "borrow_requests";

    public interface RequestCallback {
        void onSuccess(BorrowRequest request);
        void onFailure(String error);
    }

    public interface RequestListCallback {
        void onSuccess(List<BorrowRequest> requests);
        void onFailure(String error);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public BorrowRequestRepository(Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.resourceRepository = new ResourceRepository(context);
        this.creditManager = new CreditManager();
    }

    public void sendRequest(BorrowRequest request, RequestCallback callback) {
        // Before sending, check if already borrowed for these dates
        resourceRepository.fetchResource(request.getResourceID(), new ResourceRepository.ResourceCallback() {
            @Override
            public void onSuccess(Resource resource) {
                checkForConflicts(request, resource, new SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        performSend(request, callback);
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onFailure(error);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure("Could not fetch resource: " + error);
            }
        });
    }

    private void performSend(BorrowRequest request, RequestCallback callback) {
        db.collection(COLLECTION)
            .add(request)
            .addOnSuccessListener(docRef -> {
                request.setRequestID(docRef.getId());
                docRef.update("requestID", docRef.getId())
                    .addOnSuccessListener(v -> {
                        NotificationHelper.notifyRequestReceived(request);
                        callback.onSuccess(request);
                    })
                    .addOnFailureListener(e -> {
                        NotificationHelper.notifyRequestReceived(request);
                        callback.onSuccess(request);
                    });
            })
            .addOnFailureListener(e -> callback.onFailure("Failed to send request: " + e.getMessage()));
    }

    public void acceptRequest(BorrowRequest request, SimpleCallback callback) {
        // First check for conflicts again to be safe
        resourceRepository.fetchResource(request.getResourceID(), new ResourceRepository.ResourceCallback() {
            @Override
            public void onSuccess(Resource resource) {
                checkForConflicts(request, resource, new SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        performAcceptance(request, resource, callback);
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onFailure(error);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure("Could not fetch resource: " + error);
            }
        });
    }

    public void checkForConflicts(BorrowRequest request, Resource resource, SimpleCallback callback) {
        Date start = request.getEffectiveStartDate();
        Date end = request.getEffectiveEndDate();

        db.collection(COLLECTION)
            .whereEqualTo("resourceID", request.getResourceID())
            .whereIn("status", List.of(BorrowRequest.STATUS_ACCEPTED, BorrowRequest.STATUS_ONGOING, BorrowRequest.STATUS_EXTENSION_PENDING))
            .get()
            .addOnSuccessListener(snapshots -> {
                int occupied = 0;
                Date firstConflictStart = null;
                Date lastConflictEnd = null;

                for (QueryDocumentSnapshot doc : snapshots) {
                    BorrowRequest br = doc.toObject(BorrowRequest.class);
                    // Ensure requestID is populated even if not in doc fields
                    if (br.getRequestID() == null) br.setRequestID(doc.getId());

                    // Skip if comparing against itself
                    if (request.getRequestID() != null && br.getRequestID().equals(request.getRequestID())) continue;

                    // Overlap check
                    if (start.before(br.getEffectiveEndDate()) && end.after(br.getEffectiveStartDate())) {
                        occupied += Math.max(1, br.getQuantity());
                        if (firstConflictStart == null || br.getEffectiveStartDate().before(firstConflictStart)) {
                            firstConflictStart = br.getEffectiveStartDate();
                        }
                        if (lastConflictEnd == null || br.getEffectiveEndDate().after(lastConflictEnd)) {
                            lastConflictEnd = br.getEffectiveEndDate();
                        }
                    }
                }

                if (occupied + request.getQuantity() > resource.getTotalQuantity()) {
                    SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    String msg = "Resource is already borrowed by another user";
                    if (firstConflictStart != null && lastConflictEnd != null) {
                        msg += " from " + fmt.format(firstConflictStart) + " to " + fmt.format(lastConflictEnd) + ".";
                        msg += " It will be available after " + fmt.format(lastConflictEnd) + ".";
                    } else if (lastConflictEnd != null) {
                        msg += ". It will be available after " + fmt.format(lastConflictEnd) + ".";
                    } else {
                        msg += ".";
                    }
                    callback.onFailure(msg);
                } else {
                    callback.onSuccess();
                }
            })
            .addOnFailureListener(e -> callback.onFailure("Conflict check failed: " + e.getMessage()));
    }

    private void performAcceptance(BorrowRequest request, Resource resource, SimpleCallback callback) {
        Timestamp now = Timestamp.now();
        Date dueDateValue = request.getEffectiveEndDate();
        Timestamp dueDate = new Timestamp(dueDateValue);

        WriteBatch batch = db.batch();

        // 1. Update the request status
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", BorrowRequest.STATUS_ACCEPTED);
        updates.put("acceptedDate", now);
        updates.put("dueDate", dueDate);
        batch.update(db.collection(COLLECTION).document(request.getRequestID()), updates);

        // 2. Update resource available quantity
        int newQty = Math.max(0, resource.getAvailableQuantity() - request.getQuantity());
        batch.update(db.collection("resources").document(resource.getResourceID()), 
            "availableQuantity", newQty,
            "available", newQty > 0);

        batch.commit()
            .addOnSuccessListener(unused -> {
                NotificationHelper.notifyRequestAccepted(request);
                autoRejectConflicts(request, resource);
                callback.onSuccess();
            })
            .addOnFailureListener(e -> callback.onFailure("Failed to accept: " + e.getMessage()));
    }

    private void autoRejectConflicts(BorrowRequest acceptedRequest, Resource resource) {
        Date start = acceptedRequest.getEffectiveStartDate();
        Date end = acceptedRequest.getEffectiveEndDate();

        db.collection(COLLECTION)
            .whereEqualTo("resourceID", acceptedRequest.getResourceID())
            .whereEqualTo("status", BorrowRequest.STATUS_PENDING)
            .get()
            .addOnSuccessListener(snapshots -> {
                WriteBatch batch = db.batch();
                boolean hasUpdates = false;
                
                for (QueryDocumentSnapshot doc : snapshots) {
                    BorrowRequest pending = doc.toObject(BorrowRequest.class);
                    if (pending.getRequestID() == null) pending.setRequestID(doc.getId());
                    
                    if (pending.getRequestID().equals(acceptedRequest.getRequestID())) continue;

                    // Reject if it overlaps and we've reached capacity
                    if (start.before(pending.getEffectiveEndDate()) && end.after(pending.getEffectiveStartDate())) {
                        // For simplicity, we reject any overlapping pending requests if the item is unique
                        if (resource.getTotalQuantity() <= 1) {
                            batch.update(doc.getReference(), "status", BorrowRequest.STATUS_REJECTED);
                            hasUpdates = true;
                        }
                    }
                }
                if (hasUpdates) batch.commit();
            });
    }

    public void requestExtension(BorrowRequest originalRequest, Date newEndDate, SimpleCallback callback) {
        BorrowRequest extensionReq = new BorrowRequest(
                originalRequest.getResourceID(),
                originalRequest.getResourceName(),
                originalRequest.getResourcePhoto(),
                originalRequest.getBorrowerID(),
                originalRequest.getBorrowerName(),
                originalRequest.getBorrowerDept(),
                originalRequest.getOwnerID(),
                originalRequest.getOwnerName(),
                originalRequest.getStartDate(),
                newEndDate,
                originalRequest.getQuantity()
        );
        extensionReq.setStatus(BorrowRequest.STATUS_EXTENSION_PENDING);
        extensionReq.setParentRequestID(originalRequest.getRequestID());

        resourceRepository.fetchResource(originalRequest.getResourceID(), new ResourceRepository.ResourceCallback() {
            @Override
            public void onSuccess(Resource resource) {
                checkForConflicts(extensionReq, resource, new SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        performSend(extensionReq, new RequestCallback() {
                            @Override public void onSuccess(BorrowRequest request) { callback.onSuccess(); }
                            @Override public void onFailure(String error) { callback.onFailure(error); }
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onFailure("Cannot extend: " + error);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    public void rejectRequest(BorrowRequest request, SimpleCallback callback) {
        db.collection(COLLECTION)
            .document(request.getRequestID())
            .update("status", BorrowRequest.STATUS_REJECTED)
            .addOnSuccessListener(unused -> {
                NotificationHelper.notifyRequestRejected(request);
                callback.onSuccess();
            })
            .addOnFailureListener(e -> callback.onFailure("Failed to reject: " + e.getMessage()));
    }

    public void markReturned(BorrowRequest request, SimpleCallback callback) {
        resourceRepository.fetchResource(request.getResourceID(), new ResourceRepository.ResourceCallback() {
            @Override
            public void onSuccess(Resource resource) {
                performReturn(request, resource, callback);
            }

            @Override
            public void onFailure(String error) {
                performReturn(request, null, callback);
            }
        });
    }

    private void performReturn(BorrowRequest request, Resource resource, SimpleCallback callback) {
        WriteBatch batch = db.batch();
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", BorrowRequest.STATUS_RETURNED);
        updates.put("returnedDate", Timestamp.now());
        updates.put("creditApplied", true);
        batch.update(db.collection(COLLECTION).document(request.getRequestID()), updates);

        if (resource != null) {
            int newQty = Math.min(resource.getTotalQuantity(), resource.getAvailableQuantity() + request.getQuantity());
            batch.update(db.collection("resources").document(resource.getResourceID()), 
                "availableQuantity", newQty,
                "available", true);
        }

        batch.commit()
            .addOnSuccessListener(unused -> {
                creditManager.applyCredit(
                    request.getBorrowerID(), request.getBorrowerName(),
                    request.getOwnerID(), request.getOwnerName(),
                    new CreditManager.CreditCallback() {
                        @Override public void onSuccess() {}
                        @Override public void onFailure(String e) {}
                    });
                NotificationHelper.notifyItemReturned(request);
                callback.onSuccess();
            })
            .addOnFailureListener(e -> callback.onFailure("Failed to mark returned: " + e.getMessage()));
    }

    public void fetchIncomingRequests(String ownerID, RequestListCallback callback) {
        db.collection(COLLECTION)
            .whereEqualTo("ownerID", ownerID)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<BorrowRequest> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    BorrowRequest br = doc.toObject(BorrowRequest.class);
                    if (br.getRequestID() == null) br.setRequestID(doc.getId());
                    list.add(br);
                }

                Collections.sort(list, (r1, r2) -> {
                    if (r1.isPriority() != r2.isPriority()) {
                        return r1.isPriority() ? -1 : 1;
                    }
                    if (r1.getRequestDate() == null || r2.getRequestDate() == null) return 0;
                    return r2.getRequestDate().compareTo(r1.getRequestDate());
                });

                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void fetchOutgoingRequests(String borrowerID, RequestListCallback callback) {
        db.collection(COLLECTION)
            .whereEqualTo("borrowerID", borrowerID)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<BorrowRequest> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    BorrowRequest br = doc.toObject(BorrowRequest.class);
                    if (br.getRequestID() == null) br.setRequestID(doc.getId());
                    list.add(br);
                }

                Collections.sort(list, (r1, r2) -> {
                    if (r1.getRequestDate() == null || r2.getRequestDate() == null) return 0;
                    return r2.getRequestDate().compareTo(r1.getRequestDate());
                });

                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void fetchActiveBorrows(String borrowerID, RequestListCallback callback) {
        db.collection(COLLECTION)
            .whereEqualTo("borrowerID", borrowerID)
            .whereEqualTo("status", BorrowRequest.STATUS_ACCEPTED)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<BorrowRequest> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    BorrowRequest br = doc.toObject(BorrowRequest.class);
                    if (br.getRequestID() == null) br.setRequestID(doc.getId());
                    list.add(br);
                }
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
