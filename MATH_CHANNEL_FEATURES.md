# Math Channel Enhanced Features

This document describes the new counter, timer, and logical expression features added to the math channel system.

## Overview

The math channel system now supports:
- **Counter Functions**: Increment, decrement, reset, and set counter values
- **Timer Functions**: Start, stop, reset timers and get elapsed time
- **Logical Expressions**: AND, OR, NOT operations and comparison operators
- **State Management**: Persistent state across evaluations

## Counter Functions

### Basic Counter Operations

| Function | Description | Usage | Example |
|----------|-------------|-------|---------|
| `counter()` | Get current counter value | `counter()` or `counter(channel)` | `counter()` returns current value |
| `counter_inc()` | Increment counter by 1 | `counter_inc()` or `counter_inc(channel)` | `counter_inc()` increments and returns new value |
| `counter_dec()` | Decrement counter by 1 | `counter_dec()` or `counter_dec(channel)` | `counter_dec()` decrements and returns new value |
| `counter_reset()` | Reset counter to 0 | `counter_reset()` or `counter_reset(channel)` | `counter_reset()` sets counter to 0 |
| `counter_set(channel, value)` | Set counter to specific value | `counter_set(channel, value)` | `counter_set(1, 100)` sets counter_1 to 100 |

### Counter Examples

```javascript
// Basic counter usage
counter()                    // Get default counter value
counter_inc()                // Increment default counter
counter_dec()                // Decrement default counter
counter_reset()              // Reset default counter

// Named counter usage (using channel numbers)
counter(1)                   // Get counter for channel 1
counter_inc(1)               // Increment counter for channel 1
counter_set(1, 50)           // Set counter for channel 1 to 50

// Counter in expressions
counter() * 2                // Double the counter value
counter_inc(1) + 10          // Increment counter_1 and add 10
```

## Timer Functions

### Basic Timer Operations

| Function | Description | Usage | Example |
|----------|-------------|-------|---------|
| `timer()` | Get elapsed time in seconds | `timer()` or `timer(channel)` | `timer()` returns elapsed seconds |
| `timer_start()` | Start timer | `timer_start()` or `timer_start(channel)` | `timer_start()` starts default timer |
| `timer_stop()` | Stop timer | `timer_stop()` or `timer_stop(channel)` | `timer_stop()` stops default timer |
| `timer_reset()` | Reset timer | `timer_reset()` or `timer_reset(channel)` | `timer_reset()` resets default timer |
| `timer_running()` | Check if timer is running | `timer_running()` or `timer_running(channel)` | `timer_running()` returns 1 if running, 0 if stopped |

### Timer Examples

```javascript
// Basic timer usage
timer()                      // Get elapsed time for default timer
timer_start()                // Start default timer
timer_stop()                 // Stop default timer
timer_reset()                // Reset default timer
timer_running()              // Check if default timer is running

// Named timer usage (using channel numbers)
timer(1)                     // Get elapsed time for timer_1
timer_start(1)               // Start timer for channel 1
timer_stop(1)                // Stop timer for channel 1

// Timer in expressions
timer() * 1000               // Convert seconds to milliseconds
timer_running(1) * timer(1)  // Get elapsed time only if timer is running
```

## Logical Expressions

### Comparison Operators

| Operator | Description | Example | Result |
|----------|-------------|---------|---------|
| `==` | Equal to | `5 == 5` | 1.0 (true) |
| `!=` | Not equal to | `5 != 3` | 1.0 (true) |
| `<` | Less than | `3 < 5` | 1.0 (true) |
| `>` | Greater than | `5 > 3` | 1.0 (true) |
| `<=` | Less than or equal | `3 <= 3` | 1.0 (true) |
| `>=` | Greater than or equal | `5 >= 3` | 1.0 (true) |

### Logical Operators

| Operator | Description | Example | Result |
|----------|-------------|---------|---------|
| `&&` | Logical AND | `1 && 1` | 1.0 (true) |
| `\|\|` | Logical OR | `0 \|\| 1` | 1.0 (true) |

