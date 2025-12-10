package com.ga.bankapp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static List<User> users = new ArrayList<>();
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // Load users from encrypted files (if they exist)
        loadUsers();
        
        // If no users exist, create first banker
        if (users.isEmpty()) {
            createFirstBanker();
        }

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

    // Save user to encrypted file
    // Format: ID,firstName,lastName,password,balance,role (like data.txt)
    private static void saveUser(User user) {
        try {
            // Create filename: Customer-Name-ID.enc or Banker-Name-ID.enc
            String fileName;
            if (user.getRole() == 'B') {
                fileName = "data/users/Banker-" + user.getFirstName() + "_" + user.getLastName() + "-" + user.getId() + ".enc";
            } else {
                fileName = "data/users/Customer-" + user.getFirstName() + "_" + user.getLastName() + "-" + user.getId() + ".enc";
            }
            
            // Format: ID,firstName,lastName,password,balance,role
            // balance is 0 for now (we'll add accounts later)
            String content = user.getId() + "," + 
                           user.getFirstName() + "," + 
                           user.getLastName() + "," + 
                           user.getPassword() + "," + 
                           "0," + // balance placeholder
                           user.getRole();
            
            FileService.writeEncryptedFile(fileName, content);
        } catch (Exception e) {
            System.out.println("Error saving user: " + e.getMessage());
        }
    }
    
    // Load all users from encrypted files
    private static void loadUsers() {
        try {
            File usersDir = new File("data/users");
            if (!usersDir.exists()) {
                return; // No users folder, start fresh
            }
            
            File[] files = usersDir.listFiles();
            if (files == null) {
                return;
            }
            
            // Read each encrypted file
            for (File file : files) {
                if (file.getName().endsWith(".enc")) {
                    List<String> lines = FileService.readEncryptedFile(file.getPath());
                    if (!lines.isEmpty()) {
                        // Parse the line: ID,firstName,lastName,password,balance,role
                        String[] parts = lines.get(0).split(",");
                        if (parts.length >= 6) {
                            int id = Integer.parseInt(parts[0]);
                            String firstName = parts[1];
                            String lastName = parts[2];
                            String password = parts[3];
                            char role = parts[5].charAt(0);
                            
                            // Create user object
                            if (role == 'B') {
                                users.add(new Banker(id, firstName, lastName, password));
                            } else if (role == 'C') {
                                users.add(new Customer(id, firstName, lastName, password));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading users: " + e.getMessage());
        }
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
            
            // Recognize role and show appropriate menu
            if (foundUser.getRole() == 'B') {
                bankerMenu();
            } else if (foundUser.getRole() == 'C') {
                customerMenu();
            }
        } else {
            System.out.println("Login failed! Invalid user ID or password.");
        }
    }
    
    // Banker menu
    private static void bankerMenu() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n[Banker Menu]");
            System.out.println("1. Add New Customer");
            System.out.println("2. Logout");
            System.out.print("Choose an option: ");
            
            int choice = scanner.nextInt();
            scanner.nextLine();
            
            switch (choice) {
                case 1:
                    addNewCustomer();
                    break;
                case 2:
                    System.out.println("Logged out successfully.");
                    inMenu = false;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
    
    // Customer menu (placeholder for now)
    private static void customerMenu() {
        System.out.println("\n[Customer Menu]");
        System.out.println("(Customer features coming soon)");
        System.out.println("Press Enter to logout...");
        scanner.nextLine();
    }
    
    // Add new customer (Banker function)
    private static void addNewCustomer() {
        System.out.println("\n[Add New Customer]");
        
        // Get customer details
        System.out.print("Enter first name: ");
        String firstName = scanner.nextLine();
        
        System.out.print("Enter last name: ");
        String lastName = scanner.nextLine();
        
        System.out.print("Set password for customer: ");
        String password = scanner.nextLine();
        
        // Generate customer ID (find max ID + 1)
        int newId = generateCustomerId();
        
        // Create customer
        Customer newCustomer = new Customer(newId, firstName, lastName, password);
        users.add(newCustomer);
        saveUser(newCustomer);
        
        System.out.println("\nCustomer created successfully!");
        System.out.println("Customer ID: " + newId);
        System.out.println("Name: " + firstName + " " + lastName);
    }
    
    // Generate new customer ID
    private static int generateCustomerId() {
        int maxId = 10000; // Start from 10000
        for (User user : users) {
            if (user.getId() > maxId) {
                maxId = user.getId();
            }
        }
        return maxId + 1;
    }
    
    // Create first banker if no users exist
    private static void createFirstBanker() {
        System.out.println("\n[Welcome to ACME Bank]");
        System.out.println("No users found. Let's create the first banker account.");
        System.out.println();
        
        System.out.print("Enter banker first name: ");
        String firstName = scanner.nextLine();
        
        System.out.print("Enter banker last name: ");
        String lastName = scanner.nextLine();
        
        System.out.print("Set password for banker: ");
        String password = scanner.nextLine();
        
        // Create banker with ID 10001
        Banker banker = new Banker(10001, firstName, lastName, password);
        users.add(banker);
        saveUser(banker);
        
        System.out.println("\nBanker account created successfully!");
        System.out.println("Banker ID: 10001");
        System.out.println("Name: " + firstName + " " + lastName);
        System.out.println("\nYou can now login with ID: 10001");
    }
}