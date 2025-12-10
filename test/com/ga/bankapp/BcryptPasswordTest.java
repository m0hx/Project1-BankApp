package com.ga.bankapp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test to verify BCrypt password hashing is working correctly
 */
@DisplayName("Password Hashing Tests")
class BCryptPasswordTest {

    @Test
    @DisplayName("All user passwords should be hashed with BCrypt")
    void testAllPasswordsAreHashed() {
        // Check user files to see if passwords are hashed
        String[] userFiles = {
                "data/users/Banker-Mohammed_Nasser-10001.enc",
                "data/users/Customer-First_Customer-10002.enc"
        };

        for (String filePath : userFiles) {
            List<String> lines = FileService.readEncryptedFile(filePath);
            assertFalse(lines.isEmpty(), "User file should not be empty: " + filePath);

            String line = lines.get(0);
            String[] parts = line.split(",");
            assertTrue(parts.length >= 4, "User file should have at least 4 fields: " + filePath);

            String password = parts[3];

            // Verify password is hashed (BCrypt hashes start with $2a$ or $2b$)
            assertTrue(
                    password.startsWith("$2a$") || password.startsWith("$2b$"),
                    "Password should be hashed with BCrypt. File: " + filePath +
                            " User: " + parts[1] + " " + parts[2]
            );

            // Verify hash length (BCrypt hashes are typically 60 characters)
            assertTrue(
                    password.length() >= 50,
                    "BCrypt hash should be at least 50 characters long. File: " + filePath
            );
        }
    }

    @Test
    @DisplayName("BCrypt hash verification should work correctly")
    void testBCryptHashVerification() {
        // Test that BCrypt can hash and verify passwords
        String plainPassword = "testPassword123";
        String hashedPassword = Main.hashPassword(plainPassword);

        // Verify hash format
        assertTrue(
                hashedPassword.startsWith("$2a$") || hashedPassword.startsWith("$2b$"),
                "Hashed password should start with $2a$ or $2b$"
        );

        // Verify password can be checked
        assertTrue(
                Main.verifyPassword(plainPassword, hashedPassword),
                "BCrypt should verify correct password"
        );

        // Verify wrong password fails
        assertFalse(
                Main.verifyPassword("wrongPassword", hashedPassword),
                "BCrypt should reject incorrect password"
        );
    }

    @Test
    @DisplayName("No plain text passwords should exist")
    void testNoPlainTextPasswords() {
        String[] userFiles = {
                "data/users/Banker-Mohammed_Nasser-10001.enc",
                "data/users/Customer-First_Customer-10002.enc"
        };

        for (String filePath : userFiles) {
            List<String> lines = FileService.readEncryptedFile(filePath);
            if (!lines.isEmpty()) {
                String line = lines.get(0);
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    String password = parts[3];

                    // Plain text passwords are typically short and don't start with $2a$ or $2b$
                    assertFalse(
                            password.length() < 20 && !password.startsWith("$2a$") && !password.startsWith("$2b$"),
                            "Password appears to be plain text. File: " + filePath
                    );
                }
            }
        }
    }
}

