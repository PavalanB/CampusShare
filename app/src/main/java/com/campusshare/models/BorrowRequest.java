package com.campusshare.models;

import java.io.Serializable;
import java.util.Date;

/**
 * BorrowRequest represents one borrow transaction between two students.
 *
 * Note: Uses java.util.Date instead of Firebase Timestamp because Date is Serializable,
 * which prevents the app from crashing when passing a BorrowRequest via Intent.
 */
public class BorrowRequest implements Serializable {

    public static final String STATUS_PENDING  = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_RETURNED = "RETURNED";

    private String requestID;
    private String resourceID;
    private String resourceName;
    private String resourcePhoto;

    private String borrowerID;
    private String borrowerName;
    private String borrowerDept;

    private String ownerID;
    private String ownerName;

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
        this.requestDate   = new Date();
        this.creditApplied = false;
        this.borrowerRating = 0f;
        this.ownerRating    = 0f;
    }

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

    public boolean isPending()  { return STATUS_PENDING.equals(status); }
    public boolean isAccepted() { return STATUS_ACCEPTED.equals(status); }
    public boolean isReturned() { return STATUS_RETURNED.equals(status); }
    public boolean isRejected() { return STATUS_REJECTED.equals(status); }
}
