package com.rest.repository;

import com.rest.models.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * In-memory storage for Table records.
 * Pre-loaded with sample tables for demonstration purposes.
 */
public class Tablerepository {

    private final List<Table> tables = new ArrayList<>();
    private int nextId = 1;

    public Tablerepository() {
        // Seed with a few sample tables
        addTable(2);
        addTable(4);
        addTable(6);
        addTable(8);
    }

    /** Creates a new table with the given capacity and returns it. */
    public Table addTable(int capacity) {
        Table table = new Table(nextId++, capacity);
        tables.add(table);
        return table;
    }

    /** Returns all tables. */
    public List<Table> findAll() {
        return tables;
    }

    /** Finds a table by its id; returns empty if not found. */
    public Optional<Table> findById(int tableId) {
        return tables.stream()
                .filter(t -> t.getTableId() == tableId)
                .findFirst();
    }
}
