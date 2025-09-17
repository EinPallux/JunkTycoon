package com.pallux.junktycoon.managers;

import com.pallux.junktycoon.JunkTycoon;
import com.pallux.junktycoon.models.ActiveBooster;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class BoosterManager {

    private final JunkTycoon plugin;
    private final Map<String, ActiveBooster> activeBoosters = new HashMap<>();
    private final Map<String, BossBar> boosterBossBars = new HashMap<>();
    private final Map<String, BukkitTask> boosterTasks = new HashMap<>();

    public BoosterManager(JunkTycoon plugin) {
        this.plugin = plugin;
    }

    public boolean activateBooster(String boosterId, String boosterName, String type, double multiplier, int durationSeconds, Player activator) {
        // Check if booster type is already active
        if (isBoosterTypeActive(type)) {
            return false;
        }

        // Create active booster
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        ActiveBooster booster = new ActiveBooster(boosterId, boosterName, type, multiplier, endTime, activator.getName());
        activeBoosters.put(type, booster);

        // Create bossbar
        createBoosterBossBar(type, boosterName, multiplier, durationSeconds);

        // Schedule booster end task
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            endBooster(type);
        }, durationSeconds * 20L); // Convert to ticks

        boosterTasks.put(type, task);

        // Announce activation
        announceBoosterActivation(boosterName, activator.getName(), durationSeconds / 60);

        // Start bossbar update task
        startBossBarUpdateTask(type, durationSeconds);

        return true;
    }

    public void endBooster(String type) {
        ActiveBooster booster = activeBoosters.get(type);
        if (booster == null) return;

        // Remove active booster
        activeBoosters.remove(type);

        // Remove bossbar
        BossBar bossBar = boosterBossBars.remove(type);
        if (bossBar != null) {
            bossBar.removeAll();
        }

        // Cancel task
        BukkitTask task = boosterTasks.remove(type);
        if (task != null) {
            task.cancel();
        }

        // Announce end
        announceBoosterEnd(booster.getName());
    }

    private void createBoosterBossBar(String type, String boosterName, double multiplier, int duration) {
        FileConfiguration shopConfig = plugin.getConfigManager().getConfig("shop");

        String title = shopConfig.getString("bossbar." + type + "_booster.title", "&7Booster Active");
        title = title.replace("%multiplier%", String.valueOf(multiplier));
        title = title.replace("%time%", formatTime(duration));
        title = plugin.getConfigManager().formatText(title);

        BarColor color;
        try {
            color = BarColor.valueOf(shopConfig.getString("bossbar." + type + "_booster.color", "YELLOW"));
        } catch (IllegalArgumentException e) {
            color = BarColor.YELLOW;
        }

        BarStyle style;
        try {
            style = BarStyle.valueOf(shopConfig.getString("bossbar." + type + "_booster.style", "SEGMENTED_10"));
        } catch (IllegalArgumentException e) {
            style = BarStyle.SEGMENTED_10;
        }

        BossBar bossBar = Bukkit.createBossBar(title, color, style);
        bossBar.setProgress(1.0);

        // Add all online players to the bossbar
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }

        boosterBossBars.put(type, bossBar);
    }

    private void startBossBarUpdateTask(String type, int totalDuration) {
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                ActiveBooster booster = activeBoosters.get(type);
                if (booster == null) {
                    return;
                }

                BossBar bossBar = boosterBossBars.get(type);
                if (bossBar == null) {
                    return;
                }

                long currentTime = System.currentTimeMillis();
                long remainingTime = Math.max(0, booster.getEndTime() - currentTime);

                if (remainingTime <= 0) {
                    endBooster(type);
                    return;
                }

                // Update progress bar
                double progress = (double) remainingTime / (totalDuration * 1000L);
                bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));

                // Update title with remaining time
                FileConfiguration shopConfig = plugin.getConfigManager().getConfig("shop");
                String title = shopConfig.getString("bossbar." + type + "_booster.title", "&7Booster Active");
                title = title.replace("%multiplier%", String.valueOf(booster.getMultiplier()));
                title = title.replace("%time%", formatTime((int) (remainingTime / 1000)));
                title = plugin.getConfigManager().formatText(title);
                bossBar.setTitle(title);

                // Add new players to bossbar
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!bossBar.getPlayers().contains(player)) {
                        bossBar.addPlayer(player);
                    }
                }
            }
        }, 0L, 20L); // Update every second
    }

    private void announceBoosterActivation(String boosterName, String playerName, int minutes) {
        FileConfiguration shopConfig = plugin.getConfigManager().getConfig("shop");

        String message = shopConfig.getString("messages.booster_activated", "Booster activated!");
        message = message.replace("%player%", playerName);
        message = message.replace("%booster_name%", boosterName);
        message = message.replace("%duration%", String.valueOf(minutes));
        message = plugin.getConfigManager().formatText(message);

        Bukkit.broadcastMessage(message);
    }

    private void announceBoosterEnd(String boosterName) {
        FileConfiguration shopConfig = plugin.getConfigManager().getConfig("shop");

        String message = shopConfig.getString("messages.booster_ended", "Booster ended!");
        message = message.replace("%booster_name%", boosterName);
        message = plugin.getConfigManager().formatText(message);

        Bukkit.broadcastMessage(message);
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    public boolean isBoosterTypeActive(String type) {
        return activeBoosters.containsKey(type);
    }

    public double getBoosterMultiplier(String type) {
        ActiveBooster booster = activeBoosters.get(type);
        return booster != null ? booster.getMultiplier() : 1.0;
    }

    public ActiveBooster getActiveBooster(String type) {
        return activeBoosters.get(type);
    }

    public void reload() {
        // End all active boosters when reloading
        for (String type : new HashMap<>(activeBoosters).keySet()) {
            endBooster(type);
        }
    }

    public void shutdown() {
        // Clean shutdown - remove all bossbars and cancel tasks
        for (BossBar bossBar : boosterBossBars.values()) {
            bossBar.removeAll();
        }
        boosterBossBars.clear();

        for (BukkitTask task : boosterTasks.values()) {
            task.cancel();
        }
        boosterTasks.clear();

        activeBoosters.clear();
    }
}