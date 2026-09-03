package com.example.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing an order, fully aligned with Cloud SQL schema:
 * Table: orders (
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
 *   payment_method ENUM(...),
 *   payment_status ENUM(...),
 *   order_status ENUM('PLACED','ACCEPTED','PREPARING','READY_FOR_PICKUP','COMPLETED','CANCELLED'),
 *   order_timestamp BIGINT NOT NULL,
 *   estimated_ready_timestamp BIGINT NOT NULL,
 *   pickup_otp VARCHAR(10) DEFAULT '4821',
 *   special_instructions VARCHAR(255),
 *   created_at TIMESTAMP,
 *   updated_at TIMESTAMP
 * )
 */
public class Order implements Serializable {
    private int orderId;
    private String idString;
    private String orderNumber;
    private int tokenNumber = 101;
    private String tokenString = "T-101";
    private String pickupOtp = "4821"; // 4-digit pickup PIN

    private int userId;
    private String userIdString;
    private String userName = "Alex Johnson";

    private int collegeId = 1;
    private String collegeIdString = "STAN";
    private String collegeName = "Stanford University Campus";

    private String canteenId = "canteen_1";
    private String canteenName = "Byte Bites Canteen";

    private String itemsSummary = "";
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal finalAmount = BigDecimal.ZERO;

    private String paymentMethod = "STUDENT_WALLET";
    private String paymentStatus = "SUCCESS";
    private String orderStatus = "PLACED"; // 'PLACED', 'PREPARING', 'READY_FOR_PICKUP', 'COMPLETED', 'CANCELLED'

    private long orderTimestamp = System.currentTimeMillis();
    private long estimatedReadyTimestamp = System.currentTimeMillis() + (15 * 60 * 1000);
    private String specialInstructions = "";
    private Timestamp createdAt;

    private List<OrderItem> items = new ArrayList<>();

    public Order() {
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    public Order(int orderId, int userId, int collegeId, BigDecimal totalAmount, int tokenNumber, String orderStatus, Timestamp createdAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.collegeId = collegeId;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.finalAmount = this.totalAmount;
        this.tokenNumber = tokenNumber;
        this.tokenString = "T-" + tokenNumber;
        this.orderStatus = normalizeStatus(orderStatus);
        this.createdAt = createdAt;
        this.orderNumber = "ORD-" + (orderId > 0 ? orderId : System.currentTimeMillis() % 10000);
        this.idString = "ord_" + (orderId > 0 ? orderId : System.currentTimeMillis());
    }

    private String normalizeStatus(String st) {
        if (st == null) return "PLACED";
        String s = st.trim().toUpperCase();
        if (s.equals("READY")) return "READY_FOR_PICKUP";
        if (s.equals("READY_FOR_PICKUP")) return "READY_FOR_PICKUP";
        if (s.equals("PREPARING")) return "PREPARING";
        if (s.equals("ACCEPTED")) return "PREPARING";
        if (s.equals("COMPLETED")) return "COMPLETED";
        if (s.equals("CANCELLED")) return "CANCELLED";
        return "PLACED";
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
        if (orderNumber == null || orderNumber.isEmpty()) {
            this.orderNumber = "ORD-" + orderId;
        }
    }

    public String getIdString() {
        if (idString != null && !idString.isEmpty()) return idString;
        return "ord_" + (orderId > 0 ? orderId : System.currentTimeMillis());
    }

    public void setIdString(String idString) {
        this.idString = idString;
    }

    public String getOrderNumber() {
        if (orderNumber != null && !orderNumber.isEmpty()) return orderNumber;
        return "ORD-" + (orderId > 0 ? orderId : (System.currentTimeMillis() % 10000));
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public int getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(int tokenNumber) {
        this.tokenNumber = tokenNumber;
        this.tokenString = "T-" + tokenNumber;
    }

    public String getTokenString() {
        if (tokenString != null && !tokenString.isEmpty()) return tokenString;
        return "T-" + tokenNumber;
    }

    public void setTokenString(String tokenString) {
        this.tokenString = tokenString;
        try {
            if (tokenString != null && tokenString.startsWith("T-")) {
                this.tokenNumber = Integer.parseInt(tokenString.substring(2));
            } else if (tokenString != null) {
                this.tokenNumber = Integer.parseInt(tokenString);
            }
        } catch (NumberFormatException ignored) {}
    }

    public String getPickupOtp() {
        if (pickupOtp != null && !pickupOtp.isEmpty()) return pickupOtp;
        return "4821";
    }

    public void setPickupOtp(String pickupOtp) {
        this.pickupOtp = pickupOtp;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserIdString() {
        if (userIdString != null && !userIdString.isEmpty()) return userIdString;
        return "user_" + userId;
    }

    public void setUserIdString(String userIdString) {
        this.userIdString = userIdString;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getCollegeId() {
        return collegeId;
    }

    public void setCollegeId(int collegeId) {
        this.collegeId = collegeId;
    }

    public String getCollegeIdString() {
        if (collegeIdString != null && !collegeIdString.isEmpty()) return collegeIdString;
        return "col_" + collegeId;
    }

    public void setCollegeIdString(String collegeIdString) {
        this.collegeIdString = collegeIdString;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
    }

    public String getCanteenId() {
        return canteenId;
    }

    public void setCanteenId(String canteenId) {
        this.canteenId = canteenId;
    }

    public String getCanteenName() {
        return canteenName;
    }

    public void setCanteenName(String canteenName) {
        this.canteenName = canteenName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
        if (this.finalAmount == null || this.finalAmount.compareTo(BigDecimal.ZERO) == 0) {
            this.finalAmount = totalAmount;
        }
    }

    public BigDecimal getDiscount() {
        return discount != null ? discount : BigDecimal.ZERO;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount != null ? finalAmount : getTotalAmount();
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod != null ? paymentMethod : "STUDENT_WALLET";
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus != null ? paymentStatus : "SUCCESS";
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getOrderStatus() {
        return orderStatus != null ? orderStatus : "PLACED";
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = normalizeStatus(orderStatus);
    }

    public long getOrderTimestamp() {
        return orderTimestamp;
    }

    public void setOrderTimestamp(long orderTimestamp) {
        this.orderTimestamp = orderTimestamp;
    }

    public long getEstimatedReadyTimestamp() {
        return estimatedReadyTimestamp;
    }

    public void setEstimatedReadyTimestamp(long estimatedReadyTimestamp) {
        this.estimatedReadyTimestamp = estimatedReadyTimestamp;
    }

    public String getSpecialInstructions() {
        return specialInstructions;
    }

    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public String getItemsSummary() {
        if (itemsSummary != null && !itemsSummary.isEmpty()) {
            return itemsSummary;
        }
        if (items == null || items.isEmpty()) {
            return "No items";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            OrderItem item = items.get(i);
            sb.append(item.getQuantity()).append("x ").append(item.getItemName() != null ? item.getItemName() : "Item #" + item.getItemId());
            if (i < items.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public void setItemsSummary(String itemsSummary) {
        this.itemsSummary = itemsSummary;
    }
}
