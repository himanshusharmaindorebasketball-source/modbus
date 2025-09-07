package com.example.modbus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.function.BiConsumer;

/**
 * Singleton data store to share Modbus data between different components
 */
public class ModbusDataStore {
    private static ModbusDataStore instance;
    private final Map<String, Object> dataMap;
    private final List<BiConsumer<String, Object>> listeners;
    
    private ModbusDataStore() {
        this.dataMap = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
    }
    
    public static synchronized ModbusDataStore getInstance() {
        if (instance == null) {
            instance = new ModbusDataStore();
        }
        return instance;
    }
    
    /**
     * Update a value in the data store and notify all listeners
     */
    public void updateValue(String channelName, Object value) {
        dataMap.put(channelName, value);
        // Notify all listeners
        for (BiConsumer<String, Object> listener : listeners) {
            try {
                listener.accept(channelName, value);
            } catch (Exception e) {
                System.err.println("Error notifying listener: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get a value from the data store
     */
    public Object getValue(String channelName) {
        return dataMap.get(channelName);
    }
    
    /**
     * Get all values as a map
     */
    public Map<String, Object> getAllValues() {
        return new ConcurrentHashMap<>(dataMap);
    }
    
    /**
     * Add a listener to be notified when values change
     */
    public void addListener(BiConsumer<String, Object> listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove a listener
     */
    public void removeListener(BiConsumer<String, Object> listener) {
        listeners.remove(listener);
    }
    
    /**
     * Clear all data
     */
    public void clear() {
        dataMap.clear();
    }
    
    /**
     * Get the number of stored values
     */
    public int size() {
        return dataMap.size();
    }
}

