package com.pallux.junktycoon.managers;

import com.pallux.junktycoon.JunkTycoon;
import com.pallux.junktycoon.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class PrestigeManager {

    private final JunkTycoon plugin;

    public PrestigeManager(JunkTycoon plugin) {
        this.plugin = plugin;
    }

    public boolean canPrestige(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        int currentPrestige = playerData.getPrestigeLevel();
        int nextPrestige = currentPrestige + 1;

        // Check if already at max prestige
        if (isAtMaxPrestige(currentPrestige)) {
            return false;
        }

        // Check requirements for next prestige level
        return meetsPrestigeRequirements(player, nextPrestige);
    }

    public boolean meetsPrestigeRequirements(Player player, int prestigeLevel) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        FileConfiguration prestigeConfig = plugin.getConfigManager().getConfig("prestige");

        String requirementsPath = "requirements.prestige_" + prestigeLevel;

        if (!prestigeConfig.contains(requirementsPath)) {
            return false;
        }

        // Check trash pick tier requirement
        String requiredTier = prestigeConfig.getString(requirementsPath + ".trash_pick_tier");
        String currentTier = playerData.getTrashPickTier();

        if (!currentTier.equals(requiredTier)) {
            return false;
        }

        // Check minimum level requirement
        int requiredLevel = prestigeConfig.getInt(requirementsPath + ".minimum_level");
        int currentLevel = playerData.getTrashPickLevel();

        return currentLevel >= requiredLevel;
    }

    public boolean performPrestige(Player player) {
        if (!canPrestige(player)) {
            return false;
        }

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        int newPrestigeLevel = playerData.getPrestigeLevel() + 1;

        // Reset player progression (but keep statistics and prestige level)
        resetProgressionForPrestige(playerData);

        // Set new prestige level
        playerData.setPrestigeLevel(newPrestigeLevel);

        // Save data
        plugin.getPlayerDataManager().savePlayerData(player);

        // Give new starter trash pick
        plugin.getTrashPickManager().giveTrashPick(player);

        // Show prestige effects
        showPrestigeEffects(player, newPrestigeLevel);

        // Announce prestige
        announcePrestige(player, newPrestigeLevel);

        return true;
    }

    private void resetProgressionForPrestige(PlayerData playerData) {
        // Reset trash pick progression (but keep statistics and prestige)
        playerData.setTrashPickTier("starter");
        playerData.setTrashPickLevel(1);
        playerData.setTrashPickXP(0);
        playerData.setLastPickTime(0);

        // Reset all perk levels
        playerData.setCooldownPerkLevel(0);
        playerData.setMultiplierPerkLevel(0);
        playerData.setRarityPerkLevel(0);
        playerData.setPointFinderPerkLevel(0);
        playerData.setXpMultiplierPerkLevel(0);

        // Keep statistics and prestige level intact
    }

    private void showPrestigeEffects(Player player, int prestigeLevel) {
        FileConfiguration prestigeConfig = plugin.getConfigManager().getConfig("prestige");

        // Show title
        String title = plugin.getConfigManager().formatText(prestigeConfig.getString("messages.prestige_success", "PRESTIGE UP!"));
        String subtitle = prestigeConfig.getString("messages.prestige_success_subtitle", "You are now Prestige %level%!");
        subtitle = subtitle.replace("%level%", String.valueOf(prestigeLevel));
        subtitle = plugin.getConfigManager().formatText(subtitle);

        player.sendTitle(title, subtitle, 10, 60, 10);

        // Play sound effects
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.2f);

        // Send chat message
        String prefix = plugin.getConfigManager().getMessage("general.prefix");
        player.sendMessage("");
        player.sendMessage(prefix + title);
        player.sendMessage("§7You are now " + getPrestigeLevelName(prestigeLevel) + "§7!");
        player.sendMessage("§7New Bonuses:");
        player.sendMessage("§7• Money Bonus: §a+" + getMoneyBonusPercentage(prestigeLevel) + "%");
        if (prestigeLevel >= getXPMultiplierStartLevel()) {
            player.sendMessage("§7• XP Bonus: §b+" + getXPBonus(prestigeLevel) + " flat");
        }
        player.sendMessage("");
    }

    private void announcePrestige(Player player, int prestigeLevel) {
        FileConfiguration prestigeConfig = plugin.getConfigManager().getConfig("prestige");

        String announcement = prestigeConfig.getString("messages.prestige_announcement", "Player has prestiged!");
        announcement = announcement.replace("%player%", player.getName());
        announcement = announcement.replace("%level%", String.valueOf(prestigeLevel));
        announcement = announcement.replace("%money_bonus%", getMoneyBonusPercentage(prestigeLevel) + "%");
        announcement = plugin.getConfigManager().formatText(announcement);

        Bukkit.broadcastMessage(announcement);
    }

    public double getMoneyMultiplier(int prestigeLevel) {
        if (prestigeLevel <= 0) return 1.0;

        FileConfiguration prestigeConfig = plugin.getConfigManager().getConfig("prestige");
        double bonusPerLevel = prestigeConfig.getDouble("prestige.bonuses.money_multiplier_per_level", 0.10);

        return 1.0 + (prestigeLevel * bonusPerLevel);
    }

    public int getXPBonus(int prestigeLevel) {
        FileConfiguration prestigeConfig = plugin.getConfigManager().getConfig("prestige");
        int startLevel = prestigeConfig.getInt("prestige.bonuses.xp_multiplier_start_level", 5);
        int bonusPerLevel = prestigeConfig.getInt("prestige.bonuses.xp_multiplier_per_level", 1);

        if (prestigeLevel < startLevel) return 0;

        return (prestigeLevel - startLevel + 1) * bonusPerLevel;
    }

    public String getMoneyBonusPercentage(int prestigeLevel) {
        if (prestigeLevel <= 0) return "0";

        FileConfiguration prestigeConfig = plugin.getConfigManager().getConfig("prestige");
        double bonusPerLevel = prestigeConfig.getDouble("prestige.bonuses.money_multiplier_per_level", 0.10);

        return String.valueOf((int) (prestigeLevel * bonusPerLevel * 100));
    }

    public String getPrestigeLevelName(int prestigeLevel) {
        if (prestigeLevel <= 0) return "§7None";

        FileConfiguration prestigeConfig = plugin.getConfigManager().getConfig("prestige");
        String levelName = prestigeConfig.getString("level_names." + prestigeLevel);

        if (levelName != null) {
            return plugin.getConfigManager().formatText(levelName);
        }

        return "§6Prestige " + prestigeLevel;
    }

    public boolean isAtMaxPrestige(int prestigeLevel) {
        FileConfiguration prestigeConfig = plugin.getConfigManager().getConfig("prestige");
        int maxLevel = prestigeConfig.getInt("prestige.max_level", 10);
        return prestigeLevel >= maxLevel;
    }

    public int getMaxPrestigeLevel() {
        FileConfiguration prestigeConfig = plugin.getConfigManager().getConfig("prestige");
        return prestigeConfig.getInt("prestige.max_level", 10);
    }

    public int getXPMultiplierStartLevel() {
        FileConfiguration prestigeConfig = plugin.getConfigManager().getConfig("prestige");
        return prestigeConfig.getInt("prestige.bonuses.xp_multiplier_start_level", 5);
    }

    public String getRequiredTierName(int prestigeLevel) {
        FileConfiguration prestigeConfig = plugin.getConfigManager().getConfig("prestige");
        String tierId = prestigeConfig.getString("requirements.prestige_" + prestigeLevel + ".trash_pick_tier", "gods");

        // Convert tier ID to display name
        FileConfiguration mainConfig = plugin.getConfigManager().getMainConfig();
        String tierName = mainConfig.getString("trash_picks." + tierId + ".name", "Unknown");
        return plugin.getConfigManager().formatText(tierName);
    }

    public int getRequiredLevel(int prestigeLevel) {
        FileConfiguration prestigeConfig = plugin.getConfigManager().getConfig("prestige");
        return prestigeConfig.getInt("requirements.prestige_" + prestigeLevel + ".minimum_level", 100);
    }

    public void reload() {
        // Reload is handled by ConfigManager
    }
}