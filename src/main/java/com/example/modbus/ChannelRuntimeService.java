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

import javax.swing.SwingUtilities;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelRuntimeService {
    private final ModbusSettings settings;
    private final ModbusMaster master;
    private final boolean ownsMaster;
    private final Timer timer;
    private final Map<Integer, Double> rawValues = new ConcurrentHashMap<>();
    private final Map<Integer, Double> computedValues = new ConcurrentHashMap<>();
    private final ExpressionEvaluator evaluator = new ExpressionEvaluator();
    private final List<Runnable> listeners = new ArrayList<>();

    public ChannelRuntimeService(ModbusSettings settings, ModbusMaster sharedMaster) {
        this.settings = settings;
        this.master = sharedMaster;
        this.ownsMaster = false;
        this.timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() { @Override public void run() { pollOnce(); } }, 0, 1000);
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
    }

    private void pollOnce() {
        if (master == null) return;
        List<ChannelConfig> channels = ChannelRepository.load();
        if (channels.isEmpty()) channels = ChannelConfigPage.getChannelConfigs();
        if (channels == null || channels.isEmpty()) return;
        System.out.println("Polling " + channels.size() + " channels");
        try {
            for (ChannelConfig ch : channels) {
            try {
                if (master == null) break; // Check if master is still valid
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
                    rawValues.put(ch.getChannelNumber(), val);
                } else if (address >= 30001 && address < 40000) {
                    int zero = address - 30001; // FC4 Input
                    ReadInputRegistersRequest req = new ReadInputRegistersRequest(deviceId, zero, count);
                    ReadInputRegistersResponse resp = (ReadInputRegistersResponse) master.send(req);
                    if (resp.isException()) {
                        System.out.println("Exception reading channel " + ch.getChannelNumber() + " at " + address + ": " + resp.getExceptionMessage());
                        continue;
                    }
                    short[] data = resp.getShortData();
                    double val = extractValue(data, ch.getDataType());
                    val = val + ch.getOffset();
                    rawValues.put(ch.getChannelNumber(), val);
                    System.out.println("Channel " + ch.getChannelNumber() + " (" + ch.getChannelName() + ") = " + val);
                } else if (address >= 10001 && address < 20000) {
                    int zero = address - 10001; // FC2 Discrete Inputs (boolean)
                    ReadDiscreteInputsRequest req = new ReadDiscreteInputsRequest(deviceId, zero, 1);
                    ReadDiscreteInputsResponse resp = (ReadDiscreteInputsResponse) master.send(req);
                    if (resp.isException()) continue;
                    boolean[] data = resp.getBooleanData();
                    double val = (data != null && data.length > 0 && data[0]) ? 1.0 : 0.0;
                    rawValues.put(ch.getChannelNumber(), val);
                } else if (address >= 1 && address < 10000) {
                    int zero = address - 1; // FC1 Coils
                    ReadCoilsRequest req = new ReadCoilsRequest(deviceId, zero, 1);
                    ReadCoilsResponse resp = (ReadCoilsResponse) master.send(req);
                    if (resp.isException()) continue;
                    boolean[] data = resp.getBooleanData();
                    double val = (data != null && data.length > 0 && data[0]) ? 1.0 : 0.0;
                    rawValues.put(ch.getChannelNumber(), val);
                } else {
                    // Treat as raw zero-based holding register address
                    int zero = address;
                    ReadHoldingRegistersRequest req = new ReadHoldingRegistersRequest(deviceId, zero, count);
                    ReadHoldingRegistersResponse resp = (ReadHoldingRegistersResponse) master.send(req);
                    if (resp.isException()) continue;
                    short[] data = resp.getShortData();
                    double val = extractValue(data, ch.getDataType());
                    val = val + ch.getOffset();
                    rawValues.put(ch.getChannelNumber(), val);
                }
            } catch (ModbusTransportException ignored) {}
            }
        } catch (Exception e) {
            System.out.println("Error during channel polling: " + e.getMessage());
            e.printStackTrace();
        }
        computeAll(channels);
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
            double x = rawValues.getOrDefault(ch.getChannelNumber(), Double.NaN);
            vars.put("x", x);
            String expr = ch.getChannelMaths();
            double out = evaluator.evaluate(expr, vars);
            if (!Double.isNaN(out)) {
                out = Math.max(ch.getLow(), Math.min(ch.getHigh(), out));
                out = round(out, ch.getMaxDecimalDigits());
            }
            computedValues.put(ch.getChannelNumber(), out);
        }
    }

    private double round(double v, int digits) { double m = Math.pow(10, digits); return Math.round(v * m) / m; }

    public Map<Integer, Double> getRawValues() { return new HashMap<>(rawValues); }
    public Map<Integer, Double> getComputedValues() { return new HashMap<>(computedValues); }

    public void addListener(Runnable r) { listeners.add(r); }
    private void notifyListeners() { SwingUtilities.invokeLater(() -> { for (Runnable r : listeners) r.run(); }); }

    public void shutdown() { timer.cancel(); if (ownsMaster && master != null) { try { master.destroy(); } catch (Exception ignored) {} } }
}
