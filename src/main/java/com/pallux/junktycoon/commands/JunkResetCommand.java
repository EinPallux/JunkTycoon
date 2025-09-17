package com.pallux.junktycoon.commands;

import com.pallux.junktycoon.JunkTycoon;
import com.pallux.junktycoon.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class JunkResetCommand implements CommandExecutor, TabCompleter {

    private final JunkTycoon plugin;

    public JunkResetCommand(JunkTycoon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("junktycoon.admin.reset")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.no_permission"));
            return true;
        }

        if (args.length == 0) {
            sendUsageMessage(sender);
            return true;
        }

        String targetPlayerName = args[0];

        if (args.length == 1) {
            return showResetConfirmation(sender, targetPlayerName);
        } else if (args.length == 2 && args[1].equalsIgnoreCase("confirm")) {
            return executeReset(sender, targetPlayerName);
        } else {
            sendUsageMessage(sender);
            return true;
        }
    }

    private void sendUsageMessage(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().getMessage("general.prefix") +
                "§cUsage: /junkreset <player> [confirm]");
    }

    private boolean showResetConfirmation(CommandSender sender, String targetPlayerName) {
        // Check if player exists
        if (!doesPlayerExist(targetPlayerName)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.prefix") +
                    "§cPlayer '" + targetPlayerName + "' not found or has never played before!");
            return true;
        }

        // Show confirmation warning
        String prefix = plugin.getConfigManager().getMessage("general.prefix");
        sender.sendMessage(prefix + "§e⚠ WARNING: This will completely reset " + targetPlayerName + "'s JunkTycoon progress!");
        sender.sendMessage("§7This will reset:");
        sender.sendMessage("§7▪ Trash pick tier and level");
        sender.sendMessage("§7▪ All perk levels (Cooldown, Multiplier, Rarity, etc.)");
        sender.sendMessage("§7▪ XP and progression");
        sender.sendMessage("§7▪ Statistics (trash picked, money earned)");
        sender.sendMessage("");
        sender.sendMessage("§c⚠ This action CANNOT be undone!");
        sender.sendMessage("");
        sender.sendMessage("§aTo confirm this reset, run:");
        sender.sendMessage("§f/junkreset " + targetPlayerName + " confirm");

        return true;
    }

    private boolean executeReset(CommandSender sender, String targetPlayerName) {
        UUID targetUUID = getPlayerUUID(targetPlayerName);
        if (targetUUID == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.prefix") +
                    "§cPlayer '" + targetPlayerName + "' not found!");
            return true;
        }

        try {
            // Perform the reset
            performPlayerReset(targetUUID, targetPlayerName);

            // Notify admin of success
            sender.sendMessage(plugin.getConfigManager().getMessage("general.prefix") +
                    "§a✓ Successfully reset " + targetPlayerName + "'s JunkTycoon progression!");

            // Log the action
            plugin.getLogger().info("[ADMIN] " + sender.getName() + " reset JunkTycoon data for player: " + targetPlayerName + " (UUID: " + targetUUID + ")");

        } catch (Exception e) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.prefix") +
                    "§cAn error occurred while resetting player data: " + e.getMessage());
            plugin.getLogger().severe("Failed to reset player " + targetPlayerName + ": " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private void performPlayerReset(UUID playerUUID, String playerName) {
        // Get player data (loads from file if not cached)
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(playerUUID);

        // Reset all progression data
        resetAllProgressionData(playerData);

        // Save the reset data immediately
        plugin.getPlayerDataManager().savePlayerData(playerUUID);

        // Handle online player updates
        Player onlinePlayer = Bukkit.getPlayer(playerUUID);
        if (onlinePlayer != null) {
            handleOnlinePlayerReset(onlinePlayer);
        }
    }

    private void resetAllProgressionData(PlayerData playerData) {
        // Reset trash pick progression
        playerData.setTrashPickTier("starter");
        playerData.setTrashPickLevel(1);
        playerData.setTrashPickXP(0);
        playerData.setLastPickTime(0);

        // Reset all perk levels to 0
        playerData.setCooldownPerkLevel(0);
        playerData.setMultiplierPerkLevel(0);
        playerData.setRarityPerkLevel(0);
        playerData.setPointFinderPerkLevel(0);
        playerData.setXpMultiplierPerkLevel(0);

        // Reset statistics
        playerData.setTotalTrashPicked(0);
        playerData.setTotalMoneyEarned(0.0);
    }

    private void handleOnlinePlayerReset(Player player) {
        // Give the player a fresh starter trash pick
        plugin.getTrashPickManager().giveTrashPick(player);

        // Notify the player
        String prefix = plugin.getConfigManager().getMessage("general.prefix");
        player.sendMessage("");
        player.sendMessage(prefix + "§c⚠ Your JunkTycoon progression has been reset by an administrator!");
        player.sendMessage("§7All your progress has been cleared and you're starting fresh.");
        player.sendMessage("§7You have been given a new §aStarter Pick §7to begin your journey again!");
        player.sendMessage("");
    }

    private boolean doesPlayerExist(String playerName) {
        // Check if player is online
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return true;
        }

        // Check if player has played before (offline player)
        return Bukkit.getOfflinePlayer(playerName).hasPlayedBefore();
    }

    private UUID getPlayerUUID(String playerName) {
        // Try online player first
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return onlinePlayer.getUniqueId();
        }

        // Try offline player
        var offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (offlinePlayer.hasPlayedBefore()) {
            return offlinePlayer.getUniqueId();
        }

        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("junktycoon.admin.reset")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            // Tab complete with online player names
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .sorted()
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            // Tab complete with "confirm"
            if ("confirm".toLowerCase().startsWith(args[1].toLowerCase())) {
                List<String> completions = new ArrayList<>();
                completions.add("confirm");
                return completions;
            }
        }

        return new ArrayList<>();
    }
}