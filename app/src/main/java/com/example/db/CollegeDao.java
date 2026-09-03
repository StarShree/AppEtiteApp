package com.example.db;

import android.util.Log;

import com.example.model.College;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CollegeDao {
    private static final String TAG = "CollegeDao";

    public List<College> getAllColleges() {
        List<College> list = new ArrayList<>();
        // Try Cloud SQL schema: id, name, address
        String sql = "SELECT * FROM colleges ORDER BY name ASC";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String idStr = null;
                    int idInt = 1;
                    try {
                        idStr = rs.getString("id");
                    } catch (Exception ignored) {}
                    
                    if (idStr == null) {
                        try {
                            idInt = rs.getInt("college_id");
                            idStr = "col_" + idInt;
                        } catch (Exception ignored) {}
                    }

                    String name = rs.getString("name");
                    String location = "";
                    try {
                        location = rs.getString("address");
                    } catch (Exception ignored) {
                        try {
                            location = rs.getString("location");
                        } catch (Exception ignored2) {}
                    }

                    College c = new College(idStr, name, location);
                    list.add(c);
                }
                if (!list.isEmpty()) {
                    return list;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL getAllColleges query failed: " + e.getMessage() + ". Using local store.");
        }

        return LocalDataStore.getInstance().getAllColleges();
    }

    public College getCollegeById(int collegeId) {
        String shortCode = College.toShortCode(null, collegeId, null);
        String sql = "SELECT * FROM colleges WHERE id = ? OR id = ? OR college_id = ? OR id = ? LIMIT 1";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, shortCode);
                stmt.setString(2, "col_" + collegeId);
                stmt.setInt(3, collegeId);
                stmt.setString(4, String.valueOf(collegeId));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String idStr = null;
                        try { idStr = rs.getString("id"); } catch (Exception ignored) {}
                        if (idStr == null) idStr = shortCode;
                        String name = rs.getString("name");
                        String location = "";
                        try {
                            location = rs.getString("address");
                        } catch (Exception ignored) {
                            try {
                                location = rs.getString("location");
                            } catch (Exception ignored2) {}
                        }
                        return new College(idStr, name, location);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL getCollegeById query failed: " + e.getMessage());
        }

        return LocalDataStore.getInstance().getCollegeById(collegeId);
    }

    public College getCollegeByCode(String code) {
        if (code == null) return null;
        String shortCode = College.toShortCode(code, null, null);
        int numericId = College.codeToNumericId(shortCode);
        College c = getCollegeById(numericId);
        if (c != null) return c;
        return LocalDataStore.getInstance().getCollegeByCode(shortCode);
    }
}
