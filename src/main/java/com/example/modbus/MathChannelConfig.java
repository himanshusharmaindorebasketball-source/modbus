package com.example.modbus;

/**
 * Configuration class for mathematical channels
 */
public class MathChannelConfig {
    private String channelName;
    private String expression;
    private String description;
    private String unit;
    private int decimalPlaces;
    private boolean enabled;
    
    public MathChannelConfig() {
        this.channelName = "";
        this.expression = "";
        this.description = "";
        this.unit = "";
        this.decimalPlaces = 2;
        this.enabled = true;
    }
    
    public MathChannelConfig(String channelName, String expression, String description, String unit, int decimalPlaces) {
        this.channelName = channelName;
        this.expression = expression;
        this.description = description;
        this.unit = unit;
        this.decimalPlaces = decimalPlaces;
        this.enabled = true;
    }
    
    // Getters and setters
    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    
    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    public int getDecimalPlaces() { return decimalPlaces; }
    public void setDecimalPlaces(int decimalPlaces) { this.decimalPlaces = decimalPlaces; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    /**
     * Check if this math channel uses counter functions
     */
    public boolean usesCounters() {
        return expression != null && (
            expression.contains("counter()") || 
            expression.contains("counter_inc") || 
            expression.contains("counter_dec") || 
            expression.contains("counter_reset") ||
            expression.contains("counter_set")
        );
    }
    
    /**
     * Check if this math channel uses timer functions
     */
    public boolean usesTimers() {
        return expression != null && (
            expression.contains("timer()") || 
            expression.contains("timer_start") || 
            expression.contains("timer_stop") || 
            expression.contains("timer_reset") ||
            expression.contains("timer_running")
        );
    }
    
    /**
     * Check if this math channel uses logical expressions
     */
    public boolean usesLogicalExpressions() {
        return expression != null && (
            expression.contains("&&") || 
            expression.contains("||") || 
            expression.contains("==") || 
            expression.contains("!=") ||
            expression.contains("<") || 
            expression.contains(">") || 
            expression.contains("<=") || 
            expression.contains(">=") ||
            expression.contains("and(") || 
            expression.contains("or(") || 
            expression.contains("not(") ||
            expression.contains("if(")
        );
    }
    
    /**
     * Get the function type category for this math channel
     */
    public String getFunctionType() {
        if (usesCounters() && usesTimers()) {
            return "Counter + Timer";
        } else if (usesCounters()) {
            return "Counter";
        } else if (usesTimers()) {
            return "Timer";
        } else if (usesLogicalExpressions()) {
            return "Logical";
        } else {
            return "Mathematical";
        }
    }
    
    @Override
    public String toString() {
        return String.format("MathChannel[%s: %s (%s)]", channelName, expression, getFunctionType());
    }
}

