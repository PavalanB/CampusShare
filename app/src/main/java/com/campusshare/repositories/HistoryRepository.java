package com.campusshare.repositories;

import com.campusshare.models.BorrowRequest;
import com.campusshare.models.LedgerEntry;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * HistoryRepository fetches all history data from Firestore.
 */
public class HistoryRepository {

    private final FirebaseFirestore db;
    private static final String COLLECTION = "borrow_requests";

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
        db.collection(COLLECTION)
            .whereEqualTo("borrowerID", userID)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<BorrowRequest> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    BorrowRequest br = doc.toObject(BorrowRequest.class);
                    if (br.getRequestID() == null) br.setRequestID(doc.getId());
                    list.add(br);
                }
                // Sort in memory to avoid index requirements
                Collections.sort(list, (r1, r2) -> {
                    if (r1.getRequestDate() == null || r2.getRequestDate() == null) return 0;
                    return r2.getRequestDate().compareTo(r1.getRequestDate());
                });
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ── Fetch items this user lent out ────────────────────────────────────────

    public void fetchLentHistory(String userID, HistoryListCallback callback) {
        db.collection(COLLECTION)
            .whereEqualTo("ownerID", userID)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<BorrowRequest> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    BorrowRequest br = doc.toObject(BorrowRequest.class);
                    if (br.getRequestID() == null) br.setRequestID(doc.getId());
                    list.add(br);
                }
                // Sort in memory to avoid index requirements
                Collections.sort(list, (r1, r2) -> {
                    if (r1.getRequestDate() == null || r2.getRequestDate() == null) return 0;
                    return r2.getRequestDate().compareTo(r1.getRequestDate());
                });
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ── Fetch credit ledger entries ───────────────────────────────────────────

    public void fetchCreditLedger(String userID, LedgerCallback callback) {
        db.collection("users")
            .document(userID)
            .collection("ledger")
            .get()
            .addOnSuccessListener(snapshots -> {
                List<LedgerEntry> list = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots)
                    list.add(doc.toObject(LedgerEntry.class));
                
                Collections.sort(list, (l1, l2) -> {
                    if (l1.getLastUpdated() == null || l2.getLastUpdated() == null) return 0;
                    return l2.getLastUpdated().compareTo(l1.getLastUpdated());
                });
                callback.onSuccess(list);
            })
            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ── Fetch stats ───────────────────────────────────────────────────────────

    public void fetchHistoryStats(String userID, StatsCallback callback) {
        db.collection(COLLECTION)
            .whereEqualTo("borrowerID", userID)
            .get()
            .addOnSuccessListener(borrowedSnaps -> {
                List<BorrowRequest> borrowed = new ArrayList<>();
                for (QueryDocumentSnapshot doc : borrowedSnaps)
                    borrowed.add(doc.toObject(BorrowRequest.class));

                db.collection(COLLECTION)
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

    private HistoryStats computeStats(List<BorrowRequest> borrowed,
                                      List<BorrowRequest> lent) {
        HistoryStats s = new HistoryStats();
        s.totalBorrowed = borrowed.size();
        s.totalLent     = lent.size();

        for (BorrowRequest r : borrowed) {
            if (BorrowRequest.STATUS_RETURNED.equals(r.getStatus()))  s.returnedCount++;
            if (BorrowRequest.STATUS_ACCEPTED.equals(r.getStatus()) || BorrowRequest.STATUS_ONGOING.equals(r.getStatus()))  s.activeBorrows++;
            if (BorrowRequest.STATUS_PENDING.equals(r.getStatus()))   s.pendingCount++;
        }
        for (BorrowRequest r : lent) {
            if (BorrowRequest.STATUS_RETURNED.equals(r.getStatus()))  s.completedLends++;
            if (BorrowRequest.STATUS_ACCEPTED.equals(r.getStatus()) || BorrowRequest.STATUS_ONGOING.equals(r.getStatus()))  s.activeLends++;
        }
        return s;
    }

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
