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
                    }
                }
            } catch (Exception e) {
                Log.i(TAG, "Cloud SQL direct connection status: " + e.getMessage() + ". Local store active.");
            }
        });
    }
}
