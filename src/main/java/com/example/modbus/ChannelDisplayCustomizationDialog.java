package com.example.modbus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog for customizing channel display settings
 */
public class ChannelDisplayCustomizationDialog extends JDialog {
    private ChannelDisplayConfig config;
    private boolean confirmed = false;
    private String selectedChannel;
    private String[] availableChannels;
    
    // UI Components
    private JComboBox<String> channelCombo;
    private JComboBox<String> fontNameCombo;
    private JSpinner fontSizeSpinner;
    private JButton backgroundColorButton;
    private JButton fontColorButton;
    private JButton valueColorButton;
    private JLabel previewLabel;
    private JLabel previewValueLabel;
    private JButton applyButton;
    private JButton resetChannelButton;
    
    public ChannelDisplayCustomizationDialog(Frame parent, String[] availableChannels) {
        super(parent, "Channel Display Customization", true);
        this.availableChannels = availableChannels;
        
        // Load configurations
        ChannelDisplayConfigManager.loadConfigs();
        
        // Initialize with first channel's config if available
        if (availableChannels.length > 0) {
            this.selectedChannel = availableChannels[0];
            this.config = ChannelDisplayConfigManager.getConfig(selectedChannel);
        } else {
            this.config = new ChannelDisplayConfig();
        }
        
        initializeUI();
        setLocationRelativeTo(parent);
    }
    
    private void initializeUI() {
        setSize(500, 400);
        setLayout(new BorderLayout());
        
        // Main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Channel Selection
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Select Channel:"), gbc);
        channelCombo = new JComboBox<>(availableChannels);
        if (availableChannels.length > 0) {
            channelCombo.setSelectedIndex(0);
            // selectedChannel and config are already set in constructor
        }
        channelCombo.addActionListener(e -> {
            selectedChannel = (String) channelCombo.getSelectedItem();
            loadChannelConfig();
        });
        gbc.gridx = 1; gbc.gridwidth = 2;
        mainPanel.add(channelCombo, gbc);
        gbc.gridwidth = 1;
        
        // Font Name
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Font Type:"), gbc);
        String[] fontNames = {"Arial", "Times New Roman", "Courier New", "Verdana", "Tahoma", "Calibri"};
        fontNameCombo = new JComboBox<>(fontNames);
        fontNameCombo.setSelectedItem(config.getFontName());
        fontNameCombo.addActionListener(e -> {
            updatePreview();
        });
        gbc.gridx = 1; gbc.gridwidth = 2;
        mainPanel.add(fontNameCombo, gbc);
        gbc.gridwidth = 1;
        
        // Font Size
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Font Size:"), gbc);
        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(config.getFontSize(), 8, 48, 1));
        fontSizeSpinner.addChangeListener(e -> {
            updatePreview();
        });
        gbc.gridx = 1; gbc.gridwidth = 2;
        mainPanel.add(fontSizeSpinner, gbc);
        gbc.gridwidth = 1;
        
        // Background Color
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Background Color:"), gbc);
        backgroundColorButton = new JButton("Choose Color");
        backgroundColorButton.setBackground(config.getBackgroundColor());
        backgroundColorButton.addActionListener(e -> chooseColor(backgroundColorButton, "Background Color"));
        gbc.gridx = 1; gbc.gridwidth = 2;
        mainPanel.add(backgroundColorButton, gbc);
        gbc.gridwidth = 1;
        
        // Font Color
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Channel Name Color:"), gbc);
        fontColorButton = new JButton("Choose Color");
        fontColorButton.setForeground(config.getFontColor());
        fontColorButton.addActionListener(e -> chooseColor(fontColorButton, "Channel Name Color"));
        gbc.gridx = 1; gbc.gridwidth = 2;
        mainPanel.add(fontColorButton, gbc);
        gbc.gridwidth = 1;
        
        // Value Color
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Value Color:"), gbc);
        valueColorButton = new JButton("Choose Color");
        valueColorButton.setForeground(config.getValueColor());
        valueColorButton.addActionListener(e -> chooseColor(valueColorButton, "Value Color"));
        gbc.gridx = 1; gbc.gridwidth = 2;
        mainPanel.add(valueColorButton, gbc);
        gbc.gridwidth = 1;
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Preview panel
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(BorderFactory.createTitledBorder("Preview"));
        previewPanel.setPreferredSize(new Dimension(0, 120));
        
