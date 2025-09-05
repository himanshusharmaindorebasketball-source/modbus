package com.example.modbus;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ChannelRepository {
    private static final File FILE = new File("channels.csv");
    private static final String HEADER = "channelNumber,channelAddress,dataType,deviceId,value,low,high,offset,maxDecimalDigits,colorR,colorG,colorB,channelMaths,unit,channelName";

    public synchronized static List<ChannelConfig> load() {
        List<ChannelConfig> list = new ArrayList<>();
        if (!FILE.exists()) return list;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(FILE), StandardCharsets.UTF_8))) {
            String line = br.readLine(); // header
            if (line == null) return list;
            while ((line = br.readLine()) != null) {
                String[] parts = parseCsvLine(line);
                if (parts.length < 15) continue;
                int i = 0;
                int channelNumber = toInt(parts[i++]);
                int channelAddress = toInt(parts[i++]);
                String dataType = parts[i++];
                int deviceId = toInt(parts[i++]);
                double value = toDouble(parts[i++]);
                double low = toDouble(parts[i++]);
                double high = toDouble(parts[i++]);
                double offset = toDouble(parts[i++]);
                int maxDecimalDigits = toInt(parts[i++]);
                int r = toInt(parts[i++]);
                int g = toInt(parts[i++]);
                int b = toInt(parts[i++]);
                String channelMaths = parts[i++];
                String unit = parts[i++];
                String channelName = parts[i++];
                list.add(new ChannelConfig(channelNumber, channelAddress, dataType, deviceId, value, low, high, offset, maxDecimalDigits, new Color(r, g, b), channelMaths, unit, channelName));
            }
        } catch (IOException ignored) {}
        return list;
    }

    public synchronized static void save(List<ChannelConfig> configs) {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FILE), StandardCharsets.UTF_8))) {
            bw.write(HEADER);
            bw.newLine();
            for (ChannelConfig c : configs) {
                StringBuilder sb = new StringBuilder();
                sb.append(c.getChannelNumber()).append(',')
                  .append(c.getChannelAddress()).append(',')
                  .append(escape(c.getDataType())).append(',')
                  .append(c.getDeviceId()).append(',')
                  .append(c.getValue()).append(',')
                  .append(c.getLow()).append(',')
                  .append(c.getHigh()).append(',')
                  .append(c.getOffset()).append(',')
                  .append(c.getMaxDecimalDigits()).append(',')
                  .append(c.getChannelColor().getRed()).append(',')
                  .append(c.getChannelColor().getGreen()).append(',')
                  .append(c.getChannelColor().getBlue()).append(',')
                  .append(escape(c.getChannelMaths())).append(',')
                  .append(escape(c.getUnit())).append(',')
                  .append(escape(c.getChannelName()));
                bw.write(sb.toString());
                bw.newLine();
            }
        } catch (IOException ignored) {}
    }

    private static int toInt(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; } }
    private static double toDouble(String s) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0.0; } }

    private static String escape(String s) {
        if (s == null) return "";
        String out = s.replace("\\", "\\\\").replace("\"", "\\\"");
        if (out.contains(",") || out.contains("\"") || out.contains("\n")) {
            out = '"' + out + '"';
        }
        return out;
    }

    private static String[] parseCsvLine(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder curr = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { curr.append('"'); i++; }
                    else inQuotes = false;
                } else curr.append(ch);
            } else {
                if (ch == '"') inQuotes = true;
                else if (ch == ',') { parts.add(curr.toString()); curr.setLength(0); }
                else curr.append(ch);
            }
        }
        parts.add(curr.toString());
        return parts.toArray(new String[0]);
    }
}
