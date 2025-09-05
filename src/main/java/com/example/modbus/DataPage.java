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

public class DataPage {
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

    private Timer timer;
    private boolean polling;
    private long polls;
    private long validResponses;

    public DataPage(ModbusSettings settings) {
        this(settings, new ModbusConnectionManager());
    }

    public DataPage(ModbusSettings settings, ModbusConnectionManager connectionManager) {
        this.settings = settings;
        this.connectionManager = connectionManager;
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
        int slaveId = parseIntSafe(deviceIdField.getText(), settings.getDeviceId());
        int start = parseIntSafe(addressField.getText(), 0);
        int len = Math.max(1, parseIntSafe(lengthField.getText(), 10));
        String type = (String) pointTypeCombo.getSelectedItem();
        polls++;
        try {
            if (type.startsWith("01")) {
                ReadCoilsRequest req = new ReadCoilsRequest(slaveId, start, len);
                ReadCoilsResponse resp = (ReadCoilsResponse) modbusMaster.send(req);
                if (resp.isException()) return; else validResponses++;
                boolean[] data = resp.getBooleanData();
                updateTableBooleans(data, start);
            } else if (type.startsWith("02")) {
                ReadDiscreteInputsRequest req = new ReadDiscreteInputsRequest(slaveId, start, len);
                ReadDiscreteInputsResponse resp = (ReadDiscreteInputsResponse) modbusMaster.send(req);
                if (resp.isException()) return; else validResponses++;
                boolean[] data = resp.getBooleanData();
                updateTableBooleans(data, start);
            } else if (type.startsWith("03")) {
                ReadHoldingRegistersRequest req = new ReadHoldingRegistersRequest(slaveId, start, len);
                ReadHoldingRegistersResponse resp = (ReadHoldingRegistersResponse) modbusMaster.send(req);
                if (resp.isException()) return; else validResponses++;
                short[] data = resp.getShortData();
                updateTableRegisters(data, start);
            } else {
                ReadInputRegistersRequest req = new ReadInputRegistersRequest(slaveId, start, len);
                ReadInputRegistersResponse resp = (ReadInputRegistersResponse) modbusMaster.send(req);
                if (resp.isException()) return; else validResponses++;
                short[] data = resp.getShortData();
                updateTableRegisters(data, start);
            }
            SwingUtilities.invokeLater(() -> statusLabel.setText("Status: Reading from " + settings.getPortName()));
        } catch (ModbusTransportException e) {
            SwingUtilities.invokeLater(() -> statusLabel.setText("Status: Transport error - " + e.getMessage()));
        } finally {
            SwingUtilities.invokeLater(this::updateCounters);
        }
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
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
}