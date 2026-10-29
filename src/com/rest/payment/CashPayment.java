package com.rest.payment;

/**
 * Interface defining the contract for Cash payments.
 * Implemented by the common PaymentMethod class.
 */
public interface CashPayment {

    /** Returns the cash amount tendered by the customer. */
    double getAmountTendered();

    /** Returns the change to be given back to the customer. */
    double getChange();

    /** Updates the cash amount tendered. */
    void setAmountTendered(double amountTendered);
}
