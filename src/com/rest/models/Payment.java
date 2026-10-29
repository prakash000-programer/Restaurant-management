package com.rest.models;

import com.rest.payment.PaymentMethod;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a payment made against a Bill.
 * The Payment entity solely relies on the Bill entity to determine the amount owed,
 * order linkage, and discount-adjusted total.
 * Utilizes polymorphic PaymentMethod implementations from com.rest.payment
 * to process payments, calculate change, and capture transaction-specific details.
 */
public class Payment {

    // ── Payment status lifecycle ──────────────────────────────────────────────
    public enum Status {
        PENDING,    // payment not yet collected
        COMPLETED,  // payment received in full
        FAILED,     // transaction was declined / unsuccessful
        REFUNDED    // money returned to the customer
    }

    private int           paymentId;
    private Bill          bill;           // the Bill entity this payment settles
    private double        amountPaid;     // amount actually tendered/charged
    private PaymentMethod paymentMethod;  // polymorphic payment method instance
    private Status        status;
    private LocalDateTime paidAt;         // timestamp of payment; null until paid

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Creates a new payment record in PENDING state using a Bill entity.
     *
     * @param paymentId     unique identifier for this payment
     * @param bill          the Bill entity being settled
     * @param paymentMethod polymorphic payment method instance
     */
    public Payment(int paymentId, Bill bill, PaymentMethod paymentMethod) {
        if (bill == null) {
            throw new IllegalArgumentException("Bill cannot be null for payment generation.");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method cannot be null.");
        }

        this.paymentId     = paymentId;
        this.bill          = bill;
        this.amountPaid    = 0.0;
        this.paymentMethod = paymentMethod;
        this.status        = Status.PENDING;
        this.paidAt        = null;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int getPaymentId() {
        return paymentId;
    }

    public Bill getBill() {
        return bill;
    }

    /** Retrieves the order ID directly from the associated Bill entity. */
    public int getOrderId() {
        return bill.getOrderId();
    }

    /** Retrieves the amount due directly from the associated Bill entity's total amount. */
    public double getAmountDue() {
        return bill.getTotalAmount();
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    /** Returns the change to be given back to the customer. */
    public double getChange() {
        if (paymentMethod != null) {
            return paymentMethod.getChange();
        }
        return Math.max(0, amountPaid - getAmountDue());
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getMethod() {
        return (paymentMethod != null) ? paymentMethod.getMethodName() : "UNKNOWN";
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    // ── Business methods ──────────────────────────────────────────────────────

    /**
     * Executes the payment polymorphically using the attached PaymentMethod.
     * Settles the amount due from the Bill and marks the Bill as PAID upon success.
     */
    public void pay() {
        if (paymentMethod == null) {
            throw new IllegalStateException("No payment method configured for Payment #" + paymentId);
        }
        double due = bill.getTotalAmount();
        boolean success = paymentMethod.processPayment(due);
        if (success) {
            this.amountPaid = paymentMethod.getAmountTendered();
            this.status     = Status.COMPLETED;
            this.paidAt     = LocalDateTime.now();
            this.bill.markPaid();
        } else {
            this.status     = Status.FAILED;
        }
    }

    /**
     * Marks the payment as FAILED (e.g. declined or cancelled).
     */
    public void markFailed() {
        this.status = Status.FAILED;
    }

    /**
     * Processes a refund — resets payment status to REFUNDED and bill status to UNPAID.
     */
    public void refund() {
        if (this.status != Status.COMPLETED) {
            throw new IllegalStateException("Only COMPLETED payments can be refunded.");
        }
        this.status = Status.REFUNDED;
        this.bill.markUnpaid();
    }

    /** Returns true only if payment has been fully collected. */
    public boolean isPaid() {
        return status == Status.COMPLETED;
    }

    // ── toString ─────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        String timeStr = (paidAt != null)
                ? paidAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                : "Not paid yet";
        String methodStr = (paymentMethod != null) ? paymentMethod.getMethodName() : "N/A";
        String detailsStr = (paymentMethod != null) ? " | " + paymentMethod.getTransactionDetails() : "";
        return String.format(
                "Payment #%d | Bill #%d | Order #%d | Due: $%.2f | Paid: $%.2f | Change: $%.2f | Method: %-12s | Status: %-10s | Time: %s%s",
                paymentId, bill.getBillId(), bill.getOrderId(), getAmountDue(), amountPaid, getChange(),
                methodStr, status, timeStr, detailsStr
        );
    }
}
