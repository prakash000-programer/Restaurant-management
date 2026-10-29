package com.rest.cli;

import com.rest.models.Bill;
import com.rest.models.MenuItem;
import com.rest.models.OrderItem;
import com.rest.models.Orders;
import com.rest.services.BillService;
import com.rest.services.MenuItemService;
import com.rest.services.OrderItemService;
import com.rest.services.Ordersservice;
import com.rest.services.Tableservice;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Command-line interface for Order management.
 * Handles order creation, item addition, status updates, and bill printing via BillService.
 */
public class OrderCLI {

    private final Ordersservice    ordersService;
    private final OrderItemService orderItemService;
    private final MenuItemService  menuItemService;
    private final Tableservice     tableService;
    private final BillService      billService;
    private final Scanner          scanner;

    public OrderCLI(Ordersservice ordersService, OrderItemService orderItemService,
                    MenuItemService menuItemService, Tableservice tableService,
                    BillService billService, Scanner scanner) {
        this.ordersService    = ordersService;
        this.orderItemService = orderItemService;
        this.menuItemService  = menuItemService;
        this.tableService     = tableService;
        this.billService      = billService;
        this.scanner          = scanner;
    }

    public OrderCLI(Ordersservice ordersService, OrderItemService orderItemService,
                    MenuItemService menuItemService, Tableservice tableService,
                    Scanner scanner) {
        this(ordersService, orderItemService, menuItemService, tableService, new BillService(), scanner);
    }

