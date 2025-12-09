package com.ga.bankapp;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static List<User> users = new ArrayList<>();
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // Create test users -- load from file later.
        initializeTestUsers();

        // Main menu loop
        boolean running = true;
        while (running) {
            System.out.println("\n[Welcome To ACME Bank]");
            System.out.println("1. Login");
            System.out.println("2. Exit");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // newline

            switch (choice) {
                case 1:
                    login();
                    break;
                case 2:
                    System.out.println("Thank you for using ACME Bank!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
        scanner.close();
    }

    // Initialize test users (example from data.txt format)
    private static void initializeTestUsers() {
        // Banker: 10001,saad,iqbal,juagw362,1000,B
        users.add(new Banker(10001, "saad", "iqbal", "juagw362"));

        // Customer: 10003,melvin,gordon,uYWE732g4ga1,2000,C
        users.add(new Customer(10003, "melvin", "gordon", "uYWE732g4ga1"));
    }

    // Login method
    private static void login() {
        System.out.print("Enter user ID: ");
        int userId = scanner.nextInt();
        scanner.nextLine(); // Consume newline
        
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        
        // Find user by ID
        User foundUser = null;
        for (User user : users) {
            if (user.getId() == userId) {
                foundUser = user;
                break;
            }
        }
        
        // Check if user exists and password is correct
        if (foundUser != null && foundUser.login(password)) {
            System.out.println("\nLogin successful!");
            System.out.println("Welcome, " + foundUser.getFirstName() + " " + foundUser.getLastName() + "!");
            
            // Recognize role
            if (foundUser.getRole() == 'B') {
                System.out.println("Role: Banker");
                // Banker menu will go here later
            } else if (foundUser.getRole() == 'C') {
                System.out.println("Role: Customer");
                // Customer menu will go here later
            }
        } else {
            System.out.println("Login failed! Invalid user ID or password.");
        }
    }
}