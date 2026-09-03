package com.example.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class MenuItem implements Serializable {
    private int itemId;
    private String idString;
    private int collegeId;
    private String canteenId;
    private String name;
    private String description;
    private BigDecimal price;
    private String category; // 'Breakfast', 'Lunch', 'Beverages', 'Snacks'
    private String imageUrl;
    private boolean isAvailable;

    public MenuItem() {
        this.price = BigDecimal.ZERO;
        this.isAvailable = true;
    }

    public MenuItem(int itemId, int collegeId, String name, String description, BigDecimal price, String category, String imageUrl, boolean isAvailable) {
        this(String.format("item_%02d", itemId), itemId, collegeId, name, description, price, category, imageUrl, isAvailable);
    }

    public MenuItem(String idString, int itemId, int collegeId, String name, String description, BigDecimal price, String category, String imageUrl, boolean isAvailable) {
        this.idString = idString;
        this.itemId = itemId;
        this.collegeId = collegeId;
        this.name = name;
        this.description = description;
        this.price = price != null ? price : BigDecimal.ZERO;
        this.category = category;
        this.imageUrl = imageUrl;
        this.isAvailable = isAvailable;
    }

    public String getIdString() {
        if (idString == null && itemId > 0) {
            return String.format("item_%02d", itemId);
        }
        return idString;
    }

    public void setIdString(String idString) {
        this.idString = idString;
    }

    public String getCanteenId() {
        return canteenId;
    }

    public void setCanteenId(String canteenId) {
        this.canteenId = canteenId;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getCollegeId() {
        return collegeId;
    }

    public void setCollegeId(int collegeId) {
        this.collegeId = collegeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }
}
