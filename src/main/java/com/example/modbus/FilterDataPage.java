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
import java.awt.*;
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
    private JTextField deviceIdField;
    private JTextField addressField;
    private JTextField lengthField;
    private JComboBox<String> pointTypeCombo;
    private JComboBox<String> dataTypeCombo;
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

        int col = 0;
        gbc.gridx = col++; gbc.gridy = 0; top.add(new JLabel("Device Id:"), gbc);
        deviceIdField = new JTextField(Integer.toString(settings.getDeviceId()), 4);
        gbc.gridx = col++; top.add(deviceIdField, gbc);

        gbc.gridx = col++; top.add(new JLabel("Address:"), gbc);
        addressField = new JTextField("0", 6);
        gbc.gridx = col++; top.add(addressField, gbc);

        gbc.gridx = col++; top.add(new JLabel("Length:"), gbc);
        lengthField = new JTextField("10", 4);
        gbc.gridx = col++; top.add(lengthField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; col = 0; top.add(new JLabel("MODBUS Point Type"), gbc);
        pointTypeCombo = new JComboBox<>(new String[]{"01: COIL STATUS", "02: DISCRETE INPUT", "03: HOLDING REGISTERS", "04: INPUT REGISTERS"});
        gbc.gridx = 1; gbc.gridwidth = 2; top.add(pointTypeCombo, gbc);

        gbc.gridx = 3; gbc.gridwidth = 1; top.add(new JLabel("Data Type"), gbc);
        dataTypeCombo = new JComboBox<>(new String[]{"Int16", "UInt16", "Float32 (ABCD)", "Float32 (BADC)"});
        dataTypeCombo.setSelectedItem("Float32 (ABCD)");
        gbc.gridx = 4; gbc.gridwidth = 2; top.add(dataTypeCombo, gbc);
        gbc.gridwidth = 1;

        startStopButton = new JButton("Start");
        startStopButton.addActionListener(e -> togglePolling());
        gbc.gridx = 6; gbc.gridy = 1; top.add(startStopButton, gbc);

        pollsLabel = new JLabel("Number of Polls: 0");
        gbc.gridx = 7; top.add(pollsLabel, gbc);
        validRespLabel = new JLabel("Valid Slave Responses: 0");
        gbc.gridx = 8; top.add(validRespLabel, gbc);

        resetCountersButton = new JButton("Reset Ctrs");
        resetCountersButton.addActionListener(e -> {
            polls = 0; validResponses = 0; updateCounters();
        });
        gbc.gridx = 9; top.add(resetCountersButton, gbc);
        
        configButton = new JButton("Config");
        configButton.addActionListener(e -> openConfigDialog());
        gbc.gridx = 10; top.add(configButton, gbc);

        statusLabel = new JLabel("Status: Not connected. Use Settings -> Connect.");
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 10; top.add(statusLabel, gbc);
        gbc.gridwidth = 1;

        panel.add(top, BorderLayout.NORTH);

        String[] columnNames = {"Timestamp", "Index", "Value"};
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
    }

    private void readModbusData() {
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
        String dataType = (String) dataTypeCombo.getSelectedItem();
        
        // Use the two-dimensional array instead of UI fields
        for (int[] config : modbusConfigArray) {
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
                    addDataToCollection(tableData, data, address, timestamp, dataType);
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
                    addDataToCollection(tableData, data, address, timestamp, dataType);
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
                    addBooleanDataToCollection(tableData, data, address, timestamp);
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
                    addBooleanDataToCollection(tableData, data, address, timestamp);
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
                    addDataToCollection(tableData, data, address, timestamp, dataType);
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
        });
        
        System.out.println("=== End of Polling Cycle #" + polls + " ===");
        SwingUtilities.invokeLater(this::updateCounters);
        SwingUtilities.invokeLater(this::notifyDataChange);
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
    
    private void addDataToCollection(java.util.List<Object[]> tableData, short[] data, int start, String timestamp, String dataType) {
        if ("Int16".equals(dataType)) {
            for (int i = 0; i < data.length; i++) {
                tableData.add(new Object[]{timestamp, start + i, (int) data[i]});
            }
        } else if ("UInt16".equals(dataType)) {
            for (int i = 0; i < data.length; i++) {
                int val = data[i] & 0xFFFF;
                tableData.add(new Object[]{timestamp, start + i, val});
            }
        } else if ("Float32 (ABCD)".equals(dataType)) {
            for (int i = 0; i + 1 < data.length; i += 2) {
                int hi = data[i] & 0xFFFF;
                int lo = data[i + 1] & 0xFFFF;
                int bits = (hi << 16) | lo;
                float f = Float.intBitsToFloat(bits);
                tableData.add(new Object[]{timestamp, start + i, f});
            }
        } else { // Float32 (BADC) word-swapped
            for (int i = 0; i + 1 < data.length; i += 2) {
                int hi = data[i + 1] & 0xFFFF;
                int lo = data[i] & 0xFFFF;
                int bits = (hi << 16) | lo;
                float f = Float.intBitsToFloat(bits);
                tableData.add(new Object[]{timestamp, start + i, f});
            }
        }
    }
    
    private void addBooleanDataToCollection(java.util.List<Object[]> tableData, boolean[] data, int start, String timestamp) {
        for (int i = 0; i < data.length; i++) {
            tableData.add(new Object[]{timestamp, start + i, data[i]});
        }
    }

    private void updateTableRegisters(short[] data, int start) {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String dtype = (String) dataTypeCombo.getSelectedItem();
            if ("Int16".equals(dtype)) {
                for (int i = 0; i < data.length; i++) {
                    tableModel.addRow(new Object[]{ts, start + i, (int) data[i]});
                }
            } else if ("UInt16".equals(dtype)) {
                for (int i = 0; i < data.length; i++) {
                    int val = data[i] & 0xFFFF;
                    tableModel.addRow(new Object[]{ts, start + i, val});
                }
            } else if ("Float32 (ABCD)".equals(dtype)) {
                for (int i = 0; i + 1 < data.length; i += 2) {
                    int hi = data[i] & 0xFFFF;
                    int lo = data[i + 1] & 0xFFFF;
                    int bits = (hi << 16) | lo;
                    float f = Float.intBitsToFloat(bits);
                    tableModel.addRow(new Object[]{ts, start + i, f});
                }
            } else { // Float32 (BADC) word-swapped
                for (int i = 0; i + 1 < data.length; i += 2) {
                    int hi = data[i + 1] & 0xFFFF;
                    int lo = data[i] & 0xFFFF;
                    int bits = (hi << 16) | lo;
                    float f = Float.intBitsToFloat(bits);
                    tableModel.addRow(new Object[]{ts, start + i, f});
                }
            }
            notifyDataChange();
        });
    }
    
    private void addTableRegisters(short[] data, int start) {
        SwingUtilities.invokeLater(() -> {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String dtype = (String) dataTypeCombo.getSelectedItem();
            if ("Int16".equals(dtype)) {
                for (int i = 0; i < data.length; i++) {
                    tableModel.addRow(new Object[]{ts, start + i, (int) data[i]});
                }
            } else if ("UInt16".equals(dtype)) {
                for (int i = 0; i < data.length; i++) {
                    int val = data[i] & 0xFFFF;
                    tableModel.addRow(new Object[]{ts, start + i, val});
                }
            } else if ("Float32 (ABCD)".equals(dtype)) {
                for (int i = 0; i + 1 < data.length; i += 2) {
                    int hi = data[i] & 0xFFFF;
                    int lo = data[i + 1] & 0xFFFF;
                    int bits = (hi << 16) | lo;
                    float f = Float.intBitsToFloat(bits);
                    tableModel.addRow(new Object[]{ts, start + i, f});
                }
            } else { // Float32 (BADC) word-swapped
                for (int i = 0; i + 1 < data.length; i += 2) {
                    int hi = data[i + 1] & 0xFFFF;
                    int lo = data[i] & 0xFFFF;
                    int bits = (hi << 16) | lo;
                    float f = Float.intBitsToFloat(bits);
                    tableModel.addRow(new Object[]{ts, start + i, f});
                }
            }
        });
    }

    private void updateTableBooleans(boolean[] data, int start) {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            for (int i = 0; i < data.length; i++) {
                tableModel.addRow(new Object[]{ts, start + i, data[i]});
            }
            notifyDataChange();
        });
    }
    
    private void addTableBooleans(boolean[] data, int start) {
        SwingUtilities.invokeLater(() -> {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            for (int i = 0; i < data.length; i++) {
                tableModel.addRow(new Object[]{ts, start + i, data[i]});
            }
        });
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
        this.modbusConfigArray = ModbusConfigManager.convertToArray(ModbusConfigManager.loadConfig());
    }
    
    private void openConfigDialog() {
        ModbusConfigUI configDialog = new ModbusConfigUI((Frame) SwingUtilities.getWindowAncestor(panel));
        configDialog.setVisible(true);
        
        // Reload configuration after dialog closes
        loadModbusConfig();
        System.out.println("Modbus configuration reloaded. Current config:");
        for (int[] config : modbusConfigArray) {
            System.out.println("Slave: " + config[0] + ", Address: " + config[1] + ", Length: " + config[2]);
        }
    }
}
