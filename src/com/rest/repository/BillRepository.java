package com.rest.repository;

import com.rest.models.Bill;
import com.rest.models.Orders;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * In-memory repository for managing Bill entities.
 */
public class BillRepository {

    private final List<Bill> bills = new ArrayList<>();
    private int nextId = 1;

    /**
     * Creates and stores a new bill for an order with a discount percentage.
     *
     * @param order              the order being billed
     * @param discountPercentage discount percentage (0 to 100)
     * @return the saved Bill
     */
    public Bill addBill(Orders order, double discountPercentage) {
        Bill bill = new Bill(nextId++, order, discountPercentage);
        bills.add(bill);
        return bill;
    }

    /** Returns all recorded bills. */
    public List<Bill> findAll() {
        return new ArrayList<>(bills);
    }

    /** Finds a bill by its unique ID. */
    public Optional<Bill> findById(int billId) {
        return bills.stream()
                .filter(b -> b.getBillId() == billId)
                .findFirst();
    }

    /** Finds the latest bill associated with an order ID. */
    public Optional<Bill> findByOrderId(int orderId) {
        return bills.stream()
                .filter(b -> b.getOrderId() == orderId)
                .reduce((first, second) -> second);
    }

    /** Returns all bills matching a given status. */
    public List<Bill> findByStatus(Bill.Status status) {
        List<Bill> result = new ArrayList<>();
        for (Bill b : bills) {
            if (b.getStatus() == status) {
                result.add(b);
            }
        }
        return result;
    }
}
