package com.ssicecreamsshop.model;

public class OrderItem {
    private String itemName;
    private int quantity;
    private double unitPrice;
    private double totalItemPrice;

    public OrderItem(String itemName, int quantity, double unitPrice) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalItemPrice = quantity * unitPrice;
    }

    // Getters
    public String getItemName() {
        return itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getTotalItemPrice() {
        return totalItemPrice;
    }

    // Setters (if needed, though typically calculated or set at construction)
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.totalItemPrice = this.quantity * this.unitPrice; // Recalculate if quantity changes
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
        this.totalItemPrice = this.quantity * this.unitPrice; // Recalculate if price changes
    }
}
