package com.example.modbus;

import java.util.HashMap;
import java.util.Map;

/**
 * Test class to demonstrate and validate the new math channel features
 */
public class MathChannelFeaturesTest {
    
    public static void main(String[] args) {
        System.out.println("=== Math Channel Enhanced Features Test ===\n");
        
        // Test counter functions
        testCounterFunctions();
        
        // Test timer functions
        testTimerFunctions();
        
        // Test logical expressions
        testLogicalExpressions();
        
        // Test combined features
        testCombinedFeatures();
        
        System.out.println("\n=== All Tests Completed ===");
    }
    
    private static void testCounterFunctions() {
        System.out.println("--- Testing Counter Functions ---");
        
        Map<String, Double> channelValues = new HashMap<>();
        channelValues.put("30001", 10.0);
        channelValues.put("30002", 20.0);
        
        try {
            // Test basic counter operations
            System.out.println("counter(): " + MathExpressionEvaluator.evaluate("counter()", channelValues));
            System.out.println("counter_inc(): " + MathExpressionEvaluator.evaluate("counter_inc()", channelValues));
            System.out.println("counter_inc(): " + MathExpressionEvaluator.evaluate("counter_inc()", channelValues));
            System.out.println("counter(): " + MathExpressionEvaluator.evaluate("counter()", channelValues));
            System.out.println("counter_dec(): " + MathExpressionEvaluator.evaluate("counter_dec()", channelValues));
            System.out.println("counter(): " + MathExpressionEvaluator.evaluate("counter()", channelValues));
            
            // Test named counters
            System.out.println("counter(1): " + MathExpressionEvaluator.evaluate("counter(1)", channelValues));
            System.out.println("counter_inc(1): " + MathExpressionEvaluator.evaluate("counter_inc(1)", channelValues));
            System.out.println("counter_inc(1): " + MathExpressionEvaluator.evaluate("counter_inc(1)", channelValues));
            System.out.println("counter(1): " + MathExpressionEvaluator.evaluate("counter(1)", channelValues));
            
            // Test counter_set
            System.out.println("counter_set(1, 100): " + MathExpressionEvaluator.evaluate("counter_set(1, 100)", channelValues));
            System.out.println("counter(1): " + MathExpressionEvaluator.evaluate("counter(1)", channelValues));
            
            // Test counter in expressions
            System.out.println("counter() * 2: " + MathExpressionEvaluator.evaluate("counter() * 2", channelValues));
            System.out.println("counter_inc() + 10: " + MathExpressionEvaluator.evaluate("counter_inc() + 10", channelValues));
            
        } catch (Exception e) {
            System.err.println("Counter test error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void testTimerFunctions() {
        System.out.println("--- Testing Timer Functions ---");
        
        Map<String, Double> channelValues = new HashMap<>();
        
        try {
            // Test timer operations
            System.out.println("timer(): " + MathExpressionEvaluator.evaluate("timer()", channelValues));
            System.out.println("timer_start(): " + MathExpressionEvaluator.evaluate("timer_start()", channelValues));
            System.out.println("timer_running(): " + MathExpressionEvaluator.evaluate("timer_running()", channelValues));
            
            // Wait a bit
            Thread.sleep(1000);
            
            System.out.println("timer() after 1 second: " + MathExpressionEvaluator.evaluate("timer()", channelValues));
            System.out.println("timer_stop(): " + MathExpressionEvaluator.evaluate("timer_stop()", channelValues));
            System.out.println("timer_running(): " + MathExpressionEvaluator.evaluate("timer_running()", channelValues));
            System.out.println("timer() after stop: " + MathExpressionEvaluator.evaluate("timer()", channelValues));
            
            // Test named timers
            System.out.println("timer_start(1): " + MathExpressionEvaluator.evaluate("timer_start(1)", channelValues));
            Thread.sleep(500);
            System.out.println("timer(1): " + MathExpressionEvaluator.evaluate("timer(1)", channelValues));
            System.out.println("timer_stop(1): " + MathExpressionEvaluator.evaluate("timer_stop(1)", channelValues));
            
            // Test timer in expressions
            System.out.println("timer() * 1000: " + MathExpressionEvaluator.evaluate("timer() * 1000", channelValues));
            
        } catch (Exception e) {
            System.err.println("Timer test error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void testLogicalExpressions() {
        System.out.println("--- Testing Logical Expressions ---");
        
        Map<String, Double> channelValues = new HashMap<>();
        channelValues.put("30001", 10.0);
        channelValues.put("30002", 20.0);
        channelValues.put("30003", 0.0);
        
        try {
            // Test comparison operators
            System.out.println("30001 > 5: " + MathExpressionEvaluator.evaluate("30001 > 5", channelValues));
            System.out.println("30001 == 10: " + MathExpressionEvaluator.evaluate("30001 == 10", channelValues));
            System.out.println("30001 != 30002: " + MathExpressionEvaluator.evaluate("30001 != 30002", channelValues));
            System.out.println("30001 < 30002: " + MathExpressionEvaluator.evaluate("30001 < 30002", channelValues));
            System.out.println("30001 >= 10: " + MathExpressionEvaluator.evaluate("30001 >= 10", channelValues));
            System.out.println("30001 <= 15: " + MathExpressionEvaluator.evaluate("30001 <= 15", channelValues));
            
            // Test logical operators
            System.out.println("(30001 > 5) && (30002 > 10): " + MathExpressionEvaluator.evaluate("(30001 > 5) && (30002 > 10)", channelValues));
            System.out.println("(30001 > 5) || (30003 > 0): " + MathExpressionEvaluator.evaluate("(30001 > 5) || (30003 > 0)", channelValues));
            
            // Test logical functions
            System.out.println("and(30001 > 5, 30002 > 10): " + MathExpressionEvaluator.evaluate("and(30001 > 5, 30002 > 10)", channelValues));
            System.out.println("or(30001 > 5, 30003 > 0): " + MathExpressionEvaluator.evaluate("or(30001 > 5, 30003 > 0)", channelValues));
            System.out.println("not(30003 > 0): " + MathExpressionEvaluator.evaluate("not(30003 > 0)", channelValues));
            System.out.println("eq(30001, 10): " + MathExpressionEvaluator.evaluate("eq(30001, 10)", channelValues));
            System.out.println("gt(30002, 30001): " + MathExpressionEvaluator.evaluate("gt(30002, 30001)", channelValues));
            
            // Test conditional function
            System.out.println("if(30001 > 5, 100, 0): " + MathExpressionEvaluator.evaluate("if(30001 > 5, 100, 0)", channelValues));
            System.out.println("if(30003 > 0, 100, 0): " + MathExpressionEvaluator.evaluate("if(30003 > 0, 100, 0)", channelValues));
            
        } catch (Exception e) {
            System.err.println("Logical expression test error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void testCombinedFeatures() {
        System.out.println("--- Testing Combined Features ---");
        
        Map<String, Double> channelValues = new HashMap<>();
        channelValues.put("30001", 10.0);
        channelValues.put("30002", 20.0);
        
        try {
            // Test counter with logical control
            System.out.println("counter_reset(): " + MathExpressionEvaluator.evaluate("counter_reset()", channelValues));
            System.out.println("if(30001 > 5, counter_inc(), counter()): " + MathExpressionEvaluator.evaluate("if(30001 > 5, counter_inc(), counter())", channelValues));
            System.out.println("counter(): " + MathExpressionEvaluator.evaluate("counter()", channelValues));
            
            // Test timer with logical control
            System.out.println("timer_reset(): " + MathExpressionEvaluator.evaluate("timer_reset()", channelValues));
            System.out.println("if(30001 > 5, timer_start(), timer_stop()): " + MathExpressionEvaluator.evaluate("if(30001 > 5, timer_start(), timer_stop())", channelValues));
            System.out.println("timer_running(): " + MathExpressionEvaluator.evaluate("timer_running()", channelValues));
            
            // Test complex expression
            System.out.println("if((30001 > 5) && (30002 > 10), counter_inc(1) + timer_start(1), counter_reset(1)): " + 
                MathExpressionEvaluator.evaluate("if((30001 > 5) && (30002 > 10), counter_inc(1) + timer_start(1), counter_reset(1))", channelValues));
            
            System.out.println("counter(1): " + MathExpressionEvaluator.evaluate("counter(1)", channelValues));
            System.out.println("timer_running(1): " + MathExpressionEvaluator.evaluate("timer_running(1)", channelValues));
            
        } catch (Exception e) {
            System.err.println("Combined features test error: " + e.getMessage());
        }
        
        System.out.println();
    }
}
