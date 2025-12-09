package com.ga.bankapp;

public class CheckingAccount extends Account {

    // Constructor
    public CheckingAccount(int accountId, double balance) {
        super(accountId, balance, "CHECKING");
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
        if (amount > 0 && isActive) {
            if (balance >= amount) {
                balance -= amount;
                return true;
            }
            // To-do: Overdraft logic will be added later
        }
        return false; // Invalid amount or account inactive
    }
}
