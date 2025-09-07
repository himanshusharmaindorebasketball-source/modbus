package com.example.modbus;

import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.msg.ReadCoilsRequest;
import com.serotonin.modbus4j.msg.ReadCoilsResponse;
import com.serotonin.modbus4j.msg.ReadDiscreteInputsRequest;
import com.serotonin.modbus4j.msg.ReadDiscreteInputsResponse;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;
import com.serotonin.modbus4j.msg.ReadInputRegistersRequest;
import com.serotonin.modbus4j.msg.ReadInputRegistersResponse;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

public class FilterDataPage {
    private JPanel panel;
    private JLabel statusLabel;
    private JTable dataTable;
    private DefaultTableModel tableModel;
    private final ModbusSettings settings;
    private final ModbusConnectionManager connectionManager;
    private Runnable dataChangeListener;

    // Controls
    private JButton startStopButton;
    private JLabel pollsLabel;
    private JLabel validRespLabel;
    private JButton resetCountersButton;
    private JButton configButton;

    private Timer timer;
    private boolean polling;
    private long polls;
    private long validResponses;
    
    // Two-dimensional array with 3 entries: [slaveId, start, length]
    private int[][] modbusConfigArray; // Will be loaded from file
    private String[] dataTypes; // Data types for each configuration entry
    private String[] channelNames; // Channel names for each configuration entry

    public FilterDataPage(ModbusSettings settings) {
        this(settings, new ModbusConnectionManager());
    }

    public FilterDataPage(ModbusSettings settings, ModbusConnectionManager connectionManager) {
        this.settings = settings;
        this.connectionManager = connectionManager;
        loadModbusConfig();
        initializeUI();
        updateConnectionStatus();
    }

    private void updateConnectionStatus() {
        if (connectionManager.isOpen()) {
            statusLabel.setText("Status: Connected to " + settings.getPortName());
        } else {
            statusLabel.setText("Status: Not connected. Use Settings -> Connect.");
        }
    }

    private void initializeUI() {
        panel = new JPanel(new BorderLayout());

        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Control buttons and labels
        startStopButton = new JButton("Start");
        startStopButton.addActionListener(e -> togglePolling());
        gbc.gridx = 0; gbc.gridy = 0; top.add(startStopButton, gbc);

        pollsLabel = new JLabel("Number of Polls: 0");
        gbc.gridx = 1; top.add(pollsLabel, gbc);
        
        validRespLabel = new JLabel("Valid Slave Responses: 0");
        gbc.gridx = 2; top.add(validRespLabel, gbc);

        resetCountersButton = new JButton("Reset Counters");
        resetCountersButton.addActionListener(e -> {
            polls = 0; validResponses = 0; updateCounters();
        });
        gbc.gridx = 3; top.add(resetCountersButton, gbc);
        
        configButton = new JButton("Configure Registers");
        configButton.addActionListener(e -> openConfigDialog());
        gbc.gridx = 4; top.add(configButton, gbc);
        
        JButton dataLoggerButton = new JButton("Data Logger");
        dataLoggerButton.addActionListener(e -> openDataLoggerDialogWithPassword());
        gbc.gridx = 5; top.add(dataLoggerButton, gbc);

        statusLabel = new JLabel("Status: Not connected. Use Settings -> Connect.");
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 5; top.add(statusLabel, gbc);
        gbc.gridwidth = 1;

        panel.add(top, BorderLayout.NORTH);

        String[] columnNames = {"Timestamp", "Channel Name", "Address", "Value"};
        tableModel = new DefaultTableModel(columnNames, 0);
        dataTable = new JTable(tableModel);
        dataTable.setFont(new Font("Arial", Font.PLAIN, 14));
        dataTable.setRowHeight(25);
        JScrollPane scrollPane = new JScrollPane(dataTable);
        panel.add(scrollPane, BorderLayout.CENTER);
    }

    private void togglePolling() {
        if (polling) {
            stopPolling();
        } else {
            startPolling();
        }
    }

