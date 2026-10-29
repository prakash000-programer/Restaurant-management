package com.rest.payment;

import java.util.UUID;

/**
 * Common concrete class implementing all payment type interfaces:
 * CashPayment, CreditCardPayment, DebitcardPayment, UPIPayment, and OnlinePayment.
 * Serves as the central polymorphic execution engine for all transaction methods.
 */
public class PaymentMethod implements CashPayment, CreditCardPayment, DebitcardPayment, UPIPayment, OnlinePayment {

    public enum Type {
        CASH,
        CREDIT_CARD,
        DEBIT_CARD,
        UPI,
        ONLINE
    }

    private final Type   type;
    private final String methodName;

    // Cash fields
    private double amountTendered;
    private double change;

    // Card fields (Credit & Debit)
    private String cardNumber;
    private String cardHolderName;
    private String transactionId;
    private String bankName;
    private String authReference;

    // UPI fields
    private String upiId;
    private String upiRefNumber;

    // Online fields
    private String gatewayName;
    private String gatewayTransactionId;

    // Settled amount
    private double chargedAmount;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * Cash payment constructor.
     *
     * @param amountTendered cash tendered by the customer
     */
    public PaymentMethod(double amountTendered) {
        this.type           = Type.CASH;
        this.methodName     = "CASH";
        this.amountTendered = amountTendered;
        this.change         = 0.0;
    }

    /**
     * Credit Card payment constructor.
     *
     * @param cardNumber     16-digit card number
     * @param cardHolderName name of the cardholder
     */
    public PaymentMethod(String cardNumber, String cardHolderName) {
        this.type           = Type.CREDIT_CARD;
        this.methodName     = "CREDIT_CARD";
        this.cardNumber     = cardNumber;
        this.cardHolderName = cardHolderName;
        this.transactionId  = "CC-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Debit Card payment constructor.
     *
     * @param cardNumber 16-digit debit card number
     * @param bankName   name of the issuing bank
     * @param isDebit    flag distinguishing debit card signature
     */
    public PaymentMethod(String cardNumber, String bankName, boolean isDebit) {
        this.type          = Type.DEBIT_CARD;
        this.methodName    = "DEBIT_CARD";
        this.cardNumber    = cardNumber;
        this.bankName      = bankName;
        this.authReference = "DC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private PaymentMethod(Type type, String methodName) {
        this.type       = type;
        this.methodName = methodName;
    }

    // ── Static Factory Methods ────────────────────────────────────────────────

    /** Factory method to create a Cash payment. */
    public static PaymentMethod createCash(double amountTendered) {
        return new PaymentMethod(amountTendered);
    }

    /** Factory method to create a Credit Card payment. */
    public static PaymentMethod createCreditCard(String cardNumber, String cardHolderName) {
        return new PaymentMethod(cardNumber, cardHolderName);
    }

    /** Factory method to create a Debit Card payment. */
    public static PaymentMethod createDebitCard(String cardNumber, String bankName) {
        return new PaymentMethod(cardNumber, bankName, true);
    }

    /** Factory method to create a UPI payment. */
    public static PaymentMethod createUPI(String upiId) {
        PaymentMethod pm = new PaymentMethod(Type.UPI, "UPI");
        pm.upiId        = upiId;
        pm.upiRefNumber = "UPI" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return pm;
    }

    /** Factory method to create an Online payment. */
    public static PaymentMethod createOnline(String gatewayName) {
        PaymentMethod pm = new PaymentMethod(Type.ONLINE, "ONLINE");
        pm.gatewayName          = gatewayName;
        pm.gatewayTransactionId = "ONL-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        return pm;
    }

    // ── Payment Processing ────────────────────────────────────────────────────

    /**
     * Executes and authorizes the payment against the amount due.
     *
     * @param amountDue the required amount from the bill
     * @return true if successful
     */
    public boolean processPayment(double amountDue) {
        if (amountDue < 0) {
            throw new IllegalArgumentException("Amount due cannot be negative.");
        }
        this.chargedAmount = amountDue;

        if (this.type == Type.CASH) {
            if (amountTendered < amountDue) {
                throw new IllegalArgumentException(
                        String.format("Insufficient cash tendered! Due: $%.2f | Tendered: $%.2f",
                                amountDue, amountTendered));
            }
            this.change = amountTendered - amountDue;
        }
        return true;
    }

    /** Returns formatted transaction details tailored to the active payment type. */
    public String getTransactionDetails() {
        return switch (type) {
            case CASH ->
                String.format("Type: Cash | Tendered: $%.2f | Change Returned: $%.2f", amountTendered, change);
            case CREDIT_CARD ->
                String.format("Type: Credit Card | Card: %s | Holder: %s | Auth Code: %s",
                        getMaskedCardNumber(), cardHolderName, transactionId);
            case DEBIT_CARD ->
                String.format("Type: Debit Card | Card: %s | Bank: %s | Ref: %s",
                        getMaskedCardNumber(), bankName, authReference);
            case UPI ->
                String.format("Type: UPI | VPA/ID: %s | UTR/Ref: %s", upiId, upiRefNumber);
            case ONLINE ->
                String.format("Type: Online | Gateway: %s | Gateway Txn ID: %s", gatewayName, gatewayTransactionId);
        };
    }

    public String getMethodName() {
        return methodName;
    }

    public Type getType() {
        return type;
    }

    public double getAmountTendered() {
        if (type == Type.CASH) {
            return amountTendered;
        }
        return chargedAmount;
    }

    public double getChange() {
        return (type == Type.CASH) ? change : 0.0;
    }

    // ── CashPayment Interface Implementation ──────────────────────────────────

    @Override
    public void setAmountTendered(double amountTendered) {
        this.amountTendered = amountTendered;
    }

    // ── CreditCardPayment & DebitcardPayment Interface Implementation ─────────

    @Override
    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****-****-****-XXXX";
        }
        String last4 = cardNumber.substring(cardNumber.length() - 4);
        return "****-****-****-" + last4;
    }

    @Override
    public String getCardHolderName() {
        return cardHolderName;
    }

    @Override
    public String getTransactionId() {
        return transactionId;
    }

    @Override
    public String getBankName() {
        return bankName;
    }

    @Override
    public String getAuthReference() {
        return authReference;
    }

    // ── UPIPayment Interface Implementation ───────────────────────────────────

    @Override
    public String getUpiId() {
        return upiId;
    }

    @Override
    public String getUpiRefNumber() {
        return upiRefNumber;
    }

    // ── OnlinePayment Interface Implementation ────────────────────────────────

    @Override
    public String getGatewayName() {
        return gatewayName;
    }

    @Override
    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    @Override
    public String toString() {
        return methodName;
    }
}
