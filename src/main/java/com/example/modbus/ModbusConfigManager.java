package com.example.modbus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ModbusConfigManager {
    private static final String CONFIG_FILE = "modbus_config.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public static class ModbusConfig {
        private int slaveId;
        private int address;
        private int length;
        
        public ModbusConfig() {}
        
        public ModbusConfig(int slaveId, int address, int length) {
            this.slaveId = slaveId;
            this.address = address;
            this.length = length;
        }
        
        // Getters and setters
        public int getSlaveId() { return slaveId; }
        public void setSlaveId(int slaveId) { this.slaveId = slaveId; }
        
        public int getAddress() { return address; }
        public void setAddress(int address) { this.address = address; }
        
        public int getLength() { return length; }
        public void setLength(int length) { this.length = length; }
        
        @Override
        public String toString() {
            return String.format("Slave: %d, Address: %d, Length: %d", slaveId, address, length);
        }
    }
    
    public static void saveConfig(List<ModbusConfig> configs) {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(configs, writer);
            System.out.println("Modbus configuration saved to " + CONFIG_FILE);
        } catch (IOException e) {
            System.err.println("Error saving modbus configuration: " + e.getMessage());
        }
    }
    
    public static List<ModbusConfig> loadConfig() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            System.out.println("Config file not found, creating default configuration");
            return getDefaultConfig();
        }
        
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            Type listType = new TypeToken<List<ModbusConfig>>(){}.getType();
            List<ModbusConfig> configs = gson.fromJson(reader, listType);
            System.out.println("Modbus configuration loaded from " + CONFIG_FILE);
            return configs;
        } catch (IOException e) {
            System.err.println("Error loading modbus configuration: " + e.getMessage());
            return getDefaultConfig();
        }
    }
    
    public static List<ModbusConfig> getDefaultConfig() {
        List<ModbusConfig> defaultConfigs = new ArrayList<>();
        defaultConfigs.add(new ModbusConfig(5, 30001, 2));
        defaultConfigs.add(new ModbusConfig(5, 30003, 2));
        defaultConfigs.add(new ModbusConfig(5, 30005, 2));
        defaultConfigs.add(new ModbusConfig(5, 30007, 2));
        defaultConfigs.add(new ModbusConfig(5, 30009, 2));
        return defaultConfigs;
    }
    
    public static int[][] convertToArray(List<ModbusConfig> configs) {
        int[][] array = new int[configs.size()][3];
        for (int i = 0; i < configs.size(); i++) {
            ModbusConfig config = configs.get(i);
            array[i][0] = config.getSlaveId();
            array[i][1] = config.getAddress();
            array[i][2] = config.getLength();
        }
        return array;
    }
    
    public static List<ModbusConfig> convertFromArray(int[][] array) {
        List<ModbusConfig> configs = new ArrayList<>();
        for (int[] row : array) {
            configs.add(new ModbusConfig(row[0], row[1], row[2]));
        }
        return configs;
    }
}
