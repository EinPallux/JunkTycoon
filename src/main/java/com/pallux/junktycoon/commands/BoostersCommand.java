package com.pallux.junktycoon.commands;

import com.pallux.junktycoon.JunkTycoon;
import com.pallux.junktycoon.guis.BoosterShopGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BoostersCommand implements CommandExecutor, TabCompleter {

    private final JunkTycoon plugin;

    public BoostersCommand(JunkTycoon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.player_only"));
            return true;
        }

        if (!sender.hasPermission("junktycoon.boosters")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.no_permission"));
            return true;
        }

        if (!plugin.getPlayerPointsHook().isEnabled()) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.prefix") +
                    "§cPlayerPoints plugin is required to use the booster shop!");
            return true;
        }

        // Open booster shop GUI
        BoosterShopGUI gui = new BoosterShopGUI(plugin, player);
        gui.open();

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}