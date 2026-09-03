package com.example.db;

import android.util.Log;

import com.example.model.College;
import com.example.model.MenuItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MenuItemDao {
    private static final String TAG = "MenuItemDao";

    /**
     * Fetches menu items by college, supporting both:
     * 1. Direct Cloud SQL table schema (id VARCHAR, college_id VARCHAR/INT, is_available TINYINT/BOOLEAN)
     * 2. LocalDataStore in-memory cache fallback.
     */
    public List<MenuItem> getMenuItemsByCollege(int collegeId, String category) {
        List<MenuItem> list = new ArrayList<>();
        boolean hasCatFilter = category != null && !category.equalsIgnoreCase("All");

        String colStrNum = String.valueOf(collegeId);
        String colStrPref = "col_" + collegeId;
        String colShortCode = College.toShortCode(null, collegeId, null);

        String sql = "SELECT * FROM menu_items WHERE (college_id = ? OR college_id = ? OR college_id = ?)"
                + (hasCatFilter ? " AND category = ?" : "")
                + " ORDER BY category, name";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            if (conn != null && !conn.isClosed()) {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, colStrNum);
                    stmt.setString(2, colStrPref);
                    stmt.setString(3, colShortCode != null ? colShortCode : colStrNum);
                    if (hasCatFilter) {
                        stmt.setString(4, category);
                    }
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            MenuItem item = parseMenuItemFromResultSet(rs, collegeId);
                            if (item != null) {
                                list.add(item);
                            }
                        }
                    }
                }

                if (!list.isEmpty()) {
                    // Keep LocalDataStore cache synced with live Cloud SQL availability
                    for (MenuItem item : list) {
                        LocalDataStore.getInstance().updateMenuItemAvailability(item.getIdString(), item.getItemId(), item.isAvailable());
                    }
                    return list;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL getMenuItems query failed: " + e.getMessage() + ". Using local store.");
        }

        return LocalDataStore.getInstance().getMenuItemsByCollege(collegeId, category);
    }

    private MenuItem parseMenuItemFromResultSet(ResultSet rs, int fallbackCollegeId) {
        try {
            MenuItem item = new MenuItem();

            // Primary key: column may be 'id' (VARCHAR) or 'item_id' (INT)
            String idVal = null;
            try { idVal = rs.getString("id"); } catch (Exception ignored) {}
            if (idVal == null) {
                try { idVal = rs.getString("item_id"); } catch (Exception ignored) {}
            }
            item.setIdString(idVal);

            int itemIdInt = 0;
            try { itemIdInt = rs.getInt("item_id"); } catch (Exception ignored) {}
            if (itemIdInt == 0 && idVal != null) {
                if (idVal.contains("_")) {
                    try {
                        itemIdInt = Integer.parseInt(idVal.substring(idVal.lastIndexOf('_') + 1));
                    } catch (Exception ignored) {
                        itemIdInt = Math.abs(idVal.hashCode() % 100000);
                    }
                } else {
                    try {
                        itemIdInt = Integer.parseInt(idVal);
                    } catch (Exception ignored) {
                        itemIdInt = Math.abs(idVal.hashCode() % 100000);
                    }
                }
            }
            item.setItemId(itemIdInt);

            // College ID: column may be VARCHAR or INT
            int colId = fallbackCollegeId;
            try {
                String cIdStr = rs.getString("college_id");
                if (cIdStr != null) {
                    if (cIdStr.contains("_")) {
                        colId = Integer.parseInt(cIdStr.substring(cIdStr.lastIndexOf('_') + 1));
                    } else if (Character.isDigit(cIdStr.charAt(0))) {
                        colId = Integer.parseInt(cIdStr);
                    } else {
                        colId = College.codeToNumericId(cIdStr);
                    }
                }
            } catch (Exception ignored) {}
            item.setCollegeId(colId);

            try { item.setCanteenId(rs.getString("canteen_id")); } catch (Exception ignored) {}
            try { item.setName(rs.getString("name")); } catch (Exception ignored) {}
            try { item.setDescription(rs.getString("description")); } catch (Exception ignored) {}
            try { item.setPrice(rs.getBigDecimal("price")); } catch (Exception ignored) {}
            try { item.setCategory(rs.getString("category")); } catch (Exception ignored) {}
            try { item.setImageUrl(rs.getString("image_url")); } catch (Exception ignored) {}

            boolean isAvailable = true;
            try {
                isAvailable = rs.getBoolean("is_available");
            } catch (Exception ignored) {}
            item.setAvailable(isAvailable);

            return item;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing MenuItem from ResultSet: " + e.getMessage(), e);
            return null;
        }
    }

    public boolean insertMenuItem(MenuItem item) {
        if (item == null) return false;
        boolean successInCloud = false;

        String idVal = item.getIdString();
        if (idVal == null) {
            idVal = "item_" + (item.getItemId() > 0 ? String.format("%02d", item.getItemId()) : System.currentTimeMillis());
            item.setIdString(idVal);
        }

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            if (conn != null && !conn.isClosed()) {
                // Try inserting with id (VARCHAR) schema
                String sql = "INSERT INTO menu_items (id, college_id, canteen_id, name, description, price, category, image_url, is_available) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, idVal);
                    stmt.setString(2, "col_" + item.getCollegeId());
                    stmt.setString(3, item.getCanteenId() != null ? item.getCanteenId() : "canteen_" + item.getCollegeId() + "_1");
                    stmt.setString(4, item.getName());
                    stmt.setString(5, item.getDescription());
                    stmt.setBigDecimal(6, item.getPrice());
                    stmt.setString(7, item.getCategory());
                    stmt.setString(8, item.getImageUrl());
                    stmt.setBoolean(9, item.isAvailable());

                    int affected = stmt.executeUpdate();
                    if (affected > 0) {
                        successInCloud = true;
                    }
                } catch (Exception ex1) {
                    // Fallback to integer college_id / auto-inc schema if present
                    String sqlFallback = "INSERT INTO menu_items (college_id, name, description, price, category, image_url, is_available) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement stmt2 = conn.prepareStatement(sqlFallback, Statement.RETURN_GENERATED_KEYS)) {
                        stmt2.setInt(1, item.getCollegeId());
                        stmt2.setString(2, item.getName());
                        stmt2.setString(3, item.getDescription());
                        stmt2.setBigDecimal(4, item.getPrice());
                        stmt2.setString(5, item.getCategory());
                        stmt2.setString(6, item.getImageUrl());
                        stmt2.setBoolean(7, item.isAvailable());

                        int affected = stmt2.executeUpdate();
                        if (affected > 0) {
                            try (ResultSet rs = stmt2.getGeneratedKeys()) {
                                if (rs.next()) {
                                    item.setItemId(rs.getInt(1));
                                }
                            }
                            successInCloud = true;
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL insertMenuItem failed: " + e.getMessage());
        }

        boolean localSuccess = LocalDataStore.getInstance().insertMenuItem(item);
        return successInCloud || localSuccess;
    }

    public boolean updateMenuItemAvailability(int itemId, boolean isAvailable) {
        return updateMenuItemAvailability(null, itemId, isAvailable);
    }

    public boolean updateMenuItemAvailability(String idString, int itemId, boolean isAvailable) {
        boolean successInCloud = false;
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            if (conn != null && !conn.isClosed()) {
                String id1 = idString != null ? idString : ("item_" + itemId);
                String id2 = String.format("item_%02d", itemId);
                String id3 = String.valueOf(itemId);

                // 1. Try updating by column `id`
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE menu_items SET is_available = ? WHERE id = ? OR id = ? OR id = ?")) {
                    stmt.setBoolean(1, isAvailable);
                    stmt.setString(2, id1);
                    stmt.setString(3, id2);
                    stmt.setString(4, id3);
                    int affected = stmt.executeUpdate();
                    if (affected > 0) {
                        successInCloud = true;
                        Log.i(TAG, "Updated Cloud SQL menu_items by id: " + id1 + " -> " + isAvailable);
                    }
                } catch (Exception exId) {
                    Log.d(TAG, "Update by id column note: " + exId.getMessage());
                }

                // 2. If not affected, try updating by column `item_id`
                if (!successInCloud) {
                    try (PreparedStatement stmt2 = conn.prepareStatement(
                            "UPDATE menu_items SET is_available = ? WHERE item_id = ?")) {
                        stmt2.setBoolean(1, isAvailable);
                        stmt2.setInt(2, itemId);
                        int affected = stmt2.executeUpdate();
                        if (affected > 0) {
                            successInCloud = true;
                            Log.i(TAG, "Updated Cloud SQL menu_items by item_id: " + itemId + " -> " + isAvailable);
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL updateMenuItemAvailability error: " + e.getMessage());
        }

        // Always keep LocalDataStore in sync
        boolean localSuccess = LocalDataStore.getInstance().updateMenuItemAvailability(idString, itemId, isAvailable);
        return successInCloud || localSuccess;
    }

    public boolean deleteMenuItem(int itemId) {
        return deleteMenuItem(null, itemId);
    }

    public boolean deleteMenuItem(String idString, int itemId) {
        boolean successInCloud = false;
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            if (conn != null && !conn.isClosed()) {
                String id1 = idString != null ? idString : ("item_" + itemId);
                String id2 = String.format("item_%02d", itemId);
                String id3 = String.valueOf(itemId);

                try (PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM menu_items WHERE id = ? OR id = ? OR id = ?")) {
                    stmt.setString(1, id1);
                    stmt.setString(2, id2);
                    stmt.setString(3, id3);
                    if (stmt.executeUpdate() > 0) {
                        successInCloud = true;
                    }
                } catch (Exception exId) {
                    try (PreparedStatement stmt2 = conn.prepareStatement(
                            "DELETE FROM menu_items WHERE item_id = ?")) {
                        stmt2.setInt(1, itemId);
                        if (stmt2.executeUpdate() > 0) {
                            successInCloud = true;
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL deleteMenuItem failed: " + e.getMessage());
        }

        boolean localSuccess = LocalDataStore.getInstance().deleteMenuItem(itemId);
        return successInCloud || localSuccess;
    }
}