    /** Entry point – shows the order management sub-menu until the user exits. */
    public void show() {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Enter choice: ");
            System.out.println();

            switch (choice) {
                case 1 -> createOrder();
                case 2 -> addItemToOrder();
                case 3 -> viewAllOrders();
                case 4 -> viewOrderDetails();
                case 5 -> updateOrderStatus();
                case 6 -> printBill();
                case 0 -> running = false;
                default -> System.out.println("  [!] Invalid option. Please try again.\n");
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void printMenu() {
        System.out.println("╔══════════════════════════════╗");
        System.out.println("║      ORDER MANAGEMENT        ║");
        System.out.println("╠══════════════════════════════╣");
        System.out.println("║  1. Create New Order         ║");
        System.out.println("║  2. Add Item to Order        ║");
        System.out.println("║  3. View All Orders          ║");
        System.out.println("║  4. View Order Details       ║");
        System.out.println("║  5. Update Order Status      ║");
        System.out.println("║  6. Generate / Print Bill    ║");
        System.out.println("║  0. Back to Main Menu        ║");
        System.out.println("╚══════════════════════════════╝");
    }

    private void createOrder() {
        // Show available tables first
        System.out.println("  ── Available Tables ──────────────────────");
        tableService.getAllTables().forEach(t -> System.out.println("  " + t));
        System.out.println();

        int tableId = readInt("  Enter Table ID for the new order: ");
        try {
            // Validate table exists
            tableService.getTableById(tableId);
            Orders order = ordersService.createOrder(tableId);
            tableService.occupyTable(tableId);   // mark the table as occupied
            System.out.println("  [✓] Order #" + order.getOrderId()
                    + " created for Table #" + tableId + ".\n");
        } catch (IllegalArgumentException e) {
            System.out.println("  [!] " + e.getMessage() + "\n");
        }
    }

    private void addItemToOrder() {
        // Show existing orders
        viewAllOrders();
        int orderId = readInt("  Enter Order ID to add item to: ");

        // Show the menu
        System.out.println("\n  ── Menu ────────────────────────────────────────────────────────");
        menuItemService.getAllMenuItems().forEach(m -> System.out.println("  " + m));
        System.out.println();

        int menuItemId = readInt("  Enter Menu Item ID: ");
        int quantity   = readInt("  Enter Quantity    : ");

        try {
            Orders   order    = ordersService.getOrderById(orderId);
            MenuItem menuItem = menuItemService.getMenuItemById(menuItemId);
            OrderItem added   = orderItemService.addItemToOrder(order, menuItem, quantity);
            System.out.println("  [✓] Added: " + added + "\n");

            // Recalculate bill if an unpaid bill exists for this order
            Optional<Bill> existingBill = billService.getBillByOrderId(orderId);
            if (existingBill.isPresent() && !existingBill.get().isPaid()) {
                billService.recalculateBill(existingBill.get().getBillId());
            }
        } catch (IllegalArgumentException e) {
            System.out.println("  [!] " + e.getMessage() + "\n");
        }
    }

    private void viewAllOrders() {
        List<Orders> orders = ordersService.getAllOrders();
        if (orders.isEmpty()) {
            System.out.println("  No orders placed yet.\n");
            return;
        }
        System.out.println("  ── All Orders ──────────────────────────────────────────────────");
        for (Orders o : orders) {
            System.out.println("  " + o);
        }
        System.out.println();
    }

    private void viewOrderDetails() {
        viewAllOrders();
        int orderId = readInt("  Enter Order ID to inspect: ");
        try {
            Orders order = ordersService.getOrderById(orderId);
            System.out.println("\n  ── Details for Order #" + orderId + " ──────────────────────────");
            System.out.println("  " + order);
            List<OrderItem> items = order.getItems();
            if (items.isEmpty()) {
                System.out.println("  (no items added yet)");
            } else {
                for (OrderItem item : items) {
                    System.out.println("    " + item);
                }
            }

            double itemsSubTotal = 0.0;
            for (OrderItem item : items) {
                itemsSubTotal += item.getSubTotal();
            }
            System.out.printf("  %-44s Items Subtotal: $%.2f%n", "", itemsSubTotal);

            // Display associated Bill information if one exists
            Optional<Bill> billOpt = billService.getBillByOrderId(orderId);
            if (billOpt.isPresent()) {
                Bill bill = billOpt.get();
                System.out.printf("  %-44s Bill #%d Status: %s | Net Total: $%.2f (Discount: %.1f%%)%n",
                        "", bill.getBillId(), bill.getStatus(), bill.getTotalAmount(), bill.getDiscountPercentage());
            } else {
                System.out.printf("  %-44s (Bill not yet generated)%n", "");
            }
            System.out.println();
        } catch (IllegalArgumentException e) {
            System.out.println("  [!] " + e.getMessage() + "\n");
        }
    }

    private void updateOrderStatus() {
        viewAllOrders();
        int orderId = readInt("  Enter Order ID to update: ");

        System.out.println("  Statuses: 1-PENDING  2-PREPARING  3-READY  4-COMPLETED  5-CANCELLED");
        int statusChoice = readInt("  Choose new status: ");
        Orders.Status newStatus = switch (statusChoice) {
            case 1 -> Orders.Status.PENDING;
            case 2 -> Orders.Status.PREPARING;
            case 3 -> Orders.Status.READY;
            case 4 -> Orders.Status.COMPLETED;
            case 5 -> Orders.Status.CANCELLED;
            default -> {
                System.out.println("  [!] Unknown status choice, keeping current status.\n");
                yield null;
            }
        };

        if (newStatus == null) return;

        try {
            ordersService.updateOrderStatus(orderId, newStatus);

            // If order is completed or cancelled, free the table
            if (newStatus == Orders.Status.COMPLETED || newStatus == Orders.Status.CANCELLED) {
                int tableId = ordersService.getOrderById(orderId).getTableId();
                tableService.freeTable(tableId);
                System.out.println("  [✓] Table #" + tableId + " is now FREE.");
            }
            System.out.println("  [✓] Order #" + orderId + " status updated to " + newStatus + ".\n");
        } catch (IllegalArgumentException e) {
            System.out.println("  [!] " + e.getMessage() + "\n");
        }
    }

    private void printBill() {
        viewAllOrders();
        int orderId = readInt("  Enter Order ID to generate/print bill: ");
        try {
            Orders order = ordersService.getOrderById(orderId);
            if (order.getItems().isEmpty()) {
                System.out.println("  [!] Order #" + orderId + " has no items. Add items first.\n");
                return;
            }

            Optional<Bill> existingBill = billService.getBillByOrderId(orderId);
            Bill bill;
            if (existingBill.isPresent()) {
                bill = existingBill.get();
                if (!bill.isPaid()) {
                    System.out.printf("  Existing unpaid Bill #%d found (Current Discount: %.1f%%).%n",
                            bill.getBillId(), bill.getDiscountPercentage());
                    System.out.print("  Apply/update discount percentage? (y/n): ");
                    String ans = scanner.nextLine().trim();
                    if (ans.equalsIgnoreCase("y") || ans.equalsIgnoreCase("yes")) {
                        double discount = readDouble("  Enter discount percentage (0-100): ");
                        bill = billService.generateBill(order, discount);
                    } else {
                        bill.calculateBill();
                    }
                }
            } else {
                System.out.print("  Enter discount percentage if available (0-100, press Enter for 0%): ");
                String discInput = scanner.nextLine().trim();
                double discount = discInput.isEmpty() ? 0.0 : Double.parseDouble(discInput);
                bill = billService.generateBill(order, discount);
                System.out.println("  [✓] Generated Bill #" + bill.getBillId() + " successfully.");
            }

            printBillDetails(bill, order);

        } catch (Exception e) {
            System.out.println("  [!] " + e.getMessage() + "\n");
        }
    }

    private void printBillDetails(Bill bill, Orders order) {
        String timeStr = bill.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        System.out.println("\n  ╔════════════════════════════════════════════════════════════════╗");
        System.out.println("  ║                        RESTAURANT BILL                         ║");
        System.out.println("  ╠════════════════════════════════════════════════════════════════╣");
        System.out.printf ("  ║  Bill #   : %-48d║%n", bill.getBillId());
        System.out.printf ("  ║  Order #  : %-48d║%n", order.getOrderId());
        System.out.printf ("  ║  Table #  : %-48d║%n", order.getTableId());
        System.out.printf ("  ║  Date     : %-48s║%n", timeStr);
        System.out.printf ("  ║  Status   : %-48s║%n", bill.getStatus());
        System.out.println("  ╠════════════════════════════════════════════════════════════════╣");

        for (OrderItem item : order.getItems()) {
            System.out.printf("  ║  %-24s x%-3d  @$%-7.2f = $%-10.2f║%n",
                    item.getMenuItemName(), item.getQuantity(), item.getUnitPrice(), item.getSubTotal());
        }

        System.out.println("  ╠════════════════════════════════════════════════════════════════╣");
        System.out.printf ("  ║  Gross Subtotal                                  $%10.2f  ║%n", bill.getSubTotal());
        if (bill.getDiscountAmount() > 0) {
            System.out.printf ("  ║  Discount (%.1f%%)                              -$%10.2f  ║%n",
                    bill.getDiscountPercentage(), bill.getDiscountAmount());
        } else {
            System.out.printf ("  ║  Discount (None)                                  $%10.2f  ║%n", 0.0);
        }
        System.out.println("  ╠════════════════════════════════════════════════════════════════╣");
        System.out.printf ("  ║  TOTAL AMOUNT DUE                                $%10.2f  ║%n", bill.getTotalAmount());
        System.out.println("  ╚════════════════════════════════════════════════════════════════╝\n");
    }

    /** Reads an integer from the console, retrying on bad input. */
    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("  [!] Please enter a valid number.");
            }
        }
    }

    private double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("  [!] Please enter a valid decimal number.");
            }
        }
    }
}
