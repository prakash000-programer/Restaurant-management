package com.rest.cli;

import com.rest.models.Bill;
import com.rest.models.Orders;
import com.rest.models.Payment;
// import com.rest.payment.CashPayment;
// import com.rest.payment.CreditCardPayment;
// import com.rest.payment.DebitcardPayment;
// import com.rest.payment.OnlinePayment;
import com.rest.payment.PaymentMethod;
// import com.rest.payment.UPIPayment;
import com.rest.services.BillService;
import com.rest.services.Ordersservice;
import com.rest.services.PaymentService;
import com.rest.services.Tableservice;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Command-line interface for Payment and billing management.
 * Guarantees that payments are exclusively processed against Bill entities.
 * Demonstrates polymorphism by letting users choose from various PaymentMethod
 * implementations in the com.rest.payment package.
 */
public class PaymentCLI {

    private final PaymentService paymentService;
    private final BillService    billService;
    private final Ordersservice  ordersService;
    private final Tableservice   tableService;
    private final Scanner        scanner;

    public PaymentCLI(PaymentService paymentService, BillService billService,
                      Ordersservice ordersService, Tableservice tableService, Scanner scanner) {
        this.paymentService = paymentService;
        this.billService    = billService;
        this.ordersService  = ordersService;
        this.tableService   = tableService;
        this.scanner        = scanner;
    }

    public PaymentCLI(PaymentService paymentService, Ordersservice ordersService,
                      Tableservice tableService, Scanner scanner) {
        this(paymentService, new BillService(), ordersService, tableService, scanner);
    }

