package com.campusshare.repositories;

import com.campusshare.models.BorrowRequest;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;

public class BorrowRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference requestsRef = db.collection("borrow_requests");

    public interface BorrowHistoryCallback {
        void onSuccess(List<BorrowRequest> requests);
        void onFailure(String error);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public void fetchBorrowHistory(String userID, BorrowHistoryCallback callback) {
        requestsRef.whereEqualTo("borrowerID", userID)
            .orderBy("requestDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<BorrowRequest> requests = queryDocumentSnapshots.toObjects(BorrowRequest.class);
                callback.onSuccess(requests);
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void updateResourceRating(String requestID, float rating, SimpleCallback callback) {
        requestsRef.document(requestID)
            .update("resourceRating", rating)
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void updateBorrowerRatingInRequest(String requestID, float rating, SimpleCallback callback) {
        requestsRef.document(requestID)
            .update("borrowerRating", rating)
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
