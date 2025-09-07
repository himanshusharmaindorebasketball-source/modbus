package com.example.modbus;

import com.fazecast.jSerialComm.SerialPort;
import com.serotonin.modbus4j.serial.SerialPortWrapper;

import java.io.InputStream;
import java.io.OutputStream;

public class SimpleSerialPortWrapper implements SerialPortWrapper {
    private SerialPort serialPort;

    // Constructor with 5 arguments (for Main.java)
    public SimpleSerialPortWrapper(String portName, int baudRate, int dataBits, int stopBits, int parity) throws Exception {
        serialPort = SerialPort.getCommPort(portName);
        serialPort.setComPortParameters(baudRate, dataBits, stopBits, parity);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 1000);

        if (!serialPort.openPort()) {
            throw new Exception("Failed to open serial port: " + portName);
        }
        System.out.println("Serial port " + portName + " opened successfully.");
    }

    // Default constructor (for DataPage.java)
    public SimpleSerialPortWrapper() {
        // Empty constructor; port will be opened later via openPort
    }

    public void openPort(ModbusSettings settings) throws Exception {
        openPort(
                settings.getPortName(),
                settings.getBaudRate(),
                settings.getDataBits(),
                settings.getStopBits(),
                settings.getParity()
        );
    }

    public void openPort(String portName, int baudRate, int dataBits, int stopBits, int parity) throws Exception {
        // Close existing port if open
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
        
        serialPort = SerialPort.getCommPort(portName);
        serialPort.setComPortParameters(baudRate, dataBits, stopBits, parity);
        // Increase timeout to handle communication issues better
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 2000, 2000);
        
        // Clear any existing data in buffers
        if (serialPort.isOpen()) {
            serialPort.flushIOBuffers();
        }

        if (!serialPort.openPort()) {
            throw new Exception("Failed to open serial port: " + portName);
        }
        
        // Wait a moment for port to stabilize
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Serial port " + portName + " opened successfully with parameters: " + 
                          baudRate + " baud, " + dataBits + " data bits, " + stopBits + " stop bits, parity " + parity);
    }

    public void closePort() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            System.out.println("Serial port closed.");
        }
    }

    public SerialPort getSerialPort() {
        return serialPort;
    }

    public String getPortName() {
        return serialPort != null ? serialPort.getSystemPortName() : null;
    }

    // Implement SerialPortWrapper interface methods
    @Override
    public void close() throws Exception {
        closePort();
    }

    @Override
    public void open() throws Exception {
        // Port is already opened in constructor or openPort method
        if (serialPort == null || !serialPort.isOpen()) {
            throw new Exception("Serial port not initialized or already closed.");
        }
    }

    @Override
    public InputStream getInputStream() {
        return serialPort.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() {
        return serialPort.getOutputStream();
    }

    @Override
    public int getBaudRate() {
        return serialPort != null ? serialPort.getBaudRate() : 0;
    }

    @Override
    public int getDataBits() {
        return serialPort != null ? serialPort.getNumDataBits() : 0;
    }

    @Override
    public int getStopBits() {
        return serialPort != null ? serialPort.getNumStopBits() : 0;
    }

    @Override
    public int getParity() {
        return serialPort != null ? serialPort.getParity() : 0;
    }
}