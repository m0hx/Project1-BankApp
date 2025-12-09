package com.ga.bankapp;

public interface User {
    // Get user ID
    int getId();

    // Get first name
    String getFirstName();

    // Get last name
    String getLastName();

    // Get username
    String getUsername();

    // Get password // for login
    String getPassword();

    // Get role - 'B' for Banker, 'C' for Customer
    char getRole();

    // Login method //verify password
    boolean login(String password);
}
