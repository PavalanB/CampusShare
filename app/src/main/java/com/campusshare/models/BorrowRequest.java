package com.campusshare.models;

import java.io.Serializable;
import java.util.Date;

public class BorrowRequest implements Serializable {
    private String requestID;
    private String resourceID;
    private String resourceName;
    private String resourcePhotoUrl;
    private String ownerID;
    private String ownerName;
    private String borrowerID;
    private String borrowerName;
    private String status; // "PENDING", "APPROVED", "REJECTED", "RETURNED"
    private Date requestDate;
    private Date returnDate;

    public BorrowRequest() {}

    public BorrowRequest(String resourceID, String resourceName, String resourcePhotoUrl, 
                         String ownerID, String ownerName, String borrowerID, String borrowerName) {
        this.resourceID = resourceID;
        this.resourceName = resourceName;
        this.resourcePhotoUrl = resourcePhotoUrl;
        this.ownerID = ownerID;
        this.ownerName = ownerName;
        this.borrowerID = borrowerID;
        this.borrowerName = borrowerName;
        this.status = "PENDING";
        this.requestDate = new Date();
    }

    // Getters and Setters
    public String getRequestID() { return requestID; }
    public void setRequestID(String requestID) { this.requestID = requestID; }
    public String getResourceID() { return resourceID; }
    public void setResourceID(String resourceID) { this.resourceID = resourceID; }
    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }
    public String getResourcePhotoUrl() { return resourcePhotoUrl; }
    public void setResourcePhotoUrl(String resourcePhotoUrl) { this.resourcePhotoUrl = resourcePhotoUrl; }
    public String getOwnerID() { return ownerID; }
    public void setOwnerID(String ownerID) { this.ownerID = ownerID; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getBorrowerID() { return borrowerID; }
    public void setBorrowerID(String borrowerID) { this.borrowerID = borrowerID; }
    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getRequestDate() { return requestDate; }
    public void setRequestDate(Date requestDate) { this.requestDate = requestDate; }
    public Date getReturnDate() { return returnDate; }
    public void setReturnDate(Date returnDate) { this.returnDate = returnDate; }
}
