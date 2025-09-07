package com.example.modbus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

public class ChannelConfigPage {
    private JPanel panel;
    private JComboBox<Integer> channelNumberComboBox;
    private JTextField channelNameField;
    private JTextField channelAddressField;
    private JComboBox<String> dataTypeComboBox;
    private JTextField deviceIdField;
    private JTextField valueField;
    private JTextField lowField;
    private JTextField highField;
    private JTextField offsetField;
    private JTextField maxDecimalDigitsField;
    private JComboBox<String> channelColorComboBox;
    private JTextField channelMathsField;
    private JTextField unitField;
    private JTextField writeValueField;
    private JButton saveButton;
    private JButton addNewChannelButton;
    private JButton writeButton;
    private static List<ChannelConfig> channelConfigs = new ArrayList<>();
    private DataPage dataPage;

    public ChannelConfigPage(DataPage dataPage) {
        this.dataPage = dataPage;
        panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Channel Number Dropdown
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Channel Number:"), gbc);
        channelNumberComboBox = new JComboBox<>();
        updateChannelNumberComboBox();
        gbc.gridx = 1;
        panel.add(channelNumberComboBox, gbc);
        channelNumberComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    loadChannelDetails((Integer) channelNumberComboBox.getSelectedItem());
                }
            }
        });

        // Channel Name
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Channel Name:"), gbc);
        channelNameField = new JTextField("Channel1", 10);
        gbc.gridx = 1;
        panel.add(channelNameField, gbc);

        // Channel Address
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Channel Address:"), gbc);
        channelAddressField = new JTextField("3000", 5);
        gbc.gridx = 1;
        panel.add(channelAddressField, gbc);

        // Data Type
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Data Type:"), gbc);
        dataTypeComboBox = new JComboBox<>(new String[]{"Float32", "Int16"});
        gbc.gridx = 1;
        panel.add(dataTypeComboBox, gbc);

        // Device ID
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("Device ID:"), gbc);
        deviceIdField = new JTextField("1", 5);
        gbc.gridx = 1;
        panel.add(deviceIdField, gbc);

        // Value
        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(new JLabel("Value:"), gbc);
        valueField = new JTextField("0.0", 5);
        gbc.gridx = 1;
        panel.add(valueField, gbc);

        // Low
        gbc.gridx = 0;
        gbc.gridy = 6;
        panel.add(new JLabel("Low:"), gbc);
        lowField = new JTextField("0.0", 5);
        gbc.gridx = 1;
        panel.add(lowField, gbc);

        // High
        gbc.gridx = 0;
        gbc.gridy = 7;
        panel.add(new JLabel("High:"), gbc);
        highField = new JTextField("100.0", 5);
        gbc.gridx = 1;
        panel.add(highField, gbc);

        // Offset
        gbc.gridx = 0;
        gbc.gridy = 8;
        panel.add(new JLabel("Offset:"), gbc);
        offsetField = new JTextField("0.0", 5);
        gbc.gridx = 1;
        panel.add(offsetField, gbc);

        // Max Decimal Digits
        gbc.gridx = 0;
        gbc.gridy = 9;
        panel.add(new JLabel("Max Decimal Digits:"), gbc);
        maxDecimalDigitsField = new JTextField("2", 5);
        gbc.gridx = 1;
        panel.add(maxDecimalDigitsField, gbc);

        // Channel Color
        gbc.gridx = 0;
        gbc.gridy = 10;
        panel.add(new JLabel("Channel Color:"), gbc);
        channelColorComboBox = new JComboBox<>(new String[]{"Red", "Green", "Blue", "Black", "Orange"});
        gbc.gridx = 1;
        panel.add(channelColorComboBox, gbc);

        // Channel Maths
        gbc.gridx = 0;
        gbc.gridy = 11;
        panel.add(new JLabel("Channel Maths:"), gbc);
        channelMathsField = new JTextField("x", 10);
        gbc.gridx = 1;
        panel.add(channelMathsField, gbc);

        // Unit
        gbc.gridx = 0;
        gbc.gridy = 12;
        panel.add(new JLabel("Unit:"), gbc);
        unitField = new JTextField("°C", 5);
        gbc.gridx = 1;
        panel.add(unitField, gbc);

        // Write Value (for writable registers)
        gbc.gridx = 0;
        gbc.gridy = 13;
        panel.add(new JLabel("Write Value:"), gbc);
        writeValueField = new JTextField("0", 10);
        gbc.gridx = 1;
        panel.add(writeValueField, gbc);

        // Add New Channel Button
        addNewChannelButton = new JButton("Add New Channel");
        gbc.gridx = 0;
        gbc.gridy = 14;
        gbc.gridwidth = 1;
        panel.add(addNewChannelButton, gbc);

        // Save Button
        saveButton = new JButton("Save Channel");
        gbc.gridx = 1;
        gbc.gridy = 14;
        gbc.gridwidth = 1;
        panel.add(saveButton, gbc);

        // Write Button
        writeButton = new JButton("Write to Register");
        gbc.gridx = 0;
        gbc.gridy = 15;
        gbc.gridwidth = 2;
        panel.add(writeButton, gbc);

        // Add New Channel Action
        addNewChannelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                channelNumberComboBox.setSelectedIndex(0); // Select placeholder
                channelNameField.setText("Channel" + (channelConfigs.size() + 1));
                channelAddressField.setText("40001"); // Default to writable register
                dataTypeComboBox.setSelectedItem("Float32");
                deviceIdField.setText("1");
                valueField.setText("0.0");
                lowField.setText("0.0");
                highField.setText("100.0");
                offsetField.setText("0.0");
                maxDecimalDigitsField.setText("2");
                channelColorComboBox.setSelectedItem("Red");
                channelMathsField.setText("x");
                unitField.setText("°C");
                writeValueField.setText("0");
            }
        });

        // Save Action
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int channelNumber = (Integer) channelNumberComboBox.getSelectedItem();
                    String channelName = channelNameField.getText().trim();
                    int channelAddress = Integer.parseInt(channelAddressField.getText());
                    String dataType = (String) dataTypeComboBox.getSelectedItem();
                    int deviceId = Integer.parseInt(deviceIdField.getText());
                    double value = Double.parseDouble(valueField.getText());
                    double low = Double.parseDouble(lowField.getText());
                    double high = Double.parseDouble(highField.getText());
                    double offset = Double.parseDouble(offsetField.getText());
                    int maxDecimalDigits = Integer.parseInt(maxDecimalDigitsField.getText());
                    String colorName = (String) channelColorComboBox.getSelectedItem();
                    String channelMaths = channelMathsField.getText().trim();
                    String unit = unitField.getText().trim();

                    // Check for duplicate channel name
                    for (ChannelConfig config : channelConfigs) {
                        if (config.getChannelName().equals(channelName) && config.getChannelNumber() != channelNumber) {
                            JOptionPane.showMessageDialog(panel, "Channel name '" + channelName + "' already exists.");
                            return;
                        }
                    }

                    if (channelName.isEmpty()) {
                        JOptionPane.showMessageDialog(panel, "Channel Name cannot be empty.");
                        return;
                    }
                    if ("Float32".equals(dataType) && (channelAddress % 2 != 0)) {
                        JOptionPane.showMessageDialog(panel, "Channel Address must be even for Float32.");
                        return;
                    }
                    if (low >= high) {
                        JOptionPane.showMessageDialog(panel, "Low value must be less than High value.");
                        return;
                    }
                    if (maxDecimalDigits < 0) {
                        JOptionPane.showMessageDialog(panel, "Max Decimal Digits must be non-negative.");
                        return;
                    }
                    if (channelMaths.isEmpty()) channelMaths = "x";
                    if (unit.isEmpty()) {
                        JOptionPane.showMessageDialog(panel, "Unit cannot be empty.");
                        return;
                    }

                    Color channelColor;
                    if (colorName.equals("Red")) channelColor = Color.RED;
                    else if (colorName.equals("Green")) channelColor = Color.GREEN;
                    else if (colorName.equals("Blue")) channelColor = Color.BLUE;
                    else if (colorName.equals("Black")) channelColor = Color.BLACK;
                    else if (colorName.equals("Orange")) channelColor = Color.ORANGE;
                    else channelColor = Color.BLACK;

                    ChannelConfig config = new ChannelConfig(channelNumber, channelAddress, dataType, deviceId, value, low, high, offset, maxDecimalDigits, channelColor, channelMaths, unit, channelName);

                    if (channelNumber == 0) {
                        // New channel
                        channelNumber = channelConfigs.size() + 1;
                        config = new ChannelConfig(channelNumber, channelAddress, dataType, deviceId, value, low, high, offset, maxDecimalDigits, channelColor, channelMaths, unit, channelName);
                        channelConfigs.add(config);
                        JOptionPane.showMessageDialog(panel, "New channel saved successfully!");
                    } else {
                        // Edit existing channel
                        for (int i = 0; i < channelConfigs.size(); i++) {
                            if (channelConfigs.get(i).getChannelNumber() == channelNumber) {
                                channelConfigs.set(i, config);
                                JOptionPane.showMessageDialog(panel, "Channel " + channelNumber + " updated successfully!");
                                break;
                            }
                        }
                    }

                    updateChannelNumberComboBox();
                    if (dataPage != null) {
                        dataPage.refreshTable();
                    }

                    // Reset fields after save
                    addNewChannelButton.doClick();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(panel, "Please enter valid numeric values.");
                }
            }
        });

        // Write Action
        writeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int deviceId = Integer.parseInt(deviceIdField.getText());
                    int address = Integer.parseInt(channelAddressField.getText());
                    String dataType = (String) dataTypeComboBox.getSelectedItem();
                    String writeValueStr = writeValueField.getText().trim();
                    
                    if (writeValueStr.isEmpty()) {
                        JOptionPane.showMessageDialog(panel, "Please enter a value to write.");
                        return;
                    }
                    
                    // Check if address is writable
                    if (address < 1 || (address >= 10001 && address < 40001) || address >= 50000) {
                        JOptionPane.showMessageDialog(panel, "Cannot write to read-only register address: " + address + 
                            "\nWritable addresses: 1-10000 (Coils) or 40001-49999 (Holding Registers)");
                        return;
                    }
                    
                    // Get the ModbusMaster from DataPage
                    if (dataPage == null) {
                        JOptionPane.showMessageDialog(panel, "DataPage not available for Modbus communication.");
                        return;
                    }
                    
                    // Create a temporary ChannelRuntimeService for writing
                    ChannelRuntimeService writeService = new ChannelRuntimeService(dataPage.getSettings(), dataPage.getModbusMaster());
                    
                    Object writeValue;
                    if (address >= 1 && address < 10000) {
                        // Coil - convert to boolean
                        String lowerValue = writeValueStr.toLowerCase();
                        if ("true".equals(lowerValue) || "1".equals(lowerValue) || "on".equals(lowerValue)) {
                            writeValue = true;
                        } else if ("false".equals(lowerValue) || "0".equals(lowerValue) || "off".equals(lowerValue)) {
                            writeValue = false;
                        } else {
                            JOptionPane.showMessageDialog(panel, "For coils, enter: true/false, 1/0, or on/off");
                            return;
                        }
                    } else {
                        // Holding Register - convert to number
                        try {
                            if ("Float32".equalsIgnoreCase(dataType)) {
                                writeValue = Float.parseFloat(writeValueStr);
                            } else {
                                writeValue = Short.parseShort(writeValueStr);
                            }
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(panel, "Please enter a valid number for register write.");
                            return;
                        }
                    }
                    
                    // Perform the write operation
                    boolean success = writeService.writeValue(deviceId, address, writeValue, dataType);
                    
                    if (success) {
                        JOptionPane.showMessageDialog(panel, "Successfully wrote value " + writeValue + 
                            " to register " + address + " on device " + deviceId);
                    } else {
                        JOptionPane.showMessageDialog(panel, "Failed to write value to register " + address + 
                            " on device " + deviceId + "\nCheck console for error details.");
                    }
                    
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(panel, "Please enter valid numeric values for Device ID and Address.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel, "Error during write operation: " + ex.getMessage());
                }
            }
        });
    }

    private void updateChannelNumberComboBox() {
        channelNumberComboBox.removeAllItems();
        channelNumberComboBox.addItem(0); // Placeholder for new channel
        for (ChannelConfig config : channelConfigs) {
            channelNumberComboBox.addItem(config.getChannelNumber());
        }
        if (channelConfigs.isEmpty()) {
            channelNumberComboBox.addItem(1);
        }
    }

    private void loadChannelDetails(Integer channelNumber) {
        if (channelNumber == 0 || channelNumber == null) return;
        for (ChannelConfig config : channelConfigs) {
            if (config.getChannelNumber() == channelNumber) {
                channelNameField.setText(config.getChannelName());
                channelAddressField.setText(String.valueOf(config.getChannelAddress()));
                dataTypeComboBox.setSelectedItem(config.getDataType());
                deviceIdField.setText(String.valueOf(config.getDeviceId()));
                valueField.setText(String.valueOf(config.getValue()));
                lowField.setText(String.valueOf(config.getLow()));
                highField.setText(String.valueOf(config.getHigh()));
                offsetField.setText(String.valueOf(config.getOffset()));
                maxDecimalDigitsField.setText(String.valueOf(config.getMaxDecimalDigits()));
                channelColorComboBox.setSelectedItem(getColorName(config.getChannelColor()));
                channelMathsField.setText(config.getChannelMaths());
                unitField.setText(config.getUnit());
                writeValueField.setText("0"); // Reset write value
                break;
            }
        }
    }

    private String getColorName(Color color) {
        if (color.equals(Color.RED)) return "Red";
        else if (color.equals(Color.GREEN)) return "Green";
        else if (color.equals(Color.BLUE)) return "Blue";
        else if (color.equals(Color.BLACK)) return "Black";
        else if (color.equals(Color.ORANGE)) return "Orange";
        else return "Black";
    }

    public JPanel getPanel() {
        return panel;
    }

    public static List<ChannelConfig> getChannelConfigs() {
        return channelConfigs;
    }
}