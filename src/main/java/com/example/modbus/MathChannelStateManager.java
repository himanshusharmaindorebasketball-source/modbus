package com.example.modbus;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages state for math channel functions like counters and timers
 */
public class MathChannelStateManager {
    private static final MathChannelStateManager instance = new MathChannelStateManager();
    
    // Counter states: channelName -> counter value
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    
    // Timer states: channelName -> timer start time (milliseconds)
    private final Map<String, Long> timerStartTimes = new ConcurrentHashMap<>();
    
    // Timer states: channelName -> timer stop time (milliseconds)
    private final Map<String, Long> timerStopTimes = new ConcurrentHashMap<>();
    
    // Timer states: channelName -> timer running status
    private final Map<String, Boolean> timerRunning = new ConcurrentHashMap<>();
    
    private MathChannelStateManager() {
        // Private constructor for singleton
    }
    
    public static MathChannelStateManager getInstance() {
        return instance;
    }
    
    // Counter functions
    public long getCounter(String channelName) {
        return counters.computeIfAbsent(channelName, k -> new AtomicLong(0)).get();
    }
    
    public long incrementCounter(String channelName) {
        return counters.computeIfAbsent(channelName, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    public long decrementCounter(String channelName) {
        return counters.computeIfAbsent(channelName, k -> new AtomicLong(0)).decrementAndGet();
    }
    
    public void resetCounter(String channelName) {
        counters.computeIfAbsent(channelName, k -> new AtomicLong(0)).set(0);
    }
    
    public void setCounter(String channelName, long value) {
        counters.computeIfAbsent(channelName, k -> new AtomicLong(0)).set(value);
    }
    
    // Timer functions
    public void startTimer(String channelName) {
        long currentTime = System.currentTimeMillis();
        timerStartTimes.put(channelName, currentTime);
        timerRunning.put(channelName, true);
        timerStopTimes.remove(channelName); // Clear any previous stop time
    }
    
    public void stopTimer(String channelName) {
        if (timerRunning.getOrDefault(channelName, false)) {
            timerStopTimes.put(channelName, System.currentTimeMillis());
            timerRunning.put(channelName, false);
        }
    }
    
    public void resetTimer(String channelName) {
        timerStartTimes.remove(channelName);
        timerStopTimes.remove(channelName);
        timerRunning.put(channelName, false);
    }
    
    public double getElapsedTime(String channelName) {
        if (!timerStartTimes.containsKey(channelName)) {
            return 0.0;
        }
        
        long startTime = timerStartTimes.get(channelName);
        long endTime;
        
        if (timerRunning.getOrDefault(channelName, false)) {
            // Timer is running, use current time
            endTime = System.currentTimeMillis();
        } else if (timerStopTimes.containsKey(channelName)) {
            // Timer is stopped, use stop time
            endTime = timerStopTimes.get(channelName);
        } else {
            // Timer was never started properly
            return 0.0;
        }
        
        return (endTime - startTime) / 1000.0; // Return time in seconds
    }
    
    public boolean isTimerRunning(String channelName) {
        return timerRunning.getOrDefault(channelName, false);
    }
    
    // Utility functions
    public void clearAllStates() {
        counters.clear();
        timerStartTimes.clear();
        timerStopTimes.clear();
        timerRunning.clear();
    }
    
    public void clearChannelState(String channelName) {
        counters.remove(channelName);
        timerStartTimes.remove(channelName);
        timerStopTimes.remove(channelName);
        timerRunning.remove(channelName);
    }
    
    // Get all counter names
    public java.util.Set<String> getCounterNames() {
        return counters.keySet();
    }
    
    // Get all timer names
    public java.util.Set<String> getTimerNames() {
        return timerStartTimes.keySet();
    }
}
