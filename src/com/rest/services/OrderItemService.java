package com.rest.services;

import com.rest.models.MenuItem;
import com.rest.models.OrderItem;
import com.rest.models.Orders;
import com.rest.repository.OrderItemRepository;
import java.util.List;

/**
 * Business logic layer for OrderItem operations.
 * Coordinates between the order, the menu item, and the order-item storage.
 */
public class OrderItemService {

    private final OrderItemRepository repository;

    public OrderItemService() {
        repository = new OrderItemRepository();
    }

    /**
     * Adds a menu item to an existing order.
     * Takes a snapshot of the item's current price at order time.
     *
     * @param order    the order to add the item to
     * @param menuItem the menu item being ordered
     * @param quantity how many units
     * @return the created OrderItem
     * @throws IllegalArgumentException if the menu item is unavailable or quantity < 1
     */
    public OrderItem addItemToOrder(Orders order, MenuItem menuItem, int quantity) {
        if (!menuItem.isAvailable()) {
            throw new IllegalArgumentException(
                    "'" + menuItem.getName() + "' is currently unavailable.");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }

        // Persist the order item (price snapshot taken here)
        OrderItem item = repository.addOrderItem(
                order.getOrderId(),
                menuItem.getMenuItemId(),
                menuItem.getName(),
                quantity,
                menuItem.getPrice()
        );

        // Attach item to the in-memory order object as well
        order.addItem(item);
        return item;
    }

    /** Returns all items that belong to a specific order. */
    public List<OrderItem> getItemsByOrderId(int orderId) {
        return repository.findByOrderId(orderId);
    }

    /** Returns every order item across all orders. */
    public List<OrderItem> getAllOrderItems() {
        return repository.findAll();
    }
}
