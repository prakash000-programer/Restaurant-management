package com.rest.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a customer order placed at a specific table.
 * An order can contain multiple OrderItems and tracks its lifecycle status.
 */
public class Orders {

    public enum Status {
        PENDING,
        PREPARING,
        READY,
        COMPLETED,
        CANCELLED
    }

    private int           orderId;
    private int           tableId;
    private Status        status;
    private LocalDateTime createdAt;
    private List<OrderItem> items;   // all items belonging to this order

    public Orders(int orderId, int tableId) {
        this.orderId   = orderId;
        this.tableId   = tableId;
        this.status    = Status.PENDING;
        this.createdAt = LocalDateTime.now();
        this.items     = new ArrayList<>();
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int getOrderId() {
        return orderId;
    }

    public int getTableId() {
        return tableId;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    // ── Business methods ─────────────────────────────────────────────────────

    /** Adds an OrderItem to this order. */
    public void addItem(OrderItem item) {
        items.add(item);
    }

    /** Updates the status of the order. */
    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        String timeStr = createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return String.format("Order #%d | Table: %d | Status: %-10s | Items: %d | Created: %s",
                orderId, tableId, status, items.size(), timeStr);
    }
}