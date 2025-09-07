package com.example.modbus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog for changing the data logger password
 */
public class ChangePasswordDialog extends JDialog {
    private JPasswordField currentPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;
    private JCheckBox disablePasswordCheckBox;
    private final DataLoggerPasswordManager passwordManager;
    
    public ChangePasswordDialog(Window parent) {
        super(parent, "Change Data Logger Password", ModalityType.APPLICATION_MODAL);
        this.passwordManager = DataLoggerPasswordManager.getInstance();
        
        initializeUI();
        setLocationRelativeTo(parent);
    }
    
    private void initializeUI() {
        setSize(450, 350);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Change Data Logger Password");
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        JLabel subtitleLabel = new JLabel("Update password protection settings");
        subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        subtitleLabel.setForeground(Color.GRAY);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Password fields panel
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Current password
        JLabel currentLabel = new JLabel("Current Password:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        fieldsPanel.add(currentLabel, gbc);
        
        currentPasswordField = new JPasswordField(20);
        currentPasswordField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        fieldsPanel.add(currentPasswordField, gbc);
        
        // New password
        JLabel newLabel = new JLabel("New Password:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        fieldsPanel.add(newLabel, gbc);
        
        newPasswordField = new JPasswordField(20);
        newPasswordField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        fieldsPanel.add(newPasswordField, gbc);
        
        // Confirm password
        JLabel confirmLabel = new JLabel("Confirm Password:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        fieldsPanel.add(confirmLabel, gbc);
        
        confirmPasswordField = new JPasswordField(20);
        confirmPasswordField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        fieldsPanel.add(confirmPasswordField, gbc);
        
        // Disable password checkbox
        disablePasswordCheckBox = new JCheckBox("Disable password protection");
        disablePasswordCheckBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        disablePasswordCheckBox.addActionListener(e -> {
            boolean disabled = disablePasswordCheckBox.isSelected();
            newPasswordField.setEnabled(!disabled);
            confirmPasswordField.setEnabled(!disabled);
            if (disabled) {
                newPasswordField.setText("");
                confirmPasswordField.setText("");
            }
        });
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        fieldsPanel.add(disablePasswordCheckBox, gbc);
        
        // Info panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Information"));
        
        JTextArea infoText = new JTextArea(4, 30);
        infoText.setEditable(false);
        infoText.setOpaque(false);
        infoText.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        infoText.setText("• Default password is: 91147\n" +
                        "• Password is case-sensitive\n" +
                        "• Leave current password empty if using default\n" +
                        "• Disabling password removes all protection");
        infoText.setForeground(Color.DARK_GRAY);
        
        infoPanel.add(infoText, BorderLayout.CENTER);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        fieldsPanel.add(infoPanel, gbc);
        
        mainPanel.add(fieldsPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton saveButton = new JButton("Save Changes");
        saveButton.setPreferredSize(new Dimension(120, 30));
        saveButton.addActionListener(e -> savePassword());
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(80, 30));
        cancelButton.addActionListener(e -> dispose());
        
        JButton resetButton = new JButton("Reset to Default");
        resetButton.setPreferredSize(new Dimension(120, 30));
        resetButton.addActionListener(e -> resetToDefault());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(resetButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // Set focus to current password field
        SwingUtilities.invokeLater(() -> currentPasswordField.requestFocusInWindow());
    }
    
    private void savePassword() {
        String currentPassword = new String(currentPasswordField.getPassword());
        String newPassword = new String(newPasswordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        
        // Check if password protection is being disabled
        if (disablePasswordCheckBox.isSelected()) {
            if (!passwordManager.verifyPassword(currentPassword)) {
                JOptionPane.showMessageDialog(this, 
                    "Current password is incorrect.", 
                    "Authentication Failed", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            passwordManager.setPasswordEnabled(false);
            JOptionPane.showMessageDialog(this, 
                "Password protection has been disabled.", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
            dispose();
            return;
        }
        
        // Validate current password
        if (!passwordManager.verifyPassword(currentPassword)) {
            JOptionPane.showMessageDialog(this, 
                "Current password is incorrect.", 
                "Authentication Failed", 
                JOptionPane.ERROR_MESSAGE);
            currentPasswordField.setText("");
            currentPasswordField.requestFocusInWindow();
            return;
        }
        
        // Validate new password
        if (newPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "New password cannot be empty.", 
                "Invalid Password", 
                JOptionPane.ERROR_MESSAGE);
            newPasswordField.requestFocusInWindow();
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, 
                "New password and confirmation do not match.", 
                "Password Mismatch", 
                JOptionPane.ERROR_MESSAGE);
            confirmPasswordField.setText("");
            confirmPasswordField.requestFocusInWindow();
            return;
        }
        
        // Save new password
        if (passwordManager.setPassword(newPassword)) {
            JOptionPane.showMessageDialog(this, 
                "Password has been changed successfully.", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Failed to save new password.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void resetToDefault() {
        String currentPassword = new String(currentPasswordField.getPassword());
        
        if (!passwordManager.verifyPassword(currentPassword)) {
            JOptionPane.showMessageDialog(this, 
                "Current password is incorrect.", 
                "Authentication Failed", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to reset the password to the default (91147)?", 
            "Confirm Reset", 
            JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            passwordManager.resetToDefaultPassword();
            JOptionPane.showMessageDialog(this, 
                "Password has been reset to default (91147).", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
            dispose();
        }
    }
    
    /**
     * Show the dialog
     */
    public void showDialog() {
        setVisible(true);
    }
    
    /**
     * Static method to show change password dialog
     */
    public static void showChangePasswordDialog(Window parent) {
        ChangePasswordDialog dialog = new ChangePasswordDialog(parent);
        dialog.showDialog();
    }
}
