package com.ga.bankapp;

import org.mindrot.jbcrypt.BCrypt;

public class Banker implements User {
    // Fields
    private int id;
    private String firstName;
    private String lastName;
    private String password;
    private char role; // 'B' for Banker

    // Constructor
    public Banker(int id, String firstName, String lastName, String password) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.role = 'B'; // Banker role
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
        // Username (fn+ln)
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
    
    // Set password (used when loading from file or updating password)
    public void setPassword(String password) {
        this.password = password;
    }
}