    /** Entry point – displays the payment menu until user exits. */
    public void show() {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Enter choice: ");
            System.out.println();

            switch (choice) {
                case 1 -> generateBillForOrder();
                case 2 -> processPayment();
                case 3 -> viewAllBills();
                case 4 -> viewAllPayments();
                case 5 -> viewPaymentReceipt();
                case 6 -> refundPayment();
                case 7 -> viewRevenueSummary();
                case 0 -> running = false;
                default -> System.out.println("  [!] Invalid option. Please try again.\n");
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void printMenu() {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║         PAYMENT & BILLING            ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  1. Generate Bill for Order          ║");
        System.out.println("║  2. Process Payment for Bill / Order ║");
        System.out.println("║  3. View All Bills                   ║");
        System.out.println("║  4. View All Payments                ║");
        System.out.println("║  5. View Payment Receipt             ║");
        System.out.println("║  6. Refund a Payment                 ║");
        System.out.println("║  7. Revenue Summary                  ║");
        System.out.println("║  0. Back to Main Menu                ║");
        System.out.println("╚══════════════════════════════════════╝");
    }

    /**
     * Generates a Bill entity for an order with discounts if applicable.
     */
    private void generateBillForOrder() {
        List<Orders> orders = ordersService.getAllOrders();
        if (orders.isEmpty()) {
            System.out.println("  No orders exist to generate a bill.\n");
            return;
        }

        System.out.println("  ── Active Orders ─────────────────────────────────────");
        for (Orders o : orders) {
            Optional<Bill> b = billService.getBillByOrderId(o.getOrderId());
            String billStatus = b.map(bill -> "Bill #" + bill.getBillId() + " [" + bill.getStatus() + "] Total: $" + String.format("%.2f", bill.getTotalAmount()))
                                 .orElse("No Bill Generated");
            System.out.printf("  Order #%d | Table #%d | Items: %d | Status: %-10s | %s%n",
                    o.getOrderId(), o.getTableId(), o.getItems().size(), o.getStatus(), billStatus);
        }
        System.out.println();

        int orderId = readInt("  Enter Order ID to generate bill for: ");
        try {
            Orders order = ordersService.getOrderById(orderId);
            if (order.getItems().isEmpty()) {
                System.out.println("  [!] Order #" + orderId + " has no items. Add items before generating bill.\n");
                return;
            }

            Optional<Bill> existingOpt = billService.getBillByOrderId(orderId);
            if (existingOpt.isPresent() && existingOpt.get().isPaid()) {
                System.out.println("  [!] Order #" + orderId + " already has a PAID bill (Bill #"
                        + existingOpt.get().getBillId() + ").\n");
                return;
            }

            System.out.print("  Enter discount percentage (0-100, press Enter for 0%): ");
            String discInput = scanner.nextLine().trim();
            double discount = discInput.isEmpty() ? 0.0 : Double.parseDouble(discInput);

            Bill bill = billService.generateBill(order, discount);
            System.out.println("\n  [✓] Successfully generated/updated Bill #" + bill.getBillId());
            System.out.printf ("      Gross Subtotal : $%.2f%n", bill.getSubTotal());
            System.out.printf ("      Discount (%.1f%%): -$%.2f%n", bill.getDiscountPercentage(), bill.getDiscountAmount());
            System.out.printf ("      Net Total Due  : $%.2f%n%n", bill.getTotalAmount());

        } catch (Exception e) {
            System.out.println("  [!] " + e.getMessage() + "\n");
        }
    }

    /**
     * Processes payment for a Bill.
     * The Payment entity strictly uses the Bill entity to generate the payment.
     */
    private void processPayment() {
        List<Orders> orders = ordersService.getAllOrders();
        if (orders.isEmpty()) {
            System.out.println("  No orders exist to settle payment.\n");
            return;
        }

        System.out.println("  ── Unsettled Orders & Bills ──────────────────────────");
        boolean hasPayable = false;
        for (Orders o : orders) {
            Optional<Payment> existingPayment = paymentService.getPaymentByOrderId(o.getOrderId());
            boolean isPaid = existingPayment.isPresent() && existingPayment.get().isPaid();
            if (!isPaid && o.getStatus() != Orders.Status.CANCELLED) {
                Optional<Bill> billOpt = billService.getBillByOrderId(o.getOrderId());
                String billInfo = billOpt.map(b -> String.format("Bill #%d Due: $%.2f (Discount: %.1f%%)",
                        b.getBillId(), b.getTotalAmount(), b.getDiscountPercentage()))
                        .orElse("Bill not generated yet");
                System.out.printf("  Order #%d | Table #%d | Status: %-10s | %s%n",
                        o.getOrderId(), o.getTableId(), o.getStatus(), billInfo);
                hasPayable = true;
            }
        }

        if (!hasPayable) {
            System.out.println("  All active orders are already paid or cancelled.\n");
            return;
        }

        System.out.println();
        int orderId = readInt("  Enter Order ID to pay: ");

        try {
            Orders order = ordersService.getOrderById(orderId);

            // Check if already paid
            Optional<Payment> existingPayment = paymentService.getPaymentByOrderId(orderId);
            if (existingPayment.isPresent() && existingPayment.get().isPaid()) {
                System.out.println("  [!] Order #" + orderId + " has already been paid.\n");
                return;
            }

            if (order.getItems().isEmpty()) {
                System.out.println("  [!] Order #" + orderId + " has no items. Add items before settling.\n");
                return;
            }

            // Retrieve or generate the Bill entity for this order
            Optional<Bill> billOpt = billService.getBillByOrderId(orderId);
            Bill bill;
            if (billOpt.isPresent()) {
                bill = billOpt.get();
                if (bill.isPaid()) {
                    System.out.println("  [!] Bill #" + bill.getBillId() + " is already paid.\n");
                    return;
                }
                // Refresh bill calculation against order items
                bill.calculateBill();
                System.out.printf("  Found Bill #%d: Subtotal: $%.2f | Discount: $%.2f (%.1f%%) | Total Due: $%.2f%n",
                        bill.getBillId(), bill.getSubTotal(), bill.getDiscountAmount(),
                        bill.getDiscountPercentage(), bill.getTotalAmount());

                System.out.print("  Modify discount before payment? (y/n): ");
                String modChoice = scanner.nextLine().trim();
                if (modChoice.equalsIgnoreCase("y") || modChoice.equalsIgnoreCase("yes")) {
                    double newDiscount = readDouble("  Enter new discount percentage (0-100): ");
                    bill = billService.generateBill(order, newDiscount);
                }
            } else {
                System.out.println("  No bill generated yet for Order #" + orderId + ". Generating bill now...");
                System.out.print("  Enter discount percentage if available (0-100, press Enter for 0%): ");
                String discInput = scanner.nextLine().trim();
                double discount = discInput.isEmpty() ? 0.0 : Double.parseDouble(discInput);
                bill = billService.generateBill(order, discount);
                System.out.printf("  [✓] Bill #%d generated: Net Total Due = $%.2f%n",
                        bill.getBillId(), bill.getTotalAmount());
            }

            double totalDue = bill.getTotalAmount();
            if (totalDue < 0) {
                System.out.println("  [!] Invalid bill total amount.\n");
                return;
            }

            // Polymorphic method choice: instantiate specific PaymentMethod subclass
            PaymentMethod paymentMethod = choosePaymentMethod(totalDue);

            // Payment entity strictly uses the Bill entity to generate the payment!
            Payment payment = paymentService.processPayment(bill, paymentMethod);

            // Mark order as completed
            ordersService.updateOrderStatus(orderId, Orders.Status.COMPLETED);

            System.out.println();
            printReceipt(payment, order);

            // Ask to free the table
            System.out.print("  Mark Table #" + order.getTableId() + " as FREE now? (y/n): ");
            String freeTableChoice = scanner.nextLine().trim();
            if (freeTableChoice.equalsIgnoreCase("y") || freeTableChoice.equalsIgnoreCase("yes")) {
                try {
                    tableService.freeTable(order.getTableId());
                    System.out.println("  [✓] Table #" + order.getTableId() + " marked as FREE.\n");
                } catch (Exception e) {
                    System.out.println("  [!] Could not free table: " + e.getMessage() + "\n");
                }
            } else {
                System.out.println();
            }

        } catch (Exception e) {
            System.out.println("  [!] " + e.getMessage() + "\n");
        }
    }

    /**
     * Factory method demonstrating polymorphism: constructs and returns a specific
     * PaymentMethod subclass based on the user's choice.
     */
    private PaymentMethod choosePaymentMethod(double totalDue) {
        System.out.println("\n  Select Payment Method:");
        System.out.println("    1. Cash (with tendered & change handling)");
        System.out.println("    2. Credit Card (card masking & authorization code)");
        System.out.println("    3. Debit Card (bank network verification)");
        System.out.println("    4. UPI (Virtual Payment Address & UTR reference)");
        System.out.println("    5. Online (Gateway webhook settlement)");

        while (true) {
            int choice = readInt("  Choose method (1-5): ");
            switch (choice) {
                case 1 -> {
                    while (true) {
                        double tendered = readDouble(String.format("  Enter cash amount tendered (min $%.2f): $", totalDue));
                        if (tendered >= totalDue) {
                            return PaymentMethod.createCash(tendered);
                        }
                        System.out.printf("  [!] Tendered cash must be at least $%.2f%n", totalDue);
                    }
                }
                case 2 -> {
                    System.out.print("  Enter 16-digit Card Number (or Enter for demo card): ");
                    String cardNum = scanner.nextLine().trim();
                    if (cardNum.isEmpty()) cardNum = "4532890123459876";

                    System.out.print("  Enter Cardholder Name: ");
                    String cardHolder = scanner.nextLine().trim();
                    if (cardHolder.isEmpty()) cardHolder = "Valued Customer";

                    return PaymentMethod.createCreditCard(cardNum, cardHolder);
                }
                case 3 -> {
                    System.out.print("  Enter 16-digit Debit Card Number (or Enter for demo card): ");
                    String cardNum = scanner.nextLine().trim();
                    if (cardNum.isEmpty()) cardNum = "5241880099881122";

                    System.out.print("  Enter Issuing Bank Name: ");
                    String bank = scanner.nextLine().trim();
                    if (bank.isEmpty()) bank = "Chase Bank";

                    return PaymentMethod.createDebitCard(cardNum, bank);
                }
                case 4 -> {
                    System.out.print("  Enter Customer UPI ID (e.g. user@okaxis): ");
                    String upiId = scanner.nextLine().trim();
                    if (upiId.isEmpty()) upiId = "customer@bistro";

                    return PaymentMethod.createUPI(upiId);
                }
                case 5 -> {
                    System.out.print("  Enter Online Gateway / Portal Name: ");
                    String portal = scanner.nextLine().trim();
                    if (portal.isEmpty()) portal = "Stripe NetBanking";

                    return PaymentMethod.createOnline(portal);
                }
                default -> System.out.println("  [!] Invalid choice. Enter 1-5.");
            }
        }
    }

    private void viewAllBills() {
        List<Bill> bills = billService.getAllBills();
        if (bills.isEmpty()) {
            System.out.println("  No bills generated yet.\n");
            return;
        }

        System.out.println("  ── All Bills ─────────────────────────────────────────────────────────────────");
        for (Bill b : bills) {
            System.out.println("  " + b);
        }
        System.out.println();
    }

    private void viewAllPayments() {
        List<Payment> payments = paymentService.getAllPayments();
        if (payments.isEmpty()) {
            System.out.println("  No payments recorded yet.\n");
            return;
        }

        System.out.println("  ── All Payments ─────────────────────────────────────────────────────────────");
        for (Payment p : payments) {
            System.out.println("  " + p);
        }
        System.out.println();
    }

    private void viewPaymentReceipt() {
        int paymentId = readInt("  Enter Payment ID: ");
        try {
            Payment payment = paymentService.getPaymentById(paymentId);
            Orders order = ordersService.getOrderById(payment.getOrderId());
            printReceipt(payment, order);
        } catch (IllegalArgumentException e) {
            System.out.println("  [!] " + e.getMessage() + "\n");
        }
    }

    private void refundPayment() {
        int paymentId = readInt("  Enter Payment ID to refund: ");
        try {
            Payment payment = paymentService.getPaymentById(paymentId);
            if (payment.getStatus() != Payment.Status.COMPLETED) {
                System.out.println("  [!] Payment #" + paymentId + " status is " + payment.getStatus() +
                                   ". Only COMPLETED payments can be refunded.\n");
                return;
            }

            System.out.printf("  Are you sure you want to refund Payment #%d of $%.2f? (y/n): ",
                    payment.getPaymentId(), payment.getAmountDue());
            String confirm = scanner.nextLine().trim();
            if (confirm.equalsIgnoreCase("y") || confirm.equalsIgnoreCase("yes")) {
                paymentService.refundPayment(paymentId);
                System.out.println("  [✓] Payment #" + paymentId + " and Bill #"
                        + payment.getBill().getBillId() + " have been successfully REFUNDED.\n");
            } else {
                System.out.println("  Refund cancelled.\n");
            }
        } catch (Exception e) {
            System.out.println("  [!] " + e.getMessage() + "\n");
        }
    }

    private void viewRevenueSummary() {
        List<Payment> payments = paymentService.getAllPayments();
        long completedCount = payments.stream().filter(p -> p.getStatus() == Payment.Status.COMPLETED).count();
        long refundedCount  = payments.stream().filter(p -> p.getStatus() == Payment.Status.REFUNDED).count();
        long failedCount    = payments.stream().filter(p -> p.getStatus() == Payment.Status.FAILED).count();
        double totalRevenue = paymentService.getTotalRevenue();

        System.out.println("  ╔══════════════════════════════════════════╗");
        System.out.println("  ║             REVENUE SUMMARY              ║");
        System.out.println("  ╠══════════════════════════════════════════╣");
        System.out.printf ("  ║  Completed Payments : %-19d║%n", completedCount);
        System.out.printf ("  ║  Refunded Payments  : %-19d║%n", refundedCount);
        System.out.printf ("  ║  Failed Transactions: %-19d║%n", failedCount);
        System.out.println("  ╠══════════════════════════════════════════╣");
        System.out.printf ("  ║  TOTAL NET REVENUE  : $%-18.2f║%n", totalRevenue);
        System.out.println("  ╚══════════════════════════════════════════╝\n");
    }

    private void printReceipt(Payment payment, Orders order) {
        String timeStr = (payment.getPaidAt() != null)
                ? payment.getPaidAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : "N/A";
        String methodDetails = (payment.getPaymentMethod() != null)
                ? payment.getPaymentMethod().getTransactionDetails()
                : "Standard Settlement";
        Bill bill = payment.getBill();

        System.out.println("  ╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("  ║                         PAYMENT RECEIPT                           ║");
        System.out.println("  ╠═══════════════════════════════════════════════════════════════════╣");
        System.out.printf ("  ║  Receipt #   : %-51d║%n", payment.getPaymentId());
        System.out.printf ("  ║  Bill #      : %-51d║%n", bill.getBillId());
        System.out.printf ("  ║  Order #     : %-51d║%n", payment.getOrderId());
        System.out.printf ("  ║  Table #     : %-51d║%n", order.getTableId());
        System.out.printf ("  ║  Date/Time   : %-51s║%n", timeStr);
        System.out.printf ("  ║  Method      : %-51s║%n", payment.getMethod());
        System.out.printf ("  ║  Details     : %-51s║%n", methodDetails);
        System.out.printf ("  ║  Status      : %-51s║%n", payment.getStatus());
        System.out.println("  ╠═══════════════════════════════════════════════════════════════════╣");
        System.out.printf ("  ║  Subtotal    : $%-50.2f║%n", bill.getSubTotal());
        if (bill.getDiscountAmount() > 0) {
            System.out.printf ("  ║  Discount    : -$%-49.2f (%.1f%%)║%n",
                    bill.getDiscountAmount(), bill.getDiscountPercentage());
        }
        System.out.printf ("  ║  Amount Due  : $%-50.2f║%n", payment.getAmountDue());
        System.out.printf ("  ║  Amount Paid : $%-50.2f║%n", payment.getAmountPaid());
        System.out.printf ("  ║  Change      : $%-50.2f║%n", payment.getChange());
        System.out.println("  ╚═══════════════════════════════════════════════════════════════════╝\n");
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("  [!] Please enter a valid integer.");
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
