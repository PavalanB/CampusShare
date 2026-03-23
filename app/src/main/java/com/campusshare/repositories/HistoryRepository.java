package com.campusshare.repositories;

import com.campusshare.models.BorrowRequest;
import com.campusshare.models.LedgerEntry;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * HistoryRepository fetches all history data from Firestore.
 *
 * fetchBorrowedHistory()  — all requests where user is the borrower
 * fetchLentHistory()      — all requests where user is the owner
 * fetchCreditLedger()     — all ledger entries for this user
 * fetchHistoryStats()     — counts of borrows, lends, active, completed
 */
public class HistoryRepository {

    private final FirebaseFirestore db;

    public interface HistoryListCallback {
        void onSuccess(List<BorrowRequest> requests);
        void onFailure(String error);
    }

    public interface LedgerCallback {
        void onSuccess(List<LedgerEntry> entries);
        void onFailure(String error);
    }

    public interface StatsCallback {
        void onSuccess(HistoryStats stats);
        void onFailure(String error);
    }

    public HistoryRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ── Fetch items this user borrowed ────────────────────────────────────────

    public void fetchBorrowedHistory(String userID, HistoryListCallback callback) {
        db.collection("requests")
            .whereEqualTo("borrowerID", userID)
            .orderBy("requestDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<BorrowRequest> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots)
                    list.add(doc.toObject(BorrowRequest.class));
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ── Fetch items this user lent out ────────────────────────────────────────

    public void fetchLentHistory(String userID, HistoryListCallback callback) {
        db.collection("requests")
            .whereEqualTo("ownerID", userID)
            .orderBy("requestDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<BorrowRequest> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots)
                    list.add(doc.toObject(BorrowRequest.class));
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ── Fetch credit ledger entries ───────────────────────────────────────────

    /**
     * Reads from /users/{userID}/ledger subcollection.
     * Each document is one partner the user has transacted with.
     */
    public void fetchCreditLedger(String userID, LedgerCallback callback) {
        db.collection("users")
            .document(userID)
            .collection("ledger")
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<LedgerEntry> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots)
                    list.add(doc.toObject(LedgerEntry.class));
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ── Fetch stats ───────────────────────────────────────────────────────────

    /**
     * Fetches borrowed + lent history and computes counts in memory.
     * Two Firestore reads total — avoids needing aggregate queries.
     */
    public void fetchHistoryStats(String userID, StatsCallback callback) {
        // Fetch borrowed first
        db.collection("requests")
            .whereEqualTo("borrowerID", userID)
            .get()
            .addOnSuccessListener(borrowedSnaps -> {
                List<BorrowRequest> borrowed = new ArrayList<>();
                for (QueryDocumentSnapshot doc : borrowedSnaps)
                    borrowed.add(doc.toObject(BorrowRequest.class));

                // Then fetch lent
                db.collection("requests")
                    .whereEqualTo("ownerID", userID)
                    .get()
                    .addOnSuccessListener(lentSnaps -> {
                        List<BorrowRequest> lent = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : lentSnaps)
                            lent.add(doc.toObject(BorrowRequest.class));

                        callback.onSuccess(computeStats(borrowed, lent));
                    })
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ── Stats computation ─────────────────────────────────────────────────────

    private HistoryStats computeStats(List<BorrowRequest> borrowed,
                                      List<BorrowRequest> lent) {
        HistoryStats s = new HistoryStats();
        s.totalBorrowed = borrowed.size();
        s.totalLent     = lent.size();

        for (BorrowRequest r : borrowed) {
            if (BorrowRequest.STATUS_RETURNED.equals(r.getStatus()))  s.returnedCount++;
            if (BorrowRequest.STATUS_ACCEPTED.equals(r.getStatus()))  s.activeBorrows++;
            if (BorrowRequest.STATUS_PENDING.equals(r.getStatus()))   s.pendingCount++;
        }
        for (BorrowRequest r : lent) {
            if (BorrowRequest.STATUS_RETURNED.equals(r.getStatus()))  s.completedLends++;
            if (BorrowRequest.STATUS_ACCEPTED.equals(r.getStatus()))  s.activeLends++;
        }
        return s;
    }

    // ── Stats model ───────────────────────────────────────────────────────────

    public static class HistoryStats {
        public int totalBorrowed  = 0;
        public int totalLent      = 0;
        public int activeBorrows  = 0;
        public int activeLends    = 0;
        public int returnedCount  = 0;
        public int completedLends = 0;
        public int pendingCount   = 0;

        public int totalActive()    { return activeBorrows + activeLends; }
        public int totalCompleted() { return returnedCount + completedLends; }
    }
}
