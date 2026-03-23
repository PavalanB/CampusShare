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
    private Date returnedDate; // Actual date the item was returned
    private int quantity;
    private String status; // PENDING, APPROVED, REJECTED, ONGOING, COMPLETED, CANCELLED, OVERDUE
    private boolean priority;
    private Date requestDate;
    private Date acceptedDate;
    private Date dueDate;
    private float borrowerRating;
    private float ownerRating;
    private boolean creditApplied;

    public BorrowRequest() {}

    // Constructor used in ResourceDetailActivity
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
        this.requestDate = new Date();
        this.priority = false;
    }

    // Constructor used in other places (e.g. manual request creation)
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
        this.requestDate = new Date();
        this.quantity = 1;
        this.creditApplied = false;
        this.borrowerRating = 0f;
        this.ownerRating = 0f;
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

    public Date getReturnedDate() { return returnedDate; }
    public void setReturnedDate(Date returnedDate) { this.returnedDate = returnedDate; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isPriority() { return priority; }
    public void setPriority(boolean priority) { this.priority = priority; }

    public Date getRequestDate() { return requestDate; }
    public void setRequestDate(Date requestDate) { this.requestDate = requestDate; }

    public Date getAcceptedDate() { return acceptedDate; }
    public void setAcceptedDate(Date acceptedDate) { this.acceptedDate = acceptedDate; }

    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }

    public float getBorrowerRating() { return borrowerRating; }
    public void setBorrowerRating(float borrowerRating) { this.borrowerRating = borrowerRating; }

    public float getOwnerRating() { return ownerRating; }
    public void setOwnerRating(float ownerRating) { this.ownerRating = ownerRating; }

    public boolean isCreditApplied() { return creditApplied; }
    public void setCreditApplied(boolean creditApplied) { this.creditApplied = creditApplied; }

    // Compatibility Getters/Setters for older code
    @Exclude
    public Date getCreatedAt() { return requestDate; }
    @Exclude
    public void setCreatedAt(Date d) { this.requestDate = d; }
    @Exclude
    public Date getReturnDate() { return returnedDate; }
    @Exclude
    public void setReturnDate(Date d) { this.returnedDate = d; }

    @Exclude
    public Timestamp getFirebaseRequestDate() {
        return requestDate != null ? new Timestamp(requestDate) : null;
    }

    @Exclude
    public Timestamp getFirebaseDueDate() {
        return dueDate != null ? new Timestamp(dueDate) : null;
    }

    @Exclude
    public boolean isPending() { return STATUS_PENDING.equals(status); }
    @Exclude
    public boolean isAccepted() { return STATUS_ACCEPTED.equals(status); }
    @Exclude
    public boolean isReturned() { return STATUS_RETURNED.equals(status); }
}
