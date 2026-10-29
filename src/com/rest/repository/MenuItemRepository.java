package com.rest.repository;

import com.rest.models.MenuItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * In-memory storage for MenuItem records.
 * Pre-loaded with sample menu data for demonstration purposes.
 */
public class MenuItemRepository {

    private final List<MenuItem> menuItems = new ArrayList<>();
    private int nextId = 1;

    public MenuItemRepository() {
        // Seed the menu with sample items
        addMenuItem("Spring Rolls",      45.00, MenuItem.Category.STARTER);
        addMenuItem("Chicken Soup",      60.00, MenuItem.Category.STARTER);
        addMenuItem("Grilled Chicken",  180.00, MenuItem.Category.MAIN_COURSE);
        addMenuItem("Pasta Alfredo",    150.00, MenuItem.Category.MAIN_COURSE);
        addMenuItem("Veg Burger",       120.00, MenuItem.Category.MAIN_COURSE);
        addMenuItem("Chocolate Mousse",  80.00, MenuItem.Category.DESSERT);
        addMenuItem("Ice Cream",         50.00, MenuItem.Category.DESSERT);
        addMenuItem("Fresh Lime Soda",   40.00, MenuItem.Category.BEVERAGE);
        addMenuItem("Coffee",            35.00, MenuItem.Category.BEVERAGE);
    }

    /** Saves a new MenuItem and returns its assigned id. */
    public int addMenuItem(String name, double price, MenuItem.Category category) {
        int id = nextId++;
        menuItems.add(new MenuItem(id, name, price, category));
        return id;
    }

    /** Returns all menu items. */
    public List<MenuItem> findAll() {
        return menuItems;
    }

    /** Finds a menu item by its id; returns empty if not found. */
    public Optional<MenuItem> findById(int id) {
        return menuItems.stream()
                .filter(m -> m.getMenuItemId() == id)
                .findFirst();
    }

    /** Removes a menu item by id. Returns true if the item was found and removed. */
    public boolean deleteById(int id) {
        return menuItems.removeIf(m -> m.getMenuItemId() == id);
    }
}
