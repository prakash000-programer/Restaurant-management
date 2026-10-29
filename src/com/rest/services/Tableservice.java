package com.rest.services;

import com.rest.models.Table;
import com.rest.repository.Tablerepository;
import java.util.List;
import java.util.Optional;

/**
 * Business logic layer for Table operations.
 */
public class Tableservice {

    private final Tablerepository repository;

    public Tableservice() {
        repository = new Tablerepository();
    }

    /**
     * Adds a new table with the given capacity.
     *
     * @param capacity number of seats
     * @return the newly created Table
     * @throws IllegalArgumentException if capacity is less than 1
     */
    public Table addTable(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Table capacity must be at least 1.");
        }
        return repository.addTable(capacity);
    }

    /** Returns all tables. */
    public List<Table> getAllTables() {
        return repository.findAll();
    }

    /**
     * Retrieves a table by id.
     *
     * @throws IllegalArgumentException if the table does not exist.
     */
    public Table getTableById(int tableId) {
        Optional<Table> found = repository.findById(tableId);
        if (found.isEmpty()) {
            throw new IllegalArgumentException("No table found with id: " + tableId);
        }
        return found.get();
    }

    /** Marks a table as occupied. */
    public void occupyTable(int tableId) {
        getTableById(tableId).setOccupied();
    }

    /** Marks a table as free (e.g. after order is completed). */
    public void freeTable(int tableId) {
        getTableById(tableId).setFree();
    }
}
