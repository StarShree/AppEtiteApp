package com.example.db;

import android.util.Log;

import com.example.model.College;
import com.example.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User authentication, queries, and role-based registration.
 * Fully compatible with the Google Cloud SQL AppEtiteDB schema:
 * Table: users (
 *   id VARCHAR(64) PRIMARY KEY,
 *   name VARCHAR(120),
 *   email VARCHAR(120) UNIQUE,
 *   password_hash VARCHAR(255),
 *   role ENUM('STUDENT','VENDOR','COLLEGE_ADMIN','SUPER_ADMIN'),
 *   college_id VARCHAR(64),
 *   assigned_canteen_id VARCHAR(64),
 *   wallet_balance DECIMAL(10,2),
 *   student_roll_number VARCHAR(64),
 *   phone_number VARCHAR(32),
 *   status ENUM('ACTIVE','SUSPENDED','PENDING_APPROVAL'),
 *   created_at TIMESTAMP,
 *   updated_at TIMESTAMP
 * )
 */
public class UserDao {
    private static final String TAG = "UserDao";
    private volatile String lastErrorMessage = null;

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    /**
     * Maps app role strings to Cloud SQL schema enum values:
     * 'CUSTOMER' -> 'STUDENT'
     * 'KITCHEN_STAFF' -> 'VENDOR'
     * 'SUPER_ADMIN' -> 'SUPER_ADMIN'
     * 'COLLEGE_ADMIN' -> 'COLLEGE_ADMIN'
     */
    public static String toDbRole(String appRole) {
        if (appRole == null) return "STUDENT";
        String r = appRole.trim().toUpperCase();
        if ("CUSTOMER".equals(r)) return "STUDENT";
        if ("KITCHEN_STAFF".equals(r)) return "VENDOR";
        if ("SUPER_ADMIN".equals(r)) return "SUPER_ADMIN";
        if ("COLLEGE_ADMIN".equals(r)) return "COLLEGE_ADMIN";
        if ("STUDENT".equals(r)) return "STUDENT";
        if ("VENDOR".equals(r)) return "VENDOR";
        return "STUDENT";
    }

    /**
     * Maps database enum values to app-level role strings:
     * 'STUDENT' -> 'CUSTOMER'
     * 'VENDOR' -> 'KITCHEN_STAFF'
     */
    public static String toAppRole(String dbRole) {
        if (dbRole == null) return "CUSTOMER";
        String r = dbRole.trim().toUpperCase();
        if ("STUDENT".equals(r)) return "CUSTOMER";
        if ("VENDOR".equals(r)) return "KITCHEN_STAFF";
        return r;
    }

