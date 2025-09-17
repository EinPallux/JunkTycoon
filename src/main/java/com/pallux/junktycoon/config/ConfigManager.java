package com.pallux.junktycoon.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManager {

    private final JavaPlugin plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private final Map<String, File> configFiles = new HashMap<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        // Load main config
        loadConfig("config");

        // Load trash config
        loadConfig("trash");

        // Load messages config
        loadConfig("messages");

        // Load shop config
        loadConfig("shop");

        plugin.getLogger().info("All configuration files loaded successfully!");
    }

    private void loadConfig(String name) {
        File configFile = new File(plugin.getDataFolder(), name + ".yml");

        // Create config file if it doesn't exist
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource(name + ".yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Look for defaults in the jar
        InputStream defConfigStream = plugin.getResource(name + ".yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            config.setDefaults(defConfig);
        }

        configs.put(name, config);
        configFiles.put(name, configFile);
    }

    public FileConfiguration getConfig(String name) {
        return configs.get(name);
    }

    public void saveConfig(String name) {
        try {
            configs.get(name).save(configFiles.get(name));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + name + ".yml", e);
        }
    }

    public void reloadConfig(String name) {
        configFiles.put(name, new File(plugin.getDataFolder(), name + ".yml"));
        configs.put(name, YamlConfiguration.loadConfiguration(configFiles.get(name)));

        InputStream defConfigStream = plugin.getResource(name + ".yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            configs.get(name).setDefaults(defConfig);
        }
    }

    // Convenience methods
    public FileConfiguration getMainConfig() {
        return getConfig("config");
    }

    public FileConfiguration getTrashConfig() {
        return getConfig("trash");
    }

    public FileConfiguration getMessagesConfig() {
        return getConfig("messages");
    }

    public FileConfiguration getShopConfig() {
        return getConfig("shop");
    }

    // Message formatting method
    public String getMessage(String path) {
        String message = getMessagesConfig().getString(path, "Message not found: " + path);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getMessage(String path, String placeholder, String value) {
        String message = getMessage(path);
        return message.replace("%" + placeholder + "%", value);
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return message;
    }

    // Color formatting method
    public String formatText(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}