package com.example.db;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;

public class DBInit {
    private static final String TAG = "DBInit";

    public static void initializeDatabaseAsync(Context context) {
        DatabaseExecutor.execute(() -> {
            try {
                Log.d(TAG, "Attempting initial connection to Cloud SQL: " + DBConnection.getHost());
                Connection conn = DBConnection.getInstance().getConnection();
                if (conn != null && !conn.isClosed()) {
                    Log.d(TAG, "Cloud SQL connection established! Checking schema...");
                    // Run schema DDL if needed
                    try (InputStream is = context.getAssets().open("schema.sql");
                         BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.trim().startsWith("--") || line.trim().isEmpty()) {
                                continue;
                            }
                            sb.append(line).append("\n");
                            if (line.trim().endsWith(";")) {
                                String statementSql = sb.toString().trim();
                                sb.setLength(0);
                                if (!statementSql.isEmpty()) {
                                    try (Statement stmt = conn.createStatement()) {
                                        stmt.execute(statementSql);
                                    } catch (Exception sqlEx) {
                                        // Ignore errors on existing indexes/tables
                                    }
                                }
                            }
                        }
                        Log.d(TAG, "Cloud SQL schema check completed successfully.");

                        // Backfill any existing NULL assigned_canteen_id in Cloud SQL tables
                        try (Statement stmt = conn.createStatement()) {
                            stmt.executeUpdate("UPDATE campus_users SET assigned_canteen_id = 'canteen_1_1' WHERE (college_id = 1 OR college_id_str = 'col_1') AND (assigned_canteen_id IS NULL OR assigned_canteen_id = '')");
                            stmt.executeUpdate("UPDATE canteen_staff SET assigned_canteen_id = 'canteen_1_1' WHERE (college_id = 1 OR college_id_str = 'col_1') AND (assigned_canteen_id IS NULL OR assigned_canteen_id = '')");
                        } catch (Exception updateEx) {
                            Log.d(TAG, "Backfill assigned_canteen_id note: " + updateEx.getMessage());
                        }

                        // Migrate legacy college IDs ("col_1", "1", etc.) to canonical short IDs: STAN, IMP, SIT
                        try (Statement stmt = conn.createStatement()) {
                            stmt.executeUpdate("UPDATE canteens SET college_id = 'STAN' WHERE college_id = 'col_1' OR college_id = '1'");
                            stmt.executeUpdate("UPDATE canteens SET college_id = 'IMP' WHERE college_id = 'col_2' OR college_id = '2'");
                            stmt.executeUpdate("UPDATE canteens SET college_id = 'SIT' WHERE college_id = 'col_3' OR college_id = '3'");

                            stmt.executeUpdate("UPDATE campus_users SET college_id_str = 'STAN' WHERE college_id = 1 OR college_id_str = 'col_1'");
                            stmt.executeUpdate("UPDATE campus_users SET college_id_str = 'IMP' WHERE college_id = 2 OR college_id_str = 'col_2'");
                            stmt.executeUpdate("UPDATE campus_users SET college_id_str = 'SIT' WHERE college_id = 3 OR college_id_str = 'col_3'");

                            stmt.executeUpdate("UPDATE canteen_staff SET college_id_str = 'STAN' WHERE college_id = 1 OR college_id_str = 'col_1'");
                            stmt.executeUpdate("UPDATE canteen_staff SET college_id_str = 'IMP' WHERE college_id = 2 OR college_id_str = 'col_2'");
                            stmt.executeUpdate("UPDATE canteen_staff SET college_id_str = 'SIT' WHERE college_id = 3 OR college_id_str = 'col_3'");

                            stmt.executeUpdate("UPDATE college_admins SET college_id_str = 'STAN' WHERE college_id = 1 OR college_id_str = 'col_1'");
                            stmt.executeUpdate("UPDATE college_admins SET college_id_str = 'IMP' WHERE college_id = 2 OR college_id_str = 'col_2'");
                            stmt.executeUpdate("UPDATE college_admins SET college_id_str = 'SIT' WHERE college_id = 3 OR college_id_str = 'col_3'");
                        } catch (Exception updateEx) {
                            Log.d(TAG, "Short college ID migration note: " + updateEx.getMessage());
                        }

                        // Ensure app_version_config table exists and is seeded
                        try (Statement stmt = conn.createStatement()) {
                            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS app_version_config (" +
                                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                                    "platform VARCHAR(20) NOT NULL UNIQUE, " +
                                    "min_version_code INT NOT NULL DEFAULT 1, " +
                                    "min_version_name VARCHAR(20) NOT NULL DEFAULT '1.0', " +
                                    "latest_version_code INT NOT NULL DEFAULT 1, " +
                                    "latest_version_name VARCHAR(20) NOT NULL DEFAULT '1.0', " +
                                    "is_force_update BOOLEAN NOT NULL DEFAULT FALSE, " +
                                    "update_title VARCHAR(150) NOT NULL DEFAULT 'App Update Required', " +
                                    "update_message TEXT NOT NULL, " +
                                    "update_url VARCHAR(500) NULL, " +
                                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");
                            stmt.executeUpdate("INSERT IGNORE INTO app_version_config (id, platform, min_version_code, min_version_name, latest_version_code, latest_version_name, is_force_update, update_title, update_message, update_url) VALUES " +
                                    "(1, 'android', 1, '1.0', 1, '1.0', FALSE, 'App Update Required', 'A new version of AppEtite is available with essential menu availability and ordering updates. Please update to continue.', 'https://github.com/StarShree/AppEtiteApp/releases')");
                        } catch (Exception verEx) {
                            Log.d(TAG, "App version table note: " + verEx.getMessage());
                        }

                        // Seed menu items for College 2 and College 3 if they don't exist yet
                        try (Statement stmt = conn.createStatement()) {
                            stmt.executeUpdate("INSERT IGNORE INTO menu_items (id, college_id, canteen_id, name, description, price, category, image_url, is_available, is_veg) VALUES " +
                                    "('item_09', 'col_2', 'canteen_2_1', 'Classic Belgian Waffles', 'Golden malted waffles topped with maple syrup & berries', 95.00, 'Breakfast', 'https://images.unsplash.com/photo-1562376552-0d160a2f238d?w=500', 1, 1), " +
                                    "('item_10', 'col_2', 'canteen_2_1', 'Gourmet Veggie Burger', 'Herb-roasted portobello & black bean patty with aged cheddar', 130.00, 'Lunch', 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500', 1, 1), " +
                                    "('item_11', 'col_2', 'canteen_2_1', 'Iced Mango Passion Cooler', 'Fresh Alphonso mango puree with passion fruit & mint leaves', 80.00, 'Beverages', 'https://images.unsplash.com/photo-1505252585461-04db1eb84625?w=500', 1, 1), " +
                                    "('item_12', 'col_2', 'canteen_2_1', 'Loaded Cheese Nachos', 'Crisp tortilla chips topped with melted jalapeño queso & pico de gallo', 110.00, 'Snacks', 'https://images.unsplash.com/photo-1513456852971-30c0b8199d4d?w=500', 1, 1), " +
                                    "('item_13', 'col_3', 'canteen_3_1', 'Crispy Truffle Chicken Burger', 'Panko-crusted chicken fillet with melted Monterey Jack, truffle aioli on toasted brioche', 145.00, 'Lunch', 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500', 1, 0), " +
                                    "('item_14', 'col_3', 'canteen_3_1', 'Iced Taro Brown Sugar Boba', 'Velvety taro milk tea layered with slow-cooked brown sugar boba pearls', 95.00, 'Beverages', 'https://images.unsplash.com/photo-1558857563-b37cf05d8a58?w=500', 1, 1), " +
                                    "('item_15', 'col_3', 'canteen_3_1', 'Supreme Cheesy Sourdough Pizza', 'Hand-tossed crust with mozzarella, smoked provolone, roasted garlic & basil', 180.00, 'Lunch', 'https://images.unsplash.com/photo-1604382354936-07c5d9983bd3?w=500', 1, 1), " +
                                    "('item_16', 'col_3', 'canteen_3_1', 'Golden Seasoned Curly Fries', 'Spiral-cut potatoes dusted in paprika, garlic herbs & house fry dip', 75.00, 'Snacks', 'https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=500', 1, 1)");
                        } catch (Exception itemEx) {
                            Log.d(TAG, "Seed menu items note: " + itemEx.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                Log.i(TAG, "Cloud SQL direct connection status: " + e.getMessage() + ". Local store active.");
            }
        });
    }
}
