package com.example.modbus;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChannelDefinitionPage {
    private final JPanel panel;
    private final DefaultTableModel model;
    private final ChannelRuntimeService runtime;

    // Channel selection
    private JComboBox<Integer> channelNumberCombo;
    private JTextField channelNameField;
    private JTextField valueField;

    // Core settings
    private JTextField addressField;
    private JComboBox<String> dataTypeCombo;
    private JTextField deviceIdField;
    private JTextField offsetField;
    private JTextField lowField;
    private JTextField highField;
    private JTextField maxDecimalsField;
    private JComboBox<String> colorCombo;
    private JTextField unitField;
    private JTextField mathsField;

    private JButton addButton;
    private JButton saveButton;
    private JButton deleteButton;

    private List<ChannelConfig> channelConfigs;

    public ChannelDefinitionPage(ChannelRuntimeService runtime) {
        this.runtime = runtime;
        this.panel = new JPanel(new BorderLayout());
        this.model = new DefaultTableModel(new String[]{"Channel", "Register", "Raw", "Output", "Unit"}, 0);
        loadChannels();
        buildUI();
        runtime.addListener(this::refreshOutputTable);
        refreshForm(1);
        refreshOutputTable();
    }

    private void loadChannels() {
        channelConfigs = new ArrayList<>(ChannelRepository.load());
        if (channelConfigs.isEmpty()) {
            channelConfigs = new ArrayList<>(ChannelConfigPage.getChannelConfigs());
        }
    }

    private void saveChannels() {
        ChannelRepository.save(channelConfigs);
    }

    private void buildUI() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Channel Definition"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Channel Number"), gbc);
        channelNumberCombo = new JComboBox<>();
        gbc.gridx = 1; gbc.gridwidth = 1; form.add(channelNumberCombo, gbc);

        gbc.gridx = 2; form.add(new JLabel("Name"), gbc);
        channelNameField = new JTextField(16);
        gbc.gridx = 3; gbc.gridwidth = 2; form.add(channelNameField, gbc);
        gbc.gridwidth = 1;

        row++;
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Address"), gbc);
        addressField = new JTextField(8);
        gbc.gridx = 1; form.add(addressField, gbc);

        gbc.gridx = 2; form.add(new JLabel("Data Type"), gbc);
        dataTypeCombo = new JComboBox<>(new String[]{"Int16", "UInt16", "Float32"});
        gbc.gridx = 3; gbc.gridwidth = 2; form.add(dataTypeCombo, gbc);
        gbc.gridwidth = 1;

        row++;
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Device ID"), gbc);
        deviceIdField = new JTextField(6);
        gbc.gridx = 1; form.add(deviceIdField, gbc);

        gbc.gridx = 2; form.add(new JLabel("Offset"), gbc);
        offsetField = new JTextField(6);
        gbc.gridx = 3; form.add(offsetField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Low"), gbc);
        lowField = new JTextField(6);
        gbc.gridx = 1; form.add(lowField, gbc);
        gbc.gridx = 2; form.add(new JLabel("High"), gbc);
        highField = new JTextField(6);
        gbc.gridx = 3; form.add(highField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Max Decimals"), gbc);
        maxDecimalsField = new JTextField(6);
        gbc.gridx = 1; form.add(maxDecimalsField, gbc);

        gbc.gridx = 2; form.add(new JLabel("Colour"), gbc);
        colorCombo = new JComboBox<>(new String[]{"Red", "Green", "Blue", "Black", "Orange"});
        gbc.gridx = 3; form.add(colorCombo, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Unit"), gbc);
        unitField = new JTextField(8);
        gbc.gridx = 1; form.add(unitField, gbc);

        gbc.gridx = 2; form.add(new JLabel("Maths (use x, CHn)"), gbc);
        mathsField = new JTextField(16);
        gbc.gridx = 3; gbc.gridwidth = 2; form.add(mathsField, gbc);
        gbc.gridwidth = 1;

        row++;
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        addButton = new JButton("Add Channel");
        saveButton = new JButton("Save");
        deleteButton = new JButton("Delete");
        buttons.add(addButton);
        buttons.add(saveButton);
        buttons.add(deleteButton);

        addButton.addActionListener(e -> onAdd());
        saveButton.addActionListener(e -> onSave());
        deleteButton.addActionListener(e -> onDelete());

        JTable table = new JTable(model);
        table.setRowHeight(24);
        JScrollPane tablePane = new JScrollPane(table);
        tablePane.setPreferredSize(new Dimension(600, 240));

        JPanel left = new JPanel(new BorderLayout());
        left.add(form, BorderLayout.CENTER);
        left.add(buttons, BorderLayout.SOUTH);
        JPanel right = new JPanel(new BorderLayout());
        right.add(new JLabel("Channel Output", SwingConstants.CENTER), BorderLayout.NORTH);
        right.add(tablePane, BorderLayout.CENTER);

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.CENTER);

        refreshChannelNumbers();
        channelNumberCombo.addActionListener(e -> {
            Integer num = (Integer) channelNumberCombo.getSelectedItem();
            if (num != null) refreshForm(num);
        });
    }

    private void refreshChannelNumbers() {
        channelNumberCombo.removeAllItems();
        for (ChannelConfig c : channelConfigs) channelNumberCombo.addItem(c.getChannelNumber());
        if (channelNumberCombo.getItemCount() == 0) channelNumberCombo.addItem(1);
        channelNumberCombo.setSelectedIndex(0);
    }

    private void refreshForm(int channelNumber) {
        ChannelConfig c = findByNumber(channelNumber);
        if (c == null) return;
        channelNameField.setText(c.getChannelName());
        addressField.setText(String.valueOf(c.getChannelAddress()));
        dataTypeCombo.setSelectedItem(c.getDataType());
        deviceIdField.setText(String.valueOf(c.getDeviceId()));
        offsetField.setText(String.valueOf(c.getOffset()));
        lowField.setText(String.valueOf(c.getLow()));
        highField.setText(String.valueOf(c.getHigh()));
        maxDecimalsField.setText(String.valueOf(c.getMaxDecimalDigits()));
        colorCombo.setSelectedItem(colorName(c.getChannelColor()));
        unitField.setText(c.getUnit());
        mathsField.setText(c.getChannelMaths());
        updateCurrentValue(c);
    }

    private void updateCurrentValue(ChannelConfig c) {
        Map<Integer, Double> computed = runtime.getComputedValues();
        Double v = computed.get(c.getChannelNumber());
        if (valueField == null) valueField = new JTextField("0.00", 8);
        valueField.setText(v != null ? String.format("%.2f", v) : "-");
    }

    private String colorName(Color color) {
        if (color.equals(Color.RED)) return "Red";
        if (color.equals(Color.GREEN)) return "Green";
        if (color.equals(Color.BLUE)) return "Blue";
        if (color.equals(Color.BLACK)) return "Black";
        if (color.equals(Color.ORANGE)) return "Orange";
        return "Black";
    }

    private ChannelConfig findByNumber(int num) {
        for (ChannelConfig c : channelConfigs) if (c.getChannelNumber() == num) return c;
        return null;
    }

    private void onAdd() {
        int next = channelConfigs.stream().mapToInt(ChannelConfig::getChannelNumber).max().orElse(0) + 1;
        ChannelConfig c = new ChannelConfig(next, 0, "Float32", 1, 0, 0, 100, 0, 2, Color.RED, "x", "", "Channel" + next);
        channelConfigs.add(c);
        saveChannels();
        refreshChannelNumbers();
        channelNumberCombo.setSelectedItem(next);
        refreshOutputTable();
    }

    private void onSave() {
        try {
            Integer num = (Integer) channelNumberCombo.getSelectedItem();
            if (num == null) return;
            ChannelConfig c = findByNumber(num);
            if (c == null) return;
            String name = channelNameField.getText().trim();
            int address = Integer.parseInt(addressField.getText().trim());
            String dtype = (String) dataTypeCombo.getSelectedItem();
            int devId = Integer.parseInt(deviceIdField.getText().trim());
            double offset = Double.parseDouble(offsetField.getText().trim());
            double low = Double.parseDouble(lowField.getText().trim());
            double high = Double.parseDouble(highField.getText().trim());
            int maxD = Integer.parseInt(maxDecimalsField.getText().trim());
            String unit = unitField.getText().trim();
            String maths = mathsField.getText().trim();
            String colorSel = (String) colorCombo.getSelectedItem();
            Color color;
            if ("Green".equals(colorSel)) color = Color.GREEN;
            else if ("Blue".equals(colorSel)) color = Color.BLUE;
            else if ("Orange".equals(colorSel)) color = Color.ORANGE;
            else if ("Black".equals(colorSel)) color = Color.BLACK;
            else color = Color.RED;
            ChannelConfig updated = new ChannelConfig(num, address, dtype, devId, 0, low, high, offset, maxD, color, maths.isEmpty()?"x":maths, unit, name);
            for (int i = 0; i < channelConfigs.size(); i++) {
                if (channelConfigs.get(i).getChannelNumber() == num) { channelConfigs.set(i, updated); break; }
            }
            saveChannels();
            refreshForm(num);
            refreshOutputTable();
            JOptionPane.showMessageDialog(panel, "Channel saved.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(panel, "Please enter valid numeric values.");
        }
    }

    private void onDelete() {
        Integer num = (Integer) channelNumberCombo.getSelectedItem();
        if (num == null) return;
        channelConfigs.removeIf(c -> c.getChannelNumber() == num);
        saveChannels();
        refreshChannelNumbers();
        refreshForm(channelNumberCombo.getItemAt(0));
        refreshOutputTable();
    }

    private void refreshOutputTable() {
        List<ChannelConfig> cfgs = channelConfigs;
        if (cfgs == null) return;
        Map<Integer, Double> raw = runtime.getRawValues();
        Map<Integer, Double> out = runtime.getComputedValues();
        model.setRowCount(0);
        for (ChannelConfig c : cfgs) {
            Double r = raw.get(c.getChannelNumber());
            Double o = out.get(c.getChannelNumber());
            model.addRow(new Object[]{c.getChannelName(), c.getChannelAddress(), r, o, c.getUnit()});
        }
    }

    public JPanel getPanel() { return panel; }

    public static class ChannelDefinitionContext {
        private static ChannelRuntimeService runtime;
        public static void setRuntime(ChannelRuntimeService r) { runtime = r; }
        public static ChannelRuntimeService getRuntime() { return runtime; }
    }
}
