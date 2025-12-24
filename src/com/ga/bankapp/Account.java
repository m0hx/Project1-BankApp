package com.ga.bankapp;

public abstract class Account {

    // Constants
    public static final double OVERDRAFT_FEE = 35.0; // ACME overdraft protection fee
    public static final double MAX_OVERDRAFT_AMOUNT = 100.0; // Maximum overdraft allowed when already negative
    public static final int MAX_OVERDRAFT_COUNT = 2; // Deactivate after 2 overdrafts

    // Fields
    protected int accountId;
    protected int customerId; // Which customer owns this account
    protected double balance;
    protected String accountType; // "CHECKING" or "SAVINGS"
    protected boolean isActive; // For overdraft deactivation
    protected int overdraftCount; // Track number of overdrafts
    protected DebitCard debitCard; // Debit card assigned to this account

    // Constructor
    public Account(int accountId, int customerId, double balance, String accountType, DebitCard debitCard) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.balance = balance;
        this.accountType = accountType;
        this.isActive = true; // Account starts active
        this.overdraftCount = 0; // No overdrafts initially
        this.debitCard = debitCard;
    }

    // Abstract methods (must be implemented by subclasses)
    public abstract boolean deposit(double amount);
    public abstract boolean withdraw(double amount);

    // Concrete methods (shared by all accounts)
    public double getBalance() {
        return balance;
    }

    public int getAccountId() {
        return accountId;
    }

    public String getAccountType() {
        return accountType;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public int getOverdraftCount() {
        return overdraftCount;
    }

    public void setOverdraftCount(int count) {
        this.overdraftCount = count;
    }

    public void incrementOverdraftCount() {
        this.overdraftCount++;
    }
    
    public int getCustomerId() {
        return customerId;
    }
    
    public DebitCard getDebitCard() {
        return debitCard;
    }
    
    public void setBalance(double balance) {
        this.balance = balance;
    }

}
