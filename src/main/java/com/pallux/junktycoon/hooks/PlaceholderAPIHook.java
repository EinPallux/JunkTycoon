package com.pallux.junktycoon.hooks;

import com.pallux.junktycoon.JunkTycoon;
import com.pallux.junktycoon.placeholders.JunkTycoonExpansion;
import org.bukkit.plugin.java.JavaPlugin;

public class PlaceholderAPIHook {

    private final JunkTycoon plugin;
    private JunkTycoonExpansion expansion;
    private boolean enabled = false;

    public PlaceholderAPIHook(JunkTycoon plugin) {
        this.plugin = plugin;
    }

    public boolean setupPlaceholderAPI() {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            plugin.getLogger().info("PlaceholderAPI not found - Placeholder integration disabled");
            return false;
        }

        try {
            // Register our expansion
            expansion = new JunkTycoonExpansion(plugin);
            if (expansion.register()) {
                enabled = true;
                plugin.getLogger().info("PlaceholderAPI integration enabled successfully!");
                plugin.getLogger().info("Available placeholders:");
                plugin.getLogger().info("  %jt_prestige_level% - Current prestige level");
                plugin.getLogger().info("  %jt_trash_pick_level% - Current trash pick level");
                plugin.getLogger().info("  %jt_trash_pick_xp% - Current trash pick XP");
                plugin.getLogger().info("  %jt_trash_picked% - Total trash picked");
                plugin.getLogger().info("  %jt_money_earned% - Total money earned");
                plugin.getLogger().info("  And more! See plugin documentation for full list.");
                return true;
            } else {
                plugin.getLogger().warning("Failed to register PlaceholderAPI expansion");
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to setup PlaceholderAPI integration: " + e.getMessage());
            return false;
        }
    }

    public void shutdown() {
        if (enabled && expansion != null) {
            expansion.unregister();
            enabled = false;
            plugin.getLogger().info("PlaceholderAPI integration disabled");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public JunkTycoonExpansion getExpansion() {
        return expansion;
    }
}