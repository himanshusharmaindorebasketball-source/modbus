package com.example.modbus;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import com.serotonin.modbus4j.ModbusMaster;

/**
 * Extended Modbus Configuration UI with Math Channels support
 */
public class ExtendedModbusConfigUI extends JDialog {
    private JTabbedPane tabbedPane;
    
    // Regular Modbus Configuration
    private DefaultTableModel modbusTableModel;
    private List<ModbusConfigManager.ModbusConfig> modbusConfigs;
    private JTable modbusTable;
    private JTextField slaveIdField;
    private JTextField addressField;
    private JComboBox<String> dataTypeCombo;
    private JTextField channelNameField;
    private JLabel lengthInfoLabel;
    
    // Write functionality
    private JTextField writeValueField;
    private JButton writeButton;
    private JLabel writeStatusLabel;
    private JTextField passwordField;
    
    // Math Channel Configuration
    private DefaultTableModel mathTableModel;
    private List<MathChannelConfig> mathConfigs;
    private JTable mathTable;
    private JTextField mathChannelNameField;
    private JTextField expressionField;
    private JTextField descriptionField;
    private JTextField unitField;
    private JSpinner decimalPlacesSpinner;
    private JCheckBox enabledCheckBox;
    private JTextArea expressionHelpArea;
    
    // Connection manager and settings from main application
    private ModbusConnectionManager connectionManager;
    private ModbusSettings settings;
    
    public ExtendedModbusConfigUI(Frame parent) {
        super(parent, "Modbus & Math Channel Configuration", true);
        this.modbusConfigs = new ArrayList<>(ModbusConfigManager.loadConfig());
        this.mathConfigs = new ArrayList<>(MathChannelManager.getConfigs());
        initializeUI();
        loadDataToTables();
    }
    
    public ExtendedModbusConfigUI(Frame parent, ModbusConnectionManager connectionManager, ModbusSettings settings) {
        super(parent, "Modbus & Math Channel Configuration", true);
        this.connectionManager = connectionManager;
        this.settings = settings;
        this.modbusConfigs = new ArrayList<>(ModbusConfigManager.loadConfig());
        this.mathConfigs = new ArrayList<>(MathChannelManager.getConfigs());
        initializeUI();
        loadDataToTables();
    }
    
    private void initializeUI() {
        setSize(800, 600);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());
        
        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        
        // Regular Modbus Configuration Tab
        JPanel modbusPanel = createModbusConfigPanel();
        tabbedPane.addTab("Modbus Channels", modbusPanel);
        
