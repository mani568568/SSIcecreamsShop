package com.ssicecreamsshop.model;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Order {
    private String orderId;
    private LocalDateTime createdDateTime;
    private List<OrderItem> orderItems;
    private double orderTotalAmount; // Sum of all totalItemPrice in this order
    // dailyIncrementalTotal will be handled by OrderExcelUtil when writing to Excel

    public Order(List<OrderItem> orderItems) {
        this.orderId = UUID.randomUUID().toString(); // Generate a unique ID
        this.createdDateTime = LocalDateTime.now();
        this.orderItems = orderItems;
        this.orderTotalAmount = calculateOrderTotal();
    }

    // Constructor for loading from Excel (where ID and timestamp are already set)
    public Order(String orderId, LocalDateTime createdDateTime, List<OrderItem> orderItems) {
        this.orderId = orderId;
        this.createdDateTime = createdDateTime;
        this.orderItems = orderItems;
        this.orderTotalAmount = calculateOrderTotal();
    }


    private double calculateOrderTotal() {
        if (orderItems == null) {
            return 0.0;
        }
        return orderItems.stream().mapToDouble(OrderItem::getTotalItemPrice).sum();
    }

    // Getters
    public String getOrderId() {
        return orderId;
    }

    public LocalDateTime getCreatedDateTime() {
        return createdDateTime;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public double getOrderTotalAmount() {
        return orderTotalAmount;
    }

    // Setters (generally, order details are immutable after creation, but provided if needed)
    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
        this.orderTotalAmount = calculateOrderTotal(); // Recalculate
    }
}
