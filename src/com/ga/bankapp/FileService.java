package com.ga.bankapp;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class FileService {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int KEY_LENGTH = 256;
    private static final String DATA_DIR = "data/";
    private static final String KEY_FILE = DATA_DIR + "key.dat";

    private static SecretKey secretKey;

    // Initialize encryption key (load or create)
    static {
        try {
            Path keyPath = Paths.get(KEY_FILE);
            if (Files.exists(keyPath)) {
                // Load existing key
                byte[] keyBytes = Files.readAllBytes(keyPath);
                secretKey = new SecretKeySpec(keyBytes, "AES");
            } else {
                // Generate new key
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                keyGenerator.init(KEY_LENGTH);
                secretKey = keyGenerator.generateKey();
                // Save key for future use
                Files.createDirectories(Paths.get(DATA_DIR));
                Files.write(keyPath, secretKey.getEncoded());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption key", e);
        }
    }

    // Encrypt data
    private static byte[] encrypt(byte[] plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");

        // Generate IV (Initialization Vector)
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);
        byte[] ciphertext = cipher.doFinal(plaintext);

        // Prepend IV to ciphertext
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        byteBuffer.put(iv);
        byteBuffer.put(ciphertext);
        return byteBuffer.array();
    }

    // Decrypt data
    private static byte[] decrypt(byte[] ciphertext) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");

        // Extract IV
        ByteBuffer byteBuffer = ByteBuffer.wrap(ciphertext);
        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);
        byte[] encryptedData = new byte[byteBuffer.remaining()];
        byteBuffer.get(encryptedData);

        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);
        return cipher.doFinal(encryptedData);
    }

    // Read encrypted file and return as list of lines
    public static List<String> readEncryptedFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return new ArrayList<>(); // Return empty list if file doesn't exist
            }

            byte[] encryptedData = Files.readAllBytes(path);
            byte[] decryptedData = decrypt(encryptedData);
            String content = new String(decryptedData);

            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            return lines;
        } catch (Exception e) {
            System.out.println("Error reading file: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Write encrypted file from list of lines
    public static void writeEncryptedFile(String filePath, List<String> lines) {
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());

            StringBuilder content = new StringBuilder();
            for (String line : lines) {
                content.append(line).append("\n");
            }

            byte[] plaintext = content.toString().getBytes();
            byte[] encryptedData = encrypt(plaintext);
            Files.write(path, encryptedData);
        } catch (Exception e) {
            System.out.println("Error writing file: " + e.getMessage());
        }
    }

    // Write encrypted file from single string
    public static void writeEncryptedFile(String filePath, String content) {
        List<String> lines = new ArrayList<>();
        lines.add(content);
        writeEncryptedFile(filePath, lines);
    }

    // Append line to encrypted file
    public static void appendToEncryptedFile(String filePath, String line) {
        List<String> lines = readEncryptedFile(filePath);
        lines.add(line);
        writeEncryptedFile(filePath, lines);
    }

    // Check if file exists
    public static boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }
}
