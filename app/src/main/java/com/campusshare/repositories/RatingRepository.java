package com.campusshare.repositories;

import com.campusshare.models.BorrowRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

/**
 * RatingRepository saves a star rating on the BorrowRequest document
 * and recalculates the rated user's avgRating on their User document.
 *
 * avgRating is stored as a running average:
 *   newAvg = ((oldAvg * totalRatings) + newRating) / (totalRatings + 1)
 */
public class RatingRepository {

    private final FirebaseFirestore db;

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public RatingRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Submits a rating for either the borrower (isOwnerRating=true)
     * or the owner (isOwnerRating=false).
     *
     * Atomically:
     *  1. Writes the rating onto the request document
     *  2. Recalculates the rated user's avgRating
     */
    public void submitRating(BorrowRequest request, String raterID,
                             float stars, boolean isOwnerRating,
                             SimpleCallback callback) {

        // Which user is being rated?
        String ratedUserID = isOwnerRating
            ? request.getBorrowerID()  // owner rates borrower
            : request.getOwnerID();    // borrower rates owner

        // Field name on the request document
        String ratingField = isOwnerRating ? "borrowerRating" : "ownerRating";

        DocumentReference requestRef = db.collection("requests")
            .document(request.getRequestID());
        DocumentReference userRef    = db.collection("users")
            .document(ratedUserID);

        db.runTransaction((Transaction.Function<Void>) transaction -> {

            // Read current user stats
            com.google.firebase.firestore.DocumentSnapshot userSnap =
                transaction.get(userRef);

            double currentAvg     = 0.0;
            long   totalRatings   = 0;
            if (userSnap.exists()) {
                if (userSnap.getDouble("avgRating") != null)
                    currentAvg = userSnap.getDouble("avgRating");
                if (userSnap.getLong("totalRatings") != null)
                    totalRatings = userSnap.getLong("totalRatings");
            }

            // Recalculate running average
            long newTotal = totalRatings + 1;
            double newAvg = ((currentAvg * totalRatings) + stars) / newTotal;
            // Round to 1 decimal place
            newAvg = Math.round(newAvg * 10.0) / 10.0;

            // Write rating onto request
            transaction.update(requestRef, ratingField, stars);

            // Write updated avg onto user
            transaction.update(userRef, "avgRating",    newAvg);
            transaction.update(userRef, "totalRatings", newTotal);

            return null;

        }).addOnSuccessListener(v -> callback.onSuccess())
          .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
