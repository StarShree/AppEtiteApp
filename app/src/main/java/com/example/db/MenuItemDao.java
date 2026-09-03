package com.example.db;

import android.util.Log;

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

    public List<MenuItem> getMenuItemsByCollege(int collegeId, String category) {
        List<MenuItem> list = new ArrayList<>();
        boolean hasCatFilter = category != null && !category.equalsIgnoreCase("All");

        String sql = "SELECT item_id, college_id, name, description, price, category, image_url, is_available "
                + "FROM menu_items WHERE college_id = ?"
                + (hasCatFilter ? " AND category = ?" : "")
                + " ORDER BY category, name";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, collegeId);
                if (hasCatFilter) {
                    stmt.setString(2, category);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        MenuItem item = new MenuItem(
                                rs.getInt("item_id"),
                                rs.getInt("college_id"),
                                rs.getString("name"),
                                rs.getString("description"),
                                rs.getBigDecimal("price"),
                                rs.getString("category"),
                                rs.getString("image_url"),
                                rs.getBoolean("is_available")
                        );
                        list.add(item);
                    }
                    if (!list.isEmpty()) {
                        return list;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL getMenuItems query failed: " + e.getMessage() + ". Using local store.");
        }

        return LocalDataStore.getInstance().getMenuItemsByCollege(collegeId, category);
    }

    public boolean insertMenuItem(MenuItem item) {
        if (item == null) return false;
        String sql = "INSERT INTO menu_items (college_id, name, description, price, category, image_url, is_available) VALUES (?, ?, ?, ?, ?, ?, ?)";

        boolean successInCloud = false;
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, item.getCollegeId());
                stmt.setString(2, item.getName());
                stmt.setString(3, item.getDescription());
                stmt.setBigDecimal(4, item.getPrice());
                stmt.setString(5, item.getCategory());
                stmt.setString(6, item.getImageUrl());
                stmt.setBoolean(7, item.isAvailable());

                int affected = stmt.executeUpdate();
                if (affected > 0) {
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            item.setItemId(rs.getInt(1));
                        }
                    }
                    successInCloud = true;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL insertMenuItem failed: " + e.getMessage());
        }

        boolean localSuccess = LocalDataStore.getInstance().insertMenuItem(item);
        return successInCloud || localSuccess;
    }

    public boolean updateMenuItemAvailability(int itemId, boolean isAvailable) {
        String sql = "UPDATE menu_items SET is_available = ? WHERE item_id = ?";
        boolean successInCloud = false;
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setBoolean(1, isAvailable);
                stmt.setInt(2, itemId);
                int affected = stmt.executeUpdate();
                successInCloud = affected > 0;
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL updateMenuItemAvailability failed: " + e.getMessage());
        }

        boolean localSuccess = LocalDataStore.getInstance().updateMenuItemAvailability(itemId, isAvailable);
        return successInCloud || localSuccess;
    }

    public boolean deleteMenuItem(int itemId) {
        String sql = "DELETE FROM menu_items WHERE item_id = ?";
        boolean successInCloud = false;
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, itemId);
                int affected = stmt.executeUpdate();
                successInCloud = affected > 0;
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL deleteMenuItem failed: " + e.getMessage());
        }

        boolean localSuccess = LocalDataStore.getInstance().deleteMenuItem(itemId);
        return successInCloud || localSuccess;
    }
}
