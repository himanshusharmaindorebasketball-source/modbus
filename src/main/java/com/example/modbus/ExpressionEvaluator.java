package com.example.modbus;

import java.util.Map;
import java.util.Stack;

public class ExpressionEvaluator {
    public double evaluate(String expr, Map<String, Double> variables) {
        if (expr == null || expr.trim().isEmpty()) return Double.NaN;
        String replaced = replaceVariables(expr, variables);
        return evaluateSimple(replaced);
    }

    private String replaceVariables(String expr, Map<String, Double> vars) {
        String out = expr;
        for (Map.Entry<String, Double> e : vars.entrySet()) {
            String key = e.getKey();
            String val = e.getValue() == null ? "0" : Double.toString(e.getValue());
            out = out.replaceAll("\\b" + java.util.regex.Pattern.quote(key) + "\\b", val);
        }
        return out;
    }

    // Shunting-yard for +,-,*,/, parentheses
    private double evaluateSimple(String s) {
        Stack<Double> nums = new Stack<>();
        Stack<Character> ops = new Stack<>();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (Character.isDigit(c) || c == '.') {
                int j = i;
                while (j < s.length() && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.')) j++;
                nums.push(Double.parseDouble(s.substring(i, j)));
                i = j;
            } else if (c == '(') {
                ops.push(c);
                i++;
            } else if (c == ')') {
                while (!ops.isEmpty() && ops.peek() != '(') applyOp(nums, ops.pop());
                if (!ops.isEmpty() && ops.peek() == '(') ops.pop();
                i++;
            } else if (isOp(c)) {
                while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(c)) applyOp(nums, ops.pop());
                ops.push(c);
                i++;
            } else {
                // skip unknown
                i++;
            }
        }
        while (!ops.isEmpty()) applyOp(nums, ops.pop());
        return nums.isEmpty() ? Double.NaN : nums.pop();
    }

    private boolean isOp(char c) { return c=='+' || c=='-' || c=='*' || c=='/'; }
    private int precedence(char c) { return (c=='+'||c=='-') ? 1 : (c=='*'||c=='/') ? 2 : 0; }

    private void applyOp(Stack<Double> nums, char op) {
        if (nums.size() < 2) return;
        double b = nums.pop();
        double a = nums.pop();
        double r;
        switch (op) {
            case '+': r = a + b; break;
            case '-': r = a - b; break;
            case '*': r = a * b; break;
            case '/': r = b == 0 ? Double.NaN : a / b; break;
            default: r = Double.NaN;
        }
        nums.push(r);
    }
}

