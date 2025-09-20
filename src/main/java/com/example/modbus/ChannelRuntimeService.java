package com.example.modbus;

import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.msg.ReadCoilsRequest;
import com.serotonin.modbus4j.msg.ReadCoilsResponse;
import com.serotonin.modbus4j.msg.ReadDiscreteInputsRequest;
import com.serotonin.modbus4j.msg.ReadDiscreteInputsResponse;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;
import com.serotonin.modbus4j.msg.ReadInputRegistersRequest;
import com.serotonin.modbus4j.msg.ReadInputRegistersResponse;
import com.serotonin.modbus4j.msg.WriteCoilRequest;
import com.serotonin.modbus4j.msg.WriteCoilResponse;
import com.serotonin.modbus4j.msg.WriteCoilsRequest;
import com.serotonin.modbus4j.msg.WriteCoilsResponse;
import com.serotonin.modbus4j.msg.WriteRegisterRequest;
import com.serotonin.modbus4j.msg.WriteRegisterResponse;
import com.serotonin.modbus4j.msg.WriteRegistersRequest;
import com.serotonin.modbus4j.msg.WriteRegistersResponse;

import javax.swing.SwingUtilities;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.example.modbus.EnergyDataLogger;
import com.example.modbus.ModbusConfigManager;
import com.example.modbus.MathChannelManager;

public class ChannelRuntimeService {
    private final ModbusSettings settings;
    private final ModbusMaster master;
    private final boolean ownsMaster;
    private final Timer timer;
    private final Map<Integer, Double> rawValues = new ConcurrentHashMap<>();
    private final Map<Integer, Double> computedValues = new ConcurrentHashMap<>();
    private final ExpressionEvaluator evaluator = new ExpressionEvaluator();
    private final List<Runnable> listeners = new ArrayList<>();
    private final EnergyDataLogger energyLogger = EnergyDataLogger.getInstance();

    public ChannelRuntimeService(ModbusSettings settings, ModbusMaster sharedMaster) {
        this.settings = settings;
        this.master = sharedMaster;
        this.ownsMaster = false;
        this.timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() { @Override public void run() { pollOnce(); } }, 0, 1000);
        
        // Start energy logging
        energyLogger.startLogging();
        