        // Math Channel Configuration Tab
        JPanel mathPanel = createMathConfigPanel();
        tabbedPane.addTab("Math Channels", mathPanel);
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Bottom buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("Save All");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> saveAllConfigurations());
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createModbusConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Table for existing configurations
        String[] columnNames = {"Channel Name", "Slave ID", "Address", "Data Type", "Length"};
        modbusTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        modbusTable = new JTable(modbusTableModel);
        modbusTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        modbusTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedModbusConfig();
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(modbusTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Input panel for new/edit configuration
        JPanel inputPanel = createModbusInputPanel();
        panel.add(inputPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createModbusInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Channel Name
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Channel Name:"), gbc);
        channelNameField = new JTextField(15);
        gbc.gridx = 1; gbc.gridwidth = 2;
        panel.add(channelNameField, gbc);
        gbc.gridwidth = 1;
        
        // Slave ID
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Slave ID:"), gbc);
        slaveIdField = new JTextField(10);
        gbc.gridx = 1;
        panel.add(slaveIdField, gbc);
        
        // Address
        gbc.gridx = 2; gbc.gridy = row;
        panel.add(new JLabel("Address:"), gbc);
        addressField = new JTextField(10);
        gbc.gridx = 3;
        panel.add(addressField, gbc);
        
        // Data Type
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Data Type:"), gbc);
        String[] dataTypes = {"Int16", "UInt16", "Float32 (ABCD)", "Float32 (BADC)"};
        dataTypeCombo = new JComboBox<>(dataTypes);
        dataTypeCombo.addActionListener(e -> updateLengthInfo());
        gbc.gridx = 1;
        panel.add(dataTypeCombo, gbc);
        
        // Length Info
        gbc.gridx = 2; gbc.gridy = row;
        panel.add(new JLabel("Length:"), gbc);
        lengthInfoLabel = new JLabel("1 register (auto)");
        gbc.gridx = 3;
        panel.add(lengthInfoLabel, gbc);
        
        // Write functionality section
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 4;
        JPanel writePanel = new JPanel(new GridBagLayout());
        writePanel.setBorder(BorderFactory.createTitledBorder("Write to Register"));
        
        GridBagConstraints writeGbc = new GridBagConstraints();
        writeGbc.insets = new Insets(3, 3, 3, 3);
        writeGbc.anchor = GridBagConstraints.WEST;
        
        // Password field (for device unlock)
        writeGbc.gridx = 0; writeGbc.gridy = 0;
        writePanel.add(new JLabel("Device Password:"), writeGbc);
        passwordField = new JTextField(10);
        passwordField.setToolTipText("Enter password (0-9998) to unlock device for writing. Leave empty if device is already unlocked via keypad.");
        writeGbc.gridx = 1;
        writePanel.add(passwordField, writeGbc);
        
        // Write value field
        writeGbc.gridx = 0; writeGbc.gridy = 1;
        writePanel.add(new JLabel("Write Value:"), writeGbc);
        writeValueField = new JTextField(10);
        writeValueField.setText("0");
        writeGbc.gridx = 1;
        writePanel.add(writeValueField, writeGbc);
        
        // Write button
        writeButton = new JButton("Write to Register");
        writeButton.addActionListener(e -> writeToSelectedRegister());
        writeGbc.gridx = 2; writeGbc.gridy = 0; writeGbc.gridheight = 2;
        writePanel.add(writeButton, writeGbc);
        
        // Write status label
        writeStatusLabel = new JLabel("Select a writable register to enable write functionality");
        writeStatusLabel.setForeground(Color.GRAY);
        writeGbc.gridx = 0; writeGbc.gridy = 2; writeGbc.gridwidth = 3; writeGbc.gridheight = 1;
        writePanel.add(writeStatusLabel, writeGbc);
        
        panel.add(writePanel, gbc);
        
        // Buttons
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 4;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton addButton = new JButton("Add/Update");
        JButton deleteButton = new JButton("Delete");
        JButton clearButton = new JButton("Clear");
        
        addButton.addActionListener(e -> addOrUpdateModbusConfig());
        deleteButton.addActionListener(e -> deleteModbusConfig());
        clearButton.addActionListener(e -> clearModbusInputs());
        
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);
        panel.add(buttonPanel, gbc);
        
        return panel;
    }
    
    private JPanel createMathConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Table for existing math configurations
        String[] columnNames = {"Channel Name", "Expression", "Unit", "Decimals", "Enabled"};
        mathTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        mathTable = new JTable(mathTableModel);
        mathTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mathTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedMathConfig();
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(mathTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Input panel for new/edit math configuration
        JPanel inputPanel = createMathInputPanel();
        panel.add(inputPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createMathInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Channel Name
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Channel Name:"), gbc);
        mathChannelNameField = new JTextField(20);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(mathChannelNameField, gbc);
        gbc.gridwidth = 1;
        
        // Description
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Description:"), gbc);
        descriptionField = new JTextField(30);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(descriptionField, gbc);
        gbc.gridwidth = 1;
        
        // Expression
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Expression:"), gbc);
        expressionField = new JTextField(40);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(expressionField, gbc);
        gbc.gridwidth = 1;
        
        // Unit and Decimal Places
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Unit:"), gbc);
        unitField = new JTextField(10);
        gbc.gridx = 1;
        panel.add(unitField, gbc);
        
        gbc.gridx = 2; gbc.gridy = row;
        panel.add(new JLabel("Decimals:"), gbc);
        decimalPlacesSpinner = new JSpinner(new SpinnerNumberModel(2, 0, 10, 1));
        gbc.gridx = 3;
        panel.add(decimalPlacesSpinner, gbc);
        
        // Enabled checkbox
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        enabledCheckBox = new JCheckBox("Enabled", true);
        gbc.gridx = 1; gbc.gridwidth = 3;
        panel.add(enabledCheckBox, gbc);
        gbc.gridwidth = 1;
        
        // Expression Help
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0; gbc.weighty = 1.0;
        
        JPanel helpPanel = new JPanel(new BorderLayout());
        helpPanel.setBorder(BorderFactory.createTitledBorder("Expression Help"));
        
        expressionHelpArea = new JTextArea(6, 50);
        expressionHelpArea.setEditable(false);
        expressionHelpArea.setText(
            "Supported operations: +, -, *, /, ^ (power)\n" +
            "Functions: sin(), cos(), tan(), sqrt(), abs(), log(), log10(), exp()\n" +
            "Constants: pi, e\n" +
            "Examples:\n" +
            "  Voltage_R_N + Voltage_Y_N\n" +
            "  sqrt(Voltage_R_N^2 + Voltage_Y_N^2)\n" +
            "  (Voltage_R_N + Voltage_Y_N + Voltage_B_N) / 3\n" +
            "  abs(Current_A - Current_B)"
        );
        expressionHelpArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        JScrollPane helpScrollPane = new JScrollPane(expressionHelpArea);
        helpPanel.add(helpScrollPane, BorderLayout.CENTER);
        panel.add(helpPanel, gbc);
        
        // Buttons
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0; gbc.weighty = 0;
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton addButton = new JButton("Add/Update");
        JButton deleteButton = new JButton("Delete");
        JButton clearButton = new JButton("Clear");
        JButton testButton = new JButton("Test Expression");
        
        addButton.addActionListener(e -> addOrUpdateMathConfig());
        deleteButton.addActionListener(e -> deleteMathConfig());
        clearButton.addActionListener(e -> clearMathInputs());
        testButton.addActionListener(e -> testMathExpression());
        
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(testButton);
        panel.add(buttonPanel, gbc);
        
        return panel;
    }
    
    // Modbus configuration methods
    private int getLengthForDataType(String dataType) {
        switch (dataType) {
            case "Int16":
            case "UInt16":
                return 1;
            case "Float32 (ABCD)":
            case "Float32 (BADC)":
                return 2;
            default:
                return 1;
        }
    }
    
    private void updateLengthInfo() {
        String dataType = (String) dataTypeCombo.getSelectedItem();
        int length = getLengthForDataType(dataType);
        String unit = length == 1 ? "register" : "registers";
        lengthInfoLabel.setText(length + " " + unit + " (auto)");
    }
    
    private void loadDataToTables() {
        loadModbusDataToTable();
        loadMathDataToTable();
    }
    
    private void loadModbusDataToTable() {
        modbusTableModel.setRowCount(0);
        for (ModbusConfigManager.ModbusConfig config : modbusConfigs) {
            Object[] row = {
                config.getChannelName(),
                config.getSlaveId(),
                config.getAddress(),
                config.getDataType(),
                config.getLength()
            };
            modbusTableModel.addRow(row);
        }
    }
    
    private void loadMathDataToTable() {
        mathTableModel.setRowCount(0);
        for (MathChannelConfig config : mathConfigs) {
            Object[] row = {
                config.getChannelName(),
                config.getExpression(),
                config.getUnit(),
                config.getDecimalPlaces(),
                config.isEnabled() ? "Yes" : "No"
            };
            mathTableModel.addRow(row);
        }
    }
    
    private void loadSelectedModbusConfig() {
        int selectedRow = modbusTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < modbusConfigs.size()) {
            ModbusConfigManager.ModbusConfig config = modbusConfigs.get(selectedRow);
            channelNameField.setText(config.getChannelName());
            slaveIdField.setText(String.valueOf(config.getSlaveId()));
            addressField.setText(String.valueOf(config.getAddress()));
            dataTypeCombo.setSelectedItem(config.getDataType());
            
            // Update write functionality based on register type
            updateWriteControls(config);
        }
    }
    
    private void updateWriteControls(ModbusConfigManager.ModbusConfig config) {
        int address = config.getAddress();
        boolean isWritable = isWritableRegister(address);
        
        writeButton.setEnabled(isWritable);
        writeValueField.setEnabled(isWritable);
        passwordField.setEnabled(isWritable);
        
        if (isWritable) {
            writeStatusLabel.setText("Register is writable - enter password and value, then click Write");
            writeStatusLabel.setForeground(Color.BLACK);
            
            // Set default value based on register type
            if (address >= 1 && address <= 9999) {
                // Coil
                writeValueField.setText("false");
                writeValueField.setToolTipText("Enter: true/false, 1/0, or on/off");
            } else if (address >= 40001 && address <= 49999) {
                // Holding Register
                writeValueField.setText("0");
                writeValueField.setToolTipText("Enter numeric value");
            }
            
            // Clear password field for new write operation
            passwordField.setText("");
        } else {
            writeStatusLabel.setText("Register is read-only (Input Register or Discrete Input)");
            writeStatusLabel.setForeground(Color.GRAY);
            writeValueField.setToolTipText("This register type is not writable");
            passwordField.setToolTipText("This register type is not writable");
        }
    }
    
    private boolean isWritableRegister(int address) {
        // Coils (1-9999) and Holding Registers (40001-49999) are writable
        return (address >= 1 && address <= 9999) || (address >= 40001 && address <= 49999);
    }
    
    private void loadSelectedMathConfig() {
        int selectedRow = mathTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < mathConfigs.size()) {
            MathChannelConfig config = mathConfigs.get(selectedRow);
            mathChannelNameField.setText(config.getChannelName());
            descriptionField.setText(config.getDescription());
            expressionField.setText(config.getExpression());
            unitField.setText(config.getUnit());
            decimalPlacesSpinner.setValue(config.getDecimalPlaces());
            enabledCheckBox.setSelected(config.isEnabled());
        }
    }
    
    private void addOrUpdateModbusConfig() {
        try {
            String channelName = channelNameField.getText().trim();
            int slaveId = Integer.parseInt(slaveIdField.getText().trim());
            int address = Integer.parseInt(addressField.getText().trim());
            String dataType = (String) dataTypeCombo.getSelectedItem();
            int length = getLengthForDataType(dataType);
            
            if (channelName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Channel name cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            ModbusConfigManager.ModbusConfig newConfig = new ModbusConfigManager.ModbusConfig(
                slaveId, address, length, dataType, channelName
            );
            
            int selectedRow = modbusTable.getSelectedRow();
            if (selectedRow >= 0) {
                modbusConfigs.set(selectedRow, newConfig);
            } else {
                modbusConfigs.add(newConfig);
            }
            
            loadModbusDataToTable();
            clearModbusInputs();
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for Slave ID and Address!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void addOrUpdateMathConfig() {
        try {
            String channelName = mathChannelNameField.getText().trim();
            String expression = expressionField.getText().trim();
            String description = descriptionField.getText().trim();
            String unit = unitField.getText().trim();
            int decimalPlaces = (Integer) decimalPlacesSpinner.getValue();
            boolean enabled = enabledCheckBox.isSelected();
            
            if (channelName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Channel name cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (expression.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Expression cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!MathExpressionEvaluator.isValidExpression(expression)) {
                JOptionPane.showMessageDialog(this, "Invalid expression syntax!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            MathChannelConfig newConfig = new MathChannelConfig(
                channelName, expression, description, unit, decimalPlaces
            );
            newConfig.setEnabled(enabled);
            
            int selectedRow = mathTable.getSelectedRow();
            if (selectedRow >= 0) {
                mathConfigs.set(selectedRow, newConfig);
            } else {
                mathConfigs.add(newConfig);
            }
            
            loadMathDataToTable();
            clearMathInputs();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void deleteModbusConfig() {
        int selectedRow = modbusTable.getSelectedRow();
        if (selectedRow >= 0) {
            int result = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete this configuration?", 
                "Confirm Delete", 
                JOptionPane.YES_NO_OPTION);
            
            if (result == JOptionPane.YES_OPTION) {
                modbusConfigs.remove(selectedRow);
                loadModbusDataToTable();
                clearModbusInputs();
            }
        }
    }
    
    private void deleteMathConfig() {
        int selectedRow = mathTable.getSelectedRow();
        if (selectedRow >= 0) {
            int result = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete this math channel?", 
                "Confirm Delete", 
                JOptionPane.YES_NO_OPTION);
            
            if (result == JOptionPane.YES_OPTION) {
                mathConfigs.remove(selectedRow);
                loadMathDataToTable();
                clearMathInputs();
            }
        }
    }
    
    private void clearModbusInputs() {
        channelNameField.setText("");
        slaveIdField.setText("");
        addressField.setText("");
        dataTypeCombo.setSelectedIndex(0);
        modbusTable.clearSelection();
    }
    
    private void clearMathInputs() {
        mathChannelNameField.setText("");
        descriptionField.setText("");
        expressionField.setText("");
        unitField.setText("");
        decimalPlacesSpinner.setValue(2);
        enabledCheckBox.setSelected(true);
        mathTable.clearSelection();
    }
    
    private void testMathExpression() {
        String expression = expressionField.getText().trim();
        if (expression.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an expression to test!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!MathExpressionEvaluator.isValidExpression(expression)) {
            JOptionPane.showMessageDialog(this, "Invalid expression syntax!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Create sample values for testing
        java.util.Map<String, Double> testValues = new java.util.HashMap<>();
        testValues.put("Voltage_R_N", 220.5);
        testValues.put("Voltage_Y_N", 221.0);
        testValues.put("Voltage_B_N", 219.8);
        testValues.put("Current_A", 10.5);
        testValues.put("Current_B", 10.2);
        testValues.put("Current_C", 10.8);
        
        try {
            double result = MathExpressionEvaluator.evaluate(expression, testValues);
            String formattedResult = MathExpressionEvaluator.formatValue(result, (Integer) decimalPlacesSpinner.getValue());
            
            JOptionPane.showMessageDialog(this, 
                "Expression test successful!\nResult: " + formattedResult, 
                "Test Result", 
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Expression test failed!\nError: " + e.getMessage(), 
                "Test Failed", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void writeToSelectedRegister() {
        int selectedRow = modbusTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= modbusConfigs.size()) {
            JOptionPane.showMessageDialog(this, "Please select a register from the table first!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        ModbusConfigManager.ModbusConfig config = modbusConfigs.get(selectedRow);
        int address = config.getAddress();
        
        if (!isWritableRegister(address)) {
            JOptionPane.showMessageDialog(this, "This register type is not writable!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String passwordStr = passwordField.getText().trim();
        int passwordValue = 0; // Default password value
        
        // Password is optional if device is already unlocked via keypad
        if (!passwordStr.isEmpty()) {
            // Validate password range (0-9998 as per manual)
            try {
                passwordValue = Integer.parseInt(passwordStr);
                if (passwordValue < 0 || passwordValue > 9998) {
                    JOptionPane.showMessageDialog(this, "Password must be between 0 and 9998!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Password must be a valid number between 0 and 9998!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            // If no password entered, use default (0) - device might be already unlocked
            System.out.println("DEBUG: No password entered, using default password 0 (device might be already unlocked)");
        }
        
        String writeValueStr = writeValueField.getText().trim();
        if (writeValueStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a value to write!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            // Convert value based on register type and data type
            Object writeValue;
            if (address >= 1 && address <= 9999) {
                // Coil - convert to boolean
                if (writeValueStr.equalsIgnoreCase("true") || writeValueStr.equals("1") || writeValueStr.equalsIgnoreCase("on")) {
                    writeValue = true;
                } else if (writeValueStr.equalsIgnoreCase("false") || writeValueStr.equals("0") || writeValueStr.equalsIgnoreCase("off")) {
                    writeValue = false;
                } else {
                    JOptionPane.showMessageDialog(this, "For coils, enter: true/false, 1/0, or on/off", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else if (address >= 40001 && address <= 49999) {
                // Holding Register - convert based on data type
                String dataType = config.getDataType();
                if ("Float32 (ABCD)".equals(dataType) || "Float32 (BADC)".equals(dataType)) {
                    writeValue = Float.parseFloat(writeValueStr);
                } else {
                    writeValue = Short.parseShort(writeValueStr);
                }
            } else {
                JOptionPane.showMessageDialog(this, "This register type is not writable.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Use the existing connection from the main application
            if (connectionManager == null || settings == null) {
                JOptionPane.showMessageDialog(this, "No Modbus connection available. Please ensure the main application is connected.", "Connection Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try {
                // Check if connection is open
                if (!connectionManager.isOpen()) {
                    JOptionPane.showMessageDialog(this, "Modbus connection is not open. Please connect in the main application first.", "Connection Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                ModbusMaster master = connectionManager.getMaster();
                
                if (master != null) {
                    ChannelRuntimeService writeService = new ChannelRuntimeService(settings, master);
                    
                    // Try password write first (if password provided), but don't fail if it doesn't work
                    if (!passwordStr.isEmpty()) {
                        writeStatusLabel.setText("Writing password to register 40000 to unlock device...");
                        writeStatusLabel.setForeground(Color.BLUE);
                        
                        try {
                            // Write password to register 40000 (as per manual)
                            boolean passwordSuccess = writeService.writeValue(config.getSlaveId(), 40000, (short) passwordValue, "Int16");
                            
                            if (passwordSuccess) {
                                System.out.println("DEBUG: Password " + passwordValue + " written to register 40000");
                                // Wait for device to process the password (some devices need time)
                                Thread.sleep(500);
                                writeStatusLabel.setText("Password written, now writing to target register...");
                            } else {
                                System.out.println("DEBUG: Password write failed, but continuing with main write (device might be already unlocked)");
                                writeStatusLabel.setText("Password write failed, but continuing with main write...");
                            }
                            
                        } catch (Exception e) {
                            System.out.println("DEBUG: Password write error: " + e.getMessage() + ", but continuing with main write");
                            writeStatusLabel.setText("Password write error, but continuing with main write...");
                        }
                    } else {
                        writeStatusLabel.setText("No password provided, writing directly to target register...");
                        writeStatusLabel.setForeground(Color.BLUE);
                    }
                    
                    // Now perform the main write operation
                    System.out.println("DEBUG: Attempting to write value " + writeValue + " to register " + address + " (Slave ID: " + config.getSlaveId() + ")");
                    boolean success = writeService.writeValue(config.getSlaveId(), address, writeValue, config.getDataType());
                    
                    if (success) {
                        writeStatusLabel.setText("✓ Value written successfully!");
                        writeStatusLabel.setForeground(Color.GREEN);
                        System.out.println("DEBUG: Successfully wrote value " + writeValue + " to register " + address);
                        JOptionPane.showMessageDialog(this, "Value written successfully to register " + address + "!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        writeStatusLabel.setText("✗ Failed to write value");
                        writeStatusLabel.setForeground(Color.RED);
                        System.out.println("DEBUG: Failed to write value " + writeValue + " to register " + address);
                        JOptionPane.showMessageDialog(this, "Failed to write value. Check connection, register address, and ensure device is unlocked.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    
                    writeService.shutdown();
                } else {
                    JOptionPane.showMessageDialog(this, "Could not get Modbus master from existing connection.", "Connection Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                writeStatusLabel.setText("✗ Write error");
                writeStatusLabel.setForeground(Color.RED);
                JOptionPane.showMessageDialog(this, "Write error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number format: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Write error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    private void saveAllConfigurations() {
        // Save Modbus configurations
        ModbusConfigManager.saveConfig(modbusConfigs);
        
        // Save Math configurations - replace all existing math channels with current ones
        MathChannelManager.clearAllConfigs();
        for (MathChannelConfig config : mathConfigs) {
            MathChannelManager.addConfig(config);
        }
        
        // Force save to ensure persistence
        MathChannelManager.saveConfigs();
        
        JOptionPane.showMessageDialog(this, "All configurations saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }
}