    /**
     * Authenticates a user against Google Cloud SQL MySQL using PreparedStatement.
     * Supports:
     * 1. Unified view: `vw_all_system_users`
     * 2. Separate role tables: `campus_users`, `canteen_staff`, `college_admins`, `super_admins`
     * 3. Legacy `users` table for backward compatibility
     * 4. LocalDataStore fallback if offline or unreachable
     */
    public User authenticateUser(String email, String password) {
        String cleanEmail = email != null ? email.trim() : "";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            if (conn != null && !conn.isClosed()) {
                // 1. Try querying unified view vw_all_system_users
                try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM vw_all_system_users WHERE email = ? LIMIT 1")) {
                    stmt.setString(1, cleanEmail);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String storedPassword = rs.getString("password_hash");
                            if (storedPassword != null && storedPassword.equals(password)) {
                                User user = parseUserFromResultSet(rs, "CUSTOMER");
                                if (user != null) {
                                    Log.d(TAG, "User authenticated via vw_all_system_users: " + user.getEmail() + " | Role: " + user.getRole());
                                    return user;
                                }
                            }
                        }
                    }
                } catch (Exception exView) {
                    Log.d(TAG, "vw_all_system_users not queried: " + exView.getMessage() + ". Checking separate role tables...");
                }

                // 2. Check separate role tables individually
                // A. Campus Users
                try (PreparedStatement stmt = conn.prepareStatement("SELECT *, 'CUSTOMER' AS role FROM campus_users WHERE email = ? LIMIT 1")) {
                    stmt.setString(1, cleanEmail);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String storedPassword = rs.getString("password_hash");
                            if (storedPassword != null && storedPassword.equals(password)) {
                                User user = parseUserFromResultSet(rs, "CUSTOMER");
                                if (user != null) {
                                    Log.d(TAG, "User authenticated in campus_users: " + user.getEmail());
                                    return user;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

                // B. Canteen Staff
                try (PreparedStatement stmt = conn.prepareStatement("SELECT *, 'KITCHEN_STAFF' AS role FROM canteen_staff WHERE email = ? LIMIT 1")) {
                    stmt.setString(1, cleanEmail);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String storedPassword = rs.getString("password_hash");
                            if (storedPassword != null && storedPassword.equals(password)) {
                                User user = parseUserFromResultSet(rs, "KITCHEN_STAFF");
                                if (user != null) {
                                    Log.d(TAG, "User authenticated in canteen_staff: " + user.getEmail());
                                    return user;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

                // C. College Admins
                try (PreparedStatement stmt = conn.prepareStatement("SELECT *, 'COLLEGE_ADMIN' AS role FROM college_admins WHERE email = ? LIMIT 1")) {
                    stmt.setString(1, cleanEmail);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String storedPassword = rs.getString("password_hash");
                            if (storedPassword != null && storedPassword.equals(password)) {
                                User user = parseUserFromResultSet(rs, "COLLEGE_ADMIN");
                                if (user != null) {
                                    Log.d(TAG, "User authenticated in college_admins: " + user.getEmail());
                                    return user;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

                // D. Super Admins
                try (PreparedStatement stmt = conn.prepareStatement("SELECT *, 'SUPER_ADMIN' AS role FROM super_admins WHERE email = ? LIMIT 1")) {
                    stmt.setString(1, cleanEmail);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String storedPassword = rs.getString("password_hash");
                            if (storedPassword != null && storedPassword.equals(password)) {
                                User user = parseUserFromResultSet(rs, "SUPER_ADMIN");
                                if (user != null) {
                                    Log.d(TAG, "User authenticated in super_admins: " + user.getEmail());
                                    return user;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

                // 3. Fallback: Legacy users table
                try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE email = ? LIMIT 1")) {
                    stmt.setString(1, cleanEmail);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String storedPassword = rs.getString("password_hash");
                            if (storedPassword != null && storedPassword.equals(password)) {
                                User user = parseUserFromResultSet(rs, "CUSTOMER");
                                if (user != null) {
                                    Log.d(TAG, "User authenticated in legacy users table: " + user.getEmail());
                                    return user;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL authentication query failed: " + e.getMessage() + ". Checking local cache.");
        }

        return LocalDataStore.getInstance().authenticateUser(email, password);
    }

    /**
     * Parses a User model from a ResultSet whether it originates from
     * `campus_users`, `canteen_staff`, `college_admins`, `super_admins`,
     * `vw_all_system_users`, or the legacy `users` table.
     */
    private User parseUserFromResultSet(ResultSet rs, String fallbackRole) {
        try {
            User user = new User();
            String idVal = null;
            try { idVal = rs.getString("id"); } catch (Exception ignored) {}
            user.setIdString(idVal);

            int userIdInt = 0;
            try { userIdInt = rs.getInt("user_id"); } catch (Exception ignored) {}
            if (userIdInt == 0) {
                try { userIdInt = rs.getInt("staff_id"); } catch (Exception ignored) {}
            }
            if (userIdInt == 0) {
                try { userIdInt = rs.getInt("admin_id"); } catch (Exception ignored) {}
            }
            if (userIdInt == 0) {
                try { userIdInt = rs.getInt("super_admin_id"); } catch (Exception ignored) {}
            }
            if (userIdInt == 0 && idVal != null) {
                try {
                    if (idVal.contains("_")) {
                        userIdInt = Integer.parseInt(idVal.substring(idVal.lastIndexOf('_') + 1));
                    } else {
                        userIdInt = Integer.parseInt(idVal);
                    }
                } catch (Exception e) {
                    userIdInt = Math.abs(idVal.hashCode() % 100000);
                }
            }
            user.setUserId(userIdInt);

            try { user.setName(rs.getString("name")); } catch (Exception ignored) {}
            try { user.setEmail(rs.getString("email")); } catch (Exception ignored) {}
            try { user.setPasswordHash(rs.getString("password_hash")); } catch (Exception ignored) {}

            String role = null;
            try { role = rs.getString("role"); } catch (Exception ignored) {}
            if (role == null || role.isEmpty()) role = fallbackRole;
            user.setRole(toAppRole(role));

            String colIdStr = null;
            Integer colIntVal = null;
            try { colIdStr = rs.getString("college_id_str"); } catch (Exception ignored) {}
            if (colIdStr == null || colIdStr.isEmpty()) {
                try { colIdStr = rs.getString("college_id"); } catch (Exception ignored) {}
            }
            try {
                int cid = rs.getInt("college_id");
                if (!rs.wasNull()) colIntVal = cid;
            } catch (Exception ignored) {}

            if (colIdStr != null || colIntVal != null) {
                String shortCode = com.example.model.College.toShortCode(colIdStr, colIntVal, null);
                user.setCollegeIdString(shortCode);
                user.setCollegeId(com.example.model.College.codeToNumericId(shortCode));
            } else {
                user.setCollegeIdString(null);
                user.setCollegeId(null);
            }

            try { user.setAssignedCanteenId(rs.getString("assigned_canteen_id")); } catch (Exception ignored) {}
            try {
                String campusId = rs.getString("campus_id_number");
                if (campusId != null && !campusId.isEmpty()) {
                    user.setCampusIdNumber(campusId);
                } else {
                    user.setStudentRollNumber(rs.getString("student_roll_number"));
                }
            } catch (Exception ignored) {
                try { user.setStudentRollNumber(rs.getString("student_roll_number")); } catch (Exception ignored2) {}
            }

            try { user.setDepartment(rs.getString("department")); } catch (Exception ignored) {}
            try { user.setDesignation(rs.getString("designation")); } catch (Exception ignored) {}
            try { user.setPermissionLevel(rs.getString("permission_level")); } catch (Exception ignored) {}

            try { user.setPhoneNumber(rs.getString("phone_number")); } catch (Exception ignored) {}
            try {
                BigDecimal wb = rs.getBigDecimal("wallet_balance");
                if (wb != null) user.setWalletBalance(wb);
            } catch (Exception ignored) {}
            try {
                String st = rs.getString("status");
                if (st != null) user.setStatus(st);
            } catch (Exception ignored) {}

            return user;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing User model from ResultSet: " + e.getMessage());
            return null;
        }
    }

    /**
     * Inserts newly registered user into the appropriate dedicated role table:
     * - `campus_users` (for CUSTOMER / Campus Users)
     * - `canteen_staff` (for KITCHEN_STAFF / Canteen Staff)
     * - `college_admins` (for COLLEGE_ADMIN)
     * - `super_admins` (for SUPER_ADMIN)
     *
     * Also writes to legacy `users` table for backward compatibility if present,
     * and keeps LocalDataStore in sync.
     */
    public boolean insertUser(User user) {
        if (user == null) return false;

        String id = user.getIdString();
        if (id == null || id.trim().isEmpty()) {
            id = "user_campus_" + System.currentTimeMillis();
            user.setIdString(id);
        }

        String name = user.getName() != null ? user.getName().trim() : "";
        String email = user.getEmail() != null ? user.getEmail().trim() : "";
        String password = user.getPasswordHash();
        String appRole = user.getRole() != null ? user.getRole() : "CUSTOMER";
        String dbRole = toDbRole(appRole);

        // college_id in DataGrip is VARCHAR(64) referencing colleges.id (e.g. 'STAN')
        String collegeIdStr = College.toShortCode(user.getCollegeIdString(), user.getCollegeId(), null);

        String canteenId = user.getAssignedCanteenId();
        if (canteenId == null || canteenId.trim().isEmpty()) {
            canteenId = "canteen_1_1";
        }

        String campusIdNum = user.getCampusIdNumber();
        if (campusIdNum == null || campusIdNum.trim().isEmpty()) {
            campusIdNum = user.getStudentRollNumber();
        }
        if (campusIdNum == null || campusIdNum.trim().isEmpty()) {
            campusIdNum = "CAMPUS-" + (System.currentTimeMillis() % 100000);
        }

        String phone = user.getPhoneNumber() != null ? user.getPhoneNumber().trim() : "";
        BigDecimal balance = user.getWalletBalance() != null ? user.getWalletBalance() : new BigDecimal("350.00");
        String status = user.getStatus() != null ? user.getStatus() : "ACTIVE";

        if ("SUPER_ADMIN".equalsIgnoreCase(dbRole) || user.isSystemRole()) {
            collegeIdStr = null;
            canteenId = null;
        }

        boolean successInCloud = false;
        lastErrorMessage = null;

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            if (conn != null && !conn.isClosed()) {
                // Ensure default canteens exist in Cloud SQL so foreign keys succeed
                try {
                    String seedSql = "INSERT IGNORE INTO canteens (id, college_id, name, location_description, category, vendor_contact_name, vendor_phone, rating, is_open, average_prep_time_minutes) VALUES " +
                            "('cant_1', 'col_1', 'Central Campus Food Court', 'Engineering Block, Ground Floor', 'Cafeteria', 'Vendor 1', '+91 9876543201', 4.5, 1, 15), " +
                            "('cant_2', 'col_1', 'Nescafe Coffee Hub', 'Main Library Plaza', 'Beverages & Snacks', 'Vendor 2', '+91 9876543202', 4.3, 1, 10), " +
                            "('cant_3', 'col_2', 'COEP Main Canteen & Cafe', 'Campus Quadrangle, Ground Floor', 'Cafeteria', 'Vendor 3', '+91 9876543203', 4.6, 1, 15), " +
                            "('cant_4', 'col_2', 'Boat Club Food Express', 'Riverside Boat Club Complex', 'Snacks & Quick Bites', 'Vendor 4', '+91 9876543204', 4.4, 1, 10), " +
                            "('cant_5', 'col_3', 'Gulmohar Restaurant & Food Court', 'Central Campus, 1st Floor', 'Multi-Cuisine', 'Vendor 5', '+91 9876543205', 4.7, 1, 20), " +
                            "('cant_6', 'col_3', 'Hostel Quad Eatery', 'Hostel Complex Block 12', 'Meals & Dining', 'Vendor 6', '+91 9876543206', 4.2, 1, 12)";
                    try (PreparedStatement seedStmt = conn.prepareStatement(seedSql)) {
                        seedStmt.executeUpdate();
                    }
                } catch (Exception ignored) {}

                // Validate assigned_canteen_id against canteens(id) to avoid foreign key errors
                String validCanteenId = null;
                if (canteenId != null && !canteenId.trim().isEmpty()) {
                    try (PreparedStatement chk = conn.prepareStatement("SELECT id FROM canteens WHERE id = ? LIMIT 1")) {
                        chk.setString(1, canteenId);
                        try (ResultSet rs = chk.executeQuery()) {
                            if (rs.next()) validCanteenId = rs.getString(1);
                        }
                    } catch (Exception ignored) {}
                }
                if (validCanteenId == null && collegeIdStr != null) {
                    try (PreparedStatement chk = conn.prepareStatement("SELECT id FROM canteens WHERE college_id = ? LIMIT 1")) {
                        chk.setString(1, collegeIdStr);
                        try (ResultSet rs = chk.executeQuery()) {
                            if (rs.next()) validCanteenId = rs.getString(1);
                        }
                    } catch (Exception ignored) {}
                }
                if (validCanteenId == null) {
                    try (PreparedStatement chk = conn.prepareStatement("SELECT id FROM canteens LIMIT 1")) {
                        try (ResultSet rs = chk.executeQuery()) {
                            if (rs.next()) validCanteenId = rs.getString(1);
                        }
                    } catch (Exception ignored) {}
                }

                // Route to appropriate dedicated role table
                if ("CUSTOMER".equalsIgnoreCase(appRole) || "STUDENT".equalsIgnoreCase(dbRole)) {
                    // Exact schema for campus_users (10 columns):
                    // id, name, email, password_hash, college_id, assigned_canteen_id, campus_id_number, phone_number, wallet_balance, status
                    String sqlRole = "INSERT INTO campus_users (id, name, email, password_hash, college_id, assigned_canteen_id, campus_id_number, phone_number, wallet_balance, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(sqlRole)) {
                        stmt.setString(1, id);
                        stmt.setString(2, name);
                        stmt.setString(3, email);
                        stmt.setString(4, password);
                        stmt.setString(5, collegeIdStr);
                        if (validCanteenId != null) {
                            stmt.setString(6, validCanteenId);
                        } else {
                            stmt.setNull(6, java.sql.Types.VARCHAR);
                        }
                        stmt.setString(7, campusIdNum);
                        stmt.setString(8, phone);
                        stmt.setBigDecimal(9, balance);
                        stmt.setString(10, status);
                        int affected = stmt.executeUpdate();
                        if (affected > 0) {
                            successInCloud = true;
                            Log.i(TAG, "Successfully inserted into campus_users: " + email + " | ID: " + id);
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Insert into campus_users failed: " + ex.getMessage(), ex);
                        // If foreign key constraint failed on canteen, retry with NULL assigned_canteen_id
                        if (ex.getMessage() != null && ex.getMessage().contains("fk_cu_canteen")) {
                            try (PreparedStatement retryStmt = conn.prepareStatement(sqlRole)) {
                                retryStmt.setString(1, id);
                                retryStmt.setString(2, name);
                                retryStmt.setString(3, email);
                                retryStmt.setString(4, password);
                                retryStmt.setString(5, collegeIdStr);
                                retryStmt.setNull(6, java.sql.Types.VARCHAR);
                                retryStmt.setString(7, campusIdNum);
                                retryStmt.setString(8, phone);
                                retryStmt.setBigDecimal(9, balance);
                                retryStmt.setString(10, status);
                                if (retryStmt.executeUpdate() > 0) {
                                    successInCloud = true;
                                    Log.i(TAG, "Successfully inserted into campus_users after nulling canteen: " + email);
                                }
                            } catch (Exception retryEx) {
                                recordError(retryEx);
                            }
                        } else {
                            recordError(ex);
                        }
                    }
                } else if ("KITCHEN_STAFF".equalsIgnoreCase(appRole) || "VENDOR".equalsIgnoreCase(dbRole)) {
                    // Exact schema for canteen_staff (9 columns):
                    // id, name, email, password_hash, college_id, assigned_canteen_id, designation, phone_number, status
                    String sqlStaff = "INSERT INTO canteen_staff (id, name, email, password_hash, college_id, assigned_canteen_id, designation, phone_number, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(sqlStaff)) {
                        stmt.setString(1, id);
                        stmt.setString(2, name);
                        stmt.setString(3, email);
                        stmt.setString(4, password);
                        stmt.setString(5, collegeIdStr);
                        stmt.setString(6, validCanteenId != null ? validCanteenId : "cant_1");
                        stmt.setString(7, user.getDesignation() != null ? user.getDesignation() : "Kitchen Staff");
                        stmt.setString(8, phone);
                        stmt.setString(9, status);
                        int affected = stmt.executeUpdate();
                        if (affected > 0) {
                            successInCloud = true;
                            Log.i(TAG, "Successfully inserted into canteen_staff: " + email);
                        }
                    } catch (Exception ex) {
                        recordError(ex);
                        Log.e(TAG, "Insert into canteen_staff table failed: " + ex.getMessage(), ex);
                    }
                } else if ("COLLEGE_ADMIN".equalsIgnoreCase(appRole) || "COLLEGE_ADMIN".equalsIgnoreCase(dbRole)) {
                    // Exact schema for college_admins (8 columns):
                    // id, name, email, password_hash, college_id, department, phone_number, status
                    String sqlAdmin = "INSERT INTO college_admins (id, name, email, password_hash, college_id, department, phone_number, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(sqlAdmin)) {
                        stmt.setString(1, id);
                        stmt.setString(2, name);
                        stmt.setString(3, email);
                        stmt.setString(4, password);
                        stmt.setString(5, collegeIdStr);
                        stmt.setString(6, user.getDepartment() != null ? user.getDepartment() : "Campus Dining Administration");
                        stmt.setString(7, phone);
                        stmt.setString(8, status);
                        int affected = stmt.executeUpdate();
                        if (affected > 0) {
                            successInCloud = true;
                            Log.i(TAG, "Successfully inserted into college_admins: " + email);
                        }
                    } catch (Exception ex) {
                        recordError(ex);
                        Log.e(TAG, "Insert into college_admins table failed: " + ex.getMessage(), ex);
                    }
                } else if ("SUPER_ADMIN".equalsIgnoreCase(appRole) || "SUPER_ADMIN".equalsIgnoreCase(dbRole)) {
                    // Exact schema for super_admins (7 columns):
                    // id, name, email, password_hash, phone_number, permission_level, status
                    String sqlSuper = "INSERT INTO super_admins (id, name, email, password_hash, phone_number, permission_level, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(sqlSuper)) {
                        stmt.setString(1, id);
                        stmt.setString(2, name);
                        stmt.setString(3, email);
                        stmt.setString(4, password);
                        stmt.setString(5, phone);
                        stmt.setString(6, user.getPermissionLevel() != null ? user.getPermissionLevel() : "ALL");
                        stmt.setString(7, status);
                        int affected = stmt.executeUpdate();
                        if (affected > 0) {
                            successInCloud = true;
                            Log.i(TAG, "Successfully inserted into super_admins: " + email);
                        }
                    } catch (Exception ex) {
                        recordError(ex);
                        Log.e(TAG, "Insert into super_admins table failed: " + ex.getMessage(), ex);
                    }
                }
            }
        } catch (Exception e) {
            recordError(e);
            Log.e(TAG, "Cloud SQL insertUser error: " + e.getMessage(), e);
        }

        // Keep local cache in sync
        LocalDataStore.getInstance().insertUser(user);

        return successInCloud;
    }

    private void recordError(Throwable t) {
        if (t == null) return;
        String msg = t.getMessage() != null ? t.getMessage() : t.toString();
        if (msg.contains("Duplicate entry") || msg.contains("uq_campus_user_email") || msg.contains("1062")) {
            lastErrorMessage = "That email address is already in use. Please log in or use a different email.";
        } else if (msg.contains("fk_cu_college")) {
            lastErrorMessage = "Invalid college selection constraint.";
        } else if (msg.contains("fk_cu_canteen")) {
            lastErrorMessage = "Invalid canteen selection constraint.";
        } else {
            lastErrorMessage = "Database registration failed: " + msg;
        }
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            if (conn != null && !conn.isClosed()) {
                // 1. Try unified view vw_all_system_users
                try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM vw_all_system_users ORDER BY created_at DESC");
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        User u = parseUserFromResultSet(rs, "CUSTOMER");
                        if (u != null) list.add(u);
                    }
                    if (!list.isEmpty()) return list;
                } catch (Exception exView) {
                    Log.d(TAG, "vw_all_system_users query failed: " + exView.getMessage() + ". Querying separate role tables...");
                }

                // 2. Query 4 separate tables
                boolean foundInTables = false;
                try (PreparedStatement stmt = conn.prepareStatement("SELECT *, 'CUSTOMER' AS role FROM campus_users ORDER BY created_at DESC");
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        User u = parseUserFromResultSet(rs, "CUSTOMER");
                        if (u != null) { list.add(u); foundInTables = true; }
                    }
                } catch (Exception ignored) {}

                try (PreparedStatement stmt = conn.prepareStatement("SELECT *, 'KITCHEN_STAFF' AS role FROM canteen_staff ORDER BY created_at DESC");
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        User u = parseUserFromResultSet(rs, "KITCHEN_STAFF");
                        if (u != null) { list.add(u); foundInTables = true; }
                    }
                } catch (Exception ignored) {}

                try (PreparedStatement stmt = conn.prepareStatement("SELECT *, 'COLLEGE_ADMIN' AS role FROM college_admins ORDER BY created_at DESC");
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        User u = parseUserFromResultSet(rs, "COLLEGE_ADMIN");
                        if (u != null) { list.add(u); foundInTables = true; }
                    }
                } catch (Exception ignored) {}

                try (PreparedStatement stmt = conn.prepareStatement("SELECT *, 'SUPER_ADMIN' AS role FROM super_admins ORDER BY created_at DESC");
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        User u = parseUserFromResultSet(rs, "SUPER_ADMIN");
                        if (u != null) { list.add(u); foundInTables = true; }
                    }
                } catch (Exception ignored) {}

                if (foundInTables && !list.isEmpty()) return list;

                // 3. Fallback to legacy users table
                try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users ORDER BY created_at DESC");
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        User u = parseUserFromResultSet(rs, "CUSTOMER");
                        if (u != null) list.add(u);
                    }
                    if (!list.isEmpty()) return list;
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL getAllUsers failed: " + e.getMessage());
        }

        return LocalDataStore.getInstance().getAllUsers();
    }

    public boolean updateStaffCollegeAndCanteen(String email, String role, String collegeIdString, String canteenId) {
        if (email == null || email.trim().isEmpty()) return false;
        String cleanEmail = email.trim();
        String colStr = College.toShortCode(collegeIdString, null, null);
        int colInt = College.codeToNumericId(colStr);

        boolean cloudSuccess = false;
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            if (conn != null && !conn.isClosed()) {
                if ("KITCHEN_STAFF".equalsIgnoreCase(role) || "CANTEEN_STAFF".equalsIgnoreCase(role)) {
                    // Update canteen_staff table (columns: college_id, assigned_canteen_id)
                    String sql = "UPDATE canteen_staff SET college_id = ?, assigned_canteen_id = ?, updated_at = CURRENT_TIMESTAMP WHERE email = ? OR id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, colStr);
                        stmt.setString(2, canteenId != null ? canteenId : "canteen_1_1");
                        stmt.setString(3, cleanEmail);
                        stmt.setString(4, cleanEmail);
                        int affected = stmt.executeUpdate();
                        if (affected > 0) {
                            cloudSuccess = true;
                            Log.d(TAG, "canteen_staff table updated for " + cleanEmail + " -> col=" + colStr + ", canteen=" + canteenId);
                        }
                    } catch (Exception e1) {
                        try (PreparedStatement stmt = conn.prepareStatement("UPDATE canteen_staff SET college_id = ?, assigned_canteen_id = ?, updated_at = CURRENT_TIMESTAMP WHERE email = ? OR id = ?")) {
                            stmt.setInt(1, colInt);
                            stmt.setString(2, canteenId != null ? canteenId : "canteen_1_1");
                            stmt.setString(3, cleanEmail);
                            stmt.setString(4, cleanEmail);
                            int affected = stmt.executeUpdate();
                            if (affected > 0) cloudSuccess = true;
                        } catch (Exception ignored) {}
                    }
                    try (PreparedStatement stmt = conn.prepareStatement("UPDATE canteen_staff SET college_id_str = ? WHERE email = ? OR id = ?")) {
                        stmt.setString(1, colStr);
                        stmt.setString(2, cleanEmail);
                        stmt.setString(3, cleanEmail);
                        stmt.executeUpdate();
                    } catch (Exception ignored) {}
                } else if ("COLLEGE_ADMIN".equalsIgnoreCase(role)) {
                    // Update college_admins table (column: college_id)
                    String sql = "UPDATE college_admins SET college_id = ?, updated_at = CURRENT_TIMESTAMP WHERE email = ? OR id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, colStr);
                        stmt.setString(2, cleanEmail);
                        stmt.setString(3, cleanEmail);
                        int affected = stmt.executeUpdate();
                        if (affected > 0) {
                            cloudSuccess = true;
                            Log.d(TAG, "college_admins table updated for " + cleanEmail + " -> col=" + colStr);
                        }
                    } catch (Exception e1) {
                        try (PreparedStatement stmt = conn.prepareStatement("UPDATE college_admins SET college_id = ?, updated_at = CURRENT_TIMESTAMP WHERE email = ? OR id = ?")) {
                            stmt.setInt(1, colInt);
                            stmt.setString(2, cleanEmail);
                            stmt.setString(3, cleanEmail);
                            int affected = stmt.executeUpdate();
                            if (affected > 0) cloudSuccess = true;
                        } catch (Exception ignored) {}
                    }
                    try (PreparedStatement stmt = conn.prepareStatement("UPDATE college_admins SET college_id_str = ? WHERE email = ? OR id = ?")) {
                        stmt.setString(1, colStr);
                        stmt.setString(2, cleanEmail);
                        stmt.setString(3, cleanEmail);
                        stmt.executeUpdate();
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL updateStaffCollegeAndCanteen failed: " + e.getMessage());
        }

        // Update local memory store user
        for (User u : LocalDataStore.getInstance().getAllUsers()) {
            if (cleanEmail.equalsIgnoreCase(u.getEmail())) {
                u.setCollegeId(colInt);
                u.setCollegeIdString(colStr);
                if (canteenId != null && !canteenId.isEmpty()) {
                    u.setAssignedCanteenId(canteenId);
                }
                break;
            }
        }

        return cloudSuccess;
    }

    public boolean updateUserCollegeAndCanteen(int userId, String idString, int collegeId, String collegeIdString, String canteenId) {
        String idToUse = idString != null ? idString : ("user_campus_" + userId);
        String colStr = collegeIdString != null ? collegeIdString : ("col_" + collegeId);

        boolean cloudSuccess = false;
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            if (conn != null && !conn.isClosed()) {
                // Update campus_users (columns: college_id, assigned_canteen_id, id, campus_id_number)
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE campus_users SET college_id = ?, assigned_canteen_id = ? WHERE id = ? OR campus_id_number = ? OR user_id = ?")) {
                    stmt.setString(1, colStr);
                    stmt.setString(2, canteenId);
                    stmt.setString(3, idToUse);
                    stmt.setString(4, String.valueOf(userId));
                    stmt.setInt(5, userId);
                    if (stmt.executeUpdate() > 0) cloudSuccess = true;
                } catch (Exception ex) {
                    Log.d(TAG, "Update campus_users college/canteen error: " + ex.getMessage());
                }

                // Update canteen_staff (columns: college_id, assigned_canteen_id, id)
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE canteen_staff SET college_id = ?, assigned_canteen_id = ? WHERE id = ? OR staff_id = ?")) {
                    stmt.setString(1, colStr);
                    stmt.setString(2, canteenId);
                    stmt.setString(3, idToUse);
                    stmt.setInt(4, userId);
                    if (stmt.executeUpdate() > 0) cloudSuccess = true;
                } catch (Exception ex) {
                    Log.d(TAG, "Update canteen_staff college/canteen error: " + ex.getMessage());
                }

                // Update college_admins (column: college_id)
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE college_admins SET college_id = ? WHERE id = ? OR admin_id = ?")) {
                    stmt.setString(1, colStr);
                    stmt.setString(2, idToUse);
                    stmt.setInt(3, userId);
                    if (stmt.executeUpdate() > 0) cloudSuccess = true;
                } catch (Exception ex) {
                    Log.d(TAG, "Update college_admins college error: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL updateUserCollegeAndCanteen failed: " + e.getMessage());
        }

        // Update local cache
        User local = LocalDataStore.getInstance().getUserById(userId);
        if (local != null) {
            local.setCollegeId(collegeId);
            local.setCollegeIdString(colStr);
            local.setAssignedCanteenId(canteenId);
        }
        return cloudSuccess || local != null;
    }

    public boolean updateWalletBalance(int userId, String idString, BigDecimal newBalance) {
        String idToUse = idString != null ? idString : ("user_campus_" + userId);

        boolean cloudSuccess = false;
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            if (conn != null && !conn.isClosed()) {
                // Update campus_users (columns: wallet_balance, id, campus_id_number)
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE campus_users SET wallet_balance = ? WHERE id = ? OR campus_id_number = ?")) {
                    stmt.setBigDecimal(1, newBalance);
                    stmt.setString(2, idToUse);
                    stmt.setString(3, String.valueOf(userId));
                    if (stmt.executeUpdate() > 0) cloudSuccess = true;
                } catch (Exception ex) {
                    Log.d(TAG, "Update campus_users wallet_balance error: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL updateWalletBalance failed: " + e.getMessage());
        }

        User local = LocalDataStore.getInstance().getUserById(userId);
        if (local != null) {
            local.setWalletBalance(newBalance);
        }
        return cloudSuccess || local != null;
    }
}