    private void startPolling() {
        if (!connectionManager.isOpen()) {
            JOptionPane.showMessageDialog(panel, "Not connected. Go to Settings and click Connect.");
            return;
        }
        polling = true;
        startStopButton.setText("Stop");
        
        // Start data logging if it's enabled in configuration
        startDataLogging();
        
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() { readModbusData(); }
        }, 0, 1000);
    }

    private void stopPolling() {
        polling = false;
        startStopButton.setText("Start");
        if (timer != null) { timer.cancel(); timer = null; }
        
        // Stop data logging
        stopDataLogging();
    }

    private void readModbusData() {
        try {
            ModbusMaster modbusMaster = connectionManager.getMaster();
            if (modbusMaster == null) {
                SwingUtilities.invokeLater(() -> statusLabel.setText("Status: Not connected"));
                return;
            }
            
            polls++;
            System.out.println("=== FilterData Polling Cycle #" + polls + " ===");
            
            // Collect all data first, then update UI once
            java.util.List<Object[]> tableData = new java.util.ArrayList<>();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        // Use the two-dimensional array instead of UI fields
        for (int i = 0; i < modbusConfigArray.length; i++) {
            int[] config = modbusConfigArray[i];
            String dataType = (i < dataTypes.length) ? dataTypes[i] : "Float32 (ABCD)"; // Default fallback
            String channelName = (i < channelNames.length) ? channelNames[i] : "Channel_" + config[1]; // Default fallback
            int slaveId = config[0];
            int address = config[1];
            int len = Math.max(1, config[2]);
            
            try {
                // Determine function code based on address
                if (address >= 30001 && address < 40000) {
                    // Input Registers (FC04) - addresses 30001-39999
                    int zeroBasedAddress = address - 30001;
                    ReadInputRegistersRequest req = new ReadInputRegistersRequest(slaveId, zeroBasedAddress, len);
                    System.out.println("Request: FC04 Read Input Registers - Slave:" + slaveId + ", Address:" + address + " (zero-based:" + zeroBasedAddress + "), Length:" + len);
                    ReadInputRegistersResponse resp = (ReadInputRegistersResponse) modbusMaster.send(req);
                    if (resp.isException()) {
                        System.err.println("Modbus exception for address " + address + ": " + resp.getExceptionMessage());
                        continue;
                    }
                    validResponses++;
                    short[] data = resp.getShortData();
                    System.out.println("Response: " + java.util.Arrays.toString(data));
                    addDataToCollection(tableData, data, address, timestamp, dataType, channelName);
                } else if (address >= 40001 && address < 50000) {
                    // Holding Registers (FC03) - addresses 40001-49999
                    int zeroBasedAddress = address - 40001;
                    ReadHoldingRegistersRequest req = new ReadHoldingRegistersRequest(slaveId, zeroBasedAddress, len);
                    System.out.println("Request: FC03 Read Holding Registers - Slave:" + slaveId + ", Address:" + address + " (zero-based:" + zeroBasedAddress + "), Length:" + len);
                    ReadHoldingRegistersResponse resp = (ReadHoldingRegistersResponse) modbusMaster.send(req);
                    if (resp.isException()) {
                        System.err.println("Modbus exception for address " + address + ": " + resp.getExceptionMessage());
                        continue;
                    }
                    validResponses++;
                    short[] data = resp.getShortData();
                    System.out.println("Response: " + java.util.Arrays.toString(data));
                    addDataToCollection(tableData, data, address, timestamp, dataType, channelName);
                } else if (address >= 10001 && address < 20000) {
                    // Discrete Inputs (FC02) - addresses 10001-19999
                    int zeroBasedAddress = address - 10001;
                    ReadDiscreteInputsRequest req = new ReadDiscreteInputsRequest(slaveId, zeroBasedAddress, len);
                    System.out.println("Request: FC02 Read Discrete Inputs - Slave:" + slaveId + ", Address:" + address + " (zero-based:" + zeroBasedAddress + "), Length:" + len);
                    ReadDiscreteInputsResponse resp = (ReadDiscreteInputsResponse) modbusMaster.send(req);
                    if (resp.isException()) {
                        System.err.println("Modbus exception for address " + address + ": " + resp.getExceptionMessage());
                        continue;
                    }
                    validResponses++;
                    boolean[] data = resp.getBooleanData();
                    System.out.println("Response: " + java.util.Arrays.toString(data));
                    addBooleanDataToCollection(tableData, data, address, timestamp, channelName);
                } else if (address >= 1 && address < 10000) {
                    // Coils (FC01) - addresses 1-9999
                    int zeroBasedAddress = address - 1;
                    ReadCoilsRequest req = new ReadCoilsRequest(slaveId, zeroBasedAddress, len);
                    System.out.println("Request: FC01 Read Coils - Slave:" + slaveId + ", Address:" + address + " (zero-based:" + zeroBasedAddress + "), Length:" + len);
                    ReadCoilsResponse resp = (ReadCoilsResponse) modbusMaster.send(req);
                    if (resp.isException()) {
                        System.err.println("Modbus exception for address " + address + ": " + resp.getExceptionMessage());
                        continue;
                    }
                    validResponses++;
                    boolean[] data = resp.getBooleanData();
                    System.out.println("Response: " + java.util.Arrays.toString(data));
                    addBooleanDataToCollection(tableData, data, address, timestamp, channelName);
                } else {
                    // Treat as raw zero-based holding register address
                    ReadHoldingRegistersRequest req = new ReadHoldingRegistersRequest(slaveId, address, len);
                    System.out.println("Request: FC03 Read Holding Registers (raw) - Slave:" + slaveId + ", Address:" + address + ", Length:" + len);
                    ReadHoldingRegistersResponse resp = (ReadHoldingRegistersResponse) modbusMaster.send(req);
                    if (resp.isException()) {
                        System.err.println("Modbus exception for address " + address + ": " + resp.getExceptionMessage());
                        continue;
                    }
                    validResponses++;
                    short[] data = resp.getShortData();
                    System.out.println("Response: " + java.util.Arrays.toString(data));
                    addDataToCollection(tableData, data, address, timestamp, dataType, channelName);
                }
                SwingUtilities.invokeLater(() -> statusLabel.setText("Status: Reading from " + settings.getPortName()));
            } catch (ModbusTransportException e) {
                SwingUtilities.invokeLater(() -> statusLabel.setText("Status: Transport error - " + e.getMessage()));
                System.err.println("Transport error for address " + address + ": " + e.getMessage());
            }
        }
        
            // Update UI once with all collected data
            SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);
                for (Object[] row : tableData) {
                    tableModel.addRow(row);
                }
                
                // Calculate math channel values AFTER data is stored in ModbusDataStore
                calculateMathChannels();
                
                // Send data to energy logger
                sendDataToLogger();
            });
            
            System.out.println("=== End of Polling Cycle #" + polls + " ===");
            
        } catch (Exception e) {
            System.err.println("Critical error in readModbusData: " + e.getMessage());
            e.printStackTrace();
            
            // Try to recover by reconnecting
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Status: Error - attempting to reconnect...");
            });
            
            // Attempt to reconnect after a delay
            new Thread(() -> {
                try {
                    Thread.sleep(5000); // Wait 5 seconds before reconnecting
                    connectionManager.close();
                    connectionManager.open(settings);
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Status: Reconnected to " + settings.getPortName());
                    });
                } catch (Exception reconnectException) {
                    System.err.println("Failed to reconnect: " + reconnectException.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Status: Connection failed - " + reconnectException.getMessage());
                    });
                }
            }).start();
        }
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
    
    /**
     * Send current data to energy logger
     */
    private void sendDataToLogger() {
        try {
            EnergyDataLogger logger = EnergyDataLogger.getInstance();
            if (logger.isLogging()) {
                // Get all current data from ModbusDataStore
                ModbusDataStore dataStore = ModbusDataStore.getInstance();
                java.util.Map<String, Object> allValues = dataStore.getAllValues();
                
                if (allValues != null && !allValues.isEmpty()) {
                    // Send to logger
                    logger.updateData(allValues);
                } else {
                    System.out.println("DEBUG: No data available to send to energy logger");
                }
            } else {
                System.out.println("DEBUG: Energy logger is not active");
            }
        } catch (Exception e) {
            System.err.println("Error sending data to logger: " + e.getMessage());
            e.printStackTrace();
            
            // Try to restart the energy logger if it fails
            try {
                EnergyDataLogger logger = EnergyDataLogger.getInstance();
                logger.stopLogging();
                Thread.sleep(1000);
                logger.startLogging();
                System.out.println("Energy logger restarted successfully");
            } catch (Exception restartException) {
                System.err.println("Failed to restart energy logger: " + restartException.getMessage());
            }
        }
    }
    
    private void calculateMathChannels() {
        // Get current channel values from ModbusDataStore
        java.util.Map<String, Double> channelValues = new java.util.HashMap<>();
        
        // Get all available channel values from ModbusDataStore
        ModbusDataStore dataStore = ModbusDataStore.getInstance();
        java.util.Map<String, Object> allValues = dataStore.getAllValues();
        
        for (java.util.Map.Entry<String, Object> entry : allValues.entrySet()) {
            String channelName = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Number) {
                double doubleValue = ((Number) value).doubleValue();
                channelValues.put(channelName, doubleValue);
            }
        }
        
        // Calculate math channel values
        java.util.Map<String, Double> mathValues = MathChannelManager.calculateAllValues(channelValues);
        
        // Add math channel results to table and publish to data store
        String timestamp = java.time.LocalDateTime.now().toString();
        for (java.util.Map.Entry<String, Double> entry : mathValues.entrySet()) {
            String channelName = entry.getKey();
            Double value = entry.getValue();
            
            // Add to table data
            tableModel.addRow(new Object[]{timestamp, channelName, "MATH", value});
            
            // Publish to data store
            ModbusDataStore.getInstance().updateValue(channelName, value);
        }
    }
    
    private void addDataToCollection(java.util.List<Object[]> tableData, short[] data, int start, String timestamp, String dataType, String channelName) {
        if ("Int16".equals(dataType)) {
            for (int i = 0; i < data.length; i++) {
                int value = (int) data[i];
                tableData.add(new Object[]{timestamp, channelName, start + i, value});
                // Publish to data store
                ModbusDataStore.getInstance().updateValue(channelName, value);
            }
        } else if ("UInt16".equals(dataType)) {
            for (int i = 0; i < data.length; i++) {
                int val = data[i] & 0xFFFF;
                tableData.add(new Object[]{timestamp, channelName, start + i, val});
                // Publish to data store
                ModbusDataStore.getInstance().updateValue(channelName, val);
            }
        } else if ("Float32 (ABCD)".equals(dataType)) {
            for (int i = 0; i + 1 < data.length; i += 2) {
                int hi = data[i] & 0xFFFF;
                int lo = data[i + 1] & 0xFFFF;
                int bits = (hi << 16) | lo;
                float f = Float.intBitsToFloat(bits);
                tableData.add(new Object[]{timestamp, channelName, start + i, f});
                // Publish to data store
                ModbusDataStore.getInstance().updateValue(channelName, f);
            }
        } else { // Float32 (BADC) word-swapped
            for (int i = 0; i + 1 < data.length; i += 2) {
                int hi = data[i + 1] & 0xFFFF;
                int lo = data[i] & 0xFFFF;
                int bits = (hi << 16) | lo;
                float f = Float.intBitsToFloat(bits);
                tableData.add(new Object[]{timestamp, channelName, start + i, f});
                // Publish to data store
                ModbusDataStore.getInstance().updateValue(channelName, f);
            }
        }
    }
    
    private void addBooleanDataToCollection(java.util.List<Object[]> tableData, boolean[] data, int start, String timestamp, String channelName) {
        for (int i = 0; i < data.length; i++) {
            boolean value = data[i];
            tableData.add(new Object[]{timestamp, channelName, start + i, value});
            // Publish to data store
            ModbusDataStore.getInstance().updateValue(channelName, value);
        }
    }

    


    private void updateCounters() {
        pollsLabel.setText("Number of Polls: " + polls);
        validRespLabel.setText("Valid Slave Responses: " + validResponses);
    }

    public JPanel getPanel() { return panel; }

    public void shutdown() { stopPolling(); }

    public void refreshTable() { tableModel.setRowCount(0); }
    
    public Object[] getFirstTableEntry() {
        if (tableModel.getRowCount() > 0) {
            Object[] rowData = new Object[3];
            rowData[0] = tableModel.getValueAt(0, 0); // Timestamp
            rowData[1] = tableModel.getValueAt(0, 1); // Index
            rowData[2] = tableModel.getValueAt(0, 2); // Value
            return rowData;
        }
        return null;
    }
    
    public void setDataChangeListener(Runnable listener) {
        this.dataChangeListener = listener;
    }
    
    private void notifyDataChange() {
        if (dataChangeListener != null) {
            SwingUtilities.invokeLater(dataChangeListener);
        }
    }
    
    public void setModbusConfigArray(int[][] configArray) {
        this.modbusConfigArray = configArray;
    }
    
    public int[][] getModbusConfigArray() {
        return this.modbusConfigArray;
    }
    
    private void loadModbusConfig() {
        List<ModbusConfigManager.ModbusConfig> configs = ModbusConfigManager.loadConfig();
        this.modbusConfigArray = ModbusConfigManager.convertToArray(configs);
        this.dataTypes = ModbusConfigManager.getDataTypes(configs);
        this.channelNames = ModbusConfigManager.getChannelNames(configs);
    }
    
    private void openConfigDialog() {
        ExtendedModbusConfigUI configDialog = new ExtendedModbusConfigUI((Frame) SwingUtilities.getWindowAncestor(panel), connectionManager, settings);
        configDialog.setVisible(true);
        
        // Reload configuration after dialog closes
        loadModbusConfig();
        System.out.println("Modbus configuration reloaded. Current config:");
        for (int[] config : modbusConfigArray) {
            System.out.println("Slave: " + config[0] + ", Address: " + config[1] + ", Length: " + config[2]);
        }
    }
    
    private void openDataLoggerDialog() {
        DataLoggerConfigDialog loggerDialog = new DataLoggerConfigDialog(SwingUtilities.getWindowAncestor(panel));
        loggerDialog.setVisible(true);
    }
    
    private void openDataLoggerDialogWithPassword() {
        DataLoggerPasswordManager passwordManager = DataLoggerPasswordManager.getInstance();
        
        // Check if password protection is enabled
        if (!passwordManager.isPasswordEnabled()) {
            // Password protection is disabled, open dialog directly
            openDataLoggerDialog();
            return;
        }
        
        // Show password dialog
        boolean authenticated = PasswordInputDialog.showPasswordDialog(SwingUtilities.getWindowAncestor(panel));
        
        if (authenticated) {
            // Password is correct, open data logger dialog
            openDataLoggerDialog();
        }
        // If authentication failed, do nothing (user can try again)
    }
    
    /**
     * Start data logging if enabled in configuration
     */
    private void startDataLogging() {
        try {
            EnergyDataLogger logger = EnergyDataLogger.getInstance();
            EnergyDataLogger.DataLoggerConfig config = logger.getConfig();
            
            if (config.isEnabled() && !logger.isLogging()) {
                logger.startLogging();
                System.out.println("Data logging started automatically with polling");
            }
        } catch (Exception e) {
            System.err.println("Error starting data logging: " + e.getMessage());
        }
    }
    
    /**
     * Stop data logging
     */
    private void stopDataLogging() {
        try {
            EnergyDataLogger logger = EnergyDataLogger.getInstance();
            
            if (logger.isLogging()) {
                logger.stopLogging();
                System.out.println("Data logging stopped automatically with polling");
            }
        } catch (Exception e) {
            System.err.println("Error stopping data logging: " + e.getMessage());
        }
    }
}

