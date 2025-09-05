package com.example.modbus;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChannelDataArrangementPage {
    private final JPanel panel;
    private final ChannelRuntimeService runtime;
    private JPanel gridPanel;
    private JSpinner rowsSpinner;
    private JSpinner colsSpinner;
    private JComboBox<String> sortCombo;
    private JCheckBox showChannelsCheck;
    private JSpinner fontSizeSpinner;

    public ChannelDataArrangementPage(ChannelRuntimeService runtime) {
        this.runtime = runtime;
        panel = new JPanel(new BorderLayout());
        buildUI();
        runtime.addListener(this::refreshGrid);
        refreshGrid();
    }

    public void refresh() { refreshGrid(); }

    private void buildUI() {
        // Control panel at top
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        controlPanel.add(new JLabel("Layout:"));
        rowsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        controlPanel.add(new JLabel("Rows:"));
        controlPanel.add(rowsSpinner);
        colsSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 10, 1));
        controlPanel.add(new JLabel("Cols:"));
        controlPanel.add(colsSpinner);
        
        controlPanel.add(new JLabel("Sort by:"));
        sortCombo = new JComboBox<>(new String[]{"Channel Number", "Channel Name", "Value"});
        controlPanel.add(sortCombo);
        
        showChannelsCheck = new JCheckBox("Show All Channels", true);
        controlPanel.add(showChannelsCheck);
        
        controlPanel.add(new JLabel("Font Size:"));
        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(12, 8, 24, 1));
        controlPanel.add(fontSizeSpinner);
        
        JButton refreshButton = new JButton("Refresh Layout");
        refreshButton.addActionListener(e -> refreshGrid());
        controlPanel.add(refreshButton);
        
        // Grid panel for channel cards
        gridPanel = new JPanel(new GridBagLayout());
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Add change listeners
        rowsSpinner.addChangeListener(e -> refreshGrid());
        colsSpinner.addChangeListener(e -> refreshGrid());
        sortCombo.addActionListener(e -> refreshGrid());
        showChannelsCheck.addActionListener(e -> refreshGrid());
        fontSizeSpinner.addChangeListener(e -> refreshGrid());
        
        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(gridPanel), BorderLayout.CENTER);
    }

    private void refreshGrid() {
        gridPanel.removeAll();
        
        List<ChannelConfig> channels = ChannelRepository.load();
        if (channels.isEmpty()) channels = ChannelConfigPage.getChannelConfigs();
        if (channels == null || channels.isEmpty()) {
            gridPanel.add(new JLabel("No channels configured"), new GridBagConstraints());
            return;
        }
        
        // Sort channels based on selection
        sortChannels(channels);
        
        // Filter channels if needed
        if (!showChannelsCheck.isSelected()) {
            channels = channels.stream().filter(ch -> ch.getChannelNumber() <= 10).collect(Collectors.toList());
        }
        
        int rows = (Integer) rowsSpinner.getValue();
        int cols = (Integer) colsSpinner.getValue();
        int fontSize = (Integer) fontSizeSpinner.getValue();
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        
        int channelIndex = 0;
        for (int row = 0; row < rows && channelIndex < channels.size(); row++) {
            for (int col = 0; col < cols && channelIndex < channels.size(); col++) {
                ChannelConfig channel = channels.get(channelIndex);
                JPanel channelCard = createChannelCard(channel, fontSize);
                
                gbc.gridx = col;
                gbc.gridy = row;
                gridPanel.add(channelCard, gbc);
                channelIndex++;
            }
        }
        
        gridPanel.revalidate();
        gridPanel.repaint();
    }
    
    private void sortChannels(List<ChannelConfig> channels) {
        String sortBy = (String) sortCombo.getSelectedItem();
        if ("Channel Number".equals(sortBy)) {
            channels.sort((a, b) -> Integer.compare(a.getChannelNumber(), b.getChannelNumber()));
        } else if ("Channel Name".equals(sortBy)) {
            channels.sort((a, b) -> a.getChannelName().compareToIgnoreCase(b.getChannelName()));
        } else if ("Value".equals(sortBy)) {
            Map<Integer, Double> values = runtime.getComputedValues();
            channels.sort((a, b) -> {
                Double valA = values.get(a.getChannelNumber());
                Double valB = values.get(b.getChannelNumber());
                if (valA == null && valB == null) return 0;
                if (valA == null) return 1;
                if (valB == null) return -1;
                return Double.compare(valA, valB);
            });
        }
    }
    
    private JPanel createChannelCard(ChannelConfig channel, int fontSize) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(channel.getChannelColor(), 2),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        card.setBackground(Color.WHITE);
        
        JLabel nameLabel = new JLabel(channel.getChannelName(), SwingConstants.CENTER);
        nameLabel.setFont(new Font("Arial", Font.BOLD, fontSize));
        nameLabel.setForeground(channel.getChannelColor());
        card.add(nameLabel, BorderLayout.NORTH);
        
        Map<Integer, Double> values = runtime.getComputedValues();
        Double value = values.get(channel.getChannelNumber());
        String valueText = value != null ? String.format("%." + channel.getMaxDecimalDigits() + "f", value) : "N/A";
        
        JLabel valueLabel = new JLabel(valueText, SwingConstants.CENTER);
        valueLabel.setFont(new Font("Arial", Font.BOLD, fontSize + 2));
        card.add(valueLabel, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JLabel unitLabel = new JLabel(channel.getUnit(), SwingConstants.CENTER);
        unitLabel.setFont(new Font("Arial", Font.PLAIN, fontSize - 2));
        bottomPanel.add(unitLabel, BorderLayout.CENTER);
        card.add(bottomPanel, BorderLayout.SOUTH);
        
        return card;
    }

    public JPanel getPanel() { return panel; }
}
