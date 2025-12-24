package com.ga.bankapp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction {
    private int transactionId;
    private int accountId;
    private int customerId;
    private String type; // "WITHDRAW", "DEPOSIT", "TRANSFER"
    private double amount;
    private LocalDateTime dateTime;
    private double postBalance; // Balance after transaction
    private Integer recipientAccountId; // For transfers (null if not a transfer)

    // Constructor for regular transactions (withdraw/deposit)
    public Transaction(int transactionId, int accountId, int customerId,
                       String type, double amount, double postBalance) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.customerId = customerId;
        this.type = type;
        this.amount = amount;
        this.postBalance = postBalance;
        this.dateTime = LocalDateTime.now();
        this.recipientAccountId = null;
    }

    // Constructor for transfer transactions
    public Transaction(int transactionId, int accountId, int customerId,
                       String type, double amount, double postBalance,
                       int recipientAccountId) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.customerId = customerId;
        this.type = type;
        this.amount = amount;
        this.postBalance = postBalance;
        this.dateTime = LocalDateTime.now();
        this.recipientAccountId = recipientAccountId;
    }

    // Getters
    public int getTransactionId() {
        return transactionId;
    }

    public int getAccountId() {
        return accountId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public double getPostBalance() {
        return postBalance;
    }

    public Integer getRecipientAccountId() {
        return recipientAccountId;
    }
    
    // Set date time (used when loading from file)
    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    // Format date and time for display
    public String getFormattedDateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }

    // Format date only
    public String getFormattedDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return dateTime.format(formatter);
    }

    // To string for display
    @Override
    public String toString() {
        String info = String.format("Transaction ID: %d | Type: %s | Amount: $%.2f | Balance: $%.2f | Date: %s",
                transactionId, type, amount, postBalance, getFormattedDateTime());
        if (recipientAccountId != null) {
            info += " | To Account: " + recipientAccountId;
        }
        return info;
    }
}
