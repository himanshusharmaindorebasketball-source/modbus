package com.example.modbus;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Mathematical expression evaluator for math channels
 */
public class MathExpressionEvaluator {
    
    // Supported mathematical functions
    private static final Map<String, MathFunction> FUNCTIONS = new HashMap<>();
    
    // State manager for counters and timers
    private static final MathChannelStateManager stateManager = MathChannelStateManager.getInstance();
    
    static {
        // Mathematical functions
        FUNCTIONS.put("sin", args -> Math.sin(args[0]));
        FUNCTIONS.put("cos", args -> Math.cos(args[0]));
        FUNCTIONS.put("tan", args -> Math.tan(args[0]));
        FUNCTIONS.put("asin", args -> Math.asin(args[0]));
        FUNCTIONS.put("acos", args -> Math.acos(args[0]));
        FUNCTIONS.put("atan", args -> Math.atan(args[0]));
        FUNCTIONS.put("sqrt", args -> Math.sqrt(args[0]));
        FUNCTIONS.put("abs", args -> Math.abs(args[0]));
        FUNCTIONS.put("log", args -> Math.log(args[0]));
        FUNCTIONS.put("log10", args -> Math.log10(args[0]));
        FUNCTIONS.put("exp", args -> Math.exp(args[0]));
        FUNCTIONS.put("pow", args -> Math.pow(args[0], args[1]));
        FUNCTIONS.put("min", args -> Math.min(args[0], args[1]));
        FUNCTIONS.put("max", args -> Math.max(args[0], args[1]));
        FUNCTIONS.put("round", args -> Math.round(args[0]));
        FUNCTIONS.put("ceil", args -> Math.ceil(args[0]));
        FUNCTIONS.put("floor", args -> Math.floor(args[0]));
        
        // Counter functions
        FUNCTIONS.put("counter", args -> {
            if (args.length == 0) {
                return stateManager.getCounter("default");
            } else {
                // Use channel name as counter name
                String counterName = "counter_" + (long)args[0];
                return stateManager.getCounter(counterName);
            }
        });
        FUNCTIONS.put("counter_inc", args -> {
            if (args.length == 0) {
                return stateManager.incrementCounter("default");
            } else {
                String counterName = "counter_" + (long)args[0];
                return stateManager.incrementCounter(counterName);
            }
        });
        FUNCTIONS.put("counter_dec", args -> {
            if (args.length == 0) {
                return stateManager.decrementCounter("default");
            } else {
                String counterName = "counter_" + (long)args[0];
                return stateManager.decrementCounter(counterName);
            }
        });
        FUNCTIONS.put("counter_reset", args -> {
            if (args.length == 0) {
                stateManager.resetCounter("default");
            } else {
                String counterName = "counter_" + (long)args[0];
                stateManager.resetCounter(counterName);
            }
            return 0.0;
        });
        FUNCTIONS.put("counter_set", args -> {
            if (args.length < 2) {
                throw new IllegalArgumentException("counter_set requires 2 arguments: counter_set(channel, value)");
            }
            String counterName = "counter_" + (long)args[0];
            stateManager.setCounter(counterName, (long)args[1]);
            return args[1];
        });
        
        // Timer functions
        FUNCTIONS.put("timer", args -> {
            if (args.length == 0) {
                return stateManager.getElapsedTime("default");
            } else {
                String timerName = "timer_" + (long)args[0];
                return stateManager.getElapsedTime(timerName);
            }
        });
        FUNCTIONS.put("timer_start", args -> {
            if (args.length == 0) {
                stateManager.startTimer("default");
            } else {
                String timerName = "timer_" + (long)args[0];
                stateManager.startTimer(timerName);
            }
            return 0.0;
        });
        FUNCTIONS.put("timer_stop", args -> {
            if (args.length == 0) {
                stateManager.stopTimer("default");
            } else {
                String timerName = "timer_" + (long)args[0];
                stateManager.stopTimer(timerName);
            }
            return 0.0;
        });
        FUNCTIONS.put("timer_reset", args -> {
            if (args.length == 0) {
                stateManager.resetTimer("default");
            } else {
                String timerName = "timer_" + (long)args[0];
                stateManager.resetTimer(timerName);
            }
            return 0.0;
        });
        FUNCTIONS.put("timer_running", args -> {
            if (args.length == 0) {
                return stateManager.isTimerRunning("default") ? 1.0 : 0.0;
            } else {
                String timerName = "timer_" + (long)args[0];
                return stateManager.isTimerRunning(timerName) ? 1.0 : 0.0;
            }
        });
        
        // Logical functions
        FUNCTIONS.put("and", args -> (args[0] != 0.0 && args[1] != 0.0) ? 1.0 : 0.0);
        FUNCTIONS.put("or", args -> (args[0] != 0.0 || args[1] != 0.0) ? 1.0 : 0.0);
        FUNCTIONS.put("not", args -> (args[0] == 0.0) ? 1.0 : 0.0);
        FUNCTIONS.put("eq", args -> (args[0] == args[1]) ? 1.0 : 0.0);
        FUNCTIONS.put("ne", args -> (args[0] != args[1]) ? 1.0 : 0.0);
        FUNCTIONS.put("gt", args -> (args[0] > args[1]) ? 1.0 : 0.0);
        FUNCTIONS.put("ge", args -> (args[0] >= args[1]) ? 1.0 : 0.0);
        FUNCTIONS.put("lt", args -> (args[0] < args[1]) ? 1.0 : 0.0);
        FUNCTIONS.put("le", args -> (args[0] <= args[1]) ? 1.0 : 0.0);
        FUNCTIONS.put("if", args -> (args[0] != 0.0) ? args[1] : args[2]);
    }
    
