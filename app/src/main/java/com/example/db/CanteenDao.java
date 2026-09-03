package com.example.db;

import android.util.Log;

import com.example.model.Canteen;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CanteenDao {
    private static final String TAG = "CanteenDao";

    public List<Canteen> getCanteensForCollege(String collegeIdString) {
        List<Canteen> list = new ArrayList<>();
        if (collegeIdString == null || collegeIdString.isEmpty()) {
            collegeIdString = "STAN";
        }

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            if (conn != null && !conn.isClosed()) {
                ensureCanteensSeed(conn);

                // Attempt Cloud SQL query
                String shortCode = com.example.model.College.toShortCode(collegeIdString, null, null);
                int numId = com.example.model.College.codeToNumericId(shortCode);
                String sql = "SELECT * FROM canteens WHERE college_id = ? OR college_id = ? OR college_id = ? ORDER BY id ASC";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, shortCode);
                    stmt.setString(2, "col_" + numId);
                    stmt.setString(3, String.valueOf(numId));
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            String id = rs.getString("id");
                            String colId = rs.getString("college_id");
                            String name = rs.getString("name");
                            String location = "";
                            try {
                                location = rs.getString("location_description");
                            } catch (Exception ignored) {
                                try {
                                    location = rs.getString("location");
                                } catch (Exception ignored2) {}
                            }
                            list.add(new Canteen(id, shortCode, numId, name, location, "8:00 AM - 9:00 PM"));
                        }
                        if (!list.isEmpty()) {
                            return list;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Cloud SQL canteens query skipped: " + e.getMessage());
        }

        return LocalDataStore.getInstance().getCanteensForCollege(collegeIdString);
    }

    private synchronized void ensureCanteensSeed(Connection conn) {
        try {
            String seedSql = "INSERT IGNORE INTO canteens (id, college_id, name, location_description, category, vendor_contact_name, vendor_phone, rating, is_open, average_prep_time_minutes) VALUES " +
                    "('cant_1', 'col_1', 'Central Campus Food Court', 'Engineering Block, Ground Floor', 'Cafeteria', 'Vendor 1', '+91 9876543201', 4.5, 1, 15), " +
                    "('cant_2', 'col_1', 'Nescafe Coffee Hub', 'Main Library Plaza', 'Beverages & Snacks', 'Vendor 2', '+91 9876543202', 4.3, 1, 10), " +
                    "('cant_3', 'col_2', 'COEP Main Canteen & Cafe', 'Campus Quadrangle, Ground Floor', 'Cafeteria', 'Vendor 3', '+91 9876543203', 4.6, 1, 15), " +
                    "('cant_4', 'col_2', 'Boat Club Food Express', 'Riverside Boat Club Complex', 'Snacks & Quick Bites', 'Vendor 4', '+91 9876543204', 4.4, 1, 10), " +
                    "('cant_5', 'col_3', 'Gulmohar Restaurant & Food Court', 'Central Campus, 1st Floor', 'Multi-Cuisine', 'Vendor 5', '+91 9876543205', 4.7, 1, 20), " +
                    "('cant_6', 'col_3', 'Hostel Quad Eatery', 'Hostel Complex Block 12', 'Meals & Dining', 'Vendor 6', '+91 9876543206', 4.2, 1, 12)";
            try (PreparedStatement stmt = conn.prepareStatement(seedSql)) {
                stmt.executeUpdate();
            }
        } catch (Exception ignored) {}
    }

    public List<Canteen> getCanteensForCollege(int collegeId) {
        return getCanteensForCollege(com.example.model.College.toShortCode(null, collegeId, null));
    }

    public Canteen getCanteenById(String canteenId) {
        for (Canteen c : LocalDataStore.getInstance().getAllCanteens()) {
            if (c.getCanteenId().equalsIgnoreCase(canteenId)) {
                return c;
            }
        }
        return null;
    }

    private int parseCollegeInt(String colId) {
        if (colId != null && colId.startsWith("col_")) {
            try {
                return Integer.parseInt(colId.substring(4));
            } catch (Exception ignored) {}
        }
        return 1;
    }
}
