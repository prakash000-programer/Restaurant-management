package com.rest.models;

/**
 * Represents a physical dining table in the restaurant.
 */
public class Table {

    public enum Availability {
        FREE,
        OCCUPIED
    }

    private int          tableId;
    private int          capacity;
    private Availability status;

    public Table(int tableId, int capacity) {
        this.tableId  = tableId;
        this.capacity = capacity;
        this.status   = Availability.FREE;  // every table starts as free
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int getTableId() {
        return tableId;
    }

    public int getCapacity() {
        return capacity;
    }

    public Availability getStatus() {
        return status;
    }

    public boolean isFree() {
        return status == Availability.FREE;
    }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setFree() {
        this.status = Availability.FREE;
    }

    public void setOccupied() {
        this.status = Availability.OCCUPIED;
    }

    @Override
    public String toString() {
        return String.format("Table #%d | Capacity: %d | Status: %s",
                tableId, capacity, status);
    }
}
