package com.rest.services;

import com.rest.models.MenuItem;
import com.rest.repository.MenuItemRepository;
import java.util.List;
import java.util.Optional;

/**
 * Business logic layer for MenuItem operations.
 */
public class MenuItemService {

    private final MenuItemRepository repository;

    public MenuItemService() {
        repository = new MenuItemRepository();
    }

    /** Adds a new menu item and returns its assigned id. */
    public int addMenuItem(String name, double price, MenuItem.Category category) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Menu item name cannot be empty.");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
        return repository.addMenuItem(name, price, category);
    }

    /** Returns all menu items. */
    public List<MenuItem> getAllMenuItems() {
        return repository.findAll();
    }

    /**
     * Looks up a menu item by id.
     *
     * @throws IllegalArgumentException if the item does not exist.
     */
    public MenuItem getMenuItemById(int id) {
        Optional<MenuItem> found = repository.findById(id);
        if (found.isEmpty()) {
            throw new IllegalArgumentException("No menu item found with id: " + id);
        }
        return found.get();
    }

    /** Removes a menu item from the menu. Returns true if removed. */
    public boolean removeMenuItem(int id) {
        return repository.deleteById(id);
    }

    /** Marks a menu item as unavailable (soft disable). */
    public void markUnavailable(int id) {
        getMenuItemById(id).setAvailable(false);
    }

    /** Marks a menu item as available again. */
    public void markAvailable(int id) {
        getMenuItemById(id).setAvailable(true);
    }
}
