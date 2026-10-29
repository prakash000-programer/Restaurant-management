package com.rest.payment;

/**
 * Interface defining the contract for Debit Card payments.
 * Implemented by the common PaymentMethod class.
 */
public interface DebitcardPayment {

    /** Returns the masked debit card number. */
    String getMaskedCardNumber();

    /** Returns the name of the issuing bank. */
    String getBankName();

    /** Returns the bank network authorization reference code. */
    String getAuthReference();
}
