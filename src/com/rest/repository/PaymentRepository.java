package com.rest.repository;

import com.rest.models.Bill;
import com.rest.models.Payment;
import com.rest.payment.PaymentMethod;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * In-memory repository for managing Payment records generated against Bill entities.
 */
public class PaymentRepository {

    private final List<Payment> payments = new ArrayList<>();
    private int nextId = 1;

    /**
     * Creates and stores a new payment record using a Bill entity in PENDING status.
     *
     * @param bill          the bill entity being paid for
     * @param paymentMethod polymorphic payment method instance
     * @return the newly saved Payment
     */
    public Payment addPayment(Bill bill, PaymentMethod paymentMethod) {
        Payment payment = new Payment(nextId++, bill, paymentMethod);
        payments.add(payment);
        return payment;
    }

    /** Returns all recorded payments. */
    public List<Payment> findAll() {
        return new ArrayList<>(payments);
    }

    /** Finds a payment by its unique ID. */
    public Optional<Payment> findById(int paymentId) {
        return payments.stream()
                .filter(p -> p.getPaymentId() == paymentId)
                .findFirst();
    }

    /** Finds the latest payment associated with a given Bill ID. */
    public Optional<Payment> findByBillId(int billId) {
        return payments.stream()
                .filter(p -> p.getBill() != null && p.getBill().getBillId() == billId)
                .reduce((first, second) -> second);
    }

    /** Finds the latest payment associated with a given order ID through its Bill. */
    public Optional<Payment> findByOrderId(int orderId) {
        return payments.stream()
                .filter(p -> p.getBill() != null && p.getOrderId() == orderId)
                .reduce((first, second) -> second);
    }

    /** Returns all payments matching a given status. */
    public List<Payment> findByStatus(Payment.Status status) {
        List<Payment> result = new ArrayList<>();
        for (Payment p : payments) {
            if (p.getStatus() == status) {
                result.add(p);
            }
        }
        return result;
    }
}
