package com.example.model;

import java.io.Serializable;

/**
 * Represents a Campus Canteen associated with a specific College.
 * Each College has 2 canteens.
 */
public class Canteen implements Serializable {
    private String canteenId;        // e.g. "canteen_1_1", "canteen_1_2"
    private String collegeIdString;  // e.g. "col_1", "col_2", "col_3"
    private int collegeId;           // 1, 2, 3
    private String name;             // e.g. "Byte Bites Canteen (Main Block)"
    private String location;         // e.g. "Ground Floor, Engineering Wing"
    private String operatingHours;   // e.g. "7:30 AM - 8:30 PM"
    private String status;           // "ACTIVE", "INACTIVE"

    public Canteen() {}

    public Canteen(String canteenId, String collegeIdString, int collegeId, String name, String location, String operatingHours) {
        this.canteenId = canteenId;
        this.collegeIdString = collegeIdString;
        this.collegeId = collegeId;
        this.name = name;
        this.location = location;
        this.operatingHours = operatingHours;
        this.status = "ACTIVE";
    }

    public Canteen(String canteenId, String collegeIdString, String name, String location, String operatingHours, String status) {
        this.canteenId = canteenId;
        this.collegeIdString = collegeIdString;
        try {
            if (collegeIdString != null && collegeIdString.startsWith("col_")) {
                this.collegeId = Integer.parseInt(collegeIdString.substring(4));
            } else if (collegeIdString != null) {
                this.collegeId = Integer.parseInt(collegeIdString);
            }
        } catch (Exception e) {
            this.collegeId = 1;
        }
        this.name = name;
        this.location = location;
        this.operatingHours = operatingHours;
        this.status = status;
    }

    public Canteen(String canteenId, String collegeIdString, String name, String location, String operatingHours) {
        this(canteenId, collegeIdString, name, location, operatingHours, "ACTIVE");
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCanteenId() {
        return canteenId;
    }

    public void setCanteenId(String canteenId) {
        this.canteenId = canteenId;
    }

    public String getCollegeIdString() {
        return College.toShortCode(collegeIdString, collegeId, null);
    }

    public void setCollegeIdString(String collegeIdString) {
        this.collegeIdString = College.toShortCode(collegeIdString, collegeId, null);
        this.collegeId = College.codeToNumericId(this.collegeIdString);
    }

    public int getCollegeId() {
        if (collegeId <= 0) {
            collegeId = College.codeToNumericId(collegeIdString);
        }
        return collegeId;
    }

    public void setCollegeId(int collegeId) {
        this.collegeId = collegeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOperatingHours() {
        return operatingHours;
    }

    public void setOperatingHours(String operatingHours) {
        this.operatingHours = operatingHours;
    }

    @Override
    public String toString() {
        return name;
    }
}
