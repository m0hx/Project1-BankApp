package com.ga.bankapp;

import org.mindrot.jbcrypt.BCrypt;
import java.util.ArrayList;
import java.util.List;

public class Customer implements User {
    // Fields
    private int id;
    private String firstName;
    private String lastName;
    private String password;
    private char role; // 'C' for Customer
    private List<Account> accounts; // List of customer's accounts

    // Constructor
    public Customer(int id, String firstName, String lastName, String password) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.role = 'C'; // Customer role
        this.accounts = new ArrayList<>(); // Initialize empty list
    }

    // Implement User interface methods
    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getUsername() {
        // Username can be first name + last name
        return firstName.toLowerCase() + lastName.toLowerCase();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public char getRole() {
        return role;
    }

    @Override
    public boolean login(String password) {
        // Verify password using BCrypt
        try {
            return BCrypt.checkpw(password, this.password);
        } catch (Exception e) {
            return false;
        }
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void addAccount(Account account) {
        accounts.add(account);
    }
}
