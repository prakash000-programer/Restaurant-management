package com.rest.payment;

/**
 * Interface defining the contract for Credit Card payments.
 * Implemented by the common PaymentMethod class.
 */
public interface CreditCardPayment {

    /** Returns the masked 16-digit card number (e.g. ****-****-****-1234). */
    String getMaskedCardNumber();

    /** Returns the name of the cardholder. */
    String getCardHolderName();

    /** Returns the unique card authorization transaction code. */
    String getTransactionId();
}
