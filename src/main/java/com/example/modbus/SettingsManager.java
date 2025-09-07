package com.example.modbus;

import com.fazecast.jSerialComm.SerialPort;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SettingsManager {
    private static final String SETTINGS_FILE = "modbus_settings.json";

    public void saveSettings(ModbusSettings settings) {
        JSONObject json = new JSONObject();
        json.put("portName", settings.getPortName());
        json.put("baudRate", settings.getBaudRate());
        json.put("dataBits", settings.getDataBits());
        json.put("stopBits", settings.getStopBits());
        // Convert numeric parity to string
        String parityStr;
        switch (settings.getParity()) {
            case SerialPort.NO_PARITY:
                parityStr = "None";
                break;
            case SerialPort.ODD_PARITY:
                parityStr = "Odd";
                break;
            case SerialPort.EVEN_PARITY:
                parityStr = "Even";
                break;
            case SerialPort.MARK_PARITY:
                parityStr = "Mark";
                break;
            case SerialPort.SPACE_PARITY:
                parityStr = "Space";
                break;
            default:
                parityStr = "None";
        }
        json.put("parity", parityStr);
        // Device ID is no longer stored in settings - it's configured per register

        try {
            Files.writeString(Paths.get(SETTINGS_FILE), json.toString());
        } catch (IOException e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }

    public ModbusSettings loadSettings() {
        ModbusSettings settings = new ModbusSettings(); // Default settings
        try {
            if (Files.exists(Paths.get(SETTINGS_FILE))) {
                String content = Files.readString(Paths.get(SETTINGS_FILE));
                JSONObject json = new JSONObject(content);
                settings.setPortName(json.getString("portName"));
                settings.setBaudRate(json.getInt("baudRate"));
                settings.setDataBits(json.getInt("dataBits"));
                settings.setStopBits(json.getInt("stopBits"));
                // Convert string parity back to numeric
                String parityStr = json.getString("parity");
                int parity;
                switch (parityStr) {
                    case "None":
                        parity = SerialPort.NO_PARITY;
                        break;
                    case "Odd":
                        parity = SerialPort.ODD_PARITY;
                        break;
                    case "Even":
                        parity = SerialPort.EVEN_PARITY;
                        break;
                    case "Mark":
                        parity = SerialPort.MARK_PARITY;
                        break;
                    case "Space":
                        parity = SerialPort.SPACE_PARITY;
                        break;
                    default:
                        parity = SerialPort.NO_PARITY;
                }
                settings.setParity(parity);
                // Device ID is no longer loaded from settings - it's configured per register
            }
        } catch (IOException e) {
            System.err.println("Error loading settings: " + e.getMessage());
        }
        return settings;
    }
}