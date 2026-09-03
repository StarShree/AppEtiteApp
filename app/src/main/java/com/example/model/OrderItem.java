package com.example.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class OrderItem implements Serializable {
    private int orderItemId;
    private int orderId;
    private int itemId;
    private int quantity;
    private BigDecimal price;

    // Transient presentation fields
    private String itemName;
    private String itemCategory;

    public OrderItem() {
        this.quantity = 1;
        this.price = BigDecimal.ZERO;
    }

    public OrderItem(int orderItemId, int orderId, int itemId, int quantity, BigDecimal price) {
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.price = price != null ? price : BigDecimal.ZERO;
    }

    public int getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(int orderItemId) {
        this.orderItemId = orderItemId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemCategory() {
        return itemCategory;
    }

    public void setItemCategory(String itemCategory) {
        this.itemCategory = itemCategory;
    }

    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
