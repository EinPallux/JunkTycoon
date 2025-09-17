package com.pallux.junktycoon.managers;

import com.pallux.junktycoon.JunkTycoon;
import com.pallux.junktycoon.data.PlayerData;
import com.pallux.junktycoon.models.TrashPickTier;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;

import java.util.*;

public class TrashPickManager {

    private final JunkTycoon plugin;
    private final Map<String, TrashPickTier> tiers = new LinkedHashMap<>();
    private final List<String> tierOrder = Arrays.asList("starter", "sturdy", "veteran", "expert", "masters", "gods");

    public TrashPickManager(JunkTycoon plugin) {
        this.plugin = plugin;
        loadTiers();
    }

    public void loadTiers() {
        tiers.clear();
        FileConfiguration config = plugin.getConfigManager().getMainConfig();

        for (String tierId : tierOrder) {
            String path = "trash_picks." + tierId;

            if (!config.contains(path)) {
                plugin.getLogger().warning("Missing configuration for tier: " + tierId);
                continue;
            }

            TrashPickTier tier = new TrashPickTier(
                    tierId,
                    config.getString(path + ".name"),
                    Material.valueOf(config.getString(path + ".material")),
                    config.getInt(path + ".level_required"),
                    config.getInt(path + ".max_level"),
                    config.getDouble(path + ".upgrade_cost")
            );

            tiers.put(tierId, tier);
        }

        plugin.getLogger().info("Loaded " + tiers.size() + " trash pick tiers");
    }

    public void reload() {
        loadTiers();
    }

    public ItemStack createTrashPick(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        TrashPickTier tier = getTier(playerData.getTrashPickTier());

        if (tier == null) {
            tier = getTier("starter");
        }

        ItemStack item = new ItemStack(tier.getMaterial());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', tier.getName()));
            meta.setLore(createTrashPickLore(playerData, tier));
            item.setItemMeta(meta);
        }

