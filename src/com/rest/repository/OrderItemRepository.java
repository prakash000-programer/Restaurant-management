package com.rest.repository;

import com.rest.models.OrderItem;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * In-memory storage for OrderItem records.
 */
public class OrderItemRepository {

    private final List<OrderItem> orderItems = new ArrayList<>();
    private int nextId = 1;

    /**
     * Saves a new OrderItem and returns it with the assigned id.
     *
     * @param orderId      the parent order id
     * @param menuItemId   the menu item being ordered
     * @param menuItemName snapshot of the menu item name at order time
     * @param quantity     how many units
     * @param unitPrice    price per unit at order time
     * @return the persisted OrderItem
     */
    public OrderItem addOrderItem(int orderId, int menuItemId,
                                  String menuItemName, int quantity, double unitPrice) {
        OrderItem item = new OrderItem(nextId++, orderId, menuItemId,
                menuItemName, quantity, unitPrice);
        orderItems.add(item);
        return item;
    }

    /** Returns every order item in storage. */
    public List<OrderItem> findAll() {
        return orderItems;
    }

    /** Returns all order items that belong to a specific order. */
    public List<OrderItem> findByOrderId(int orderId) {
        return orderItems.stream()
                .filter(i -> i.getOrderId() == orderId)
                .collect(Collectors.toList());
    }
}
