package com.example.modbus;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;

public class ChannelDataArrangementPage {
    private final JPanel panel;
    private final ChannelRuntimeService runtime;
    private JPanel gridPanel;
    private JSpinner rowsSpinner;
    private JSpinner colsSpinner;
    private JSpinner fontSizeSpinner;
    private JPanel channelSelectionPanel;
    private Map<String, JCheckBox> channelCheckboxes;
    private List<ModbusConfigManager.ModbusConfig> modbusConfigs;
    private JButton channelDropdownButton;
    private JPopupMenu channelDropdownMenu;
    private ChannelDisplayConfig displayConfig;
    private JButton customizeButton;

    public ChannelDataArrangementPage(ChannelRuntimeService runtime) {
        this.runtime = runtime;
        this.channelCheckboxes = new HashMap<>();
        this.modbusConfigs = new ArrayList<>();
        this.displayConfig = new ChannelDisplayConfig(); // Initialize with default settings
        
        // Load channel display configurations
        ChannelDisplayConfigManager.loadConfigs();
        
        panel = new JPanel(new BorderLayout());
        loadModbusConfig();
        buildUI();
        
        // Only add runtime listener if runtime service is available
        if (runtime != null) {
            runtime.addListener(this::refreshGrid);
        }
        
        // Add listener to ModbusDataStore for real-time updates
        ModbusDataStore.getInstance().addListener((channelName, value) -> {
            SwingUtilities.invokeLater(() -> refreshGrid());
        });
        
        refreshGrid();
    }

    public void refresh() { 
        loadModbusConfig();
        refreshChannelSelection();
        refreshGrid(); 
    }
    
    private void loadModbusConfig() {
        modbusConfigs = ModbusConfigManager.loadConfig();
        // Math channels are loaded separately and will be included in the channel selection
    }
    
    private void refreshChannelSelection() {
        channelSelectionPanel.removeAll();
        channelCheckboxes.clear();
        
        // Create dropdown button
        channelDropdownButton = new JButton("Select Channels ▼");
        channelDropdownButton.addActionListener(e -> showChannelDropdown());
        
        // Create popup menu with checkboxes
        channelDropdownMenu = new JPopupMenu();
        
        // Add regular Modbus channels
        for (ModbusConfigManager.ModbusConfig config : modbusConfigs) {
            // Handle null or empty channel names
            String channelName = config.getChannelName();
            if (channelName == null || channelName.trim().isEmpty()) {
                channelName = "Channel_" + config.getAddress(); // Generate default name
            }
            
            JCheckBox checkbox = new JCheckBox(channelName, true); // Default to selected
            checkbox.addActionListener(e -> {
                updateDropdownButtonText();
                refreshGrid();
            });
            channelCheckboxes.put(channelName, checkbox);
            channelDropdownMenu.add(checkbox);
        }
        
        // Add math channels
        List<MathChannelConfig> mathChannels = MathChannelManager.getConfigs();
        for (MathChannelConfig config : mathChannels) {
            if (config.isEnabled()) {
                String channelName = config.getChannelName();
                JCheckBox checkbox = new JCheckBox(channelName + " (Math)", true); // Default to selected
                checkbox.addActionListener(e -> {
                    updateDropdownButtonText();
                    refreshGrid();
                });
                channelCheckboxes.put(channelName, checkbox);
                channelDropdownMenu.add(checkbox);
            }
        }
        
        // Add select all / deselect all options
        channelDropdownMenu.addSeparator();
        JMenuItem selectAllItem = new JMenuItem("Select All");
        selectAllItem.addActionListener(e -> {
            channelCheckboxes.values().forEach(cb -> cb.setSelected(true));
            updateDropdownButtonText();
            refreshGrid();
        });
        channelDropdownMenu.add(selectAllItem);
        
        JMenuItem deselectAllItem = new JMenuItem("Deselect All");
        deselectAllItem.addActionListener(e -> {
            channelCheckboxes.values().forEach(cb -> cb.setSelected(false));
            updateDropdownButtonText();
            refreshGrid();
        });
        channelDropdownMenu.add(deselectAllItem);
        
        channelSelectionPanel.add(new JLabel("Channels:"));
        channelSelectionPanel.add(channelDropdownButton);
        
        updateDropdownButtonText();
        
        channelSelectionPanel.revalidate();
        channelSelectionPanel.repaint();
    }
    
    private void showChannelDropdown() {
        channelDropdownMenu.show(channelDropdownButton, 0, channelDropdownButton.getHeight());
    }
    
