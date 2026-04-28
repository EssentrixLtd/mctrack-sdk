package com.mctrack.common.model;

public class PaymentProduct {
    private final String name;
    private final int quantity;
    private final Double price;

    public PaymentProduct(String name, int quantity, Double price) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    public PaymentProduct(String name, int quantity) {
        this(name, quantity, null);
    }

    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public Double getPrice() { return price; }
}
