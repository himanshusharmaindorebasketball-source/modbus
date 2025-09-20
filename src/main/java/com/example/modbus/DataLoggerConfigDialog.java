package com.example.modbus;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

/**
 * Configuration dialog for Energy Data Logger
 */
public class DataLoggerConfigDialog extends JDialog {
    private EnergyDataLogger.DataLoggerConfig config;
    private EnergyDataLogger logger;
    
    // UI Components
    private JCheckBox enabledCheckBox;
    private JSpinner intervalSpinner;
    private JSpinner bufferSizeSpinner;
    private JCheckBox logToFileCheckBox;
    private JCheckBox logToDatabaseCheckBox;
    private JComboBox<String> formatComboBox;
    private JCheckBox includeCalculatedCheckBox;
    private JList<String> channelsList;
    private DefaultListModel<String> channelsListModel;
    private JTextField databaseUrlField;
    private JTextField databaseUsernameField;
    private JPasswordField databasePasswordField;
    
    public DataLoggerConfigDialog(Window parent) {
        super(parent, "Data Logger Configuration", ModalityType.APPLICATION_MODAL);
        this.logger = EnergyDataLogger.getInstance();
        // Reload configuration to ensure we have the latest settings
        this.logger.reloadConfig();
        this.config = logger.getConfig();
        
        initializeUI();
        loadConfigToUI();
    }
    
