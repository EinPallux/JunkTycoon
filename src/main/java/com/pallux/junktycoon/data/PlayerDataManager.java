package com.pallux.junktycoon.data;

import com.pallux.junktycoon.JunkTycoon;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerDataManager {

    private final JunkTycoon plugin;
    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();
    private final File dataFolder;

    public PlayerDataManager(JunkTycoon plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        PlayerData data = playerDataCache.get(uuid);
        if (data == null) {
            data = loadPlayerData(uuid);
            playerDataCache.put(uuid, data);
        }
        return data;
    }

    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    private PlayerData loadPlayerData(UUID uuid) {
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");

        if (!playerFile.exists()) {
            return createNewPlayerData(uuid);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        PlayerData data = new PlayerData(uuid);

        // Load basic data
        data.setTrashPickTier(config.getString("trash_pick.tier", "starter"));
        data.setTrashPickLevel(config.getInt("trash_pick.level", 1));
        data.setTrashPickXP(config.getInt("trash_pick.xp", 0));
        data.setLastPickTime(config.getLong("last_pick_time", 0));

        // Load perks
        data.setCooldownPerkLevel(config.getInt("perks.cooldown", 0));
        data.setMultiplierPerkLevel(config.getInt("perks.multiplier", 0));
        data.setRarityPerkLevel(config.getInt("perks.rarity", 0));
        data.setPointFinderPerkLevel(config.getInt("perks.point_finder", 0));
        data.setXpMultiplierPerkLevel(config.getInt("perks.xp_multiplier", 0));

        // Load prestige data
        data.setPrestigeLevel(config.getInt("prestige.level", 0));

        // Load statistics
        data.setTotalTrashPicked(config.getInt("stats.total_trash", 0));
        data.setTotalMoneyEarned(config.getDouble("stats.total_money", 0.0));

        return data;
    }

    private PlayerData createNewPlayerData(UUID uuid) {
        PlayerData data = new PlayerData(uuid);
        data.setTrashPickTier("starter");
        data.setTrashPickLevel(1);
        data.setTrashPickXP(0);
        data.setLastPickTime(0);
        data.setCooldownPerkLevel(0);
        data.setMultiplierPerkLevel(0);
        data.setRarityPerkLevel(0);
        data.setPointFinderPerkLevel(0);
        data.setXpMultiplierPerkLevel(0);
        data.setPrestigeLevel(0);
        data.setTotalTrashPicked(0);
        data.setTotalMoneyEarned(0.0);
        return data;
    }

    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataCache.get(uuid);
        if (data == null) return;

        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        // Save basic data
        config.set("trash_pick.tier", data.getTrashPickTier());
        config.set("trash_pick.level", data.getTrashPickLevel());
        config.set("trash_pick.xp", data.getTrashPickXP());
        config.set("last_pick_time", data.getLastPickTime());

        // Save perks
        config.set("perks.cooldown", data.getCooldownPerkLevel());
        config.set("perks.multiplier", data.getMultiplierPerkLevel());
        config.set("perks.rarity", data.getRarityPerkLevel());
        config.set("perks.point_finder", data.getPointFinderPerkLevel());
        config.set("perks.xp_multiplier", data.getXpMultiplierPerkLevel());

        // Save prestige data
        config.set("prestige.level", data.getPrestigeLevel());

        // Save statistics
        config.set("stats.total_trash", data.getTotalTrashPicked());
        config.set("stats.total_money", data.getTotalMoneyEarned());

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player data for " + uuid, e);
        }
    }

    public void savePlayerData(Player player) {
        savePlayerData(player.getUniqueId());
    }

    public void saveAllData() {
        for (UUID uuid : playerDataCache.keySet()) {
            savePlayerData(uuid);
        }
    }

    public void unloadPlayerData(UUID uuid) {
        savePlayerData(uuid);
        playerDataCache.remove(uuid);
    }
}