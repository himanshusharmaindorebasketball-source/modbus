package com.example.modbus;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import javax.swing.border.EmptyBorder;

public class SettingsPage {
    private JPanel panel;
    private JTextField portNameField;
    private JTextField baudRateField;
    private JTextField dataBitsField;
    private JTextField stopBitsField;
    private JComboBox<String> parityComboBox;
    private JTextField deviceIdField;
    private JButton saveButton;
    private JButton connectButton;
    private Consumer<ModbusSettings> onSettingsSaved;
    private Consumer<ModbusSettings> onConnectRequested;

    public SettingsPage(Consumer<ModbusSettings> onSettingsSaved) {
        this(onSettingsSaved, null);
    }

    public SettingsPage(Consumer<ModbusSettings> onSettingsSaved, Consumer<ModbusSettings> onConnectRequested) {
        this.onSettingsSaved = onSettingsSaved;
        this.onConnectRequested = onConnectRequested;

        panel = new JPanel(new BorderLayout());
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 16, 16, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;

        int row = 0;

        // Port Name
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        form.add(new JLabel("Port Name:"), gbc);
        portNameField = new JTextField("COM7", 20);
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(portNameField, gbc);
        row++;

        // Baud Rate
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        form.add(new JLabel("Baud Rate:"), gbc);
        baudRateField = new JTextField("19200", 20);
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(baudRateField, gbc);
        row++;

        // Data Bits
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        form.add(new JLabel("Data Bits:"), gbc);
        dataBitsField = new JTextField("8", 20);
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(dataBitsField, gbc);
        row++;

        // Stop Bits
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        form.add(new JLabel("Stop Bits:"), gbc);
        stopBitsField = new JTextField("1", 20);
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(stopBitsField, gbc);
        row++;

        // Parity
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        form.add(new JLabel("Parity:"), gbc);
        parityComboBox = new JComboBox<>(new String[]{"None", "Even", "Odd"});
        parityComboBox.setSelectedIndex(0);
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(parityComboBox, gbc);
        row++;

        // Device ID
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        form.add(new JLabel("Device ID:"), gbc);
        deviceIdField = new JTextField("1", 20);
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(deviceIdField, gbc);
        row++;

        // Buttons row
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveButton = new JButton("Save Settings");
        saveButton.addActionListener(e -> saveSettings());
        buttons.add(saveButton);
        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connect());
        buttons.add(connectButton);

        panel.add(form, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
    }

    public void setInitialSettings(ModbusSettings settings) {
        if (settings != null) {
            portNameField.setText(settings.getPortName());
            baudRateField.setText(String.valueOf(settings.getBaudRate()));
            dataBitsField.setText(String.valueOf(settings.getDataBits()));
            stopBitsField.setText(String.valueOf(settings.getStopBits()));
            parityComboBox.setSelectedIndex(getParityIndex(settings.getParity()));
            deviceIdField.setText(String.valueOf(settings.getDeviceId()));
        }
    }

    private int getParityIndex(int parity) {
        switch (parity) {
            case com.fazecast.jSerialComm.SerialPort.EVEN_PARITY: return 1;
            case com.fazecast.jSerialComm.SerialPort.ODD_PARITY: return 2;
            default: return 0; // NO_PARITY
        }
    }

    private ModbusSettings readSettingsFromForm() throws NumberFormatException {
        ModbusSettings settings = new ModbusSettings();
        settings.setPortName(portNameField.getText().trim());
        settings.setBaudRate(Integer.parseInt(baudRateField.getText().trim()));
        settings.setDataBits(Integer.parseInt(dataBitsField.getText().trim()));
        settings.setStopBits(Integer.parseInt(stopBitsField.getText().trim()));
        String parityStr = (String) parityComboBox.getSelectedItem();
        settings.setParity(parityStr.equals("Even") ? com.fazecast.jSerialComm.SerialPort.EVEN_PARITY :
                parityStr.equals("Odd") ? com.fazecast.jSerialComm.SerialPort.ODD_PARITY :
                        com.fazecast.jSerialComm.SerialPort.NO_PARITY);
        settings.setDeviceId(Integer.parseInt(deviceIdField.getText().trim()));
        return settings;
    }

    private void saveSettings() {
        try {
            ModbusSettings settings = readSettingsFromForm();
            if (onSettingsSaved != null) onSettingsSaved.accept(settings);
            JOptionPane.showMessageDialog(panel, "Settings saved.");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(panel, "Please enter valid numeric values.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void connect() {
        try {
            ModbusSettings settings = readSettingsFromForm();
            if (onConnectRequested != null) onConnectRequested.accept(settings);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(panel, "Please enter valid numeric values.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public JPanel getPanel() {
        return panel;
    }
}