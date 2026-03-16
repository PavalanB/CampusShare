package com.campusshare.models;

import com.google.firebase.Timestamp;
import java.io.Serializable;

/**
 * BorrowRequest represents one borrow transaction between two students.
 *
 * Status flow:
 *   PENDING → ACCEPTED → RETURNED
 *           ↘ REJECTED
 *
 * isPriority = true when the borrower previously lent to the owner
 * (the owner owes them a favour — credit engine sets this automatically).
 */
public class BorrowRequest implements Serializable {

    public static final String STATUS_PENDING  = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_RETURNED = "RETURNED";

    private String requestID;
    private String resourceID;
    private String resourceName;   // stored here so list screens don't need extra reads
    private String resourcePhoto;

    private String borrowerID;
    private String borrowerName;
    private String borrowerDept;

    private String ownerID;
    private String ownerName;

    private String status;
    private boolean isPriority;    // true = borrower is owed a favour by owner

    private Timestamp requestDate;
    private Timestamp acceptedDate;
    private Timestamp dueDate;
    private Timestamp returnedDate;

    private float borrowerRating;  // owner rates borrower after return (1–5)
    private float ownerRating;     // borrower rates owner after return (1–5)
    private boolean creditApplied; // prevents double-crediting

    // Required empty constructor for Firestore
    public BorrowRequest() {}

    public BorrowRequest(String resourceID, String resourceName, String resourcePhoto,
                         String borrowerID, String borrowerName, String borrowerDept,
                         String ownerID, String ownerName, boolean isPriority) {
        this.resourceID    = resourceID;
        this.resourceName  = resourceName;
        this.resourcePhoto = resourcePhoto;
        this.borrowerID    = borrowerID;
        this.borrowerName  = borrowerName;
        this.borrowerDept  = borrowerDept;
        this.ownerID       = ownerID;
        this.ownerName     = ownerName;
        this.isPriority    = isPriority;
        this.status        = STATUS_PENDING;
        this.requestDate   = Timestamp.now();
        this.creditApplied = false;
        this.borrowerRating = 0f;
        this.ownerRating    = 0f;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public String getRequestID()      { return requestID; }
    public String getResourceID()     { return resourceID; }
    public String getResourceName()   { return resourceName; }
    public String getResourcePhoto()  { return resourcePhoto; }
    public String getBorrowerID()     { return borrowerID; }
    public String getBorrowerName()   { return borrowerName; }
    public String getBorrowerDept()   { return borrowerDept; }
    public String getOwnerID()        { return ownerID; }
    public String getOwnerName()      { return ownerName; }
    public String getStatus()         { return status; }
    public boolean isPriority()       { return isPriority; }
    public Timestamp getRequestDate()  { return requestDate; }
    public Timestamp getAcceptedDate() { return acceptedDate; }
    public Timestamp getDueDate()      { return dueDate; }
    public Timestamp getReturnedDate() { return returnedDate; }
    public float getBorrowerRating()  { return borrowerRating; }
    public float getOwnerRating()     { return ownerRating; }
    public boolean isCreditApplied()  { return creditApplied; }

    // ── Setters ──────────────────────────────────────────────────────────────
    public void setRequestID(String v)       { this.requestID = v; }
    public void setResourceID(String v)      { this.resourceID = v; }
    public void setResourceName(String v)    { this.resourceName = v; }
    public void setResourcePhoto(String v)   { this.resourcePhoto = v; }
    public void setBorrowerID(String v)      { this.borrowerID = v; }
    public void setBorrowerName(String v)    { this.borrowerName = v; }
    public void setBorrowerDept(String v)    { this.borrowerDept = v; }
    public void setOwnerID(String v)         { this.ownerID = v; }
    public void setOwnerName(String v)       { this.ownerName = v; }
    public void setStatus(String v)          { this.status = v; }
    public void setPriority(boolean v)       { this.isPriority = v; }
    public void setRequestDate(Timestamp v)  { this.requestDate = v; }
    public void setAcceptedDate(Timestamp v) { this.acceptedDate = v; }
    public void setDueDate(Timestamp v)      { this.dueDate = v; }
    public void setReturnedDate(Timestamp v) { this.returnedDate = v; }
    public void setBorrowerRating(float v)   { this.borrowerRating = v; }
    public void setOwnerRating(float v)      { this.ownerRating = v; }
    public void setCreditApplied(boolean v)  { this.creditApplied = v; }

    // ── Convenience helpers ──────────────────────────────────────────────────
    public boolean isPending()  { return STATUS_PENDING.equals(status); }
    public boolean isAccepted() { return STATUS_ACCEPTED.equals(status); }
    public boolean isReturned() { return STATUS_RETURNED.equals(status); }
    public boolean isRejected() { return STATUS_REJECTED.equals(status); }
}
