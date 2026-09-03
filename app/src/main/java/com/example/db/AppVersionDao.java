package com.example.db;

import android.util.Log;

import com.example.model.AppVersionConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AppVersionDao {
    private static final String TAG = "AppVersionDao";

    /**
     * Retrieves the active app version configuration for the specified platform from Cloud SQL.
     * Falls back to LocalDataStore if database connection fails or table is unreachable.
     */
    public AppVersionConfig getVersionConfig(String platform) {
        String queryPlatform = platform != null ? platform.toLowerCase() : "android";
        String sql = "SELECT id, platform, min_version_code, min_version_name, latest_version_code, "
                + "latest_version_name, is_force_update, update_title, update_message, update_url "
                + "FROM app_version_config WHERE platform = ? ORDER BY id DESC LIMIT 1";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            if (conn != null && !conn.isClosed()) {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, queryPlatform);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            AppVersionConfig config = new AppVersionConfig(
                                    rs.getInt("id"),
                                    rs.getString("platform"),
                                    rs.getInt("min_version_code"),
                                    rs.getString("min_version_name"),
                                    rs.getInt("latest_version_code"),
                                    rs.getString("latest_version_name"),
                                    rs.getBoolean("is_force_update"),
                                    rs.getString("update_title"),
                                    rs.getString("update_message"),
                                    rs.getString("update_url")
                            );
                            Log.d(TAG, "Fetched version config from Cloud SQL: minCode="
                                    + config.getMinVersionCode() + ", latestCode=" + config.getLatestVersionCode());
                            // Keep LocalDataStore in sync
                            LocalDataStore.getInstance().updateAppVersionConfig(config);
                            return config;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL version check query failed: " + e.getMessage() + ". Using LocalDataStore fallback.");
        }

        return LocalDataStore.getInstance().getAppVersionConfig(queryPlatform);
    }

    /**
     * Updates the version config in Cloud SQL and LocalDataStore.
     */
    public boolean updateVersionConfig(AppVersionConfig config) {
        if (config == null) return false;

        String sql = "UPDATE app_version_config SET min_version_code = ?, min_version_name = ?, "
                + "latest_version_code = ?, latest_version_name = ?, is_force_update = ?, "
                + "update_title = ?, update_message = ?, update_url = ? WHERE platform = ?";

        boolean cloudSuccess = false;
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            if (conn != null && !conn.isClosed()) {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, config.getMinVersionCode());
                    stmt.setString(2, config.getMinVersionName());
                    stmt.setInt(3, config.getLatestVersionCode());
                    stmt.setString(4, config.getLatestVersionName());
                    stmt.setBoolean(5, config.isForceUpdate());
                    stmt.setString(6, config.getUpdateTitle());
                    stmt.setString(7, config.getUpdateMessage());
                    stmt.setString(8, config.getUpdateUrl());
                    stmt.setString(9, config.getPlatform() != null ? config.getPlatform() : "android");

                    int affected = stmt.executeUpdate();
                    cloudSuccess = affected > 0;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL updateVersionConfig failed: " + e.getMessage());
        }

        boolean localSuccess = LocalDataStore.getInstance().updateAppVersionConfig(config);
        return cloudSuccess || localSuccess;
    }
}
