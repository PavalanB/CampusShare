package com.campusshare.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import java.io.Serializable;
import java.util.Date;

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

    // java.util.Date is Serializable and handled by Firestore as a Timestamp
    private Date createdAt;

    // Location coordinates for Map integration
    private double latitude;
    private double longitude;

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
        this.createdAt = new Date(); // Current time
        this.latitude = 0.0;
        this.longitude = 0.0;
    }

    public Resource(String ownerID, String ownerName, String ownerDepartment,
                    String resourceName, String category, String description,
                    String condition, double latitude, double longitude) {
        this(ownerID, ownerName, ownerDepartment, resourceName, category, description, condition);
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = new Date();
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

    public Date getCreatedAt()         { return createdAt; }

    // Convert to Timestamp for Firebase compatibility if needed
    @Exclude
    public Timestamp getCreatedAtTimestamp() {
        return createdAt != null ? new Timestamp(createdAt) : null;
    }

    public double getLatitude()        { return latitude; }
    public double getLongitude()       { return longitude; }

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
    public void setCreatedAt(Date createdAt)               { this.createdAt = createdAt; }
    public void setLatitude(double latitude)               { this.latitude = latitude; }
    public void setLongitude(double longitude)             { this.longitude = longitude; }

    @Exclude
    public void setCreatedAtTimestamp(Timestamp createdAt) {
        this.createdAt = createdAt != null ? createdAt.toDate() : null;
    }
}
