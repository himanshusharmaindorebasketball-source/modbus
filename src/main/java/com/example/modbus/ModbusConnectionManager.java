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
        wrapper = new SimpleSerialPortWrapper();
        wrapper.openPort(settings);
        ModbusFactory factory = new ModbusFactory();
        master = factory.createRtuMaster(wrapper);
        master.setTimeout(2000);
        master.setRetries(3);
        master.init();
        opened = true;
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

