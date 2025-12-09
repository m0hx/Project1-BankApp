package com.ga.bankapp;

public abstract class Account {

    // Fields
    protected int accountId;
    protected double balance;
    protected String accountType; // "CHECKING" or "SAVINGS"
    protected boolean isActive; // For overdraft deactivation
    protected int overdraftCount; // Track number of overdrafts

    // Constructor
    public Account(int accountId, double balance, String accountType) {
        this.accountId = accountId;
        this.balance = balance;
        this.accountType = accountType;
        this.isActive = true; // Account starts active
        this.overdraftCount = 0; // No overdrafts initially
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

}
