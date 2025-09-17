package com.pallux.junktycoon.listeners;

import com.pallux.junktycoon.JunkTycoon;
import com.pallux.junktycoon.data.PlayerData;
import com.pallux.junktycoon.guis.UpgradeGUI;
import com.pallux.junktycoon.models.TrashType;
import com.pallux.junktycoon.models.TrashPickTier;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PlayerInteractListener implements Listener {

    private final JunkTycoon plugin;
    private final Random random = new Random();

    public PlayerInteractListener(JunkTycoon plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        // Check if it's a trash pick
        if (!plugin.getTrashPickManager().isTrashPick(item)) return;

        Action action = event.getAction();

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            handleTrashPicking(player);
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            openUpgradeGUI(player);
        }
    }

    private void handleTrashPicking(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);

        // Check cooldown
        if (!canPickTrash(player, playerData)) {
            long remainingTime = getRemainingCooldown(playerData);
            double remainingSeconds = remainingTime / 1000.0;

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("time", String.format("%.1f", remainingSeconds));
            String message = plugin.getConfigManager().getMessage("trash_finding.cooldown", placeholders);
            player.sendMessage(message);
            return;
        }

        // Update last pick time IMMEDIATELY
        playerData.setLastPickTime(System.currentTimeMillis());

        // Generate trash
        TrashType trashType = plugin.getTrashManager().generateTrash(player);
        int trashAmount = plugin.getTrashManager().generateTrashAmount(player);
        int trashValue = plugin.getTrashManager().generateTrashValue(trashType);
        int totalValue = trashValue * trashAmount;

        // Give money
        plugin.getVaultHook().depositMoney(player, totalValue);

        // Update player data
        playerData.addTrashPicked(trashAmount);
        playerData.addMoneyEarned(totalValue);

        // Give XP only if not at max level for current tier
        TrashPickTier currentTier = plugin.getTrashPickManager().getTier(playerData.getTrashPickTier());
        boolean canGainXP = true;

        if (currentTier != null && currentTier.getMaxLevel() != -1) {
            // Check if already at max level for this tier (-1 means infinite levels)
            if (playerData.getTrashPickLevel() >= currentTier.getMaxLevel()) {
                canGainXP = false;
            }
        }

        int xpGain = 0;
        if (canGainXP) {
            int baseXP = plugin.getTrashPickManager().getXPForTrashType(trashType.getId(), playerData) * trashAmount;

            // Apply global XP booster if active
            double xpBoosterMultiplier = plugin.getBoosterManager().getBoosterMultiplier("xp");
            xpGain = (int) (baseXP * xpBoosterMultiplier);

            playerData.addXP(xpGain);

            // Check for level up
            if (plugin.getTrashPickManager().canLevelUp(playerData)) {
                plugin.getTrashPickManager().levelUp(player, playerData);
            }
        }

        // Display trash found message
        displayTrashFoundMessage(player, trashType, trashAmount, totalValue, xpGain);

        // Play effects
        playTrashEffects(player, trashType);

        // Check for points (if PlayerPoints is enabled)
        checkForPoints(player, playerData);

        // Update trash pick in inventory
        plugin.getTrashPickManager().updateTrashPickInInventory(player);

        // Immediately update cooldown indicator
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.updateCooldownIndicator(player);
        });

        // Save player data immediately to ensure timestamp is persisted
        plugin.getPlayerDataManager().savePlayerData(player);
    }

    private boolean canPickTrash(Player player, PlayerData playerData) {
        if (player.hasPermission("junktycoon.admin.nocooldown")) {
            return true;
        }

        long currentTime = System.currentTimeMillis();
        long lastPickTime = playerData.getLastPickTime();

        // If it's the first time picking (lastPickTime = 0), allow it
        if (lastPickTime == 0) {
            return true;
        }

        double cooldownSeconds = calculateCooldown(playerData);
        long cooldownMs = (long) (cooldownSeconds * 1000);
        long timePassed = currentTime - lastPickTime;

        return timePassed >= cooldownMs;
    }

    private double calculateCooldown(PlayerData playerData) {
        double baseCooldown = plugin.getConfigManager().getMainConfig().getDouble("settings.default_picking_cooldown", 2.0);

        int cooldownPerkLevel = playerData.getCooldownPerkLevel();
        if (cooldownPerkLevel > 0) {
            double reductionPerLevel = plugin.getConfigManager().getMainConfig().getDouble("perks.picking_cooldown.cooldown_reduction", 0.1);
            double reduction = cooldownPerkLevel * reductionPerLevel;
            baseCooldown = Math.max(0.1, baseCooldown - reduction); // Minimum 0.1 seconds
        }

        return baseCooldown;
    }

    private long getRemainingCooldown(PlayerData playerData) {
        long currentTime = System.currentTimeMillis();
        long lastPickTime = playerData.getLastPickTime();

        // If never picked before, no cooldown
        if (lastPickTime == 0) {
            return 0;
        }

        double cooldownSeconds = calculateCooldown(playerData);
        long cooldownMs = (long) (cooldownSeconds * 1000);
        long timePassed = currentTime - lastPickTime;

        return Math.max(0, cooldownMs - timePassed);
    }

    private void displayTrashFoundMessage(Player player, TrashType trashType, int amount, int value, int xp) {
        // Title and subtitle
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", String.valueOf(amount));

        // Format the rarity name with colors properly
        String formattedRarityName = plugin.getConfigManager().formatText(trashType.getName());
        placeholders.put("rarity", formattedRarityName);
        placeholders.put("value", String.valueOf(value));

        String title = plugin.getConfigManager().getMessage("trash_finding.found_title", placeholders);
        String subtitle = plugin.getConfigManager().getMessage("trash_finding.found_subtitle", placeholders);

        player.sendTitle(title, subtitle, 10, 40, 10);

        // Chat message (if enabled)
        if (plugin.getConfigManager().getMessagesConfig().getBoolean("trash_finding.chat_message.enabled", false)) {
            String chatMessage = plugin.getConfigManager().getMessage("trash_finding.chat_message.format", placeholders);
            player.sendMessage(chatMessage);
        }

        // XP gain message
        Map<String, String> xpPlaceholders = new HashMap<>();
        xpPlaceholders.put("xp", String.valueOf(xp));
        String xpMessage = plugin.getConfigManager().getMessage("trash_finding.xp_gain", xpPlaceholders);
        player.sendActionBar(xpMessage);
    }

    private void playTrashEffects(Player player, TrashType trashType) {
        if (!plugin.getConfigManager().getTrashConfig().getBoolean("effects.particles.enabled", true)) {
            return;
        }

        // Play particle effect
        if (trashType.hasParticle()) {
            player.getWorld().spawnParticle(trashType.getParticle(),
                    player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
        }

        // Play sound effect
        if (trashType.hasSound() && plugin.getConfigManager().getTrashConfig().getBoolean("effects.sounds.enabled", true)) {
            player.playSound(player.getLocation(), trashType.getSound(), 0.5f, 1.0f);
        }
    }

    private void checkForPoints(Player player, PlayerData playerData) {
        if (!plugin.getPlayerPointsHook().isEnabled()) return;

        int pointFinderLevel = playerData.getPointFinderPerkLevel();
        if (pointFinderLevel <= 0) return;

        double baseChance = plugin.getConfigManager().getMainConfig().getDouble("perks.point_finder.base_chance", 0.01);
        double chanceIncrease = plugin.getConfigManager().getMainConfig().getDouble("perks.point_finder.chance_increase", 0.005);

        double totalChance = baseChance + (pointFinderLevel * chanceIncrease);

        if (random.nextDouble() < totalChance) {
            int minPoints = plugin.getConfigManager().getMainConfig().getInt("perks.point_finder.min_points", 1);
            int maxPoints = plugin.getConfigManager().getMainConfig().getInt("perks.point_finder.max_points", 5);

            int pointsFound = random.nextInt(maxPoints - minPoints + 1) + minPoints;
            plugin.getPlayerPointsHook().givePoints(player, pointsFound);

            // Display points message
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", String.valueOf(pointsFound));

            String pointsTitle = plugin.getConfigManager().getMessage("points.found_title", placeholders);
            String pointsSubtitle = plugin.getConfigManager().getMessage("points.found_subtitle", placeholders);

            // Show points message after a short delay
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.sendTitle(pointsTitle, pointsSubtitle, 5, 30, 5);

                String chatMessage = plugin.getConfigManager().getMessage("points.chat_message", placeholders);
                player.sendMessage(chatMessage);
            }, 60L); // 3 second delay
        }
    }

    private void openUpgradeGUI(Player player) {
        UpgradeGUI gui = new UpgradeGUI(plugin, player);
        gui.open();
    }
}