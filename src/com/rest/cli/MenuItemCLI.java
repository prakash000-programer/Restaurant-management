package com.rest.cli;

import com.rest.models.MenuItem;
import com.rest.services.MenuItemService;
import java.util.List;
import java.util.Scanner;
import com.rest.authentication.authendicate;

/**
 * Command-line interface for Menu Item management.
 * Allows staff to view, add, remove, and toggle availability of menu items.
 */
public class MenuItemCLI {

    private final MenuItemService menuItemService;
    private final Scanner scanner;

    public MenuItemCLI(MenuItemService menuItemService, Scanner scanner) {
        this.menuItemService = menuItemService;
        this.scanner = scanner;
    }

    /**
     * Entry point – shows the menu-item management sub-menu until the user exits.
     */
    public void show() {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Enter choice: ");
            System.out.println();

            switch (choice) {
                case 1 -> viewAllMenuItems();
                case 2 -> addMenuItem();
                case 3 -> removeMenuItem();
                case 4 -> toggleAvailability();
                case 0 -> running = false;
                default -> System.out.println("  [!] Invalid option. Please try again.\n");
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void printMenu() {
        System.out.println("╔══════════════════════════════╗");
        System.out.println("║      MENU ITEM MANAGEMENT    ║");
        System.out.println("╠══════════════════════════════╣");
        System.out.println("║  1. View Full Menu           ║");
        System.out.println("║  2. Add Menu Item            ║");
        System.out.println("║  3. Remove Menu Item         ║");
        System.out.println("║  4. Toggle Availability      ║");
        System.out.println("║  0. Back to Main Menu        ║");
        System.out.println("╚══════════════════════════════╝");
    }

    private void viewAllMenuItems() {
        List<MenuItem> items = menuItemService.getAllMenuItems();
        if (items.isEmpty()) {
            System.out.println("  No menu items available.\n");
            return;
        }
        System.out.println("  ── Full Menu ───────────────────────────────────────────────────");
        for (MenuItem item : items) {
            System.out.println("  " + item);
        }
        System.out.println();
    }

    private void addMenuItem() {
        authendicate auth = new authendicate();
        if (!auth.check_password()) {
            return;
        }
        System.out.print("  Item name        : ");
        String name = scanner.nextLine().trim();

        double price = readDouble("  Price ($)         : ");

        System.out.println("  Categories: 1-STARTER  2-MAIN_COURSE  3-DESSERT  4-BEVERAGE");
        int catChoice = readInt("  Choose category  : ");
        MenuItem.Category category = switch (catChoice) {
            case 1 -> MenuItem.Category.STARTER;
            case 2 -> MenuItem.Category.MAIN_COURSE;
            case 3 -> MenuItem.Category.DESSERT;
            case 4 -> MenuItem.Category.BEVERAGE;
            default -> {
                System.out.println("  [!] Unknown category, defaulting to MAIN_COURSE.");
                yield MenuItem.Category.MAIN_COURSE;
            }
        };

        try {
            int id = menuItemService.addMenuItem(name, price, category);
            System.out.println("  [✓] Menu item '" + name + "' added with ID #" + id + ".\n");
        } catch (IllegalArgumentException e) {
            System.out.println("  [!] " + e.getMessage() + "\n");
        }
    }

    private void removeMenuItem() {
        viewAllMenuItems();
        int id = readInt("  Enter Menu Item ID to remove: ");
        boolean removed = menuItemService.removeMenuItem(id);
        if (removed) {
            System.out.println("  [✓] Menu item #" + id + " removed.\n");
        } else {
            System.out.println("  [!] No item found with ID #" + id + ".\n");
        }
    }

    private void toggleAvailability() {
        viewAllMenuItems();
        int id = readInt("  Enter Menu Item ID to toggle: ");
        try {
            MenuItem item = menuItemService.getMenuItemById(id);
            if (item.isAvailable()) {
                menuItemService.markUnavailable(id);
                System.out.println("  [✓] '" + item.getName() + "' marked as UNAVAILABLE.\n");
            } else {
                menuItemService.markAvailable(id);
                System.out.println("  [✓] '" + item.getName() + "' marked as AVAILABLE.\n");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("  [!] " + e.getMessage() + "\n");
        }
    }

    /** Reads an integer from the console, retrying on bad input. */
    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("  [!] Please enter a valid whole number.");
            }
        }
    }

    /** Reads a double from the console, retrying on bad input. */
    private double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("  [!] Please enter a valid price (e.g. 12.50).");
            }
        }
    }
}
