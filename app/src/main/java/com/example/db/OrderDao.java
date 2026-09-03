package com.example.db;

import android.util.Log;

import com.example.model.Order;
import com.example.model.OrderItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for handling live campus food orders.
 * Fully integrated with Google Cloud SQL AppEtiteDB `orders` table:
 * Table columns:
 *   id VARCHAR(64) PRIMARY KEY,
 *   order_number VARCHAR(32) NOT NULL,
 *   token_number VARCHAR(32) NOT NULL,
 *   user_id VARCHAR(64) NOT NULL,
 *   user_name VARCHAR(120) NOT NULL,
 *   college_id VARCHAR(64) NOT NULL,
 *   college_name VARCHAR(150) NOT NULL,
 *   canteen_id VARCHAR(64) NOT NULL,
 *   canteen_name VARCHAR(120) NOT NULL,
 *   items_summary TEXT NOT NULL,
 *   total_amount DECIMAL(10,2) NOT NULL,
 *   discount DECIMAL(10,2) DEFAULT 0.00,
 *   final_amount DECIMAL(10,2) NOT NULL,
 *   payment_method ENUM('STUDENT_WALLET','CAMPUS_CARD','UPI_QR','CREDIT_DEBIT_CARD','NET_BANKING','CASH_AT_COUNTER'),
 *   payment_status ENUM('INITIATED','PENDING','SUCCESS','FAILED','REFUNDED'),
 *   order_status ENUM('PLACED','ACCEPTED','PREPARING','READY_FOR_PICKUP','COMPLETED','CANCELLED'),
 *   order_timestamp BIGINT NOT NULL,
 *   estimated_ready_timestamp BIGINT NOT NULL,
 *   pickup_otp VARCHAR(10) DEFAULT '4821',
 *   special_instructions VARCHAR(255),
 *   created_at TIMESTAMP,
 *   updated_at TIMESTAMP
 */
public class OrderDao {
    private static final String TAG = "OrderDao";

    public String getNextTokenString(String collegeIdStr) {
        String sql = "SELECT token_number FROM orders WHERE college_id = ? ORDER BY created_at DESC LIMIT 1";
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, com.example.model.College.toShortCode(collegeIdStr, null, null));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String lastToken = rs.getString("token_number");
                        if (lastToken != null) {
                            String numPart = lastToken.replaceAll("\\D+", "");
                            if (!numPart.isEmpty()) {
                                int n = Integer.parseInt(numPart) + 1;
                                return "T-" + n;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed reading last token from Cloud SQL: " + e.getMessage());
        }
        int rand = (int) (System.currentTimeMillis() % 900) + 101;
        return "T-" + rand;
    }

    public static String generate4DigitPin() {
        int pin = (int) (Math.random() * 9000) + 1000;
        return String.valueOf(pin);
    }

    public int createOrder(Order order, List<OrderItem> items) {
        if (order == null) return -1;

        // Generate Token and PIN if not already set
        if (order.getTokenString() == null || order.getTokenString().isEmpty() || "T-101".equals(order.getTokenString())) {
            String nextToken = getNextTokenString(order.getCollegeIdString());
            order.setTokenString(nextToken);
        }
        if (order.getPickupOtp() == null || order.getPickupOtp().isEmpty() || "4821".equals(order.getPickupOtp())) {
            order.setPickupOtp(generate4DigitPin());
        }

        long now = System.currentTimeMillis();
        order.setOrderTimestamp(now);
        order.setEstimatedReadyTimestamp(now + (15 * 60 * 1000)); // 15 mins cooking estimation

        String idStr = "ord_" + now + "_" + ((int) (Math.random() * 900) + 100);
        order.setIdString(idStr);

        String orderNum = "ORD-" + ((int) (Math.random() * 9000) + 1000);
        order.setOrderNumber(orderNum);

        // Build items summary
        if (items != null && !items.isEmpty()) {
            order.setItems(items);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < items.size(); i++) {
                OrderItem oi = items.get(i);
                sb.append(oi.getQuantity()).append("x ").append(oi.getItemName() != null ? oi.getItemName() : "Item #" + oi.getItemId());
                if (i < items.size() - 1) sb.append(", ");
            }
            order.setItemsSummary(sb.toString());
        }

