package com.campusshare.repositories;

import android.content.Context;

import com.campusshare.models.BorrowRequest;
import com.campusshare.utils.CreditManager;
import com.campusshare.utils.NotificationHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BorrowRequestRepository — all Firestore operations for borrow requests.
 *
 * Sorting is handled in-memory to avoid requiring manual Firestore Indexes.
 */
public class BorrowRequestRepository {

    private final FirebaseFirestore db;
    private final ResourceRepository resourceRepository;
    private final CreditManager creditManager;
    private static final String COLLECTION = "requests";

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

    public BorrowRequestRepository() {
    public BorrowRequestRepository(Context context) {
        this.db                 = FirebaseFirestore.getInstance();
        this.resourceRepository = new ResourceRepository();
        this.resourceRepository = new ResourceRepository(context);
        this.creditManager      = new CreditManager();
    }

    // ── Send Request ──────────────────────────────────────────────────────────

    public void sendRequest(BorrowRequest request, RequestCallback callback) {
        db.collection(COLLECTION)
            .add(request)
            .addOnSuccessListener(docRef -> {
                request.setRequestID(docRef.getId());
                docRef.update("requestID", docRef.getId())
                    .addOnSuccessListener(v -> {
                        // Notify owner a new request arrived
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

    // ── Accept Request ────────────────────────────────────────────────────────

    public void acceptRequest(BorrowRequest request, SimpleCallback callback) {
        Timestamp now     = Timestamp.now();
        Timestamp dueDate = new Timestamp(now.getSeconds() + (7L * 24 * 60 * 60), 0);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status",       BorrowRequest.STATUS_ACCEPTED);
        updates.put("acceptedDate", now);
        updates.put("dueDate",      dueDate);

        db.collection(COLLECTION)
            .document(request.getRequestID())
            .update(updates)
            .addOnSuccessListener(unused -> {
                resourceRepository.setAvailability(request.getResourceID(), false,
                    new ResourceRepository.SimpleCallback() {
                        @Override public void onSuccess() {}
                        @Override public void onFailure(String e) {}
                    });
                // Notify borrower their request was accepted
                NotificationHelper.notifyRequestAccepted(request);
                callback.onSuccess();
            })
            .addOnFailureListener(e -> callback.onFailure("Failed to accept: " + e.getMessage()));
    }

    // ── Reject Request ────────────────────────────────────────────────────────

    public void rejectRequest(BorrowRequest request, SimpleCallback callback) {
        db.collection(COLLECTION)
            .document(request.getRequestID())
            .update("status", BorrowRequest.STATUS_REJECTED)
            .addOnSuccessListener(unused -> {
                // Notify borrower their request was rejected
                NotificationHelper.notifyRequestRejected(request);
                callback.onSuccess();
            })
            .addOnFailureListener(e -> callback.onFailure("Failed to reject: " + e.getMessage()));
    }

    // ── Mark Returned ─────────────────────────────────────────────────────────

    public void markReturned(BorrowRequest request, SimpleCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status",        BorrowRequest.STATUS_RETURNED);
        updates.put("returnedDate",  Timestamp.now());
        updates.put("creditApplied", true);

        db.collection(COLLECTION)
            .document(request.getRequestID())
            .update(updates)
            .addOnSuccessListener(unused -> {
                resourceRepository.setAvailability(request.getResourceID(), true,
                    new ResourceRepository.SimpleCallback() {
                        @Override public void onSuccess() {}
                        @Override public void onFailure(String e) {}
                    });
                creditManager.applyCredit(
                    request.getBorrowerID(), request.getBorrowerName(),
                    request.getOwnerID(),    request.getOwnerName(),
                    new CreditManager.CreditCallback() {
                        @Override public void onSuccess() {}
                        @Override public void onFailure(String e) {}
                    });
                // Notify borrower to rate the experience
                NotificationHelper.notifyItemReturned(request);
                callback.onSuccess();
            })
            .addOnFailureListener(e -> callback.onFailure("Failed to mark returned: " + e.getMessage()));
    }

    // ── Fetch Incoming (Owner Inbox) ──────────────────────────────────────────

    public void fetchIncomingRequests(String ownerID, RequestListCallback callback) {
        // Removed orderBy to avoid the "The query requires an index" error.
        db.collection(COLLECTION)
            .whereEqualTo("ownerID", ownerID)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<BorrowRequest> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots)
                    list.add(doc.toObject(BorrowRequest.class));

                // Sort in-memory: Priority first, then newest date
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

    // ── Fetch Outgoing (Borrower Sent) ────────────────────────────────────────

    public void fetchOutgoingRequests(String borrowerID, RequestListCallback callback) {
        // Removed orderBy to avoid the "The query requires an index" error.
        db.collection(COLLECTION)
            .whereEqualTo("borrowerID", borrowerID)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<BorrowRequest> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots)
                    list.add(doc.toObject(BorrowRequest.class));

                // Sort in-memory: Newest date first
                Collections.sort(list, (r1, r2) -> {
                    if (r1.getRequestDate() == null || r2.getRequestDate() == null) return 0;
                    return r2.getRequestDate().compareTo(r1.getRequestDate());
                });

                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ── Fetch Active Borrows ──────────────────────────────────────────────────

    public void fetchActiveBorrows(String borrowerID, RequestListCallback callback) {
        db.collection(COLLECTION)
            .whereEqualTo("borrowerID", borrowerID)
            .whereEqualTo("status", BorrowRequest.STATUS_ACCEPTED)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<BorrowRequest> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots)
                    list.add(doc.toObject(BorrowRequest.class));
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
