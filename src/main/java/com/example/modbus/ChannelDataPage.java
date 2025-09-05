package com.example.modbus;

import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

public class ChannelDataPage {
    private JPanel panel;
    private JLabel statusLabel;
    private JTable dataTable;
    private DefaultTableModel tableModel;
    private final ModbusSettings settings;
    private final ModbusConnectionManager connectionManager;
    private Timer timer;

    public ChannelDataPage(ModbusSettings settings) {
        this(settings, new ModbusConnectionManager());
    }

    public ChannelDataPage(ModbusSettings settings, ModbusConnectionManager connectionManager) {
        this.settings = settings;
        this.connectionManager = connectionManager;
        initializeUI();
        startPolling();
    }

    private void initializeUI() {
        panel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Status: " + (connectionManager.isOpen() ? ("Connected to " + settings.getPortName()) : "Not connected"));
        panel.add(statusLabel, BorderLayout.NORTH);

        String[] columnNames = {"Timestamp", "Register", "Value"};
        tableModel = new DefaultTableModel(columnNames, 0);
        dataTable = new JTable(tableModel);
        dataTable.setFont(new Font("Arial", Font.PLAIN, 14));
        dataTable.setRowHeight(25);
        JScrollPane scrollPane = new JScrollPane(dataTable);
        panel.add(scrollPane, BorderLayout.CENTER);
    }

    private void startPolling() {
        if (!connectionManager.isOpen()) {
            statusLabel.setText("Status: Not connected. Use Settings -> Connect.");
            return;
        }
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                readModbusData();
            }
        }, 0, 1000);
    }

    private void readModbusData() {
        ModbusMaster modbusMaster = connectionManager.getMaster();
        if (modbusMaster == null) {
            SwingUtilities.invokeLater(() -> statusLabel.setText("Status: Not connected"));
            return;
        }
        int slaveId = settings.getDeviceId();
        try {
            int startRegister = 0;
            int numRegisters = 10;
            ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId, startRegister, numRegisters);
            ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) modbusMaster.send(request);
            if (response.isException()) {
                System.err.println("Modbus exception: " + response.getExceptionMessage());
                return;
            }

            short[] data = response.getShortData();
            SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                for (int i = 0; i < data.length; i++) {
                    tableModel.addRow(new Object[]{timestamp, startRegister + i, data[i]});
                }
                statusLabel.setText("Status: Reading from " + settings.getPortName());
            });
        } catch (ModbusTransportException e) {
            SwingUtilities.invokeLater(() -> statusLabel.setText("Status: Transport error - " + e.getMessage()));
            JOptionPane.showMessageDialog(panel, "Modbus Transport Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public JPanel getPanel() {
        return panel;
    }

    public void shutdown() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}