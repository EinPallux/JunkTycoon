package com.pallux.junktycoon.commands;

import com.pallux.junktycoon.JunkTycoon;
import com.pallux.junktycoon.guis.PrestigeGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PrestigeCommand implements CommandExecutor, TabCompleter {

    private final JunkTycoon plugin;

    public PrestigeCommand(JunkTycoon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.player_only"));
            return true;
        }

        if (!sender.hasPermission("junktycoon.prestige")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.no_permission"));
            return true;
        }

        if (args.length == 0) {
            // Open prestige GUI
            PrestigeGUI gui = new PrestigeGUI(plugin, player);
            gui.open();
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("confirm")) {
            // Handle prestige confirmation
            return handlePrestigeConfirmation(player);
        }

        // Invalid arguments
        player.sendMessage(plugin.getConfigManager().getMessage("general.prefix") +
                "§cUsage: /prestige [confirm]");
        return true;
    }

    private boolean handlePrestigeConfirmation(Player player) {
        // Check if player can prestige
        if (!plugin.getPrestigeManager().canPrestige(player)) {
            String message = plugin.getConfigManager().formatText(
                    plugin.getConfigManager().getConfig("prestige").getString("messages.prestige_failed", "You cannot prestige right now!"));
            player.sendMessage(plugin.getConfigManager().getMessage("general.prefix") + message);
            return true;
        }

        // Perform the prestige
        boolean success = plugin.getPrestigeManager().performPrestige(player);

        if (success) {
            // Success is handled in PrestigeManager with effects and announcements
            return true;
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("general.prefix") +
                    "§cFailed to prestige. Please try again.");
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("junktycoon.prestige")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if ("confirm".toLowerCase().startsWith(args[0].toLowerCase())) {
                completions.add("confirm");
            }
            return completions;
        }

        return new ArrayList<>();
    }
}