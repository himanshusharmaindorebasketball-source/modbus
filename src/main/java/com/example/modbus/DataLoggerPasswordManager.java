package com.example.modbus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Manages password protection for the data logger functionality
 */
public class DataLoggerPasswordManager {
    private static final String PASSWORD_FILE = "datalogger_password.json";
    private static final String DEFAULT_PASSWORD = "91147";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private static DataLoggerPasswordManager instance;
    
    private PasswordConfig passwordConfig;
    
    private DataLoggerPasswordManager() {
        loadPasswordConfig();
    }
    
    public static synchronized DataLoggerPasswordManager getInstance() {
        if (instance == null) {
            instance = new DataLoggerPasswordManager();
        }
        return instance;
    }
    
    /**
     * Configuration class for password storage
     */
    private static class PasswordConfig {
        private String hashedPassword;
        private boolean passwordEnabled;
        private long lastChanged;
        
        public PasswordConfig() {
            this.passwordEnabled = true;
            this.lastChanged = System.currentTimeMillis();
        }
        
        public PasswordConfig(String hashedPassword, boolean passwordEnabled, long lastChanged) {
            this.hashedPassword = hashedPassword;
            this.passwordEnabled = passwordEnabled;
            this.lastChanged = lastChanged;
        }
        
        // Getters and setters
        public String getHashedPassword() { return hashedPassword; }
        public void setHashedPassword(String hashedPassword) { this.hashedPassword = hashedPassword; }
        
        public boolean isPasswordEnabled() { return passwordEnabled; }
        public void setPasswordEnabled(boolean passwordEnabled) { this.passwordEnabled = passwordEnabled; }
        
        public long getLastChanged() { return lastChanged; }
        public void setLastChanged(long lastChanged) { this.lastChanged = lastChanged; }
    }
    
    /**
     * Load password configuration from file
     */
    private void loadPasswordConfig() {
        try {
            if (Files.exists(Paths.get(PASSWORD_FILE))) {
                try (FileReader reader = new FileReader(PASSWORD_FILE)) {
                    passwordConfig = gson.fromJson(reader, PasswordConfig.class);
                    if (passwordConfig == null) {
                        passwordConfig = new PasswordConfig();
                    }
                }
            } else {
                // Create default configuration with default password
                passwordConfig = new PasswordConfig();
                setPassword(DEFAULT_PASSWORD);
                savePasswordConfig();
            }
        } catch (Exception e) {
            System.err.println("Error loading password config: " + e.getMessage());
            passwordConfig = new PasswordConfig();
            setPassword(DEFAULT_PASSWORD);
        }
    }
    
    /**
     * Save password configuration to file
     */
    private void savePasswordConfig() {
        try (FileWriter writer = new FileWriter(PASSWORD_FILE)) {
            gson.toJson(passwordConfig, writer);
        } catch (IOException e) {
            System.err.println("Error saving password config: " + e.getMessage());
        }
    }
    
    /**
     * Hash a password using SHA-256
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            System.err.println("Error hashing password: " + e.getMessage());
            return password; // Fallback to plain text (not recommended for production)
        }
    }
    
    /**
     * Verify if the provided password is correct
     */
    public boolean verifyPassword(String password) {
        if (!passwordConfig.isPasswordEnabled()) {
            return true; // Password protection is disabled
        }
        
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        
        String hashedInput = hashPassword(password.trim());
        return hashedInput.equals(passwordConfig.getHashedPassword());
    }
    
    /**
     * Set a new password
     */
    public boolean setPassword(String newPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return false;
        }
        
        passwordConfig.setHashedPassword(hashPassword(newPassword.trim()));
        passwordConfig.setLastChanged(System.currentTimeMillis());
        savePasswordConfig();
        return true;
    }
    
    /**
     * Change password (requires current password verification)
     */
    public boolean changePassword(String currentPassword, String newPassword) {
        if (!verifyPassword(currentPassword)) {
            return false; // Current password is incorrect
        }
        
        return setPassword(newPassword);
    }
    
    /**
     * Enable or disable password protection
     */
    public void setPasswordEnabled(boolean enabled) {
        passwordConfig.setPasswordEnabled(enabled);
        savePasswordConfig();
    }
    
    /**
     * Check if password protection is enabled
     */
    public boolean isPasswordEnabled() {
        return passwordConfig.isPasswordEnabled();
    }
    
    /**
     * Get the last time the password was changed
     */
    public long getLastPasswordChange() {
        return passwordConfig.getLastChanged();
    }
    
    /**
     * Reset to default password
     */
    public void resetToDefaultPassword() {
        setPassword(DEFAULT_PASSWORD);
    }
    
    /**
     * Check if the current password is the default password
     */
    public boolean isDefaultPassword() {
        return verifyPassword(DEFAULT_PASSWORD);
    }
}
