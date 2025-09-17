package com.pallux.junktycoon.hooks;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerPointsHook {

    private final JavaPlugin plugin;
    private PlayerPointsAPI pointsAPI = null;
    private boolean enabled = false;

    public PlayerPointsHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setupPlayerPoints() {
        if (plugin.getServer().getPluginManager().getPlugin("PlayerPoints") == null) {
            plugin.getLogger().info("PlayerPoints not found - Point Finder perk will be disabled");
            return false;
        }

        try {
            pointsAPI = PlayerPoints.getInstance().getAPI();
            enabled = true;
            plugin.getLogger().info("PlayerPoints hook enabled successfully!");
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to hook into PlayerPoints: " + e.getMessage());
            return false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void givePoints(Player player, int amount) {
        if (enabled && pointsAPI != null) {
            pointsAPI.give(player.getUniqueId(), amount);
        }
    }

    public int getPoints(Player player) {
        if (enabled && pointsAPI != null) {
            return pointsAPI.look(player.getUniqueId());
        }
        return 0;
    }

    public boolean takePoints(Player player, int amount) {
        if (enabled && pointsAPI != null) {
            int currentPoints = pointsAPI.look(player.getUniqueId());
            if (currentPoints >= amount) {
                pointsAPI.take(player.getUniqueId(), amount);
                return true;
            }
        }
        return false;
    }
}