        JPanel previewCard = new JPanel(new BorderLayout());
        previewCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLUE, 2),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        previewCard.setBackground(config.getBackgroundColor());
        previewCard.setOpaque(true); // Ensure the background color is visible
        
        previewLabel = new JLabel("Channel Name", SwingConstants.CENTER);
        previewLabel.setFont(config.getFont());
        previewLabel.setForeground(config.getFontColor());
        previewCard.add(previewLabel, BorderLayout.NORTH);
        
        previewValueLabel = new JLabel("23.45", SwingConstants.CENTER);
        previewValueLabel.setFont(config.getValueFont());
        previewValueLabel.setForeground(config.getValueColor());
        previewCard.add(previewValueLabel, BorderLayout.CENTER);
        
        previewPanel.add(previewCard, BorderLayout.CENTER);
        add(previewPanel, BorderLayout.SOUTH);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        JButton resetButton = new JButton("Reset to Defaults");
        applyButton = new JButton("Apply");
        resetChannelButton = new JButton("Reset This Channel");
        
        okButton.addActionListener(e -> {
            saveCurrentConfig();
            confirmed = true;
            dispose();
        });
        
        cancelButton.addActionListener(e -> dispose());
        
        resetButton.addActionListener(e -> resetToDefaults());
        
        applyButton.addActionListener(e -> applyChanges());
        
        resetChannelButton.addActionListener(e -> resetCurrentChannel());
        
        buttonPanel.add(applyButton);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(resetChannelButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Now that all UI components are created, update them with the loaded config
        if (selectedChannel != null) {
            updateUIFromConfig();
            updatePreview();
        }
    }
    
    private void chooseColor(JButton button, String title) {
        Color currentColor;
        if (button == backgroundColorButton) {
            currentColor = button.getBackground();
        } else {
            currentColor = button.getForeground();
        }
        
        Color newColor = JColorChooser.showDialog(this, "Choose " + title, currentColor);
        if (newColor != null) {
            if (button == backgroundColorButton) {
                button.setBackground(newColor);
            } else {
                button.setForeground(newColor);
            }
            updatePreview();
        }
    }
    
    private void updateConfigFromUI() {
        // Update config with current UI values
        config.setFontName((String) fontNameCombo.getSelectedItem());
        config.setFontSize((Integer) fontSizeSpinner.getValue());
        config.setBackgroundColor(backgroundColorButton.getBackground());
        config.setFontColor(fontColorButton.getForeground());
        config.setValueColor(valueColorButton.getForeground());
    }
    
    private void updatePreview() {
        // Update preview with current UI values (not the config)
        String fontName = (String) fontNameCombo.getSelectedItem();
        int fontSize = (Integer) fontSizeSpinner.getValue();
        Color bgColor = backgroundColorButton.getBackground();
        Color fontColor = fontColorButton.getForeground();
        Color valueColor = valueColorButton.getForeground();
        
        // Create temporary font objects for preview
        Font labelFont = new Font(fontName, Font.BOLD, fontSize);
        Font valueFont = new Font(fontName, Font.BOLD, fontSize + 4);
        
        previewLabel.setFont(labelFont);
        previewLabel.setForeground(fontColor);
        previewValueLabel.setFont(valueFont);
        previewValueLabel.setForeground(valueColor);
        
        // Update preview card background
        Container previewCard = previewLabel.getParent();
        if (previewCard instanceof JPanel) {
            previewCard.setBackground(bgColor);
        }
    }
    
    private void resetToDefaults() {
        ChannelDisplayConfig defaults = new ChannelDisplayConfig();
        fontNameCombo.setSelectedItem(defaults.getFontName());
        fontSizeSpinner.setValue(defaults.getFontSize());
        backgroundColorButton.setBackground(defaults.getBackgroundColor());
        fontColorButton.setForeground(defaults.getFontColor());
        valueColorButton.setForeground(defaults.getValueColor());
        updatePreview();
    }
    
    private void loadChannelConfig() {
        if (selectedChannel != null) {
            // Get the saved configuration for this channel
            config = ChannelDisplayConfigManager.getConfig(selectedChannel);
            updateUIFromConfig();
            updatePreview();
        }
    }
    
    private void updateUIFromConfig() {
        if (fontNameCombo != null) {
            fontNameCombo.setSelectedItem(config.getFontName());
        }
        if (fontSizeSpinner != null) {
            fontSizeSpinner.setValue(config.getFontSize());
        }
        if (backgroundColorButton != null) {
            backgroundColorButton.setBackground(config.getBackgroundColor());
        }
        if (fontColorButton != null) {
            fontColorButton.setForeground(config.getFontColor());
        }
        if (valueColorButton != null) {
            valueColorButton.setForeground(config.getValueColor());
        }
    }
    
    private void saveCurrentConfig() {
        if (selectedChannel != null) {
            // Update the config with current UI values before saving
            updateConfigFromUI();
            ChannelDisplayConfigManager.setConfig(selectedChannel, config);
            ChannelDisplayConfigManager.saveConfigs();
        }
    }
    
    private void applyChanges() {
        if (selectedChannel != null) {
            // Update the config with current UI values
            updateConfigFromUI();
            
            // Save the configuration
            ChannelDisplayConfigManager.setConfig(selectedChannel, config);
            ChannelDisplayConfigManager.saveConfigs();
            
            JOptionPane.showMessageDialog(this, 
                "Settings applied to '" + selectedChannel + "'!", 
                "Settings Applied", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void resetCurrentChannel() {
        if (selectedChannel != null) {
            int result = JOptionPane.showConfirmDialog(this, 
                "Reset settings for '" + selectedChannel + "' to defaults?", 
                "Reset Channel", 
                JOptionPane.YES_NO_OPTION);
            
            if (result == JOptionPane.YES_OPTION) {
                ChannelDisplayConfigManager.removeConfig(selectedChannel);
                config = new ChannelDisplayConfig();
                updateUIFromConfig();
                updatePreview();
                JOptionPane.showMessageDialog(this, "Channel '" + selectedChannel + "' reset to defaults!");
            }
        }
    }

    public ChannelDisplayConfig getConfig() {
        return confirmed ? config : null;
    }
    
    public String getSelectedChannel() {
        return selectedChannel;
    }
}
