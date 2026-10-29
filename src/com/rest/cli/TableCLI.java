package com.rest.cli;

import com.rest.models.Table;
import com.rest.services.Tableservice;
import java.util.List;
import java.util.Scanner;
import com.rest.authentication.authendicate;
/**
 * Command-line interface for Table management.
 * Provides a menu-driven console UI for adding, viewing, and
 * updating table availability.
 */
public class TableCLI {

    private final Tableservice tableService;
    private final Scanner scanner;

    public TableCLI(Tableservice tableService, Scanner scanner) {
        this.tableService = tableService;
        this.scanner = scanner;
    }

    /** Entry point – shows the table management sub-menu until the user exits. */
    public void show() {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Enter choice: ");
            System.out.println();

            switch (choice) {
                case 1 -> viewAllTables();
                case 2 -> addTable();
                case 3 -> occupyTable();
                case 4 -> freeTable();
                case 0 -> running = false;
                default -> System.out.println("  [!] Invalid option. Please try again.\n");
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void printMenu() {
        System.out.println("╔══════════════════════════════╗");
        System.out.println("║      TABLE MANAGEMENT        ║");
        System.out.println("╠══════════════════════════════╣");
        System.out.println("║  1. View All Tables          ║");
        System.out.println("║  2. Add New Table            ║");
        System.out.println("║  3. Mark Table as Occupied   ║");
        System.out.println("║  4. Mark Table as Free       ║");
        System.out.println("║  0. Back to Main Menu        ║");
        System.out.println("╚══════════════════════════════╝");
    }

    private void viewAllTables() {
        List<Table> tables = tableService.getAllTables();
        if (tables.isEmpty()) {
            System.out.println("  No tables registered yet.\n");
            return;
        }
        System.out.println("*\n******************************************************************\n");
        System.out.println("  ──────────────────────── All Tables ──────────────────────────");
        for (Table t : tables) {
            System.out.println("  " + t);
        }
        System.out.println("\n******************************************************************\n");
    }

    private void addTable() {
        authendicate auth = new authendicate();
        if (!auth.check_password()) {
            return;
        }
        int capacity = readInt("  Enter table capacity (seats): ");
        try {
            Table table = tableService.addTable(capacity);
            System.out.println("  [✓] " + table + " added successfully.\n");
        } catch (IllegalArgumentException e) {
            System.out.println("  [!] " + e.getMessage() + "\n");
        }
    }

    private void occupyTable() {
        viewAllTables();
        int id = readInt("  Enter Table ID to mark as Occupied: ");
        try {
            tableService.occupyTable(id);
            System.out.println("  [✓] Table #" + id + " is now OCCUPIED.\n");
        } catch (IllegalArgumentException e) {
            System.out.println("  [!] " + e.getMessage() + "\n");
        }
    }

    private void freeTable() {
        viewAllTables();
        int id = readInt("  Enter Table ID to mark as Free: ");
        try {
            tableService.freeTable(id);
            System.out.println("  [✓] Table #" + id + " is now FREE.\n");
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
                System.out.println("  [!] Please enter a valid number.");
            }
        }
    }
}
