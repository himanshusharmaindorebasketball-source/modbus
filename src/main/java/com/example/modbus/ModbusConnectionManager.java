package com.example.modbus;

import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;

public class ModbusConnectionManager {
    private SimpleSerialPortWrapper wrapper;
    private ModbusMaster master;
    private boolean opened;

    public synchronized void open(ModbusSettings settings) throws Exception {
        // Don't close existing connection if it's the same port
        if (opened && wrapper != null && wrapper.getPortName().equals(settings.getPortName())) {
            return; // Already connected to the same port
        }
        
        close();
        
        try {
            wrapper = new SimpleSerialPortWrapper();
            wrapper.openPort(settings);
            ModbusFactory factory = new ModbusFactory();
            master = factory.createRtuMaster(wrapper);
            // Increase timeout and retries for better reliability
            master.setTimeout(3000);
            master.setRetries(2);
            master.init();
            opened = true;
            System.out.println("Modbus connection established successfully");
        } catch (Exception e) {
            System.err.println("Failed to establish Modbus connection: " + e.getMessage());
            close(); // Clean up on failure
            throw e;
        }
    }

    public synchronized ModbusMaster getMaster() {
        return master;
    }

    public synchronized boolean isOpen() { return opened && master != null; }

    public synchronized void close() {
        try {
            if (master != null) master.destroy();
        } catch (Exception ignored) {}
        try {
            if (wrapper != null) wrapper.closePort();
        } catch (Exception ignored) {}
        master = null;
        wrapper = null;
        opened = false;
    }
}

