package com.example.modbus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.reflect.TypeToken;

import java.awt.Color;
import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Manager for individual channel display configurations
 */
public class ChannelDisplayConfigManager {
    private static final String CONFIG_FILE = "channel_display_configs.json";
    
    // Custom Color adapter for Gson
    private static final TypeAdapter<Color> COLOR_ADAPTER = new TypeAdapter<Color>() {
        @Override
        public void write(JsonWriter out, Color value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.beginObject();
                out.name("r").value(value.getRed());
                out.name("g").value(value.getGreen());
                out.name("b").value(value.getBlue());
                out.name("a").value(value.getAlpha());
                out.endObject();
            }
        }

        @Override
        public Color read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            
            in.beginObject();
            int r = 0, g = 0, b = 0, a = 255;
            while (in.hasNext()) {
                String name = in.nextName();
                switch (name) {
                    case "r": r = in.nextInt(); break;
                    case "g": g = in.nextInt(); break;
                    case "b": b = in.nextInt(); break;
                    case "a": a = in.nextInt(); break;
                    default: in.skipValue(); break;
                }
            }
            in.endObject();
            return new Color(r, g, b, a);
        }
    };
    
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Color.class, COLOR_ADAPTER)
            .create();
    private static Map<String, ChannelDisplayConfig> channelConfigs = new HashMap<>();
    
    /**
     * Get display configuration for a specific channel
     */
    public static ChannelDisplayConfig getConfig(String channelName) {
        return channelConfigs.getOrDefault(channelName, new ChannelDisplayConfig());
    }
    
    /**
     * Set display configuration for a specific channel
     */
    public static void setConfig(String channelName, ChannelDisplayConfig config) {
        channelConfigs.put(channelName, config);
    }
    
    /**
     * Save all configurations to file
     */
    public static void saveConfigs() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(channelConfigs, writer);
            System.out.println("Channel display configurations saved to " + CONFIG_FILE);
        } catch (IOException e) {
            System.err.println("Error saving channel display configurations: " + e.getMessage());
        }
    }
    
    /**
     * Load all configurations from file
     */
    public static void loadConfigs() {
        try {
            File file = new File(CONFIG_FILE);
            if (!file.exists()) {
                System.out.println("Channel display config file not found, using defaults");
                channelConfigs = new HashMap<>();
                return;
            }
            
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                Type type = new TypeToken<Map<String, ChannelDisplayConfig>>(){}.getType();
                channelConfigs = gson.fromJson(reader, type);
                if (channelConfigs == null) {
                    channelConfigs = new HashMap<>();
                }
                System.out.println("Channel display configurations loaded from " + CONFIG_FILE);
            } catch (IOException e) {
                System.err.println("Error loading channel display configurations: " + e.getMessage());
                channelConfigs = new HashMap<>();
            }
        } catch (Exception e) {
            System.err.println("Unexpected error in loadConfigs: " + e.getMessage());
            e.printStackTrace();
            channelConfigs = new HashMap<>();
        }
    }
    
    /**
     * Get all available channel names
     */
    public static String[] getAvailableChannels() {
        return channelConfigs.keySet().toArray(new String[0]);
    }
    
    /**
     * Remove configuration for a specific channel
     */
    public static void removeConfig(String channelName) {
        channelConfigs.remove(channelName);
    }
    
    /**
     * Clear all configurations
     */
    public static void clearAllConfigs() {
        channelConfigs.clear();
    }
    
    /**
     * Check if a channel has custom configuration
     */
    public static boolean hasCustomConfig(String channelName) {
        return channelConfigs.containsKey(channelName);
    }
}
