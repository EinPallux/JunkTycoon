package com.pallux.junktycoon.commands;

import com.pallux.junktycoon.JunkTycoon;
import com.pallux.junktycoon.data.PlayerData;
import com.pallux.junktycoon.guis.UpgradeGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JunkTycoonCommand implements CommandExecutor, TabCompleter {

    private final JunkTycoon plugin;

    public JunkTycoonCommand(JunkTycoon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> sendHelpMessage(sender);
            case "stats" -> handleStatsCommand(sender);
            case "upgrade", "gui" -> handleUpgradeCommand(sender);
            case "give" -> handleGiveCommand(sender);
            case "version", "info" -> handleInfoCommand(sender);
            default -> sendHelpMessage(sender);
        }

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        String prefix = plugin.getConfigManager().getMessage("general.prefix");

        sender.sendMessage(prefix + "§6=== JunkTycoon Commands ===");
        sender.sendMessage("§e/junktycoon help §7- Show this help message");
        sender.sendMessage("§e/junktycoon stats §7- View your statistics");
        sender.sendMessage("§e/junktycoon upgrade §7- Open upgrade GUI");
        sender.sendMessage("§e/junktycoon info §7- Plugin information");

        if (sender.hasPermission("junktycoon.admin.reload")) {
            sender.sendMessage("§e/junkreload §7- Reload the plugin");
        }

        if (sender.hasPermission("junktycoon.admin.give")) {
            sender.sendMessage("§e/junktycoon give §7- Get a new trash pick");
        }
    }

    private void handleStatsCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.player_only"));
            return;
        }

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        String prefix = plugin.getConfigManager().getMessage("general.prefix");

        sender.sendMessage(prefix + "§6=== Your Statistics ===");
        sender.sendMessage("§7Trash Pick Tier: §f" + plugin.getConfigManager().formatText(
                plugin.getTrashPickManager().getTier(playerData.getTrashPickTier()).getName()));
        sender.sendMessage("§7Trash Pick Level: §f" + playerData.getTrashPickLevel());
        sender.sendMessage("§7Trash Pick XP: §f" + playerData.getTrashPickXP() + "§7/§f" +
                plugin.getTrashPickManager().calculateRequiredXP(playerData.getTrashPickLevel()));
        sender.sendMessage("§7Total Trash Picked: §f" + playerData.getTotalTrashPicked());
        sender.sendMessage("§7Total Money Earned: §f" + plugin.getVaultHook().formatMoney(playerData.getTotalMoneyEarned()));
        sender.sendMessage("§7Current Balance: §f" + plugin.getVaultHook().formatMoney(plugin.getVaultHook().getBalance(player)));
        sender.sendMessage("");
        sender.sendMessage("§6=== Perk Levels ===");
        sender.sendMessage("§7Cooldown Reduction: §f" + playerData.getCooldownPerkLevel());
        sender.sendMessage("§7Trash Multiplier: §f" + playerData.getMultiplierPerkLevel());
        sender.sendMessage("§7Trash Rarity: §f" + playerData.getRarityPerkLevel());
        sender.sendMessage("§7Point Finder: §f" + playerData.getPointFinderPerkLevel());

        if (plugin.getPlayerPointsHook().isEnabled()) {
            sender.sendMessage("§7Current Points: §f" + plugin.getPlayerPointsHook().getPoints(player));
        }
    }

    private void handleUpgradeCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.player_only"));
            return;
        }

        UpgradeGUI gui = new UpgradeGUI(plugin, player);
        gui.open();
    }

    private void handleGiveCommand(CommandSender sender) {
        if (!sender.hasPermission("junktycoon.admin.give")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.no_permission"));
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.player_only"));
            return;
        }

        plugin.getTrashPickManager().giveTrashPick(player);
        sender.sendMessage(plugin.getConfigManager().getMessage("general.prefix") + "§aYou have been given a new trash pick!");
    }

    private void handleInfoCommand(CommandSender sender) {
        String prefix = plugin.getConfigManager().getMessage("general.prefix");

        sender.sendMessage(prefix + "§6=== JunkTycoon Information ===");
        sender.sendMessage("§7Version: §f" + plugin.getDescription().getVersion());
        sender.sendMessage("§7Author: §f" + plugin.getDescription().getAuthors().get(0));
        sender.sendMessage("§7Description: §f" + plugin.getDescription().getDescription());
        sender.sendMessage("§7API Version: §f" + plugin.getDescription().getAPIVersion());

        // Hook status
        sender.sendMessage("");
        sender.sendMessage("§6=== Plugin Hooks ===");
        sender.sendMessage("§7Vault: §a✓ Connected");
        sender.sendMessage("§7PlayerPoints: " + (plugin.getPlayerPointsHook().isEnabled() ? "§a✓ Connected" : "§c✗ Not found"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("help", "stats", "upgrade", "info"));

            if (sender.hasPermission("junktycoon.admin.give")) {
                completions.add("give");
            }

            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return new ArrayList<>();
    }
}