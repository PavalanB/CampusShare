package com.campusshare.utils;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

/**
 * CreditManager is the brain of the credit system.
 *
 * It does two things:
 *  1. checkPriority()  — before a request is sent, checks if the borrower
 *                        is owed a favour by the owner (so the request gets
 *                        flagged as isPriority = true)
 *  2. applyCredit()    — after an item is returned, updates both students'
 *                        ledger entries atomically using a Firestore transaction
 *
 * Ledger structure:
 *   /users/{userID}/ledger/{partnerID}  →  { balance, partnerName, lastUpdated }
 *
 * Balance rules:
 *   Borrower's ledger entry for owner:  balance -= 1  (I owe them)
 *   Owner's ledger entry for borrower:  balance += 1  (they owe me)
 */
public class CreditManager {

    private final FirebaseFirestore db;

    public interface PriorityCallback {
        void onResult(boolean isPriority);
    }

    public interface CreditCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public CreditManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ── 1. Check Priority ─────────────────────────────────────────────────────

    /**
     * Checks whether borrowerID has previously lent to ownerID
     * (i.e. whether ownerID owes borrowerID a favour).
     *
     * Reads ownerID's ledger entry for borrowerID.
     * If balance < 0 → owner owes borrower → isPriority = true.
     *
     * Called from ResourceDetailActivity before sending a borrow request.
     */
    public void checkPriority(String borrowerID, String ownerID, PriorityCallback callback) {
        // We check the OWNER's ledger at the BORROWER's key
        // If owner's balance for borrower < 0, the owner owes the borrower
        db.collection("users")
            .document(ownerID)
            .collection("ledger")
            .document(borrowerID)
            .get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    double balance = snapshot.getDouble("balance") != null
                        ? snapshot.getDouble("balance") : 0.0;
                    // balance < 0 means ownerID owes borrowerID
                    callback.onResult(balance < 0);
                } else {
                    // No ledger entry means no history → not priority
                    callback.onResult(false);
                }
            })
            .addOnFailureListener(e -> callback.onResult(false)); // fail safe: not priority
    }

    // ── 2. Apply Credit After Return ──────────────────────────────────────────

    /**
     * Called when a borrow request is marked RETURNED.
     * Updates both students' ledger entries in a single atomic Firestore transaction.
     *
     * Borrower's credit goes down by 1 (they borrowed)
     * Owner's credit goes up by 1 (they lent)
     *
     * Uses Firestore runTransaction so both writes succeed or fail together —
     * no partial updates that could corrupt the ledger.
     */
    public void applyCredit(String borrowerID, String borrowerName,
                            String ownerID,    String ownerName,
                            CreditCallback callback) {

        // References to both ledger entries
        DocumentReference borrowerLedgerRef = db.collection("users")
            .document(borrowerID).collection("ledger").document(ownerID);

        DocumentReference ownerLedgerRef = db.collection("users")
            .document(ownerID).collection("ledger").document(borrowerID);

        // References to both users' top-level creditScore field
        DocumentReference borrowerUserRef = db.collection("users").document(borrowerID);
        DocumentReference ownerUserRef    = db.collection("users").document(ownerID);

        db.runTransaction((Transaction.Function<Void>) transaction -> {

            // ── Read current values ───────────────────────────────────────────
            double borrowerLedgerBalance = 0.0;
            double ownerLedgerBalance    = 0.0;
            double borrowerCreditScore   = 0.0;
            double ownerCreditScore      = 0.0;

            com.google.firebase.firestore.DocumentSnapshot bLedger =
                transaction.get(borrowerLedgerRef);
            com.google.firebase.firestore.DocumentSnapshot oLedger =
                transaction.get(ownerLedgerRef);
            com.google.firebase.firestore.DocumentSnapshot bUser =
                transaction.get(borrowerUserRef);
            com.google.firebase.firestore.DocumentSnapshot oUser =
                transaction.get(ownerUserRef);

            if (bLedger.exists() && bLedger.getDouble("balance") != null)
                borrowerLedgerBalance = bLedger.getDouble("balance");
            if (oLedger.exists() && oLedger.getDouble("balance") != null)
                ownerLedgerBalance = oLedger.getDouble("balance");
            if (bUser.exists() && bUser.getDouble("creditScore") != null)
                borrowerCreditScore = bUser.getDouble("creditScore");
            if (oUser.exists() && oUser.getDouble("creditScore") != null)
                ownerCreditScore = oUser.getDouble("creditScore");

            // ── Write updated values ──────────────────────────────────────────

            // Borrower's ledger for owner: balance goes down (they owe more)
            java.util.Map<String, Object> bLedgerData = new java.util.HashMap<>();
            bLedgerData.put("partnerID",   ownerID);
            bLedgerData.put("partnerName", ownerName);
            bLedgerData.put("balance",     borrowerLedgerBalance - 1);
            bLedgerData.put("lastUpdated", com.google.firebase.Timestamp.now());
            transaction.set(borrowerLedgerRef, bLedgerData);

            // Owner's ledger for borrower: balance goes up (they are owed more)
            java.util.Map<String, Object> oLedgerData = new java.util.HashMap<>();
            oLedgerData.put("partnerID",   borrowerID);
            oLedgerData.put("partnerName", borrowerName);
            oLedgerData.put("balance",     ownerLedgerBalance + 1);
            oLedgerData.put("lastUpdated", com.google.firebase.Timestamp.now());
            transaction.set(ownerLedgerRef, oLedgerData);

            // Update top-level creditScore on both user documents
            transaction.update(borrowerUserRef, "creditScore", borrowerCreditScore - 1);
            transaction.update(ownerUserRef,    "creditScore", ownerCreditScore + 1);

            return null;

        }).addOnSuccessListener(unused -> callback.onSuccess())
          .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
