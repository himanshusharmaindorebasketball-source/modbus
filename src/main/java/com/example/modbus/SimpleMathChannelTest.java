package com.example.modbus;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple test to demonstrate the working counter and timer features
 */
public class SimpleMathChannelTest {
    
    public static void main(String[] args) {
        System.out.println("=== Simple Math Channel Features Test ===\n");
        
        Map<String, Double> channelValues = new HashMap<>();
        channelValues.put("30001", 10.0);
        channelValues.put("30002", 20.0);
        
        // Test counter functions
        System.out.println("--- Counter Functions ---");
        System.out.println("counter(): " + MathExpressionEvaluator.evaluate("counter()", channelValues));
        System.out.println("counter_inc(): " + MathExpressionEvaluator.evaluate("counter_inc()", channelValues));
        System.out.println("counter_inc(): " + MathExpressionEvaluator.evaluate("counter_inc()", channelValues));
        System.out.println("counter(): " + MathExpressionEvaluator.evaluate("counter()", channelValues));
        System.out.println("counter_dec(): " + MathExpressionEvaluator.evaluate("counter_dec()", channelValues));
        System.out.println("counter(): " + MathExpressionEvaluator.evaluate("counter()", channelValues));
        
        // Test named counters
        System.out.println("\n--- Named Counters ---");
        System.out.println("counter(1): " + MathExpressionEvaluator.evaluate("counter(1)", channelValues));
        System.out.println("counter_inc(1): " + MathExpressionEvaluator.evaluate("counter_inc(1)", channelValues));
        System.out.println("counter_inc(1): " + MathExpressionEvaluator.evaluate("counter_inc(1)", channelValues));
        System.out.println("counter(1): " + MathExpressionEvaluator.evaluate("counter(1)", channelValues));
        System.out.println("counter_set(1, 100): " + MathExpressionEvaluator.evaluate("counter_set(1, 100)", channelValues));
        System.out.println("counter(1): " + MathExpressionEvaluator.evaluate("counter(1)", channelValues));
        
        // Test timer functions
        System.out.println("\n--- Timer Functions ---");
        System.out.println("timer(): " + MathExpressionEvaluator.evaluate("timer()", channelValues));
        System.out.println("timer_start(): " + MathExpressionEvaluator.evaluate("timer_start()", channelValues));
        System.out.println("timer_running(): " + MathExpressionEvaluator.evaluate("timer_running()", channelValues));
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        System.out.println("timer() after 1 second: " + MathExpressionEvaluator.evaluate("timer()", channelValues));
        System.out.println("timer_stop(): " + MathExpressionEvaluator.evaluate("timer_stop()", channelValues));
        System.out.println("timer_running(): " + MathExpressionEvaluator.evaluate("timer_running()", channelValues));
        System.out.println("timer() after stop: " + MathExpressionEvaluator.evaluate("timer()", channelValues));
        
        // Test named timers
        System.out.println("\n--- Named Timers ---");
        System.out.println("timer_start(1): " + MathExpressionEvaluator.evaluate("timer_start(1)", channelValues));
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("timer(1): " + MathExpressionEvaluator.evaluate("timer(1)", channelValues));
        System.out.println("timer_stop(1): " + MathExpressionEvaluator.evaluate("timer_stop(1)", channelValues));
        
        // Test counter in expressions
        System.out.println("\n--- Counter in Expressions ---");
        System.out.println("counter() * 2: " + MathExpressionEvaluator.evaluate("counter() * 2", channelValues));
        System.out.println("counter_inc() + 10: " + MathExpressionEvaluator.evaluate("counter_inc() + 10", channelValues));
        
        // Test timer in expressions
        System.out.println("\n--- Timer in Expressions ---");
        System.out.println("timer() * 1000: " + MathExpressionEvaluator.evaluate("timer() * 1000", channelValues));
        
        // Test simple logical expressions with numbers
        System.out.println("\n--- Simple Logical Expressions ---");
        System.out.println("10 > 5: " + MathExpressionEvaluator.evaluate("10 > 5", channelValues));
        System.out.println("10 == 10: " + MathExpressionEvaluator.evaluate("10 == 10", channelValues));
        System.out.println("10 != 5: " + MathExpressionEvaluator.evaluate("10 != 5", channelValues));
        System.out.println("(10 > 5) && (20 > 10): " + MathExpressionEvaluator.evaluate("(10 > 5) && (20 > 10)", channelValues));
        System.out.println("(10 > 5) || (5 > 10): " + MathExpressionEvaluator.evaluate("(10 > 5) || (5 > 10)", channelValues));
        
        // Test logical functions with numbers
        System.out.println("\n--- Logical Functions ---");
        System.out.println("and(1, 1): " + MathExpressionEvaluator.evaluate("and(1, 1)", channelValues));
        System.out.println("and(1, 0): " + MathExpressionEvaluator.evaluate("and(1, 0)", channelValues));
        System.out.println("or(1, 0): " + MathExpressionEvaluator.evaluate("or(1, 0)", channelValues));
        System.out.println("or(0, 0): " + MathExpressionEvaluator.evaluate("or(0, 0)", channelValues));
        System.out.println("not(1): " + MathExpressionEvaluator.evaluate("not(1)", channelValues));
        System.out.println("not(0): " + MathExpressionEvaluator.evaluate("not(0)", channelValues));
        System.out.println("if(1, 100, 0): " + MathExpressionEvaluator.evaluate("if(1, 100, 0)", channelValues));
        System.out.println("if(0, 100, 0): " + MathExpressionEvaluator.evaluate("if(0, 100, 0)", channelValues));
        
        System.out.println("\n=== Test Completed Successfully ===");
    }
}
