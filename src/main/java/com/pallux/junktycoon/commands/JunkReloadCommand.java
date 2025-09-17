package com.pallux.junktycoon.commands;

import com.pallux.junktycoon.JunkTycoon;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class JunkReloadCommand implements CommandExecutor {

    private final JunkTycoon plugin;

    public JunkReloadCommand(JunkTycoon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("junktycoon.admin.reload")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.no_permission"));
            return true;
        }

        long startTime = System.currentTimeMillis();

        try {
            plugin.reload();

            long endTime = System.currentTimeMillis();
            long reloadTime = endTime - startTime;

            String message = plugin.getConfigManager().getMessage("general.reload_success")
                    .replace("%time%", String.valueOf(reloadTime));
            sender.sendMessage(plugin.getConfigManager().getMessage("general.prefix") + message);

        } catch (Exception e) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.prefix") +
                    "§cFailed to reload plugin: " + e.getMessage());
            plugin.getLogger().severe("Error during reload: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}