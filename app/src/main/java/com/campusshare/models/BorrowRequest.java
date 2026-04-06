package com.campusshare.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.Date;

/**
 * BorrowRequest represents one borrow transaction between two students.
 */
public class BorrowRequest implements Serializable {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_ONGOING = "ONGOING";
    public static final String STATUS_RETURNED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_OVERDUE = "OVERDUE";
    public static final String STATUS_EXTENSION_PENDING = "EXTENSION_PENDING";

    private String requestID;
    private String resourceID;
    private String resourceName;
    
    private String resourcePhoto; // Canonical field for the photo URL
    
    private String borrowerID;
    private String borrowerName;
    private String borrowerDept;
    private String ownerID;
    private String ownerName;
    private Date startDate;
    private Date endDate;
    private Date returnedDate; 
    private int quantity;
    private String status; 
    private boolean priority;
    private Date requestDate;
    private Date acceptedDate;
    private Date dueDate;
    private float borrowerRating;
    private float ownerRating;
    private float resourceRating; // Rating given by borrower to the product
    private boolean creditApplied;
    private String parentRequestID; // To link extension to original request

    public BorrowRequest() {}

    // Constructor used by BorrowRequestActivity (9 arguments)
    public BorrowRequest(String resourceID, String resourceName, String resourcePhoto,
                         String borrowerID, String borrowerName, String borrowerDept,
                         String ownerID, String ownerName, boolean priority) {
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
        this.startDate = new Date(); // Default to now
        this.endDate = new Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000); // Default to 1 week
        this.quantity = 1;
        this.borrowerRating = 0f;
        this.ownerRating = 0f;
        this.resourceRating = 0f;
        this.creditApplied = false;
    }

    // Constructor used by ResourceDetailActivity (11 arguments)
    public BorrowRequest(String resourceID, String resourceName, String resourcePhoto,
                         String borrowerID, String borrowerName, String borrowerDept,
                         String ownerID, String ownerName, Date startDate, Date endDate, int quantity) {
        this.resourceID = resourceID;
        this.resourceName = resourceName;
        this.resourcePhoto = resourcePhoto;
        this.borrowerID = borrowerID;
        this.borrowerName = borrowerName;
        this.borrowerDept = borrowerDept;
        this.ownerID = ownerID;
        this.ownerName = ownerName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.quantity = quantity;
        this.status = STATUS_PENDING;
        this.requestDate = new Date();
        this.borrowerRating = 0f;
        this.ownerRating = 0f;
        this.resourceRating = 0f;
        this.creditApplied = false;
    }

    @PropertyName("requestID")
    public String getRequestID() { return requestID; }
    @PropertyName("requestID")
    public void setRequestID(String requestID) { this.requestID = requestID; }

    @PropertyName("resourceID")
    public String getResourceID() { return resourceID; }
    @PropertyName("resourceID")
    public void setResourceID(String resourceID) { this.resourceID = resourceID; }

    @PropertyName("resourceName")
    public String getResourceName() { return resourceName; }
    @PropertyName("resourceName")
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    @PropertyName("resourcePhoto")
    public String getResourcePhoto() { return resourcePhoto; }
    @PropertyName("resourcePhoto")
    public void setResourcePhoto(String resourcePhoto) { 
        if (resourcePhoto != null && !resourcePhoto.isEmpty()) this.resourcePhoto = resourcePhoto; 
    }

    // Support for multiple field names in Firestore (photoUrl, imageUrl)
    @PropertyName("photoUrl")
    public String getPhotoUrl() { return resourcePhoto; }
    @PropertyName("photoUrl")
    public void setPhotoUrl(String photoUrl) { 
        if (photoUrl != null && !photoUrl.isEmpty()) this.resourcePhoto = photoUrl; 
    }

    @PropertyName("imageUrl")
    public String getImageUrl() { return resourcePhoto; }
    @PropertyName("imageUrl")
    public void setImageUrl(String imageUrl) { 
        if (imageUrl != null && !imageUrl.isEmpty()) this.resourcePhoto = imageUrl; 
    }

    @Exclude
    public String getEffectivePhotoUrl() {
        if (resourcePhoto != null && !resourcePhoto.trim().isEmpty() && !resourcePhoto.equalsIgnoreCase("null")) {
            return resourcePhoto;
        }
        return "";
    }

    @PropertyName("borrowerID")
    public String getBorrowerID() { return borrowerID; }
    @PropertyName("borrowerID")
    public void setBorrowerID(String borrowerID) { this.borrowerID = borrowerID; }

    @PropertyName("borrowerName")
    public String getBorrowerName() { return borrowerName; }
    @PropertyName("borrowerName")
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }

    @PropertyName("borrowerDept")
    public String getBorrowerDept() { return borrowerDept; }
    @PropertyName("borrowerDept")
    public void setBorrowerDept(String borrowerDept) { this.borrowerDept = borrowerDept; }

    @PropertyName("ownerID")
    public String getOwnerID() { return ownerID; }
    @PropertyName("ownerID")
    public void setOwnerID(String ownerID) { this.ownerID = ownerID; }

    @PropertyName("ownerName")
    public String getOwnerName() { return ownerName; }
    @PropertyName("ownerName")
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    @PropertyName("startDate")
    public Date getStartDate() { return startDate; }
    @PropertyName("startDate")
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    @PropertyName("endDate")
    public Date getEndDate() { return endDate; }
    @PropertyName("endDate")
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    @PropertyName("returnedDate")
    public Date getReturnedDate() { return returnedDate; }
    @PropertyName("returnedDate")
    public void setReturnedDate(Date returnedDate) { this.returnedDate = returnedDate; }

    @PropertyName("quantity")
    public int getQuantity() { return quantity; }
    @PropertyName("quantity")
    public void setQuantity(int quantity) { this.quantity = quantity; }

    @PropertyName("status")
    public String getStatus() { return status; }
    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }

    @PropertyName("priority")
    public boolean isPriority() { return priority; }
    @PropertyName("priority")
    public void setPriority(boolean priority) { this.priority = priority; }

    @PropertyName("requestDate")
    public Date getRequestDate() { return requestDate; }
    @PropertyName("requestDate")
    public void setRequestDate(Date requestDate) { this.requestDate = requestDate; }

    @PropertyName("acceptedDate")
    public Date getAcceptedDate() { return acceptedDate; }
    @PropertyName("acceptedDate")
    public void setAcceptedDate(Date acceptedDate) { this.acceptedDate = acceptedDate; }

    @PropertyName("dueDate")
    public Date getDueDate() { return dueDate; }
    @PropertyName("dueDate")
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }

    @PropertyName("borrowerRating")
    public float getBorrowerRating() { return borrowerRating; }
    @PropertyName("borrowerRating")
    public void setBorrowerRating(float borrowerRating) { this.borrowerRating = borrowerRating; }

    @PropertyName("ownerRating")
    public float getOwnerRating() { return ownerRating; }
    @PropertyName("ownerRating")
    public void setOwnerRating(float ownerRating) { this.ownerRating = ownerRating; }

    @PropertyName("resourceRating")
    public float getResourceRating() { return resourceRating; }
    @PropertyName("resourceRating")
    public void setResourceRating(float resourceRating) { this.resourceRating = resourceRating; }

    @PropertyName("creditApplied")
    public boolean isCreditApplied() { return creditApplied; }
    @PropertyName("creditApplied")
    public void setCreditApplied(boolean creditApplied) { this.creditApplied = creditApplied; }

    @PropertyName("parentRequestID")
    public String getParentRequestID() { return parentRequestID; }
    @PropertyName("parentRequestID")
    public void setParentRequestID(String parentRequestID) { this.parentRequestID = parentRequestID; }

    @Exclude
    public boolean isPending() { return STATUS_PENDING.equals(status) || STATUS_EXTENSION_PENDING.equals(status); }
    @Exclude
    public boolean isAccepted() { return STATUS_ACCEPTED.equals(status); }
    @Exclude
    public boolean isReturned() { return STATUS_RETURNED.equals(status); }

    @Exclude
    public Date getEffectiveStartDate() {
        if (startDate != null) return startDate;
        if (requestDate != null) return requestDate;
        return new Date();
    }

    @Exclude
    public Date getEffectiveEndDate() {
        if (endDate != null) return endDate;
        if (dueDate != null) return dueDate;
        // Default to 7 days after start
        return new Date(getEffectiveStartDate().getTime() + 7L * 24 * 60 * 60 * 1000);
    }
}
