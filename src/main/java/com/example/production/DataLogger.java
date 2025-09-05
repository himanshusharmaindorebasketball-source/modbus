package com.example.production;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DataLogger {
    private final Path filePath;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DataLogger() {
        this("production_data.csv");
    }

    public DataLogger(String filename) {
        this.filePath = Path.of(filename);
        ensureHeader();
    }

    private void ensureHeader() {
        if (!Files.exists(filePath)) {
            try {
                Files.writeString(filePath, "timestamp,channel,register,value\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE);
            } catch (IOException ignored) {}
        }
    }

    public synchronized void appendSample(String channelName, int register, double value) {
        String line = String.format("%s,%s,%d,%s\n", LocalDateTime.now().format(formatter), sanitize(channelName), register, Double.toString(value));
        try {
            Files.writeString(filePath, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }

    public synchronized List<String[]> readAll() {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] parts = line.split(",");
                rows.add(parts);
            }
        } catch (IOException ignored) {}
        return rows;
    }

    private String sanitize(String s) {
        return s == null ? "" : s.replace(",", " ").trim();
    }

    public Path getFilePath() {
        return filePath;
    }
}

