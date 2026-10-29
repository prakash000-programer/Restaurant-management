package com.rest.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a Bill generated for an Order.
 * Calculates the gross subtotal from order items, applies discounts if available,
 * and maintains the final net total amount to be settled.
 */
public class Bill {

    public enum Status {
        UNPAID,
        PAID,
        CANCELLED
    }

    private int           billId;
    private Orders        order;
    private int           orderId;
    private double        subTotal;           // Gross sum across all order items
    private double        discountPercentage; // Discount percentage (e.g. 10.0 for 10%)
    private double        discountAmount;     // Actual dollar amount subtracted
    private double        totalAmount;        // Net amount due (subTotal - discountAmount)
    private Status        status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    /**
     * Creates a bill for an order with no discount.
     *
     * @param billId unique identifier for this bill
     * @param order  the order being billed
     */
    public Bill(int billId, Orders order) {
        this(billId, order, 0.0);
    }

    /**
     * Creates a bill for an order with a discount percentage.
     *
     * @param billId             unique identifier for this bill
     * @param order              the order being billed
     * @param discountPercentage discount percentage (0 to 100)
     */
    public Bill(int billId, Orders order, double discountPercentage) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null for bill generation.");
        }
        if (discountPercentage < 0 || discountPercentage > 100) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100.");
        }

        this.billId             = billId;
        this.order              = order;
        this.orderId            = order.getOrderId();
        this.discountPercentage = discountPercentage;
        this.status             = Status.UNPAID;
        this.createdAt          = LocalDateTime.now();
        this.paidAt             = null;

        calculateBill();
    }

    /**
     * Recalculates the subtotal from order items and applies discounts.
     */
    public void calculateBill() {
        double sum = 0.0;
        if (order != null && order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                sum += item.getSubTotal();
            }
        }
        this.subTotal = sum;
        this.discountAmount = (this.subTotal * this.discountPercentage) / 100.0;
        this.totalAmount = Math.max(0.0, this.subTotal - this.discountAmount);
    }

    /**
     * Updates the discount percentage and recalculates totals.
     */
    public void applyDiscountPercentage(double percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100.");
        }
        this.discountPercentage = percentage;
        calculateBill();
    }

    /**
     * Applies a flat discount amount and recalculates totals.
     */
    public void applyFlatDiscountAmount(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Discount amount cannot be negative.");
        }
        double sum = 0.0;
        if (order != null && order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                sum += item.getSubTotal();
            }
        }
        this.subTotal = sum;
        this.discountAmount = Math.min(amount, this.subTotal);
        this.discountPercentage = (this.subTotal > 0) ? (this.discountAmount / this.subTotal) * 100.0 : 0.0;
        this.totalAmount = Math.max(0.0, this.subTotal - this.discountAmount);
    }

    /** Marks the bill as settled/PAID. */
    public void markPaid() {
        this.status = Status.PAID;
        this.paidAt = LocalDateTime.now();
    }

    /** Marks the bill as UNPAID (e.g. after a refund). */
    public void markUnpaid() {
        this.status = Status.UNPAID;
        this.paidAt = null;
    }

    /** Marks the bill as CANCELLED. */
    public void markCancelled() {
        this.status = Status.CANCELLED;
    }

    public boolean isPaid() {
        return this.status == Status.PAID;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int getBillId() {
        return billId;
    }

    public Orders getOrder() {
        return order;
    }

    public int getOrderId() {
        return (order != null) ? order.getOrderId() : orderId;
    }

    public double getSubTotal() {
        return subTotal;
    }

    public double getDiscountPercentage() {
        return discountPercentage;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    @Override
    public String toString() {
        String timeStr = createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return String.format(
                "Bill #%d | Order #%d | Subtotal: $%.2f | Discount: $%.2f (%.1f%%) | Total: $%.2f | Status: %-8s | Created: %s",
                billId, getOrderId(), subTotal, discountAmount, discountPercentage, totalAmount, status, timeStr
        );
    }
}
