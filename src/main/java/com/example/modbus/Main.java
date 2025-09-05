package com.example.modbus;

import com.fazecast.jSerialComm.SerialPort;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;
import com.serotonin.modbus4j.serial.SerialPortWrapper;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        ModbusSettings settings = new ModbusSettings();
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter COM port (e.g., COM7): ");
        settings.setPortName(scanner.nextLine());

        System.out.print("Enter baud rate (e.g., 19200): ");
        settings.setBaudRate(Integer.parseInt(scanner.nextLine()));

        System.out.print("Enter data bits (e.g., 8): ");
        settings.setDataBits(Integer.parseInt(scanner.nextLine()));

        System.out.print("Enter stop bits (e.g., 1): ");
        settings.setStopBits(Integer.parseInt(scanner.nextLine()));

        System.out.print("Enter parity (None, Even, Odd): ");
        String parityInput = scanner.nextLine();
        int parity;
        if ("Even".equalsIgnoreCase(parityInput)) {
            parity = SerialPort.EVEN_PARITY;
        } else if ("Odd".equalsIgnoreCase(parityInput)) {
            parity = SerialPort.ODD_PARITY;
        } else {
            parity = SerialPort.NO_PARITY;
        }
        settings.setParity(parity);

        System.out.print("Enter device ID (e.g., 1): ");
        settings.setDeviceId(Integer.parseInt(scanner.nextLine()));

        System.out.println("\nAvailable COM ports:");
        for (SerialPort port : SerialPort.getCommPorts()) {
            System.out.println(port.getSystemPortName());
        }

        ModbusFactory factory = new ModbusFactory();
        SerialPortWrapper wrapper = null;

        try {
            wrapper = new SimpleSerialPortWrapper(
                    settings.getPortName(),
                    settings.getBaudRate(),
                    settings.getDataBits(),
                    settings.getStopBits(),
                    settings.getParity()
            );

            ModbusMaster master = factory.createRtuMaster(wrapper);
            master.init();
            System.out.println("Modbus master connected");

            int slaveId = settings.getDeviceId();

            for (int registerAddress = 3000; registerAddress <= 3048; registerAddress += 2) {
                ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId, registerAddress, 2);
                ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.send(request);
                if (response.isException()) {
                    System.err.println("Exception: " + response.getExceptionMessage());
                    continue;
                }
                short[] data = response.getShortData();
                // Debug: Print raw short values and intBits
                System.out.printf("Register %d-%d: data[0]=%d, data[1]=%d%n",
                        registerAddress, registerAddress + 1, data[0], data[1]);
                int intBits = (data[0] << 16) | (data[1] & 0xFFFF);
                System.out.println("intBits: " + Integer.toHexString(intBits));
                float value = Float.intBitsToFloat(intBits);
                System.out.println("Float value: " + value);
                System.out.printf("Register %d-%d (Float32): %.2f%n", registerAddress, registerAddress + 1, value);
            }

            ReadHoldingRegistersRequest lastRequest = new ReadHoldingRegistersRequest(slaveId, 3050, 1);
            ReadHoldingRegistersResponse lastResponse = (ReadHoldingRegistersResponse) master.send(lastRequest);
            if (!lastResponse.isException()) {
                short[] lastData = lastResponse.getShortData();
                System.out.printf("Register 3050 (Int16): %d%n", lastData[0]);
            } else {
                System.err.println("Exception: " + lastResponse.getExceptionMessage());
            }

            master.destroy();
        } catch (ModbusInitException e) {
            System.err.println("Initialization error: " + e.getMessage());
            e.printStackTrace();
        } catch (ModbusTransportException e) {
            System.err.println("Transport error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (wrapper != null) {
                try {
                    wrapper.close();
                } catch (Exception e) {
                    System.err.println("Error closing wrapper: " + e.getMessage());
                }
            }
            scanner.close();
        }
    }
}