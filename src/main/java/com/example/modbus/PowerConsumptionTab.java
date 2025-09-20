package com.example.modbus;

import com.example.production.PowerConsumptionCalculator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Power Consumption Tab for the Modbus GUI
 * Integrates power consumption analysis as a tab in the main application
 */
public class PowerConsumptionTab extends JPanel {
    
    private PowerConsumptionCalculator calculator;
    private JTable dataTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> energyFieldComboBox;
    private JTextField startDateField;
    private JTextField startTimeField;
    private JTextField endDateField;
    private JTextField endTimeField;
    private JLabel statusLabel;
    private JLabel summaryLabel;
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public PowerConsumptionTab() {
        this.calculator = new PowerConsumptionCalculator();
        initializeComponents();
        loadInitialData();
    }
    
    private void initializeComponents() {
        setLayout(new BorderLayout(5, 5)); // Add padding between components
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add margin around the entire panel
        
        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        
        // Create control panel
        JPanel controlPanel = createControlPanel();
        controlPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        
        // Create data table
        createDataTable();
        JScrollPane scrollPane = new JScrollPane(dataTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Report Data"));
        scrollPane.setPreferredSize(new Dimension(800, 300));
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Create status panel
        JPanel statusPanel = createStatusPanel();
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private JPanel createControlPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createTitledBorder("Reports"));
        
        // Top row - Channel selection
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topRow.add(new JLabel("Channel:"));
        energyFieldComboBox = new JComboBox<>();
        energyFieldComboBox.setPreferredSize(new Dimension(200, 25));
        loadAvailableChannels();
        topRow.add(energyFieldComboBox);
        
        mainPanel.add(topRow, BorderLayout.NORTH);
        
        // Middle row - Date and Time inputs
        JPanel middleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        middleRow.add(new JLabel("Start Date:"));
        startDateField = new JTextField(10);
        startDateField.setText(LocalDate.now().minusDays(7).format(DATE_FORMAT));
        middleRow.add(startDateField);
        
        middleRow.add(new JLabel("Start Time:"));
        startTimeField = new JTextField(8);
        startTimeField.setText("00:00:00");
        middleRow.add(startTimeField);
        
        middleRow.add(Box.createHorizontalStrut(20)); // Spacer
        
        middleRow.add(new JLabel("End Date:"));
        endDateField = new JTextField(10);
        endDateField.setText(LocalDate.now().format(DATE_FORMAT));
        middleRow.add(endDateField);
        
        middleRow.add(new JLabel("End Time:"));
        endTimeField = new JTextField(8);
        endTimeField.setText("23:59:59");
        middleRow.add(endTimeField);
        
        mainPanel.add(middleRow, BorderLayout.CENTER);
        
        // Bottom row - Buttons
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        bottomRow.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        JButton generateButton = new JButton("Generate Report");
        generateButton.setPreferredSize(new Dimension(120, 30));
        generateButton.addActionListener(new GenerateReportListener());
        bottomRow.add(generateButton);
        
        JButton exportButton = new JButton("Export CSV");
        exportButton.setPreferredSize(new Dimension(100, 30));
        exportButton.addActionListener(new ExportCSVListener());
        bottomRow.add(exportButton);
        
        JButton refreshButton = new JButton("Refresh Data");
        refreshButton.setPreferredSize(new Dimension(120, 30));
        refreshButton.addActionListener(new RefreshDataListener());
        bottomRow.add(refreshButton);
        
        mainPanel.add(bottomRow, BorderLayout.SOUTH);
        
        return mainPanel;
    }
    
    private void createDataTable() {
        String[] columnNames = {
            "Period", "Start Date", "End Date", "Total Consumption (kWh)", 
            "Average (kWh)", "Min (kWh)", "Max (kWh)", "Data Points"
        };
        
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        
        dataTable = new JTable(tableModel);
        dataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dataTable.setRowHeight(25);
        dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        // Set column widths
        dataTable.getColumnModel().getColumn(0).setPreferredWidth(250);
        dataTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        dataTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        dataTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        dataTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        dataTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        dataTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        dataTable.getColumnModel().getColumn(7).setPreferredWidth(80);
    }
    
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Summary label
        summaryLabel = new JLabel("Ready to generate report");
        summaryLabel.setFont(summaryLabel.getFont().deriveFont(Font.BOLD));
        panel.add(summaryLabel, BorderLayout.WEST);
        
        // Status label
        statusLabel = new JLabel("Status: Ready");
        panel.add(statusLabel, BorderLayout.EAST);
        
