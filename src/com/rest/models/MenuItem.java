package com.rest.models;

/**
 * Represents a food/drink item available on the restaurant menu.
 */
public class MenuItem {

    public enum Category {
        STARTER,
        MAIN_COURSE,
        DESSERT,
        BEVERAGE
    }

    private int    menuItemId;
    private String name;
    private double price;
    private Category category;
    private boolean available;

    public MenuItem(int menuItemId, String name, double price, Category category) {
        this.menuItemId = menuItemId;
        this.name       = name;
        this.price      = price;
        this.category   = category;
        this.available  = true;          // newly added items are available by default
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int getMenuItemId() {
        return menuItemId;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isAvailable() {
        return available;
    }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setPrice(double price) {
        this.price = price;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return String.format("[%d] %-20s | %-12s | $%.2f | %s",
                menuItemId, name, category, price,
                available ? "Available" : "Unavailable");
    }
}
