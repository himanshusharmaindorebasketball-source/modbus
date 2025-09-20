package com.example.modbus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Manager for mathematical channel configurations
 */
public class MathChannelManager {
    private static final String CONFIG_FILE = "math_channels.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static List<MathChannelConfig> mathChannels = new ArrayList<>();
    
    /**
     * Load math channel configurations from file
     */
    public static void loadConfigs() {
        try {
            File file = new File(CONFIG_FILE);
            if (!file.exists()) {
                System.out.println("Math channels config file not found, using defaults");
                mathChannels = new ArrayList<>();
                return;
            }
            
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                Type listType = new TypeToken<List<MathChannelConfig>>(){}.getType();
                mathChannels = gson.fromJson(reader, listType);
                if (mathChannels == null) {
                    mathChannels = new ArrayList<>();
                }
                System.out.println("Math channels loaded from " + CONFIG_FILE + " (" + mathChannels.size() + " channels)");
            } catch (IOException e) {
                System.err.println("Error loading math channels: " + e.getMessage());
                mathChannels = new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("Unexpected error in loadConfigs: " + e.getMessage());
            e.printStackTrace();
            mathChannels = new ArrayList<>();
        }
    }
    
    /**
     * Clear math channel cache to force reload
     */
    public static void clearCache() {
        mathChannels = new ArrayList<>();
        System.out.println("Math channel cache cleared");
    }
    
    /**
     * Save math channel configurations to file
     */
    public static void saveConfigs() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(mathChannels, writer);
            System.out.println("Math channels saved to " + CONFIG_FILE);
        } catch (IOException e) {
            System.err.println("Error saving math channels: " + e.getMessage());
        }
    }
    
    /**
     * Get all math channel configurations
     */
    public static List<MathChannelConfig> getConfigs() {
        return new ArrayList<>(mathChannels);
    }
    
    /**
     * Add a new math channel configuration
     */
    public static void addConfig(MathChannelConfig config) {
        mathChannels.add(config);
        // Don't auto-save here - let the caller decide when to save
    }
    
    /**
     * Update an existing math channel configuration
     */
    public static void updateConfig(int index, MathChannelConfig config) {
        if (index >= 0 && index < mathChannels.size()) {
            mathChannels.set(index, config);
            // Don't auto-save here - let the caller decide when to save
        }
    }
    
    /**
     * Remove a math channel configuration
     */
    public static void removeConfig(int index) {
        if (index >= 0 && index < mathChannels.size()) {
            mathChannels.remove(index);
            // Don't auto-save here - let the caller decide when to save
        }
    }
    
    /**
     * Get math channel configuration by index
     */
    public static MathChannelConfig getConfig(int index) {
        if (index >= 0 && index < mathChannels.size()) {
            return mathChannels.get(index);
        }
        return null;
    }
    
    /**
     * Get math channel configuration by name
     */
    public static MathChannelConfig getConfigByName(String channelName) {
        for (MathChannelConfig config : mathChannels) {
            if (config.getChannelName().equals(channelName)) {
                return config;
            }
        }
        return null;
    }
    
    /**
     * Calculate values for all enabled math channels
     */
    public static Map<String, Double> calculateAllValues(Map<String, Double> channelValues) {
        Map<String, Double> mathValues = new HashMap<>();
        
        for (MathChannelConfig config : mathChannels) {
            if (config.isEnabled()) {
                try {
                    double result = MathExpressionEvaluator.evaluate(config.getExpression(), channelValues);
                    mathValues.put(config.getChannelName(), result);
                } catch (Exception e) {
                    System.err.println("Error calculating math channel '" + config.getChannelName() + "': " + e.getMessage());
                    mathValues.put(config.getChannelName(), Double.NaN);
                }
            }
        }
        
        return mathValues;
    }
    
    /**
     * Get all math channel names
     */
    public static List<String> getChannelNames() {
        List<String> names = new ArrayList<>();
        for (MathChannelConfig config : mathChannels) {
            names.add(config.getChannelName());
        }
        return names;
    }
    
    /**
     * Check if a channel name already exists
     */
    public static boolean channelNameExists(String channelName) {
        for (MathChannelConfig config : mathChannels) {
            if (config.getChannelName().equals(channelName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Clear all math channel configurations
     */
    public static void clearAllConfigs() {
        mathChannels.clear();
        // Don't auto-save here - let the caller decide when to save
    }
    
    /**
     * Get all math channels that use counter functions
     */
    public static List<MathChannelConfig> getCounterChannels() {
        List<MathChannelConfig> counterChannels = new ArrayList<>();
        for (MathChannelConfig config : mathChannels) {
            if (config.usesCounters()) {
                counterChannels.add(config);
            }
        }
        return counterChannels;
    }
    
    /**
     * Get all math channels that use timer functions
     */
    public static List<MathChannelConfig> getTimerChannels() {
        List<MathChannelConfig> timerChannels = new ArrayList<>();
        for (MathChannelConfig config : mathChannels) {
            if (config.usesTimers()) {
                timerChannels.add(config);
            }
        }
        return timerChannels;
    }
    
    /**
     * Get all math channels that use logical expressions
     */
    public static List<MathChannelConfig> getLogicalChannels() {
        List<MathChannelConfig> logicalChannels = new ArrayList<>();
        for (MathChannelConfig config : mathChannels) {
            if (config.usesLogicalExpressions()) {
                logicalChannels.add(config);
            }
        }
        return logicalChannels;
    }
    
    /**
     * Reset all counters and timers for all math channels
     */
    public static void resetAllStates() {
        MathChannelStateManager.getInstance().clearAllStates();
    }
    
    /**
     * Get statistics about math channel types
     */
    public static Map<String, Integer> getChannelTypeStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("Total", mathChannels.size());
        stats.put("Counter", 0);
        stats.put("Timer", 0);
        stats.put("Logical", 0);
        stats.put("Mathematical", 0);
        stats.put("Counter + Timer", 0);
        
        for (MathChannelConfig config : mathChannels) {
            String type = config.getFunctionType();
            stats.put(type, stats.getOrDefault(type, 0) + 1);
        }
        
        return stats;
    }
}
