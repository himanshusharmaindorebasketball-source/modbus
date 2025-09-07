package com.example.modbus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Dialog for password input authentication
 */
public class PasswordInputDialog extends JDialog {
    private JPasswordField passwordField;
    private boolean authenticated = false;
    private final DataLoggerPasswordManager passwordManager;
    
    public PasswordInputDialog(Window parent, String title) {
        super(parent, title, ModalityType.APPLICATION_MODAL);
        this.passwordManager = DataLoggerPasswordManager.getInstance();
        
        initializeUI();
        setLocationRelativeTo(parent);
    }
    
    private void initializeUI() {
        setSize(400, 200);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Data Logger Access");
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        JLabel subtitleLabel = new JLabel("Enter password to access data logger configuration");
        subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        subtitleLabel.setForeground(Color.GRAY);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Password input panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JLabel passwordLabel = new JLabel("Password:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        inputPanel.add(passwordLabel, gbc);
        
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        inputPanel.add(passwordField, gbc);
        
        // Add key listener for Enter key
        passwordField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}
            
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    authenticate();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose();
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {}
        });
        
        mainPanel.add(inputPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton okButton = new JButton("OK");
        okButton.setPreferredSize(new Dimension(80, 30));
        okButton.addActionListener(e -> authenticate());
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(80, 30));
        cancelButton.addActionListener(e -> dispose());
        
        JButton changePasswordButton = new JButton("Change Password");
        changePasswordButton.setPreferredSize(new Dimension(120, 30));
        changePasswordButton.addActionListener(e -> openChangePasswordDialog());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(changePasswordButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // Set focus to password field
        SwingUtilities.invokeLater(() -> passwordField.requestFocusInWindow());
    }
    
    private void authenticate() {
        String password = new String(passwordField.getPassword());
        
        if (passwordManager.verifyPassword(password)) {
            authenticated = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Incorrect password. Please try again.", 
                "Authentication Failed", 
                JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        }
    }
    
    private void openChangePasswordDialog() {
        ChangePasswordDialog changeDialog = new ChangePasswordDialog(this);
        changeDialog.setVisible(true);
    }
    
    /**
     * Show the dialog and return true if authentication was successful
     */
    public boolean showDialog() {
        setVisible(true);
        return authenticated;
    }
    
    /**
     * Static method to show password dialog and return authentication result
     */
    public static boolean showPasswordDialog(Window parent) {
        PasswordInputDialog dialog = new PasswordInputDialog(parent, "Data Logger Authentication");
        return dialog.showDialog();
    }
}