        return item;
    }

    private void applyCooldownIndicator(ItemStack item, ItemMeta meta, Player player, PlayerData playerData) {
        // Don't apply cooldown indicator if player has bypass permission
        if (player.hasPermission("junktycoon.admin.nocooldown")) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long lastPickTime = playerData.getLastPickTime();

        // If never picked before, no cooldown
        if (lastPickTime == 0) {
            return;
        }

        double cooldownSeconds = calculateCooldownForPlayer(playerData);
        long cooldownMs = (long) (cooldownSeconds * 1000);
        long timePassed = currentTime - lastPickTime;
        long remainingMs = Math.max(0, cooldownMs - timePassed);

        // If no cooldown remaining, don't apply durability
        if (remainingMs <= 0) {
            return;
        }

        // Calculate cooldown percentage (0.0 = full cooldown, 1.0 = no cooldown)
        double cooldownPercentage = 1.0 - ((double) remainingMs / (double) cooldownMs);

        // Apply durability damage based on cooldown
        // When cooldown is active (percentage close to 0), durability should be low
        // When cooldown is almost done (percentage close to 1), durability should be high

        if (meta instanceof Damageable damageable) {
            short maxDurability = item.getType().getMaxDurability();
            if (maxDurability > 0) {
                short durabilityDamage = (short) (maxDurability * (1.0 - cooldownPercentage));
                // Ensure we don't break the item completely
                durabilityDamage = (short) Math.min(durabilityDamage, maxDurability - 1);

                damageable.setDamage(durabilityDamage);
                meta.setUnbreakable(true); // Prevent the item from actually breaking
            }
        }
    }

    private double calculateCooldownForPlayer(PlayerData playerData) {
        double baseCooldown = plugin.getConfigManager().getMainConfig().getDouble("settings.default_picking_cooldown", 2.0);

        int cooldownPerkLevel = playerData.getCooldownPerkLevel();
        if (cooldownPerkLevel > 0) {
            double reductionPerLevel = plugin.getConfigManager().getMainConfig().getDouble("perks.picking_cooldown.cooldown_reduction", 0.1);
            double reduction = cooldownPerkLevel * reductionPerLevel;
            baseCooldown = Math.max(0.1, baseCooldown - reduction);
        }

        return baseCooldown;
    }

    private List<String> createTrashPickLore(PlayerData playerData, TrashPickTier tier) {
        List<String> lore = new ArrayList<>();
        FileConfiguration messagesConfig = plugin.getConfigManager().getMessagesConfig();

        List<String> lorTemplate = messagesConfig.getStringList("trash_pick_lore");

        int requiredXP = calculateRequiredXP(playerData.getTrashPickLevel());

        for (String line : lorTemplate) {
            line = line.replace("%level%", String.valueOf(playerData.getTrashPickLevel()));
            line = line.replace("%max_level%", tier.getMaxLevel() == -1 ? "∞" : String.valueOf(tier.getMaxLevel()));
            line = line.replace("%xp%", String.valueOf(playerData.getTrashPickXP()));
            line = line.replace("%required_xp%", String.valueOf(requiredXP));
            line = line.replace("%tier%", tier.getName());
            line = line.replace("%cooldown_level%", String.valueOf(playerData.getCooldownPerkLevel()));
            line = line.replace("%multiplier_level%", String.valueOf(playerData.getMultiplierPerkLevel()));
            line = line.replace("%rarity_level%", String.valueOf(playerData.getRarityPerkLevel()));
            line = line.replace("%points_level%", String.valueOf(playerData.getPointFinderPerkLevel()));

            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }

        return lore;
    }

    public void giveTrashPick(Player player) {
        ItemStack trashPick = createTrashPick(player);

        // Set in first hotbar slot and make it undroppable
        player.getInventory().setItem(0, trashPick);

        // Update the item if it's already there
        updateTrashPickInInventory(player);
    }

    public void updateTrashPickInInventory(Player player) {
        ItemStack currentItem = player.getInventory().getItem(0);

        if (currentItem != null && isTrashPick(currentItem)) {
            ItemStack newTrashPick = createTrashPick(player);
            player.getInventory().setItem(0, newTrashPick);
        } else {
            // If trash pick is missing, give a new one
            giveTrashPick(player);
        }
    }

    public boolean isTrashPick(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) return false;

        String displayName = ChatColor.stripColor(meta.getDisplayName());

        // Check if it's any of our trash pick tiers
        for (TrashPickTier tier : tiers.values()) {
            String tierName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', tier.getName()));
            if (displayName.equals(tierName)) {
                return true;
            }
        }

        return false;
    }

    public int calculateRequiredXP(int level) {
        FileConfiguration config = plugin.getConfigManager().getMainConfig();
        int baseXP = config.getInt("xp.base_xp_requirement", 100);
        int multiplier = config.getInt("xp.level_multiplier", 10);

        return baseXP + (level * multiplier);
    }

    public int getXPForTrashType(String trashTypeId) {
        FileConfiguration config = plugin.getConfigManager().getMainConfig();
        return config.getInt("xp." + trashTypeId, 1);
    }

    public boolean canLevelUp(PlayerData playerData) {
        int requiredXP = calculateRequiredXP(playerData.getTrashPickLevel());
        return playerData.getTrashPickXP() >= requiredXP;
    }

    public void levelUp(Player player, PlayerData playerData) {
        TrashPickTier currentTier = getTier(playerData.getTrashPickTier());

        if (currentTier != null && currentTier.getMaxLevel() != -1 &&
                playerData.getTrashPickLevel() >= currentTier.getMaxLevel()) {
            return; // Already at max level for this tier
        }

        int requiredXP = calculateRequiredXP(playerData.getTrashPickLevel());
        playerData.setTrashPickXP(playerData.getTrashPickXP() - requiredXP);
        playerData.levelUp();

        // Send level up message
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("level", String.valueOf(playerData.getTrashPickLevel()));
        String message = plugin.getConfigManager().getMessage("trash_finding.level_up", placeholders);
        player.sendMessage(message);

        // Update trash pick in inventory
        updateTrashPickInInventory(player);
    }

    public TrashPickTier getTier(String tierId) {
        return tiers.get(tierId);
    }

    public TrashPickTier getNextTier(String currentTierId) {
        int currentIndex = tierOrder.indexOf(currentTierId);
        if (currentIndex == -1 || currentIndex >= tierOrder.size() - 1) {
            return null; // No next tier
        }
        return getTier(tierOrder.get(currentIndex + 1));
    }

    public boolean canUpgradeTier(PlayerData playerData) {
        TrashPickTier nextTier = getNextTier(playerData.getTrashPickTier());
        return nextTier != null && playerData.getTrashPickLevel() >= nextTier.getLevelRequired();
    }

    public Collection<TrashPickTier> getAllTiers() {
        return tiers.values();
    }
}