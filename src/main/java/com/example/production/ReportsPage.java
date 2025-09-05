package com.example.production;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public class ReportsPage {
    private final DataLogger logger;
    private JPanel panel;
    private JTable table;
    private DefaultTableModel model;
    private JTextField fromField;
    private JTextField toField;
    private JButton refreshButton;
    private JButton exportButton;

    public ReportsPage(DataLogger logger) {
        this.logger = logger;
        buildUI();
        refresh();
    }

    private void buildUI() {
        panel = new JPanel(new BorderLayout());

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("From (yyyy-mm-dd):"));
        fromField = new JTextField(LocalDate.now().minusDays(1).toString(), 12);
        filters.add(fromField);
        filters.add(new JLabel("To:"));
        toField = new JTextField(LocalDate.now().toString(), 12);
        filters.add(toField);
        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        filters.add(refreshButton);
        exportButton = new JButton("Export CSV");
        exportButton.addActionListener(e -> export());
        filters.add(exportButton);

        panel.add(filters, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"Timestamp", "Channel", "Register", "Value"}, 0);
        table = new JTable(model);
        table.setRowHeight(24);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void refresh() {
        model.setRowCount(0);
        List<String[]> rows = logger.readAll();
        String from = fromField.getText().trim();
        String to = toField.getText().trim();
        for (String[] r : rows) {
            if (r.length < 4) continue;
            String ts = r[0];
            if (!ts.startsWith(from) && ts.compareTo(from) < 0) continue;
            if (!ts.startsWith(to) && ts.compareTo(to) > 0) continue;
            model.addRow(new Object[]{r[0], r[1], r[2], r[3]});
        }
    }

    private void export() {
        Path src = logger.getFilePath();
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("production_export.csv"));
        int res = chooser.showSaveDialog(panel);
        if (res == JFileChooser.APPROVE_OPTION) {
            try {
                Files.copy(src, chooser.getSelectedFile().toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                JOptionPane.showMessageDialog(panel, "Exported to " + chooser.getSelectedFile().getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(panel, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public JPanel getPanel() { return panel; }
}

