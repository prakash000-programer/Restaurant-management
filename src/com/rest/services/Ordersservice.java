package com.rest.services;

import com.rest.models.Orders;
import com.rest.repository.Ordersrepository;
import java.util.List;
import java.util.Optional;

/**
 * Business logic layer for Order operations.
 */
public class Ordersservice {

    private final Ordersrepository repository;

    public Ordersservice() {
        repository = new Ordersrepository();
    }

    /**
     * Creates a new order for the given table.
     *
     * @param tableId the table the order is placed for
     * @return the newly created Orders object
     */
    public Orders createOrder(int tableId) {
        return repository.addOrder(tableId);
    }

    /** Returns all orders. */
    public List<Orders> getAllOrders() {
        return repository.findAll();
    }

    /**
     * Retrieves an order by id.
     *
     * @throws IllegalArgumentException if the order does not exist.
     */
    public Orders getOrderById(int orderId) {
        Optional<Orders> found = repository.findById(orderId);
        if (found.isEmpty()) {
            throw new IllegalArgumentException("No order found with id: " + orderId);
        }
        return found.get();
    }

    /** Returns all orders placed at a given table. */
    public List<Orders> getOrdersByTable(int tableId) {
        return repository.findByTableId(tableId);
    }

    /**
     * Updates the status of an existing order.
     *
     * @param orderId the target order
     * @param status  the new status to apply
     */
    public void updateOrderStatus(int orderId, Orders.Status status) {
        getOrderById(orderId).setStatus(status);
    }
}