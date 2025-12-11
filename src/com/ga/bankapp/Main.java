package com.ga.bankapp;

import org.mindrot.jbcrypt.BCrypt;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    private static List<User> users = new ArrayList<>();
    private static Scanner scanner = new Scanner(System.in);
    
    // Fraud detection: Track failed login attempts per user ID
    private static Map<Integer, Integer> failedAttempts = new HashMap<>();
    
    // Fraud detection: Track lockout times (user ID -> lockout timestamp in milliseconds)
    private static Map<Integer, Long> lockoutTimes = new HashMap<>();
    
    // Constants for fraud detection
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCKOUT_DURATION_MS = 60000; // 1 minute in milliseconds
    
    // Daily transaction limits tracking
    // Map: accountId -> DailyLimits object
    private static Map<Integer, DailyLimits> dailyLimits = new HashMap<>();
    
    /**
     * Inner class to track daily transaction amounts per account
     */
    private static class DailyLimits {
        LocalDate date;
        double dailyWithdraw = 0;
        double dailyDeposit = 0;
        double dailyTransfer = 0;
        double dailyTransferOwn = 0;
        
        DailyLimits() {
            this.date = LocalDate.now();
        }
        
        boolean isToday() {
            return date.equals(LocalDate.now());
        }
        
        void reset() {
            date = LocalDate.now();
            dailyWithdraw = 0;
            dailyDeposit = 0;
            dailyTransfer = 0;
            dailyTransferOwn = 0;
        }
    }

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

    // Hash password using BCrypt
    static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    // Verify password against hash
    static boolean verifyPassword(String plainPassword, String hashedPassword) {
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
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
            // Always hash password before saving
            String passwordToSave = hashPassword(user.getPassword());
            
            String content = user.getId() + "," + 
                           user.getFirstName() + "," + 
                           user.getLastName() + "," + 
                           passwordToSave + "," + 
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
                                Customer customer = new Customer(id, firstName, lastName, password);
                                users.add(customer);
                                // Load accounts for this customer
                                loadCustomerAccounts(customer);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading users: " + e.getMessage());
        }
    }
    
    // Load accounts for a customer
    private static void loadCustomerAccounts(Customer customer) {
        try {
            File accountsDir = new File("data/accounts");
            if (!accountsDir.exists()) {
                return;
            }
            
            File[] files = accountsDir.listFiles();
            if (files == null) {
                return;
            }
            
            // Read each account file
            for (File file : files) {
                if (file.getName().endsWith(".enc") && file.getName().startsWith("Account-")) {
                    List<String> lines = FileService.readEncryptedFile(file.getPath());
                    if (!lines.isEmpty()) {
                        // Parse: accountId,customerId,accountType,balance,isActive,overdraftCount,cardType
                        String[] parts = lines.get(0).split(",");
                        if (parts.length >= 7) {
                            int accountId = Integer.parseInt(parts[0]);
                            int customerId = Integer.parseInt(parts[1]);
                            
                            // Only load if this account belongs to this customer
                            if (customerId == customer.getId()) {
                                String accountType = parts[2];
                                double balance = Double.parseDouble(parts[3]);
                                boolean isActive = Boolean.parseBoolean(parts[4]);
                                int overdraftCount = Integer.parseInt(parts[5]);
                                String cardType = parts[6];
                                
                                // Create debit card
                                DebitCard debitCard;
                                if (cardType.equals("Platinum")) {
                                    debitCard = new MastercardPlatinum();
                                } else if (cardType.equals("Titanium")) {
                                    debitCard = new MastercardTitanium();
                                } else {
                                    debitCard = new MastercardStandard();
                                }
                                
                                // Create account object
                                Account account;
                                if (accountType.equals("CHECKING")) {
                                    account = new CheckingAccount(accountId, customerId, balance, debitCard);
                                } else {
                                    account = new SavingsAccount(accountId, customerId, balance, debitCard);
                                }
                                
                                // Set account properties
                                account.setActive(isActive);
                                account.setOverdraftCount(overdraftCount);
                                
                                // Add to customer (check for duplicates first)
                                boolean alreadyExists = false;
                                for (Account existingAccount : customer.getAccounts()) {
                                    if (existingAccount.getAccountId() == accountId) {
                                        alreadyExists = true;
                                        break;
                                    }
                                }
                                if (!alreadyExists) {
                                    customer.addAccount(account);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading accounts: " + e.getMessage());
        }
    }

    // Login method with fraud detection
    private static void login() {
        System.out.print("Enter user ID: ");
        int userId = scanner.nextInt();
        scanner.nextLine(); // Consume newline
        
        // Check if account is currently locked
        if (isAccountLocked(userId)) {
            long remainingTime = getRemainingLockoutTime(userId);
            System.out.println("\nAccount is locked due to multiple failed login attempts.");
            System.out.println("Please try again in " + (remainingTime / 1000) + " seconds.");
            return;
        }
        
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
            // Successful login - reset failed attempts and remove lockout
            failedAttempts.remove(userId);
            lockoutTimes.remove(userId);
            
            System.out.println("\nLogin successful!");
            System.out.println("Welcome, " + foundUser.getFirstName() + " " + foundUser.getLastName() + "!");
            
            // Recognize role and show appropriate menu
            if (foundUser.getRole() == 'B') {
                bankerMenu();
            } else if (foundUser.getRole() == 'C') {
                customerMenu((Customer) foundUser);
            }
        } else {
            // Failed login - record attempt
            recordFailedAttempt(userId);
        }
    }
    
    /**
     * Check if account is currently locked
     */
    private static boolean isAccountLocked(int userId) {
        if (!lockoutTimes.containsKey(userId)) {
            return false; // No lockout
        }
        
        long lockoutTime = lockoutTimes.get(userId);
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lockoutTime;
        
        // If lockout period has passed, remove from map
        if (elapsed >= LOCKOUT_DURATION_MS) {
            lockoutTimes.remove(userId);
            failedAttempts.remove(userId); // Also reset attempt counter
            return false; // No longer locked
        }
        
        return true; // Still locked
    }
    
    /**
     * Get remaining lockout time in milliseconds
     */
    private static long getRemainingLockoutTime(int userId) {
        if (!lockoutTimes.containsKey(userId)) {
            return 0;
        }
        
        long lockoutTime = lockoutTimes.get(userId);
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lockoutTime;
        long remaining = LOCKOUT_DURATION_MS - elapsed;
        
        return remaining > 0 ? remaining : 0;
    }
    
    /**
     * Record a failed login attempt and lock account if threshold reached
     */
    private static void recordFailedAttempt(int userId) {
        int attempts = failedAttempts.getOrDefault(userId, 0) + 1;
        failedAttempts.put(userId, attempts);
        
        int remainingAttempts = MAX_FAILED_ATTEMPTS - attempts;
        
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            // Lock the account for 1 minute
            lockoutTimes.put(userId, System.currentTimeMillis());
            failedAttempts.put(userId, 0); // Reset counter (will be cleared after lockout)
            
            System.out.println("\nLogin failed! Invalid user ID or password.");
            System.out.println("Account locked for 1 minute due to " + MAX_FAILED_ATTEMPTS + " failed login attempts.");
            System.out.println("Please try again after 1 minute.");
        } else {
            System.out.println("\nLogin failed! Invalid user ID or password.");
            System.out.println(remainingAttempts + " attempt(s) remaining before account lockout.");
        }
    }
    
    // Banker menu
    private static void bankerMenu() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n[Banker Menu]");
            System.out.println("1. Add New Customer");
            System.out.println("2. Create Account for Customer");
            System.out.println("3. Logout");
            System.out.print("Choose an option: ");
            
            int choice = scanner.nextInt();
            scanner.nextLine();
            
            switch (choice) {
                case 1:
                    addNewCustomer();
                    break;
                case 2:
                    createAccountForCustomer();
                    break;
                case 3:
                    System.out.println("Logged out successfully.");
                    inMenu = false;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
    
    // Customer menu
    private static void customerMenu(Customer customer) {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n[Customer Menu]");
            System.out.println("1. View Accounts");
            System.out.println("2. Withdraw Money");
            System.out.println("3. Deposit Money");
            System.out.println("4. Transfer Money");
            System.out.println("5. View Transaction History");
            System.out.println("6. View Account Statement");
            System.out.println("7. Generate PDF Statement");
            System.out.println("8. Logout");
            System.out.print("Choose an option: ");
            
            int choice = scanner.nextInt();
            scanner.nextLine();
            
            switch (choice) {
                case 1:
                    viewAccounts(customer);
                    break;
                case 2:
                    withdrawMoney(customer);
                    break;
                case 3:
                    depositMoney(customer);
                    break;
                case 4:
                    transferMoney(customer);
                    break;
                case 5:
                    viewTransactionHistory(customer);
                    break;
                case 6:
                    viewAccountStatement(customer);
                    break;
                case 7:
                    generatePDFStatement(customer);
                    break;
                case 8:
                    System.out.println("Logged out successfully.");
                    inMenu = false;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
    
    // View customer accounts
    private static void viewAccounts(Customer customer) {
        System.out.println("\n[Your Accounts]");
        List<Account> accounts = customer.getAccounts();
        
        if (accounts.isEmpty()) {
            System.out.println("You have no accounts yet. Please ask a banker to create one.");
            return;
        }
        
        for (Account account : accounts) {
            System.out.println("\nAccount ID: " + account.getAccountId());
            System.out.println("Type: " + account.getAccountType());
            System.out.println("Balance: $" + String.format("%.2f", account.getBalance()));
            System.out.println("Card: " + account.getDebitCard().getCardType());
            System.out.println("Status: " + (account.isActive() ? "Active" : "Deactivated"));
        }
    }
    
    // Withdraw money
    private static void withdrawMoney(Customer customer) {
        System.out.println("\n[Withdraw Money]");
        
        Account account = selectAccount(customer);
        if (account == null) {
            return;
        }
        
        if (!account.isActive()) {
            System.out.println("This account is deactivated. Please resolve negative balance first.");
            return;
        }
        
        System.out.print("Enter amount to withdraw: $");
        double amount = scanner.nextDouble();
        scanner.nextLine();
        
        if (amount <= 0) {
            System.out.println("Invalid amount!");
            return;
        }
        
        // Check daily withdraw limit
        try {
            checkDailyWithdrawLimit(account, amount);
        } catch (Exception e) {
            System.out.println("\nError: " + e.getMessage());
            return;
        }
        
        // Perform withdrawal
        if (account.withdraw(amount)) {
            // Update daily limit tracking
            updateDailyWithdraw(account.getAccountId(), amount);
            // Save account after transaction
            saveAccount(account);
            
            // Record transaction
            recordTransaction(account, "WITHDRAW", amount, account.getBalance());
            
            System.out.println("\nWithdrawal successful!");
            System.out.println("New balance: $" + String.format("%.2f", account.getBalance()));
        } else {
            System.out.println("Withdrawal failed! Insufficient funds or invalid amount.");
        }
    }
    
    // Deposit money
    private static void depositMoney(Customer customer) {
        System.out.println("\n[Deposit Money]");
        
        Account account = selectAccount(customer);
        if (account == null) {
            return;
        }
        
        System.out.print("Enter amount to deposit: $");
        double amount = scanner.nextDouble();
        scanner.nextLine();
        
        if (amount <= 0) {
            System.out.println("Invalid amount!");
            return;
        }
        
        // Check daily deposit limit (regular deposit, not own account)
        try {
            checkDailyDepositLimit(account, amount, false);
        } catch (Exception e) {
            System.out.println("\nError: " + e.getMessage());
            return;
        }
        
        // Perform deposit
        if (account.deposit(amount)) {
            // Update daily limit tracking
            updateDailyDeposit(account.getAccountId(), amount);
            // Save account after transaction
            saveAccount(account);
            
            // Record transaction
            recordTransaction(account, "DEPOSIT", amount, account.getBalance());
            
            System.out.println("\nDeposit successful!");
            System.out.println("New balance: $" + String.format("%.2f", account.getBalance()));
        } else {
            System.out.println("Deposit failed!");
        }
    }
    
    // Transfer money
    private static void transferMoney(Customer customer) {
        System.out.println("\n[Transfer Money]");
        
        // Select source account
        System.out.println("Select source account:");
        Account fromAccount = selectAccount(customer);
        if (fromAccount == null) {
            return;
        }
        
        if (!fromAccount.isActive()) {
            System.out.println("This account is deactivated. Please resolve negative balance first.");
            return;
        }
        
        System.out.print("Enter amount to transfer: $");
        double amount = scanner.nextDouble();
        scanner.nextLine();
        
        if (amount <= 0) {
            System.out.println("Invalid amount!");
            return;
        }
        
        if (amount > fromAccount.getBalance()) {
            System.out.println("Insufficient funds!");
            return;
        }
        
        // Choose transfer type
        System.out.println("\nTransfer to:");
        System.out.println("1. My own account");
        System.out.println("2. Another customer's account");
        System.out.print("Choice: ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        if (choice == 1) {
            // Transfer to own account
            System.out.println("Select destination account:");
            Account toAccount = selectAccount(customer);
            if (toAccount == null || toAccount.getAccountId() == fromAccount.getAccountId()) {
                System.out.println("Invalid destination account!");
                return;
            }
            
            // Check daily transfer limit (own account)
            try {
                checkDailyTransferLimit(fromAccount, amount, true);
            } catch (Exception e) {
                System.out.println("\nError: " + e.getMessage());
                return;
            }
            
            // Perform transfer
            fromAccount.withdraw(amount);
            toAccount.deposit(amount);
            
            // Update daily limit tracking (own account transfer)
            updateDailyTransferOwn(fromAccount.getAccountId(), amount);
            
            // Save both accounts
            saveAccount(fromAccount);
            saveAccount(toAccount);
            
            // Record transactions
            recordTransaction(fromAccount, "TRANSFER", amount, fromAccount.getBalance(), toAccount.getAccountId());
            recordTransaction(toAccount, "TRANSFER", amount, toAccount.getBalance(), fromAccount.getAccountId());
            
            System.out.println("\nTransfer successful!");
            System.out.println("From Account " + fromAccount.getAccountId() + ": $" + String.format("%.2f", fromAccount.getBalance()));
            System.out.println("To Account " + toAccount.getAccountId() + ": $" + String.format("%.2f", toAccount.getBalance()));
            
        } else if (choice == 2) {
            // Transfer to another customer
            System.out.print("Enter destination customer ID: ");
            int toCustomerId = scanner.nextInt();
            scanner.nextLine();
            
            Customer toCustomer = findCustomerById(toCustomerId);
            if (toCustomer == null) {
                System.out.println("Customer not found!");
                return;
            }
            
            // Reload accounts for destination customer to ensure we have latest data
            loadCustomerAccounts(toCustomer);
            
            if (toCustomer.getAccounts().isEmpty()) {
                System.out.println("Destination customer has no accounts!");
                return;
            }
            
            // If multiple accounts, let them choose
            Account toAccount;
            if (toCustomer.getAccounts().size() == 1) {
                toAccount = toCustomer.getAccounts().get(0);
            } else {
                System.out.println("Select destination account:");
                toAccount = selectAccount(toCustomer);
                if (toAccount == null) {
                    return;
                }
            }
            
            // Check daily transfer limit (to other customer)
            try {
                checkDailyTransferLimit(fromAccount, amount, false);
            } catch (Exception e) {
                System.out.println("\nError: " + e.getMessage());
                return;
            }
            
            // Perform transfer
            fromAccount.withdraw(amount);
            toAccount.deposit(amount);
            
            // Update daily limit tracking (regular transfer)
            updateDailyTransfer(fromAccount.getAccountId(), amount);
            
            // Save both accounts
            saveAccount(fromAccount);
            saveAccount(toAccount);
            
            // Record transactions
            recordTransaction(fromAccount, "TRANSFER", amount, fromAccount.getBalance(), toAccount.getAccountId());
            recordTransaction(toAccount, "TRANSFER", amount, toAccount.getBalance(), fromAccount.getAccountId());
            
            System.out.println("\nTransfer successful!");
            System.out.println("From Account " + fromAccount.getAccountId() + ": $" + String.format("%.2f", fromAccount.getBalance()));
            System.out.println("To Account " + toAccount.getAccountId() + ": $" + String.format("%.2f", toAccount.getBalance()));
        } else {
            System.out.println("Invalid choice!");
        }
    }
    
    // Select account from customer's accounts
    private static Account selectAccount(Customer customer) {
        List<Account> accounts = customer.getAccounts();
        
        if (accounts.isEmpty()) {
            System.out.println("You have no accounts!");
            return null;
        }
        
        if (accounts.size() == 1) {
            return accounts.get(0);
        }
        
        // Show accounts and let user choose
        System.out.println("\nSelect account:");
        for (int i = 0; i < accounts.size(); i++) {
            Account acc = accounts.get(i);
            System.out.println((i + 1) + ". Account " + acc.getAccountId() + " (" + acc.getAccountType() + ") - Balance: $" + String.format("%.2f", acc.getBalance()));
        }
        System.out.print("Choice: ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        if (choice < 1 || choice > accounts.size()) {
            System.out.println("Invalid choice!");
            return null;
        }
        
        return accounts.get(choice - 1);
    }
    
    // View transaction history
    private static void viewTransactionHistory(Customer customer) {
        System.out.println("\n[Transaction History]");
        
        List<Transaction> transactions = loadTransactions(customer.getId());
        
        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }
        
        System.out.println("\nRecent transactions:");
        for (Transaction transaction : transactions) {
            System.out.println(transaction.toString());
        }
    }
    
    // View detailed account statement
    private static void viewAccountStatement(Customer customer) {
        System.out.println("\n[Account Statement]");
        
        // Select account
        Account account = selectAccount(customer);
        if (account == null) {
            return;
        }
        
        // Load all transactions for this customer
        List<Transaction> allTransactions = loadTransactions(customer.getId());
        
        // Filter transactions for this specific account
        List<Transaction> accountTransactions = new ArrayList<>();
        for (Transaction transaction : allTransactions) {
            if (transaction.getAccountId() == account.getAccountId()) {
                accountTransactions.add(transaction);
            }
        }
        
        // Display account statement
        System.out.println("\n[Account Statement]");
        System.out.println("\nAccount Information:");
        System.out.println("Account ID: " + account.getAccountId());
        System.out.println("Account Type: " + account.getAccountType());
        System.out.println("Debit Card: " + account.getDebitCard().getCardType());
        System.out.println("Status: " + (account.isActive() ? "Active" : "Deactivated"));
        System.out.println("Current Balance: $" + String.format("%.2f", account.getBalance()));
        
        System.out.println("\nTransaction History:");
        
        if (accountTransactions.isEmpty()) {
            System.out.println("No transactions found for this account.");
        } else {
            System.out.println("\nDate & Time          | Type      | Amount      | Balance After");
            System.out.println("------------------------------------------------------------");
            
            for (Transaction transaction : accountTransactions) {
                String dateTime = transaction.getFormattedDateTime();
                String type = transaction.getType();
                String amount = "$" + String.format("%.2f", transaction.getAmount());
                String balance = "$" + String.format("%.2f", transaction.getPostBalance());
                
                // Format with proper spacing
                System.out.printf("%-20s | %-9s | %-11s | %s", dateTime, type, amount, balance);
                
                // Show recipient for transfers
                if (transaction.getRecipientAccountId() != null) {
                    System.out.print(" | To Account: " + transaction.getRecipientAccountId());
                }
                System.out.println();
            }
            
            System.out.println("------------------------------------------------------------");
            System.out.println("Total Transactions: " + accountTransactions.size());
        }
    }
    
    // Generate PDF account statement
    private static void generatePDFStatement(Customer customer) {
        System.out.println("\n[Generate PDF Statement]");
        
        // Select account
        Account account = selectAccount(customer);
        if (account == null) {
            return;
        }
        
        // Load all transactions for this customer
        List<Transaction> allTransactions = loadTransactions(customer.getId());
        
        // Filter transactions for this specific account
        List<Transaction> accountTransactions = new ArrayList<>();
        for (Transaction transaction : allTransactions) {
            if (transaction.getAccountId() == account.getAccountId()) {
                accountTransactions.add(transaction);
            }
        }
        
        try {
            // Create PDF document
            PDDocument document = new PDDocument();
            PDPage page = new PDPage();
            document.addPage(page);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            
            float yPos = 750;
            float lineHeight = 20;
            float pageWidth = 612; // Standard PDF page width in points
            
            // Header - ACME Bank (centered)
            String headerText = "[ACME Bank]";
            // Approximate text width for size 16 font: ~120 points
            // Center position: (pageWidth - textWidth) / 2
            float headerX = (pageWidth - 120) / 2; // Approximately centered
            contentStream.newLineAtOffset(headerX, yPos);
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 16);
            contentStream.showText(headerText);
            yPos -= lineHeight * 2;
            
            // Title (back to left margin)
            contentStream.newLineAtOffset(-headerX + 50, -lineHeight * 2);
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            contentStream.showText("ACCOUNT STATEMENT");
            yPos -= lineHeight * 2;
            
            // Account Information
            contentStream.newLineAtOffset(0, -lineHeight * 2);
            contentStream.showText("Account Information:");
            yPos -= lineHeight;
            
            contentStream.newLineAtOffset(0, -lineHeight);
            contentStream.showText("Account ID: " + account.getAccountId());
            yPos -= lineHeight;
            
            contentStream.newLineAtOffset(0, -lineHeight);
            contentStream.showText("Account Type: " + account.getAccountType());
            yPos -= lineHeight;
            
            contentStream.newLineAtOffset(0, -lineHeight);
            contentStream.showText("Debit Card: " + account.getDebitCard().getCardType());
            yPos -= lineHeight;
            
            contentStream.newLineAtOffset(0, -lineHeight);
            contentStream.showText("Status: " + (account.isActive() ? "Active" : "Deactivated"));
            yPos -= lineHeight;
            
            contentStream.newLineAtOffset(0, -lineHeight);
            contentStream.showText("Current Balance: $" + String.format("%.2f", account.getBalance()));
            yPos -= lineHeight * 2;
            
            // Transaction History
            contentStream.newLineAtOffset(0, -lineHeight);
            contentStream.showText("Transaction History:");
            yPos -= lineHeight;
            
            if (accountTransactions.isEmpty()) {
                contentStream.newLineAtOffset(0, -lineHeight);
                contentStream.showText("No transactions found for this account.");
            } else {
                contentStream.newLineAtOffset(0, -lineHeight);
                contentStream.showText("Date & Time | Type | Amount | Balance After");
                yPos -= lineHeight;
                
                for (Transaction transaction : accountTransactions) {
                    if (yPos < 50) {
                        contentStream.endText();
                        contentStream.close();
                        page = new PDPage();
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        contentStream.beginText();
                        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        contentStream.newLineAtOffset(50, 750);
                        yPos = 750;
                    }
                    
                    String dateTime = transaction.getFormattedDateTime();
                    String type = transaction.getType();
                    String amount = "$" + String.format("%.2f", transaction.getAmount());
                    String balance = "$" + String.format("%.2f", transaction.getPostBalance());
                    String line = dateTime + " | " + type + " | " + amount + " | " + balance;
                    if (transaction.getRecipientAccountId() != null) {
                        line += " | To: " + transaction.getRecipientAccountId();
                    }
                    
                    contentStream.newLineAtOffset(0, -lineHeight);
                    contentStream.showText(line.replace("\n", "").replace("\r", ""));
                    yPos -= lineHeight;
                }
                
                contentStream.newLineAtOffset(0, -lineHeight);
                contentStream.showText("Total Transactions: " + accountTransactions.size());
            }
            
            contentStream.endText();
            contentStream.close();
            
            // Create pdf directory if it doesn't exist
            String pdfDir = "data/pdf";
            File pdfDirectory = new File(pdfDir);
            if (!pdfDirectory.exists()) {
                pdfDirectory.mkdirs();
            }
            
            // Save PDF with timestamp to avoid overwriting
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String timestamp = LocalDateTime.now().format(formatter);
            String fileName = "AccountStatement_" + account.getAccountId() + "_" + timestamp + ".pdf";
            String fullPath = pdfDir + "/" + fileName;
            
            document.save(fullPath);
            document.close();
            
            // Get absolute path for display
            String absolutePath = new File(fullPath).getAbsolutePath();
            
            System.out.println("\nPDF statement generated successfully!");
            System.out.println("File saved in: " + absolutePath);
            
        } catch (IOException e) {
            System.out.println("Error generating PDF: " + e.getMessage());
        }
    }
    
    // Record transaction to file
    private static void recordTransaction(Account account, String type, double amount, double postBalance) {
        recordTransaction(account, type, amount, postBalance, null);
    }
    
    // Record transaction to file (with recipient account for transfers)
    private static void recordTransaction(Account account, String type, double amount, double postBalance, Integer recipientAccountId) {
        try {
            // Generate transaction ID
            int transactionId = generateTransactionId();
            
            // Create transaction
            Transaction transaction;
            if (recipientAccountId != null) {
                transaction = new Transaction(transactionId, account.getAccountId(), account.getCustomerId(), 
                                             type, Math.abs(amount), postBalance, recipientAccountId);
            } else {
                transaction = new Transaction(transactionId, account.getAccountId(), account.getCustomerId(), 
                                             type, Math.abs(amount), postBalance);
            }
            
            // Save to file
            String fileName = "data/transactions/Transactions-" + account.getCustomerId() + ".enc";
            
            // Format: transactionId,accountId,customerId,type,amount,dateTime,postBalance,recipientAccountId
            String content = transaction.getTransactionId() + "," +
                           transaction.getAccountId() + "," +
                           transaction.getCustomerId() + "," +
                           transaction.getType() + "," +
                           transaction.getAmount() + "," +
                           transaction.getFormattedDateTime() + "," +
                           transaction.getPostBalance() + "," +
                           (transaction.getRecipientAccountId() != null ? transaction.getRecipientAccountId() : "");
            
            // Append to file
            FileService.appendToEncryptedFile(fileName, content);
        } catch (Exception e) {
            System.out.println("Error recording transaction: " + e.getMessage());
        }
    }
    
    // Load transactions for a customer
    private static List<Transaction> loadTransactions(int customerId) {
        List<Transaction> transactions = new ArrayList<>();
        
        try {
            String fileName = "data/transactions/Transactions-" + customerId + ".enc";
            List<String> lines = FileService.readEncryptedFile(fileName);
            
            for (String line : lines) {
                // Parse: transactionId,accountId,customerId,type,amount,dateTime,postBalance,recipientAccountId
                String[] parts = line.split(",");
                if (parts.length >= 7) {
                    int transactionId = Integer.parseInt(parts[0]);
                    int accountId = Integer.parseInt(parts[1]);
                    int custId = Integer.parseInt(parts[2]);
                    String type = parts[3];
                    double amount = Double.parseDouble(parts[4]);
                    double postBalance = Double.parseDouble(parts[6]);
                    
                    Transaction transaction;
                    // Check if there's a recipient account ID (for transfers)
                    if (parts.length >= 8 && !parts[7].isEmpty() && type.equals("TRANSFER")) {
                        int recipientId = Integer.parseInt(parts[7]);
                        transaction = new Transaction(transactionId, accountId, custId, type, amount, postBalance, recipientId);
                    } else {
                        transaction = new Transaction(transactionId, accountId, custId, type, amount, postBalance);
                    }
                    transactions.add(transaction);
                }
            }
        } catch (Exception e) {
            // File might not exist yet
        }
        
        return transactions;
    }
    
    // Generate transaction ID
    private static int generateTransactionId() {
        // Use timestamp + random to avoid collisions
        // In production, would use a proper ID generator or database sequence
        return (int)(System.currentTimeMillis() % 1000000) + (int)(Math.random() * 1000);
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
    
    // Create account for customer (Banker function)
    private static void createAccountForCustomer() {
        System.out.println("\n[Create Account for Customer]");
        
        // Find customer
        System.out.print("Enter customer ID: ");
        int customerId = scanner.nextInt();
        scanner.nextLine();
        
        Customer customer = findCustomerById(customerId);
        if (customer == null) {
            System.out.println("Customer not found!");
            return;
        }
        
        System.out.println("Customer: " + customer.getFirstName() + " " + customer.getLastName());
        
        // Choose account type
        System.out.println("\nSelect account type:");
        System.out.println("1. Checking Account");
        System.out.println("2. Savings Account");
        System.out.print("Choice: ");
        int accountChoice = scanner.nextInt();
        scanner.nextLine();
        
        String accountType;
        if (accountChoice == 1) {
            accountType = "CHECKING";
        } else if (accountChoice == 2) {
            accountType = "SAVINGS";
        } else {
            System.out.println("Invalid choice!");
            return;
        }
        
        // Check if customer already has this account type
        for (Account acc : customer.getAccounts()) {
            if (acc.getAccountType().equals(accountType)) {
                System.out.println("Customer already has a " + accountType + " account!");
                return;
            }
        }
        
        // Get initial balance
        System.out.print("Enter initial balance: $");
        double initialBalance = scanner.nextDouble();
        scanner.nextLine();
        
        if (initialBalance < 0) {
            System.out.println("Initial balance cannot be negative!");
            return;
        }
        
        // Choose debit card type
        System.out.println("\nSelect debit card type:");
        System.out.println("1. Mastercard Platinum");
        System.out.println("2. Mastercard Titanium");
        System.out.println("3. Mastercard Standard");
        System.out.print("Choice: ");
        int cardChoice = scanner.nextInt();
        scanner.nextLine();
        
        DebitCard debitCard;
        if (cardChoice == 1) {
            debitCard = new MastercardPlatinum();
        } else if (cardChoice == 2) {
            debitCard = new MastercardTitanium();
        } else {
            debitCard = new MastercardStandard();
        }
        
        // Generate account ID
        int accountId = generateAccountId();
        
        // Create account
        Account newAccount;
        if (accountType.equals("CHECKING")) {
            newAccount = new CheckingAccount(accountId, customerId, initialBalance, debitCard);
        } else {
            newAccount = new SavingsAccount(accountId, customerId, initialBalance, debitCard);
        }
        
        // Add account to customer
        customer.addAccount(newAccount);
        
        // Save account to file
        saveAccount(newAccount);
        
        System.out.println("\nAccount created successfully!");
        System.out.println("Account ID: " + accountId);
        System.out.println("Account Type: " + accountType);
        System.out.println("Initial Balance: $" + initialBalance);
        System.out.println("Debit Card: " + debitCard.getCardType());
    }
    
    // Find customer by ID
    private static Customer findCustomerById(int customerId) {
        for (User user : users) {
            if (user instanceof Customer && user.getId() == customerId) {
                return (Customer) user;
            }
        }
        return null;
    }
    
    // Generate new account ID
    private static int generateAccountId() {
        // Start from 50000 for accounts
        int maxId = 50000;
        
        // Check all customers' accounts in memory
        for (User user : users) {
            if (user instanceof Customer) {
                Customer customer = (Customer) user;
                for (Account acc : customer.getAccounts()) {
                    if (acc.getAccountId() > maxId) {
                        maxId = acc.getAccountId();
                    }
                }
            }
        }
        
        // Also check account files
        try {
            File accountsDir = new File("data/accounts");
            if (accountsDir.exists()) {
                File[] files = accountsDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().endsWith(".enc") && file.getName().startsWith("Account-")) {
                            // Extract ID from filename: Account-50001.enc
                            String idStr = file.getName().replace("Account-", "").replace(".enc", "");
                            try {
                                int fileId = Integer.parseInt(idStr);
                                if (fileId > maxId) {
                                    maxId = fileId;
                                }
                            } catch (NumberFormatException e) {
                                // Skip invalid filenames
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // If error
        }
        
        return maxId + 1;
    }
    
    // Check daily withdraw limit
    private static void checkDailyWithdrawLimit(Account account, double amount) throws Exception {
        if (account.getDebitCard() == null) {
            throw new Exception("Account does not have a debit card");
        }
        
        DailyLimits limits = getDailyLimits(account.getAccountId());
        double newTotal = limits.dailyWithdraw + amount;
        double limit = account.getDebitCard().getWithdrawLimitPerDay();
        
        if (newTotal > limit) {
            throw new Exception("Daily withdrawal limit exceeded. Limit: $" + 
                String.format("%.2f", limit) + ". Already used: $" + 
                String.format("%.2f", limits.dailyWithdraw) + ". Remaining: $" + 
                String.format("%.2f", limit - limits.dailyWithdraw));
        }
    }
    
    // Check daily deposit limit
    private static void checkDailyDepositLimit(Account account, double amount, boolean isOwnAccount) throws Exception {
        if (account.getDebitCard() == null) {
            throw new Exception("Account does not have a debit card");
        }
        
        DailyLimits limits = getDailyLimits(account.getAccountId());
        double limit = isOwnAccount ? 
            account.getDebitCard().getDepositLimitPerDayOwnAccount() :
            account.getDebitCard().getDepositLimitPerDay();
        
        double newTotal = limits.dailyDeposit + amount;
        
        if (newTotal > limit) {
            throw new Exception("Daily deposit limit exceeded. Limit: $" + 
                String.format("%.2f", limit) + ". Already used: $" + 
                String.format("%.2f", limits.dailyDeposit) + ". Remaining: $" + 
                String.format("%.2f", limit - limits.dailyDeposit));
        }
    }
    
    // Check daily transfer limit
    private static void checkDailyTransferLimit(Account account, double amount, boolean isOwnAccount) throws Exception {
        if (account.getDebitCard() == null) {
            throw new Exception("Account does not have a debit card");
        }
        
        DailyLimits limits = getDailyLimits(account.getAccountId());
        double limit = isOwnAccount ?
            account.getDebitCard().getTransferLimitPerDayOwnAccount() :
            account.getDebitCard().getTransferLimitPerDay();
        
        double currentTotal = isOwnAccount ? limits.dailyTransferOwn : limits.dailyTransfer;
        double newTotal = currentTotal + amount;
        
        if (newTotal > limit) {
            throw new Exception("Daily transfer limit exceeded. Limit: $" + 
                String.format("%.2f", limit) + ". Already used: $" + 
                String.format("%.2f", currentTotal) + ". Remaining: $" + 
                String.format("%.2f", limit - currentTotal));
        }
    }
    
    // Get or create daily limits tracker
    private static DailyLimits getDailyLimits(int accountId) {
        DailyLimits limits = dailyLimits.get(accountId);
        if (limits == null || !limits.isToday()) {
            limits = new DailyLimits();
            dailyLimits.put(accountId, limits);
        }
        return limits;
    }
    
    // Update daily withdraw amount
    private static void updateDailyWithdraw(int accountId, double amount) {
        DailyLimits limits = getDailyLimits(accountId);
        limits.dailyWithdraw += amount;
    }
    
    // Update daily deposit amount
    private static void updateDailyDeposit(int accountId, double amount) {
        DailyLimits limits = getDailyLimits(accountId);
        limits.dailyDeposit += amount;
    }
    
    // Update daily transfer amount (regular)
    private static void updateDailyTransfer(int accountId, double amount) {
        DailyLimits limits = getDailyLimits(accountId);
        limits.dailyTransfer += amount;
    }
    
    // Update daily transfer amount (own account)
    private static void updateDailyTransferOwn(int accountId, double amount) {
        DailyLimits limits = getDailyLimits(accountId);
        limits.dailyTransferOwn += amount;
    }
    
    // Save account to encrypted file
    // Format: accountId,customerId,accountType,balance,isActive,overdraftCount,cardType
    private static void saveAccount(Account account) {
        try {
            String fileName = "data/accounts/Account-" + account.getAccountId() + ".enc";
            
            String content = account.getAccountId() + "," +
                            account.getCustomerId() + "," +
                            account.getAccountType() + "," +
                            account.getBalance() + "," +
                            account.isActive() + "," +
                            account.getOverdraftCount() + "," +
                            account.getDebitCard().getCardType();
            
            FileService.writeEncryptedFile(fileName, content);
        } catch (Exception e) {
            System.out.println("Error saving account: " + e.getMessage());
        }
    }
}