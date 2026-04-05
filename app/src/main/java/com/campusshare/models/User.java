package com.campusshare.models;

import java.io.Serializable;

public class User implements Serializable {

    private String userID;
    private String name;
    private String email;
    private String phone;
    private String department;
    private String year;
    private String collegeID;
    private String profilePhoto;
    private double creditScore;
    private double avgRating;
    private int totalRatings;
    private int totalBorrows;
    private int totalLends;
    private String fcmToken;

    // Required empty constructor for Firestore deserialization
    public User() {}

    public User(String userID, String name, String email, String phone,
                String department, String year, String collegeID) {
        this.userID = userID;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.department = department;
        this.year = year;
        this.collegeID = collegeID;
        this.creditScore = 0.0;
        this.avgRating = 0.0;
        this.totalRatings = 0;
        this.totalBorrows = 0;
        this.totalLends = 0;
        this.profilePhoto = "";
        this.fcmToken = "";
    }

    // Getters
    public String getUserID() { return userID; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getDepartment() { return department; }
    public String getYear() { return year; }
    public String getCollegeID() { return collegeID; }
    public String getProfilePhoto() { return profilePhoto; }
    public double getCreditScore() { return creditScore; }
    public double getAvgRating() { return avgRating; }
    public int getTotalRatings() { return totalRatings; }
    public int getTotalBorrows() { return totalBorrows; }
    public int getTotalLends() { return totalLends; }
    public String getFcmToken() { return fcmToken; }

    // Setters
    public void setUserID(String userID) { this.userID = userID; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setDepartment(String department) { this.department = department; }
    public void setYear(String year) { this.year = year; }
    public void setCollegeID(String collegeID) { this.collegeID = collegeID; }
    public void setProfilePhoto(String profilePhoto) { this.profilePhoto = profilePhoto; }
    public void setCreditScore(double creditScore) { this.creditScore = creditScore; }
    public void setAvgRating(double avgRating) { this.avgRating = avgRating; }
    public void setTotalRatings(int totalRatings) { this.totalRatings = totalRatings; }
    public void setTotalBorrows(int totalBorrows) { this.totalBorrows = totalBorrows; }
    public void setTotalLends(int totalLends) { this.totalLends = totalLends; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
}
