package com.ga.bankapp;

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
        // Check if password matches
        return this.password.equals(password);
    }
}
