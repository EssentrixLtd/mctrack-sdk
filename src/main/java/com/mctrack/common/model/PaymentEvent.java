package com.mctrack.common.model;

import java.util.Collections;
import java.util.List;

public class PaymentEvent {
    private final String paymentUuid;
    private final String playerUuid;
    private final String playerName;
    private final double amount;
    private final String currency;
    private final String provider;
    private final String transactionId;
    private final List<PaymentProduct> products;
    private final long timestamp;

    public PaymentEvent(String paymentUuid, String playerUuid, String playerName,
                       double amount, String currency, String provider,
                       String transactionId, List<PaymentProduct> products) {
        this.paymentUuid = paymentUuid;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.amount = amount;
        this.currency = currency;
        this.provider = provider;
        this.transactionId = transactionId;
        this.products = products != null ? products : Collections.emptyList();
        this.timestamp = System.currentTimeMillis();
    }

    public String getPaymentUuid() { return paymentUuid; }
    public String getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public double getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getProvider() { return provider; }
    public String getTransactionId() { return transactionId; }
    public List<PaymentProduct> getProducts() { return products; }
    public long getTimestamp() { return timestamp; }
}
