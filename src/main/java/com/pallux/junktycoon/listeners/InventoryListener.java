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
import org.bukkit.inventory.ItemStack;

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
}