        // Load math channel configurations
        MathChannelManager.loadConfigs();
    }

    @Deprecated
    public ChannelRuntimeService(ModbusSettings settings) throws Exception {
        this.settings = settings;
        ModbusConnectionManager cm = new ModbusConnectionManager();
        cm.open(settings);
        this.master = cm.getMaster();
        this.ownsMaster = true;
        this.timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() { @Override public void run() { pollOnce(); } }, 0, 1000);
        
        // Start energy logging
        energyLogger.startLogging();
    }

    public void pollOnce() {
        if (master == null) return;
        List<ChannelConfig> channels = ChannelRepository.load();
        if (channels.isEmpty()) channels = ChannelConfigPage.getChannelConfigs();
        if (channels.isEmpty()) {
            // Try to load from modbus_config.json
            try {
                List<ModbusConfigManager.ModbusConfig> modbusConfigs = ModbusConfigManager.loadConfig();
                if (modbusConfigs != null && !modbusConfigs.isEmpty()) {
                    System.out.println("Loading " + modbusConfigs.size() + " channels from modbus_config.json");
                    // Convert ModbusConfig to ChannelConfig
                    channels = new ArrayList<>();
                    for (int i = 0; i < modbusConfigs.size(); i++) {
                        ModbusConfigManager.ModbusConfig config = modbusConfigs.get(i);
                        ChannelConfig channelConfig = new ChannelConfig(
                            i + 1, // channel number
                            config.getAddress(), // channel address
                            config.getDataType(), // data type
                            config.getSlaveId(), // device id
                            0.0, // value
                            0.0, // low
                            1000.0, // high
                            0.0, // offset
                            2, // max decimal digits
                            new java.awt.Color(0, 0, 255), // color
                            "", // channel maths
                            "", // unit
                            config.getChannelName() // channel name
                        );
                        channels.add(channelConfig);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error loading modbus config: " + e.getMessage());
            }
        }
        if (channels == null || channels.isEmpty()) {
            System.out.println("No channels to poll");
            return;
        }
        System.out.println("Polling " + channels.size() + " channels");
        try {
            for (ChannelConfig ch : channels) {
            try {
                if (master == null) break; // Check if master is still valid
                
                // Skip channels with invalid channel numbers
                int channelNumber = ch.getChannelNumber();
                if (channelNumber <= 0) {
                    System.out.println("Skipping channel with invalid channel number: " + channelNumber);
                    continue;
                }
                
                int deviceId = ch.getDeviceId();
                int address = ch.getChannelAddress();
                // Support 1xxxx/3xxxx/4xxxx style addressing
                int count = "Float32".equalsIgnoreCase(ch.getDataType()) ? 2 : 1;
                if (address >= 40001 && address < 50000) {
                    int zero = address - 40001; // FC3 Holding
                    ReadHoldingRegistersRequest req = new ReadHoldingRegistersRequest(deviceId, zero, count);
                    ReadHoldingRegistersResponse resp = (ReadHoldingRegistersResponse) master.send(req);
                    if (resp.isException()) continue;
                    short[] data = resp.getShortData();
                    double val = extractValue(data, ch.getDataType());
                    val = val + ch.getOffset();
                    rawValues.put(channelNumber, val);
                } else if (address >= 30001 && address < 40000) {
                    int zero = address - 30001; // FC4 Input
                    ReadInputRegistersRequest req = new ReadInputRegistersRequest(deviceId, zero, count);
                    ReadInputRegistersResponse resp = (ReadInputRegistersResponse) master.send(req);
                    if (resp.isException()) {
                        System.out.println("Exception reading channel " + channelNumber + " at " + address + ": " + resp.getExceptionMessage());
                        continue;
                    }
                    short[] data = resp.getShortData();
                    double val = extractValue(data, ch.getDataType());
                    val = val + ch.getOffset();
                    rawValues.put(channelNumber, val);
                    System.out.println("Channel " + channelNumber + " (" + ch.getChannelName() + ") = " + val);
                } else if (address >= 10001 && address < 20000) {
                    int zero = address - 10001; // FC2 Discrete Inputs (boolean)
                    ReadDiscreteInputsRequest req = new ReadDiscreteInputsRequest(deviceId, zero, 1);
                    ReadDiscreteInputsResponse resp = (ReadDiscreteInputsResponse) master.send(req);
                    if (resp.isException()) continue;
                    boolean[] data = resp.getBooleanData();
                    double val = (data != null && data.length > 0 && data[0]) ? 1.0 : 0.0;
                    rawValues.put(channelNumber, val);
                } else if (address >= 1 && address < 10000) {
                    int zero = address - 1; // FC1 Coils
                    ReadCoilsRequest req = new ReadCoilsRequest(deviceId, zero, 1);
                    ReadCoilsResponse resp = (ReadCoilsResponse) master.send(req);
                    if (resp.isException()) continue;
                    boolean[] data = resp.getBooleanData();
                    double val = (data != null && data.length > 0 && data[0]) ? 1.0 : 0.0;
                    rawValues.put(channelNumber, val);
                } else {
                    // Treat as raw zero-based holding register address
                    int zero = address;
                    ReadHoldingRegistersRequest req = new ReadHoldingRegistersRequest(deviceId, zero, count);
                    ReadHoldingRegistersResponse resp = (ReadHoldingRegistersResponse) master.send(req);
                    if (resp.isException()) continue;
                    short[] data = resp.getShortData();
                    double val = extractValue(data, ch.getDataType());
                    val = val + ch.getOffset();
                    rawValues.put(channelNumber, val);
                }
            } catch (ModbusTransportException ignored) {}
            }
        } catch (Exception e) {
            System.out.println("Error during channel polling: " + e.getMessage());
            e.printStackTrace();
        }
        computeAll(channels);
        computeMathChannels();
        // DO NOT call logEnergyData - FilterDataPage handles energy logging with calculated values only
        logToCSV(channels);
        notifyListeners();
    }

    private double extractValue(short[] data, String dataType) {
        if (data == null || data.length == 0) return Double.NaN;
        if (dataType == null) dataType = "Int16";
        if ("Float32".equalsIgnoreCase(dataType)) {
            if (data.length < 2) return Double.NaN;
            int hi = data[0] & 0xFFFF;
            int lo = data[1] & 0xFFFF;
            int bits = (hi << 16) | lo;
            return Float.intBitsToFloat(bits);
        }
        // Int16 or default
        return data[0];
    }

    private void computeAll(List<ChannelConfig> channels) {
        Map<String, Double> vars = new HashMap<>();
        for (Map.Entry<Integer, Double> e : rawValues.entrySet()) vars.put("CH" + e.getKey(), e.getValue());
        for (ChannelConfig ch : channels) {
            int channelNumber = ch.getChannelNumber();
            if (channelNumber <= 0) continue; // Skip invalid channel numbers
            
            double x = rawValues.getOrDefault(channelNumber, Double.NaN);
            vars.put("x", x);
            String expr = ch.getChannelMaths();
            double out = evaluator.evaluate(expr, vars);
            if (!Double.isNaN(out)) {
                out = Math.max(ch.getLow(), Math.min(ch.getHigh(), out));
                out = round(out, ch.getMaxDecimalDigits());
            }
            computedValues.put(channelNumber, out);
        }
    }

    private double round(double v, int digits) { double m = Math.pow(10, digits); return Math.round(v * m) / m; }

    public Map<Integer, Double> getRawValues() { return new HashMap<>(rawValues); }
    public Map<Integer, Double> getComputedValues() { return new HashMap<>(computedValues); }
    
    public String getChannelName(int channelNumber) {
        List<ChannelConfig> channels = ChannelRepository.load();
        if (channels.isEmpty()) channels = ChannelConfigPage.getChannelConfigs();
        if (channels == null || channels.isEmpty()) return null;
        
        for (ChannelConfig ch : channels) {
            if (ch.getChannelNumber() == channelNumber) {
                return ch.getChannelName();
            }
        }
        return null;
    }

    public void addListener(Runnable r) { listeners.add(r); }
    private void notifyListeners() { SwingUtilities.invokeLater(() -> { for (Runnable r : listeners) r.run(); }); }
    
    /**
     * Log energy data including math channels to EnergyDataLogger
     */
    private void logEnergyData(List<ChannelConfig> channels) {
        if (!energyLogger.isLogging()) {
            return;
        }

        Map<String, Object> energyData = new HashMap<>();

        // DO NOT add raw channel values - only calculated/formatted values should be logged
        // Raw data should never be exposed to Data Logger

        // Add computed math channel values
        for (ChannelConfig ch : channels) {
            int channelNumber = ch.getChannelNumber();
            if (channelNumber <= 0) continue;

            String channelName = ch.getChannelName();
            if (channelName != null && !channelName.trim().isEmpty()) {
                Double computedValue = computedValues.get(channelNumber);
                if (computedValue != null && !Double.isNaN(computedValue)) {
                    // Add computed value with a suffix to distinguish from raw
                    energyData.put(channelName + "_Computed", computedValue);
                }
            }
        }

        // DO NOT update energy logger from ChannelRuntimeService
        // Only FilterDataPage should send calculated/formatted data to energy logger
        // This prevents raw data from being logged
    }
    
    /**
     * Get a DataLogger instance for CSV logging
     */
    private com.example.production.DataLogger getDataLogger() {
        return new com.example.production.DataLogger("production_data.csv");
    }
    
    /**
     * Compute math channels from math_channels.json
     */
    private void computeMathChannels() {
        try {
            List<MathChannelConfig> mathChannels = MathChannelManager.getConfigs();
            if (mathChannels == null || mathChannels.isEmpty()) {
                System.out.println("No math channels found");
                return;
            }
            
            System.out.println("Computing " + mathChannels.size() + " math channels");
            
            for (MathChannelConfig mathChannel : mathChannels) {
                if (!mathChannel.isEnabled()) continue;
                
                try {
                    // Create a context with raw values for the expression evaluator
                    Map<String, Double> context = new HashMap<>();
                    for (Map.Entry<Integer, Double> entry : rawValues.entrySet()) {
                        context.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                    
                    // Evaluate the math expression
                    double result = evaluator.evaluate(mathChannel.getExpression(), context);
                    
                    // Store the computed value with a unique channel number
                    int mathChannelNumber = 1000 + mathChannels.indexOf(mathChannel);
                    computedValues.put(mathChannelNumber, result);
                    
                    System.out.println("Math channel " + mathChannel.getChannelName() + " = " + result);
                    
                } catch (Exception e) {
                    System.err.println("Error computing math channel " + mathChannel.getChannelName() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading math channels: " + e.getMessage());
        }
    }
    
    /**
     * Log data to CSV file
     */
    private void logToCSV(List<ChannelConfig> channels) {
        try {
            com.example.production.DataLogger csvLogger = getDataLogger();
            
            // Log raw channel values
            for (ChannelConfig ch : channels) {
                int channelNumber = ch.getChannelNumber();
                if (channelNumber <= 0) continue;

                String channelName = ch.getChannelName();
                if (channelName != null && !channelName.trim().isEmpty()) {
                    Double rawValue = rawValues.get(channelNumber);
                    if (rawValue != null && !Double.isNaN(rawValue)) {
                        csvLogger.appendSample(channelName, channelNumber, rawValue);
                    }
                }
            }
            
            // Log math channel values
            try {
                List<MathChannelConfig> mathChannels = MathChannelManager.getConfigs();
                if (mathChannels != null && !mathChannels.isEmpty()) {
                    for (int i = 0; i < mathChannels.size(); i++) {
                        MathChannelConfig mathChannel = mathChannels.get(i);
                        if (!mathChannel.isEnabled()) continue;
                        
                        int mathChannelNumber = 1000 + i;
                        Double computedValue = computedValues.get(mathChannelNumber);
                        if (computedValue != null && !Double.isNaN(computedValue)) {
                            csvLogger.appendSample(mathChannel.getChannelName() + " (Math)", mathChannelNumber, computedValue);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error logging math channels to CSV: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error logging to CSV: " + e.getMessage());
        }
    }

    /**
     * Write a value to a Modbus register
     */
    public boolean writeValue(int deviceId, int address, Object value, String dataType) {
        if (master == null) {
            System.err.println("Modbus master is not available for writing");
            return false;
        }
        
        try {
            if (address >= 40001 && address < 50000) {
                // Holding Registers (4xxxx) - FC06/FC16
                return writeHoldingRegister(deviceId, address, value, dataType);
            } else if (address >= 1 && address < 10000) {
                // Coils (0xxxx) - FC05/FC15
                return writeCoil(deviceId, address, value);
            } else {
                System.err.println("Cannot write to read-only register address: " + address);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error writing to register " + address + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Write to Holding Register (4xxxx)
     */
    private boolean writeHoldingRegister(int deviceId, int address, Object value, String dataType) throws ModbusTransportException {
        int zeroBasedAddress = address - 40001;
        
        System.out.println("DEBUG: Attempting to write to register " + address + " (zero-based: " + zeroBasedAddress + ")");
        System.out.println("DEBUG: Device ID: " + deviceId + ", Data Type: " + dataType + ", Value: " + value + " (" + value.getClass().getSimpleName() + ")");
        
        if ("Float32".equalsIgnoreCase(dataType)) {
            // Write Float32 (2 registers)
            if (value instanceof Number) {
                float floatValue = ((Number) value).floatValue();
                int intBits = Float.floatToIntBits(floatValue);
                short high = (short) (intBits >> 16);
                short low = (short) (intBits & 0xFFFF);
                
                System.out.println("DEBUG: Float32 conversion - Value: " + floatValue + ", High: " + high + ", Low: " + low);
                
                WriteRegistersRequest req = new WriteRegistersRequest(deviceId, zeroBasedAddress, new short[]{high, low});
                WriteRegistersResponse resp = (WriteRegistersResponse) master.send(req);
                if (resp.isException()) {
                    System.err.println("Exception writing Float32 to register " + address + ": " + resp.getExceptionMessage());
                    return false;
                }
                System.out.println("Successfully wrote Float32 value " + floatValue + " to register " + address);
                return true;
            }
        } else {
            // Write single register (Int16, UInt16)
            if (value instanceof Number) {
                short shortValue = ((Number) value).shortValue();
                System.out.println("DEBUG: Int16 conversion - Original value: " + value + ", Short value: " + shortValue);
                
                WriteRegisterRequest req = new WriteRegisterRequest(deviceId, zeroBasedAddress, shortValue);
                WriteRegisterResponse resp = (WriteRegisterResponse) master.send(req);
                if (resp.isException()) {
                    System.err.println("Exception writing to register " + address + ": " + resp.getExceptionMessage());
                    System.err.println("DEBUG: Modbus exception details - Code: " + resp.getExceptionCode() + ", Message: " + resp.getExceptionMessage());
                    return false;
                }
                System.out.println("Successfully wrote value " + shortValue + " to register " + address);
                return true;
            }
        }
        
        System.err.println("Invalid value type for register write: " + value.getClass().getSimpleName());
        return false;
    }
    
    /**
     * Write to Coil (0xxxx)
     */
    private boolean writeCoil(int deviceId, int address, Object value) throws ModbusTransportException {
        int zeroBasedAddress = address - 1;
        boolean coilValue;
        
        if (value instanceof Boolean) {
            coilValue = (Boolean) value;
        } else if (value instanceof Number) {
            coilValue = ((Number) value).doubleValue() != 0.0;
        } else if (value instanceof String) {
            String strValue = ((String) value).toLowerCase();
            coilValue = "true".equals(strValue) || "1".equals(strValue) || "on".equals(strValue);
        } else {
            System.err.println("Invalid value type for coil write: " + value.getClass().getSimpleName());
            return false;
        }
        
        WriteCoilRequest req = new WriteCoilRequest(deviceId, zeroBasedAddress, coilValue);
        WriteCoilResponse resp = (WriteCoilResponse) master.send(req);
        if (resp.isException()) {
            System.err.println("Exception writing to coil " + address + ": " + resp.getExceptionMessage());
            return false;
        }
        System.out.println("Successfully wrote coil value " + coilValue + " to coil " + address);
        return true;
    }

    public void shutdown() { timer.cancel(); if (ownsMaster && master != null) { try { master.destroy(); } catch (Exception ignored) {} } }
}
