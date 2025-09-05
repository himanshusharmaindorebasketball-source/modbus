package com.example.production;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardPage {
    private JPanel panel;
    private JLabel kpiThroughput;
    private JLabel kpiUptime;
    private JLabel kpiGoodRate;
    private DefaultTableModel recentModel;
    private final Map<Integer, Double> latestByRegister = new LinkedHashMap<>();

    public DashboardPage(DataLogger dataLogger) {
        buildUI();
    }

    private void buildUI() {
        panel = new JPanel(new BorderLayout());

        JPanel kpis = new JPanel(new GridLayout(1, 3, 16, 16));
        kpiThroughput = kpi("Throughput", "0 / min");
        kpiUptime = kpi("Uptime", "0%");
        kpiGoodRate = kpi("Good Rate", "0%");
        kpis.add(kpiThroughput);
        kpis.add(kpiUptime);
        kpis.add(kpiGoodRate);
        panel.add(kpis, BorderLayout.NORTH);

        recentModel = new DefaultTableModel(new String[]{"Register", "Latest Value"}, 0);
        JTable recent = new JTable(recentModel);
        recent.setRowHeight(24);
        panel.add(new JScrollPane(recent), BorderLayout.CENTER);
    }

    private JLabel kpi(String title, String value) {
        JLabel lbl = new JLabel(title + ": " + value, SwingConstants.CENTER);
        lbl.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 16f));
        return lbl;
    }

    public void ingestSample(int register, double value) {
        latestByRegister.put(register, value);
        SwingUtilities.invokeLater(() -> refreshRecent());
        // Simple derived KPIs placeholders
        kpiThroughput.setText("Throughput: " + Math.max(0, (int) (value)) + " / min");
        kpiUptime.setText("Uptime: " + (int) (Math.min(100, Math.abs(value))) + "%");
        kpiGoodRate.setText("Good Rate: " + (int) (Math.min(100, Math.abs(value))) + "%");
    }

    private void refreshRecent() {
        recentModel.setRowCount(0);
        for (Map.Entry<Integer, Double> e : latestByRegister.entrySet()) {
            recentModel.addRow(new Object[]{e.getKey(), e.getValue()});
        }
    }

    public JPanel getPanel() { return panel; }
}

