package com.example.modbus;

import java.awt.Color;
import java.awt.Font;

/**
 * Configuration class for individual channel display settings
 */
public class ChannelDisplayConfig {
    private String fontName;
    private int fontSize;
    private Color backgroundColor;
    private Color fontColor;
    private Color valueColor;
    
    public ChannelDisplayConfig() {
        // Default settings
        this.fontName = "Arial";
        this.fontSize = 14;
        this.backgroundColor = Color.WHITE;
        this.fontColor = Color.BLUE;
        this.valueColor = Color.DARK_GRAY;
    }
    
    public ChannelDisplayConfig(String fontName, int fontSize, Color backgroundColor, Color fontColor, Color valueColor) {
        this.fontName = fontName;
        this.fontSize = fontSize;
        this.backgroundColor = backgroundColor;
        this.valueColor = valueColor;
        this.fontColor = fontColor;
    }
    
    // Getters and setters
    public String getFontName() { return fontName; }
    public void setFontName(String fontName) { this.fontName = fontName; }
    
    public int getFontSize() { return fontSize; }
    public void setFontSize(int fontSize) { this.fontSize = fontSize; }
    
    public Color getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(Color backgroundColor) { this.backgroundColor = backgroundColor; }
    
    public Color getFontColor() { return fontColor; }
    public void setFontColor(Color fontColor) { this.fontColor = fontColor; }
    
    public Color getValueColor() { return valueColor; }
    public void setValueColor(Color valueColor) { this.valueColor = valueColor; }
    
    public Font getFont() {
        return new Font(fontName, Font.BOLD, fontSize);
    }
    
    public Font getValueFont() {
        return new Font(fontName, Font.BOLD, fontSize + 4);
    }
    
    @Override
    public String toString() {
        return String.format("Font: %s, Size: %d, BG: %s, Font: %s, Value: %s", 
            fontName, fontSize, backgroundColor, fontColor, valueColor);
    }
}

