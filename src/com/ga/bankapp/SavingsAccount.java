package com.ga.bankapp;

public class SavingsAccount extends Account {

    // Constructor
    public SavingsAccount(int accountId, int customerId, double balance, DebitCard debitCard) {
        super(accountId, customerId, balance, "SAVINGS", debitCard);
    }

    // Implement abstract deposit method
    @Override
    public boolean deposit(double amount) {
        if (amount > 0) {
            balance += amount;
            return true;
        }
        return false; // Invalid amount
    }

    // Implement abstract withdraw method
    @Override
    public boolean withdraw(double amount) {
        if (amount <= 0 || !isActive) {
            return false; // Invalid amount or account inactive
        }
        
        // If balance is sufficient, simple withdrawal
        if (balance >= amount) {
            balance -= amount;
            return true;
        }
        
        // Overdraft scenario
        // Calculate new balance after withdrawal and fee
        double newBalance = balance - amount - OVERDRAFT_FEE;
        
        // If account is already negative, cannot overdraw more than $100 total
        if (balance < 0) {
            // Already negative - check if this withdrawal would exceed $100 overdraft limit
            if (newBalance < -MAX_OVERDRAFT_AMOUNT) {
                return false; // Cannot overdraw more than $100 when already negative
            }
        } else {
            // Account is positive but withdrawal causes overdraft
            // Check if final balance would exceed -$100 limit
            if (newBalance < -MAX_OVERDRAFT_AMOUNT) {
                return false; // Cannot overdraw more than $100
            }
        }
        
        // Apply overdraft
        balance = newBalance;
        incrementOverdraftCount();
        
        // Check if account should be deactivated (after 2 overdrafts)
        if (overdraftCount >= MAX_OVERDRAFT_COUNT) {
            isActive = false;
        }
        
        return true;
    }

}
