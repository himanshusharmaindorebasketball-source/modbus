package com.example.modbus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Energy Data Logger for monitoring and logging energy consumption data
 */
public class EnergyDataLogger {
    private static final String LOG_DIRECTORY = "energy_logs";
    private static final String CONFIG_FILE = "datalogger_config.json";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    private static EnergyDataLogger instance;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, Object> currentData = new ConcurrentHashMap<>();
    private final List<EnergyDataPoint> dataBuffer = new ArrayList<>();
    private final Object bufferLock = new Object();
    
    private DataLoggerConfig config;
    private boolean isLogging = false;
    private String currentLogFile;
    private PrintWriter currentWriter;
    
    private EnergyDataLogger() {
        loadConfig();
        createLogDirectory();
    }
    
    public static synchronized EnergyDataLogger getInstance() {
        if (instance == null) {
            instance = new EnergyDataLogger();
        }
        return instance;
    }
    
    /**
     * Data point structure for energy monitoring
     */
    public static class EnergyDataPoint {
        private String timestamp;
        private String date;
        private String time;
        private Map<String, Object> data;
        
        public EnergyDataPoint() {
            this.timestamp = LocalDateTime.now().toString();
            this.date = LocalDateTime.now().format(DATE_FORMAT);
            this.time = LocalDateTime.now().format(TIME_FORMAT);
            this.data = new HashMap<>();
        }
        
        public EnergyDataPoint(Map<String, Object> data) {
            this();
            this.data = new HashMap<>(data);
        }
        
        // Getters and setters
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
        
        public void addData(String key, Object value) {
            this.data.put(key, value);
        }
        
        public Object getDataValue(String key) {
            return this.data.get(key);
        }
    }
    
    /**
     * Configuration for data logger
     */
    public static class DataLoggerConfig {
        private boolean enabled = true;
        private int logIntervalSeconds = 1; // Log every 1 second
        private int bufferSize = 100; // Buffer size before writing to file
        private boolean logToFile = true;
        private boolean logToDatabase = false;
        private String logFormat = "JSON"; // JSON, CSV, or BOTH
        private boolean includeCalculatedValues = true;
        private List<String> channelsToLog = new ArrayList<>();
        private String databaseUrl = "";
        private String databaseUsername = "";
        private String databasePassword = "";
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public int getLogIntervalSeconds() { return logIntervalSeconds; }
        public void setLogIntervalSeconds(int logIntervalSeconds) { this.logIntervalSeconds = logIntervalSeconds; }
        
        public int getBufferSize() { return bufferSize; }
        public void setBufferSize(int bufferSize) { this.bufferSize = bufferSize; }
        
        public boolean isLogToFile() { return logToFile; }
        public void setLogToFile(boolean logToFile) { this.logToFile = logToFile; }
        
        public boolean isLogToDatabase() { return logToDatabase; }
        public void setLogToDatabase(boolean logToDatabase) { this.logToDatabase = logToDatabase; }
        
        public String getLogFormat() { return logFormat; }
        public void setLogFormat(String logFormat) { this.logFormat = logFormat; }
        
        public boolean isIncludeCalculatedValues() { return includeCalculatedValues; }
        public void setIncludeCalculatedValues(boolean includeCalculatedValues) { this.includeCalculatedValues = includeCalculatedValues; }
        
        public List<String> getChannelsToLog() { return channelsToLog; }
        public void setChannelsToLog(List<String> channelsToLog) { this.channelsToLog = channelsToLog; }
        
        public String getDatabaseUrl() { return databaseUrl; }
        public void setDatabaseUrl(String databaseUrl) { this.databaseUrl = databaseUrl; }
        
        public String getDatabaseUsername() { return databaseUsername; }
        public void setDatabaseUsername(String databaseUsername) { this.databaseUsername = databaseUsername; }
        
        public String getDatabasePassword() { return databasePassword; }
        public void setDatabasePassword(String databasePassword) { this.databasePassword = databasePassword; }
    }
    
