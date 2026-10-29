package com.rest.payment;

/**
 * Interface defining the contract for UPI payments.
 * Implemented by the common PaymentMethod class.
 */
public interface UPIPayment {

    /** Returns the Virtual Payment Address / UPI ID. */
    String getUpiId();

    /** Returns the UPI transaction reference (UTR) number. */
    String getUpiRefNumber();
}