    @FunctionalInterface
    private interface MathFunction {
        double apply(double... args);
    }
    
    /**
     * Evaluate a mathematical expression with channel values
     */
    public static double evaluate(String expression, Map<String, Double> channelValues) {
        if (expression == null || expression.trim().isEmpty()) {
            return 0.0;
        }
        
        try {
            // First, evaluate functions (including counter and timer functions)
            String processedExpression = evaluateFunctions(expression);
            
            // Then replace channel names with their values
            processedExpression = replaceChannelNames(processedExpression, channelValues);
            
            // Finally, evaluate the mathematical expression
            double result = evaluateExpression(processedExpression);
            
            return result;
        } catch (Exception e) {
            System.err.println("Error evaluating expression '" + expression + "': " + e.getMessage());
            return Double.NaN;
        }
    }
    
    /**
     * Replace channel names and register addresses in expression with their numeric values
     */
    private static String replaceChannelNames(String expression, Map<String, Double> channelValues) {
        String result = expression;
        
        // First, replace register addresses (like 30001, 40001, etc.)
        Pattern registerPattern = Pattern.compile("\\b(\\d{5})\\b");
        Matcher registerMatcher = registerPattern.matcher(result);
        
        while (registerMatcher.find()) {
            String registerAddress = registerMatcher.group(1);
            
            // Find the channel name that corresponds to this register address
            String channelName = findChannelNameByAddress(registerAddress, channelValues);
            if (channelName != null) {
                Double value = channelValues.get(channelName);
                if (value != null) {
                    // Replace the register address with its actual value
                    result = result.replaceAll("\\b" + Pattern.quote(registerAddress) + "\\b", value.toString());
                } else {
                    throw new IllegalArgumentException("No value found for register address " + registerAddress + " (channel: " + channelName + ")");
                }
            } else {
                throw new IllegalArgumentException("No channel found for register address " + registerAddress);
            }
        }
        
        // Then, try to replace exact channel names (including those with spaces and special characters)
        for (String channelName : channelValues.keySet()) {
            // Skip if it's a mathematical function
            if (FUNCTIONS.containsKey(channelName.toLowerCase())) {
                continue;
            }
            
            // Skip if it's a mathematical constant
            if (isMathConstant(channelName)) {
                continue;
            }
            
            // Skip if it's a function call (contains parentheses)
            if (result.contains(channelName + "(")) {
                continue;
            }
            
            // Replace exact channel name with its value
            Double value = channelValues.get(channelName);
            if (value != null) {
                // For channel names with spaces or special characters, use direct replacement
                if (channelName.contains(" ") || channelName.contains("-")) {
                    // Complex name with spaces/special chars - replace directly
                    result = result.replace(Pattern.quote(channelName), value.toString());
                } else {
                    // Simple name - use word boundaries
                    result = result.replaceAll("\\b" + Pattern.quote(channelName) + "\\b", value.toString());
                }
            }
        }
        
        // Finally, find any remaining unmatched identifiers and check if they're valid
        Pattern identifierPattern = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_\\s\\-]*)\\b");
        Matcher matcher = identifierPattern.matcher(result);
        
        while (matcher.find()) {
            String identifier = matcher.group(1).trim();
            
            // Skip if it's a mathematical function
            if (FUNCTIONS.containsKey(identifier.toLowerCase())) {
                continue;
            }
            
            // Skip if it's a mathematical constant
            if (isMathConstant(identifier)) {
                continue;
            }
            
            // Skip if it's already a number
            try {
                Double.parseDouble(identifier);
                continue;
            } catch (NumberFormatException e) {
                // Not a number, continue checking
            }
            
            // If we find an identifier that's not in our channel values, it's an error
            if (!channelValues.containsKey(identifier)) {
                throw new IllegalArgumentException("Channel '" + identifier + "' not found in values");
            }
        }
        
        return result;
    }
    
    /**
     * Find channel name by register address
     */
    private static String findChannelNameByAddress(String registerAddress, Map<String, Double> channelValues) {
        // Load Modbus configurations to find the channel name for this address
        try {
            List<ModbusConfigManager.ModbusConfig> configs = ModbusConfigManager.loadConfig();
            for (ModbusConfigManager.ModbusConfig config : configs) {
                if (String.valueOf(config.getAddress()).equals(registerAddress)) {
                    return config.getChannelName();
                }
            }
        } catch (Exception e) {
            // If we can't load configs, return null
        }
        return null;
    }
    
    /**
     * Check if a string is a mathematical constant
     */
    private static boolean isMathConstant(String name) {
        return name.equals("pi") || name.equals("e");
    }
    
    /**
     * Evaluate a mathematical expression (simplified parser)
     */
    private static double evaluateExpression(String expression) {
        // Replace mathematical constants
        expression = expression.replaceAll("\\bpi\\b", String.valueOf(Math.PI));
        expression = expression.replaceAll("\\be\\b", String.valueOf(Math.E));
        
        // Handle parentheses first
        expression = evaluateParentheses(expression);
        
        // Handle power operations (^)
        expression = evaluatePower(expression);
        
        // Handle multiplication and division
        expression = evaluateMultiplicationDivision(expression);
        
        // Handle addition and subtraction
        expression = evaluateAdditionSubtraction(expression);
        
        // Handle logical operations
        expression = evaluateLogicalOperations(expression);
        
        return Double.parseDouble(expression);
    }
    
    /**
     * Evaluate mathematical functions in the expression
     */
    private static String evaluateFunctions(String expression) {
        Pattern functionPattern = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(([^)]*)\\)");
        Matcher matcher = functionPattern.matcher(expression);
        
        while (matcher.find()) {
            String functionName = matcher.group(1).toLowerCase();
            String args = matcher.group(2);
            
            if (FUNCTIONS.containsKey(functionName)) {
                MathFunction func = FUNCTIONS.get(functionName);
                String[] argArray = args.isEmpty() ? new String[0] : args.split(",");
                
                try {
                    double result;
                    if (argArray.length == 0) {
                        // No arguments (like counter(), timer())
                        result = func.apply();
                    } else if (argArray.length == 1) {
                        // Single argument
                        double arg = Double.parseDouble(argArray[0].trim());
                        result = func.apply(arg);
                    } else if (argArray.length == 2) {
                        // Two arguments
                        double arg1 = Double.parseDouble(argArray[0].trim());
                        double arg2 = Double.parseDouble(argArray[1].trim());
                        result = func.apply(arg1, arg2);
                    } else if (argArray.length == 3) {
                        // Three arguments (like if function)
                        double arg1 = Double.parseDouble(argArray[0].trim());
                        double arg2 = Double.parseDouble(argArray[1].trim());
                        double arg3 = Double.parseDouble(argArray[2].trim());
                        result = func.apply(arg1, arg2, arg3);
                    } else {
                        throw new IllegalArgumentException("Too many arguments for function " + functionName);
                    }
                    expression = expression.replace(matcher.group(0), String.valueOf(result));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid argument for function " + functionName + ": " + e.getMessage());
                }
            }
        }
        
        return expression;
    }
    
    /**
     * Evaluate power operations (^)
     */
    private static String evaluatePower(String expression) {
        Pattern powerPattern = Pattern.compile("([0-9.]+)\\s*\\^\\s*([0-9.]+)");
        Matcher matcher = powerPattern.matcher(expression);
        
        while (matcher.find()) {
            double base = Double.parseDouble(matcher.group(1));
            double exponent = Double.parseDouble(matcher.group(2));
            double result = Math.pow(base, exponent);
            expression = expression.replace(matcher.group(0), String.valueOf(result));
        }
        
        return expression;
    }
    
    /**
     * Evaluate multiplication and division
     */
    private static String evaluateMultiplicationDivision(String expression) {
        Pattern mdPattern = Pattern.compile("([0-9.]+)\\s*([*/])\\s*([0-9.]+)");
        Matcher matcher = mdPattern.matcher(expression);
        
        while (matcher.find()) {
            double left = Double.parseDouble(matcher.group(1));
            String operator = matcher.group(2);
            double right = Double.parseDouble(matcher.group(3));
            
            double result = operator.equals("*") ? left * right : left / right;
            expression = expression.replace(matcher.group(0), String.valueOf(result));
        }
        
        return expression;
    }
    
    /**
     * Evaluate addition and subtraction
     */
    private static String evaluateAdditionSubtraction(String expression) {
        Pattern asPattern = Pattern.compile("([0-9.]+)\\s*([+-])\\s*([0-9.]+)");
        Matcher matcher = asPattern.matcher(expression);
        
        while (matcher.find()) {
            double left = Double.parseDouble(matcher.group(1));
            String operator = matcher.group(2);
            double right = Double.parseDouble(matcher.group(3));
            
            double result = operator.equals("+") ? left + right : left - right;
            expression = expression.replace(matcher.group(0), String.valueOf(result));
        }
        
        return expression;
    }
    
    /**
     * Evaluate logical operations (==, !=, <, >, <=, >=, &&, ||, !)
     */
    private static String evaluateLogicalOperations(String expression) {
        // Handle comparison operators first (higher precedence)
        expression = evaluateComparisonOperators(expression);
        
        // Handle logical AND (&&)
        expression = evaluateLogicalAnd(expression);
        
        // Handle logical OR (||)
        expression = evaluateLogicalOr(expression);
        
        return expression;
    }
    
    /**
     * Evaluate comparison operators (==, !=, <, >, <=, >=)
     */
    private static String evaluateComparisonOperators(String expression) {
        // Handle == and !=
        Pattern eqPattern = Pattern.compile("([0-9.]+)\\s*(==|!=)\\s*([0-9.]+)");
        Matcher matcher = eqPattern.matcher(expression);
        
        while (matcher.find()) {
            double left = Double.parseDouble(matcher.group(1));
            String operator = matcher.group(2);
            double right = Double.parseDouble(matcher.group(3));
            
            double result;
            if (operator.equals("==")) {
                result = (left == right) ? 1.0 : 0.0;
            } else { // !=
                result = (left != right) ? 1.0 : 0.0;
            }
            expression = expression.replace(matcher.group(0), String.valueOf(result));
        }
        
        // Handle <, >, <=, >=
        Pattern compPattern = Pattern.compile("([0-9.]+)\\s*(<|>|<=|>=)\\s*([0-9.]+)");
        matcher = compPattern.matcher(expression);
        
        while (matcher.find()) {
            double left = Double.parseDouble(matcher.group(1));
            String operator = matcher.group(2);
            double right = Double.parseDouble(matcher.group(3));
            
            double result;
            switch (operator) {
                case "<": result = (left < right) ? 1.0 : 0.0; break;
                case ">": result = (left > right) ? 1.0 : 0.0; break;
                case "<=": result = (left <= right) ? 1.0 : 0.0; break;
                case ">=": result = (left >= right) ? 1.0 : 0.0; break;
                default: result = 0.0;
            }
            expression = expression.replace(matcher.group(0), String.valueOf(result));
        }
        
        return expression;
    }
    
    /**
     * Evaluate logical AND (&&)
     */
    private static String evaluateLogicalAnd(String expression) {
        Pattern andPattern = Pattern.compile("([0-9.]+)\\s*&&\\s*([0-9.]+)");
        Matcher matcher = andPattern.matcher(expression);
        
        while (matcher.find()) {
            double left = Double.parseDouble(matcher.group(1));
            double right = Double.parseDouble(matcher.group(2));
            
            double result = (left != 0.0 && right != 0.0) ? 1.0 : 0.0;
            expression = expression.replace(matcher.group(0), String.valueOf(result));
        }
        
        return expression;
    }
    
    /**
     * Evaluate logical OR (||)
     */
    private static String evaluateLogicalOr(String expression) {
        Pattern orPattern = Pattern.compile("([0-9.]+)\\s*\\|\\|\\s*([0-9.]+)");
        Matcher matcher = orPattern.matcher(expression);
        
        while (matcher.find()) {
            double left = Double.parseDouble(matcher.group(1));
            double right = Double.parseDouble(matcher.group(2));
            
            double result = (left != 0.0 || right != 0.0) ? 1.0 : 0.0;
            expression = expression.replace(matcher.group(0), String.valueOf(result));
        }
        
        return expression;
    }
    
    /**
     * Format a double value with specified decimal places
     */
    public static String formatValue(double value, int decimalPlaces) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "Error";
        }
        
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(decimalPlaces, RoundingMode.HALF_UP);
        return bd.toString();
    }
    
    /**
     * Validate an expression for syntax errors
     */
    public static boolean isValidExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Basic validation - check for balanced parentheses
            int parenCount = 0;
            for (char c : expression.toCharArray()) {
                if (c == '(') parenCount++;
                if (c == ')') parenCount--;
                if (parenCount < 0) return false;
            }
            return parenCount == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Evaluate parentheses in the expression
     */
    private static String evaluateParentheses(String expression) {
        while (expression.contains("(")) {
            int start = expression.lastIndexOf("(");
            int end = findMatchingParenthesis(expression, start);
            
            if (end == -1) {
                throw new IllegalArgumentException("Unmatched parentheses in expression: " + expression);
            }
            
            String innerExpression = expression.substring(start + 1, end);
            double result = evaluateExpression(innerExpression); // Recursive call
            expression = expression.substring(0, start) + result + expression.substring(end + 1);
        }
        
        return expression;
    }
    
    /**
     * Find the matching closing parenthesis for an opening parenthesis
     */
    private static int findMatchingParenthesis(String expression, int start) {
        int count = 1;
        for (int i = start + 1; i < expression.length(); i++) {
            if (expression.charAt(i) == '(') {
                count++;
            } else if (expression.charAt(i) == ')') {
                count--;
                if (count == 0) {
                    return i;
                }
            }
        }
        return -1; // No matching parenthesis found
    }
}
