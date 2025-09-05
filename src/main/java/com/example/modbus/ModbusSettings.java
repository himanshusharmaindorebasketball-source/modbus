package com.example.modbus;

public class ModbusSettings {
    private String portName;
    private int baudRate;
    private int dataBits;
    private int stopBits;
    private int parity;
    private int deviceId;

    public ModbusSettings() {
        this.portName = "COM1";
        this.baudRate = 9600;
        this.dataBits = 8;
        this.stopBits = 1;
        this.parity = 0; // No parity by default
        this.deviceId = 1;
    }

    public ModbusSettings(String portName, int baudRate, int dataBits, int stopBits, int parity, int deviceId) {
        this.portName = portName;
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
        this.deviceId = deviceId;
    }

    public String getPortName() { return portName; }
    public int getBaudRate() { return baudRate; }
    public int getDataBits() { return dataBits; }
    public int getStopBits() { return stopBits; }
    public int getParity() { return parity; }
    public int getDeviceId() { return deviceId; }

    public void setPortName(String portName) { this.portName = portName; }
    public void setBaudRate(int baudRate) { this.baudRate = baudRate; }
    public void setDataBits(int dataBits) { this.dataBits = dataBits; }
    public void setStopBits(int stopBits) { this.stopBits = stopBits; }
    public void setParity(int parity) { this.parity = parity; }
    public void setDeviceId(int deviceId) { this.deviceId = deviceId; }
}