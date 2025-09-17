package com.pallux.junktycoon.guis;

import com.pallux.junktycoon.JunkTycoon;
import com.pallux.junktycoon.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PrestigeGUI implements Listener {

    private final JunkTycoon plugin;
    private final Player player;
    private final PlayerData playerData;
    private final Inventory inventory;

    public PrestigeGUI(JunkTycoon plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.playerData = plugin.getPlayerDataManager().getPlayerData(player);

        FileConfiguration prestigeConfig = plugin.getConfigManager().getConfig("prestige");
        int size = prestigeConfig.getInt("prestige.gui.size", 45);
        String title = plugin.getConfigManager().formatText(prestigeConfig.getString("prestige.gui.title", "&6&lPrestige System"));

        this.inventory = Bukkit.createInventory(null, size, title);

        setupGUI();
    }

    private void setupGUI() {
        // Clear inventory
        inventory.clear();

        // Fill with glass panes
        fillWithGlass();

        // Add current prestige display
        addCurrentPrestigeItem();

        // Add next prestige item
        addNextPrestigeItem();
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

    private void addCurrentPrestigeItem() {
        FileConfiguration prestigeConfig = plugin.getConfigManager().getConfig("prestige");
        int slot = prestigeConfig.getInt("gui_items.current_prestige.slot", 13);

        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = prestigeConfig.getString("gui_items.current_prestige.name", "Your Current Prestige");
            meta.setDisplayName(plugin.getConfigManager().formatText(name));

            // Create lore
            List<String> loreTemplate = prestigeConfig.getStringList("gui_items.current_prestige.lore");
            List<String> lore = new ArrayList<>();

            int currentPrestige = playerData.getPrestigeLevel();
            String moneyBonus = plugin.getPrestigeManager().getMoneyBonusPercentage(currentPrestige) + "%";
            String xpBonus = String.valueOf(plugin.getPrestigeManager().getXPBonus(currentPrestige));

            for (String line : loreTemplate) {
                line = line.replace("%current_level%", currentPrestige == 0 ? "None" : String.valueOf(currentPrestige));
                line = line.replace("%money_bonus%", moneyBonus);
                line = line.replace("%xp_bonus%", xpBonus);
                line = line.replace("%total_trash%", String.valueOf(playerData.getTotalTrashPicked()));
                line = line.replace("%total_money%", plugin.getVaultHook().formatMoney(playerData.getTotalMoneyEarned()));

                lore.add(plugin.getConfigManager().formatText(line));
            }

            // Add current prestige name if exists
            if (currentPrestige > 0) {
                lore.add("");
                lore.add("§7Current Prestige: " + plugin.getPrestigeManager().getPrestigeLevelName(currentPrestige));
            }

            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
    }

    private void addNextPrestigeItem() {
        FileConfiguration prestigeConfig = plugin.getConfigManager().getConfig("prestige");
        int slot = prestigeConfig.getInt("gui_items.next_prestige.slot", 31);

        int currentPrestige = playerData.getPrestigeLevel();
        int nextPrestige = currentPrestige + 1;

        ItemStack item = new ItemStack(Material.END_CRYSTAL);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = prestigeConfig.getString("gui_items.next_prestige.name", "Next Prestige Level");
            meta.setDisplayName(plugin.getConfigManager().formatText(name));

            List<String> lore = new ArrayList<>();

            if (plugin.getPrestigeManager().isAtMaxPrestige(currentPrestige)) {
                // Max prestige reached
                List<String> maxedLore = prestigeConfig.getStringList("gui_items.next_prestige.lore_maxed");

                for (String line : maxedLore) {
                    line = line.replace("%max_level%", String.valueOf(plugin.getPrestigeManager().getMaxPrestigeLevel()));
                    line = line.replace("%money_bonus%", plugin.getPrestigeManager().getMoneyBonusPercentage(currentPrestige) + "%");
                    line = line.replace("%xp_bonus%", String.valueOf(plugin.getPrestigeManager().getXPBonus(currentPrestige)));

                    lore.add(plugin.getConfigManager().formatText(line));
                }

                // Change item to golden apple for max prestige
                item.setType(Material.GOLDEN_APPLE);

            } else {
                // Check if can prestige
                boolean canPrestige = plugin.getPrestigeManager().canPrestige(player);

                List<String> loreTemplate;
                if (canPrestige) {
                    loreTemplate = prestigeConfig.getStringList("gui_items.next_prestige.lore_available");
                } else {
                    loreTemplate = prestigeConfig.getStringList("gui_items.next_prestige.lore_locked");
                    // Change item to barrier if locked
                    item.setType(Material.BARRIER);
                }

                // Create lore with placeholders
                String nextMoneyBonus = plugin.getPrestigeManager().getMoneyBonusPercentage(nextPrestige) + "%";
                String nextXpBonus = String.valueOf(plugin.getPrestigeManager().getXPBonus(nextPrestige));
                String requiredTier = plugin.getPrestigeManager().getRequiredTierName(nextPrestige);
                String requiredLevel = String.valueOf(plugin.getPrestigeManager().getRequiredLevel(nextPrestige));

                String currentTierName = plugin.getConfigManager().formatText(
                        plugin.getConfigManager().getMainConfig().getString("trash_picks." + playerData.getTrashPickTier() + ".name", "Unknown"));

                for (String line : loreTemplate) {
                    line = line.replace("%next_level%", String.valueOf(nextPrestige));
                    line = line.replace("%next_money_bonus%", nextMoneyBonus);
                    line = line.replace("%next_xp_bonus%", nextXpBonus);
                    line = line.replace("%required_tier%", requiredTier);
                    line = line.replace("%required_level%", requiredLevel);
                    line = line.replace("%current_tier%", currentTierName);
                    line = line.replace("%current_level%", String.valueOf(playerData.getTrashPickLevel()));

                    lore.add(plugin.getConfigManager().formatText(line));
                }
            }

            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
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

        FileConfiguration prestigeConfig = plugin.getConfigManager().getConfig("prestige");
        int nextPrestigeSlot = prestigeConfig.getInt("gui_items.next_prestige.slot", 31);

        if (event.getSlot() == nextPrestigeSlot) {
            handlePrestigeAttempt();
        }
    }

    private void handlePrestigeAttempt() {
        int currentPrestige = playerData.getPrestigeLevel();

        // Check if at max prestige
        if (plugin.getPrestigeManager().isAtMaxPrestige(currentPrestige)) {
            FileConfiguration prestigeConfig = plugin.getConfigManager().getConfig("prestige");
            String message = plugin.getConfigManager().formatText(prestigeConfig.getString("messages.already_max_prestige"));
            player.sendMessage(plugin.getConfigManager().getMessage("general.prefix") + message);
            return;
        }

        // Check if can prestige
        if (!plugin.getPrestigeManager().canPrestige(player)) {
            FileConfiguration prestigeConfig = plugin.getConfigManager().getConfig("prestige");
            String message = plugin.getConfigManager().formatText(prestigeConfig.getString("messages.prestige_failed"));
            player.sendMessage(plugin.getConfigManager().getMessage("general.prefix") + message);
            return;
        }

        // Show final confirmation
        showPrestigeConfirmation();
    }

    private void showPrestigeConfirmation() {
        int nextPrestige = playerData.getPrestigeLevel() + 1;

        player.closeInventory();

        String prefix = plugin.getConfigManager().getMessage("general.prefix");
        player.sendMessage("");
        player.sendMessage(prefix + "§e⚠ PRESTIGE CONFIRMATION ⚠");
        player.sendMessage("§7You are about to prestige to level §6" + nextPrestige + "§7!");
        player.sendMessage("");
        player.sendMessage("§cThis will reset:");
        player.sendMessage("§c• Your trash pick back to Starter Pick (Tier 1, Level 1)");
        player.sendMessage("§c• All your perk levels to 0");
        player.sendMessage("§c• Your XP progress");
        player.sendMessage("");
        player.sendMessage("§aYou will gain:");
        player.sendMessage("§a• +" + plugin.getPrestigeManager().getMoneyBonusPercentage(nextPrestige) + "% permanent money bonus");
        if (nextPrestige >= plugin.getPrestigeManager().getXPMultiplierStartLevel()) {
            player.sendMessage("§a• +" + plugin.getPrestigeManager().getXPBonus(nextPrestige) + " permanent XP bonus");
        }
        player.sendMessage("");
        player.sendMessage("§7Your statistics will be kept.");
        player.sendMessage("");
        player.sendMessage("§6Type 'prestige confirm' in chat to proceed!");
        player.sendMessage("§7Or reopen /prestige to cancel.");

        // Set a flag that this player is in prestige confirmation mode
        // We'll handle this in a chat listener or add it to player data temporarily
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;

        HandlerList.unregisterAll(this);
    }
}