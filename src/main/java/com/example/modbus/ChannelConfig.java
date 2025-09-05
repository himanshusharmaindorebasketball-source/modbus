package com.example.modbus;

import java.awt.*;

public class ChannelConfig {
    private final int channelNumber;
    private final int channelAddress;
    private final String dataType;
    private final int deviceId;
    private final double value;
    private final double low;
    private final double high;
    private final double offset;
    private final int maxDecimalDigits;
    private final Color channelColor;
    private final String channelMaths;
    private final String unit;
    private final String channelName; // Added channelName field

    public ChannelConfig(int channelNumber, int channelAddress, String dataType, int deviceId, double value, double low, double high, double offset, int maxDecimalDigits, Color channelColor, String channelMaths, String unit, String channelName) {
        this.channelNumber = channelNumber;
        this.channelAddress = channelAddress;
        this.dataType = dataType;
        this.deviceId = deviceId;
        this.value = value;
        this.low = low;
        this.high = high;
        this.offset = offset;
        this.maxDecimalDigits = maxDecimalDigits;
        this.channelColor = channelColor;
        this.channelMaths = channelMaths;
        this.unit = unit;
        this.channelName = channelName; // Initialize channelName
    }

    // Getters
    public int getChannelNumber() { return channelNumber; }
    public int getChannelAddress() { return channelAddress; }
    public String getDataType() { return dataType; }
    public int getDeviceId() { return deviceId; }
    public double getValue() { return value; }
    public double getLow() { return low; }
    public double getHigh() { return high; }
    public double getOffset() { return offset; }
    public int getMaxDecimalDigits() { return maxDecimalDigits; }
    public Color getChannelColor() { return channelColor; }
    public String getChannelMaths() { return channelMaths; }
    public String getUnit() { return unit; }
    public String getChannelName() { return channelName; } // Added getChannelName

    @Override
    public String toString() {
        return channelName + " (Addr: " + channelAddress + ", Type: " + dataType + ")";
    }
}