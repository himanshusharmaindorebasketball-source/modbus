package com.example.production;

import com.example.modbus.ModbusSettings;
import com.example.modbus.SettingsPage;

import javax.swing.*;
import java.awt.*;

public class ProductionMonitorGUI {
    private JFrame frame;
    private DashboardPage dashboardPage;
    private LiveDataPage liveDataPage;
    private ReportsPage reportsPage;
    private SettingsPage settingsPage;
    private ModbusSettings settings;

    public ProductionMonitorGUI() {
        this(defaultSettings());
    }

    public ProductionMonitorGUI(ModbusSettings initialSettings) {
        if (initialSettings == null) {
            this.settings = defaultSettings();
        } else {
            this.settings = cloneSettings(initialSettings);
        }
        buildUI();
    }

    private static ModbusSettings defaultSettings() {
        ModbusSettings s = new ModbusSettings();
        s.setPortName("COM1");
        s.setBaudRate(9600);
        s.setDataBits(8);
        s.setParity(0);
        s.setStopBits(1);
        s.setDeviceId(1);
        return s;
    }

    private static ModbusSettings cloneSettings(ModbusSettings src) {
        ModbusSettings s = new ModbusSettings();
        s.setPortName(src.getPortName());
        s.setBaudRate(src.getBaudRate());
        s.setDataBits(src.getDataBits());
        s.setParity(src.getParity());
        s.setStopBits(src.getStopBits());
        s.setDeviceId(src.getDeviceId());
        return s;
    }

    private void buildUI() {
        frame = new JFrame("Production Monitor");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(1000, 700);

        DataLogger dataLogger = new DataLogger();

        dashboardPage = new DashboardPage(dataLogger);
        liveDataPage = new LiveDataPage(settings, dataLogger, dashboardPage::ingestSample);
        reportsPage = new ReportsPage(dataLogger);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Dashboard", dashboardPage.getPanel());
        tabs.addTab("Live Data", liveDataPage.getPanel());
        tabs.addTab("Reports", reportsPage.getPanel());

        settingsPage = new SettingsPage(updated -> {
            settings = cloneSettings(updated);
            liveDataPage.shutdown();
            liveDataPage = new LiveDataPage(settings, dataLogger, dashboardPage::ingestSample);
            int idx = tabs.indexOfTab("Live Data");
            if (idx >= 0) tabs.setComponentAt(idx, liveDataPage.getPanel());
        });
        settingsPage.setInitialSettings(settings);
        tabs.addTab("Settings", settingsPage.getPanel());

        frame.add(tabs, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    public void applySettings(ModbusSettings updated) {
        if (updated == null) return;
        this.settings = cloneSettings(updated);
        if (settingsPage != null) settingsPage.setInitialSettings(this.settings);
        if (liveDataPage != null) {
            liveDataPage.shutdown();
            DataLogger dataLogger = new DataLogger();
            liveDataPage = new LiveDataPage(this.settings, dataLogger, dashboardPage::ingestSample);
        }
    }

    public void bringToFront() {
        if (frame != null) {
            frame.setVisible(true);
            frame.toFront();
            frame.requestFocus();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ProductionMonitorGUI::new);
    }
}