    /**
     * Load configuration from file
     */
    private void loadConfig() {
        try {
            File file = new File(CONFIG_FILE);
            if (!file.exists()) {
                config = new DataLoggerConfig();
                saveConfig();
                return;
            }
            
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                Gson gson = new Gson();
                config = gson.fromJson(reader, DataLoggerConfig.class);
                if (config == null) {
                    config = new DataLoggerConfig();
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading data logger config: " + e.getMessage());
            config = new DataLoggerConfig();
        }
    }
    
    /**
     * Reload configuration from file (public method for external use)
     */
    public void reloadConfig() {
        System.out.println("DEBUG: Reloading EnergyDataLogger configuration from file");
        loadConfig();
        System.out.println("DEBUG: Loaded " + config.getChannelsToLog().size() + " channels from configuration: " + config.getChannelsToLog());
    }
    
    /**
     * Save configuration to file
     */
    public void saveConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(config, writer);
        } catch (IOException e) {
            System.err.println("Error saving data logger config: " + e.getMessage());
        }
    }
    
    /**
     * Create log directory if it doesn't exist
     */
    private void createLogDirectory() {
        File dir = new File(LOG_DIRECTORY);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    /**
     * Start data logging
     */
    public void startLogging() {
        if (isLogging || !config.isEnabled()) {
            return;
        }
        
        isLogging = true;
        currentLogFile = LOG_DIRECTORY + "/energy_data_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".json";
        
        // Schedule periodic logging
        scheduler.scheduleAtFixedRate(this::logCurrentData, 0, config.getLogIntervalSeconds(), TimeUnit.SECONDS);
        
        // Schedule buffer flush
        scheduler.scheduleAtFixedRate(this::flushBuffer, 5, 5, TimeUnit.SECONDS);
        
        System.out.println("Energy data logging started. Log file: " + currentLogFile);
    }
    
    /**
     * Stop data logging
     */
    public void stopLogging() {
        if (!isLogging) {
            return;
        }
        
        isLogging = false;
        flushBuffer();
        closeCurrentWriter();
        System.out.println("Energy data logging stopped.");
    }
    
    /**
     * Update current data values
     */
    public void updateData(String channelName, Object value) {
        currentData.put(channelName, value);
    }
    
    /**
     * Update multiple data values at once
     */
    public void updateData(Map<String, Object> data) {
        currentData.putAll(data);
    }
    
    /**
     * Log current data to buffer
     */
    private void logCurrentData() {
        if (!isLogging || currentData.isEmpty()) {
            return;
        }
        
        // Create a copy of current data
        Map<String, Object> dataCopy = new HashMap<>(currentData);
        
        // Add calculated values if enabled
        if (config.isIncludeCalculatedValues()) {
            addCalculatedValues(dataCopy);
        }
        
        // Filter channels if specific channels are configured
        if (!config.getChannelsToLog().isEmpty()) {
            System.out.println("DEBUG: Filtering channels. Selected channels: " + config.getChannelsToLog());
            System.out.println("DEBUG: Available data keys: " + dataCopy.keySet());
            dataCopy = filterChannels(dataCopy);
            System.out.println("DEBUG: Filtered data keys: " + dataCopy.keySet());
        }
        
        EnergyDataPoint dataPoint = new EnergyDataPoint(dataCopy);
        
        synchronized (bufferLock) {
            dataBuffer.add(dataPoint);
            
            // Flush buffer if it reaches the configured size
            if (dataBuffer.size() >= config.getBufferSize()) {
                flushBuffer();
            }
        }
    }
    
    /**
     * Add calculated energy values
     */
    private void addCalculatedValues(Map<String, Object> data) {
        try {
            // Calculate total power if individual phase powers are available
            Double powerA = getDoubleValue(data, "Power_A");
            Double powerB = getDoubleValue(data, "Power_B");
            Double powerC = getDoubleValue(data, "Power_C");
            
            if (powerA != null && powerB != null && powerC != null) {
                data.put("Total_Power", powerA + powerB + powerC);
            }
            
            // Calculate average voltage
            Double voltageRN = getDoubleValue(data, "Voltage R-N");
            Double voltageYN = getDoubleValue(data, "Voltage Y-N");
            Double voltageBN = getDoubleValue(data, "Voltage B-N");
            
            if (voltageRN != null && voltageYN != null && voltageBN != null) {
                data.put("Average_Voltage", (voltageRN + voltageYN + voltageBN) / 3.0);
            }
            
            // Calculate average current
            Double currentA = getDoubleValue(data, "Current_A");
            Double currentB = getDoubleValue(data, "Current_B");
            Double currentC = getDoubleValue(data, "Current_C");
            
            if (currentA != null && currentB != null && currentC != null) {
                data.put("Average_Current", (currentA + currentB + currentC) / 3.0);
            }
            
            // Calculate power factor if available
            Double activePower = getDoubleValue(data, "Total_Power");
            Double apparentPower = getDoubleValue(data, "Apparent_Power");
            
            if (activePower != null && apparentPower != null && apparentPower != 0) {
                data.put("Power_Factor", activePower / apparentPower);
            }
            
        } catch (Exception e) {
            System.err.println("Error calculating energy values: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to safely get double values
     */
    private Double getDoubleValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * Filter data to only include specified channels
     */
    private Map<String, Object> filterChannels(Map<String, Object> data) {
        Map<String, Object> filtered = new HashMap<>();
        for (String selectedChannel : config.getChannelsToLog()) {
            // Try exact match first
            if (data.containsKey(selectedChannel)) {
                filtered.put(selectedChannel, data.get(selectedChannel));
            } else {
                // Try to find math channel without "(Math)" suffix
                String baseChannelName = selectedChannel.replace(" (Math)", "");
                if (data.containsKey(baseChannelName)) {
                    filtered.put(selectedChannel, data.get(baseChannelName));
                } else {
                    // Try to find math channel with "(Math)" suffix
                    String mathChannelName = baseChannelName + " (Math)";
                    if (data.containsKey(mathChannelName)) {
                        filtered.put(selectedChannel, data.get(mathChannelName));
                    }
                }
            }
        }
        return filtered;
    }
    
    /**
     * Flush data buffer to file
     */
    private void flushBuffer() {
        synchronized (bufferLock) {
            if (dataBuffer.isEmpty()) {
                return;
            }
            
            if (config.isLogToFile()) {
                writeToFile();
            }
            
            if (config.isLogToDatabase()) {
                writeToDatabase();
            }
            
            dataBuffer.clear();
        }
    }
    
    /**
     * Write data to file
     */
    private void writeToFile() {
        try {
            if (currentWriter == null) {
                currentWriter = new PrintWriter(new FileWriter(currentLogFile, true));
            }
            
            for (EnergyDataPoint dataPoint : dataBuffer) {
                if ("JSON".equals(config.getLogFormat()) || "BOTH".equals(config.getLogFormat())) {
                    Gson gson = new Gson();
                    currentWriter.println(gson.toJson(dataPoint));
                }
                
                if ("CSV".equals(config.getLogFormat()) || "BOTH".equals(config.getLogFormat())) {
                    writeCSVLine(dataPoint);
                }
            }
            
            currentWriter.flush();
            
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }
    
    /**
     * Write CSV line
     */
    private void writeCSVLine(EnergyDataPoint dataPoint) {
        StringBuilder csvLine = new StringBuilder();
        csvLine.append(dataPoint.getTimestamp()).append(",");
        csvLine.append(dataPoint.getDate()).append(",");
        csvLine.append(dataPoint.getTime()).append(",");
        
        for (Map.Entry<String, Object> entry : dataPoint.getData().entrySet()) {
            csvLine.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
        }
        
        // Remove trailing comma
        if (csvLine.length() > 0) {
            csvLine.setLength(csvLine.length() - 1);
        }
        
        currentWriter.println(csvLine.toString());
    }
    
    /**
     * Write data to database (placeholder for future implementation)
     */
    private void writeToDatabase() {
        // TODO: Implement database logging
        System.out.println("Database logging not yet implemented. " + dataBuffer.size() + " records would be written.");
    }
    
    /**
     * Close current writer
     */
    private void closeCurrentWriter() {
        if (currentWriter != null) {
            currentWriter.close();
            currentWriter = null;
        }
    }
    
    /**
     * Get current configuration
     */
    public DataLoggerConfig getConfig() {
        return config;
    }
    
    /**
     * Check if logging is active
     */
    public boolean isLogging() {
        return isLogging;
    }
    
    /**
     * Get current log file path
     */
    public String getCurrentLogFile() {
        return currentLogFile;
    }
    
    /**
     * Shutdown the logger
     */
    public void shutdown() {
        stopLogging();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
    
    /**
     * Export data to CSV file
     */
    public void exportToCSV(String outputFile, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            File logDir = new File(LOG_DIRECTORY);
            if (!logDir.exists()) {
                System.err.println("Log directory does not exist: " + LOG_DIRECTORY);
                return;
            }
            
            // Get all log files (both .json and other formats)
            File[] logFiles = logDir.listFiles((dir, name) -> name.endsWith(".json") || name.contains("energy_data_"));
            if (logFiles == null || logFiles.length == 0) {
                System.err.println("No log files found in directory: " + LOG_DIRECTORY);
                return;
            }
            
            System.out.println("Starting CSV export of " + logFiles.length + " files to: " + outputFile);
            
            // Create CSV writer
            try (PrintWriter csvWriter = new PrintWriter(new FileWriter(outputFile))) {
                Set<String> allColumns = new LinkedHashSet<>();
                
                // First pass: collect all unique column names
                System.out.println("Collecting column names from log files...");
                for (File logFile : logFiles) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.trim().isEmpty()) continue;
                            
                            try {
                                Gson gson = new Gson();
                                EnergyDataPoint dataPoint = gson.fromJson(line, EnergyDataPoint.class);
                                if (dataPoint != null && dataPoint.getData() != null) {
                                    allColumns.addAll(dataPoint.getData().keySet());
                                }
                            } catch (Exception e) {
                                System.err.println("Warning: Skipping invalid JSON line in " + logFile.getName() + ": " + e.getMessage());
                                continue;
                            }
                        }
                    }
                }
                
                System.out.println("Found " + allColumns.size() + " unique columns");
                
                // Write CSV header with proper escaping
                csvWriter.print(escapeCSVValue("Timestamp"));
                csvWriter.print("," + escapeCSVValue("Date"));
                csvWriter.print("," + escapeCSVValue("Time"));
                for (String column : allColumns) {
                    csvWriter.print("," + escapeCSVValue(column));
                }
                csvWriter.println();
                
                // Second pass: write data
                System.out.println("Writing data to CSV...");
                int totalRecords = 0;
                for (File logFile : logFiles) {
                    System.out.println("Processing file: " + logFile.getName());
                    try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                        String line;
                        int fileRecords = 0;
                        while ((line = reader.readLine()) != null) {
                            if (line.trim().isEmpty()) continue;
                            
                            try {
                                Gson gson = new Gson();
                                EnergyDataPoint dataPoint = gson.fromJson(line, EnergyDataPoint.class);
                                if (dataPoint != null && dataPoint.getData() != null) {
                                    // Write timestamp, date, time with proper escaping
                                    csvWriter.print(escapeCSVValue(dataPoint.getTimestamp()));
                                    csvWriter.print("," + escapeCSVValue(dataPoint.getDate()));
                                    csvWriter.print("," + escapeCSVValue(dataPoint.getTime()));
                                    
                                    // Write data values in the same order as header
                                    for (String column : allColumns) {
                                        Object value = dataPoint.getData().get(column);
                                        csvWriter.print("," + escapeCSVValue(value != null ? value.toString() : ""));
                                    }
                                    csvWriter.println();
                                    fileRecords++;
                                    totalRecords++;
                                }
                            } catch (Exception e) {
                                System.err.println("Warning: Skipping invalid JSON line in " + logFile.getName() + ": " + e.getMessage());
                                continue;
                            }
                        }
                        System.out.println("Processed " + fileRecords + " records from " + logFile.getName());
                    }
                }
                
                System.out.println("CSV export completed successfully: " + outputFile + " (Total records: " + totalRecords + ")");
                
            } catch (IOException e) {
                System.err.println("Error writing CSV file: " + e.getMessage());
                throw e;
            }
            
        } catch (Exception e) {
            System.err.println("Error during CSV export: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to export CSV", e);
        }
    }
    
    /**
     * Export specific log file to CSV
     */
    public void exportLogFileToCSV(String logFileName, String outputFile) {
        try {
            System.out.println("DEBUG: exportLogFileToCSV called with logFileName=" + logFileName + ", outputFile=" + outputFile);
            
            File logFile = new File(LOG_DIRECTORY + "/" + logFileName);
            System.out.println("DEBUG: Looking for log file at: " + logFile.getAbsolutePath());
            
            if (!logFile.exists()) {
                System.err.println("DEBUG: Log file does not exist: " + logFile.getAbsolutePath());
                throw new FileNotFoundException("Log file not found: " + logFileName);
            }
            
            System.out.println("DEBUG: Log file exists, size: " + logFile.length() + " bytes");
            System.out.println("Starting CSV export of " + logFileName + " to: " + outputFile);
            
            // Create CSV writer
            try (PrintWriter csvWriter = new PrintWriter(new FileWriter(outputFile));
                 BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                
                String firstLine = reader.readLine();
                if (firstLine == null) {
                    System.out.println("DEBUG: Log file is empty");
                    return;
                }
                
                // Check if the file is already in CSV format or JSON format
                boolean isCSVFormat = !firstLine.trim().startsWith("{");
                
                if (isCSVFormat) {
                    System.out.println("DEBUG: Detected CSV format log file");
                    exportCSVFormatFile(reader, csvWriter, firstLine);
                } else {
                    System.out.println("DEBUG: Detected JSON format log file");
                    exportJSONFormatFile(reader, csvWriter, firstLine);
                }
                
                System.out.println("Log file exported to CSV successfully: " + outputFile);
                
            } catch (IOException e) {
                System.err.println("Error writing CSV file: " + e.getMessage());
                throw e;
            }
            
        } catch (Exception e) {
            System.err.println("Error during log file export: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to export log file to CSV", e);
        }
    }
    
    /**
     * Export CSV format log file
     */
    private void exportCSVFormatFile(BufferedReader reader, PrintWriter csvWriter, String firstLine) throws IOException {
        Set<String> allColumns = new LinkedHashSet<>();
        List<String[]> allRows = new ArrayList<>();
        
        // Process first line
        String[] firstRow = parseCSVLine(firstLine);
        if (firstRow.length >= 4) {
            allRows.add(firstRow);
            // Extract column names from data part (after timestamp, date, time)
            for (int i = 3; i < firstRow.length; i++) {
                String[] keyValue = firstRow[i].split("=", 2);
                if (keyValue.length == 2) {
                    allColumns.add(keyValue[0]);
                }
            }
        }
        
        // Process remaining lines
        String line;
        int lineCount = 1;
        while ((line = reader.readLine()) != null) {
            lineCount++;
            if (lineCount % 100 == 0) {
                System.out.println("DEBUG: Processed " + lineCount + " lines...");
            }
            if (line.trim().isEmpty()) continue;
            
            String[] row = parseCSVLine(line);
            if (row.length >= 4) {
                allRows.add(row);
                // Extract column names from data part
                for (int i = 3; i < row.length; i++) {
                    String[] keyValue = row[i].split("=", 2);
                    if (keyValue.length == 2) {
                        allColumns.add(keyValue[0]);
                    }
                }
            }
        }
        
        System.out.println("DEBUG: Found " + allColumns.size() + " unique columns and " + allRows.size() + " valid records");
        
        // Write CSV header
        csvWriter.print(escapeCSVValue("Timestamp"));
        csvWriter.print("," + escapeCSVValue("Date"));
        csvWriter.print("," + escapeCSVValue("Time"));
        for (String column : allColumns) {
            csvWriter.print("," + escapeCSVValue(column));
        }
        csvWriter.println();
        
        // Write data rows
        System.out.println("Writing data to CSV...");
        for (String[] row : allRows) {
            if (row.length >= 3) {
                // Write timestamp, date, time
                csvWriter.print(escapeCSVValue(row[0]));
                csvWriter.print("," + escapeCSVValue(row[1]));
                csvWriter.print("," + escapeCSVValue(row[2]));
                
                // Write data values in the same order as header
                Map<String, String> dataMap = new HashMap<>();
                for (int i = 3; i < row.length; i++) {
                    String[] keyValue = row[i].split("=", 2);
                    if (keyValue.length == 2) {
                        dataMap.put(keyValue[0], keyValue[1]);
                    }
                }
                
                for (String column : allColumns) {
                    String value = dataMap.get(column);
                    csvWriter.print("," + escapeCSVValue(value != null ? value : ""));
                }
                csvWriter.println();
            }
        }
    }
    
    /**
     * Export JSON format log file
     */
    private void exportJSONFormatFile(BufferedReader reader, PrintWriter csvWriter, String firstLine) throws IOException {
        Set<String> allColumns = new LinkedHashSet<>();
        List<EnergyDataPoint> dataPoints = new ArrayList<>();
        
        // Process first line
        try {
            Gson gson = new Gson();
            EnergyDataPoint dataPoint = gson.fromJson(firstLine, EnergyDataPoint.class);
            if (dataPoint != null && dataPoint.getData() != null) {
                dataPoints.add(dataPoint);
                allColumns.addAll(dataPoint.getData().keySet());
            }
        } catch (Exception e) {
            System.err.println("Warning: Skipping invalid JSON line 1: " + e.getMessage());
        }
        
        // Process remaining lines
        String line;
        int lineCount = 1;
        while ((line = reader.readLine()) != null) {
            lineCount++;
            if (lineCount % 100 == 0) {
                System.out.println("DEBUG: Processed " + lineCount + " lines...");
            }
            if (line.trim().isEmpty()) continue;
            
            try {
                Gson gson = new Gson();
                EnergyDataPoint dataPoint = gson.fromJson(line, EnergyDataPoint.class);
                if (dataPoint != null && dataPoint.getData() != null) {
                    dataPoints.add(dataPoint);
                    allColumns.addAll(dataPoint.getData().keySet());
                }
            } catch (Exception e) {
                System.err.println("Warning: Skipping invalid JSON line " + lineCount + ": " + e.getMessage());
                continue;
            }
        }
        
        System.out.println("DEBUG: Found " + allColumns.size() + " unique columns and " + dataPoints.size() + " valid records");
        
        // Write CSV header with proper escaping
        csvWriter.print(escapeCSVValue("Timestamp"));
        csvWriter.print("," + escapeCSVValue("Date"));
        csvWriter.print("," + escapeCSVValue("Time"));
        for (String column : allColumns) {
            csvWriter.print("," + escapeCSVValue(column));
        }
        csvWriter.println();
        
        // Write data rows
        System.out.println("Writing data to CSV...");
        for (EnergyDataPoint dataPoint : dataPoints) {
            // Write timestamp, date, time with proper escaping
            csvWriter.print(escapeCSVValue(dataPoint.getTimestamp()));
            csvWriter.print("," + escapeCSVValue(dataPoint.getDate()));
            csvWriter.print("," + escapeCSVValue(dataPoint.getTime()));
            
            // Write data values in the same order as header
            for (String column : allColumns) {
                Object value = dataPoint.getData().get(column);
                csvWriter.print("," + escapeCSVValue(value != null ? value.toString() : ""));
            }
            csvWriter.println();
        }
    }
    
    /**
     * Parse CSV line (simple comma-separated parsing)
     */
    private String[] parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        fields.add(currentField.toString());
        
        return fields.toArray(new String[0]);
    }
    
    /**
     * Get list of available log files
     */
    public List<String> getLogFiles() {
        List<String> logFiles = new ArrayList<>();
        File logDir = new File(LOG_DIRECTORY);
        if (logDir.exists()) {
            File[] files = logDir.listFiles((dir, name) -> name.endsWith(".json") || name.contains("energy_data_"));
            if (files != null) {
                for (File file : files) {
                    logFiles.add(file.getName());
                }
            }
        }
        return logFiles;
    }
    

    /**
     * Escape CSV values to handle commas, quotes, and newlines properly
     */
    private String escapeCSVValue(String value) {
        if (value == null) {
            return "";
        }
        
        // If the value contains comma, quote, or newline, wrap it in quotes and escape internal quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        
        return value;
    }
}