    private void updateDropdownButtonText() {
        long selectedCount = channelCheckboxes.values().stream().mapToLong(cb -> cb.isSelected() ? 1 : 0).sum();
        int totalCount = channelCheckboxes.size();
        
        if (selectedCount == 0) {
            channelDropdownButton.setText("No Channels Selected ▼");
        } else if (selectedCount == totalCount) {
            channelDropdownButton.setText("All Channels Selected ▼");
        } else {
            channelDropdownButton.setText(selectedCount + " of " + totalCount + " Selected ▼");
        }
    }

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
        
        // Channel selection panel
        channelSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        channelSelectionPanel.add(new JLabel("Select Channels:"));
        refreshChannelSelection();
        
        controlPanel.add(new JLabel("Font Size:"));
        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(12, 8, 24, 1));
        controlPanel.add(fontSizeSpinner);
        
        JButton refreshButton = new JButton("Refresh Layout");
        refreshButton.addActionListener(e -> refreshGrid());
        controlPanel.add(refreshButton);
        
        customizeButton = new JButton("Customize Display");
        customizeButton.addActionListener(e -> openCustomizationDialog());
        controlPanel.add(customizeButton);
        
        // Grid panel for channel cards
        gridPanel = new JPanel(new GridBagLayout());
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Add change listeners
        rowsSpinner.addChangeListener(e -> refreshGrid());
        colsSpinner.addChangeListener(e -> refreshGrid());
        fontSizeSpinner.addChangeListener(e -> refreshGrid());
        
        // Create main panel with control and channel selection
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(controlPanel, BorderLayout.NORTH);
        topPanel.add(channelSelectionPanel, BorderLayout.SOUTH);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(gridPanel), BorderLayout.CENTER);
    }

    private void refreshGrid() {
        gridPanel.removeAll();
        
        if (modbusConfigs == null || modbusConfigs.isEmpty()) {
            gridPanel.add(new JLabel("No channels configured. Use 'Configure Registers' to add channels."), new GridBagConstraints());
            gridPanel.revalidate();
            gridPanel.repaint();
            return;
        }
        
        // Filter regular Modbus channels based on checkbox selection
        List<ModbusConfigManager.ModbusConfig> selectedModbusChannels = modbusConfigs.stream()
            .filter(config -> {
                JCheckBox checkbox = channelCheckboxes.get(config.getChannelName());
                return checkbox != null && checkbox.isSelected();
            })
            .collect(Collectors.toList());
        
        // Filter math channels based on checkbox selection
        List<MathChannelConfig> selectedMathChannels = MathChannelManager.getConfigs().stream()
            .filter(config -> {
                JCheckBox checkbox = channelCheckboxes.get(config.getChannelName());
                return checkbox != null && checkbox.isSelected() && config.isEnabled();
            })
            .collect(Collectors.toList());
        
        // Combine all selected channels
        List<Object> selectedChannels = new ArrayList<>();
        selectedChannels.addAll(selectedModbusChannels);
        selectedChannels.addAll(selectedMathChannels);
        
        if (selectedChannels.isEmpty()) {
            gridPanel.add(new JLabel("No channels selected for display."), new GridBagConstraints());
            gridPanel.revalidate();
            gridPanel.repaint();
            return;
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
        for (int row = 0; row < rows && channelIndex < selectedChannels.size(); row++) {
            for (int col = 0; col < cols && channelIndex < selectedChannels.size(); col++) {
                Object config = selectedChannels.get(channelIndex);
                JPanel channelCard;
                
                if (config instanceof ModbusConfigManager.ModbusConfig) {
                    channelCard = createModbusChannelCard((ModbusConfigManager.ModbusConfig) config, fontSize);
                } else if (config instanceof MathChannelConfig) {
                    channelCard = createMathChannelCard((MathChannelConfig) config, fontSize);
                } else {
                    continue; // Skip unknown types
                }
                
                gbc.gridx = col;
                gbc.gridy = row;
                gridPanel.add(channelCard, gbc);
                channelIndex++;
            }
        }
        
        gridPanel.revalidate();
        gridPanel.repaint();
    }
    
    
    private JPanel createModbusChannelCard(ModbusConfigManager.ModbusConfig config, int fontSize) {
        // Get channel name
        String channelName = config.getChannelName();
        if (channelName == null || channelName.trim().isEmpty()) {
            channelName = "Channel_" + config.getAddress(); // Generate default name
        }
        
        // Get individual channel display configuration
        ChannelDisplayConfig channelDisplayConfig = ChannelDisplayConfigManager.getConfig(channelName);
        
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLUE, 2),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        card.setBackground(channelDisplayConfig.getBackgroundColor());
        card.setOpaque(true); // Ensure the background color is visible
        
        JLabel nameLabel = new JLabel(channelName, SwingConstants.CENTER);
        nameLabel.setFont(channelDisplayConfig.getFont());
        nameLabel.setForeground(channelDisplayConfig.getFontColor());
        card.add(nameLabel, BorderLayout.NORTH);
        
        // Get current value from FilterDataPage
        String valueText = getCurrentValue(config);
        
        JLabel valueLabel = new JLabel(valueText, SwingConstants.CENTER);
        valueLabel.setFont(channelDisplayConfig.getValueFont());
        valueLabel.setForeground(channelDisplayConfig.getValueColor());
        card.add(valueLabel, BorderLayout.CENTER);
        
        
        return card;
    }
    
    private JPanel createMathChannelCard(MathChannelConfig config, int fontSize) {
        // Get channel name
        String channelName = config.getChannelName();
        
        // Get the current value from ModbusDataStore
        Object value = ModbusDataStore.getInstance().getValue(channelName);
        String valueText = "N/A";
        if (value != null) {
            if (value instanceof Number) {
                valueText = MathExpressionEvaluator.formatValue(((Number) value).doubleValue(), config.getDecimalPlaces());
            } else {
                valueText = value.toString();
            }
        }
        
        // Create the card panel
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setOpaque(true);
        
        // Get display configuration for this channel
        ChannelDisplayConfig displayConfig = ChannelDisplayConfigManager.getConfig(channelName);
        
        // Set background color
        card.setBackground(displayConfig.getBackgroundColor());
        
        // Channel name label
        JLabel nameLabel = new JLabel(channelName, SwingConstants.CENTER);
        nameLabel.setFont(displayConfig.getFont());
        nameLabel.setForeground(displayConfig.getFontColor());
        card.add(nameLabel, BorderLayout.NORTH);
        
        // Value label
        JLabel valueLabel = new JLabel(valueText, SwingConstants.CENTER);
        valueLabel.setFont(displayConfig.getValueFont());
        valueLabel.setForeground(displayConfig.getValueColor());
        card.add(valueLabel, BorderLayout.CENTER);
        
        // Unit label (if specified)
        if (config.getUnit() != null && !config.getUnit().trim().isEmpty()) {
            JLabel unitLabel = new JLabel(config.getUnit(), SwingConstants.CENTER);
            unitLabel.setFont(new Font(displayConfig.getFontName(), Font.PLAIN, fontSize));
            unitLabel.setForeground(displayConfig.getFontColor());
            card.add(unitLabel, BorderLayout.SOUTH);
        }
        
        return card;
    }
    
    private String getCurrentValue(ModbusConfigManager.ModbusConfig config) {
        // Handle null or empty channel names
        String channelName = config.getChannelName();
        if (channelName == null || channelName.trim().isEmpty()) {
            channelName = "Channel_" + config.getAddress(); // Generate default name
        }
        
        Object value = ModbusDataStore.getInstance().getValue(channelName);
        if (value == null) {
            return "N/A";
        }
        
        // Format the value based on its type
        if (value instanceof Float) {
            return String.format("%.2f", (Float) value);
        } else if (value instanceof Integer) {
            return String.valueOf((Integer) value);
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? "ON" : "OFF";
        } else {
            return value.toString();
        }
    }

    private void openCustomizationDialog() {
        try {
            // Get available channel names
            String[] channelNames;
            if (modbusConfigs.isEmpty()) {
                // If no channels are configured, show a message
                JOptionPane.showMessageDialog(panel, 
                    "No channels are configured yet. Please configure some channels in the FilterData tab first.", 
                    "No Channels Available", 
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            channelNames = modbusConfigs.stream()
                .map(config -> {
                    String name = config.getChannelName();
                    return (name == null || name.trim().isEmpty()) ? 
                        "Channel_" + config.getAddress() : name;
                })
                .toArray(String[]::new);
            
            if (channelNames.length == 0) {
                JOptionPane.showMessageDialog(panel, 
                    "No valid channel names found.", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(panel);
            ChannelDisplayCustomizationDialog dialog = new ChannelDisplayCustomizationDialog(
                parentFrame, channelNames);
            
            dialog.pack();
            dialog.setVisible(true);
            
            // Refresh grid to apply any changes
            refreshGrid();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(panel, 
                "Error opening customization dialog: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }


    public JPanel getPanel() { return panel; }
}
