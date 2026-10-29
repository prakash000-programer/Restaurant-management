package com.rest.services;

import com.rest.models.Bill;
import com.rest.models.Orders;
import com.rest.repository.BillRepository;
import java.util.List;
import java.util.Optional;

/**
 * Business logic layer for Bill generation and management.
 * Coordinates discount calculations, bill status updates, and repository persistence.
 */
public class BillService {

    private final BillRepository repository;

    public BillService() {
        this.repository = new BillRepository();
    }

    public BillService(BillRepository repository) {
        this.repository = repository;
    }

    /**
     * Generates a bill for an order with a discount percentage.
     * If an unpaid bill already exists for this order, its discount is updated
     * and totals are recalculated to reflect current items.
     *
     * @param order              the order to generate a bill for
     * @param discountPercentage discount percentage to apply (0 to 100)
     * @return the generated or updated Bill
     */
    public Bill generateBill(Orders order, double discountPercentage) {
        if (order == null) {
            throw new IllegalArgumentException("Cannot generate bill for a null order.");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot generate bill for Order #" + order.getOrderId() + " because it contains no items.");
        }

        // Check if an existing bill exists for this order
        Optional<Bill> existingOpt = repository.findByOrderId(order.getOrderId());
        if (existingOpt.isPresent()) {
            Bill existing = existingOpt.get();
            if (existing.isPaid()) {
                throw new IllegalStateException("Order #" + order.getOrderId()
                        + " already has a completed payment on Bill #" + existing.getBillId() + ".");
            }
            // Update discount and recalculate existing unpaid bill
            existing.applyDiscountPercentage(discountPercentage);
            return existing;
        }

        return repository.addBill(order, discountPercentage);
    }

    /** Generates a bill with no discount (0%). */
    public Bill generateBill(Orders order) {
        return generateBill(order, 0.0);
    }

    /** Returns all recorded bills. */
    public List<Bill> getAllBills() {
        return repository.findAll();
    }

    /**
     * Retrieves a bill by its ID.
     *
     * @throws IllegalArgumentException if not found
     */
    public Bill getBillById(int billId) {
        Optional<Bill> found = repository.findById(billId);
        if (found.isEmpty()) {
            throw new IllegalArgumentException("No bill found with ID: " + billId);
        }
        return found.get();
    }

    /** Finds the latest bill for a given order ID. */
    public Optional<Bill> getBillByOrderId(int orderId) {
        return repository.findByOrderId(orderId);
    }

    /** Returns all unpaid bills. */
    public List<Bill> getUnpaidBills() {
        return repository.findByStatus(Bill.Status.UNPAID);
    }

    /** Returns all bills matching a given status. */
    public List<Bill> getBillsByStatus(Bill.Status status) {
        return repository.findByStatus(status);
    }

    /** Cancels an unpaid bill. */
    public void cancelBill(int billId) {
        Bill bill = getBillById(billId);
        if (bill.isPaid()) {
            throw new IllegalStateException("Cannot cancel Bill #" + billId + " because it has already been paid.");
        }
        bill.markCancelled();
    }

    /** Recalculates an unpaid bill (e.g. if items were added/modified). */
    public void recalculateBill(int billId) {
        Bill bill = getBillById(billId);
        if (bill.isPaid()) {
            throw new IllegalStateException("Cannot recalculate paid Bill #" + billId + ".");
        }
        bill.calculateBill();
    }
}
