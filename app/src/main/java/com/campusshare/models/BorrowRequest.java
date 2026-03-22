package com.campusshare.models;

import java.io.Serializable;
import java.util.Date;

public class BorrowRequest implements Serializable {
    private String requestID;
    private String resourceID;
    private String resourceName;
    private String borrowerID;
    private String borrowerName;
    private String ownerID;
    private Date startDate;
    private Date endDate;
    private Date returnDate; // Actual date the item was returned
    private int quantity;
    private String status; // PENDING, APPROVED, REJECTED, ONGOING, COMPLETED, CANCELLED, OVERDUE
    private Date createdAt;

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
        this.status = "PENDING";
        this.createdAt = new Date();
    }

    // Getters and Setters
    public String getRequestID() { return requestID; }
    public void setRequestID(String requestID) { this.requestID = requestID; }

    public String getResourceID() { return resourceID; }
    public void setResourceID(String resourceID) { this.resourceID = resourceID; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public String getBorrowerID() { return borrowerID; }
    public void setBorrowerID(String borrowerID) { this.borrowerID = borrowerID; }

    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }

    public String getOwnerID() { return ownerID; }
    public void setOwnerID(String ownerID) { this.ownerID = ownerID; }

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
}
