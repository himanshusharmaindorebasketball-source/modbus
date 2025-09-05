package com.example.modbus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ModbusGUI {
    private JFrame mainFrame;
    private DataPage dataPage;
    private SettingsPage settingsPage;
    private ModbusSettings settings;
    private ChannelRuntimeService channelRuntimeService;
    private ChannelDefinitionPage channelDefinitionPage;
    private ChannelDataArrangementPage channelDataArrangementPage;
    private com.example.production.ProductionMonitorGUI productionMonitor;
    private final ModbusConnectionManager connectionManager = new ModbusConnectionManager();

    public ModbusGUI() {
        try {
            settings = new ModbusSettings();
            settings.setPortName("COM1");
            settings.setBaudRate(9600);
            settings.setDataBits(8);
            settings.setParity(0);
            settings.setStopBits(1);
            settings.setDeviceId(1);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Failed to initialize ModbusSettings: " + e.getMessage());
            return;
        }

        buildUI();
    }

    private void buildUI() {
        dataPage = new DataPage(settings, connectionManager);

        mainFrame = new JFrame("Modbus GUI");
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.setSize(1200, 800);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Data", dataPage.getPanel());

        try {
            channelRuntimeService = new ChannelRuntimeService(settings, connectionManager.getMaster());
            ChannelDefinitionPage.ChannelDefinitionContext.setRuntime(channelRuntimeService);
            channelDefinitionPage = new ChannelDefinitionPage(channelRuntimeService);
            channelDataArrangementPage = new ChannelDataArrangementPage(channelRuntimeService);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Channel runtime init failed: " + e.getMessage());
        }

        if (channelDefinitionPage != null) tabbedPane.addTab("Channel Definition", channelDefinitionPage.getPanel());
        if (channelDataArrangementPage != null) tabbedPane.addTab("Channel Arrangement", channelDataArrangementPage.getPanel());

        JPanel monitorPanel = new JPanel(new BorderLayout());
        JButton openMonitor = new JButton("Open Production Monitor");
        openMonitor.addActionListener(e -> {
            if (productionMonitor == null) {
                productionMonitor = new com.example.production.ProductionMonitorGUI(settings);
            } else {
                productionMonitor.applySettings(settings);
                productionMonitor.bringToFront();
            }
        });
        monitorPanel.add(new JLabel("Launch the production monitoring UI with current Modbus settings."), BorderLayout.NORTH);
        monitorPanel.add(openMonitor, BorderLayout.CENTER);
        tabbedPane.addTab("Production Monitor", monitorPanel);

        settingsPage = new SettingsPage(
                updatedSettings -> {
                    settings = updatedSettings;
                    if (productionMonitor != null) productionMonitor.applySettings(settings);
                },
                connectSettings -> {
                    try {
                        connectionManager.open(connectSettings);
                        JOptionPane.showMessageDialog(mainFrame, "Connected to " + connectSettings.getPortName());
                        settings = connectSettings;

                        if (dataPage != null) dataPage.shutdown();
                        if (channelRuntimeService != null) channelRuntimeService.shutdown();

                        dataPage = new DataPage(settings, connectionManager);
                        try {
                            channelRuntimeService = new ChannelRuntimeService(settings, connectionManager.getMaster());
                            ChannelDefinitionPage.ChannelDefinitionContext.setRuntime(channelRuntimeService);
                            channelDefinitionPage = new ChannelDefinitionPage(channelRuntimeService);
                            channelDataArrangementPage = new ChannelDataArrangementPage(channelRuntimeService);
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(null, "Channel runtime re-init failed: " + e.getMessage());
                        }

                        int dataIndex = tabbedPane.indexOfTab("Data");
                        if (dataIndex >= 0) tabbedPane.setComponentAt(dataIndex, dataPage.getPanel());
                        int chDefIndex = tabbedPane.indexOfTab("Channel Definition");
                        if (chDefIndex >= 0 && channelDefinitionPage != null) tabbedPane.setComponentAt(chDefIndex, channelDefinitionPage.getPanel());
                        else if (channelDefinitionPage != null) tabbedPane.addTab("Channel Definition", channelDefinitionPage.getPanel());
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