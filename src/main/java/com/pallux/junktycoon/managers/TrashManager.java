package com.pallux.junktycoon.managers;

import com.pallux.junktycoon.JunkTycoon;
import com.pallux.junktycoon.data.PlayerData;
import com.pallux.junktycoon.models.TrashType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class TrashManager {

    private final JunkTycoon plugin;
    private final Map<String, TrashType> trashTypes = new HashMap<>();
    private final Random random = new Random();

    public TrashManager(JunkTycoon plugin) {
        this.plugin = plugin;
        loadTrashTypes();
    }

    public void loadTrashTypes() {
        trashTypes.clear();
        FileConfiguration config = plugin.getConfigManager().getTrashConfig();

        for (String key : config.getConfigurationSection("trash_rarities").getKeys(false)) {
            String path = "trash_rarities." + key;

            TrashType trashType = new TrashType(
                    key,
                    config.getString(path + ".name"),
                    config.getDouble(path + ".chance"),
                    config.getInt(path + ".min_value"),
                    config.getInt(path + ".max_value"),
                    config.getString(path + ".color")
            );

            // Load effects
            if (config.contains("effects.particles." + key)) {
                try {
                    Particle particle = Particle.valueOf(config.getString("effects.particles." + key));
                    trashType.setParticle(particle);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid particle effect for " + key + ": " + config.getString("effects.particles." + key));
                }
            }

            if (config.contains("effects.sounds." + key)) {
                try {
                    Sound sound = Sound.valueOf(config.getString("effects.sounds." + key));
                    trashType.setSound(sound);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid sound effect for " + key + ": " + config.getString("effects.sounds." + key));
                }
            }

            trashTypes.put(key, trashType);
        }

        plugin.getLogger().info("Loaded " + trashTypes.size() + " trash types");
    }

    public void reload() {
        loadTrashTypes();
    }

    public TrashType generateTrash(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        double rarityBoost = calculateRarityBoost(playerData);

        double totalWeight = 0.0;
        Map<TrashType, Double> adjustedWeights = new HashMap<>();

        // Calculate adjusted weights based on rarity boost
        for (TrashType trashType : trashTypes.values()) {
            double adjustedChance = trashType.getChance();

            // Apply rarity boost (higher rarity gets more boost)
            if (rarityBoost > 0) {
                // Reduce common trash chance, increase rare trash chance
                if (trashType.getId().equals("common")) {
                    adjustedChance *= (1.0 - rarityBoost * 0.5);
                } else {
                    // Boost for rare items
                    double rareMultiplier = getRareMultiplier(trashType.getId());
                    adjustedChance *= (1.0 + rarityBoost * rareMultiplier);
                }
            }

            adjustedWeights.put(trashType, adjustedChance);
            totalWeight += adjustedChance;
        }

        // Generate random number and select trash type
        double randomValue = random.nextDouble() * totalWeight;
        double currentWeight = 0.0;

        for (Map.Entry<TrashType, Double> entry : adjustedWeights.entrySet()) {
            currentWeight += entry.getValue();
            if (randomValue <= currentWeight) {
                return entry.getKey();
            }
        }

        // Fallback to common trash
        return trashTypes.get("common");
    }

    private double calculateRarityBoost(PlayerData playerData) {
        int rarityPerkLevel = playerData.getRarityPerkLevel();
        if (rarityPerkLevel <= 0) return 0.0;

        FileConfiguration config = plugin.getConfigManager().getMainConfig();
        double boostPerLevel = config.getDouble("perks.trash_rarity.rarity_boost", 0.05);

        return rarityPerkLevel * boostPerLevel;
    }

    private double getRareMultiplier(String trashId) {
        // Higher multipliers for rarer items
        return switch (trashId) {
            case "uncommon" -> 1.0;
            case "rare" -> 2.0;
            case "epic" -> 3.0;
            case "legendary" -> 4.0;
            case "mythical" -> 5.0;
            case "godlike" -> 6.0;
            default -> 0.5;
        };
    }

    public int generateTrashAmount(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        int multiplierLevel = playerData.getMultiplierPerkLevel();

        if (multiplierLevel <= 0) return 1;

        // Random amount between 1 and multiplier level
        return random.nextInt(multiplierLevel) + 1;
    }

    public int generateTrashValue(TrashType trashType) {
        int minValue = trashType.getMinValue();
        int maxValue = trashType.getMaxValue();

        if (minValue == maxValue) return minValue;

        return random.nextInt(maxValue - minValue + 1) + minValue;
    }

    public TrashType getTrashType(String id) {
        return trashTypes.get(id);
    }

    public Collection<TrashType> getAllTrashTypes() {
        return trashTypes.values();
    }
}