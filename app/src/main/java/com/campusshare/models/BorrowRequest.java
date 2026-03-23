package com.campusshare.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import java.io.Serializable;
import java.util.Date;

/**
 * BorrowRequest represents one borrow transaction between two students.
 *
 * Note: Uses java.util.Date instead of Firebase Timestamp because Date is Serializable,
 * which prevents the app from crashing when passing a BorrowRequest via Intent.
 */
public class BorrowRequest implements Serializable {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_ONGOING = "ONGOING";
    public static final String STATUS_RETURNED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_OVERDUE = "OVERDUE";

    private String requestID;
    private String resourceID;
    private String resourceName;
    private String resourcePhoto;
    private String borrowerID;
    private String borrowerName;
    private String borrowerDept;
    private String ownerID;
    private String ownerName;
    private Date startDate;
    private Date endDate;
    private Date returnDate; // Actual date the item was returned
    private int quantity;
    private String status; // PENDING, APPROVED, REJECTED, ONGOING, COMPLETED, CANCELLED, OVERDUE
    private Date createdAt;
    private boolean priority;

    private String status;
    private boolean isPriority;

    private Date requestDate;
    private Date acceptedDate;
    private Date dueDate;
    private Date returnedDate;

    private float borrowerRating;
    private float ownerRating;
    private boolean creditApplied;

    public BorrowRequest() {}

    public BorrowRequest(String resourceID, String resourceName, String borrowerID, String borrowerName, String ownerID,
                         Date startDate, Date endDate, int quantity) {
        this.resourceID = resourceID;
        this.resourceName = resourceName;
        this.borrowerID = borrowerID;
        this.borrowerName = borrowerName;
        this.ownerID = ownerID;
        this.startDate = startDate;
        this.endDate = endDate;
        this.quantity = quantity;
        this.status = STATUS_PENDING;
        this.createdAt = new Date();
        this.priority = false;
    }

    public BorrowRequest(String resourceID, String resourceName, String resourcePhoto, String borrowerID, String borrowerName, String borrowerDept, String ownerID, String ownerName, boolean priority) {
        this.resourceID = resourceID;
        this.resourceName = resourceName;
        this.resourcePhoto = resourcePhoto;
        this.borrowerID = borrowerID;
        this.borrowerName = borrowerName;
        this.borrowerDept = borrowerDept;
        this.ownerID = ownerID;
        this.ownerName = ownerName;
        this.priority = priority;
        this.status = STATUS_PENDING;
        this.createdAt = new Date();
        this.quantity = 1;
        this.borrowerID    = borrowerID;
        this.borrowerName  = borrowerName;
        this.borrowerDept  = borrowerDept;
        this.ownerID       = ownerID;
        this.ownerName     = ownerName;
        this.isPriority    = isPriority;
        this.status        = STATUS_PENDING;
        this.requestDate   = new Date();
        this.creditApplied = false;
        this.borrowerRating = 0f;
        this.ownerRating    = 0f;
    }

    // Getters and Setters
    public String getRequestID() { return requestID; }
    public void setRequestID(String requestID) { this.requestID = requestID; }

    public String getResourceID() { return resourceID; }
    public void setResourceID(String resourceID) { this.resourceID = resourceID; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public String getResourcePhoto() { return resourcePhoto; }
    public void setResourcePhoto(String resourcePhoto) { this.resourcePhoto = resourcePhoto; }

    public String getBorrowerID() { return borrowerID; }
    public void setBorrowerID(String borrowerID) { this.borrowerID = borrowerID; }

    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }

    public String getBorrowerDept() { return borrowerDept; }
    public void setBorrowerDept(String borrowerDept) { this.borrowerDept = borrowerDept; }

    public String getOwnerID() { return ownerID; }
    public void setOwnerID(String ownerID) { this.ownerID = ownerID; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public Date getReturnDate() { return returnDate; }
    public void setReturnDate(Date returnDate) { this.returnDate = returnDate; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
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
    public Date getRequestDate()      { return requestDate; }
    public Date getAcceptedDate()     { return acceptedDate; }
    public Date getDueDate()          { return dueDate; }
    public Date getReturnedDate()     { return returnedDate; }
    public float getBorrowerRating()  { return borrowerRating; }
    public float getOwnerRating()     { return ownerRating; }
    public boolean isCreditApplied()  { return creditApplied; }

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
    public void setRequestDate(Date v)       { this.requestDate = v; }
    public void setAcceptedDate(Date v)      { this.acceptedDate = v; }
    public void setDueDate(Date v)           { this.dueDate = v; }
    public void setReturnedDate(Date v)      { this.returnedDate = v; }
    public void setBorrowerRating(float v)   { this.borrowerRating = v; }
    public void setOwnerRating(float v)      { this.ownerRating = v; }
    public void setCreditApplied(boolean v)  { this.creditApplied = v; }
    public boolean isPriority() { return priority; }
    public void setPriority(boolean priority) { this.priority = priority; }

    @Exclude
    public Timestamp getRequestDate() {
        return createdAt != null ? new Timestamp(createdAt) : null;
    }

    @Exclude
    public Timestamp getDueDate() {
        return endDate != null ? new Timestamp(endDate) : null;
    }

    @Exclude
    public boolean isPending() { return STATUS_PENDING.equals(status); }
    @Exclude
    public boolean isPending()  { return STATUS_PENDING.equals(status); }
    public boolean isAccepted() { return STATUS_ACCEPTED.equals(status); }
}
