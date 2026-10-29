package com.rest.repository;

import com.rest.models.Orders;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * In-memory storage for Order records.
 */
public class Ordersrepository {

    private final List<Orders> orders = new ArrayList<>();
    private int nextId = 1;

    /** Creates a new order for the given table and returns the persisted object. */
    public Orders addOrder(int tableId) {
        Orders order = new Orders(nextId++, tableId);
        orders.add(order);
        return order;
    }

    /** Returns all orders. */
    public List<Orders> findAll() {
        return orders;
    }

    /** Finds an order by id; returns empty if not found. */
    public Optional<Orders> findById(int orderId) {
        return orders.stream()
                .filter(o -> o.getOrderId() == orderId)
                .findFirst();
    }

    /** Returns all orders that are linked to a specific table. */
    public List<Orders> findByTableId(int tableId) {
        List<Orders> result = new ArrayList<>();
        for (Orders o : orders) {
            if (o.getTableId() == tableId) {
                result.add(o);
            }
        }
        return result;
    }
}