package com.pallux.junktycoon.listeners;

import com.pallux.junktycoon.JunkTycoon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class InventoryListener implements Listener {

    private final JunkTycoon plugin;

    public InventoryListener(JunkTycoon plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        if (plugin.getTrashPickManager().isTrashPick(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getConfigManager().getMessage("general.prefix") +
                    "§cYou cannot drop your trash pick!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        int lockedSlot = plugin.getConfigManager().getMainConfig().getInt("settings.locked_hotbar_slot", 0);

        // Check if trying to move trash pick from locked slot
        if (event.getSlot() == lockedSlot && event.getClickedInventory() == player.getInventory()) {
            if (currentItem != null && plugin.getTrashPickManager().isTrashPick(currentItem)) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getMessage("general.prefix") +
                        "§cYou cannot move your trash pick from this slot!");
                return;
            }
        }

        // Check if trying to place trash pick in non-locked slot
        if (cursorItem != null && plugin.getTrashPickManager().isTrashPick(cursorItem)) {
            if (event.getSlot() != lockedSlot || event.getClickedInventory() != player.getInventory()) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getMessage("general.prefix") +
                        "§cYour trash pick must stay in slot " + (lockedSlot + 1) + "!");
                return;
            }
        }

        // Prevent moving items into the locked slot (except trash picks)
        if (event.getSlot() == lockedSlot && event.getClickedInventory() == player.getInventory()) {
            if (cursorItem != null && !plugin.getTrashPickManager().isTrashPick(cursorItem)) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getMessage("general.prefix") +
                        "§cThis slot is reserved for your trash pick!");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int lockedSlot = plugin.getConfigManager().getMainConfig().getInt("settings.locked_hotbar_slot", 0);

        // Check if dragging involves the locked slot
        if (event.getInventorySlots().contains(lockedSlot)) {
            ItemStack draggedItem = event.getOldCursor();

            if (draggedItem != null && !plugin.getTrashPickManager().isTrashPick(draggedItem)) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getMessage("general.prefix") +
                        "§cThis slot is reserved for your trash pick!");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = event.getMainHandItem();
        ItemStack offHand = event.getOffHandItem();

        // Prevent swapping trash pick to offhand
        if (mainHand != null && plugin.getTrashPickManager().isTrashPick(mainHand)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("general.prefix") +
                    "§cYou cannot move your trash pick to your offhand!");
        }

        // Prevent swapping trash pick from offhand to main hand (shouldn't happen, but just in case)
        if (offHand != null && plugin.getTrashPickManager().isTrashPick(offHand)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        int lockedSlot = plugin.getConfigManager().getMainConfig().getInt("settings.locked_hotbar_slot", 0);

        // Check if player is switching away from the locked slot or to the locked slot
        if (event.getPreviousSlot() == lockedSlot || event.getNewSlot() == lockedSlot) {
            // Run check after a short delay to ensure inventory state is updated
            new BukkitRunnable() {
                @Override
                public void run() {
                    ensureTrashPickInCorrectSlot(player);
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    private void ensureTrashPickInCorrectSlot(Player player) {
        int lockedSlot = plugin.getConfigManager().getMainConfig().getInt("settings.locked_hotbar_slot", 0);
        ItemStack lockedSlotItem = player.getInventory().getItem(lockedSlot);

        // If the locked slot doesn't have a trash pick, find it and move it back
        if (lockedSlotItem == null || !plugin.getTrashPickManager().isTrashPick(lockedSlotItem)) {

            // Search for trash picks in other slots
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);

                if (item != null && plugin.getTrashPickManager().isTrashPick(item)) {
                    // Found a trash pick in wrong slot

                    if (i != lockedSlot) {
                        // Move any item from locked slot to the trash pick's current slot
                        player.getInventory().setItem(i, lockedSlotItem);

                        // Move trash pick back to locked slot
                        player.getInventory().setItem(lockedSlot, item);

                        player.sendMessage(plugin.getConfigManager().getMessage("general.prefix") +
                                "§eYour trash pick has been moved back to slot " + (lockedSlot + 1) + "!");
                        return;
                    }
                }
            }

            // If no trash pick found anywhere, give a new one
            plugin.getTrashPickManager().giveTrashPick(player);
            player.sendMessage(plugin.getConfigManager().getMessage("general.prefix") +
                    "§aYour trash pick has been restored!");
        }

        // Remove any duplicate trash picks from other slots
        removeDuplicateTrashPicks(player, lockedSlot);
    }

    private void removeDuplicateTrashPicks(Player player, int lockedSlot) {
        boolean foundDuplicates = false;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (i == lockedSlot) continue; // Skip the locked slot

            ItemStack item = player.getInventory().getItem(i);
            if (item != null && plugin.getTrashPickManager().isTrashPick(item)) {
                // Remove duplicate trash pick
                player.getInventory().setItem(i, null);
                foundDuplicates = true;
            }
        }

        if (foundDuplicates) {
            player.sendMessage(plugin.getConfigManager().getMessage("general.prefix") +
                    "§cDuplicate trash picks have been removed!");
        }
    }
}