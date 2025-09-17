package com.pallux.junktycoon.listeners;

import com.pallux.junktycoon.JunkTycoon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerJoinListener implements Listener {

    private final JunkTycoon plugin;

    public PlayerJoinListener(JunkTycoon plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load player data
        plugin.getPlayerDataManager().getPlayerData(player);

        // Give trash pick after a short delay to ensure inventory is loaded
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getTrashPickManager().giveTrashPick(player);
            }
        }.runTaskLater(plugin, 10L); // 0.5 second delay
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Save and unload player data
        plugin.getPlayerDataManager().unloadPlayerData(player.getUniqueId());
    }
}