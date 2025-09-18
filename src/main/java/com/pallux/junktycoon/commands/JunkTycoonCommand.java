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

        sender.sendMessage("");
        sender.sendMessage("§6§l         JUNK TYCOON HELP");
        sender.sendMessage("§6§m▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("");

        // Core Commands
        sender.sendMessage("§6▪ §lCore Commands:");
        sender.sendMessage("  §e/jt help §7- §fShow this help menu");
        sender.sendMessage("  §e/jt stats §7- §fView your progression statistics");
        sender.sendMessage("  §e/jt upgrade §7- §fOpen the perk upgrade menu");
        sender.sendMessage("  §e/jt info §7- §fView plugin information");
        sender.sendMessage("");

        // New Feature Commands
        sender.sendMessage("§6▪ §lFeature Commands:");
        sender.sendMessage("  §e/boosters §7- §fOpen global boosters shop");
        sender.sendMessage("  §e/prestige §7- §fOpen prestige system menu");
        sender.sendMessage("");

        // Admin Commands (only show if has permission)
        if (sender.hasPermission("junktycoon.admin.reload") || sender.hasPermission("junktycoon.admin.reset")) {
            sender.sendMessage("§c▪ §lAdmin Commands:");
            if (sender.hasPermission("junktycoon.admin.reload")) {
                sender.sendMessage("  §c/junkreload §7- §fReload plugin configuration");
            }
            if (sender.hasPermission("junktycoon.admin.reset")) {
                sender.sendMessage("  §c/junkreset <player> §7- §fReset player's progression");
            }
            sender.sendMessage("");
        }

        // Quick Info Section
        sender.sendMessage("§6▪ §lQuick Info:");
        sender.sendMessage("  §7• §fLeft-click air with trash pick to collect trash");
        sender.sendMessage("  §7• §fRight-click trash pick to upgrade perks");
        sender.sendMessage("  §7• §fUpgrade your pick tier for better progression");
        sender.sendMessage("  §7• §fReach prestige to unlock permanent bonuses!");
        sender.sendMessage("");

        // Plugin Hooks Status (only show if has permission)
        if (sender.hasPermission("junktycoon.admin.*")) {
            sender.sendMessage("§6▪ §lPlugin Integrations:");
            sender.sendMessage("  §7• §aVault §7- §f✓ Economy system connected");
            sender.sendMessage("  §7• §9PlayerPoints §7- " + (plugin.getPlayerPointsHook().isEnabled() ? "§f✓ Points system active" : "§c✗ Not available"));
            sender.sendMessage("  §7• §bPlaceholderAPI §7- " + (plugin.getPlaceholderAPIHook().isEnabled() ? "§f✓ Placeholders active" : "§c✗ Not available"));
            sender.sendMessage("");
        }
        sender.sendMessage("§6§m▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("§6§l      Happy Trash Collecting!");
        sender.sendMessage("");
    }

    private void handleStatsCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.player_only"));
            return;
        }

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        String prefix = plugin.getConfigManager().getMessage("general.prefix");

        sender.sendMessage("");
        sender.sendMessage("§6§l         YOUR STATISTICS");
        sender.sendMessage("§6§m▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("");

        // Prestige & Trash Pick Info
        sender.sendMessage("§6▪ §lProgression:");
        if (playerData.getPrestigeLevel() > 0) {
            sender.sendMessage("  §7Prestige: " + plugin.getPrestigeManager().getPrestigeLevelName(playerData.getPrestigeLevel()));
            sender.sendMessage("  §7Money Bonus: §a+" + plugin.getPrestigeManager().getMoneyBonusPercentage(playerData.getPrestigeLevel()) + "%");
            int xpBonus = plugin.getPrestigeManager().getXPBonus(playerData.getPrestigeLevel());
            if (xpBonus > 0) {
                sender.sendMessage("  §7XP Bonus: §b+" + xpBonus + " flat");
            }
        } else {
            sender.sendMessage("  §7Prestige: §7None");
        }
        sender.sendMessage("  §7Trash Pick: §f" + plugin.getConfigManager().formatText(
                plugin.getTrashPickManager().getTier(playerData.getTrashPickTier()).getName()));
        sender.sendMessage("  §7Level: §f" + playerData.getTrashPickLevel());

        int requiredXP = plugin.getTrashPickManager().calculateRequiredXP(playerData.getTrashPickLevel());
        double xpProgress = requiredXP > 0 ? ((double) playerData.getTrashPickXP() / requiredXP) * 100.0 : 100.0;
        sender.sendMessage("  §7XP: §f" + playerData.getTrashPickXP() + "§7/§f" + requiredXP +
                " §7(§e" + String.format("%.1f", xpProgress) + "%§7)");
        sender.sendMessage("");

        // Statistics
        sender.sendMessage("§6▪ §lLifetime Statistics:");
        sender.sendMessage("  §7Total Trash Picked: §f" + String.format("%,d", playerData.getTotalTrashPicked()));
        sender.sendMessage("  §7Total Money Earned: §f" + plugin.getVaultHook().formatMoney(playerData.getTotalMoneyEarned()));
        sender.sendMessage("  §7Current Balance: §f" + plugin.getVaultHook().formatMoney(plugin.getVaultHook().getBalance(player)));

        if (plugin.getPlayerPointsHook().isEnabled()) {
            sender.sendMessage("  §7Current Points: §9" + plugin.getPlayerPointsHook().getPoints(player));
        }
        sender.sendMessage("");

        // Perk Levels
        sender.sendMessage("§6▪ §lPerk Levels:");
        sender.sendMessage("  §7Cooldown Reduction: §f" + playerData.getCooldownPerkLevel());
        sender.sendMessage("  §7Trash Multiplier: §f" + playerData.getMultiplierPerkLevel());
        sender.sendMessage("  §7Trash Rarity: §f" + playerData.getRarityPerkLevel());
        sender.sendMessage("  §7Point Finder: §f" + playerData.getPointFinderPerkLevel());
        sender.sendMessage("  §7XP Multiplier: §f" + playerData.getXpMultiplierPerkLevel());
        sender.sendMessage("");

        // Quick Tips
        sender.sendMessage("§6▪ §lQuick Tips:");
        if (playerData.getPrestigeLevel() == 0 && playerData.getTrashPickTier().equals("gods") && playerData.getTrashPickLevel() >= 100) {
            sender.sendMessage("  §a✨ You can prestige! Use §f/prestige §ato unlock bonuses!");
        } else if (!playerData.getTrashPickTier().equals("gods")) {
            sender.sendMessage("  §e⚡ Keep leveling to upgrade your trash pick tier!");
        } else {
            sender.sendMessage("  §b🎯 Use §f/boosters §bto buy global multipliers!");
        }
        sender.sendMessage("");
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

        sender.sendMessage("");
        sender.sendMessage("§6§l         JUNK TYCOON INFO");
        sender.sendMessage("§6§m▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("");

        // Plugin Information
        sender.sendMessage("§6▪ §lPlugin Details:");
        sender.sendMessage("  §7Version: §f" + plugin.getDescription().getVersion());
        sender.sendMessage("  §7Author: §f" + plugin.getDescription().getAuthors().get(0));
        sender.sendMessage("  §7API Version: §f" + plugin.getDescription().getAPIVersion());
        sender.sendMessage("");

        // Features Overview
        sender.sendMessage("§6▪ §lMain Features:");
        sender.sendMessage("  §7• §fTrash Picking System §7- Collect trash with custom picks");
        sender.sendMessage("  §7• §fPerk System §7- Upgrade with 5 different perks");
        sender.sendMessage("  §7• §fTier Progression §7- 6 trash pick tiers to unlock");
        sender.sendMessage("  §7• §fPrestige System §7- 10 prestige levels with bonuses");
        sender.sendMessage("  §7• §fGlobal Boosters §7- Server-wide XP and trash multipliers");
        sender.sendMessage("  §7• §fPlaceholderAPI §7- Full integration with other plugins");
        sender.sendMessage("");

        // Plugin Integrations Status
        sender.sendMessage("§6▪ §lPlugin Integrations:");
        sender.sendMessage("  §7Vault: §a✓ Connected §7- Economy system active");
        sender.sendMessage("  §7PlayerPoints: " + (plugin.getPlayerPointsHook().isEnabled() ?
                "§a✓ Connected §7- Points system active" :
                "§c✗ Not Found §7- Install for XP Multiplier perk & boosters"));
        sender.sendMessage("  §7PlaceholderAPI: " + (plugin.getPlaceholderAPIHook().isEnabled() ?
                "§a✓ Connected §7- Placeholders active" :
                "§c✗ Not Found §7- Install for placeholder support"));
        sender.sendMessage("");

        // Configuration Info
        sender.sendMessage("§6▪ §lConfiguration:");
        sender.sendMessage("  §7All messages, prices, and mechanics are");
        sender.sendMessage("  §7fully configurable via YAML files!");
        sender.sendMessage("  §7Use §f/junkreload §7to apply changes.");
        sender.sendMessage("");

        sender.sendMessage("§6§m▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("§6§l    Thanks for using JunkTycoon!");
        sender.sendMessage("");
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