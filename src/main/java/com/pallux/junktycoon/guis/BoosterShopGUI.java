package com.pallux.junktycoon.guis;

import com.pallux.junktycoon.JunkTycoon;
import org.bukkit.Bukkit;
import org.bukkit.Color;
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
import org.bukkit.inventory.meta.PotionMeta;

import java.util.ArrayList;
import java.util.List;

public class BoosterShopGUI implements Listener {

    private final JunkTycoon plugin;
    private final Player player;
    private final Inventory inventory;

    public BoosterShopGUI(JunkTycoon plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        FileConfiguration shopConfig = plugin.getConfigManager().getConfig("shop");
        int size = shopConfig.getInt("shop.size", 45);
        String title = plugin.getConfigManager().formatText(shopConfig.getString("shop.title", "&6&lGlobal Boosters Shop"));

        this.inventory = Bukkit.createInventory(null, size, title);

        setupGUI();
    }

    private void setupGUI() {
        // Clear inventory
        inventory.clear();

        // Fill with glass panes
        fillWithGlass();

        // Add booster items
        FileConfiguration shopConfig = plugin.getConfigManager().getConfig("shop");

        for (String boosterId : shopConfig.getConfigurationSection("boosters").getKeys(false)) {
            addBoosterItem(boosterId);
        }
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

    private void addBoosterItem(String boosterId) {
        FileConfiguration shopConfig = plugin.getConfigManager().getConfig("shop");
        String path = "boosters." + boosterId;

        int slot = shopConfig.getInt(path + ".slot");
        String materialName = shopConfig.getString(path + ".material", "POTION");
        Material material = Material.valueOf(materialName);
        int price = shopConfig.getInt(path + ".price");
        String name = shopConfig.getString(path + ".name");
        List<String> description = shopConfig.getStringList(path + ".description");
        String type = shopConfig.getString(path + ".type");

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Set display name
            meta.setDisplayName(plugin.getConfigManager().formatText(name));

            // Handle potion coloring
            if (material == Material.POTION && meta instanceof PotionMeta potionMeta) {
                String colorName = shopConfig.getString(path + ".potion_color", "GREEN");
                Color color = getColorByName(colorName);
                potionMeta.setColor(color);
            }

            // Set lore
            List<String> lore = new ArrayList<>();
            for (String line : description) {
                line = line.replace("%price%", String.valueOf(price));
                lore.add(plugin.getConfigManager().formatText(line));
            }

            // Add status information
            lore.add("");
            if (plugin.getBoosterManager().isBoosterTypeActive(type)) {
                lore.add(plugin.getConfigManager().formatText("&c⚠ A " + type + " booster is already active!"));
            } else {
                lore.add(plugin.getConfigManager().formatText("&a✓ Available for purchase"));
            }

            meta.setLore(lore);

            // Hide other flags
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
    }

    private Color getColorByName(String colorName) {
        return switch (colorName.toLowerCase()) {
            case "red" -> Color.RED;
            case "green" -> Color.GREEN;
            case "blue" -> Color.BLUE;
            case "yellow" -> Color.YELLOW;
            case "purple" -> Color.PURPLE;
            case "orange" -> Color.ORANGE;
            case "pink" -> Color.FUCHSIA;
            case "lime" -> Color.LIME;
            case "cyan" -> Color.AQUA;
            default -> Color.WHITE;
        };
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

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        // Find which booster was clicked
        FileConfiguration shopConfig = plugin.getConfigManager().getConfig("shop");

        for (String boosterId : shopConfig.getConfigurationSection("boosters").getKeys(false)) {
            String path = "boosters." + boosterId;
            int slot = shopConfig.getInt(path + ".slot");

            if (event.getSlot() == slot) {
                handleBoosterPurchase(boosterId);
                break;
            }
        }
    }

    private void handleBoosterPurchase(String boosterId) {
        if (!plugin.getPlayerPointsHook().isEnabled()) {
            FileConfiguration shopConfig = plugin.getConfigManager().getConfig("shop");
            String message = plugin.getConfigManager().formatText(shopConfig.getString("messages.playerpoints_required"));
            player.sendMessage(plugin.getConfigManager().getMessage("general.prefix") + message);
            return;
        }

        FileConfiguration shopConfig = plugin.getConfigManager().getConfig("shop");
        String path = "boosters." + boosterId;

        int price = shopConfig.getInt(path + ".price");
        String name = shopConfig.getString(path + ".name");
        String type = shopConfig.getString(path + ".type");
        double multiplier = shopConfig.getDouble(path + ".multiplier");
        int duration = shopConfig.getInt(path + ".duration");

        // Check if booster type is already active
        if (plugin.getBoosterManager().isBoosterTypeActive(type)) {
            String message = plugin.getConfigManager().formatText(shopConfig.getString("messages.booster_already_active"));
            player.sendMessage(plugin.getConfigManager().getMessage("general.prefix") + message);
            return;
        }

        // Check if player has enough points
        int currentPoints = plugin.getPlayerPointsHook().getPoints(player);
        if (currentPoints < price) {
            String message = shopConfig.getString("messages.insufficient_points");
            message = message.replace("%required%", String.valueOf(price));
            player.sendMessage(plugin.getConfigManager().getMessage("general.prefix") +
                    plugin.getConfigManager().formatText(message));
            return;
        }

        // Purchase successful
        plugin.getPlayerPointsHook().takePoints(player, price);

        // Activate booster
        String formattedName = plugin.getConfigManager().formatText(name);
        boolean success = plugin.getBoosterManager().activateBooster(boosterId, formattedName, type, multiplier, duration, player);

        if (success) {
            // Send success message
            String message = shopConfig.getString("messages.purchase_success");
            message = message.replace("%booster_name%", formattedName);
            player.sendMessage(plugin.getConfigManager().getMessage("general.prefix") +
                    plugin.getConfigManager().formatText(message));

            // Close GUI
            player.closeInventory();
        } else {
            // Refund points if activation failed
            plugin.getPlayerPointsHook().givePoints(player, price);

            String message = plugin.getConfigManager().formatText(shopConfig.getString("messages.booster_already_active"));
            player.sendMessage(plugin.getConfigManager().getMessage("general.prefix") + message);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;

        HandlerList.unregisterAll(this);
    }
}