    private void initializeUI() {
        setSize(1100, 700);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // General Settings Tab
        tabbedPane.addTab("General", createGeneralSettingsPanel());
        
        // Channel Selection Tab
        tabbedPane.addTab("Channels", createChannelSelectionPanel());
        
        // Database Settings Tab
        tabbedPane.addTab("Database", createDatabaseSettingsPanel());
        
        // Log Files Tab
        tabbedPane.addTab("Log Files", createLogFilesPanel());
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Buttons panel
        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        bottomButtonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        
        JButton saveButton = new JButton("Save Configuration");
        JButton cancelButton = new JButton("Cancel");
        
        // Set button properties for better text visibility and sizing
        Font bottomButtonFont = new Font("Arial", Font.PLAIN, 13);
        saveButton.setFont(bottomButtonFont);
        cancelButton.setFont(bottomButtonFont);
        
        // Set button sizes to ensure text fits properly - slightly smaller buttons
        Dimension saveButtonSize = new Dimension(180, 40);
        Dimension cancelButtonSize = new Dimension(100, 40);
        
        saveButton.setPreferredSize(saveButtonSize);
        saveButton.setMinimumSize(saveButtonSize);
        saveButton.setMaximumSize(saveButtonSize);
        
        cancelButton.setPreferredSize(cancelButtonSize);
        cancelButton.setMinimumSize(cancelButtonSize);
        cancelButton.setMaximumSize(cancelButtonSize);
        
        saveButton.addActionListener(e -> saveConfiguration());
        cancelButton.addActionListener(e -> dispose());
        
        bottomButtonPanel.add(saveButton);
        bottomButtonPanel.add(cancelButton);
        
        mainPanel.add(bottomButtonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private JPanel createGeneralSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("General Settings"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Enable logging
        gbc.gridx = 0; gbc.gridy = 0;
        enabledCheckBox = new JCheckBox("Enable Data Logging");
        panel.add(enabledCheckBox, gbc);
        
        // Log interval
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Log Interval (seconds):"), gbc);
        gbc.gridx = 1;
        intervalSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 3600, 1));
        panel.add(intervalSpinner, gbc);
        
        // Buffer size
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Buffer Size:"), gbc);
        gbc.gridx = 1;
        bufferSizeSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 10000, 10));
        panel.add(bufferSizeSpinner, gbc);
        
        // Log destinations
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Log Destinations:"), gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        logToFileCheckBox = new JCheckBox("Log to File");
        panel.add(logToFileCheckBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5;
        logToDatabaseCheckBox = new JCheckBox("Log to Database");
        panel.add(logToDatabaseCheckBox, gbc);
        
        // Log format
        gbc.gridx = 0; gbc.gridy = 6;
        panel.add(new JLabel("Log Format:"), gbc);
        gbc.gridx = 1;
        formatComboBox = new JComboBox<>(new String[]{"JSON", "CSV", "BOTH"});
        panel.add(formatComboBox, gbc);
        
        // Include calculated values
        gbc.gridx = 0; gbc.gridy = 7;
        includeCalculatedCheckBox = new JCheckBox("Include Calculated Values (Power, Power Factor, etc.)");
        panel.add(includeCalculatedCheckBox, gbc);
        
        // Status panel
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2;
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(new TitledBorder("Status"));
        
        JLabel statusLabel = new JLabel("Status: " + (logger.isLogging() ? "Logging Active" : "Stopped"));
        statusLabel.setForeground(logger.isLogging() ? Color.GREEN : Color.RED);
        statusPanel.add(statusLabel, BorderLayout.WEST);
        
        if (logger.isLogging()) {
            JLabel fileLabel = new JLabel("File: " + logger.getCurrentLogFile());
            statusPanel.add(fileLabel, BorderLayout.SOUTH);
        }
        
        panel.add(statusPanel, gbc);
        
        return panel;
    }
    
    private JPanel createChannelSelectionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Channel Selection"));
        
        // Get available channels from ModbusDataStore
        List<String> availableChannels = getAvailableChannels();
        
        // Available channels list
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(new TitledBorder("Available Channels"));
        leftPanel.setPreferredSize(new Dimension(380, 450));
        
        DefaultListModel<String> availableModel = new DefaultListModel<>();
        for (String channel : availableChannels) {
            availableModel.addElement(channel);
        }
        JList<String> availableList = new JList<>(availableModel);
        availableList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // Add refresh button for available channels
        JPanel availableHeaderPanel = new JPanel(new BorderLayout());
        JButton refreshAvailableButton = new JButton("Refresh");
        refreshAvailableButton.setPreferredSize(new Dimension(100, 35));
        refreshAvailableButton.setFont(new Font("Arial", Font.PLAIN, 11));
               refreshAvailableButton.addActionListener(e -> {
                   // Clear only available channels, NOT selected channels
                   availableModel.clear();
                   // DO NOT clear channelsListModel - this preserves selected channels
                   
                   // Force reload math channels and get updated list
                   MathChannelManager.clearCache();
                   List<String> updatedChannels = getAvailableChannels();
                   
                   // Populate available channels list
                   for (String channel : updatedChannels) {
                       availableModel.addElement(channel);
                   }
                   
                   System.out.println("Data Logger Configuration refreshed - " + updatedChannels.size() + " channels available");
                   System.out.println("DEBUG: Selected channels preserved: " + channelsListModel.getSize() + " channels");
               });
               
               // Auto-refresh when panel is created
               SwingUtilities.invokeLater(() -> {
                   refreshAvailableButton.doClick();
               });
        
        JLabel headerLabel = new JLabel("Available Channels (Modbus + Math)");
        headerLabel.setToolTipText("Shows all configured Modbus channels and math channels. Math channels are marked with '(Math)' suffix.");
        
        availableHeaderPanel.add(headerLabel, BorderLayout.WEST);
        availableHeaderPanel.add(refreshAvailableButton, BorderLayout.EAST);
        
        leftPanel.add(availableHeaderPanel, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(availableList), BorderLayout.CENTER);
        
        // Selected channels list
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(new TitledBorder("Selected Channels (Empty = Log All)"));
        rightPanel.setPreferredSize(new Dimension(380, 450));
        
        channelsListModel = new DefaultListModel<>();
        // Load selected channels from configuration
        System.out.println("DEBUG: About to load channels from config. ChannelsToLog size: " + config.getChannelsToLog().size());
        System.out.println("DEBUG: ChannelsToLog content: " + config.getChannelsToLog());
        
        for (String channel : config.getChannelsToLog()) {
            channelsListModel.addElement(channel);
            System.out.println("DEBUG: Added channel to model: " + channel);
        }
        System.out.println("DEBUG: Loaded " + config.getChannelsToLog().size() + " selected channels from configuration");
        System.out.println("DEBUG: Model size after loading: " + channelsListModel.getSize());
        channelsList = new JList<>(channelsListModel);
        channelsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        rightPanel.add(new JScrollPane(channelsList), BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 12, 12));
        buttonPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
        buttonPanel.setPreferredSize(new Dimension(280, 180));
        
        JButton addButton = new JButton("Add Selected");
        JButton removeButton = new JButton("Remove Selected");
        JButton addAllButton = new JButton("Add All");
        JButton clearButton = new JButton("Clear All");
        
        // Set button properties for better text visibility and sizing
        Font buttonFont = new Font("Arial", Font.PLAIN, 12);
        addButton.setFont(buttonFont);
        removeButton.setFont(buttonFont);
        addAllButton.setFont(buttonFont);
        clearButton.setFont(buttonFont);
        
        // Set button sizes to ensure text fits properly - slightly smaller buttons
        Dimension buttonSize = new Dimension(160, 45);
        addButton.setPreferredSize(buttonSize);
        addButton.setMinimumSize(buttonSize);
        addButton.setMaximumSize(buttonSize);
        
        removeButton.setPreferredSize(buttonSize);
        removeButton.setMinimumSize(buttonSize);
        removeButton.setMaximumSize(buttonSize);
        
        addAllButton.setPreferredSize(buttonSize);
        addAllButton.setMinimumSize(buttonSize);
        addAllButton.setMaximumSize(buttonSize);
        
        clearButton.setPreferredSize(buttonSize);
        clearButton.setMinimumSize(buttonSize);
        clearButton.setMaximumSize(buttonSize);
        
        addButton.addActionListener(e -> {
            List<String> selected = availableList.getSelectedValuesList();
            for (String channel : selected) {
                if (!channelsListModel.contains(channel)) {
                    channelsListModel.addElement(channel);
                }
            }
        });
        
        removeButton.addActionListener(e -> {
            List<String> selected = channelsList.getSelectedValuesList();
            for (String channel : selected) {
                channelsListModel.removeElement(channel);
            }
        });
        
        addAllButton.addActionListener(e -> {
            channelsListModel.clear();
            for (int i = 0; i < availableModel.getSize(); i++) {
                channelsListModel.addElement(availableModel.getElementAt(i));
            }
        });
        
        clearButton.addActionListener(e -> channelsListModel.clear());
        
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(addAllButton);
        buttonPanel.add(clearButton);
        
        // Main panel layout
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.EAST);
        
        panel.add(mainPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createDatabaseSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Database Settings"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Database URL
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Database URL:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        databaseUrlField = new JTextField(30);
        panel.add(databaseUrlField, gbc);
        
        // Username
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        databaseUsernameField = new JTextField(20);
        panel.add(databaseUsernameField, gbc);
        
        // Password
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        databasePasswordField = new JPasswordField(20);
        panel.add(databasePasswordField, gbc);
        
        // Info label
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JLabel infoLabel = new JLabel("<html><i>Note: Database logging is not yet implemented.<br>This is a placeholder for future functionality.</i></html>");
        infoLabel.setForeground(Color.GRAY);
        panel.add(infoLabel, gbc);
        
        return panel;
    }
    
    private JPanel createLogFilesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Log Files Management"));
        
        // Log files list
        DefaultListModel<String> filesModel = new DefaultListModel<>();
        List<String> logFiles = logger.getLogFiles();
        for (String file : logFiles) {
            filesModel.addElement(file);
        }
        JList<String> filesList = new JList<>(filesModel);
        filesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane scrollPane = new JScrollPane(filesList);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton refreshButton = new JButton("Refresh");
        JButton exportButton = new JButton("Export to CSV");
        JButton exportAllButton = new JButton("Export All to CSV");
        JButton deleteButton = new JButton("Delete Selected");
        
        refreshButton.addActionListener(e -> {
            filesModel.clear();
            List<String> files = logger.getLogFiles();
            for (String file : files) {
                filesModel.addElement(file);
            }
        });
        
        exportButton.addActionListener(e -> {
            String selectedFile = filesList.getSelectedValue();
            if (selectedFile != null) {
                exportLogFile(selectedFile);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a log file to export.", "No Selection", JOptionPane.WARNING_MESSAGE);
            }
        });
        
        exportAllButton.addActionListener(e -> exportAllLogFiles());
        
        deleteButton.addActionListener(e -> {
            String selectedFile = filesList.getSelectedValue();
            if (selectedFile != null) {
                int result = JOptionPane.showConfirmDialog(this, 
                    "Are you sure you want to delete " + selectedFile + "?", 
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    deleteLogFile(selectedFile);
                    filesModel.removeElement(selectedFile);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a log file to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            }
        });
        
        buttonPanel.add(refreshButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(exportAllButton);
        buttonPanel.add(deleteButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private List<String> getAvailableChannels() {
        java.util.List<String> availableChannels = new java.util.ArrayList<>();
        
        // Force reload math channel configurations (clear cache and reload)
        MathChannelManager.clearCache();
        MathChannelManager.loadConfigs();
        
        // Add Modbus channels from ModbusConfigManager
        try {
            List<ModbusConfigManager.ModbusConfig> modbusConfigs = ModbusConfigManager.loadConfig();
            for (ModbusConfigManager.ModbusConfig config : modbusConfigs) {
                String channelName = config.getChannelName();
                if (channelName != null && !channelName.trim().isEmpty()) {
                    availableChannels.add(channelName);
                } else {
                    // Generate default name if channel name is empty
                    availableChannels.add("Channel_" + config.getAddress());
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading Modbus configurations: " + e.getMessage());
        }
        
        // Add math channels from MathChannelManager
        List<MathChannelConfig> mathChannels = MathChannelManager.getConfigs();
        for (MathChannelConfig config : mathChannels) {
            if (config.isEnabled()) {
                String channelName = config.getChannelName();
                if (channelName != null && !channelName.trim().isEmpty()) {
                    // Add "(Math)" suffix to distinguish from Modbus channels
                    availableChannels.add(channelName + " (Math)");
                }
            }
        }
        
        // Sort channels alphabetically for better organization
        availableChannels.sort(String.CASE_INSENSITIVE_ORDER);
        
        return availableChannels;
    }
    
    private void loadConfigToUI() {
        enabledCheckBox.setSelected(config.isEnabled());
        intervalSpinner.setValue(config.getLogIntervalSeconds());
        bufferSizeSpinner.setValue(config.getBufferSize());
        logToFileCheckBox.setSelected(config.isLogToFile());
        logToDatabaseCheckBox.setSelected(config.isLogToDatabase());
        formatComboBox.setSelectedItem(config.getLogFormat());
        includeCalculatedCheckBox.setSelected(config.isIncludeCalculatedValues());
        databaseUrlField.setText(config.getDatabaseUrl());
        databaseUsernameField.setText(config.getDatabaseUsername());
        databasePasswordField.setText(config.getDatabasePassword());
        
        // Note: Selected channels will be loaded after UI is fully initialized
        // This is handled in createChannelSelectionPanel()
    }
    
    private void saveConfiguration() {
        config.setEnabled(enabledCheckBox.isSelected());
        config.setLogIntervalSeconds((Integer) intervalSpinner.getValue());
        config.setBufferSize((Integer) bufferSizeSpinner.getValue());
        config.setLogToFile(logToFileCheckBox.isSelected());
        config.setLogToDatabase(logToDatabaseCheckBox.isSelected());
        config.setLogFormat((String) formatComboBox.getSelectedItem());
        config.setIncludeCalculatedValues(includeCalculatedCheckBox.isSelected());
        config.setDatabaseUrl(databaseUrlField.getText());
        config.setDatabaseUsername(databaseUsernameField.getText());
        config.setDatabasePassword(new String(databasePasswordField.getPassword()));
        
        // Update channels list
        config.getChannelsToLog().clear();
        for (int i = 0; i < channelsListModel.getSize(); i++) {
            config.getChannelsToLog().add(channelsListModel.getElementAt(i));
        }
        
        logger.saveConfig();
        JOptionPane.showMessageDialog(this, "Configuration saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }
    
    
    private void exportLogFile(String fileName) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new java.io.File(fileName.replace(".json", ".csv")));
        fileChooser.setDialogTitle("Export Log File to CSV");
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = fileChooser.getSelectedFile();
            String outputPath = selectedFile.getAbsolutePath();
            
            // Ensure the file has .csv extension
            if (!outputPath.toLowerCase().endsWith(".csv")) {
                outputPath += ".csv";
            }
            
            try {
                // Show progress dialog (non-modal to avoid deadlocks)
                final JDialog progressDialog = new JDialog(this, "Exporting Energy Data...", false);
                progressDialog.setSize(450, 200);
                progressDialog.setLocationRelativeTo(this);
                progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                
                JPanel progressPanel = new JPanel(new BorderLayout());
                progressPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                
                final JLabel statusLabel = new JLabel("Exporting " + fileName + " to CSV...");
                statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
                progressPanel.add(statusLabel, BorderLayout.CENTER);
                
                JLabel waitLabel = new JLabel("Please wait while the file is being processed...");
                waitLabel.setHorizontalAlignment(SwingConstants.CENTER);
                waitLabel.setForeground(Color.GRAY);
                progressPanel.add(waitLabel, BorderLayout.SOUTH);
                
                // Add cancel button
                JButton cancelButton = new JButton("Cancel");
                JPanel buttonPanel = new JPanel(new FlowLayout());
                buttonPanel.add(cancelButton);
                progressPanel.add(buttonPanel, BorderLayout.SOUTH);
                
                progressDialog.add(progressPanel);
                
                // Start export in a separate thread
                final String finalOutputPath = outputPath;
                final String finalFileName = fileName;
                final SwingWorker<Void, String> exportWorker = new SwingWorker<Void, String>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        try {
                            publish("Starting export...");
                            System.out.println("DEBUG: Starting export of " + finalFileName + " to " + finalOutputPath);
                            
                            // Add timeout mechanism
                            long startTime = System.currentTimeMillis();
                            long timeout = 30000; // 30 seconds timeout
                            
                            publish("Reading log file...");
                            logger.exportLogFileToCSV(finalFileName, finalOutputPath);
                            
                            long endTime = System.currentTimeMillis();
                            if (endTime - startTime > timeout) {
                                throw new RuntimeException("Export timed out after " + timeout + "ms");
                            }
                            
                            publish("Export completed successfully!");
                            System.out.println("DEBUG: Export completed successfully");
                            return null;
                        } catch (Exception e) {
                            System.err.println("DEBUG: Export failed with error: " + e.getMessage());
                            e.printStackTrace();
                            throw e;
                        }
                    }
                    
                    @Override
                    protected void process(java.util.List<String> chunks) {
                        if (!chunks.isEmpty()) {
                            String latestStatus = chunks.get(chunks.size() - 1);
                            statusLabel.setText(latestStatus);
                            System.out.println("DEBUG: Status update: " + latestStatus);
                        }
                    }
                    
                    @Override
                    protected void done() {
                        System.out.println("DEBUG: Export worker done() called");
                        SwingUtilities.invokeLater(() -> {
                            progressDialog.dispose();
                            try {
                                get(); // Check for exceptions
                                JOptionPane.showMessageDialog(DataLoggerConfigDialog.this, 
                                    "Log file exported successfully to:\n" + finalOutputPath, 
                                    "Export Successful", 
                                    JOptionPane.INFORMATION_MESSAGE);
                            } catch (Exception e) {
                                String errorMsg = e.getMessage();
                                if (e.getCause() != null) {
                                    errorMsg = e.getCause().getMessage();
                                }
                                JOptionPane.showMessageDialog(DataLoggerConfigDialog.this, 
                                    "Error exporting log file:\n" + errorMsg, 
                                    "Export Error", 
                                    JOptionPane.ERROR_MESSAGE);
                                e.printStackTrace(); // Print full stack trace for debugging
                            }
                        });
                    }
                };
                
                // Cancel button action
                cancelButton.addActionListener(e -> {
                    System.out.println("DEBUG: Cancel button clicked");
                    exportWorker.cancel(true);
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(DataLoggerConfigDialog.this, 
                        "Export cancelled by user.", 
                        "Export Cancelled", 
                        JOptionPane.INFORMATION_MESSAGE);
                });
                
                progressDialog.setVisible(true);
                exportWorker.execute();
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    "Error exporting log file:\n" + e.getMessage(), 
                    "Export Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void exportAllLogFiles() {
        List<String> logFiles = logger.getLogFiles();
        if (logFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No log files found to export.", "No Files", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new java.io.File("all_energy_data.csv"));
        fileChooser.setDialogTitle("Export All Log Files to CSV");
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = fileChooser.getSelectedFile();
            String outputPath = selectedFile.getAbsolutePath();
            
            // Ensure the file has .csv extension
            if (!outputPath.toLowerCase().endsWith(".csv")) {
                outputPath += ".csv";
            }
            
            try {
                // Show progress dialog (non-modal to avoid deadlocks)
                final JDialog progressDialog = new JDialog(this, "Exporting All Energy Data...", false);
                progressDialog.setSize(500, 220);
                progressDialog.setLocationRelativeTo(this);
                progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                
                JPanel progressPanel = new JPanel(new BorderLayout());
                progressPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                
                final JLabel statusLabel = new JLabel("Exporting " + logFiles.size() + " log files to CSV...");
                statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
                progressPanel.add(statusLabel, BorderLayout.CENTER);
                
                JLabel waitLabel = new JLabel("This may take a moment for large files...");
                waitLabel.setHorizontalAlignment(SwingConstants.CENTER);
                waitLabel.setForeground(Color.GRAY);
                progressPanel.add(waitLabel, BorderLayout.SOUTH);
                
                // Add cancel button
                JButton cancelButton = new JButton("Cancel");
                JPanel buttonPanel = new JPanel(new FlowLayout());
                buttonPanel.add(cancelButton);
                progressPanel.add(buttonPanel, BorderLayout.SOUTH);
                
                progressDialog.add(progressPanel);
                
                // Start export in a separate thread
                final String finalOutputPath = outputPath;
                final int fileCount = logFiles.size();
                final SwingWorker<Void, String> exportWorker = new SwingWorker<Void, String>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        try {
                            publish("Starting export of " + fileCount + " files...");
                            System.out.println("DEBUG: Starting export of " + fileCount + " files to " + finalOutputPath);
                            
                            // Add timeout mechanism
                            long startTime = System.currentTimeMillis();
                            long timeout = 60000; // 60 seconds timeout for all files
                            
                            publish("Processing log files...");
                            logger.exportToCSV(finalOutputPath, null, null);
                            
                            long endTime = System.currentTimeMillis();
                            if (endTime - startTime > timeout) {
                                throw new RuntimeException("Export timed out after " + timeout + "ms");
                            }
                            
                            publish("All files exported successfully!");
                            System.out.println("DEBUG: All files export completed successfully");
                            return null;
                        } catch (Exception e) {
                            System.err.println("DEBUG: All files export failed with error: " + e.getMessage());
                            e.printStackTrace();
                            throw e;
                        }
                    }
                    
                    @Override
                    protected void process(java.util.List<String> chunks) {
                        if (!chunks.isEmpty()) {
                            String latestStatus = chunks.get(chunks.size() - 1);
                            statusLabel.setText(latestStatus);
                            System.out.println("DEBUG: Status update: " + latestStatus);
                        }
                    }
                    
                    @Override
                    protected void done() {
                        System.out.println("DEBUG: All files export worker done() called");
                        SwingUtilities.invokeLater(() -> {
                            progressDialog.dispose();
                            try {
                                get(); // Check for exceptions
                                JOptionPane.showMessageDialog(DataLoggerConfigDialog.this, 
                                    "All log files exported successfully to:\n" + finalOutputPath + 
                                    "\n\nTotal files processed: " + fileCount, 
                                    "Export Successful", 
                                    JOptionPane.INFORMATION_MESSAGE);
                            } catch (Exception e) {
                                String errorMsg = e.getMessage();
                                if (e.getCause() != null) {
                                    errorMsg = e.getCause().getMessage();
                                }
                                JOptionPane.showMessageDialog(DataLoggerConfigDialog.this, 
                                    "Error exporting log files:\n" + errorMsg, 
                                    "Export Error", 
                                    JOptionPane.ERROR_MESSAGE);
                                e.printStackTrace(); // Print full stack trace for debugging
                            }
                        });
                    }
                };
                
                // Cancel button action
                cancelButton.addActionListener(e -> {
                    System.out.println("DEBUG: Cancel button clicked for all files export");
                    exportWorker.cancel(true);
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(DataLoggerConfigDialog.this, 
                        "Export cancelled by user.", 
                        "Export Cancelled", 
                        JOptionPane.INFORMATION_MESSAGE);
                });
                
                progressDialog.setVisible(true);
                exportWorker.execute();
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    "Error exporting log files:\n" + e.getMessage(), 
                    "Export Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void deleteLogFile(String fileName) {
        java.io.File file = new java.io.File("energy_logs/" + fileName);
        if (file.exists()) {
            if (file.delete()) {
                JOptionPane.showMessageDialog(this, "File deleted successfully!", "Delete", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete file.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