### Logical Functions

| Function | Description | Usage | Example |
|----------|-------------|-------|---------|
| `and(a, b)` | Logical AND | `and(a, b)` | `and(1, 0)` returns 0.0 |
| `or(a, b)` | Logical OR | `or(a, b)` | `or(1, 0)` returns 1.0 |
| `not(a)` | Logical NOT | `not(a)` | `not(1)` returns 0.0 |
| `eq(a, b)` | Equal to | `eq(a, b)` | `eq(5, 5)` returns 1.0 |
| `ne(a, b)` | Not equal to | `ne(a, b)` | `ne(5, 3)` returns 1.0 |
| `gt(a, b)` | Greater than | `gt(a, b)` | `gt(5, 3)` returns 1.0 |
| `ge(a, b)` | Greater than or equal | `ge(a, b)` | `ge(5, 5)` returns 1.0 |
| `lt(a, b)` | Less than | `lt(a, b)` | `lt(3, 5)` returns 1.0 |
| `le(a, b)` | Less than or equal | `le(a, b)` | `le(3, 5)` returns 1.0 |
| `if(condition, true_value, false_value)` | Conditional | `if(condition, true_val, false_val)` | `if(1, 10, 20)` returns 10.0 |

### Logical Expression Examples

```javascript
// Comparison operators
30001 > 50                   // Check if channel 30001 is greater than 50
30001 == 30002               // Check if two channels are equal
30001 != 0                   // Check if channel 30001 is not zero

// Logical operators
(30001 > 50) && (30002 < 100)  // Both conditions must be true
(30001 > 50) || (30002 < 100)  // Either condition can be true

// Logical functions
and(30001 > 50, 30002 < 100)   // Same as && operator
or(30001 > 50, 30002 < 100)    // Same as || operator
not(30001 == 0)                 // Logical NOT
if(30001 > 50, 100, 0)         // Conditional: if true return 100, else 0

// Complex logical expressions
if((30001 > 50) && (30002 < 100), counter_inc(1), counter_reset(1))
```

## Combined Examples

### Counter with Timer
```javascript
// Start timer when counter reaches 10
if(counter(1) >= 10, timer_start(1), 0)

// Increment counter every 5 seconds
if(timer(1) >= 5, counter_inc(1) + timer_reset(1), counter(1))
```

### Logical Control
```javascript
// Reset counter if temperature is too high
if(30001 > 80, counter_reset(1), counter(1))

// Start timer if both conditions are met
if((30001 > 50) && (30002 < 100), timer_start(1), timer_stop(1))

// Conditional counter increment
if(30001 > 0, counter_inc(1), counter_dec(1))
```

### Production Monitoring
```javascript
// Count production cycles
if(30001 == 1, counter_inc(1), counter(1))

// Measure cycle time
if(30001 == 1, timer_start(1), if(30001 == 0, timer_stop(1), timer(1)))

// Quality control
if((30001 > 50) && (30001 < 100), counter_inc(1), if(30001 <= 50, counter_inc(2), counter_inc(3)))
```

## State Management

- **Counters**: Values persist across evaluations until explicitly reset
- **Timers**: Start/stop states and elapsed times persist across evaluations
- **Channel-specific**: Each channel can have its own counter and timer instances
- **Thread-safe**: All operations are thread-safe for concurrent access

## Error Handling

- Invalid function arguments return appropriate error messages
- Division by zero returns `NaN`
- Invalid expressions return `NaN`
- Missing channel values are handled gracefully

## Performance Notes

- State management is optimized for frequent evaluations
- Counters and timers use efficient data structures
- Logical operations are evaluated in proper precedence order
- Function calls are cached where possible

## Migration from Old Expressions

Existing math channel expressions will continue to work without modification. The new features are additive and backward compatible.

## Best Practices

1. **Use meaningful channel numbers** for named counters and timers
2. **Reset counters and timers** when appropriate to prevent overflow
3. **Use logical expressions** for conditional operations
4. **Combine functions** for complex control logic
5. **Test expressions** thoroughly before deploying in production