        String insertOrderSql = "INSERT INTO orders (" +
                "id, order_number, token_number, user_id, user_name, " +
                "college_id, college_name, canteen_id, canteen_name, " +
                "items_summary, total_amount, discount, final_amount, " +
                "payment_method, payment_status, order_status, " +
                "order_timestamp, estimated_ready_timestamp, pickup_otp, special_instructions" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        boolean successInCloud = false;

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(insertOrderSql)) {
                stmt.setString(1, order.getIdString());
                stmt.setString(2, order.getOrderNumber());
                stmt.setString(3, order.getTokenString());
                stmt.setString(4, order.getUserIdString());
                stmt.setString(5, order.getUserName() != null ? order.getUserName() : "Student");
                stmt.setString(6, order.getCollegeIdString());
                stmt.setString(7, order.getCollegeName() != null ? order.getCollegeName() : "Campus Canteen");
                stmt.setString(8, order.getCanteenId() != null ? order.getCanteenId() : "canteen_1");
                stmt.setString(9, order.getCanteenName() != null ? order.getCanteenName() : "Byte Bites Canteen");
                stmt.setString(10, order.getItemsSummary() != null ? order.getItemsSummary() : "Meal order");
                stmt.setBigDecimal(11, order.getTotalAmount());
                stmt.setBigDecimal(12, order.getDiscount() != null ? order.getDiscount() : BigDecimal.ZERO);
                stmt.setBigDecimal(13, order.getFinalAmount() != null ? order.getFinalAmount() : order.getTotalAmount());
                stmt.setString(14, order.getPaymentMethod() != null ? order.getPaymentMethod() : "STUDENT_WALLET");
                stmt.setString(15, order.getPaymentStatus() != null ? order.getPaymentStatus() : "SUCCESS");
                stmt.setString(16, order.getOrderStatus() != null ? order.getOrderStatus() : "PLACED");
                stmt.setLong(17, order.getOrderTimestamp());
                stmt.setLong(18, order.getEstimatedReadyTimestamp());
                stmt.setString(19, order.getPickupOtp());
                stmt.setString(20, order.getSpecialInstructions() != null ? order.getSpecialInstructions() : "");

                int affected = stmt.executeUpdate();
                if (affected > 0) {
                    successInCloud = true;
                    Log.d(TAG, "Order successfully saved to Cloud SQL! ID: " + idStr + " | Token: " + order.getTokenString() + " | PIN: " + order.getPickupOtp());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed inserting order into Cloud SQL: " + e.getMessage(), e);
        }

        // Keep local cache in sync
        int localId = LocalDataStore.getInstance().createOrder(order, items);
        try {
            String numOnly = order.getOrderNumber().replaceAll("\\D+", "");
            if (!numPartEmpty(numOnly)) {
                order.setOrderId(Integer.parseInt(numOnly));
            } else {
                order.setOrderId(localId);
            }
        } catch (Exception ignored) {
            order.setOrderId(localId);
        }

        return successInCloud ? order.getOrderId() : localId;
    }

    private static boolean numPartEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    public List<Order> getCustomerOrders(int userId, String userIdStr) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE user_id = ? OR user_id = ? OR user_id = ? ORDER BY order_timestamp DESC, created_at DESC";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, userIdStr != null ? userIdStr : "user_" + userId);
                stmt.setString(2, "user_" + userId);
                stmt.setString(3, String.valueOf(userId));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Order o = mapOrderFromResultSet(rs);
                        orders.add(o);
                    }
                    if (!orders.isEmpty()) {
                        // Sync into local data store as well
                        for (Order o : orders) {
                            try {
                                LocalDataStore.getInstance().updateOrderStatus(o.getOrderId(), o.getOrderStatus());
                            } catch (Exception ignored) {}
                        }
                        return orders;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL getCustomerOrders failed: " + e.getMessage());
        }

        return LocalDataStore.getInstance().getCustomerOrders(userId);
    }

    public List<Order> getCustomerOrders(int userId) {
        return getCustomerOrders(userId, "user_" + userId);
    }

    public List<Order> getOrdersByCollege(int collegeId) {
        return getOrdersByCollegeAndStatus(collegeId, null);
    }

    public List<Order> getOrdersByCanteen(String canteenId, int collegeId) {
        return getOrdersByCanteenAndStatus(canteenId, collegeId, null);
    }

    public List<Order> getOrdersByCanteenAndStatus(String canteenId, int collegeId, String status) {
        List<Order> orders = new ArrayList<>();
        boolean hasStatus = status != null && !status.equalsIgnoreCase("ALL");
        boolean hasCanteen = canteenId != null && !canteenId.trim().isEmpty();

        String shortCode = com.example.model.College.toShortCode(null, collegeId, null);
        StringBuilder sql = new StringBuilder("SELECT * FROM orders WHERE (college_id = ? OR college_id = ? OR college_id = ?)");
        if (hasCanteen) {
            sql.append(" AND (canteen_id = ? OR canteen_id = ? OR canteen_name = ?)");
        }
        if (hasStatus) {
            sql.append(" AND order_status = ?");
        }
        sql.append(" ORDER BY order_timestamp DESC");

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            if (conn != null && !conn.isClosed()) {
                try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                    int paramIdx = 1;
                    stmt.setString(paramIdx++, shortCode);
                    stmt.setString(paramIdx++, "col_" + collegeId);
                    stmt.setString(paramIdx++, String.valueOf(collegeId));
                    if (hasCanteen) {
                        stmt.setString(paramIdx++, canteenId);
                        stmt.setString(paramIdx++, canteenId.replace("canteen_", "cant_"));
                        stmt.setString(paramIdx++, canteenId);
                    }
                    if (hasStatus) {
                        stmt.setString(paramIdx++, toDbStatus(status));
                    }
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            orders.add(mapOrderFromResultSet(rs));
                        }
                        if (!orders.isEmpty()) {
                            return orders;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL getOrdersByCanteenAndStatus failed: " + e.getMessage());
        }

        return LocalDataStore.getInstance().getOrdersByCanteenAndStatus(canteenId, collegeId, status);
    }

    public List<Order> getOrdersByCollegeAndStatus(int collegeId, String status) {
        List<Order> orders = new ArrayList<>();
        boolean hasStatus = status != null && !status.equalsIgnoreCase("ALL");
        String shortCode = com.example.model.College.toShortCode(null, collegeId, null);
        String sql = "SELECT * FROM orders WHERE (college_id = ? OR college_id = ? OR college_id = ?)"
                + (hasStatus ? " AND order_status = ?" : "")
                + " ORDER BY order_timestamp DESC";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, shortCode);
                stmt.setString(2, "col_" + collegeId);
                stmt.setString(3, String.valueOf(collegeId));
                if (hasStatus) {
                    stmt.setString(4, toDbStatus(status));
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Order o = mapOrderFromResultSet(rs);
                        orders.add(o);
                    }
                    if (!orders.isEmpty()) {
                        return orders;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL getOrdersByCollegeAndStatus failed: " + e.getMessage());
        }

        return LocalDataStore.getInstance().getOrdersByCollegeAndStatus(collegeId, status);
    }

    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY order_timestamp DESC";

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapOrderFromResultSet(rs));
                }
                if (!orders.isEmpty()) {
                    return orders;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL getAllOrders failed: " + e.getMessage());
        }

        return LocalDataStore.getInstance().getAllOrders();
    }

    public boolean updateOrderStatus(String idString, String orderNumber, String tokenNumber, String newStatus) {
        String sql = "UPDATE orders SET order_status = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE id = ? OR order_number = ? OR order_number = ? OR token_number = ? OR token_number = ?";
        boolean successInCloud = false;
        String dbStatus = toDbStatus(newStatus);

        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, dbStatus);
                stmt.setString(2, idString != null ? idString : "");
                stmt.setString(3, orderNumber != null ? orderNumber : "");
                stmt.setString(4, orderNumber != null ? ("ORD-" + orderNumber.replace("ORD-", "")) : "");
                stmt.setString(5, tokenNumber != null ? tokenNumber : "");
                stmt.setString(6, tokenNumber != null ? ("T-" + tokenNumber.replaceAll("[^0-9]", "")) : "");
                int affected = stmt.executeUpdate();
                successInCloud = affected > 0;
                Log.d(TAG, "updateOrderStatus executed in Cloud SQL: affected=" + affected + ", status=" + dbStatus + " for token=" + tokenNumber + ", ordNum=" + orderNumber);
            }
        } catch (Exception e) {
            Log.w(TAG, "Cloud SQL updateOrderStatus failed: " + e.getMessage());
        }

        // Keep local cache in sync
        try {
            String candidate = orderNumber != null ? orderNumber : (idString != null ? idString : tokenNumber);
            if (candidate != null) {
                int num = Integer.parseInt(candidate.replaceAll("\\D+", ""));
                LocalDataStore.getInstance().updateOrderStatus(num, newStatus);
            }
        } catch (Exception ignored) {}

        return successInCloud;
    }

    public boolean updateOrderStatus(String orderIdOrNumber, String newStatus) {
        return updateOrderStatus(orderIdOrNumber, orderIdOrNumber, orderIdOrNumber, newStatus);
    }

    public boolean updateOrderStatus(int orderId, String newStatus) {
        return updateOrderStatus(String.valueOf(orderId), "ORD-" + orderId, "T-" + orderId, newStatus);
    }

    private Order mapOrderFromResultSet(ResultSet rs) throws Exception {
        Order o = new Order();
        o.setIdString(rs.getString("id"));
        o.setOrderNumber(rs.getString("order_number"));
        o.setTokenString(rs.getString("token_number"));
        o.setPickupOtp(rs.getString("pickup_otp"));
        o.setUserIdString(rs.getString("user_id"));
        o.setUserName(rs.getString("user_name"));
        o.setCollegeIdString(rs.getString("college_id"));
        o.setCollegeName(rs.getString("college_name"));
        o.setCanteenId(rs.getString("canteen_id"));
        o.setCanteenName(rs.getString("canteen_name"));
        o.setItemsSummary(rs.getString("items_summary"));
        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        o.setDiscount(rs.getBigDecimal("discount"));
        o.setFinalAmount(rs.getBigDecimal("final_amount"));
        o.setPaymentMethod(rs.getString("payment_method"));
        o.setPaymentStatus(rs.getString("payment_status"));
        o.setOrderStatus(rs.getString("order_status"));
        o.setOrderTimestamp(rs.getLong("order_timestamp"));
        o.setEstimatedReadyTimestamp(rs.getLong("estimated_ready_timestamp"));
        o.setSpecialInstructions(rs.getString("special_instructions"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            o.setCreatedAt(ts);
        } else if (o.getOrderTimestamp() > 0) {
            o.setCreatedAt(new Timestamp(o.getOrderTimestamp()));
        }

        try {
            String numOnly = o.getOrderNumber().replaceAll("\\D+", "");
            if (!numOnly.isEmpty()) {
                o.setOrderId(Integer.parseInt(numOnly));
            }
        } catch (Exception ignored) {}

        return o;
    }

    public static String toDbStatus(String s) {
        if (s == null) return "PLACED";
        String u = s.trim().toUpperCase();
        if (u.equals("ACCEPTED")) return "ACCEPTED";
        if (u.equals("PREPARING") || u.equals("COOKING")) return "PREPARING";
        if (u.equals("READY") || u.equals("READY_FOR_PICKUP")) return "READY_FOR_PICKUP";
        if (u.equals("COMPLETED") || u.equals("PICKED_UP") || u.equals("DONE")) return "COMPLETED";
        if (u.equals("CANCELLED")) return "CANCELLED";
        return "PLACED";
    }
}
