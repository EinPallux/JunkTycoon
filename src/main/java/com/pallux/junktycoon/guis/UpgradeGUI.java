package com.pallux.junktycoon.guis;

import com.pallux.junktycoon.JunkTycoon;
import com.pallux.junktycoon.data.PlayerData;
import com.pallux.junktycoon.models.TrashPickTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpgradeGUI implements Listener {

    private final JunkTycoon plugin;
    private final Player player;
    private final PlayerData playerData;
    private final Inventory inventory;

    // Slot positions
    private static final int COOLDOWN_SLOT = 10;
    private static final int MULTIPLIER_SLOT = 12;
    private static final int RARITY_SLOT = 14;
    private static final int POINTS_SLOT = 16;
    private static final int XP_MULTIPLIER_SLOT = 28;
    private static final int UPGRADE_PICK_SLOT = 31;

    public UpgradeGUI(JunkTycoon plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.playerData = plugin.getPlayerDataManager().getPlayerData(player);

        FileConfiguration config = plugin.getConfigManager().getMainConfig();
        int size = config.getInt("gui.upgrade_gui_size", 45);
        String title = plugin.getConfigManager().formatText(config.getString("gui.upgrade_gui_title", "&6&lTrash Pick Upgrades"));

        this.inventory = Bukkit.createInventory(null, size, title);

        setupGUI();
    }

    private void setupGUI() {
        // Clear inventory
        inventory.clear();

        // Fill with glass panes
        fillWithGlass();

        // Add perk items
        addPerkItem(COOLDOWN_SLOT, "picking_cooldown", Material.CLOCK, playerData.getCooldownPerkLevel());
        addPerkItem(MULTIPLIER_SLOT, "trash_multiplier", Material.DIAMOND, playerData.getMultiplierPerkLevel());
        addPerkItem(RARITY_SLOT, "trash_rarity", Material.EMERALD, playerData.getRarityPerkLevel());

        // Add points perk only if PlayerPoints is enabled
        if (plugin.getPlayerPointsHook().isEnabled()) {
            addPerkItem(POINTS_SLOT, "point_finder", Material.NETHER_STAR, playerData.getPointFinderPerkLevel());
            // Use custom method for XP Multiplier that handles PlayerPoints
            addXPMultiplierPerkItem();
        } else {
            addDisabledPerkItem(POINTS_SLOT, "point_finder", Material.BARRIER);
            addDisabledPerkItem(XP_MULTIPLIER_SLOT, "xp_multiplier", Material.BARRIER);
        }

        // Add trash pick upgrade item
        addTrashPickUpgradeItem();
    }

    private void fillWithGlass() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, glass);
        }
    }

    private void addPerkItem(int slot, String perkId, Material material, int currentLevel) {
        FileConfiguration config = plugin.getConfigManager().getMainConfig();

        if (!config.getBoolean("perks." + perkId + ".enabled", true)) {
            addDisabledPerkItem(slot, perkId, Material.BARRIER);
            return;
        }

        int maxLevel = config.getInt("perks." + perkId + ".max_level");
        double baseCost = config.getDouble("perks." + perkId + ".base_cost");
        double multiplier = config.getDouble("perks." + perkId + ".cost_multiplier");

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String perkName = plugin.getConfigManager().formatText(config.getString("perks." + perkId + ".name"));
            String description = plugin.getConfigManager().formatText(config.getString("perks." + perkId + ".description"));

            // Determine item state
            if (currentLevel >= maxLevel) {
                // Maxed perk
                meta.setDisplayName(plugin.getConfigManager().getMessage("upgrade_gui.perk_maxed")
                        .replace("%perk_name%", perkName));
                meta.setLore(createPerkLore("perk_lore_maxed", description, currentLevel, maxLevel, 0));
            } else if (currentLevel == 0) {
                // Locked perk
                meta.setDisplayName(plugin.getConfigManager().getMessage("upgrade_gui.perk_locked")
                        .replace("%perk_name%", perkName));
                double cost = baseCost;
                meta.setLore(createPerkLore("perk_lore_locked", description, currentLevel, maxLevel, cost));
            } else {
                // Unlocked perk
                meta.setDisplayName(plugin.getConfigManager().getMessage("upgrade_gui.perk_unlocked")
                        .replace("%perk_name%", perkName)
                        .replace("%level%", String.valueOf(currentLevel)));
                double cost = baseCost * Math.pow(multiplier, currentLevel);
                meta.setLore(createPerkLore("perk_lore", description, currentLevel, maxLevel, cost));
            }

            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
    }

    private void addXPMultiplierPerkItem() {
        FileConfiguration config = plugin.getConfigManager().getMainConfig();

        if (!config.getBoolean("perks.xp_multiplier.enabled", true)) {
            addDisabledPerkItem(XP_MULTIPLIER_SLOT, "xp_multiplier", Material.BARRIER);
            return;
        }

        int currentLevel = playerData.getXpMultiplierPerkLevel();
        int maxLevel = config.getInt("perks.xp_multiplier.max_level");
        int baseCost = config.getInt("perks.xp_multiplier.base_cost");
        double multiplier = config.getDouble("perks.xp_multiplier.cost_multiplier");

        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String perkName = plugin.getConfigManager().formatText(config.getString("perks.xp_multiplier.name"));
            String description = plugin.getConfigManager().formatText(config.getString("perks.xp_multiplier.description"));

            // Determine item state
            if (currentLevel >= maxLevel) {
                // Maxed perk
                meta.setDisplayName(plugin.getConfigManager().getMessage("upgrade_gui.perk_maxed")
                        .replace("%perk_name%", perkName));
                meta.setLore(createXPMultiplierLore("perk_lore_maxed", description, currentLevel, maxLevel, 0));
            } else if (currentLevel == 0) {
                // Locked perk
                meta.setDisplayName(plugin.getConfigManager().getMessage("upgrade_gui.perk_locked")
                        .replace("%perk_name%", perkName));
                int cost = baseCost;
                meta.setLore(createXPMultiplierLore("perk_lore_locked", description, currentLevel, maxLevel, cost));
            } else {
                // Unlocked perk
                meta.setDisplayName(plugin.getConfigManager().getMessage("upgrade_gui.perk_unlocked")
                        .replace("%perk_name%", perkName)
                        .replace("%level%", String.valueOf(currentLevel)));
                int cost = (int) (baseCost * Math.pow(multiplier, currentLevel));
                meta.setLore(createXPMultiplierLore("perk_lore", description, currentLevel, maxLevel, cost));
            }

            item.setItemMeta(meta);
        }

        inventory.setItem(XP_MULTIPLIER_SLOT, item);
    }

    private List<String> createXPMultiplierLore(String templateKey, String description, int currentLevel, int maxLevel, int cost) {
        List<String> lore = new ArrayList<>();

        // Create custom lore for PlayerPoints perk instead of using templates
        lore.add("§7" + description);
        lore.add("");

        if (templateKey.equals("perk_lore_maxed")) {
            lore.add("§7Level: §6MAX");
            lore.add("");
            lore.add("§c✗ Already maxed out!");
        } else if (templateKey.equals("perk_lore_locked")) {
            lore.add("§7Level: §c0§7/§f" + maxLevel);
            lore.add("§7Cost to unlock: §9" + cost + " Points");
            lore.add("");
            lore.add("§e▶ Click to unlock!");
        } else {
            lore.add("§7Current Level: §f" + currentLevel + "§7/§f" + maxLevel);
            lore.add("§7Cost to upgrade: §9" + cost + " Points");
            lore.add("");
            lore.add("§e▶ Click to upgrade!");
        }

        // Apply color formatting to all lines
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, plugin.getConfigManager().formatText(lore.get(i)));
        }

        return lore;
    }

    private void addDisabledPerkItem(int slot, String perkId, Material material) {
        FileConfiguration config = plugin.getConfigManager().getMainConfig();

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String perkName = plugin.getConfigManager().formatText(config.getString("perks." + perkId + ".name"));
            meta.setDisplayName("§c" + perkName + " - DISABLED");

            List<String> lore = new ArrayList<>();
            lore.add("§7This perk is currently disabled.");
            if (perkId.equals("point_finder")) {
                lore.add("§7PlayerPoints plugin required.");
            }
            meta.setLore(lore);

            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
    }

    private List<String> createPerkLore(String templateKey, String description, int currentLevel, int maxLevel, double cost) {
        List<String> template = plugin.getConfigManager().getMessagesConfig().getStringList("upgrade_gui." + templateKey);
        List<String> lore = new ArrayList<>();

        for (String line : template) {
            line = line.replace("%description%", description);
            line = line.replace("%current_level%", String.valueOf(currentLevel));
            line = line.replace("%max_level%", String.valueOf(maxLevel));
            line = line.replace("%cost%", plugin.getVaultHook().formatMoney(cost));

            lore.add(plugin.getConfigManager().formatText(line));
        }

        return lore;
    }

    private void addTrashPickUpgradeItem() {
        TrashPickTier nextTier = plugin.getTrashPickManager().getNextTier(playerData.getTrashPickTier());
        TrashPickTier currentTier = plugin.getTrashPickManager().getTier(playerData.getTrashPickTier());

        ItemStack item;

        if (nextTier == null) {
            // Max tier reached
            item = new ItemStack(Material.GOLDEN_APPLE);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(plugin.getConfigManager().getMessage("upgrade_gui.trash_pick_maxed")
                        .replace("%current_pick%", currentTier.getName()));

                List<String> lore = plugin.getConfigManager().getMessagesConfig().getStringList("upgrade_gui.trash_pick_maxed.lore");
                List<String> processedLore = new ArrayList<>();
                for (String line : lore) {
                    line = line.replace("%current_pick%", plugin.getConfigManager().formatText(currentTier.getName()));
                    processedLore.add(plugin.getConfigManager().formatText(line));
                }
                meta.setLore(processedLore);

                item.setItemMeta(meta);
            }
        } else {
            // Can upgrade or locked
            item = new ItemStack(nextTier.getMaterial());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                boolean canUpgrade = plugin.getTrashPickManager().canUpgradeTier(playerData);

                if (canUpgrade) {
                    meta.setDisplayName(plugin.getConfigManager().getMessage("upgrade_gui.trash_pick_upgrade.name"));
                } else {
                    meta.setDisplayName(plugin.getConfigManager().getMessage("upgrade_gui.trash_pick_upgrade_locked.name"));
                }

                // Create lore
                String loreKey = canUpgrade ? "trash_pick_upgrade" : "trash_pick_upgrade_locked";
                List<String> lore = plugin.getConfigManager().getMessagesConfig().getStringList("upgrade_gui." + loreKey + ".lore");
                List<String> processedLore = new ArrayList<>();

                for (String line : lore) {
                    line = line.replace("%current_pick%", plugin.getConfigManager().formatText(currentTier.getName()));
                    line = line.replace("%next_pick%", plugin.getConfigManager().formatText(nextTier.getName()));
                    line = line.replace("%required_level%", String.valueOf(nextTier.getLevelRequired()));
                    line = line.replace("%current_level%", String.valueOf(playerData.getTrashPickLevel()));
                    line = line.replace("%cost%", plugin.getVaultHook().formatMoney(nextTier.getUpgradeCost()));
                    processedLore.add(plugin.getConfigManager().formatText(line));
                }
                meta.setLore(processedLore);

                item.setItemMeta(meta);
            }
        }

        inventory.setItem(UPGRADE_PICK_SLOT, item);
    }

    public void open() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player clicker) || !clicker.equals(player)) return;

        int slot = event.getSlot();

        switch (slot) {
            case COOLDOWN_SLOT -> handlePerkUpgrade("picking_cooldown");
            case MULTIPLIER_SLOT -> handlePerkUpgrade("trash_multiplier");
            case RARITY_SLOT -> handlePerkUpgrade("trash_rarity");
            case POINTS_SLOT -> {
                if (plugin.getPlayerPointsHook().isEnabled()) {
                    handlePerkUpgrade("point_finder");
                }
            }
            case XP_MULTIPLIER_SLOT -> {
                if (plugin.getPlayerPointsHook().isEnabled()) {
                    handleXPMultiplierUpgrade();
                }
            }
            case UPGRADE_PICK_SLOT -> handleTrashPickUpgrade();
        }
    }

    private void handlePerkUpgrade(String perkId) {
        FileConfiguration config = plugin.getConfigManager().getMainConfig();

        if (!config.getBoolean("perks." + perkId + ".enabled", true)) {
            player.sendMessage(plugin.getConfigManager().getMessage("general.prefix") + "§cThis perk is disabled!");
            return;
        }

        int currentLevel = getCurrentPerkLevel(perkId);
        int maxLevel = config.getInt("perks." + perkId + ".max_level");

        if (currentLevel >= maxLevel) {
            player.sendMessage(plugin.getConfigManager().getMessage("errors.perk_maxed"));
            return;
        }

        double baseCost = config.getDouble("perks." + perkId + ".base_cost");
        double multiplier = config.getDouble("perks." + perkId + ".cost_multiplier");
        double cost = currentLevel == 0 ? baseCost : baseCost * Math.pow(multiplier, currentLevel);

        if (!plugin.getVaultHook().hasEnoughMoney(player, cost)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("required", plugin.getVaultHook().formatMoney(cost));
            player.sendMessage(plugin.getConfigManager().getMessage("errors.insufficient_funds", placeholders));
            return;
        }

        // Purchase successful
        plugin.getVaultHook().withdrawMoney(player, cost);
        upgradePerk(perkId, currentLevel + 1);

        // Send success message
        String perkName = plugin.getConfigManager().formatText(config.getString("perks." + perkId + ".name"));
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("perk_name", perkName);
        placeholders.put("level", String.valueOf(currentLevel + 1));
        player.sendMessage(plugin.getConfigManager().getMessage("success.perk_upgraded", placeholders));

        // Update GUI and trash pick
        setupGUI();
        plugin.getTrashPickManager().updateTrashPickInInventory(player);
        plugin.getPlayerDataManager().savePlayerData(player);
    }

    private void handleXPMultiplierUpgrade() {
        FileConfiguration config = plugin.getConfigManager().getMainConfig();

        if (!config.getBoolean("perks.xp_multiplier.enabled", true)) {
            player.sendMessage(plugin.getConfigManager().getMessage("general.prefix") + "§cThis perk is disabled!");
            return;
        }

        int currentLevel = playerData.getXpMultiplierPerkLevel();
        int maxLevel = config.getInt("perks.xp_multiplier.max_level");

        if (currentLevel >= maxLevel) {
            player.sendMessage(plugin.getConfigManager().getMessage("errors.perk_maxed"));
            return;
        }

        int baseCost = config.getInt("perks.xp_multiplier.base_cost");
        double multiplier = config.getDouble("perks.xp_multiplier.cost_multiplier");
        int cost = currentLevel == 0 ? baseCost : (int) (baseCost * Math.pow(multiplier, currentLevel));

        int currentPoints = plugin.getPlayerPointsHook().getPoints(player);
        if (currentPoints < cost) {
            player.sendMessage(plugin.getConfigManager().getMessage("general.prefix") +
                    "§cYou don't have enough Points! Required: §9" + cost + " Points");
            return;
        }

        // Purchase successful
        plugin.getPlayerPointsHook().takePoints(player, cost);
        playerData.setXpMultiplierPerkLevel(currentLevel + 1);

        // Send success message
        String perkName = plugin.getConfigManager().formatText(config.getString("perks.xp_multiplier.name"));
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("perk_name", perkName);
        placeholders.put("level", String.valueOf(currentLevel + 1));
        player.sendMessage(plugin.getConfigManager().getMessage("success.perk_upgraded", placeholders));

        // Update GUI and trash pick
        setupGUI();
        plugin.getTrashPickManager().updateTrashPickInInventory(player);
        plugin.getPlayerDataManager().savePlayerData(player);
    }

    private void handleTrashPickUpgrade() {
        TrashPickTier nextTier = plugin.getTrashPickManager().getNextTier(playerData.getTrashPickTier());

        if (nextTier == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("errors.pick_maxed"));
            return;
        }

        if (!plugin.getTrashPickManager().canUpgradeTier(playerData)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("required", String.valueOf(nextTier.getLevelRequired()));
            player.sendMessage(plugin.getConfigManager().getMessage("errors.level_requirement", placeholders));
            return;
        }

        double cost = nextTier.getUpgradeCost();
        if (!plugin.getVaultHook().hasEnoughMoney(player, cost)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("required", plugin.getVaultHook().formatMoney(cost));
            player.sendMessage(plugin.getConfigManager().getMessage("errors.insufficient_funds", placeholders));
            return;
        }

        // Upgrade successful
        plugin.getVaultHook().withdrawMoney(player, cost);
        playerData.upgradeTier(nextTier.getId());

        // Send success message
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("pick_name", plugin.getConfigManager().formatText(nextTier.getName()));
        player.sendMessage(plugin.getConfigManager().getMessage("success.pick_upgraded", placeholders));

        // Update GUI and give new trash pick
        setupGUI();
        plugin.getTrashPickManager().giveTrashPick(player);
        plugin.getPlayerDataManager().savePlayerData(player);
    }

    private int getCurrentPerkLevel(String perkId) {
        return switch (perkId) {
            case "picking_cooldown" -> playerData.getCooldownPerkLevel();
            case "trash_multiplier" -> playerData.getMultiplierPerkLevel();
            case "trash_rarity" -> playerData.getRarityPerkLevel();
            case "point_finder" -> playerData.getPointFinderPerkLevel();
            case "xp_multiplier" -> playerData.getXpMultiplierPerkLevel();
            default -> 0;
        };
    }

    private void upgradePerk(String perkId, int newLevel) {
        switch (perkId) {
            case "picking_cooldown" -> playerData.setCooldownPerkLevel(newLevel);
            case "trash_multiplier" -> playerData.setMultiplierPerkLevel(newLevel);
            case "trash_rarity" -> playerData.setRarityPerkLevel(newLevel);
            case "point_finder" -> playerData.setPointFinderPerkLevel(newLevel);
            case "xp_multiplier" -> playerData.setXpMultiplierPerkLevel(newLevel);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;

        HandlerList.unregisterAll(this);
    }
}