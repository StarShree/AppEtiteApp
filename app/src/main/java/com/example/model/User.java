package com.example.model;

import java.io.Serializable;

public class User implements Serializable {
    private int userId;
    private String idString; // String ID corresponding to Cloud SQL `id` (e.g. 'user_student_1', 'user_12345')
    private String name;
    private String email;
    private String passwordHash;
    private String role; // 'SUPER_ADMIN', 'STUDENT'/'CUSTOMER', 'COLLEGE_ADMIN', 'VENDOR'/'KITCHEN_STAFF'
    private Integer collegeId; // Nullable for system roles
    private String collegeIdString; // String representation of college_id for Cloud SQL
    private String assignedCanteenId;
    private java.math.BigDecimal walletBalance = new java.math.BigDecimal("350.00");
    private String studentRollNumber;
    private String phoneNumber;
    private String status = "ACTIVE";

    public User() {}

    public User(int userId, String name, String email, String passwordHash, String role, Integer collegeId) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.collegeId = collegeId;
        this.idString = "user_" + (userId > 0 ? userId : System.currentTimeMillis());
        if (collegeId != null) {
            this.collegeIdString = College.toShortCode(null, collegeId, null);
        }
    }

    public String getIdString() {
        if (idString != null && !idString.isEmpty()) {
            return idString;
        }
        return "user_" + (userId > 0 ? userId : System.currentTimeMillis());
    }

    public void setIdString(String idString) {
        this.idString = idString;
    }

    public String getCollegeIdString() {
        if (collegeIdString != null && !collegeIdString.isEmpty()) {
            return College.toShortCode(collegeIdString, collegeId, null);
        }
        if (collegeId != null) {
            return College.toShortCode(null, collegeId, null);
        }
        return null;
    }

    public void setCollegeIdString(String collegeIdString) {
        this.collegeIdString = collegeIdString != null ? College.toShortCode(collegeIdString, collegeId, null) : null;
        if (this.collegeIdString != null && this.collegeId == null) {
            this.collegeId = College.codeToNumericId(this.collegeIdString);
        }
    }

    public String getAssignedCanteenId() {
        return assignedCanteenId;
    }

    public void setAssignedCanteenId(String assignedCanteenId) {
        this.assignedCanteenId = assignedCanteenId;
    }

    public java.math.BigDecimal getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(java.math.BigDecimal walletBalance) {
        this.walletBalance = walletBalance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getCollegeId() {
        return collegeId;
    }

    public void setCollegeId(Integer collegeId) {
        this.collegeId = collegeId;
    }

    private String campusIdNumber; // Campus user ID / Roll number
    private String department;     // College Admin department
    private String designation;    // Canteen Staff designation
    private String permissionLevel = "SUPER_ACCESS_ALL"; // Super Admin permission level

    public String getCampusIdNumber() {
        return campusIdNumber != null ? campusIdNumber : studentRollNumber;
    }

    public void setCampusIdNumber(String campusIdNumber) {
        this.campusIdNumber = campusIdNumber;
        this.studentRollNumber = campusIdNumber;
    }

    public String getStudentRollNumber() {
        return getCampusIdNumber();
    }

    public void setStudentRollNumber(String studentRollNumber) {
        setCampusIdNumber(studentRollNumber);
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(String permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isSystemRole() {
        return "SUPER_ADMIN".equalsIgnoreCase(role)
                || "ROLE_ORDER".equalsIgnoreCase(role)
                || "ROLE_CUSTOMER_DETAIL".equalsIgnoreCase(role)
                || "ROLE_TRANSACTION".equalsIgnoreCase(role);
    }
}
