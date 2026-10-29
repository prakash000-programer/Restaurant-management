package com.rest.models;

/**
 * Represents a single line-item within an order.
 * Links an Order to a specific MenuItem along with quantity and subtotal.
 */
public class OrderItem {

    private int    orderItemId;
    private int    orderId;       // which order this item belongs to
    private int    menuItemId;    // which menu item was ordered
    private String menuItemName;  // stored for easy display (snapshot at order time)
    private int    quantity;
    private double unitPrice;     // price at the time of ordering

    public OrderItem(int orderItemId, int orderId, int menuItemId,
                     String menuItemName, int quantity, double unitPrice) {
        this.orderItemId  = orderItemId;
        this.orderId      = orderId;
        this.menuItemId   = menuItemId;
        this.menuItemName = menuItemName;
        this.quantity     = quantity;
        this.unitPrice    = unitPrice;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int getOrderItemId() {
        return orderItemId;
    }

    public int getOrderId() {
        return orderId;
    }

    public int getMenuItemId() {
        return menuItemId;
    }

    public String getMenuItemName() {
        return menuItemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    /** Calculated field: quantity × unitPrice */
    public double getSubTotal() {
        return quantity * unitPrice;
    }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return String.format("[Item#%d] %-20s x%d  @$%.2f  = $%.2f",
                orderItemId, menuItemName, quantity, unitPrice, getSubTotal());
    }
}
