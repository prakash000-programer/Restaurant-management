package com.rest.payment;

/**
 * Interface defining the contract for Online / NetBanking payment gateways.
 * Implemented by the common PaymentMethod class.
 */
public interface OnlinePayment {

    /** Returns the payment gateway or portal name. */
    String getGatewayName();

    /** Returns the payment gateway transaction reference ID. */
    String getGatewayTransactionId();
}
