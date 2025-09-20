package com.example.modbus;

import com.serotonin.modbus4j.ModbusMaster;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ModbusGUI {
    private JFrame mainFrame;
    private DataPage dataPage;
    private FilterDataPage filterDataPage;
    private SettingsPage settingsPage;
    private ModbusSettings settings;
    private ChannelRuntimeService channelRuntimeService;
    private ChannelDataArrangementPage channelDataArrangementPage;
    private final ModbusConnectionManager connectionManager = new ModbusConnectionManager();

    public ModbusGUI() {
        try {
            // Load saved settings or use defaults
            SettingsManager settingsManager = new SettingsManager();
            settings = settingsManager.loadSettings();
            
            // If no saved settings exist, use defaults
            if (settings.getPortName() == null || settings.getPortName().isEmpty()) {
                settings.setPortName("COM1");
                settings.setBaudRate(9600);
                settings.setDataBits(8);
                settings.setParity(0);
                settings.setStopBits(1);
                // Device ID is no longer set here - it's configured per register
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Failed to initialize ModbusSettings: " + e.getMessage());
            return;
        }

        buildUI();
    }

    private void buildUI() {
        // Load math channel configurations at startup
        MathChannelManager.loadConfigs();
        
        dataPage = new DataPage(settings, connectionManager);
        filterDataPage = new FilterDataPage(settings, connectionManager);

        mainFrame = new JFrame("Modbus GUI");
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.setSize(1200, 800);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Data", dataPage.getPanel());
        tabbedPane.addTab("FilterData", filterDataPage.getPanel());

        try {
            // Only initialize channel runtime if we have a valid master connection
            ModbusMaster master = connectionManager.getMaster();
            if (master != null) {
                channelRuntimeService = new ChannelRuntimeService(settings, master);
                channelDataArrangementPage = new ChannelDataArrangementPage(channelRuntimeService);
                // Connect ChannelRuntimeService to FilterDataPage for math channel computation
                filterDataPage.setChannelRuntimeService(channelRuntimeService);
            } else {
                // Create channel arrangement page without runtime service for now
                channelDataArrangementPage = new ChannelDataArrangementPage(null);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Channel runtime init failed: " + e.getMessage());
            // Create channel arrangement page without runtime service as fallback
            try {
                channelDataArrangementPage = new ChannelDataArrangementPage(null);
            } catch (Exception ex) {
                System.err.println("Failed to create ChannelDataArrangementPage: " + ex.getMessage());
            }
        }

        if (channelDataArrangementPage != null) tabbedPane.addTab("Channel Arrangement", channelDataArrangementPage.getPanel());

        // Production Monitor functionality integrated into FilterDataPage Data Logger

        // Add Reports tab
        PowerConsumptionTab powerConsumptionTab = new PowerConsumptionTab();
        tabbedPane.addTab("Reports", powerConsumptionTab);

        settingsPage = new SettingsPage(
                updatedSettings -> {
                    settings = updatedSettings;
                    // Production Monitor functionality integrated into FilterDataPage
                },
                connectSettings -> {
                    try {
                        connectionManager.open(connectSettings);
                        JOptionPane.showMessageDialog(mainFrame, "Connected to " + connectSettings.getPortName());
                        settings = connectSettings;

                        if (dataPage != null) dataPage.shutdown();
                        if (filterDataPage != null) filterDataPage.shutdown();
                        if (channelRuntimeService != null) channelRuntimeService.shutdown();

                        dataPage = new DataPage(settings, connectionManager);
                        filterDataPage = new FilterDataPage(settings, connectionManager);
                        try {
                            channelRuntimeService = new ChannelRuntimeService(settings, connectionManager.getMaster());
                            channelDataArrangementPage = new ChannelDataArrangementPage(channelRuntimeService);
                            // Connect ChannelRuntimeService to FilterDataPage for math channel computation
                            filterDataPage.setChannelRuntimeService(channelRuntimeService);
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(null, "Channel runtime re-init failed: " + e.getMessage());
                        }

                        int dataIndex = tabbedPane.indexOfTab("Data");
                        if (dataIndex >= 0) tabbedPane.setComponentAt(dataIndex, dataPage.getPanel());
                        int filterDataIndex = tabbedPane.indexOfTab("FilterData");
                        if (filterDataIndex >= 0) tabbedPane.setComponentAt(filterDataIndex, filterDataPage.getPanel());
                        int chArrIndex = tabbedPane.indexOfTab("Channel Arrangement");
                        if (chArrIndex >= 0 && channelDataArrangementPage != null) tabbedPane.setComponentAt(chArrIndex, channelDataArrangementPage.getPanel());
                        else if (channelDataArrangementPage != null) tabbedPane.addTab("Channel Arrangement", channelDataArrangementPage.getPanel());
                        
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(mainFrame, "Connection failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
        );
        settingsPage.setInitialSettings(settings);
        tabbedPane.addTab("Settings", settingsPage.getPanel());

        mainFrame.add(tabbedPane, BorderLayout.CENTER);

        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (dataPage != null) dataPage.shutdown();
                if (filterDataPage != null) filterDataPage.shutdown();
                if (channelRuntimeService != null) channelRuntimeService.shutdown();
                connectionManager.close();
            }
        });

        mainFrame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ModbusGUI());
    }
}