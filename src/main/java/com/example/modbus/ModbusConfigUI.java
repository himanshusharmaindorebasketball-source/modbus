package com.example.modbus;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class ModbusConfigUI extends JDialog {
    private DefaultTableModel tableModel;
    private List<ModbusConfigManager.ModbusConfig> configs;
    private JTable configTable;
    private JTextField slaveIdField;
    private JTextField addressField;
    private JComboBox<String> dataTypeCombo;
    private JTextField channelNameField;
    private JLabel lengthInfoLabel;
    
    public ModbusConfigUI(Frame parent) {
        super(parent, "Modbus Configuration", true);
        this.configs = new ArrayList<>(ModbusConfigManager.loadConfig());
        initializeUI();
        loadDataToTable();
    }
    
    private int getLengthForDataType(String dataType) {
        switch (dataType) {
            case "Int16":
            case "UInt16":
                return 1; // 1 register (16 bits)
            case "Float32 (ABCD)":
            case "Float32 (BADC)":
                return 2; // 2 registers (32 bits)
            default:
                return 1; // Default fallback
        }
    }
    
    private void updateLengthInfo() {
        String dataType = (String) dataTypeCombo.getSelectedItem();
        int length = getLengthForDataType(dataType);
        String unit = length == 1 ? "register" : "registers";
        lengthInfoLabel.setText(length + " " + unit + " (auto)");
    }
    
    private void initializeUI() {
        setSize(600, 500);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());
        
        // Top panel with input fields
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Add/Edit Configuration"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Slave ID
        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("Slave ID:"), gbc);
        slaveIdField = new JTextField(8);
        gbc.gridx = 1;
        inputPanel.add(slaveIdField, gbc);
        
        // Address
        gbc.gridx = 2;
        inputPanel.add(new JLabel("Address:"), gbc);
        addressField = new JTextField(10);
        gbc.gridx = 3;
        inputPanel.add(addressField, gbc);
        
        // Data Type
        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("Data Type:"), gbc);
        dataTypeCombo = new JComboBox<>(new String[]{"Int16", "UInt16", "Float32 (ABCD)", "Float32 (BADC)"});
        dataTypeCombo.setSelectedItem("Float32 (ABCD)");
        dataTypeCombo.addActionListener(e -> updateLengthInfo());
        gbc.gridx = 1; gbc.gridwidth = 2;
        inputPanel.add(dataTypeCombo, gbc);
        
        // Length Info Label
        gbc.gridx = 3; gbc.gridwidth = 1;
        inputPanel.add(new JLabel("Length:"), gbc);
        lengthInfoLabel = new JLabel("2 registers (auto)");
        gbc.gridx = 4; gbc.gridwidth = 2;
        inputPanel.add(lengthInfoLabel, gbc);
        
        // Channel Name
        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("Channel Name:"), gbc);
        channelNameField = new JTextField(15);
        gbc.gridx = 1; gbc.gridwidth = 2;
        inputPanel.add(channelNameField, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addButton = new JButton("Add");
        JButton updateButton = new JButton("Update");
        JButton deleteButton = new JButton("Delete");
        JButton clearButton = new JButton("Clear");
        
        addButton.addActionListener(e -> addConfig());
        updateButton.addActionListener(e -> updateConfig());
        deleteButton.addActionListener(e -> deleteConfig());
        clearButton.addActionListener(e -> clearFields());
        
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 6;
        inputPanel.add(buttonPanel, gbc);
        
        add(inputPanel, BorderLayout.NORTH);
        
        // Table
        String[] columnNames = {"Slave ID", "Address", "Data Type", "Channel Name"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        configTable = new JTable(tableModel);
        configTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        configTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedToFields();
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(configTable);
        scrollPane.setPreferredSize(new Dimension(580, 300));
        add(scrollPane, BorderLayout.CENTER);
        
        // Bottom panel with action buttons
        JPanel bottomPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("Save Configuration");
        JButton loadButton = new JButton("Load Configuration");
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> saveConfig());
        loadButton.addActionListener(e -> loadConfig());
        okButton.addActionListener(e -> {
            saveConfig();
            dispose();
        });
        cancelButton.addActionListener(e -> dispose());
        
        bottomPanel.add(saveButton);
        bottomPanel.add(loadButton);
        bottomPanel.add(okButton);
        bottomPanel.add(cancelButton);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void addConfig() {
        try {
            int slaveId = Integer.parseInt(slaveIdField.getText().trim());
            int address = Integer.parseInt(addressField.getText().trim());
            String dataType = (String) dataTypeCombo.getSelectedItem();
            String channelName = channelNameField.getText().trim();
            
            // Auto-calculate length based on data type
            int length = getLengthForDataType(dataType);
            
            // Use default channel name if empty
            if (channelName.isEmpty()) {
                channelName = "Channel_" + address;
            }
            
            ModbusConfigManager.ModbusConfig config = new ModbusConfigManager.ModbusConfig(slaveId, address, length, dataType, channelName);
            configs.add(config);
            loadDataToTable();
            clearFields();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numeric values.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateConfig() {
        int selectedRow = configTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a row to update.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            int slaveId = Integer.parseInt(slaveIdField.getText().trim());
            int address = Integer.parseInt(addressField.getText().trim());
            String dataType = (String) dataTypeCombo.getSelectedItem();
            String channelName = channelNameField.getText().trim();
            
            // Auto-calculate length based on data type
            int length = getLengthForDataType(dataType);
            
            // Use default channel name if empty
            if (channelName.isEmpty()) {
                channelName = "Channel_" + address;
            }
            
            ModbusConfigManager.ModbusConfig config = new ModbusConfigManager.ModbusConfig(slaveId, address, length, dataType, channelName);
            configs.set(selectedRow, config);
            loadDataToTable();
            clearFields();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numeric values.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void deleteConfig() {
        int selectedRow = configTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a row to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this configuration?", 
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            configs.remove(selectedRow);
            loadDataToTable();
            clearFields();
        }
    }
    
    private void clearFields() {
        slaveIdField.setText("");
        addressField.setText("");
        dataTypeCombo.setSelectedItem("Float32 (ABCD)");
        channelNameField.setText("");
        updateLengthInfo(); // Update the length info display
    }
    
    private void loadSelectedToFields() {
        int selectedRow = configTable.getSelectedRow();
        if (selectedRow != -1 && selectedRow < configs.size()) {
            ModbusConfigManager.ModbusConfig config = configs.get(selectedRow);
            slaveIdField.setText(String.valueOf(config.getSlaveId()));
            addressField.setText(String.valueOf(config.getAddress()));
            dataTypeCombo.setSelectedItem(config.getDataType());
            channelNameField.setText(config.getChannelName());
            updateLengthInfo(); // Update the length info display
        }
    }
    
    private void loadDataToTable() {
        tableModel.setRowCount(0);
        for (ModbusConfigManager.ModbusConfig config : configs) {
            tableModel.addRow(new Object[]{
                config.getSlaveId(),
                config.getAddress(),
                config.getDataType(),
                config.getChannelName()
            });
        }
    }
    
    private void saveConfig() {
        ModbusConfigManager.saveConfig(configs);
        JOptionPane.showMessageDialog(this, "Configuration saved successfully!", "Save Success", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void loadConfig() {
        configs = new ArrayList<>(ModbusConfigManager.loadConfig());
        loadDataToTable();
        clearFields();
        JOptionPane.showMessageDialog(this, "Configuration loaded successfully!", "Load Success", JOptionPane.INFORMATION_MESSAGE);
    }
    
    public List<ModbusConfigManager.ModbusConfig> getConfigs() {
        return new ArrayList<>(configs);
    }
    
    public int[][] getConfigArray() {
        return ModbusConfigManager.convertToArray(configs);
    }
}
