package com.rest.services;

import com.rest.models.Bill;
import com.rest.models.Payment;
import com.rest.payment.PaymentMethod;
import com.rest.repository.PaymentRepository;
import java.util.List;
import java.util.Optional;

/**
 * Business logic layer for Payment operations.
 * Coordinates polymorphic PaymentMethod execution and persistence using Bill entities.
 */
public class PaymentService {

    private final PaymentRepository repository;

    public PaymentService() {
        this.repository = new PaymentRepository();
    }

    public PaymentService(PaymentRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates and immediately processes a payment for a Bill using polymorphism.
     * The payment entity strictly consumes the Bill entity to determine amount due.
     *
     * @param bill          the Bill entity being settled
     * @param paymentMethod chosen polymorphic payment method
     * @return the completed Payment
     */
    public Payment processPayment(Bill bill, PaymentMethod paymentMethod) {
        if (bill == null) {
            throw new IllegalArgumentException("Bill cannot be null.");
        }
        if (bill.isPaid()) {
            throw new IllegalStateException("Bill #" + bill.getBillId() + " has already been paid.");
        }
        if (bill.getTotalAmount() < 0) {
            throw new IllegalArgumentException("Bill total amount cannot be negative.");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method cannot be null.");
        }

        Payment payment = repository.addPayment(bill, paymentMethod);
        payment.pay(); // polymorphic execution
        return payment;
    }

    /** Returns all recorded payments. */
    public List<Payment> getAllPayments() {
        return repository.findAll();
    }

    /**
     * Retrieves a payment by its ID.
     *
     * @throws IllegalArgumentException if not found
     */
    public Payment getPaymentById(int paymentId) {
        Optional<Payment> found = repository.findById(paymentId);
        if (found.isEmpty()) {
            throw new IllegalArgumentException("No payment found with ID: " + paymentId);
        }
        return found.get();
    }

    /** Retrieves the latest payment for a given bill ID. */
    public Optional<Payment> getPaymentByBillId(int billId) {
        return repository.findByBillId(billId);
    }

    /** Retrieves the latest payment for a given order ID. */
    public Optional<Payment> getPaymentByOrderId(int orderId) {
        return repository.findByOrderId(orderId);
    }

    /**
     * Refunds a completed payment.
     *
     * @param paymentId ID of the payment to refund
     */
    public void refundPayment(int paymentId) {
        Payment payment = getPaymentById(paymentId);
        payment.refund();
    }

    /** Marks a payment as failed. */
    public void markFailed(int paymentId) {
        Payment payment = getPaymentById(paymentId);
        payment.markFailed();
    }

    /** Returns all payments with a given status. */
    public List<Payment> getPaymentsByStatus(Payment.Status status) {
        return repository.findByStatus(status);
    }

    /** Calculates total revenue collected across all completed payments. */
    public double getTotalRevenue() {
        double total = 0.0;
        for (Payment p : repository.findAll()) {
            if (p.getStatus() == Payment.Status.COMPLETED) {
                total += p.getAmountDue();
            }
        }
        return total;
    }
}
