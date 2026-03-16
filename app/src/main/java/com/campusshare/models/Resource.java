package com.campusshare.models;

import com.google.firebase.Timestamp;
import java.io.Serializable;

public class Resource implements Serializable {

    private String resourceID;
    private String ownerID;
    private String ownerName;
    private String ownerDepartment;
    private String resourceName;
    private String category;
    private String description;
    private String condition;      // "New", "Good", "Fair", "Worn"
    private String photoUrl;       // Firebase Storage download URL
    private boolean available;
    
    // Note: Timestamp is not Serializable. We store it as a Date for Intent passing, 
    // but Firestore handles it fine as a Timestamp in the DB.
    private java.util.Date createdAtDate;

    // Required empty constructor for Firestore deserialization
    public Resource() {}

    public Resource(String ownerID, String ownerName, String ownerDepartment,
                    String resourceName, String category, String description,
                    String condition) {
        this.ownerID = ownerID;
        this.ownerName = ownerName;
        this.ownerDepartment = ownerDepartment;
        this.resourceName = resourceName;
        this.category = category;
        this.description = description;
        this.condition = condition;
        this.available = true;
        this.photoUrl = "";
        this.createdAtDate = new java.util.Date();
    }

    // Getters
    public String getResourceID()      { return resourceID; }
    public String getOwnerID()         { return ownerID; }
    public String getOwnerName()       { return ownerName; }
    public String getOwnerDepartment() { return ownerDepartment; }
    public String getResourceName()    { return resourceName; }
    public String getCategory()        { return category; }
    public String getDescription()     { return description; }
    public String getCondition()       { return condition; }
    public String getPhotoUrl()        { return photoUrl; }
    public boolean isAvailable()       { return available; }
    
    // Convert between Timestamp and Date for compatibility
    public Timestamp getCreatedAt() { 
        return createdAtDate != null ? new Timestamp(createdAtDate) : null; 
    }

    // Setters
    public void setResourceID(String resourceID)           { this.resourceID = resourceID; }
    public void setOwnerID(String ownerID)                 { this.ownerID = ownerID; }
    public void setOwnerName(String ownerName)             { this.ownerName = ownerName; }
    public void setOwnerDepartment(String ownerDepartment) { this.ownerDepartment = ownerDepartment; }
    public void setResourceName(String resourceName)       { this.resourceName = resourceName; }
    public void setCategory(String category)               { this.category = category; }
    public void setDescription(String description)         { this.description = description; }
    public void setCondition(String condition)             { this.condition = condition; }
    public void setPhotoUrl(String photoUrl)               { this.photoUrl = photoUrl; }
    public void setAvailable(boolean available)            { this.available = available; }
    
    public void setCreatedAt(Timestamp createdAt) { 
        this.createdAtDate = createdAt != null ? createdAt.toDate() : null; 
    }
}