        return panel;
    }
    
    private void loadAvailableChannels() {
        List<String> allChannels = getAllAvailableChannels();
        energyFieldComboBox.removeAllItems();
        
        if (allChannels.isEmpty()) {
            energyFieldComboBox.addItem("No channels found");
        } else {
            for (String channel : allChannels) {
                energyFieldComboBox.addItem(channel);
            }
            // Set default to "Energy Consumption" if available
            if (allChannels.contains("Energy Consumption")) {
                energyFieldComboBox.setSelectedItem("Energy Consumption");
            }
        }
    }
    
    private List<String> getAllAvailableChannels() {
        List<String> channels = new ArrayList<>();
        
        // Load Modbus channels from modbus_config.json
        try {
            File modbusConfigFile = new File("modbus_config.json");
            if (modbusConfigFile.exists()) {
                String jsonContent = new String(java.nio.file.Files.readAllBytes(modbusConfigFile.toPath()));
                com.google.gson.Gson gson = new com.google.gson.Gson();
                com.google.gson.reflect.TypeToken<List<ModbusChannel>> typeToken = new com.google.gson.reflect.TypeToken<List<ModbusChannel>>(){};
                List<ModbusChannel> modbusChannels = gson.fromJson(jsonContent, typeToken.getType());
                
                if (modbusChannels != null) {
                    for (ModbusChannel channel : modbusChannels) {
                        if (channel.channelName != null && !channel.channelName.trim().isEmpty()) {
                            channels.add(channel.channelName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading Modbus channels: " + e.getMessage());
        }
        
        // Load Math channels from math_channels.json
        try {
            File mathChannelsFile = new File("math_channels.json");
            if (mathChannelsFile.exists()) {
                String jsonContent = new String(java.nio.file.Files.readAllBytes(mathChannelsFile.toPath()));
                com.google.gson.Gson gson = new com.google.gson.Gson();
                com.google.gson.reflect.TypeToken<List<MathChannel>> typeToken = new com.google.gson.reflect.TypeToken<List<MathChannel>>(){};
                List<MathChannel> mathChannels = gson.fromJson(jsonContent, typeToken.getType());
                
                if (mathChannels != null) {
                    for (MathChannel channel : mathChannels) {
                        if (channel.channelName != null && !channel.channelName.trim().isEmpty() && channel.enabled) {
                            channels.add(channel.channelName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading Math channels: " + e.getMessage());
        }
        
        // Remove duplicates and sort
        return channels.stream().distinct().sorted().collect(java.util.stream.Collectors.toList());
    }
    
    // Inner classes for JSON parsing
    private static class ModbusChannel {
        public int slaveId;
        public int address;
        public int length;
        public String dataType;
        public String channelName;
    }
    
    private static class MathChannel {
        public String channelName;
        public String expression;
        public String description;
        public String unit;
        public int decimalPlaces;
        public boolean enabled;
    }
    
    private void loadInitialData() {
        // Get available date range
        Map<String, LocalDate> dateRange = calculator.getAvailableDateRange();
        LocalDate startDate = dateRange.get("start");
        LocalDate endDate = dateRange.get("end");
        
        // Update date and time fields with available range
        startDateField.setText(startDate.format(DATE_FORMAT));
        startTimeField.setText("00:00:00");
        endDateField.setText(endDate.format(DATE_FORMAT));
        endTimeField.setText("23:59:59");
        
        // Update status
        statusLabel.setText("Status: Data loaded from " + startDate + " to " + endDate);
        
        // Generate initial report
        generateReport();
    }
    
    private void generateReport() {
        try {
            // Clear existing data
            tableModel.setRowCount(0);
            
            // Parse dates and times
            LocalDate startDate = LocalDate.parse(startDateField.getText(), DATE_FORMAT);
            LocalTime startTime = LocalTime.parse(startTimeField.getText(), TIME_FORMAT);
            LocalDateTime startDateTime = LocalDateTime.of(startDate, startTime);
            
            LocalDate endDate = LocalDate.parse(endDateField.getText(), DATE_FORMAT);
            LocalTime endTime = LocalTime.parse(endTimeField.getText(), TIME_FORMAT);
            LocalDateTime endDateTime = LocalDateTime.of(endDate, endTime);
            
            // Validate datetime range
            if (startDateTime.isAfter(endDateTime)) {
                JOptionPane.showMessageDialog(this, "Start datetime cannot be after end datetime", "Invalid DateTime Range", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            statusLabel.setText("Status: Generating report...");
            
            // Set the selected channel
            String selectedChannel = (String) energyFieldComboBox.getSelectedItem();
            if (selectedChannel != null && !selectedChannel.equals("No channels found")) {
                calculator.setEnergyConsumptionKey(selectedChannel);
            }
            
            // Calculate consumption as difference between two specific readings
            PowerConsumptionCalculator.ConsumptionSummary reportSummary = calculator.calculateConsumptionBetweenReadings(startDateTime, endDateTime);
            List<PowerConsumptionCalculator.ConsumptionSummary> summaries = new ArrayList<>();
            summaries.add(reportSummary);
            
            // Populate table
            for (PowerConsumptionCalculator.ConsumptionSummary summary : summaries) {
                Object[] row = {
                    summary.getPeriod(),
                    summary.getStartDate(),
                    summary.getEndDate(),
                    String.format("%.2f", summary.getTotalConsumption()),
                    String.format("%.2f", summary.getAverageConsumption()),
                    String.format("%.2f", summary.getMinConsumption()),
                    String.format("%.2f", summary.getMaxConsumption()),
                    summary.getDataPoints()
                };
                tableModel.addRow(row);
            }
            
            // Update summary
            double totalConsumption = summaries.stream().mapToDouble(PowerConsumptionCalculator.ConsumptionSummary::getTotalConsumption).sum();
            int totalDataPoints = summaries.stream().mapToInt(PowerConsumptionCalculator.ConsumptionSummary::getDataPoints).sum();
            
            summaryLabel.setText(String.format("Total Consumption: %.2f kWh | Data Points: %d | Periods: %d", 
                    totalConsumption, totalDataPoints, summaries.size()));
            
            statusLabel.setText("Status: Report generated successfully");
            
        } catch (Exception e) {
            statusLabel.setText("Status: Error generating report");
            JOptionPane.showMessageDialog(this, "Error generating report: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void exportToCSV() {
        try {
            // Parse dates and times
            LocalDate startDate = LocalDate.parse(startDateField.getText(), DATE_FORMAT);
            LocalTime startTime = LocalTime.parse(startTimeField.getText(), TIME_FORMAT);
            LocalDateTime startDateTime = LocalDateTime.of(startDate, startTime);
            
            LocalDate endDate = LocalDate.parse(endDateField.getText(), DATE_FORMAT);
            LocalTime endTime = LocalTime.parse(endTimeField.getText(), TIME_FORMAT);
            LocalDateTime endDateTime = LocalDateTime.of(endDate, endTime);
            
            // Set the selected channel
            String selectedChannel = (String) energyFieldComboBox.getSelectedItem();
            if (selectedChannel != null && !selectedChannel.equals("No channels found")) {
                calculator.setEnergyConsumptionKey(selectedChannel);
            }
            
            // Calculate consumption as difference between two specific readings
            PowerConsumptionCalculator.ConsumptionSummary exportSummary = calculator.calculateConsumptionBetweenReadings(startDateTime, endDateTime);
            List<PowerConsumptionCalculator.ConsumptionSummary> summaries = new ArrayList<>();
            summaries.add(exportSummary);
            
            // Choose file location
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File("report_" + 
                    startDate.format(DATE_FORMAT) + "_to_" + endDate.format(DATE_FORMAT) + ".csv"));
            
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                calculator.exportToCSV(summaries, selectedFile.getAbsolutePath());
                JOptionPane.showMessageDialog(this, "Data exported successfully to: " + selectedFile.getName(), "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error exporting data: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void refreshData() {
        statusLabel.setText("Status: Refreshing data...");
        
        // Reload available channels
        loadAvailableChannels();
        
        // Reload available date range
        Map<String, LocalDate> dateRange = calculator.getAvailableDateRange();
        LocalDate startDate = dateRange.get("start");
        LocalDate endDate = dateRange.get("end");
        
        // Update date and time fields
        startDateField.setText(startDate.format(DATE_FORMAT));
        startTimeField.setText("00:00:00");
        endDateField.setText(endDate.format(DATE_FORMAT));
        endTimeField.setText("23:59:59");
        
        statusLabel.setText("Status: Data refreshed");
        
        // Regenerate report
        generateReport();
    }
    
    // Event listeners
    private class GenerateReportListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            generateReport();
        }
    }
    
    private class ExportCSVListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            exportToCSV();
        }
    }
    
    private class RefreshDataListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            refreshData();
        }
    }
}
