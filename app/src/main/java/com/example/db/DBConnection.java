package com.example.db;

import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Thread-safe Singleton managing JDBC connections to Google Cloud SQL (MySQL 8.0).
 * Host: 34.100.184.249 | Database: AppEtiteDB
 */
public class DBConnection {
    private static final String TAG = "DBConnection";

    // Google Cloud SQL Configuration
    private static final String HOST = "34.100.184.249";
    private static final String PORT = "3306";
    private static final String DATABASE = "AppEtiteDB";
    private static final String USER = "root";
    private static final String PASSWORD = "AppEtite123!";

    // JDBC Connection URL with required flags for Cloud SQL MySQL 8.0
    private static final String JDBC_URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
            + "?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=6000&socketTimeout=10000&autoReconnect=true";

    private static volatile DBConnection instance;
    private Connection connection;

    private DBConnection() {
        registerDriver();
    }

    /**
     * Attempts to register modern or standard MySQL / MariaDB JDBC drivers for Android.
     */
    private void registerDriver() {
        try {
            // Try MariaDB driver first (lightweight, zero Java EE dependencies on Android)
            Class.forName("org.mariadb.jdbc.Driver");
            Log.d(TAG, "Loaded MariaDB JDBC Driver");
        } catch (ClassNotFoundException e1) {
            try {
                // Fallback to MySQL Connector/J driver
                Class.forName("com.mysql.jdbc.Driver");
                Log.d(TAG, "Loaded MySQL JDBC Driver");
            } catch (ClassNotFoundException e2) {
                Log.e(TAG, "Could not load JDBC driver classes: " + e2.getMessage());
            }
        }
    }

    public static DBConnection getInstance() {
        if (instance == null) {
            synchronized (DBConnection.class) {
                if (instance == null) {
                    instance = new DBConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Retrieves an active Connection to Google Cloud SQL.
     * Reconnects automatically if previous connection timed out or was closed.
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            Log.d(TAG, "Opening new JDBC Connection to Cloud SQL: " + HOST);
            DriverManager.setLoginTimeout(6);
            connection = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
            Log.d(TAG, "Successfully connected to Cloud SQL database: " + DATABASE);
        }
        return connection;
    }

    /**
     * Checks if current connection is alive.
     */
    public synchronized boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Safely closes the current connection.
     */
    public synchronized void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                Log.d(TAG, "JDBC Connection closed.");
            } catch (SQLException e) {
                Log.e(TAG, "Error closing connection: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }

    public static String getHost() {
        return HOST;
    }

    public static String getDatabaseName() {
        return DATABASE;
    }
}
