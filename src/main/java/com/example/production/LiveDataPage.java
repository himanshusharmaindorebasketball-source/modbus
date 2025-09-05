package com.example.production;

import com.example.modbus.ModbusSettings;
import com.example.modbus.SimpleSerialPortWrapper;
import com.serotonin.modbus4j.ModbusFactory;
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
import java.util.function.BiConsumer;

public class LiveDataPage {
    private JPanel panel;
    private JLabel statusLabel;
    private JTable table;
    private DefaultTableModel model;
    private ModbusMaster modbusMaster;
    private SimpleSerialPortWrapper serialPortWrapper;
    private final ModbusSettings settings;
    private final DataLogger dataLogger;
    private final BiConsumer<Integer, Double> onSample;

    public LiveDataPage(ModbusSettings settings, DataLogger dataLogger, BiConsumer<Integer, Double> onSample) {
        this.settings = settings;
        this.dataLogger = dataLogger;
        this.onSample = onSample;
        buildUI();
        initModbus();
        if (modbusMaster != null) startPolling();
    }

    private void buildUI() {
        panel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Status: Initializing...");
        panel.add(statusLabel, BorderLayout.NORTH);
        model = new DefaultTableModel(new String[]{"Timestamp", "Register", "Value"}, 0);
        table = new JTable(model);
        table.setRowHeight(24);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void initModbus() {
        try {
            serialPortWrapper = new SimpleSerialPortWrapper();
            serialPortWrapper.openPort(settings);
            ModbusFactory factory = new ModbusFactory();
            modbusMaster = factory.createRtuMaster(serialPortWrapper);
            modbusMaster.setTimeout(2000);
            modbusMaster.setRetries(3);
            modbusMaster.init();
            SwingUtilities.invokeLater(() -> statusLabel.setText("Status: Connected to " + settings.getPortName()));
        } catch (Exception e) {
            modbusMaster = null;
            SwingUtilities.invokeLater(() -> statusLabel.setText("Status: Init error - " + e.getMessage()));
        }
    }

    private void startPolling() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                pollOnce();
            }
        }, 0, 1000);
    }

    private void pollOnce() {
        if (modbusMaster == null) return;
        int slaveId = settings.getDeviceId();
        int startRegister = 0;
        int numRegisters = 10;
        try {
            ReadHoldingRegistersRequest req = new ReadHoldingRegistersRequest(slaveId, startRegister, numRegisters);
            ReadHoldingRegistersResponse resp = (ReadHoldingRegistersResponse) modbusMaster.send(req);
            if (resp.isException()) return;
            short[] data = resp.getShortData();
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            SwingUtilities.invokeLater(() -> {
                model.setRowCount(0);
                for (int i = 0; i < data.length; i++) {
                    int register = startRegister + i;
                    double value = data[i];
                    model.addRow(new Object[]{ts, register, value});
                    dataLogger.appendSample("R" + register, register, value);
                    if (onSample != null) onSample.accept(register, value);
                }
                statusLabel.setText("Status: Reading from " + settings.getPortName());
            });
        } catch (ModbusTransportException ignored) {}
    }

    public JPanel getPanel() { return panel; }

    public void shutdown() {
        if (modbusMaster != null) {
            modbusMaster.destroy();
            modbusMaster = null;
        }
        if (serialPortWrapper != null) {
            serialPortWrapper.closePort();
            serialPortWrapper = null;
        }
    }
}

